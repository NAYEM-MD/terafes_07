package com.tronai.agent.runner;

import com.tronai.agent.dto.AgentDtos;
import com.tronai.agent.model.*;
import com.tronai.agent.planner.AgentPlanner;
import com.tronai.agent.registry.AgentToolRegistry;
import com.tronai.ai.model.ModelManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * AgentRunner executes the bounded autonomous agent loop:
 * GOAL -> PLAN -> CHOOSE TOOL -> EXECUTE TOOL -> OBSERVE RESULT -> DECIDE NEXT ACTION -> REPEAT -> FINISH.
 * 
 * Features:
 * - Bounded execution (MAX_AGENT_STEPS = 8)
 * - Loop / repeated action protection
 * - Deterministic fallback if planner misbehaves
 * - Strict observation recording without hidden chain-of-thought
 */
@Component
public class AgentRunner {

    private static final Logger log = LoggerFactory.getLogger(AgentRunner.class);
    public static final int MAX_AGENT_STEPS = 8;

    private final AgentPlanner planner;
    private final AgentToolRegistry toolRegistry;
    private final ModelManager modelManager;
    private final com.tronai.ai.profile.AiProfileService aiProfileService;

    @org.springframework.beans.factory.annotation.Autowired
    public AgentRunner(AgentPlanner planner, AgentToolRegistry toolRegistry, ModelManager modelManager, com.tronai.ai.profile.AiProfileService aiProfileService) {
        this.planner = planner;
        this.toolRegistry = toolRegistry;
        this.modelManager = modelManager;
        this.aiProfileService = aiProfileService;
    }

    public AgentRunner(AgentPlanner planner, AgentToolRegistry toolRegistry, ModelManager modelManager) {
        this(planner, toolRegistry, modelManager, null);
    }

    /**
     * Runs the multi-step agent loop until FINISH or MAX_AGENT_STEPS reached.
     */
    public AgentState run(AgentState state) {
        state.setStatus("running");
        state.setStartedAt(Instant.now());

        AgentAction previousAction = null;
        int repeatCount = 0;

        for (int step = 1; step <= MAX_AGENT_STEPS; step++) {
            log.info("Agent run {} - Step {}/{}", state.getRunId(), step, MAX_AGENT_STEPS);

            // 1. Planner decides next action
            AgentDecision decision = planner.plan(state, step, MAX_AGENT_STEPS);

            // 2. Loop protection: detect repeated action
            if (decision.getAction() == previousAction && decision.getAction() != AgentAction.FINISH) {
                repeatCount++;
                if (repeatCount >= 1) {
                    log.warn("Repeated action detected: {}. Overriding with deterministic fallback.", decision.getAction());
                    decision = planner.fallbackPlan(state);
                }
            } else {
                repeatCount = 0;
            }
            previousAction = decision.getAction();

            // 3. Handle FINISH action
            if (decision.getAction() == AgentAction.FINISH) {
                log.info("Agent run {} reached FINISH at step {}", state.getRunId(), step);
                state.recordStep(AgentAction.FINISH, decision.getSummary(), "completed");
                break;
            }

            // 4. Resolve safe tool from registry
            AgentTool tool;
            try {
                tool = toolRegistry.resolve(decision.getAction());
            } catch (Exception e) {
                log.error("Failed to resolve tool for action {}: {}", decision.getAction(), e.getMessage());
                // Fallback to deterministic plan
                decision = planner.fallbackPlan(state);
                if (decision.getAction() == AgentAction.FINISH) {
                    state.recordStep(AgentAction.FINISH, decision.getSummary(), "completed");
                    break;
                }
                tool = toolRegistry.resolve(decision.getAction());
            }

            // 5. Execute tool
            AgentToolContext context = AgentToolContext.builder()
                    .state(state)
                    .build();

            AgentToolResult result;
            try {
                result = tool.execute(context);
            } catch (Exception e) {
                log.error("Tool execution failed for action {}: {}", decision.getAction(), e.getMessage(), e);
                result = AgentToolResult.failure("Tool failed: " + e.getMessage());
            }

            // 6. Record safe observation & step
            String stepStatus = "completed";
            if (!result.isSuccess()) {
                stepStatus = "failed";
                log.warn("Tool {} returned failure: {}", tool.name(), result.getErrorMessage());
            } else if ("skipped".equals(result.getStatus())) {
                stepStatus = "skipped";
            }

            if (result.getObservationKey() != null && result.getData() != null) {
                state.addObservation(result.getObservationKey(), result.getData());
            }

            String stepDesc = decision.getSummary();
            if (result.getErrorMessage() != null) {
                stepDesc += " (Notice: " + result.getErrorMessage() + ")";
            }

            state.recordStep(decision.getAction(), stepDesc, stepStatus);

            // If we have collected all 5 key observations, finalize early if appropriate
            if (hasAllEssentialObservations(state)) {
                log.info("Agent run {} has gathered all essential observations. Finalizing.", state.getRunId());
                state.recordStep(AgentAction.FINISH, "All preparation artifacts gathered successfully.", "completed");
                break;
            }
        }

        state.setCompletedAt(Instant.now());
        state.setStatus("completed");
        return state;
    }

