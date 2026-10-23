package com.tronai.desktopagent.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tronai.desktopagent.DesktopAgentProperties;
import com.tronai.desktopagent.approval.AgentApprovalEntity;
import com.tronai.desktopagent.approval.ApprovalGate;
import com.tronai.desktopagent.approval.ApprovalService;
import com.tronai.desktopagent.approval.ApprovalSigner;
import com.tronai.desktopagent.connection.WebSocketAgentTransport;
import com.tronai.desktopagent.device.DesktopDeviceEntity;
import com.tronai.desktopagent.device.DesktopDeviceRepository;
import com.tronai.desktopagent.device.DeviceService;
import com.tronai.desktopagent.events.AgentEventBus;
import com.tronai.desktopagent.policy.CommandPolicyEngine;
import com.tronai.desktopagent.policy.Fingerprints;
import com.tronai.desktopagent.policy.PolicyDecision;
import com.tronai.desktopagent.policy.PolicyRequest;
import com.tronai.desktopagent.policy.RiskLevel;
import com.tronai.desktopagent.policy.SecretRedactor;
import com.tronai.desktopagent.protocol.MessageType;
import com.tronai.desktopagent.protocol.ProtocolMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class TaskOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(TaskOrchestrator.class);

    private final ExecutorService executor;
    private final AgentTaskRepository tasks;
    private final AgentTaskStepRepository steps;
    private final AgentActionExecutionRepository executions;
    private final AgentFileChangeRepository fileChanges;
    private final DesktopDeviceRepository devices;
    private final DeviceService deviceService;
    private final DesktopAgentPlanner planner;
    private final WebSocketAgentTransport transport;
    private final ApprovalService approvals;
    private final ApprovalGate approvalGate;
    private final ApprovalSigner approvalSigner;
    private final TaskControl control;
    private final AgentEventBus events;
    private final DesktopAgentProperties properties;
    private final ObjectMapper objectMapper;
    private final TaskMessages conversation;
    private final AgentBrowserSessionRepository browserSessions;
    private final CommandPolicyEngine policy = new CommandPolicyEngine();
    private final ConcurrentHashMap<String, Boolean> running = new ConcurrentHashMap<>();

    public TaskOrchestrator(ExecutorService desktopAgentExecutor,
                            AgentTaskRepository tasks,
                            AgentTaskStepRepository steps,
                            AgentActionExecutionRepository executions,
                            AgentFileChangeRepository fileChanges,
                            DesktopDeviceRepository devices,
                            DeviceService deviceService,
                            DesktopAgentPlanner planner,
                            WebSocketAgentTransport transport,
                            ApprovalService approvals,
                            ApprovalGate approvalGate,
                            ApprovalSigner approvalSigner,
                            TaskControl control,
                            AgentEventBus events,
                            DesktopAgentProperties properties,
                            ObjectMapper objectMapper,
                            TaskMessages conversation,
                            AgentBrowserSessionRepository browserSessions) {
        this.executor = desktopAgentExecutor;
        this.tasks = tasks;
        this.steps = steps;
        this.executions = executions;
        this.fileChanges = fileChanges;
        this.devices = devices;
        this.deviceService = deviceService;
        this.planner = planner;
        this.transport = transport;
        this.approvals = approvals;
        this.approvalGate = approvalGate;
        this.approvalSigner = approvalSigner;
        this.control = control;
        this.events = events;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.conversation = conversation;
        this.browserSessions = browserSessions;
    }

    public boolean isRunning(String taskId) {
        return running.containsKey(taskId);
    }

    public void releaseBrowser(String taskId) {
        AgentTaskEntity task = tasks.findById(taskId).orElse(null);
        if (task == null) {
            return;
        }
        DesktopDeviceEntity device = devices.findById(task.getDeviceId()).orElse(null);
        if (device != null) {
            sendTask(device, task, MessageType.TASK_CANCEL, true);
        }
        closeBrowserSession(taskId);
    }

    public void start(String taskId) {
        if (running.putIfAbsent(taskId, Boolean.TRUE) != null) {
            return;
        }
        executor.submit(() -> {
            try {
                run(taskId);
            } catch (Exception e) {
                log.warn("Desktop task {} failed: {}", taskId, e.getMessage());
                finish(taskId, TaskStatus.FAILED, "Task failed before it could finish safely");
            } finally {
                running.remove(taskId);
            }
        });
    }

    private void run(String taskId) {
        AgentTaskEntity task = tasks.findById(taskId).orElse(null);
        if (task == null) {
            return;
        }
        DesktopDeviceEntity device = devices.findById(task.getDeviceId()).orElse(null);
        if (device == null || !transport.isConnected(device.getId())) {
            finish(taskId, TaskStatus.FAILED, "Device is offline");
            return;
        }
        deviceService.markBusy(device.getId());
        task.setStatus(TaskStatus.RUNNING);
        if (task.getStartedAt() == null) {
            task.setStartedAt(Instant.now());
        }
        tasks.save(task);
        publishStatus(task);
        sendTask(device, task, task.getTurnStepIndex() > 0 ? MessageType.TASK_RESUME : MessageType.TASK_START, false);
        reclaimBrowser(device, task);

            String lastObservation = "";
            String pageText = "";
            boolean lastFailed = false;
            boolean lastDenied = false;
            Map<String, Integer> failures = new HashMap<>();
            Map<String, Integer> successRepeats = new HashMap<>();
            String approvalMac = null;
            String approvalId = null;
            String approvalExpiresAt = null;

        while (true) {
            task = tasks.findById(taskId).orElse(null);
            if (task == null) {
                return;
            }
            if (control.isStopped(taskId) || task.getStatus() == TaskStatus.STOPPED) {
                finish(taskId, TaskStatus.STOPPED, "Task stopped");
                return;
            }
            try {
                if (!control.awaitRunnable(taskId)) {
                    finish(taskId, TaskStatus.STOPPED, "Task stopped");
                    return;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                finish(taskId, TaskStatus.STOPPED, "Task stopped");
                return;
            }
            task = tasks.findById(taskId).orElseThrow();
            if (task.getActionCount() >= properties.getMaxActionsPerTask()) {
                finish(taskId, TaskStatus.FAILED, "Maximum action limit reached");
                return;
            }
            device = devices.findById(task.getDeviceId()).orElseThrow();
            List<AgentTaskStepEntity> allSteps = steps.findByTaskIdOrderByStepIndexAsc(taskId);
            int turn = task.getTurnStepIndex();
            List<DesktopAgentPlanner.TaskContext.StepView> stepViews = allSteps.stream()
                    .filter(step -> step.getStepIndex() >= turn)
                    .map(step -> new DesktopAgentPlanner.TaskContext.StepView(step.getSummary(), step.getTool(), step.getOperation(), step.getStatus()))
                    .toList();
            List<String> history = allSteps.stream()
                    .filter(step -> step.getStepIndex() < turn)
                    .map(AgentTaskStepEntity::getSummary)
                    .toList();
            if (history.size() > 8) {
                history = history.subList(history.size() - 8, history.size());
            }
            AgentBrowserSessionEntity browser = browserSessions.findFirstByTaskIdOrderByCreatedAtDesc(taskId).orElse(null);
            boolean repeated = failures.values().stream().anyMatch(count -> count >= 3);
            DesktopAgentPlanner.TaskContext context = new DesktopAgentPlanner.TaskContext(
                    task.getGoal(), task.getWorkspace(), stepViews, lastObservation, lastFailed, lastDenied,
                    task.isNeedsReinspect(), repeated, task.getActionCount(), properties.getMaxActionsPerTask(),
                    conversation.latestUserContent(taskId), history, changedPaths(taskId), recentResults(taskId),
                    browser == null ? null : browser.getCurrentUrl(), browser == null ? null : browser.getTitle(),
                    browser == null ? null : browser.getState().name(), pageText, task.getStatus().name(),
                    task.getUserId(), task.getAiProfile());
            DesktopAgentPlanner.PlannedAction planned = planner.plan(context);
            if (task.isNeedsReinspect()) {
                task.setNeedsReinspect(false);
                tasks.save(task);
            }
            if (planned.kind() == DesktopAgentPlanner.PlannedAction.Kind.FINISH) {
                conversation.streamAgent(task, planned.summary());
                TaskStatus terminal = AgentGoalVerifier.canAcceptFinish(context)
                        ? TaskStatus.VERIFIED
                        : TaskStatus.COMPLETED;
                finish(taskId, terminal, planned.summary(), !keepBrowser(taskId));
                return;
            }
            if (planned.kind() == DesktopAgentPlanner.PlannedAction.Kind.FAIL) {
                conversation.streamAgent(task, planned.summary());
                finish(taskId, TaskStatus.FAILED, planned.summary(), true);
                return;
            }
            if (planned.kind() == DesktopAgentPlanner.PlannedAction.Kind.WAIT_USER) {
                conversation.streamAgent(task, planned.summary());
                waitForUser(task, planned.summary());
                return;
            }
            if (sensitiveBrowserInput(planned)) {
                conversation.streamAgent(task, "This site requires login. I opened the browser. Please log in manually. Do not type your password into TRON chat.");
                waitForUser(task, "This site requires login. I opened the browser. Please log in manually. Do not type your password into TRON chat.");
                return;
            }
            if (applicationClick(planned)) {
                conversation.streamAgent(task, "Applications are not submitted automatically.");
                finish(taskId, TaskStatus.COMPLETED, "Applications are not submitted automatically.", false);
                return;
            }

            ProtocolMessage.Arguments arguments = planned.arguments() == null ? new ProtocolMessage.Arguments() : planned.arguments();
            String workingDirectory = resolve(task.getWorkspace(), planned.workingDirectory() == null ? task.getWorkspace() : planned.workingDirectory());
            if (arguments.getPath() != null) {
                arguments.setPath(resolve(task.getWorkspace(), arguments.getPath()));
            }
            if (arguments.getDestinationPath() != null) {
                arguments.setDestinationPath(resolve(task.getWorkspace(), arguments.getDestinationPath()));
            }
            String operation = planned.operation();
            if ("BROWSER".equals(planned.tool())) {
                operation = com.tronai.desktopagent.policy.CommandPolicyEngine.normalizeBrowserOperation(operation);
                planned = new DesktopAgentPlanner.PlannedAction(
                        planned.kind(), planned.summary(), planned.tool(), operation,
                        planned.workingDirectory(), arguments);
            }
            PolicyDecision decision = policy.evaluate(new PolicyRequest(
                    planned.tool(), planned.operation(), task.getWorkspace(), workingDirectory,
                    arguments.getPath(), arguments.getDestinationPath(), arguments.getArgv(),
                    arguments.getCommand(), arguments.getUrl()));
            String fingerprint = Fingerprints.of(planned.tool(), planned.operation(), workingDirectory,
                    arguments.getPath(), arguments.getDestinationPath(), arguments.getArgv(),
                    arguments.getContent(), arguments.getUrl());
            String actionId = UUID.randomUUID().toString();
            task.setStatus(TaskStatus.PLANNING);
            tasks.save(task);

            if (decision.risk() == RiskLevel.BLOCKED) {
                addStep(task, planned, "FAILED");
                saveExecution(task, actionId, planned, workingDirectory, arguments, fingerprint, decision, null, "BLOCKED");
                finish(taskId, TaskStatus.FAILED, "Blocked by policy: " + decision.reason());
                return;
            }

            if (decision.risk() == RiskLevel.REQUIRES_APPROVAL) {
                AgentApprovalEntity approval = approvals.createPending(task, actionId, fingerprint, decision.risk(),
                        planned.summary(), arguments.getCommand(), arguments.getPath());
                sendApprovalRequired(device, task, approval);
                boolean approved;
                try {
                    approved = approvalGate.await(approval.getId()).get(properties.getApprovalTtlSeconds() + 5L, TimeUnit.SECONDS);
                } catch (Exception e) {
                    approved = false;
                }
                if (control.isStopped(taskId)) {
                    finish(taskId, TaskStatus.STOPPED, "Task stopped");
                    return;
                }
                if (!approved) {
                    addStep(task, planned, "DENIED");
                    saveExecution(task, actionId, planned, workingDirectory, arguments, fingerprint, decision, null, "DENIED");
                    lastDenied = true;
                    lastFailed = true;
                    lastObservation = "User denied the action";
                    continue;
                }
                try {
                    approvals.consume(approval.getId(), fingerprint);
                } catch (RuntimeException e) {
                    finish(taskId, TaskStatus.FAILED, "Approval could not be used for this action");
                    return;
                }
                approvalMac = approvalSigner.sign(device, task.getId(), actionId, fingerprint, approval.getExpiresAt());
                approvalId = approval.getId();
                approvalExpiresAt = Long.toString(approval.getExpiresAt().toEpochMilli());
            }

            if (!transport.isConnected(device.getId())) {
                finish(taskId, TaskStatus.FAILED, "Device disconnected");
                return;
            }
            task = tasks.findById(taskId).orElseThrow();
            task.setStatus(TaskStatus.RUNNING);
            task.setActionCount(task.getActionCount() + 1);
            tasks.save(task);
            addStep(task, planned, "RUNNING");
            publish(task, "STEP_STARTED", planned.summary(), actionId);
            ProtocolMessage request = actionRequest(device, task, actionId, planned, workingDirectory, arguments,
                    fingerprint, approvalId, approvalMac, approvalExpiresAt);
            approvalMac = null;
            approvalId = null;
            approvalExpiresAt = null;
            ProtocolMessage.Result result;
            try {
                result = transport.requestAction(device.getId(), request)
                        .get(properties.getActionTimeoutSeconds(), TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                transport.cancel(device.getId());
                sendTask(device, task, MessageType.TASK_CANCEL, false);
                result = new ProtocolMessage.Result();
                result.setSuccess(false);
                result.setTimedOut(true);
                result.setSummary("Action timed out");
            } catch (Exception e) {
                result = new ProtocolMessage.Result();
                result.setSuccess(false);
                result.setSummary("Desktop agent did not return a result");
            }
            result.setStdout(SecretRedactor.truncate(result.getStdout(), 64_000));
            result.setStderr(SecretRedactor.truncate(result.getStderr(), 64_000));
            boolean success = result.isSuccess() && !Boolean.TRUE.equals(result.getCancelled());
            if (success && "BROWSER".equals(planned.tool()) && "SCREENSHOT".equals(planned.operation())
                    && !hasRealScreenshot(result)) {
                success = false;
                result.setSuccess(false);
                result.setSummary("Screenshot claimed success without image evidence");
            }
            if (success && "BROWSER".equals(planned.tool()) && "OPEN_URL".equals(planned.operation())) {
                String expected = arguments.getUrl();
                String actual = result.getUrl();
                if (expected != null && !expected.isBlank()
                        && (actual == null || actual.isBlank())) {
                    // Bind the requested URL into session when the agent omitted it, but do not invent navigation.
                    result.setUrl(expected);
                } else if (expected != null && actual != null && !urlsCompatible(expected, actual)) {
                    success = false;
                    result.setSuccess(false);
                    result.setSummary("Opened URL does not match the requested URL");
                }
            }
            String executionStatus = success ? "COMPLETED" : "FAILED";
            saveExecution(task, actionId, planned, workingDirectory, arguments, fingerprint, decision, result, executionStatus);
            rememberFile(task, actionId, planned.operation(), result);
            updateLastStep(taskId, executionStatus);
            publish(task, success ? "STEP_COMPLETED" : "STEP_COMPLETED", result.getSummary() == null ? planned.summary() : result.getSummary(), actionId);
            lastFailed = !success;
            lastDenied = false;
            lastObservation = observation(result);
            if ("GET_TEXT".equals(planned.operation()) || "INSPECT".equals(planned.operation())) {
                pageText = lastObservation;
            }
            if ("BROWSER".equals(planned.tool()) && success) {
                String browserState = "SET_CONTROL".equals(planned.operation()) && planned.arguments() != null
                        && "USER".equalsIgnoreCase(planned.arguments().getBrowserControl())
                        ? BrowserControlState.USER_CONTROLLED.name()
                        : BrowserControlState.AGENT_CONTROLLED.name();
                if ("CLOSE".equals(planned.operation())) {
                    browserState = BrowserControlState.CLOSED.name();
                }
                touchBrowser(task, browserState, result.getUrl(), result.getTitle());
            }
            if (!success) {
                int seen = failures.merge(fingerprint, 1, Integer::sum);
                if (seen >= 3) {
                    finish(taskId, TaskStatus.FAILED, "Repeated failing action stopped");
                    return;
                }
            } else {
                // Success-side no-progress: identical LIST/READ/etc loops must not run forever.
                boolean mutating = "CREATE_FILE".equals(planned.operation())
                        || "WRITE_FILE".equals(planned.operation())
                        || "PATCH_FILE".equals(planned.operation())
                        || "DELETE_FILE".equals(planned.operation())
                        || "MOVE_FILE".equals(planned.operation())
                        || "RUN".equals(planned.operation())
                        || "OPEN_URL".equals(planned.operation())
                        || "CLICK".equals(planned.operation())
                        || "TYPE".equals(planned.operation());
                if (!mutating) {
                    int seen = successRepeats.merge(fingerprint, 1, Integer::sum);
                    int limit = Math.max(2, properties.getMaxRepeatedSuccessFingerprint());
                    if (seen >= limit) {
                        finish(taskId, TaskStatus.FAILED,
                                "No progress: the same observation action repeated without advancing the goal");
                        return;
                    }
                } else {
                    successRepeats.clear();
                }
            }
        }
    }

    private ProtocolMessage actionRequest(DesktopDeviceEntity device, AgentTaskEntity task, String actionId,
                                          DesktopAgentPlanner.PlannedAction planned, String workingDirectory,
                                          ProtocolMessage.Arguments arguments, String fingerprint,
                                          String approvalId, String approvalMac, String approvalExpiresAt) {
        ProtocolMessage message = ProtocolMessage.of(MessageType.ACTION_REQUEST);
        message.setDeviceId(device.getDeviceUuid());
        message.setTaskId(task.getId());
        message.setActionId(actionId);
        message.setSummary(planned.summary());
        ProtocolMessage.Action action = new ProtocolMessage.Action();
        action.setTool(planned.tool());
        action.setOperation(planned.operation());
        action.setWorkingDirectory(workingDirectory);
        action.setArguments(arguments);
        action.setFingerprint(fingerprint);
        action.setApprovalId(approvalId);
        action.setApprovalMac(approvalMac);
        action.setApprovalExpiresAt(approvalExpiresAt);
        message.setAction(action);
        return message;
    }

    private void sendApprovalRequired(DesktopDeviceEntity device, AgentTaskEntity task, AgentApprovalEntity approval) {
        ProtocolMessage message = ProtocolMessage.of(MessageType.APPROVAL_REQUIRED);
        message.setDeviceId(device.getDeviceUuid());
        message.setTaskId(task.getId());
        message.setActionId(approval.getActionId());
        ProtocolMessage.Approval body = new ProtocolMessage.Approval();
        body.setApprovalId(approval.getId());
        body.setFingerprint(approval.getFingerprint());
        body.setRisk(approval.getRisk());
        body.setSummary(approval.getSummary());
        body.setCommandPreview(approval.getCommandPreview());
        body.setPathPreview(approval.getPathPreview());
        body.setExpiresAt(Long.toString(approval.getExpiresAt().toEpochMilli()));
        message.setApproval(body);
        transport.send(device.getId(), message);
    }

    private void sendTask(DesktopDeviceEntity device, AgentTaskEntity task, MessageType type, boolean releaseBrowser) {
        if (!transport.isConnected(device.getId())) {
            return;
        }
        ProtocolMessage message = ProtocolMessage.of(type);
        message.setDeviceId(device.getDeviceUuid());
        message.setTaskId(task.getId());
        ProtocolMessage.TaskSignal signal = new ProtocolMessage.TaskSignal();
        signal.setGoal(task.getGoal());
        signal.setReason(task.getControlMode().name());
        signal.setReleaseBrowser(releaseBrowser);
        message.setTask(signal);
        message.setSummary(task.getSummary());
        transport.send(device.getId(), message);
    }

    public void notifyPaused(String taskId, boolean manual) {
        AgentTaskEntity task = tasks.findById(taskId).orElse(null);
        if (task == null) {
            return;
        }
        DesktopDeviceEntity device = devices.findById(task.getDeviceId()).orElse(null);
        if (device != null) {
            sendTask(device, task, manual ? MessageType.TAKE_CONTROL : MessageType.TASK_PAUSE, manual);
        }
    }

    public void notifyResumed(String taskId) {
        AgentTaskEntity task = tasks.findById(taskId).orElse(null);
        DesktopDeviceEntity device = task == null ? null : devices.findById(task.getDeviceId()).orElse(null);
        if (task != null && device != null) {
            sendTask(device, task, MessageType.TASK_RESUME, false);
        }
    }

    public void notifyStopped(String taskId) {
        approvalGate.cancelTask(taskId);
        AgentTaskEntity task = tasks.findById(taskId).orElse(null);
        if (task == null) {
            return;
        }
        DesktopDeviceEntity device = devices.findById(task.getDeviceId()).orElse(null);
        if (device != null) {
            transport.cancel(device.getId());
            sendTask(device, task, MessageType.TASK_CANCEL, true);
            closeBrowserSession(taskId);
            if (transport.isConnected(device.getId())) {
                deviceService.markOnline(device.getId());
            }
        }
    }

    private void finish(String taskId, TaskStatus status, String summary) {
        finish(taskId, status, summary, true);
    }

    private void finish(String taskId, TaskStatus status, String summary, boolean releaseBrowser) {
        AgentTaskEntity task = tasks.findById(taskId).orElse(null);
        if (task == null) {
            return;
        }
        if (task.getStatus() == TaskStatus.STOPPED && status != TaskStatus.STOPPED) {
            status = TaskStatus.STOPPED;
        }
        task.setStatus(status);
        task.setSummary(SecretRedactor.truncate(summary, 2000));
        task.setFinishedAt(Instant.now());
        tasks.save(task);
        publishStatus(task);
        events.publish(task.getUserId(), task.getId(),
                (status == TaskStatus.COMPLETED || status == TaskStatus.VERIFIED) ? "TASK_COMPLETED"
                        : status == TaskStatus.FAILED ? "TASK_FAILED" : "TASK_STATUS",
                statusPayload(task));
        DesktopDeviceEntity device = devices.findById(task.getDeviceId()).orElse(null);
        if (device != null && transport.isConnected(device.getId())) {
            MessageType type = (status == TaskStatus.COMPLETED || status == TaskStatus.VERIFIED)
                    ? MessageType.TASK_COMPLETED
                    : status == TaskStatus.FAILED ? MessageType.TASK_FAILED : MessageType.TASK_CANCEL;
            sendTask(device, task, type, releaseBrowser);
            if (releaseBrowser) {
                closeBrowserSession(taskId);
            }
            deviceService.markOnline(device.getId());
        }
    }

    private void reclaimBrowser(DesktopDeviceEntity device, AgentTaskEntity task) {
        AgentBrowserSessionEntity session = browserSessions.findFirstByTaskIdOrderByCreatedAtDesc(task.getId()).orElse(null);
        if (session == null || session.getState() == BrowserControlState.CLOSED || session.getState() == BrowserControlState.AGENT_CONTROLLED) {
            return;
        }
        ProtocolMessage.Arguments arguments = new ProtocolMessage.Arguments();
        arguments.setBrowserControl("AGENT");
        DesktopAgentPlanner.PlannedAction planned = new DesktopAgentPlanner.PlannedAction(
                DesktopAgentPlanner.PlannedAction.Kind.ACTION, "Resuming browser control", "BROWSER", "SET_CONTROL",
                task.getWorkspace(), arguments);
        String fingerprint = Fingerprints.of("BROWSER", "SET_CONTROL", task.getWorkspace(), null, null, null, null, null);
        ProtocolMessage request = actionRequest(device, task, UUID.randomUUID().toString(), planned, task.getWorkspace(),
                arguments, fingerprint, null, null, null);
        try {
            transport.requestAction(device.getId(), request).get(20, TimeUnit.SECONDS);
            session.setState(BrowserControlState.AGENT_CONTROLLED);
            session.setLastActivity(Instant.now());
            browserSessions.save(session);
        } catch (Exception e) {
            log.info("Browser control was not returned to the agent for task {}", task.getId());
        }
    }

    private void waitForUser(AgentTaskEntity task, String summary) {
        task.setStatus(TaskStatus.WAITING_USER);
        task.setControlMode(ControlMode.AGENT);
        task.setSummary(SecretRedactor.truncate(summary, 2000));
        task.setFinishedAt(null);
        tasks.save(task);
        touchBrowser(task, BrowserControlState.WAITING_USER.name(), null, null);
        publishStatus(task);
        Map<String, Object> payload = statusPayload(task);
        payload.put("summary", task.getSummary());
        events.publish(task.getUserId(), task.getId(), "WAITING_USER", payload);
        conversation.append(task, MessageRole.SYSTEM_EVENT, task.getSummary(), "COMPLETED");
    }

    private boolean keepBrowser(String taskId) {
        return browserSessions.findFirstByTaskIdOrderByCreatedAtDesc(taskId)
                .filter(session -> session.getState() != BrowserControlState.CLOSED)
                .isPresent();
    }

    private void touchBrowser(AgentTaskEntity task, String state, String url, String title) {
        AgentBrowserSessionEntity session = browserSessions.findFirstByTaskIdOrderByCreatedAtDesc(task.getId())
                .orElseGet(() -> AgentBrowserSessionEntity.builder()
                        .id(UUID.randomUUID().toString())
                        .taskId(task.getId())
                        .deviceId(task.getDeviceId())
                        .userId(task.getUserId())
                        .profileKey("device-" + task.getDeviceId())
                        .state(BrowserControlState.AGENT_CONTROLLED)
                        .build());
        try {
            session.setState(BrowserControlState.valueOf(state));
        } catch (IllegalArgumentException ignored) {
            session.setState(BrowserControlState.AGENT_CONTROLLED);
        }
        if (url != null && !url.isBlank()) {
            session.setCurrentUrl(SecretRedactor.truncate(url, 2000));
        }
        if (title != null && !title.isBlank() && !"Screenshot captured".equals(title)) {
            session.setTitle(trim(title, 500));
        }
        session.setLastActivity(Instant.now());
        browserSessions.save(session);
    }

    private void closeBrowserSession(String taskId) {
        browserSessions.findFirstByTaskIdOrderByCreatedAtDesc(taskId).ifPresent(session -> {
            session.setState(BrowserControlState.CLOSED);
            session.setLastActivity(Instant.now());
            browserSessions.save(session);
        });
    }

    private List<String> changedPaths(String taskId) {
        List<String> paths = fileChanges.findByTaskIdOrderByCreatedAtAsc(taskId).stream()
                .map(AgentFileChangeEntity::getPath)
                .toList();
        if (paths.size() > 12) {
            return paths.subList(paths.size() - 12, paths.size());
        }
        return paths;
    }

    private String recentResults(String taskId) {
        List<AgentActionExecutionEntity> all = executions.findByTaskIdOrderByCreatedAtAsc(taskId);
        int from = Math.max(0, all.size() - 3);
        StringBuilder builder = new StringBuilder();
        for (AgentActionExecutionEntity execution : all.subList(from, all.size())) {
            String text = execution.getStdoutText() == null ? "" : execution.getStdoutText();
            if (text.startsWith("iVBORw0KGgo") || "SCREENSHOT".equals(execution.getOperation())) {
                text = "screenshot captured";
            }
            if (builder.length() > 0) {
                builder.append(" | ");
            }
            builder.append(execution.getOperation()).append(": ").append(SecretRedactor.truncate(text, 180));
        }
        return SecretRedactor.truncate(builder.toString(), 600);
    }

    private static boolean sensitiveBrowserInput(DesktopAgentPlanner.PlannedAction planned) {
        if (!"BROWSER".equals(planned.tool()) || !"TYPE".equals(planned.operation()) || planned.arguments() == null) {
            return false;
        }
        String selector = planned.arguments().getSelector() == null ? "" : planned.arguments().getSelector().toLowerCase();
        return selector.contains("password") || selector.contains("passwd");
    }

    private static boolean applicationClick(DesktopAgentPlanner.PlannedAction planned) {
        if (!"BROWSER".equals(planned.tool()) || !"CLICK".equals(planned.operation())) {
            return false;
        }
        String selector = planned.arguments() == null || planned.arguments().getSelector() == null
                ? "" : planned.arguments().getSelector().toLowerCase();
        String summary = planned.summary() == null ? "" : planned.summary().toLowerCase();
        return selector.contains("apply") || summary.contains("apply");
    }

    private void addStep(AgentTaskEntity task, DesktopAgentPlanner.PlannedAction planned, String status) {
        int index = steps.countByTaskId(task.getId());
        AgentTaskStepEntity step = AgentTaskStepEntity.builder()
                .id(UUID.randomUUID().toString())
                .taskId(task.getId())
                .stepIndex(index)
                .summary(trim(planned.summary(), 1000))
                .tool(planned.tool())
                .operation(planned.operation())
                .status(status)
                .build();
        steps.save(step);
    }

    private void updateLastStep(String taskId, String status) {
        List<AgentTaskStepEntity> all = steps.findByTaskIdOrderByStepIndexAsc(taskId);
        if (all.isEmpty()) {
            return;
        }
        AgentTaskStepEntity last = all.get(all.size() - 1);
        if ("RUNNING".equals(last.getStatus())) {
            last.setStatus(status);
            steps.save(last);
        }
    }

    private void saveExecution(AgentTaskEntity task, String actionId, DesktopAgentPlanner.PlannedAction planned,
                               String workingDirectory, ProtocolMessage.Arguments arguments, String fingerprint,
                               PolicyDecision decision, ProtocolMessage.Result result, String status) {
        String stdout = result == null ? null : result.getStdout();
        if (result != null && result.getScreenshotBase64() != null && result.getScreenshotBase64().length() <= 200_000) {
            stdout = result.getScreenshotBase64();
        }
        AgentActionExecutionEntity execution = AgentActionExecutionEntity.builder()
                .id(UUID.randomUUID().toString())
                .taskId(task.getId())
                .actionId(actionId)
                .tool(planned.tool())
                .operation(planned.operation())
                .fingerprint(fingerprint)
                .risk(decision.risk().name())
                .workingDirectory(workingDirectory)
                .argumentsJson(argumentsJson(arguments))
                .stdoutText(stdout)
                .stderrText(result == null ? null : result.getStderr())
                .exitCode(result == null ? null : result.getExitCode())
                .durationMs(result == null ? null : result.getDurationMs())
                .status(status)
                .build();
        executions.save(execution);
    }

    private void rememberFile(AgentTaskEntity task, String actionId, String operation, ProtocolMessage.Result result) {
        if (result.getPath() == null || operation == null) {
            return;
        }
        if (!List.of("CREATE_FILE", "WRITE_FILE", "PATCH_FILE", "MOVE_FILE", "DELETE_FILE").contains(operation)) {
            return;
        }
        fileChanges.save(AgentFileChangeEntity.builder()
                .taskId(task.getId())
                .actionId(actionId)
                .path(result.getPath())
                .changeType(operation)
                .sha256(result.getSha256())
                .sizeBytes(result.getSizeBytes())
                .build());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("taskId", task.getId());
        payload.put("actionId", actionId);
        payload.put("path", result.getPath());
        payload.put("changeType", operation);
        payload.put("sha256", result.getSha256());
        events.publish(task.getUserId(), task.getId(), "FILE_CHANGED", payload);
    }

    private String argumentsJson(ProtocolMessage.Arguments arguments) {
        try {
            ProtocolMessage.Arguments copy = new ProtocolMessage.Arguments();
            copy.setArgv(arguments.getArgv());
            copy.setCommand(arguments.getCommand());
            copy.setPath(arguments.getPath());
            copy.setDestinationPath(arguments.getDestinationPath());
            copy.setUrl(arguments.getUrl());
            copy.setSelector(arguments.getSelector());
            if (arguments.getContent() != null) {
                copy.setContent(SecretRedactor.truncate(arguments.getContent(), 2000));
            }
            String selector = arguments.getSelector() == null ? "" : arguments.getSelector().toLowerCase();
            if (selector.contains("password") || selector.contains("passwd")) {
                copy.setText("[REDACTED]");
            } else if (arguments.getText() != null) {
                copy.setText(SecretRedactor.truncate(arguments.getText(), 500));
            }
            return objectMapper.writeValueAsString(copy);
        } catch (Exception e) {
            return "{}";
        }
    }

    private void publishStatus(AgentTaskEntity task) {
        events.publish(task.getUserId(), task.getId(), "TASK_STATUS", statusPayload(task));
    }

    private void publish(AgentTaskEntity task, String event, String summary, String actionId) {
        Map<String, Object> payload = statusPayload(task);
        payload.put("summary", SecretRedactor.truncate(summary, 500));
        payload.put("actionId", actionId);
        events.publish(task.getUserId(), task.getId(), event, payload);
    }

    private static Map<String, Object> statusPayload(AgentTaskEntity task) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("taskId", task.getId());
        payload.put("status", task.getStatus().name());
        payload.put("controlMode", task.getControlMode().name());
        payload.put("summary", task.getSummary());
        return payload;
    }

    private static String observation(ProtocolMessage.Result result) {
        String combined = ((result.getStdout() == null ? "" : result.getStdout()) + "\n" + (result.getStderr() == null ? "" : result.getStderr())).trim();
        if (combined.isBlank() && result.getSummary() != null) {
            combined = result.getSummary();
        }
        return SecretRedactor.truncate(combined, 1500);
    }

    private static String resolve(String workspace, String candidate) {
        Path root = Path.of(workspace).toAbsolutePath().normalize();
        if (candidate == null || candidate.isBlank() || ".".equals(candidate)) {
            return root.toString();
        }
        Path path = Path.of(candidate);
        Path resolved = path.isAbsolute() ? path.normalize() : root.resolve(path).normalize();
        return resolved.toString();
    }

    private static String trim(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static boolean hasRealScreenshot(ProtocolMessage.Result result) {
        if (result == null) {
            return false;
        }
        String shot = result.getScreenshotBase64();
        if (shot != null && shot.length() > 64) {
            return true;
        }
        String stdout = result.getStdout();
        return stdout != null && stdout.startsWith("iVBORw0KGgo") && stdout.length() > 64;
    }

    private static boolean urlsCompatible(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        String e = expected.trim().toLowerCase(Locale.ROOT);
        String a = actual.trim().toLowerCase(Locale.ROOT);
        if (e.equals(a) || a.startsWith(e) || e.startsWith(a)) {
            return true;
        }
        try {
            java.net.URI eu = java.net.URI.create(e);
            java.net.URI au = java.net.URI.create(a);
            return Objects.equals(eu.getHost(), au.getHost());
        } catch (Exception ignored) {
            return false;
        }
    }
}
