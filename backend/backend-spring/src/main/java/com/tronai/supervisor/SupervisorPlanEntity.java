package com.tronai.supervisor;

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
@Table(name = "supervisor_plans")
@Getter
@Setter
public class SupervisorPlanEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Immutable original user goal. */
    @Column(name = "original_goal", nullable = false, columnDefinition = "LONGTEXT")
    private String originalGoal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private SupervisorPlanStatus status;

    @Column(nullable = false, length = 40)
    private String engine;

    @Column(name = "model_route", nullable = false, length = 40)
    private String modelRoute;

    @Column(name = "current_milestone_index", nullable = false)
    private int currentMilestoneIndex;

    @Column(name = "plan_json", columnDefinition = "LONGTEXT")
    private String planJson;

    @Column(name = "verified_result", columnDefinition = "LONGTEXT")
    private String verifiedResult;

    @Column(name = "error_summary", columnDefinition = "LONGTEXT")
    private String errorSummary;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
