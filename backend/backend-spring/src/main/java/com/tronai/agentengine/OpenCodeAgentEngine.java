package com.tronai.agentengine;

import com.tronai.desktopagent.connection.WebSocketAgentTransport;
import com.tronai.desktopagent.device.DesktopDeviceEntity;
import com.tronai.desktopagent.device.DesktopDeviceRepository;
import com.tronai.desktopagent.device.DeviceStatus;
import com.tronai.desktopagent.policy.WorkspaceGuard;
import com.tronai.desktopagent.protocol.MessageType;
import com.tronai.desktopagent.protocol.ProtocolMessage;
import com.tronai.hybrid.HybridRoutingService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * OpenCode control-plane adapter.
 * <p>
 * Architecture (V4-C):
 * <pre>
 * Spring Core → authenticated desktop WebSocket → paired JavaFX agent
 *   → local OpenCode loopback (cwd = authorized workspacePath)
 * </pre>
 * Spring never assumes Core localhost is the user desktop.
 */
@Component
public class OpenCodeAgentEngine implements AgentEngine {

    private final AgentEngineSessionRepository sessions;
    private final HybridRoutingService hybrid;
    private final DesktopDeviceRepository devices;
    private final WebSocketAgentTransport transport;

    public OpenCodeAgentEngine(AgentEngineSessionRepository sessions,
                               HybridRoutingService hybrid,
                               DesktopDeviceRepository devices,
                               WebSocketAgentTransport transport) {
        this.sessions = sessions;
        this.hybrid = hybrid;
        this.devices = devices;
        this.transport = transport;
    }

    @Override
    public AgentEngineType type() {
        return AgentEngineType.OPENCODE;
    }

    @Override
    public Map<String, Object> health() {
        Map<String, Object> h = new LinkedHashMap<>();
        h.put("engine", type().name());
        h.put("executionModel", "DESKTOP_WSS");
        h.put("note", "OpenCode runs on the paired desktop via ACTION_REQUEST tool=OPENCODE. "
                + "Spring Core does not call localhost OpenCode.");
        h.put("directLoopbackDisabled", true);
        return h;
    }

    @Override
    public Map<String, Object> createSession(Long userId, String goal, String workspacePath, String planId) {
        return createSession(userId, goal, workspacePath, planId, null);
    }

    @Override
    public Map<String, Object> createSession(Long userId, String goal, String workspacePath, String planId, Long deviceId) {
        assertJobsAllowed();
        DesktopDeviceEntity device = requireOnlineDevice(userId, deviceId);
        String boundWorkspace = bindWorkspace(device, workspacePath);

        AgentEngineSessionEntity s = new AgentEngineSessionEntity();
        s.setId(UUID.randomUUID().toString());
        s.setUserId(userId);
        s.setDeviceId(device.getId());
        s.setPlanId(planId);
        s.setEngineType(AgentEngineType.OPENCODE);
        s.setGoal(goal);
        s.setWorkspacePath(boundWorkspace);
        s.setStatus("CREATED");
        s.setCreatedAt(Instant.now());
        s.setUpdatedAt(Instant.now());

        try {
            ProtocolMessage.Result ensure = invoke(device, s.getId(), boundWorkspace, "ENSURE_SERVE", null);
            if (!ensure.isSuccess()) {
                s.setStatus("UNAVAILABLE");
                s.setLastError(ensure.getSummary());
                sessions.save(s);
                return view(s);
            }
            ProtocolMessage.Arguments createArgs = new ProtocolMessage.Arguments();
            createArgs.setText("tron-" + s.getId().substring(0, 8));
            ProtocolMessage.Result created = invoke(device, s.getId(), boundWorkspace, "SESSION_CREATE", createArgs);
            if (!created.isSuccess() || created.getStdout() == null || created.getStdout().isBlank()) {
                s.setStatus("UNAVAILABLE");
                s.setLastError(created.getSummary() == null ? "SESSION_CREATE failed" : created.getSummary());
                sessions.save(s);
                return view(s);
            }
            s.setExternalSessionId(created.getStdout().trim());
            s.setStatus("READY");
        } catch (Exception e) {
            s.setStatus("UNAVAILABLE");
            s.setLastError(e.getMessage());
        }
        sessions.save(s);
        return view(s);
    }

