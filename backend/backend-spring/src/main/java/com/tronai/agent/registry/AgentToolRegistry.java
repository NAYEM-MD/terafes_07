package com.tronai.agent.registry;

import com.tronai.agent.model.AgentAction;
import com.tronai.agent.model.AgentTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of allowlisted agent tools.
 * Enforces safe lookup and strictly forbids execution of unregistered or arbitrary tools.
 */
@Component
public class AgentToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(AgentToolRegistry.class);

    private final Map<String, AgentTool> tools = new ConcurrentHashMap<>();

    public AgentToolRegistry(List<AgentTool> registeredTools) {
        if (registeredTools != null) {
            for (AgentTool tool : registeredTools) {
                tools.put(tool.name().toUpperCase(), tool);
                log.info("Registered safe agent tool: [{}] - {}", tool.name(), tool.description());
            }
        }
    }

    /**
     * Resolves an AgentTool by its allowlisted action name.
     * Throws IllegalArgumentException if tool is unknown.
     */
    public AgentTool resolve(AgentAction action) {
        if (action == null || action == AgentAction.FINISH) {
            throw new IllegalArgumentException("Action " + action + " does not correspond to an executable tool");
        }
        return resolve(action.name());
    }

    /**
     * Resolves an AgentTool by raw string name.
     */
    public AgentTool resolve(String toolName) {
        if (toolName == null || toolName.isBlank()) {
            throw new IllegalArgumentException("Tool name cannot be empty");
        }
        String normalized = toolName.trim().toUpperCase();
        AgentTool tool = tools.get(normalized);
        if (tool == null) {
            throw new IllegalArgumentException("Unknown or disallowed agent tool: " + toolName);
        }
        return tool;
    }

    /**
     * Checks if a tool is registered.
     */
    public boolean contains(String toolName) {
        return toolName != null && tools.containsKey(toolName.trim().toUpperCase());
    }

    public boolean contains(AgentAction action) {
        return action != null && action != AgentAction.FINISH && contains(action.name());
    }

    /**
     * Lists all registered tool names.
     */
    public Set<String> getRegisteredToolNames() {
        return Collections.unmodifiableSet(tools.keySet());
    }

    /**
     * Returns all registered tools.
     */
    public Collection<AgentTool> getAllTools() {
        return Collections.unmodifiableCollection(tools.values());
    }
}
