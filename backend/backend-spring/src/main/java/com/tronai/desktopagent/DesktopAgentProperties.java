package com.tronai.desktopagent;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Control-plane limits for the desktop agent. Values are not secrets.
 */
@ConfigurationProperties(prefix = "desktop-agent")
public class DesktopAgentProperties {

    private String protocolVersion = "1.0";
    private int pairingTtlSeconds = 600;
    private int heartbeatTimeoutSeconds = 45;
    private int maxActionsPerTask = 80;
    private int actionTimeoutSeconds = 180;
    private int approvalTtlSeconds = 300;
    private int maxFileBytes = 1_048_576;
    /** Max tokens for planner JSON replies (must fit multi-file write plans). */
    private int plannerMaxTokens = 1200;
    /** Identical successful fingerprints before no-progress stop. */
    private int maxRepeatedSuccessFingerprint = 3;

    public String getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(String protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public int getPairingTtlSeconds() {
        return pairingTtlSeconds;
    }

    public void setPairingTtlSeconds(int pairingTtlSeconds) {
        this.pairingTtlSeconds = pairingTtlSeconds;
    }

    public int getHeartbeatTimeoutSeconds() {
        return heartbeatTimeoutSeconds;
    }

    public void setHeartbeatTimeoutSeconds(int heartbeatTimeoutSeconds) {
        this.heartbeatTimeoutSeconds = heartbeatTimeoutSeconds;
    }

    public int getMaxActionsPerTask() {
        return maxActionsPerTask;
    }

    public void setMaxActionsPerTask(int maxActionsPerTask) {
        this.maxActionsPerTask = maxActionsPerTask;
    }

    public int getActionTimeoutSeconds() {
        return actionTimeoutSeconds;
    }

    public void setActionTimeoutSeconds(int actionTimeoutSeconds) {
        this.actionTimeoutSeconds = actionTimeoutSeconds;
    }

    public int getApprovalTtlSeconds() {
        return approvalTtlSeconds;
    }

    public void setApprovalTtlSeconds(int approvalTtlSeconds) {
        this.approvalTtlSeconds = approvalTtlSeconds;
    }

    public int getMaxFileBytes() {
        return maxFileBytes;
    }

    public void setMaxFileBytes(int maxFileBytes) {
        this.maxFileBytes = maxFileBytes;
    }

    public int getPlannerMaxTokens() {
        return plannerMaxTokens;
    }

    public void setPlannerMaxTokens(int plannerMaxTokens) {
        this.plannerMaxTokens = plannerMaxTokens;
    }

    public int getMaxRepeatedSuccessFingerprint() {
        return maxRepeatedSuccessFingerprint;
    }

    public void setMaxRepeatedSuccessFingerprint(int maxRepeatedSuccessFingerprint) {
        this.maxRepeatedSuccessFingerprint = maxRepeatedSuccessFingerprint;
    }
}
