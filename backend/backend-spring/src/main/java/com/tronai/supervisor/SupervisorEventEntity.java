package com.tronai.supervisor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "supervisor_events")
@Getter
@Setter
public class SupervisorEventEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "plan_id", nullable = false, length = 36)
    private String planId;

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    @Column(columnDefinition = "LONGTEXT")
    private String detail;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
