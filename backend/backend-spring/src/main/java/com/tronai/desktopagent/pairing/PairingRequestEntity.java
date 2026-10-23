package com.tronai.desktopagent.pairing;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;

import java.time.Instant;

@Entity
@Table(name = "desktop_pairing_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PairingRequestEntity implements Persistable<String> {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(name = "poll_secret_hash", nullable = false, length = 64)
    private String pollSecretHash;

    @Column(name = "device_uuid", nullable = false, length = 36)
    private String deviceUuid;

    @Column(name = "public_key", nullable = false, columnDefinition = "TEXT")
    private String publicKey;

    @Column(name = "fingerprint", nullable = false, length = 128)
    private String fingerprint;

    @Column(name = "os_name", length = 100)
    private String osName;

    @Column(name = "hostname")
    private String hostname;

    @Column(name = "agent_version", length = 50)
    private String agentVersion;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "credential_delivered_at")
    private Instant credentialDeliveredAt;

    @Column(name = "credential_envelope", columnDefinition = "TEXT")
    private String credentialEnvelope;

    @Column(name = "approval_key_envelope", columnDefinition = "TEXT")
    private String approvalKeyEnvelope;

    @Column(name = "device_id")
    private Long deviceId;

    @Column(name = "failed_attempts", nullable = false)
    @Builder.Default
    private int failedAttempts = 0;

    @Column(name = "requested_workspace", length = 1024)
    private String requestedWorkspace;

    @Column(name = "status", length = 30)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "conflict_code", length = 64)
    private String conflictCode;

    @Column(name = "public_request_id", length = 36)
    private String publicRequestId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
