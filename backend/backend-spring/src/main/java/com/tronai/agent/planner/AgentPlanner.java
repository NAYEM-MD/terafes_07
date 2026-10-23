package com.tronai.agent.planner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tronai.agent.model.AgentAction;
import com.tronai.agent.model.AgentDecision;
import com.tronai.agent.model.AgentState;
import com.tronai.ai.AiProvider;
import com.tronai.ai.ChatGenerationRequest;
import com.tronai.ai.ChatGenerationResult;
import com.tronai.ai.ChatMessage;
import com.tronai.ai.model.ModelManager;
import com.tronai.ai.model.ModelProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * AgentPlanner decides the next safe tool action to execute based on the user's goal,
 * current safe observations, completed steps, and remaining steps.
 * Uses ModelManager (qwen3:8b) with strict JSON output schema and deterministic fallback.
 */
@Component
public class AgentPlanner {

    private static final Logger log = LoggerFactory.getLogger(AgentPlanner.class);

    private final ModelManager modelManager;
    private final com.tronai.ai.profile.AiProfileService aiProfileService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @org.springframework.beans.factory.annotation.Autowired
    public AgentPlanner(ModelManager modelManager, com.tronai.ai.profile.AiProfileService aiProfileService) {
        this.modelManager = modelManager;
        this.aiProfileService = aiProfileService;
    }

    public AgentPlanner(ModelManager modelManager) {
        this(modelManager, null);
    }

    /**
     * Determines the next action to execute.
     */
    public AgentDecision plan(AgentState state, int currentStep, int maxSteps) {
        // If remaining steps is 1 (the final step), we must finish
        if (currentStep >= maxSteps) {
            return AgentDecision.builder()
                    .action(AgentAction.FINISH)
                    .summary("Maximum step limit reached; finalizing preparation package.")
                    .build();
        }

        // Try LLM-driven planning
        try {
            AgentDecision decision = planWithLlm(state, currentStep, maxSteps);
            if (decision != null && isValidNextAction(state, decision.getAction())) {
                log.info("AgentPlanner LLM selected action: {} with summary: {}", decision.getAction(), decision.getSummary());
                return decision;
            }
        } catch (Exception e) {
            log.warn("AgentPlanner LLM planning encountered an issue: {}. Falling back to deterministic plan.", e.getMessage());
        }

        // Deterministic fallback plan
        AgentDecision fallback = fallbackPlan(state);
        log.info("AgentPlanner deterministic fallback selected action: {} with summary: {}", fallback.getAction(), fallback.getSummary());
        return fallback;
    }

    private AgentDecision planWithLlm(AgentState state, int currentStep, int maxSteps) {
        ModelProfile profile = (aiProfileService != null && state.getAiProfile() != null)
                ? aiProfileService.resolveModel(state.getAiProfile())
                : modelManager.getDefaultModel();
        AiProvider provider = modelManager.getProvider(profile.getProvider());

        String systemPrompt = """
                You are the TRON Interview Agent Planner.
                Your task is to review the candidate's interview preparation goal and current observations,
                and choose the NEXT SINGLE BEST ACTION from the allowlisted safe actions below.
                
                ALLOWLISTED ACTIONS:
                1. CV_ANALYSIS: Analyze candidate profile, skills, and background from the provided CV.
                2. JOB_ANALYSIS: Extract structured technical and behavioral requirements from the target role and company.
                3. GAP_ANALYSIS: Compare candidate profile against job requirements to identify matching skills and missing gaps.
                4. INTERVIEW_STRATEGY: Synthesize a tailored preparation strategy targeting identified gaps.
                5. QUESTION_GENERATION: Generate targeted practice interview questions based on the strategy and gaps.
                6. FINISH: Finish the agent run when all necessary analysis and preparation artifacts are gathered.
                
                CONSTRAINTS:
                - Output ONLY valid JSON matching this exact structure:
                  {
                    "action": "<ACTION_NAME>",
                    "summary": "<Short, safe, user-visible explanation in 1 sentence>"
                  }
                - Do NOT output any chain-of-thought, reasoning steps, or markdown wrappers.
                - Do NOT select an action that has already completed successfully unless new inputs require re-analysis.
                - Do NOT select GAP_ANALYSIS before JOB_ANALYSIS is done.
                - Do NOT select INTERVIEW_STRATEGY or QUESTION_GENERATION before GAP_ANALYSIS is done.
                """;

        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("Goal: ").append(state.getGoal()).append("\n");
        userPrompt.append("Company: ").append(state.getCompanyName()).append("\n");
        userPrompt.append("Position: ").append(state.getJobPosition()).append("\n");
        userPrompt.append("Language: ").append(state.getLanguage()).append("\n");
        userPrompt.append("CV Attached: ").append(state.getCvId() != null ? "Yes (ID " + state.getCvId() + ")" : "No CV provided").append("\n");
        userPrompt.append("Current Step: ").append(currentStep).append(" of ").append(maxSteps).append("\n\n");

        userPrompt.append("Observations so far:\n");
        if (state.hasObservation("candidateProfile")) {
            userPrompt.append("- candidateProfile: [AVAILABLE]\n");
        } else {
            userPrompt.append("- candidateProfile: [NOT GATHERED]\n");
        }
        if (state.hasObservation("jobAnalysis")) {
            userPrompt.append("- jobAnalysis: [AVAILABLE]\n");
        } else {
            userPrompt.append("- jobAnalysis: [NOT GATHERED]\n");
        }
        if (state.hasObservation("gapAnalysis")) {
            userPrompt.append("- gapAnalysis: [AVAILABLE]\n");
        } else {
            userPrompt.append("- gapAnalysis: [NOT GATHERED]\n");
        }
        if (state.hasObservation("interviewStrategy")) {
            userPrompt.append("- interviewStrategy: [AVAILABLE]\n");
        } else {
            userPrompt.append("- interviewStrategy: [NOT GATHERED]\n");
        }
        if (state.hasObservation("practiceQuestions")) {
            userPrompt.append("- practiceQuestions: [AVAILABLE]\n");
        } else {
            userPrompt.append("- practiceQuestions: [NOT GATHERED]\n");
        }

        userPrompt.append("\nAction History: ").append(state.getActionHistory()).append("\n");
        userPrompt.append("Decide the single next action JSON now:");

        List<ChatMessage> messages = List.of(
                new ChatMessage("system", systemPrompt),
                new ChatMessage("user", userPrompt.toString())
        );

        ChatGenerationRequest request = ChatGenerationRequest.builder()
                .model(profile.getProviderModelName())
                .messages(messages)
                .temperature(0.1)
                .numPredict(250)
                .jsonMode(true)
                .build();

        ChatGenerationResult result = provider.generate(request);
        if (result == null || result.getText() == null || result.getText().isBlank()) {
            return null;
        }

        return parseDecisionJson(result.getText());
    }

