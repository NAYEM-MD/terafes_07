package com.tronai.desktopagent.task;

import com.tronai.auth.SecurityUtils;
import com.tronai.desktopagent.events.AgentEventBus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
public class TaskController {

    private final TaskApplicationService tasks;
    private final AgentEventBus events;

    public TaskController(TaskApplicationService tasks, AgentEventBus events) {
        this.tasks = tasks;
        this.events = events;
    }

    @PostMapping("/api/v1/agent-tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@Valid @RequestBody CreateTaskRequest request) {
        return tasks.create(
                SecurityUtils.getRequiredUserId(),
                request.deviceId(),
                request.goal(),
                request.workspace(),
                request.aiProfile());
    }

    @GetMapping("/api/v1/agent-tasks")
    public List<Map<String, Object>> list() {
        return tasks.list(SecurityUtils.getRequiredUserId());
    }

    @GetMapping("/api/v1/agent-tasks/{id}")
    public Map<String, Object> get(@PathVariable String id) {
        return tasks.detail(SecurityUtils.getRequiredUserId(), id);
    }

    @PostMapping("/api/v1/agent-tasks/{id}/messages")
    public Map<String, Object> message(@PathVariable String id, @Valid @RequestBody TaskMessageRequest request) {
        return tasks.postMessage(SecurityUtils.getRequiredUserId(), id, request.message());
    }

    @PostMapping("/api/v1/agent-tasks/{id}/pause")
    public Map<String, Object> pause(@PathVariable String id) {
        return tasks.pause(SecurityUtils.getRequiredUserId(), id);
    }

    @PostMapping("/api/v1/agent-tasks/{id}/resume")
    public Map<String, Object> resume(@PathVariable String id) {
        return tasks.resume(SecurityUtils.getRequiredUserId(), id);
    }

    @PostMapping("/api/v1/agent-tasks/{id}/stop")
    public Map<String, Object> stop(@PathVariable String id) {
        return tasks.stop(SecurityUtils.getRequiredUserId(), id);
    }

    @PostMapping("/api/v1/agent-tasks/{id}/take-control")
    public Map<String, Object> takeControl(@PathVariable String id) {
        return tasks.takeControl(SecurityUtils.getRequiredUserId(), id);
    }

    @PostMapping("/api/v1/agent-tasks/{id}/approvals/{approvalId}/approve")
    public Map<String, Object> approve(@PathVariable String id, @PathVariable String approvalId) {
        return tasks.approve(SecurityUtils.getRequiredUserId(), id, approvalId);
    }

    @PostMapping("/api/v1/agent-tasks/{id}/approvals/{approvalId}/deny")
    public Map<String, Object> deny(@PathVariable String id, @PathVariable String approvalId) {
        return tasks.deny(SecurityUtils.getRequiredUserId(), id, approvalId);
    }

    @GetMapping(path = "/api/v1/agent-tasks/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter taskEvents(@PathVariable String id) {
        tasks.detail(SecurityUtils.getRequiredUserId(), id);
        return events.subscribeTask(id);
    }

    @GetMapping(path = "/api/v1/desktop-agents/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter userEvents() {
        return events.subscribeUser(SecurityUtils.getRequiredUserId());
    }

    public record CreateTaskRequest(
            @NotNull Long deviceId,
            @NotBlank String goal,
            String workspace,
            String aiProfile
    ) {
    }

    public record TaskMessageRequest(@NotBlank String message) {
    }
}
