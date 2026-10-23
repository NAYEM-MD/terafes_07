package com.tronai.desktopagent.task;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;

import java.time.Instant;

@Entity
@Table(name = "agent_action_executions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentActionExecutionEntity implements Persistable<String> {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Column(name = "task_id", nullable = false, length = 36)
    private String taskId;

    @Column(name = "action_id", nullable = false, length = 36)
    private String actionId;

    @Column(name = "tool", nullable = false, length = 50)
    private String tool;

    @Column(name = "operation", nullable = false, length = 50)
    private String operation;

    @Column(name = "fingerprint", nullable = false, length = 64)
    private String fingerprint;

    @Column(name = "risk", nullable = false, length = 30)
    private String risk;

    @Column(name = "working_directory", length = 1024)
    private String workingDirectory;

    @Column(name = "arguments_json", columnDefinition = "LONGTEXT")
    private String argumentsJson;

    @Column(name = "stdout_text", columnDefinition = "LONGTEXT")
    private String stdoutText;

    @Column(name = "stderr_text", columnDefinition = "LONGTEXT")
    private String stderrText;

    @Column(name = "exit_code")
    private Integer exitCode;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    @PrePersist
    void onCreate() {
        if (id == null || id.isBlank()) {
            id = java.util.UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
