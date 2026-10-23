package com.tronai.agent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Result of an individual tool execution.
 * Contains safe human-visible summary and structured observation data.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentToolResult {
    private boolean success;
    private String summary;
    private String observationKey;
    private Object observationData;
    @Builder.Default
    private String status = "completed";

    public static AgentToolResult success(String summary, String observationKey, Object observationData) {
        return AgentToolResult.builder()
                .success(true)
                .status("completed")
                .summary(summary)
                .observationKey(observationKey)
                .observationData(observationData)
                .build();
    }

    public static AgentToolResult failure(String errorMessage) {
        return AgentToolResult.builder()
                .success(false)
                .status("failed")
                .summary(errorMessage)
                .build();
    }

    public static AgentToolResult skipped(String reason) {
        return AgentToolResult.builder()
                .success(true)
                .status("skipped")
                .summary(reason)
                .build();
    }

    public Object getData() {
        return observationData;
    }

    public String getErrorMessage() {
        return !success ? summary : null;
    }
}
