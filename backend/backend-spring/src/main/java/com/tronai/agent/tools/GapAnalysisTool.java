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
 * Tool 3: Gap Analysis Tool.
 * Compares verified candidate profile against extracted job requirements.
 * Identifies matches, gaps, strong areas, and risk areas without fabricating evidence.
 */
@Component
public class GapAnalysisTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(GapAnalysisTool.class);

    private final ModelManager modelManager;
    private final com.tronai.ai.profile.AiProfileService aiProfileService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @org.springframework.beans.factory.annotation.Autowired
    public GapAnalysisTool(ModelManager modelManager, com.tronai.ai.profile.AiProfileService aiProfileService) {
        this.modelManager = modelManager;
        this.aiProfileService = aiProfileService;
    }

    public GapAnalysisTool(ModelManager modelManager) {
        this(modelManager, null);
    }

    @Override
    public String name() {
        return AgentAction.GAP_ANALYSIS.name();
    }

    @Override
    public String description() {
        return "Compares candidate qualifications against job requirements to identify matching skills and gaps.";
    }

    @Override
    public Set<String> requiredInputs() {
        return Set.of("jobAnalysis");
    }

    @Override
    public AgentToolResult execute(AgentToolContext context) {
        @SuppressWarnings("unchecked")
        Map<String, Object> jobAnalysis = (Map<String, Object>) context.getState().getObservation("jobAnalysis");
        if (jobAnalysis == null) {
            return AgentToolResult.builder()
                    .success(false)
                    .summary("Cannot perform gap analysis: job requirements have not been analyzed yet.")
                    .observationKey("gapAnalysis")
                    .observationData(Collections.emptyMap())
                    .build();
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> candidateProfile = (Map<String, Object>) context.getState().getObservation("candidateProfile");

        List<String> candidateSkills = Collections.emptyList();
        if (candidateProfile != null && candidateProfile.get("skills") instanceof List<?>) {
            @SuppressWarnings("unchecked")
            List<String> casted = (List<String>) candidateProfile.get("skills");
            candidateSkills = casted;
        }

        @SuppressWarnings("unchecked")
        List<String> requiredSkills = (List<String>) jobAnalysis.getOrDefault("requiredSkills", List.of());
        @SuppressWarnings("unchecked")
        List<String> preferredSkills = (List<String>) jobAnalysis.getOrDefault("preferredSkills", List.of());
        com.tronai.ai.profile.AiProfile profile = context.getState().getAiProfile();

        Map<String, Object> gapData = null;

        try {
            gapData = callModelForGapAnalysis(candidateSkills, requiredSkills, preferredSkills, profile,
                    context.getState().getLanguage());
        } catch (Exception e) {
            log.warn("GapAnalysisTool LLM call failed: {}. Using deterministic matching.", e.getMessage());
            gapData = deterministicGapAnalysis(candidateSkills, requiredSkills, preferredSkills);
        }

        if (gapData == null) {
            gapData = deterministicGapAnalysis(candidateSkills, requiredSkills, preferredSkills);
        }

        @SuppressWarnings("unchecked")
        List<String> matching = (List<String>) gapData.getOrDefault("matchingSkills", List.of());
        @SuppressWarnings("unchecked")
        List<String> missing = (List<String>) gapData.getOrDefault("missingSkills", List.of());

        String summary = String.format("Identified %d matching skills and %d skill gaps against target role requirements.",
                matching.size(), missing.size());

        return AgentToolResult.builder()
                .success(true)
                .summary(summary)
                .observationKey("gapAnalysis")
                .observationData(gapData)
                .build();
    }

    private Map<String, Object> callModelForGapAnalysis(List<String> candidateSkills,
                                                        List<String> requiredSkills,
                                                        List<String> preferredSkills,
                                                        com.tronai.ai.profile.AiProfile aiProfile,
                                                        String language) throws Exception {
        ModelProfile modelProfile = (aiProfileService != null && aiProfile != null)
                ? aiProfileService.resolveModel(aiProfile)
                : modelManager.getDefaultModel();
        AiProvider provider = modelManager.getProvider(modelProfile.getProvider());

        String prompt = String.format("""
                You are a career readiness evaluator. Compare candidate verified skills against job requirements.
                Strict Rules:
                - A skill is MATCHING ONLY if candidate evidence explicitly exists.
                - Missing skills must be drawn directly from job requirements that candidate lacks.
                - Do NOT fabricate candidate experience.
                - Keep skill names exactly as given.
                - Write strongAreas, riskAreas, and recommendedPreparation in %s.

                Candidate Skills:
                %s

                Job Required Skills:
                %s

                Job Preferred Skills:
                %s

                Return ONLY a JSON object:
                {
                  "matchingSkills": ["skill1"],
                  "missingSkills": ["skill2"],
                  "strongAreas": ["area1"],
                  "riskAreas": ["risk1"],
                  "recommendedPreparation": ["prep1", "prep2"]
                }
                """,
                com.tronai.language.ExperienceLanguage.parseOrDefault(language, com.tronai.language.ExperienceLanguage.JA).englishName(),
                candidateSkills.isEmpty() ? "No CV provided (general evaluation)" : String.join(", ", candidateSkills),
                String.join(", ", requiredSkills),
                String.join(", ", preferredSkills));

        ChatGenerationRequest req = ChatGenerationRequest.builder()
                .model(modelProfile.getId())
                .messages(List.of(new ChatMessage("user", prompt)))
                .temperature(0.2)
                .jsonMode(true)
                .build();

        ChatGenerationResult result = provider.generate(req);
        String json = result.getText();
        if (json == null || json.isBlank()) throw new IllegalStateException("Empty reply from model");

        JsonNode root = objectMapper.readTree(extractJsonSubstring(json));
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("matchingSkills", extractStringList(root, "matchingSkills"));
        map.put("missingSkills", extractStringList(root, "missingSkills"));
        map.put("strongAreas", extractStringList(root, "strongAreas"));
        map.put("riskAreas", extractStringList(root, "riskAreas"));
        map.put("recommendedPreparation", extractStringList(root, "recommendedPreparation"));
        return map;
    }

    private Map<String, Object> deterministicGapAnalysis(List<String> candidateSkills,
                                                         List<String> requiredSkills,
                                                         List<String> preferredSkills) {
        Set<String> candSet = new HashSet<>();
        for (String s : candidateSkills) candSet.add(s.toLowerCase());

        List<String> matching = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        for (String req : requiredSkills) {
            String lower = req.toLowerCase();
            boolean match = false;
            for (String c : candSet) {
                if (lower.contains(c) || c.contains(lower)) {
                    match = true;
                    break;
                }
            }
            if (match) {
                matching.add(req);
            } else {
                missing.add(req);
            }
        }

        for (String pref : preferredSkills) {
            String lower = pref.toLowerCase();
            boolean match = false;
            for (String c : candSet) {
                if (lower.contains(c) || c.contains(lower)) {
                    match = true;
                    break;
                }
            }
            if (match) {
                matching.add(pref + " (Preferred)");
            } else {
                missing.add(pref + " (Preferred)");
            }
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("matchingSkills", matching);
        map.put("missingSkills", missing);
        map.put("strongAreas", matching.isEmpty() ? List.of("Fundamental Software Engineering") : matching);
        map.put("riskAreas", missing.isEmpty() ? List.of("Advanced System Scenarios") : missing);
        map.put("recommendedPreparation", List.of(
                "Review theoretical architecture for: " + (missing.isEmpty() ? "System Design" : String.join(", ", missing)),
                "Prepare concrete project anecdotes demonstrating: " + (matching.isEmpty() ? "Problem solving" : String.join(", ", matching)),
                "Rehearse explanation in target interview language"
        ));
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
