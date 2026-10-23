package com.tronai.desktopagent.device;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "desktop_devices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DesktopDeviceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "device_uuid", nullable = false, unique = true, length = 36)
    private String deviceUuid;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "os_name", length = 100)
    private String osName;

    @Column(name = "hostname")
    private String hostname;

    @Column(name = "agent_version", length = 50)
    private String agentVersion;

    @Column(name = "public_key", columnDefinition = "TEXT")
    private String publicKey;

    @Column(name = "fingerprint", length = 128)
    private String fingerprint;

    @Column(name = "credential_hash", length = 64)
    private String credentialHash;

    @Column(name = "approval_key_envelope", columnDefinition = "TEXT")
    private String approvalKeyEnvelope;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private DeviceStatus status;

    @Column(name = "last_seen")
    private Instant lastSeen;

    @Column(name = "paired_at")
    private Instant pairedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "allowed_workspace", length = 1024)
    private String allowedWorkspace;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
