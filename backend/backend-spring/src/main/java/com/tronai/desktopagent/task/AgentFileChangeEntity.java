package com.tronai.desktopagent.task;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "agent_file_changes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentFileChangeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false, length = 36)
    private String taskId;

    @Column(name = "action_id", length = 36)
    private String actionId;

    @Column(name = "path", nullable = false, length = 1024)
    private String path;

    @Column(name = "change_type", nullable = false, length = 30)
    private String changeType;

    @Column(name = "sha256", length = 64)
    private String sha256;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
