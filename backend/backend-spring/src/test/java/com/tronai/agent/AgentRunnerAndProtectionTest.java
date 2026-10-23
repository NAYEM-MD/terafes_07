package com.tronai.agent;

import com.tronai.agent.dto.AgentDtos;
import com.tronai.agent.model.*;
import com.tronai.agent.planner.AgentPlanner;
import com.tronai.agent.registry.AgentToolRegistry;
import com.tronai.agent.runner.AgentRunner;
import com.tronai.ai.model.ModelManager;
import com.tronai.ai.model.ModelProfile;
import com.tronai.ai.model.ModelProviderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

class AgentRunnerAndProtectionTest {

    private AgentPlanner mockPlanner;
    private AgentToolRegistry toolRegistry;
    private ModelManager mockModelManager;
    private AgentRunner runner;

    @BeforeEach
    void setUp() {
        mockPlanner = Mockito.mock(AgentPlanner.class);
        mockModelManager = Mockito.mock(ModelManager.class);

        ModelProfile profile = ModelProfile.builder()
                .id("qwen3-8b")
                .provider(ModelProviderType.OLLAMA)
                .providerModelName("qwen3:8b")
                .build();
        when(mockModelManager.getDefaultModel()).thenReturn(profile);

        AgentTool tool1 = new MockTool(AgentAction.JOB_ANALYSIS, "jobAnalysis", Map.of("requiredSkills", List.of("Java")));
        AgentTool tool2 = new MockTool(AgentAction.GAP_ANALYSIS, "gapAnalysis", Map.of("strongAreas", List.of("Java"), "missingSkills", List.of("AWS")));
        AgentTool tool3 = new MockTool(AgentAction.INTERVIEW_STRATEGY, "interviewStrategy", Map.of("priorityTopics", List.of("Cloud Architecture")));
        AgentTool tool4 = new MockTool(AgentAction.QUESTION_GENERATION, "practiceQuestions", List.of(Map.of("question", "Explain Spring Boot")));

        toolRegistry = new AgentToolRegistry(List.of(tool1, tool2, tool3, tool4));
        runner = new AgentRunner(mockPlanner, toolRegistry, mockModelManager);
    }

    @Test
    @DisplayName("Runner completes bounded multi-step flow when planner decides FINISH")
    void testRunnerSuccessfulMultiStep() {
        when(mockPlanner.plan(any(AgentState.class), anyInt(), anyInt()))
                .thenReturn(AgentDecision.builder().action(AgentAction.JOB_ANALYSIS).summary("Step 1: Analyze job").build())
                .thenReturn(AgentDecision.builder().action(AgentAction.GAP_ANALYSIS).summary("Step 2: Compare skills").build())
                .thenReturn(AgentDecision.builder().action(AgentAction.INTERVIEW_STRATEGY).summary("Step 3: Strategy").build())
                .thenReturn(AgentDecision.builder().action(AgentAction.QUESTION_GENERATION).summary("Step 4: Questions").build())
                .thenReturn(AgentDecision.builder().action(AgentAction.FINISH).summary("Step 5: Finish").build());

        AgentState state = AgentState.builder()
                .runId("run-runner-1")
                .goal("Prepare for LINE Yahoo")
                .companyName("LINE Yahoo")
                .jobPosition("Backend Engineer")
                .jobDescription("Java, Spring")
                .build();

        AgentState finished = runner.run(state);

        assertThat(finished.getStatus()).isEqualTo("completed");
        assertThat(finished.getSteps().size()).isGreaterThanOrEqualTo(4);
        assertThat(finished.getToolsUsed().size()).isGreaterThanOrEqualTo(3);

        AgentDtos.AgentResultDto result = runner.extractResult(finished);
        assertThat(result.getCandidateStrengths()).contains("Java");
        assertThat(result.getSkillGaps()).contains("AWS");
        assertThat(result.getPriorityTopics()).contains("Cloud Architecture");
        assertThat(result.getPracticeQuestions()).isNotEmpty();

        // Ensure no chain-of-thought is leaked into step descriptions
        for (AgentStepRecord step : finished.getSteps()) {
            assertThat(step.getDescription()).doesNotContain("chain-of-thought");
            assertThat(step.getDescription()).doesNotContain("thinking");
        }
    }

    @Test
    @DisplayName("Runner prevents infinite loop when planner repeatedly outputs the same action")
    void testRunnerLoopProtection() {
        // Planner keeps returning JOB_ANALYSIS repeatedly
        when(mockPlanner.plan(any(AgentState.class), anyInt(), anyInt()))
                .thenReturn(AgentDecision.builder().action(AgentAction.JOB_ANALYSIS).summary("Job analysis 1").build())
                .thenReturn(AgentDecision.builder().action(AgentAction.JOB_ANALYSIS).summary("Job analysis 2").build());

        when(mockPlanner.fallbackPlan(any(AgentState.class)))
                .thenReturn(AgentDecision.builder().action(AgentAction.GAP_ANALYSIS).summary("Fallback gap analysis").build())
                .thenReturn(AgentDecision.builder().action(AgentAction.FINISH).summary("Fallback finish").build());

        AgentState state = AgentState.builder()
                .runId("run-loop-protect")
                .goal("Prepare for interview")
                .companyName("Company")
                .jobPosition("Position")
                .jobDescription("Desc")
                .build();

        AgentState finished = runner.run(state);

        assertThat(finished.getStatus()).isEqualTo("completed");
        assertThat(finished.getSteps().size()).isLessThanOrEqualTo(AgentRunner.MAX_AGENT_STEPS);
    }

    private static class MockTool implements AgentTool {
        private final AgentAction action;
        private final String obsKey;
        private final Object data;

        public MockTool(AgentAction action, String obsKey, Object data) {
            this.action = action;
            this.obsKey = obsKey;
            this.data = data;
        }

        @Override
        public String name() {
            return action.name();
        }

        @Override
        public String description() {
            return "Mock " + action.name();
        }

        @Override
        public Set<String> requiredInputs() {
            return Set.of();
        }

        @Override
        public AgentToolResult execute(AgentToolContext context) {
            return AgentToolResult.success("Executed " + action.name(), obsKey, data);
        }
    }
}