    private boolean hasAllEssentialObservations(AgentState state) {
        // Essential: jobAnalysis, gapAnalysis, interviewStrategy, practiceQuestions
        // candidateProfile is essential only if cvId was provided
        boolean cvCheck = state.getCvId() == null || state.hasObservation("candidateProfile");
        return cvCheck &&
                state.hasObservation("jobAnalysis") &&
                state.hasObservation("gapAnalysis") &&
                state.hasObservation("interviewStrategy") &&
                state.hasObservation("practiceQuestions");
    }

    /**
     * Extracts a structured AgentResultDto from the state's accumulated observations.
     */
    @SuppressWarnings("unchecked")
    public AgentDtos.AgentResultDto extractResult(AgentState state) {
        List<String> strengths = new ArrayList<>();
        List<String> gaps = new ArrayList<>();
        List<String> priorityTopics = new ArrayList<>();
        Map<String, Object> strategy = new LinkedHashMap<>();
        List<Map<String, Object>> questions = new ArrayList<>();

        // 1. Strengths & Gaps from GapAnalysis
        Object gapObj = state.getObservation("gapAnalysis");
        if (gapObj instanceof Map<?, ?> gapMap) {
            Object sAreas = gapMap.get("strongAreas");
            if (sAreas instanceof List<?> sList) {
                for (Object item : sList) strengths.add(String.valueOf(item));
            }
            if (strengths.isEmpty()) {
                Object mSkills = gapMap.get("matchingSkills");
                if (mSkills instanceof List<?> mList) {
                    for (Object item : mList) strengths.add(String.valueOf(item));
                }
            }

            Object mGaps = gapMap.get("missingSkills");
            if (mGaps instanceof List<?> gList) {
                for (Object item : gList) gaps.add(String.valueOf(item));
            }
            Object rAreas = gapMap.get("riskAreas");
            if (rAreas instanceof List<?> rList) {
                for (Object item : rList) {
                    String rStr = String.valueOf(item);
                    if (!gaps.contains(rStr)) gaps.add(rStr);
                }
            }
        }

        // If candidateProfile has strengths and strengths list is still empty
        if (strengths.isEmpty()) {
            Object profileObj = state.getObservation("candidateProfile");
            if (profileObj instanceof Map<?, ?> pMap) {
                Object pSkills = pMap.get("skills");
                if (pSkills instanceof List<?> psList) {
                    for (Object item : psList) strengths.add(String.valueOf(item));
                }
            }
        }

        // 2. Priority Topics & Strategy from InterviewStrategy
        Object stratObj = state.getObservation("interviewStrategy");
        if (stratObj instanceof Map<?, ?> sMap) {
            strategy = (Map<String, Object>) sMap;
            Object pTopics = sMap.get("priorityTopics");
            if (pTopics instanceof List<?> ptList) {
                for (Object item : ptList) priorityTopics.add(String.valueOf(item));
            }
        }

        // 3. Practice Questions from QuestionGeneration
        Object qObj = state.getObservation("practiceQuestions");
        if (qObj instanceof List<?> qList) {
            for (Object item : qList) {
                if (item instanceof Map<?, ?> qMap) {
                    questions.add((Map<String, Object>) qMap);
                }
            }
        }

        return AgentDtos.AgentResultDto.builder()
                .candidateStrengths(strengths)
                .skillGaps(gaps)
                .priorityTopics(priorityTopics)
                .interviewStrategy(strategy)
                .practiceQuestions(questions)
                .build();
    }

    /**
     * Extracts metrics for user display.
     */
    public AgentDtos.AgentMetricsDto extractMetrics(AgentState state) {
        long duration = 0;
        if (state.getStartedAt() != null && state.getCompletedAt() != null) {
            duration = Duration.between(state.getStartedAt(), state.getCompletedAt()).toMillis();
        }

        String modelName = state.getModelId();
        if (modelName == null || modelName.isBlank()) {
            try {
                if (aiProfileService != null && state.getAiProfile() != null) {
                    modelName = aiProfileService.resolveModel(state.getAiProfile()).getProviderModelName();
                } else if (modelManager != null) {
                    modelName = modelManager.getDefaultModel().getProviderModelName();
                }
            } catch (Exception ignored) {}
        }
        if (modelName == null || modelName.isBlank()) {
            modelName = "tron-fast";
        }

        return AgentDtos.AgentMetricsDto.builder()
                .stepsExecuted(state.getSteps().size())
                .toolsUsed(new ArrayList<>(state.getToolsUsed()))
                .model(modelName)
                .aiProfile(state.getAiProfile() != null ? state.getAiProfile().name() : "FAST")
                .durationMs(duration)
                .build();
    }
}
