package com.tronai.desktopagent.approval;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;

import java.time.Instant;

@Entity
@Table(name = "agent_approvals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentApprovalEntity implements Persistable<String> {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    @Column(name = "task_id", nullable = false, length = 36)
    private String taskId;

    @Column(name = "action_id", nullable = false, length = 36)
    private String actionId;

    @Column(name = "fingerprint", nullable = false, length = 64)
    private String fingerprint;

    @Column(name = "risk", nullable = false, length = 30)
    private String risk;

    @Column(name = "summary", nullable = false, length = 1000)
    private String summary;

    @Column(name = "command_preview", length = 2000)
    private String commandPreview;

    @Column(name = "path_preview", length = 1024)
    private String pathPreview;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ApprovalStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

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
