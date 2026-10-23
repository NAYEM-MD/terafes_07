package com.tronai.voice;

import com.tronai.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;

import java.time.Instant;

@Entity
@Table(name = "voice_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoiceSessionEntity implements Persistable<String> {

    @Id
    @Column(length = 100, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "ai_profile", length = 32, nullable = false)
    @Builder.Default
    private String aiProfile = "FAST";

    @Column(name = "model_id", length = 100)
    private String modelId;

    @Column(name = "language", length = 8, nullable = false)
    @Builder.Default
    private String language = "ja";

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Transient
    @Builder.Default
    private boolean isNewEntity = true;

    @Override
    public boolean isNew() {
        return isNewEntity;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (startedAt == null) {
            startedAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    @PostLoad
    protected void onPostLoad() {
        this.isNewEntity = false;
    }
}
