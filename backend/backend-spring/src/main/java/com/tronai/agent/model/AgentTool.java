package com.tronai.agent.model;

import java.util.Set;

/**
 * Common contract for all safe allowlisted agent tools.
 */
public interface AgentTool {

    /**
     * Unique identifier matching an allowlisted AgentAction name.
     */
    String name();

    /**
     * Human-readable description of tool responsibility.
     */
    String description();

    /**
     * Set of required observation keys in state needed for tool execution.
     */
    Set<String> requiredInputs();

    /**
     * Executes the tool within the provided context and returns an AgentToolResult.
     */
    AgentToolResult execute(AgentToolContext context);
}
