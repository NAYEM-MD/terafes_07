package com.tronai.agent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.*;

/**
 * Encapsulates the entire state of an Agent execution run.
 * Holds user inputs, accumulated observations, execution trace, and metadata.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentState {
    private String runId;
    private Long userId;
    private String goal;
    private String companyName;
    private String jobPosition;
    private String jobDescription;
    private String language;
    private Long cvId;

    @Builder.Default
    private com.tronai.ai.profile.AiProfile aiProfile = com.tronai.ai.profile.AiProfile.FAST;

    private String modelId;

    @Builder.Default
    private Map<String, Object> observations = new LinkedHashMap<>();

    @Builder.Default
    private List<AgentStepRecord> steps = new ArrayList<>();

    @Builder.Default
    private Set<String> toolsUsed = new LinkedHashSet<>();

    @Builder.Default
    private List<AgentAction> actionHistory = new ArrayList<>();

    private String status;
    private Instant startedAt;
    private Instant completedAt;

    public void addObservation(String key, Object value) {
        observations.put(key, value);
    }

    public Object getObservation(String key) {
        return observations.get(key);
    }

    public boolean hasObservation(String key) {
        return observations.containsKey(key) && observations.get(key) != null;
    }

    public void recordStep(AgentAction action, String description, String status) {
        int nextStep = steps.size() + 1;
        steps.add(AgentStepRecord.builder()
                .stepNumber(nextStep)
                .action(action)
                .description(description)
                .status(status)
                .timestamp(Instant.now())
                .build());
        actionHistory.add(action);
        if (action != AgentAction.FINISH) {
            toolsUsed.add(action.name());
        }
    }
}
