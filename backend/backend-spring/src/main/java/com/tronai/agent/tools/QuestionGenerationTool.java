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
 * Tool 5: Question Generation Tool.
 * Creates 5 targeted, highly customized mock interview questions utilizing candidate strengths,
 * identified skill gaps, and strategic advice.
 */
@Component
public class QuestionGenerationTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(QuestionGenerationTool.class);

    private final ModelManager modelManager;
    private final com.tronai.ai.profile.AiProfileService aiProfileService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @org.springframework.beans.factory.annotation.Autowired
    public QuestionGenerationTool(ModelManager modelManager, com.tronai.ai.profile.AiProfileService aiProfileService) {
        this.modelManager = modelManager;
        this.aiProfileService = aiProfileService;
    }

    public QuestionGenerationTool(ModelManager modelManager) {
        this(modelManager, null);
    }

    @Override
    public String name() {
        return AgentAction.QUESTION_GENERATION.name();
    }

    @Override
    public String description() {
        return "Generates 5 tailored interview questions targeting verified strengths and critical skill gaps.";
    }

    @Override
    public Set<String> requiredInputs() {
        return Set.of("jobAnalysis");
    }

    @Override
    public AgentToolResult execute(AgentToolContext context) {
        @SuppressWarnings("unchecked")
        Map<String, Object> jobAnalysis = (Map<String, Object>) context.getState().getObservation("jobAnalysis");
        @SuppressWarnings("unchecked")
        Map<String, Object> gapAnalysis = (Map<String, Object>) context.getState().getObservation("gapAnalysis");
        @SuppressWarnings("unchecked")
        Map<String, Object> strategy = (Map<String, Object>) context.getState().getObservation("interviewStrategy");

        String company = context.getState().getCompanyName();
        String position = context.getState().getJobPosition();
        String language = context.getState().getLanguage();
        com.tronai.ai.profile.AiProfile profile = context.getState().getAiProfile();

        List<Map<String, Object>> questions = null;

        try {
            questions = callModelForQuestions(company, position, language, jobAnalysis, gapAnalysis, strategy, profile);
        } catch (Exception e) {
            log.warn("QuestionGenerationTool LLM call failed: {}. Using deterministic question generator.", e.getMessage());
            questions = deterministicQuestions(company, position, language, gapAnalysis);
        }

        if (questions == null || questions.isEmpty()) {
            questions = deterministicQuestions(company, position, language, gapAnalysis);
        }

        String summary = String.format("Generated %d customized practice questions tailored to %s %s requirements.",
                questions.size(), company, position);

        return AgentToolResult.builder()
                .success(true)
                .summary(summary)
                .observationKey("practiceQuestions")
                .observationData(questions)
                .build();
    }

    private List<Map<String, Object>> callModelForQuestions(String company, String position, String language,
                                                            Map<String, Object> jobAnalysis,
                                                            Map<String, Object> gapAnalysis,
                                                            Map<String, Object> strategy,
                                                            com.tronai.ai.profile.AiProfile aiProfile) throws Exception {
        ModelProfile modelProfile = (aiProfileService != null && aiProfile != null)
                ? aiProfileService.resolveModel(aiProfile)
                : modelManager.getDefaultModel();
        AiProvider provider = modelManager.getProvider(modelProfile.getProvider());

        com.tronai.language.ExperienceLanguage selected =
                com.tronai.language.ExperienceLanguage.parseOrDefault(language, com.tronai.language.ExperienceLanguage.JA);
        String prose = selected.englishName();

        String prompt = String.format("""
                You are a senior technical interviewer for %s.
                Create exactly 5 targeted interview questions for a %s position.
                Language for questions and explanations: %s.
                Keep company names, skill names, and quoted source text unchanged.

                Context:
                - Company: %s
                - Role: %s
                - Candidate Strengths: %s
                - Identified Gaps: %s
                - Strategic Priorities: %s

                Return ONLY a JSON array with 5 items matching this schema:
                [
                  {
                    "question": "Question text in %s",
                    "category": "Technical | System Design | Behavioral | Experience",
                    "difficulty": "Easy | Medium | Hard",
                    "whyThisQuestionMatters": "Reasoning relating to company and role",
                    "preparationHint": "Actionable tip on how to structure the answer"
                  }
                ]
                """,
                company, position, prose,
                company, position,
                gapAnalysis != null ? gapAnalysis.getOrDefault("matchingSkills", List.of()) : "General",
                gapAnalysis != null ? gapAnalysis.getOrDefault("missingSkills", List.of()) : "None",
                strategy != null ? strategy.getOrDefault("priorityTopics", List.of()) : "Core competencies",
                prose);

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
        List<Map<String, Object>> list = new ArrayList<>();

        if (root.isArray()) {
            for (JsonNode n : root) {
                if (n.has("question")) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("question", n.get("question").asText());
                    item.put("category", n.has("category") ? n.get("category").asText() : "Technical");
                    item.put("difficulty", n.has("difficulty") ? n.get("difficulty").asText() : "Medium");
                    item.put("whyThisQuestionMatters", n.has("whyThisQuestionMatters") ? n.get("whyThisQuestionMatters").asText() : "Evaluates practical readiness for " + company);
                    item.put("preparationHint", n.has("preparationHint") ? n.get("preparationHint").asText() : "Use concrete examples and focus on trade-offs.");
                    list.add(item);
                }
            }
        }

        if (list.size() < 3) throw new IllegalStateException("Less than 3 valid questions parsed");
        return list;
    }

    private List<Map<String, Object>> deterministicQuestions(String company, String position, String language,
                                                             Map<String, Object> gapAnalysis) {
        boolean isJa = com.tronai.language.ExperienceLanguage.parseOrDefault(
                language, com.tronai.language.ExperienceLanguage.JA) == com.tronai.language.ExperienceLanguage.JA;
        List<Map<String, Object>> questions = new ArrayList<>();

        @SuppressWarnings("unchecked")
        List<String> missing = gapAnalysis != null ? (List<String>) gapAnalysis.getOrDefault("missingSkills", List.of()) : List.of();
        String gapTopic = !missing.isEmpty() ? missing.get(0) : "大規模分散システム";

        if (isJa) {
            questions.add(createQuestion(
                    "これまでに担当したバックエンド開発プロジェクトにおいて、設計上の最大の技術的課題と、それをどのように解決したかを教えてください。",
                    "Technical Experience", "Medium",
                    company + "における実践的なアーキテクチャ設計・問題解決能力を確認するため。",
                    "STAR法（状況・課題・行動・結果）を用い、定量的な成果（パフォーマンス改善率等）を含めて回答してください。"
            ));
            questions.add(createQuestion(
                    "Spring Bootを用いたマイクロサービス開発において、APIのレイテンシー削減やスケーラビリティ向上のために考慮すべき設計原則を説明してください。",
                    "System Architecture", "Hard",
                    company + "の大規模トラフィックを支える高可用性・高性能なAPI設計知識を評価するため。",
                    "キャッシュ戦略（Redis等）、非同期処理、DBインデックスやコネクションプールの最適化について具体的に言及してください。"
            ));
            questions.add(createQuestion(
                    "ご自身のこれまでのスキルセットを踏まえ、実務で経験が少ない技術領域（例: " + gapTopic + "）について、どのようにキャッチアップし実務に適用しますか？",
                    "Adaptability & Growth", "Medium",
                    "技術的なギャップを迅速に埋める自律的学習姿勢と適応力を確認するため。",
                    "過去に未経験技術を短期間で習得した具体例を挙げ、公式ドキュメントやプロトタイプ作成を通じた学習サイクルをアピールしてください。"
            ));
            questions.add(createQuestion(
                    "データベースの設計において、正規化とパフォーマンス（非正規化）のトレードオフをどのように判断していますか？具体的なシチュエーションを交えて教えてください。",
                    "Database Engineering", "Medium",
                    "データ整合性とスループットのバランスを判断できる実務知見を評価するため。",
                    "リードヘビー vs ライトヘビーの特性、キャッシュ層との組み合わせによる負荷分散の観点を盛り込んでください。"
            ));
            questions.add(createQuestion(
                    "チーム開発において、他のエンジニアと設計方針やコードレビューで意見が分かれた際、どのように合意形成を図っていますか？",
                    "Behavioral & Teamwork", "Easy",
                    "多様なメンバーが協調して開発する組織におけるコミュニケーションとチームワークを測るため。",
                    "個人の主観ではなく、保守性・可読性・パフォーマンスといった客観的評価基準に基づいて建設的に議論した経験を述べてください。"
            ));
        } else {
            questions.add(createQuestion(
                    "Walk us through a complex backend project you worked on. What was the core architectural challenge and how did you resolve it?",
                    "Technical Experience", "Medium",
                    "Evaluates practical problem-solving and system architecture depth at " + company + ".",
                    "Use the STAR framework and provide measurable impact (latency reduction, concurrency handled)."
            ));
            questions.add(createQuestion(
                    "In a Spring Boot environment, what architectural patterns do you employ to guarantee high availability and low latency under peak traffic?",
                    "System Architecture", "Hard",
                    "Measures system design capabilities for high-concurrency services.",
                    "Discuss caching, asynchronous message processing, connection pool tuning, and database index optimization."
            ));
            questions.add(createQuestion(
                    "Looking at our role requirements, how would you approach ramping up on areas outside your immediate experience, such as " + gapTopic + "?",
                    "Adaptability & Learning", "Medium",
                    "Assesses continuous learning ability and proactive adaptation.",
                    "Highlight previous experience mastering unfamiliar technologies quickly through POCs and documentation."
            ));
            questions.add(createQuestion(
                    "How do you evaluate trade-offs between strict database normalization and query performance in high-throughput services?",
                    "Database Engineering", "Medium",
                    "Assesses real-world trade-off decision making for persistent data layers.",
                    "Discuss read/write patterns, replication, and caching strategies."
            ));
            questions.add(createQuestion(
                    "Describe a situation where you had a technical disagreement during code review or architecture design. How did you reach consensus?",
                    "Behavioral & Teamwork", "Easy",
                    "Tests cross-functional communication and collaborative problem resolution.",
                    "Focus on objective criteria (maintainability, scalability) rather than subjective preference."
            ));
        }

        return questions;
    }

    private Map<String, Object> createQuestion(String question, String category, String difficulty,
                                               String whyThisQuestionMatters, String preparationHint) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("question", question);
        map.put("category", category);
        map.put("difficulty", difficulty);
        map.put("whyThisQuestionMatters", whyThisQuestionMatters);
        map.put("preparationHint", preparationHint);
        return map;
    }

    private String extractJsonSubstring(String text) {
        int firstBracket = text.indexOf('[');
        int lastBracket = text.lastIndexOf(']');
        if (firstBracket >= 0 && lastBracket > firstBracket) {
            return text.substring(firstBracket, lastBracket + 1);
        }
        int firstBrace = text.indexOf('{');
        int lastBrace = text.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return text.substring(firstBrace, lastBrace + 1);
        }
        return text;
    }
}
