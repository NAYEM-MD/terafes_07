package com.tronai.agentengine;

import java.util.Map;

/**
 * Unified agent execution port. Spring remains the control plane;
 * engines execute under ownership/quota constraints.
 */
public interface AgentEngine {

    AgentEngineType type();

    Map<String, Object> health();

    Map<String, Object> createSession(Long userId, String goal, String workspacePath, String planId);

    /**
     * Create a session bound to a paired desktop device. {@code deviceId} may be null only when
     * the engine can safely pick a single ONLINE device for the user.
     */
    default Map<String, Object> createSession(Long userId, String goal, String workspacePath, String planId, Long deviceId) {
        return createSession(userId, goal, workspacePath, planId);
    }

    Map<String, Object> startTask(Long userId, String sessionId, String prompt);

    Map<String, Object> continueSession(Long userId, String sessionId, String prompt);

    Map<String, Object> status(Long userId, String sessionId);

    Map<String, Object> cancel(Long userId, String sessionId);
}