    @Override
    public Map<String, Object> startTask(Long userId, String sessionId, String prompt) {
        return promptSession(userId, sessionId, prompt, true);
    }

    @Override
    public Map<String, Object> continueSession(Long userId, String sessionId, String prompt) {
        return promptSession(userId, sessionId, prompt, false);
    }

    @Override
    public Map<String, Object> status(Long userId, String sessionId) {
        return view(require(userId, sessionId));
    }

    @Override
    public Map<String, Object> cancel(Long userId, String sessionId) {
        AgentEngineSessionEntity s = require(userId, sessionId);
        if (s.getDeviceId() != null && s.getWorkspacePath() != null) {
            devices.findById(s.getDeviceId()).ifPresent(device -> {
                if (transport.isConnected(device.getId())) {
                    try {
                        invoke(device, s.getId(), s.getWorkspacePath(), "STOP_SERVE", null);
                    } catch (Exception ignored) {
                        // best-effort stop
                    }
                }
            });
        }
        s.setStatus("CANCELLED");
        s.setUpdatedAt(Instant.now());
        sessions.save(s);
        return view(s);
    }

    /**
     * Verify a file exists inside the session workspace via the desktop agent (real disk evidence).
     */
    public Map<String, Object> verifyFile(Long userId, String sessionId, String relativeOrAbsolutePath) {
        AgentEngineSessionEntity s = require(userId, sessionId);
        DesktopDeviceEntity device = requireOnlineDevice(userId, s.getDeviceId());
        ProtocolMessage.Arguments args = new ProtocolMessage.Arguments();
        args.setPath(relativeOrAbsolutePath);
        try {
            ProtocolMessage.Result result = invoke(device, s.getId(), s.getWorkspacePath(), "VERIFY_FILE", args);
            Map<String, Object> out = view(s);
            out.put("verified", result.isSuccess());
            out.put("path", result.getPath());
            out.put("sizeBytes", result.getSizeBytes());
            out.put("content", result.getStdout());
            out.put("summary", result.getSummary());
            out.put("note", "Disk verification via desktop agent — not model text.");
            return out;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "VERIFY_FILE failed: " + e.getMessage());
        }
    }

    private Map<String, Object> promptSession(Long userId, String sessionId, String prompt, boolean start) {
        assertJobsAllowed();
        AgentEngineSessionEntity s = require(userId, sessionId);
        DesktopDeviceEntity device = requireOnlineDevice(userId, s.getDeviceId());
        if (s.getExternalSessionId() == null || s.getExternalSessionId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "OpenCode external session missing — createSession must succeed first");
        }
        ProtocolMessage.Arguments args = new ProtocolMessage.Arguments();
        args.setQuery(s.getExternalSessionId());
        args.setText(prompt == null ? "" : prompt);
        try {
            ProtocolMessage.Result result = invoke(device, s.getId(), s.getWorkspacePath(), "PROMPT", args);
            s.setStatus(result.isSuccess() ? (start ? "RUNNING" : "CONTINUED") : "FAILED");
            if (!result.isSuccess()) {
                s.setLastError(result.getSummary());
            }
            s.setUpdatedAt(Instant.now());
            sessions.save(s);
            Map<String, Object> out = view(s);
            out.put("desktopSuccess", result.isSuccess());
            out.put("engineResponse", truncate(result.getStdout()));
            out.put("verificationRequired", true);
            out.put("note", "LLM completion text is NOT verification. Use VERIFY_FILE / inspect workspace.");
            return out;
        } catch (Exception e) {
            s.setStatus("FAILED");
            s.setLastError(e.getMessage());
            s.setUpdatedAt(Instant.now());
            sessions.save(s);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OpenCode prompt failed: " + e.getMessage());
        }
    }

    private ProtocolMessage.Result invoke(DesktopDeviceEntity device,
                                          String sessionId,
                                          String workspace,
                                          String operation,
                                          ProtocolMessage.Arguments args) throws Exception {
        if (!transport.isConnected(device.getId())) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Desktop device is not connected over WebSocket");
        }
        String actionId = UUID.randomUUID().toString();
        ProtocolMessage message = ProtocolMessage.of(MessageType.ACTION_REQUEST);
        message.setDeviceId(device.getDeviceUuid());
        message.setTaskId("engine-" + sessionId);
        message.setActionId(actionId);
        ProtocolMessage.Action action = new ProtocolMessage.Action();
        action.setTool("OPENCODE");
        action.setOperation(operation);
        action.setWorkingDirectory(workspace);
        action.setArguments(args == null ? new ProtocolMessage.Arguments() : args);
        message.setAction(action);
        return transport.requestAction(device.getId(), message).get(180, TimeUnit.SECONDS);
    }

    private DesktopDeviceEntity requireOnlineDevice(Long userId, Long deviceId) {
        DesktopDeviceEntity device;
        if (deviceId == null) {
            device = devices.findAll().stream()
                    .filter(d -> userId.equals(d.getUserId()))
                    .filter(d -> d.getStatus() == DeviceStatus.ONLINE || d.getStatus() == DeviceStatus.BUSY)
                    .filter(d -> transport.isConnected(d.getId()))
                    .filter(d -> d.getRevokedAt() == null)
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "deviceId is required (or pair an ONLINE desktop agent). "
                                    + "OpenCode executes on the user desktop via WebSocket, not Spring localhost."));
        } else {
            device = devices.findByIdAndUserId(deviceId, userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found for user"));
        }
        if (device.getRevokedAt() != null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Device credential revoked");
        }
        if (!transport.isConnected(device.getId())) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Device is not connected — start the Desktop Agent and ensure WSS pairing");
        }
        if (device.getAllowedWorkspace() == null || device.getAllowedWorkspace().isBlank()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Device has no allowedWorkspace");
        }
        return device;
    }

    private String bindWorkspace(DesktopDeviceEntity device, String workspacePath) {
        String selected = workspacePath == null || workspacePath.isBlank()
                ? device.getAllowedWorkspace() : workspacePath;
        try {
            selected = WorkspaceGuard.requireInside(device.getAllowedWorkspace(), selected, "OpenCode workspace")
                    .toString();
        } catch (WorkspaceGuard.WorkspaceViolation v) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, v.getMessage());
        }
        // Defense in depth: refuse obvious Core trees even if somehow allowed on device.
        String lower = selected.toLowerCase(Locale.ROOT);
        if (lower.contains("backend-spring") || lower.contains("backend-old")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Refusing OpenCode workspace under Spring Core tree");
        }
        try {
            Path.of(selected);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid workspacePath");
        }
        return selected;
    }

    private void assertJobsAllowed() {
        if (hybrid.state().isStopNewAgentJobs()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "New agent jobs are stopped by admin");
        }
    }

    private AgentEngineSessionEntity require(Long userId, String sessionId) {
        return sessions.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent session not found"));
    }

    private Map<String, Object> view(AgentEngineSessionEntity s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("engine", s.getEngineType().name());
        m.put("status", s.getStatus());
        m.put("deviceId", s.getDeviceId());
        m.put("externalSessionId", s.getExternalSessionId());
        m.put("planId", s.getPlanId());
        m.put("goal", s.getGoal());
        m.put("workspacePath", s.getWorkspacePath());
        m.put("lastError", s.getLastError());
        m.put("executionModel", "DESKTOP_WSS");
        m.put("health", health());
        m.put("createdAt", s.getCreatedAt());
        m.put("updatedAt", s.getUpdatedAt());
        return m;
    }

    private static String truncate(String s) {
        if (s == null) {
            return null;
        }
        return s.length() > 2000 ? s.substring(0, 2000) + "…" : s;
    }
}
