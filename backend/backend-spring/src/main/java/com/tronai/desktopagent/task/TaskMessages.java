package com.tronai.desktopagent.task;

import com.tronai.desktopagent.events.AgentEventBus;
import com.tronai.desktopagent.policy.SecretRedactor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Stores and streams safe task conversation text. Hidden reasoning is never accepted.
 */
@Service
public class TaskMessages {

    private final AgentTaskMessageRepository messages;
    private final AgentEventBus events;

    public TaskMessages(AgentTaskMessageRepository messages, AgentEventBus events) {
        this.messages = messages;
        this.events = events;
    }

    public AgentTaskMessageEntity append(AgentTaskEntity task, MessageRole role, String content, String status) {
        AgentTaskMessageEntity message = AgentTaskMessageEntity.builder()
                .id(UUID.randomUUID().toString())
                .taskId(task.getId())
                .userId(task.getUserId())
                .role(role)
                .content(safe(content))
                .status(status)
                .build();
        return messages.save(message);
    }

    public void streamAgent(AgentTaskEntity task, String content) {
        String safe = safe(content);
        if (safe.isBlank()) {
            return;
        }
        // Deduplicate consecutive identical assistant completions for the same task.
        List<AgentTaskMessageEntity> existing = messages.findByTaskIdOrderByCreatedAtAsc(task.getId());
        if (!existing.isEmpty()) {
            AgentTaskMessageEntity last = existing.get(existing.size() - 1);
            if (last.getRole() == MessageRole.AGENT
                    && "COMPLETED".equals(last.getStatus())
                    && safe.equals(last.getContent())) {
                Map<String, Object> completed = base(task, last);
                completed.put("content", safe);
                completed.put("deduplicated", true);
                events.publish(task.getUserId(), task.getId(), "AGENT_MESSAGE_COMPLETED", completed);
                return;
            }
        }
        AgentTaskMessageEntity message = append(task, MessageRole.AGENT, safe, "STREAMING");
        Map<String, Object> started = base(task, message);
        started.put("role", MessageRole.AGENT.name());
        events.publish(task.getUserId(), task.getId(), "AGENT_MESSAGE_STARTED", started);
        int cursor = 0;
        while (cursor < safe.length()) {
            int end = Math.min(safe.length(), cursor + 80);
            Map<String, Object> delta = base(task, message);
            delta.put("delta", safe.substring(cursor, end));
            events.publish(task.getUserId(), task.getId(), "AGENT_MESSAGE_DELTA", delta);
            cursor = end;
        }
        message.setStatus("COMPLETED");
        messages.save(message);
        Map<String, Object> completed = base(task, message);
        completed.put("content", safe);
        events.publish(task.getUserId(), task.getId(), "AGENT_MESSAGE_COMPLETED", completed);
    }

    public String latestUserContent(String taskId) {
        List<AgentTaskMessageEntity> all = messages.findByTaskIdOrderByCreatedAtAsc(taskId);
        String latest = null;
        for (AgentTaskMessageEntity message : all) {
            if (message.getRole() == MessageRole.USER) {
                latest = message.getContent();
            }
        }
        return latest;
    }

    public List<AgentTaskMessageEntity> list(String taskId) {
        return messages.findByTaskIdOrderByCreatedAtAsc(taskId);
    }

    private static Map<String, Object> base(AgentTaskEntity task, AgentTaskMessageEntity message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("taskId", task.getId());
        payload.put("messageId", message.getId());
        return payload;
    }

    static String safe(String content) {
        String redacted = SecretRedactor.truncate(content, 4000);
        if (redacted == null) {
            return "";
        }
        String lower = redacted.toLowerCase();
        if (lower.contains("chain of thought") || lower.contains("hidden reasoning")) {
            return "Progress update";
        }
        return redacted;
    }
}
