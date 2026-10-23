package com.tronai.agent.entity;

import com.tronai.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "agent_runs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentRunEntity implements Persistable<String> {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "goal", columnDefinition = "TEXT", nullable = false)
    private String goal;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "job_position", nullable = false)
    private String jobPosition;

    @Column(name = "job_description", columnDefinition = "LONGTEXT", nullable = false)
    private String jobDescription;

    @Column(name = "language", length = 30, nullable = false)
    @Builder.Default
    private String language = "ja";

    @Column(name = "status", length = 30, nullable = false)
    @Builder.Default
    private String status = "IN_PROGRESS";

    @Column(name = "ai_profile", length = 32, nullable = false)
    @Builder.Default
    private String aiProfile = "FAST";

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "steps_executed", nullable = false)
    @Builder.Default
    private int stepsExecuted = 0;

    @Column(name = "model_id", length = 100, nullable = false)
    @Builder.Default
    private String modelId = "tron-fast";

    @Column(name = "provider_type", length = 16)
    private String providerType;

    @Column(name = "actual_model_id", length = 200)
    private String actualModelId;

    @Column(name = "duration_ms", nullable = false)
    @Builder.Default
    private long durationMs = 0;

    @OneToMany(mappedBy = "run", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("stepNumber ASC")
    @Builder.Default
    private List<AgentStepEntity> steps = new ArrayList<>();

    @OneToOne(mappedBy = "run", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private AgentResultEntity result;

    @PrePersist
    protected void onCreate() {
        if (startedAt == null) {
            startedAt = Instant.now();
        }
    }
}