    private AgentDecision parseDecisionJson(String text) {
        try {
            String cleaned = text.trim();
            if (cleaned.startsWith("```json")) {
                cleaned = cleaned.substring(7);
            } else if (cleaned.startsWith("```")) {
                cleaned = cleaned.substring(3);
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3);
            }
            cleaned = cleaned.trim();

            int startBrace = cleaned.indexOf('{');
            int endBrace = cleaned.lastIndexOf('}');
            if (startBrace >= 0 && endBrace > startBrace) {
                cleaned = cleaned.substring(startBrace, endBrace + 1);
            }

            JsonNode root = objectMapper.readTree(cleaned);
            if (!root.has("action")) {
                return null;
            }

            String actionStr = root.get("action").asText("").trim().toUpperCase();
            AgentAction action = AgentAction.valueOf(actionStr);
            String summary = root.has("summary") ? root.get("summary").asText("").trim() : "";
            if (summary.isBlank()) {
                summary = "Executing " + action.name() + " based on agent planning decision.";
            }

            return AgentDecision.builder()
                    .action(action)
                    .summary(summary)
                    .build();
        } catch (Exception e) {
            log.warn("Failed to parse decision JSON from model: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Verifies that the proposed action is logically valid and not a premature step.
     */
    private boolean isValidNextAction(AgentState state, AgentAction action) {
        if (action == null) {
            return false;
        }

        // Check if action was already done and not needing repeat
        switch (action) {
            case CV_ANALYSIS:
                return !state.hasObservation("candidateProfile");
            case JOB_ANALYSIS:
                return !state.hasObservation("jobAnalysis");
            case GAP_ANALYSIS:
                // Requires job analysis first
                return state.hasObservation("jobAnalysis") && !state.hasObservation("gapAnalysis");
            case INTERVIEW_STRATEGY:
                // Requires gap analysis first
                return state.hasObservation("gapAnalysis") && !state.hasObservation("interviewStrategy");
            case QUESTION_GENERATION:
                // Requires gap analysis first
                return state.hasObservation("gapAnalysis") && !state.hasObservation("practiceQuestions");
            case FINISH:
                // Finish is valid if at least jobAnalysis and practiceQuestions are done
                return state.hasObservation("jobAnalysis") && state.hasObservation("practiceQuestions");
            default:
                return false;
        }
    }

    /**
     * Deterministic fallback sequence if model fails or outputs invalid action.
     */
    public AgentDecision fallbackPlan(AgentState state) {
        if (state.getCvId() != null && !state.hasObservation("candidateProfile")) {
            return AgentDecision.builder()
                    .action(AgentAction.CV_ANALYSIS)
                    .summary("Analyzing candidate profile and verified competencies from CV.")
                    .build();
        }
        if (!state.hasObservation("jobAnalysis")) {
            return AgentDecision.builder()
                    .action(AgentAction.JOB_ANALYSIS)
                    .summary("Analyzing target job requirements and role expectations.")
                    .build();
        }
        if (!state.hasObservation("gapAnalysis")) {
            return AgentDecision.builder()
                    .action(AgentAction.GAP_ANALYSIS)
                    .summary("Performing skill gap analysis between candidate profile and job requirements.")
                    .build();
        }
        if (!state.hasObservation("interviewStrategy")) {
            return AgentDecision.builder()
                    .action(AgentAction.INTERVIEW_STRATEGY)
                    .summary("Synthesizing strategic preparation plan targeting priority areas.")
                    .build();
        }
        if (!state.hasObservation("practiceQuestions")) {
            return AgentDecision.builder()
                    .action(AgentAction.QUESTION_GENERATION)
                    .summary("Generating contextual practice interview questions addressing identified gaps.")
                    .build();
        }
        return AgentDecision.builder()
                .action(AgentAction.FINISH)
                .summary("All preparation stages complete. Final preparation package assembled.")
                .build();
    }
}
