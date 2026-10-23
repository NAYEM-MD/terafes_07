package com.tronai.agent;

import com.tronai.agent.model.AgentAction;
import com.tronai.agent.model.AgentDecision;
import com.tronai.agent.model.AgentState;
import com.tronai.agent.planner.AgentPlanner;
import com.tronai.ai.AiProvider;
import com.tronai.ai.ChatGenerationRequest;
import com.tronai.ai.ChatGenerationResult;
import com.tronai.ai.model.ModelManager;
import com.tronai.ai.model.ModelProfile;
import com.tronai.ai.model.ModelProviderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class AgentPlannerTest {

    private ModelManager modelManager;
    private AiProvider mockProvider;
    private AgentPlanner planner;

    @BeforeEach
    void setUp() {
        modelManager = Mockito.mock(ModelManager.class);
        mockProvider = Mockito.mock(AiProvider.class);

        ModelProfile profile = ModelProfile.builder()
                .id("qwen3-8b")
                .provider(ModelProviderType.OLLAMA)
                .providerModelName("qwen3:8b")
                .build();

        when(modelManager.getDefaultModel()).thenReturn(profile);
        when(modelManager.getProvider(ModelProviderType.OLLAMA)).thenReturn(mockProvider);

        planner = new AgentPlanner(modelManager);
    }

    @Test
    @DisplayName("Planner successfully parses valid JSON response from model")
    void testPlannerValidStructuredOutput() {
        String jsonReply = """
                ```json
                {
                  "action": "JOB_ANALYSIS",
                  "summary": "Extract target backend requirements for LINE Yahoo."
                }
                ```
                """;

        when(mockProvider.generate(any(ChatGenerationRequest.class)))
                .thenReturn(ChatGenerationResult.builder().text(jsonReply).build());

        AgentState state = AgentState.builder()
                .runId("run-1")
                .goal("Prepare for LINE Yahoo Backend interview")
                .companyName("LINE Yahoo")
                .jobPosition("Backend Engineer")
                .jobDescription("Java, Spring Boot, MySQL, Cloud")
                .build();

        AgentDecision decision = planner.plan(state, 1, 8);

        assertThat(decision).isNotNull();
        assertThat(decision.getAction()).isEqualTo(AgentAction.JOB_ANALYSIS);
        assertThat(decision.getSummary()).contains("Extract target backend requirements");
    }

    @Test
    @DisplayName("Planner gracefully falls back to deterministic sequence on malformed JSON")
    void testPlannerMalformedJsonFallback() {
        when(mockProvider.generate(any(ChatGenerationRequest.class)))
                .thenReturn(ChatGenerationResult.builder().text("This is invalid non-json reasoning text").build());

        AgentState state = AgentState.builder()
                .runId("run-2")
                .goal("Prepare for Backend interview")
                .companyName("LINE Yahoo")
                .jobPosition("Backend Engineer")
                .jobDescription("Spring Boot")
                .build();

        // Without CV, fallback should be JOB_ANALYSIS
        AgentDecision decision = planner.plan(state, 1, 8);
        assertThat(decision).isNotNull();
        assertThat(decision.getAction()).isEqualTo(AgentAction.JOB_ANALYSIS);

        // Once jobAnalysis is gathered, fallback should advance to GAP_ANALYSIS
        state.addObservation("jobAnalysis", Map.of("requiredSkills", "Java"));
        AgentDecision nextDecision = planner.plan(state, 2, 8);
        assertThat(nextDecision.getAction()).isEqualTo(AgentAction.GAP_ANALYSIS);
    }

    @Test
    @DisplayName("Planner with attached CV prioritizes CV_ANALYSIS first in fallback")
    void testPlannerFallbackWithCv() {
        AgentState state = AgentState.builder()
                .runId("run-3")
                .cvId(101L)
                .goal("Prepare for interview")
                .companyName("LINE Yahoo")
                .jobPosition("Backend Engineer")
                .jobDescription("Spring Boot")
                .build();

        AgentDecision fallback = planner.fallbackPlan(state);
        assertThat(fallback.getAction()).isEqualTo(AgentAction.CV_ANALYSIS);

        // After CV analysis is done
        state.addObservation("candidateProfile", Map.of("skills", "Java"));
        AgentDecision nextFallback = planner.fallbackPlan(state);
        assertThat(nextFallback.getAction()).isEqualTo(AgentAction.JOB_ANALYSIS);
    }
}
