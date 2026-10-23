package com.tronai.agentengine;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class AgentEngineRegistry {

    private final Map<AgentEngineType, AgentEngine> engines = new EnumMap<>(AgentEngineType.class);

    public AgentEngineRegistry(List<AgentEngine> engineList) {
        for (AgentEngine e : engineList) {
            engines.put(e.type(), e);
        }
    }

    public AgentEngine require(AgentEngineType type) {
        AgentEngine e = engines.get(type);
        if (e == null) {
            throw new IllegalArgumentException("No agent engine registered for " + type);
        }
        return e;
    }

    public Map<String, Object> healthAll() {
        Map<String, Object> all = new java.util.LinkedHashMap<>();
        for (AgentEngine e : engines.values()) {
            all.put(e.type().name(), e.health());
        }
        return all;
    }
}
