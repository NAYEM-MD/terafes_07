package com.tronai.ai.gateway.usage;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "ai_provider_health")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiProviderHealthEntity {

    @Id
    @Column(name = "provider", length = 64)
    private String provider;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "detail", length = 512)
    private String detail;

    @Column(name = "checked_at", nullable = false)
    private Instant checkedAt;
}
