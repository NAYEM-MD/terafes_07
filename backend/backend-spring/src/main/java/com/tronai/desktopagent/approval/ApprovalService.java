package com.tronai.desktopagent.approval;

import com.tronai.desktopagent.DesktopAgentException;
import com.tronai.desktopagent.DesktopAgentProperties;
import com.tronai.desktopagent.device.DesktopDeviceEntity;
import com.tronai.desktopagent.events.AgentEventBus;
import com.tronai.desktopagent.policy.RiskLevel;
import com.tronai.desktopagent.task.AgentTaskEntity;
import com.tronai.desktopagent.task.AgentTaskRepository;
import com.tronai.desktopagent.task.TaskStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class ApprovalService {

    private final AgentApprovalRepository approvals;
    private final AgentTaskRepository tasks;
    private final ApprovalGate gate;
    private final AgentEventBus events;
    private final DesktopAgentProperties properties;

    public ApprovalService(AgentApprovalRepository approvals,
                           AgentTaskRepository tasks,
                           ApprovalGate gate,
                           AgentEventBus events,
                           DesktopAgentProperties properties) {
        this.approvals = approvals;
        this.tasks = tasks;
        this.gate = gate;
        this.events = events;
        this.properties = properties;
    }

    @Transactional
    public AgentApprovalEntity createPending(AgentTaskEntity task, String actionId, String fingerprint,
                                              RiskLevel risk, String summary, String commandPreview, String pathPreview) {
        AgentApprovalEntity approval = AgentApprovalEntity.builder()
                .id(UUID.randomUUID().toString())
                .userId(task.getUserId())
                .deviceId(task.getDeviceId())
                .taskId(task.getId())
                .actionId(actionId)
                .fingerprint(fingerprint)
                .risk(risk.name())
                .summary(trim(summary, 1000))
                .commandPreview(trim(commandPreview, 2000))
                .pathPreview(trim(pathPreview, 1024))
                .status(ApprovalStatus.PENDING)
                .expiresAt(Instant.now().plusSeconds(properties.getApprovalTtlSeconds()))
                .build();
        approvals.save(approval);
        gate.register(approval.getId(), task.getId());
        task.setStatus(TaskStatus.WAITING_APPROVAL);
        tasks.save(task);
        events.publish(task.getUserId(), task.getId(), "APPROVAL_REQUIRED", toView(approval));
        events.publish(task.getUserId(), task.getId(), "TASK_STATUS", statusPayload(task));
        return approval;
    }

    @Transactional
    public AgentApprovalEntity decide(Long userId, String taskId, String approvalId, boolean approved) {
        AgentApprovalEntity approval = approvals.findByIdAndTaskIdAndUserId(approvalId, taskId, userId)
                .orElseThrow(() -> new DesktopAgentException(HttpStatus.NOT_FOUND, "Approval not found"));
        return applyDecision(approval, approved);
    }

    @Transactional
    public AgentApprovalEntity decideFromDevice(DesktopDeviceEntity device, String actionId, String fingerprint, boolean approved) {
        AgentApprovalEntity approval = approvals.findByActionIdAndStatus(actionId, ApprovalStatus.PENDING)
                .filter(item -> item.getDeviceId().equals(device.getId()))
                .orElseThrow(() -> new DesktopAgentException(HttpStatus.NOT_FOUND, "Approval not found"));
        if (fingerprint != null && !fingerprint.equals(approval.getFingerprint())) {
            throw new DesktopAgentException(HttpStatus.CONFLICT, "Approval does not match this action");
        }
        return applyDecision(approval, approved);
    }

    @Transactional
    public AgentApprovalEntity consume(String approvalId, String fingerprint) {
        AgentApprovalEntity approval = approvals.findById(approvalId)
                .orElseThrow(() -> new DesktopAgentException(HttpStatus.NOT_FOUND, "Approval not found"));
        if (!approval.getFingerprint().equals(fingerprint)) {
            throw new DesktopAgentException(HttpStatus.CONFLICT, "Approval is bound to a different action");
        }
        if (approval.getStatus() != ApprovalStatus.APPROVED) {
            throw new DesktopAgentException(HttpStatus.CONFLICT, "Approval is not active");
        }
        if (approval.getConsumedAt() != null) {
            throw new DesktopAgentException(HttpStatus.CONFLICT, "Approval was already used");
        }
        if (approval.getExpiresAt().isBefore(Instant.now())) {
            approval.setStatus(ApprovalStatus.EXPIRED);
            approvals.save(approval);
            throw new DesktopAgentException(HttpStatus.GONE, "Approval has expired");
        }
        approval.setStatus(ApprovalStatus.CONSUMED);
        approval.setConsumedAt(Instant.now());
        return approvals.save(approval);
    }

    public Map<String, Object> toView(AgentApprovalEntity approval) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", approval.getId());
        view.put("taskId", approval.getTaskId());
        view.put("deviceId", approval.getDeviceId());
        view.put("actionId", approval.getActionId());
        view.put("fingerprint", approval.getFingerprint());
        view.put("risk", approval.getRisk());
        view.put("summary", approval.getSummary());
        view.put("commandPreview", approval.getCommandPreview());
        view.put("pathPreview", approval.getPathPreview());
        view.put("status", approval.getStatus().name());
        view.put("expiresAt", approval.getExpiresAt().toString());
        view.put("decidedAt", approval.getDecidedAt() == null ? null : approval.getDecidedAt().toString());
        return view;
    }

    private AgentApprovalEntity applyDecision(AgentApprovalEntity approval, boolean approved) {
        if (approval.getStatus() != ApprovalStatus.PENDING) {
            throw new DesktopAgentException(HttpStatus.CONFLICT, "Approval is already decided");
        }
        if (approval.getExpiresAt().isBefore(Instant.now())) {
            approval.setStatus(ApprovalStatus.EXPIRED);
            approvals.save(approval);
            gate.complete(approval.getId(), false);
            throw new DesktopAgentException(HttpStatus.GONE, "Approval has expired");
        }
        approval.setStatus(approved ? ApprovalStatus.APPROVED : ApprovalStatus.DENIED);
        approval.setDecidedAt(Instant.now());
        approvals.save(approval);
        tasks.findById(approval.getTaskId()).ifPresent(task -> {
            if (task.getStatus() == TaskStatus.WAITING_APPROVAL) {
                task.setStatus(TaskStatus.RUNNING);
                tasks.save(task);
                events.publish(task.getUserId(), task.getId(), "TASK_STATUS", statusPayload(task));
            }
        });
        events.publish(approval.getUserId(), approval.getTaskId(),
                approved ? "APPROVAL_GRANTED" : "APPROVAL_DENIED", toView(approval));
        signalAfterCommit(approval.getId(), approved);
        return approval;
    }

    private void signalAfterCommit(String approvalId, boolean approved) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    gate.complete(approvalId, approved);
                }
            });
            return;
        }
        gate.complete(approvalId, approved);
    }

    private static Map<String, Object> statusPayload(AgentTaskEntity task) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("taskId", task.getId());
        payload.put("status", task.getStatus().name());
        payload.put("controlMode", task.getControlMode().name());
        payload.put("summary", task.getSummary());
        return payload;
    }

    private static String trim(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
