package com.tronai.agentengine;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "agent_engine_sessions")
@Getter
@Setter
public class AgentEngineSessionEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Paired desktop device that owns the OpenCode/OpenClaw execution host. */
    @Column(name = "device_id")
    private Long deviceId;

    @Column(name = "plan_id", length = 36)
    private String planId;

    @Enumerated(EnumType.STRING)
    @Column(name = "engine_type", nullable = false, length = 40)
    private AgentEngineType engineType;

    @Column(name = "external_session_id", length = 200)
    private String externalSessionId;

    @Column(nullable = false, length = 40)
    private String status;

    @Column(name = "workspace_path", length = 1000)
    private String workspacePath;

    @Column(columnDefinition = "LONGTEXT")
    private String goal;

    @Column(name = "last_error", columnDefinition = "LONGTEXT")
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
