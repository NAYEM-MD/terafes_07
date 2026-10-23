package com.tronai.agent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Record of an executed step in the agent's visible execution trace.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentStepRecord {
    private int stepNumber;
    private AgentAction action;
    private String description;
    private String status;
    private Instant timestamp;
}
