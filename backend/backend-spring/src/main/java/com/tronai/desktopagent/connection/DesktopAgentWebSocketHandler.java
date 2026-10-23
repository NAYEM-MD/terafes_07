package com.tronai.desktopagent.connection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tronai.desktopagent.approval.ApprovalService;
import com.tronai.desktopagent.device.DeviceService;
import com.tronai.desktopagent.events.AgentEventBus;
import com.tronai.desktopagent.policy.SecretRedactor;
import com.tronai.desktopagent.protocol.MessageType;
import com.tronai.desktopagent.protocol.ProtocolMessage;
import com.tronai.desktopagent.task.AgentFileChangeEntity;
import com.tronai.desktopagent.task.AgentFileChangeRepository;
import com.tronai.desktopagent.task.TaskApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class DesktopAgentWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(DesktopAgentWebSocketHandler.class);

    private final ObjectMapper objectMapper;
    private final DeviceSessionRegistry sessions;
    private final DeviceService devices;
    private final ApprovalService approvals;
    private final AgentEventBus events;
    private final AgentFileChangeRepository fileChanges;
    private final TaskApplicationService tasks;

    public DesktopAgentWebSocketHandler(ObjectMapper objectMapper,
                                        DeviceSessionRegistry sessions,
                                        DeviceService devices,
                                        ApprovalService approvals,
                                        AgentEventBus events,
                                        AgentFileChangeRepository fileChanges,
                                        @Lazy TaskApplicationService tasks) {
        this.objectMapper = objectMapper;
        this.sessions = sessions;
        this.devices = devices;
        this.approvals = approvals;
        this.events = events;
        this.fileChanges = fileChanges;
        this.tasks = tasks;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long deviceId = (Long) session.getAttributes().get("deviceDbId");
        String deviceUuid = (String) session.getAttributes().get("deviceUuid");
        Long userId = (Long) session.getAttributes().get("userId");
        sessions.register(deviceId, deviceUuid, userId, session);
        devices.markOnline(deviceId);
        log.info("Desktop agent socket open for device {}", deviceUuid);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long deviceId = (Long) session.getAttributes().get("deviceDbId");
        String deviceUuid = (String) session.getAttributes().get("deviceUuid");
        ProtocolMessage incoming = objectMapper.readValue(message.getPayload(), ProtocolMessage.class);
        MessageType type;
        try {
            type = MessageType.valueOf(incoming.getType());
        } catch (Exception e) {
            return;
        }
        DeviceSessionRegistry.Session state = sessions.require(deviceId);
        if (!state.helloSeen() && type != MessageType.DEVICE_HELLO) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }
        switch (type) {
            case DEVICE_HELLO -> onHello(deviceId, deviceUuid, incoming);
            case DEVICE_HEARTBEAT -> devices.markSeen(deviceId,
                    incoming.getHello() == null ? null : incoming.getHello().getOs(),
                    incoming.getHello() == null ? null : incoming.getHello().getHostname(),
                    incoming.getHello() == null ? null : incoming.getHello().getAgentVersion(),
                    incoming.getHello() == null ? null : incoming.getHello().getAllowedWorkspace());
            case ACTION_RESULT -> {
                if (incoming.getResult() != null) {
                    redactResult(incoming.getResult());
                    sessions.completeAction(deviceId, incoming.getActionId(), incoming.getResult());
                }
            }
            case TERMINAL_OUTPUT -> onTerminal(deviceId, state.userId(), incoming);
            case FILE_CHANGED -> onFileChanged(state.userId(), incoming);
            case BROWSER_EVENT -> events.publish(state.userId(), incoming.getTaskId(), "BROWSER_EVENT", browserPayload(incoming));
            case APPROVAL_GRANTED, APPROVAL_DENIED -> onApproval(deviceId, incoming, type == MessageType.APPROVAL_GRANTED);
            case TASK_PAUSE -> {
                if (incoming.getTaskId() != null) {
                    tasks.pauseFromDevice(deviceId, state.userId(), incoming.getTaskId());
                }
            }
            case TASK_CANCEL -> {
                if (incoming.getTaskId() != null) {
                    tasks.stopFromDevice(deviceId, state.userId(), incoming.getTaskId());
                }
            }
            case TAKE_CONTROL -> {
                if (incoming.getTaskId() != null) {
                    tasks.takeControlFromDevice(deviceId, state.userId(), incoming.getTaskId());
                }
            }
            case TASK_RESUME -> {
                if (incoming.getTaskId() != null) {
                    tasks.resumeFromDevice(deviceId, state.userId(), incoming.getTaskId());
                }
            }
            default -> log.debug("Ignoring desktop message {}", type);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long deviceId = (Long) session.getAttributes().get("deviceDbId");
        if (deviceId != null) {
            sessions.remove(deviceId, session);
            devices.markOffline(deviceId);
        }
    }

    private void onHello(Long deviceId, String expectedUuid, ProtocolMessage incoming) {
        ProtocolMessage.Hello hello = incoming.getHello();
        if (hello == null || hello.getDeviceUuid() == null || !hello.getDeviceUuid().equals(expectedUuid)) {
            sessions.close(deviceId);
            return;
        }
        sessions.require(deviceId).markHello();
        devices.markSeen(deviceId, hello.getOs(), hello.getHostname(), hello.getAgentVersion(), hello.getAllowedWorkspace());
        ProtocolMessage welcome = ProtocolMessage.of(MessageType.DEVICE_WELCOME);
        welcome.setDeviceId(expectedUuid);
        ProtocolMessage.Hello ack = new ProtocolMessage.Hello();
        ack.setDeviceUuid(expectedUuid);
        welcome.setHello(ack);
        welcome.setSummary("Connected securely");
        sessions.send(deviceId, welcome);
    }

    private void onTerminal(Long deviceId, Long userId, ProtocolMessage incoming) {
        if (incoming.getTerminal() == null) {
            return;
        }
        String chunk = SecretRedactor.truncate(incoming.getTerminal().getChunk(), 4000);
        sessions.appendOutput(deviceId, incoming.getActionId(), incoming.getTerminal().getStream(), chunk);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("taskId", incoming.getTaskId());
        payload.put("actionId", incoming.getActionId());
        payload.put("stream", incoming.getTerminal().getStream());
        payload.put("chunk", chunk);
        events.publish(userId, incoming.getTaskId(), "TERMINAL_OUTPUT", payload);
    }

    private void onFileChanged(Long userId, ProtocolMessage incoming) {
        if (incoming.getFileChanged() == null || incoming.getTaskId() == null) {
            return;
        }
        ProtocolMessage.FileChanged changed = incoming.getFileChanged();
        AgentFileChangeEntity entity = AgentFileChangeEntity.builder()
                .taskId(incoming.getTaskId())
                .actionId(incoming.getActionId())
                .path(changed.getPath())
                .changeType(changed.getChangeType())
                .sha256(changed.getSha256())
                .sizeBytes(changed.getSizeBytes())
                .build();
        fileChanges.save(entity);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("taskId", incoming.getTaskId());
        payload.put("actionId", incoming.getActionId());
        payload.put("path", changed.getPath());
        payload.put("changeType", changed.getChangeType());
        payload.put("sha256", changed.getSha256());
        payload.put("sizeBytes", changed.getSizeBytes());
        events.publish(userId, incoming.getTaskId(), "FILE_CHANGED", payload);
    }

    private void onApproval(Long deviceId, ProtocolMessage incoming, boolean approved) {
        try {
            String fingerprint = incoming.getApproval() == null ? null : incoming.getApproval().getFingerprint();
            devicesFor(deviceId, incoming.getActionId(), fingerprint, approved);
        } catch (RuntimeException e) {
            log.info("Desktop approval decision rejected for action {}", incoming.getActionId());
        }
    }

    private void devicesFor(Long deviceId, String actionId, String fingerprint, boolean approved) {
        com.tronai.desktopagent.device.DesktopDeviceEntity device = new com.tronai.desktopagent.device.DesktopDeviceEntity();
        device.setId(deviceId);
        approvals.decideFromDevice(device, actionId, fingerprint, approved);
    }

    private static void redactResult(ProtocolMessage.Result result) {
        result.setStdout(SecretRedactor.truncate(result.getStdout(), 64_000));
        result.setStderr(SecretRedactor.truncate(result.getStderr(), 64_000));
        result.setSummary(SecretRedactor.truncate(result.getSummary(), 1000));
        if (result.getScreenshotBase64() != null && result.getScreenshotBase64().length() > 1_500_000) {
            result.setScreenshotBase64(null);
            result.setSummary("Screenshot exceeded the size limit");
        }
    }

    private static Map<String, Object> browserPayload(ProtocolMessage incoming) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("taskId", incoming.getTaskId());
        payload.put("actionId", incoming.getActionId());
        if (incoming.getBrowser() != null) {
            payload.put("event", incoming.getBrowser().getEvent());
            payload.put("url", incoming.getBrowser().getUrl());
            payload.put("title", incoming.getBrowser().getTitle());
            payload.put("detail", SecretRedactor.truncate(incoming.getBrowser().getDetail(), 2000));
            String shot = incoming.getBrowser().getScreenshotBase64();
            boolean screenshot = "SCREENSHOT".equalsIgnoreCase(incoming.getBrowser().getEvent());
            if (screenshot && (shot == null || shot.isBlank())) {
                payload.put("detail", "Screenshot failed");
                payload.put("title", "Screenshot failed");
            } else if (shot != null && shot.length() <= 1_500_000 && shot.startsWith("iVBORw0KGgo")) {
                payload.put("screenshotBase64", shot);
            } else if (screenshot) {
                payload.put("detail", "Screenshot failed");
                payload.put("title", "Screenshot failed");
            }
        }
        return payload;
    }
}
