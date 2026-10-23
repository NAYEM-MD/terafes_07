package com.tronai.hybrid;

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
@Table(name = "platform_control_state")
@Getter
@Setter
public class PlatformControlEntity {

    @Id
    private Integer id = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PlatformMode mode = PlatformMode.NORMAL;

    @Column(name = "local_routing_enabled", nullable = false)
    private boolean localRoutingEnabled = true;

    @Column(name = "cloud_routing_enabled", nullable = false)
    private boolean cloudRoutingEnabled = true;

    @Column(name = "pause_new_ai_tasks", nullable = false)
    private boolean pauseNewAiTasks = false;

    @Column(name = "stop_new_agent_jobs", nullable = false)
    private boolean stopNewAgentJobs = false;

    @Column(name = "exhibition_registration_open", nullable = false)
    private boolean exhibitionRegistrationOpen = true;

    @Column(name = "max_concurrent_ai_requests", nullable = false)
    private int maxConcurrentAiRequests = 10;

    @Column(name = "queue_capacity", nullable = false)
    private int queueCapacity = 50;

    /** AUTO | FORCE_ONLINE | FORCE_OFFLINE */
    @Column(name = "rtx_override", nullable = false, length = 20)
    private String rtxOverride = "AUTO";

    @Column(name = "gtx_override", nullable = false, length = 20)
    private String gtxOverride = "AUTO";

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "updated_by")
    private Long updatedBy;
}
