package com.tronai.supervisor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "supervisor_milestones")
@Getter
@Setter
public class SupervisorMilestoneEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "plan_id", nullable = false, length = 36)
    private String planId;

    @Column(name = "milestone_index", nullable = false)
    private int milestoneIndex;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, length = 40)
    private String status;

    @Column(name = "acceptance_criteria", columnDefinition = "LONGTEXT")
    private String acceptanceCriteria;

    @Column(name = "result_summary", columnDefinition = "LONGTEXT")
    private String resultSummary;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
