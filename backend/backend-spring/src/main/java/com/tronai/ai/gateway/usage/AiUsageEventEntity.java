package com.tronai.ai.gateway.usage;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "ai_usage_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiUsageEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "feature", nullable = false, length = 64)
    private String feature;

    @Column(name = "mode", nullable = false, length = 32)
    private String mode;

    @Column(name = "provider", nullable = false, length = 64)
    private String provider;

    @Column(name = "model_id", nullable = false, length = 128)
    private String modelId;

    @Column(name = "request_id", nullable = false, length = 64, unique = true)
    private String requestId;

    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Column(name = "prompt_tokens", nullable = false)
    @Builder.Default
    private int promptTokens = 0;

    @Column(name = "completion_tokens", nullable = false)
    @Builder.Default
    private int completionTokens = 0;

    @Column(name = "latency_ms", nullable = false)
    @Builder.Default
    private int latencyMs = 0;

    @Column(name = "ultra_credits", nullable = false)
    @Builder.Default
    private int ultraCredits = 0;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "error_category", length = 64)
    private String errorCategory;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
