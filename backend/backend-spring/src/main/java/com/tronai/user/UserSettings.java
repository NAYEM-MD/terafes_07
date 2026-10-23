package com.tronai.user;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * UserSettings entity representing user preferences (language, model, preferred AI profile).
 * Step 2 persistence foundation.
 */
@Entity
@Table(name = "user_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "preferred_language", length = 50)
    @Builder.Default
    private String preferredLanguage = "ja";

    @Column(name = "preferred_model", length = 100)
    @Builder.Default
    private String preferredModel = "qwen3:8b";

    @Column(name = "preferred_ai_profile", length = 32)
    @Builder.Default
    private String preferredAiProfile = "FAST";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (preferredLanguage == null) {
            preferredLanguage = "ja";
        }
        if (preferredModel == null) {
            preferredModel = "qwen3:8b";
        }
        if (preferredAiProfile == null) {
            preferredAiProfile = "FAST";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
