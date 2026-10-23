package com.tronai.agent.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tronai.agent.model.AgentAction;
import com.tronai.agent.model.AgentTool;
import com.tronai.agent.model.AgentToolContext;
import com.tronai.agent.model.AgentToolResult;
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
 * Tool 4: Interview Strategy Tool.
 * Creates tailored preparation roadmaps and tactical advice based on candidate gaps and company expectations.
 */
@Component
public class InterviewStrategyTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(InterviewStrategyTool.class);

    private final ModelManager modelManager;
    private final com.tronai.ai.profile.AiProfileService aiProfileService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @org.springframework.beans.factory.annotation.Autowired
    public InterviewStrategyTool(ModelManager modelManager, com.tronai.ai.profile.AiProfileService aiProfileService) {
        this.modelManager = modelManager;
        this.aiProfileService = aiProfileService;
    }

    public InterviewStrategyTool(ModelManager modelManager) {
        this(modelManager, null);
    }

    @Override
    public String name() {
        return AgentAction.INTERVIEW_STRATEGY.name();
    }

    @Override
    public String description() {
        return "Devises a customized interview preparation strategy addressing candidate weaknesses and company expectations.";
    }

    @Override
    public Set<String> requiredInputs() {
        return Set.of("jobAnalysis", "gapAnalysis");
    }

    @Override
    public AgentToolResult execute(AgentToolContext context) {
        @SuppressWarnings("unchecked")
        Map<String, Object> jobAnalysis = (Map<String, Object>) context.getState().getObservation("jobAnalysis");
        @SuppressWarnings("unchecked")
        Map<String, Object> gapAnalysis = (Map<String, Object>) context.getState().getObservation("gapAnalysis");

        String company = context.getState().getCompanyName();
        String position = context.getState().getJobPosition();
        String language = context.getState().getLanguage();
        com.tronai.ai.profile.AiProfile profile = context.getState().getAiProfile();

        if (jobAnalysis == null || gapAnalysis == null) {
            return AgentToolResult.builder()
                    .success(false)
                    .summary("Cannot formulate strategy: prerequisites (jobAnalysis and gapAnalysis) are missing.")
                    .observationKey("interviewStrategy")
                    .observationData(Collections.emptyMap())
                    .build();
        }

        Map<String, Object> strategyData = null;

        try {
            strategyData = callModelForStrategy(company, position, language, jobAnalysis, gapAnalysis, profile);
        } catch (Exception e) {
            log.warn("InterviewStrategyTool LLM call failed: {}. Using deterministic strategy.", e.getMessage());
            strategyData = deterministicStrategy(company, position, gapAnalysis);
        }

        if (strategyData == null) {
            strategyData = deterministicStrategy(company, position, gapAnalysis);
        }

        @SuppressWarnings("unchecked")
        List<String> priorityTopics = (List<String>) strategyData.getOrDefault("priorityTopics", List.of());

        String summary = String.format("Formulated preparation roadmap focusing on %d high-priority topics for %s.",
                priorityTopics.size(), company);

        return AgentToolResult.builder()
                .success(true)
                .summary(summary)
                .observationKey("interviewStrategy")
                .observationData(strategyData)
                .build();
    }

    private Map<String, Object> callModelForStrategy(String company, String position, String language,
                                                     Map<String, Object> jobAnalysis,
                                                     Map<String, Object> gapAnalysis,
                                                     com.tronai.ai.profile.AiProfile aiProfile) throws Exception {
        ModelProfile modelProfile = (aiProfileService != null && aiProfile != null)
                ? aiProfileService.resolveModel(aiProfile)
                : modelManager.getDefaultModel();
        AiProvider provider = modelManager.getProvider(modelProfile.getProvider());

        String prompt = String.format("""
                You are a senior hiring advisor. Create an actionable interview preparation strategy.
                Target Company: %s
                Position: %s
                Write the advice fields in %s.
                Keep company names, role titles, and skill names unchanged.

                Job Technical Topics: %s
                Candidate Missing Skills (Gaps): %s
                Candidate Matching Skills (Strengths): %s

                Return ONLY a JSON object:
                {
                  "priorityTopics": ["topic1", "topic2"],
                  "technicalPreparation": ["advice1", "advice2"],
                  "behavioralPreparation": ["advice1", "advice2"],
                  "communicationPreparation": ["advice1", "advice2"],
                  "japanesePreparation": "advice on Japanese technical terms if applicable",
                  "studyOrder": ["step 1", "step 2", "step 3"],
                  "likelyQuestionAreas": ["area1", "area2"],
                  "riskMitigation": ["how to address missing skills with learning mindset"]
                }
                """,
                company, position,
                com.tronai.language.ExperienceLanguage.parseOrDefault(language, com.tronai.language.ExperienceLanguage.JA).englishName(),
                jobAnalysis.getOrDefault("technicalTopics", List.of()),
                gapAnalysis.getOrDefault("missingSkills", List.of()),
                gapAnalysis.getOrDefault("matchingSkills", List.of()));

        ChatGenerationRequest req = ChatGenerationRequest.builder()
                .model(modelProfile.getId())
                .messages(List.of(new ChatMessage("user", prompt)))
                .temperature(0.3)
                .jsonMode(true)
                .build();

        ChatGenerationResult result = provider.generate(req);
        String json = result.getText();
        if (json == null || json.isBlank()) throw new IllegalStateException("Empty reply from model");

        JsonNode root = objectMapper.readTree(extractJsonSubstring(json));
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("priorityTopics", extractStringList(root, "priorityTopics"));
        map.put("technicalPreparation", extractStringList(root, "technicalPreparation"));
        map.put("behavioralPreparation", extractStringList(root, "behavioralPreparation"));
        map.put("communicationPreparation", extractStringList(root, "communicationPreparation"));
        map.put("japanesePreparation", root.has("japanesePreparation") ? root.get("japanesePreparation").asText() : "Rehearse self-intro and technical Japanese terms (面接対策)");
        map.put("studyOrder", extractStringList(root, "studyOrder"));
        map.put("likelyQuestionAreas", extractStringList(root, "likelyQuestionAreas"));
        map.put("riskMitigation", extractStringList(root, "riskMitigation"));
        return map;
    }

    private Map<String, Object> deterministicStrategy(String company, String position, Map<String, Object> gapAnalysis) {
        @SuppressWarnings("unchecked")
        List<String> missing = (List<String>) gapAnalysis.getOrDefault("missingSkills", List.of());
        @SuppressWarnings("unchecked")
        List<String> matching = (List<String>) gapAnalysis.getOrDefault("matchingSkills", List.of());

        List<String> priority = new ArrayList<>();
        if (!missing.isEmpty()) {
            priority.addAll(missing.subList(0, Math.min(3, missing.size())));
        }
        if (!matching.isEmpty()) {
            priority.add(matching.get(0) + " Deep-Dive & Architecture");
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("priorityTopics", priority.isEmpty() ? List.of("System Architecture", "RESTful API Design", "Database Indexing") : priority);
        map.put("technicalPreparation", List.of(
                "Review internal mechanics of Spring Boot (IoC, AOP, Transaction Management)",
                "Prepare database schema design and query optimization examples",
                "Understand distributed architectural patterns relevant to " + company
        ));
        map.put("behavioralPreparation", List.of(
                "Prepare STAR-method examples for overcoming technical conflicts in team settings",
                "Articulate motivation for joining " + company + " as a " + position
        ));
        map.put("communicationPreparation", List.of(
                "State conclusions first (PREP method: Point, Reason, Example, Point)",
                "Clarify questions before jumping into long technical explanations"
        ));
        map.put("japanesePreparation", "Practice business Japanese expressions (よろしくお願いいたします, 承知いたしました) and technical terminology in katakana/kanji.");
        map.put("studyOrder", List.of(
                "1. Solidify verified core strengths with project metrics",
                "2. Learn foundational concepts for detected gaps: " + (missing.isEmpty() ? "Cloud / System Design" : String.join(", ", missing)),
                "3. Rehearse mock interview questions aloud"
        ));
        map.put("likelyQuestionAreas", List.of("System Design", "Database Bottleneck Resolution", "Code Review & Team Collaboration"));
        map.put("riskMitigation", List.of("When asked about unfamiliar technologies, explain your transferable learning methodology and eagerness to ramp up quickly."));
        return map;
    }

    private List<String> extractStringList(JsonNode root, String field) {
        List<String> list = new ArrayList<>();
        if (root.has(field) && root.get(field).isArray()) {
            for (JsonNode n : root.get(field)) {
                if (n.isTextual() && !n.asText().isBlank()) {
                    list.add(n.asText().trim());
                }
            }
        }
        return list;
    }

    private String extractJsonSubstring(String text) {
        int firstBrace = text.indexOf('{');
        int lastBrace = text.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return text.substring(firstBrace, lastBrace + 1);
        }
        return text;
    }
}
