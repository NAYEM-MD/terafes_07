package com.tronai.desktopagent.approval;

import com.tronai.desktopagent.device.DesktopDeviceEntity;
import com.tronai.desktopagent.policy.ApprovalTokens;
import com.tronai.desktopagent.policy.SecretBox;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class ApprovalSigner {

    private final String jwtSecret;

    public ApprovalSigner(@Value("${jwt.secret}") String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public String sign(DesktopDeviceEntity device, String taskId, String actionId, String fingerprint, Instant expiresAt) {
        if (device.getApprovalKeyEnvelope() == null) {
            throw new IllegalStateException("Device approval key is not available");
        }
        String approvalKey = SecretBox.open(jwtSecret, "approval:" + device.getDeviceUuid(), device.getApprovalKeyEnvelope());
        return ApprovalTokens.mac(approvalKey, device.getDeviceUuid(), taskId, actionId, fingerprint,
                Long.toString(expiresAt.toEpochMilli()));
    }
}
