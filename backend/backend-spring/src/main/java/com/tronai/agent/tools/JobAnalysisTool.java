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
 * Tool 2: Job Analysis Tool.
 * Parses and categorizes job requirements, required vs preferred skills, and core responsibilities.
 * Uses ModelManager with one automatic retry and deterministic fallback.
 */
@Component
public class JobAnalysisTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(JobAnalysisTool.class);

    private final ModelManager modelManager;
    private final com.tronai.ai.profile.AiProfileService aiProfileService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @org.springframework.beans.factory.annotation.Autowired
    public JobAnalysisTool(ModelManager modelManager, com.tronai.ai.profile.AiProfileService aiProfileService) {
        this.modelManager = modelManager;
        this.aiProfileService = aiProfileService;
    }

    public JobAnalysisTool(ModelManager modelManager) {
        this(modelManager, null);
    }

    @Override
    public String name() {
        return AgentAction.JOB_ANALYSIS.name();
    }

    @Override
    public String description() {
        return "Extracts structured technical and behavioral requirements from the target job description.";
    }

    @Override
    public Set<String> requiredInputs() {
        return Collections.emptySet();
    }

    @Override
    public AgentToolResult execute(AgentToolContext context) {
        String company = context.getState().getCompanyName();
        String position = context.getState().getJobPosition();
        String description = context.getState().getJobDescription();
        String language = context.getState().getLanguage();
        com.tronai.ai.profile.AiProfile profile = context.getState().getAiProfile();

        Map<String, Object> jobData = null;

        // Attempt 1 with ModelManager
        try {
            jobData = callModelForJobAnalysis(company, position, description, language, profile);
        } catch (Exception e) {
            log.warn("JobAnalysisTool attempt 1 failed: {}. Retrying once...", e.getMessage());
            try {
                jobData = callModelForJobAnalysis(company, position, description, language, profile);
            } catch (Exception e2) {
                log.warn("JobAnalysisTool attempt 2 failed: {}. Employing deterministic fallback.", e2.getMessage());
                jobData = deterministicFallback(company, position, description);
            }
        }

        if (jobData == null) {
            jobData = deterministicFallback(company, position, description);
        }

        @SuppressWarnings("unchecked")
        List<String> requiredSkills = (List<String>) jobData.getOrDefault("requiredSkills", List.of());
        @SuppressWarnings("unchecked")
        List<String> techTopics = (List<String>) jobData.getOrDefault("technicalTopics", List.of());

        String summary = String.format("Analyzed job specifications for %s at %s (%d required skills, %d key technical topics).",
                position, company, requiredSkills.size(), techTopics.size());

        return AgentToolResult.builder()
                .success(true)
                .summary(summary)
                .observationKey("jobAnalysis")
                .observationData(jobData)
                .build();
    }

    private Map<String, Object> callModelForJobAnalysis(String company, String position, String description, String language, com.tronai.ai.profile.AiProfile aiProfile) throws Exception {
        ModelProfile modelProfile = (aiProfileService != null && aiProfile != null)
                ? aiProfileService.resolveModel(aiProfile)
                : modelManager.getDefaultModel();
        AiProvider provider = modelManager.getProvider(modelProfile.getProvider());

        String prompt = String.format("""
                You are an expert technical recruiter analyzing a job posting.
                Analyze the following position and extract structured requirements strictly based on the text.
                Do NOT invent requirements not present in the description.
                Keep requiredSkills, preferredSkills, and importantKeywords in the posting's original wording.
                Write languageExpectations in %s. Do not translate the job posting itself.

                Company: %s
                Position: %s
                Job Description:
                %s

                Respond with ONLY a valid JSON object matching this schema:
                {
                  "requiredSkills": ["skill1", "skill2"],
                  "preferredSkills": ["skill1", "skill2"],
                  "responsibilities": ["resp1", "resp2"],
                  "technicalTopics": ["topic1", "topic2"],
                  "softSkills": ["skill1", "skill2"],
                  "languageExpectations": "description of Japanese/English level",
                  "importantKeywords": ["kw1", "kw2"]
                }
                """,
                com.tronai.language.ExperienceLanguage.parseOrDefault(language, com.tronai.language.ExperienceLanguage.JA).englishName(),
                company, position, description);

        ChatGenerationRequest req = ChatGenerationRequest.builder()
                .model(modelProfile.getId())
                .messages(List.of(new ChatMessage("user", prompt)))
                .temperature(0.2)
                .jsonMode(true)
                .build();

        ChatGenerationResult result = provider.generate(req);
        String json = result.getText();
        if (json == null || json.isBlank()) {
            throw new IllegalStateException("Empty reply from model");
        }

        JsonNode root = objectMapper.readTree(extractJsonSubstring(json));
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("requiredSkills", extractStringList(root, "requiredSkills"));
        map.put("preferredSkills", extractStringList(root, "preferredSkills"));
        map.put("responsibilities", extractStringList(root, "responsibilities"));
        map.put("technicalTopics", extractStringList(root, "technicalTopics"));
        map.put("softSkills", extractStringList(root, "softSkills"));
        map.put("languageExpectations", root.has("languageExpectations") ? root.get("languageExpectations").asText() : "Japanese communication");
        map.put("importantKeywords", extractStringList(root, "importantKeywords"));

        return map;
    }

    private Map<String, Object> deterministicFallback(String company, String position, String description) {
        String lower = description.toLowerCase();
        List<String> required = new ArrayList<>();
        List<String> preferred = new ArrayList<>();
        List<String> topics = new ArrayList<>();
        List<String> soft = new ArrayList<>();

        if (lower.contains("java")) required.add("Java");
        if (lower.contains("spring")) required.add("Spring Boot");
        if (lower.contains("sql") || lower.contains("mysql") || lower.contains("database")) required.add("SQL / Databases");
        if (lower.contains("rest") || lower.contains("api")) required.add("REST APIs");
        if (lower.contains("git")) required.add("Git");
        if (lower.contains("cloud") || lower.contains("aws")) preferred.add("Cloud / AWS");
        if (lower.contains("team") || lower.contains("チーム")) soft.add("Team Collaboration");
        if (lower.contains("japanese") || lower.contains("日本語") || lower.contains("コミュニケーション")) soft.add("Japanese Communication");

        if (required.isEmpty()) {
            required.addAll(List.of("Software Engineering", "Backend Development", "System Design"));
        }
        topics.addAll(required);
        if (!preferred.isEmpty()) topics.addAll(preferred);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("requiredSkills", required);
        map.put("preferredSkills", preferred.isEmpty() ? List.of("Cloud Infrastructure") : preferred);
        map.put("responsibilities", List.of("Backend API development", "Database design", "Team feature delivery"));
        map.put("technicalTopics", topics);
        map.put("softSkills", soft.isEmpty() ? List.of("Communication", "Teamwork") : soft);
        map.put("languageExpectations", "Business Japanese communication for technical discussions");
        map.put("importantKeywords", List.of(company, position, "Backend", "High Performance"));
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
