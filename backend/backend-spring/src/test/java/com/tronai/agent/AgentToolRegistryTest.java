package com.tronai.agent;

import com.tronai.agent.model.AgentAction;
import com.tronai.agent.model.AgentTool;
import com.tronai.agent.registry.AgentToolRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentToolRegistryTest {

    @Test
    @DisplayName("Registry successfully resolves all allowlisted tools and rejects unknown/FINISH actions")
    void testRegistryResolutionAndRejection() {
        AgentTool mockCvTool = new DummyTool(AgentAction.CV_ANALYSIS);
        AgentTool mockJobTool = new DummyTool(AgentAction.JOB_ANALYSIS);
        AgentTool mockGapTool = new DummyTool(AgentAction.GAP_ANALYSIS);

        AgentToolRegistry registry = new AgentToolRegistry(List.of(mockCvTool, mockJobTool, mockGapTool));

        assertThat(registry.contains(AgentAction.CV_ANALYSIS)).isTrue();
        assertThat(registry.contains(AgentAction.JOB_ANALYSIS)).isTrue();
        assertThat(registry.contains(AgentAction.GAP_ANALYSIS)).isTrue();
        assertThat(registry.contains(AgentAction.FINISH)).isFalse();

        assertThat(registry.resolve(AgentAction.CV_ANALYSIS)).isSameAs(mockCvTool);
        assertThat(registry.resolve(AgentAction.JOB_ANALYSIS)).isSameAs(mockJobTool);

        // FINISH has no tool mapping and must be rejected safely
        assertThatThrownBy(() -> registry.resolve(AgentAction.FINISH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not correspond to an executable tool");
    }

    private static class DummyTool implements AgentTool {
        private final AgentAction action;

        public DummyTool(AgentAction action) {
            this.action = action;
        }

        @Override
        public String name() {
            return action.name();
        }

        @Override
        public String description() {
            return "Test tool for " + action.name();
        }

        @Override
        public Set<String> requiredInputs() {
            return Set.of();
        }

        @Override
        public com.tronai.agent.model.AgentToolResult execute(com.tronai.agent.model.AgentToolContext context) {
            return com.tronai.agent.model.AgentToolResult.success("Done", "testKey", "data");
        }
    }
}
