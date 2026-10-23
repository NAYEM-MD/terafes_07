package com.tronai.interview.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tronai.ai.AiProvider;
import com.tronai.ai.ChatGenerationRequest;
import com.tronai.ai.ChatGenerationResult;
import com.tronai.ai.ChatMessage;
import com.tronai.ai.gateway.AiFeature;
import com.tronai.ai.gateway.AiGateway;
import com.tronai.ai.gateway.AiMode;
import com.tronai.ai.gateway.AiRequestContext;
import com.tronai.ai.model.ModelManager;
import com.tronai.ai.model.ModelProfile;
import com.tronai.ai.profile.AiProfile;
import com.tronai.ai.profile.AiProfileService;
import com.tronai.interview.dto.InterviewConfigDto;
import com.tronai.interview.session.InterviewSession;
import com.tronai.interview.session.InterviewTurn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generates contextual interview questions using ModelManager and AiProfileService with deterministic fallbacks.
 * Enforces session-locked model routing (FAST vs QUALITY).
 */
@Component
public class InterviewQuestionGenerator {

    private static final Logger log = LoggerFactory.getLogger(InterviewQuestionGenerator.class);

    private final ModelManager modelManager;
    private final AiProfileService aiProfileService;
    private final AiGateway aiGateway;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final List<String> TOPICS_JA = List.of(
            "自己紹介",
            "志望動機",
            "自己PR",
            "強み・弱み",
            "学校で学んだこと",
            "プロジェクト経験",
            "チームワーク",
            "技術スキル",
            "将来のキャリア目標",
            "逆質問"
    );

    private static final Map<String, String> FALLBACK_QUESTIONS_JA = Map.of(
            "自己紹介", "1分程度で自己紹介をお願いできますか？",
            "志望動機", "この職種、そして弊社を志望された理由を教えてください。",
            "自己PR", "これまでの経験の範囲で、ご自身の最大の強みとそれを発揮したエピソードを教えてください。",
            "強み・弱み", "ご自身の課題や弱みと、それを克服するために取り組んでいることを教えてください。",
            "学校で学んだこと", "学業や研究で学んだことで、実際の開発業務に生かせる点は何ですか？",
            "プロジェクト経験", "これまでに関わった開発プロジェクトの中で、ご自身の役割と直面した技術的課題について教えてください。",
            "チームワーク", "チームで開発を進める際、意見が分かれたときはどのように合意形成を図っていますか？",
            "技術スキル", "得意なプログラミング言語や技術スタックについて、選定理由や使い所を教えてください。",
            "将来のキャリア目標", "将来どのようなエンジニアになりたいか、キャリア目標を教えてください。",
            "逆質問", "最後に、聞いておきたいことはありますか？"
    );

    private static final Map<String, String> FALLBACK_QUESTIONS_EN = Map.of(
            "自己紹介", "Could you briefly introduce yourself and highlight your background?",
            "志望動機", "What motivated you to apply for this role and join our company?",
            "自己PR", "What do you consider your greatest professional strength, and how has it helped you succeed?",
            "強み・弱み", "What is an area you are currently striving to improve, and what steps are you taking?",
            "学校で学んだこと", "How has your academic background or studies prepared you for this role?",
            "プロジェクト経験", "Could you tell us about a challenging software project you worked on and your contributions?",
            "チームワーク", "How do you approach collaboration within a development team when differing opinions arise?",
            "技術スキル", "Which programming languages or technologies are you most confident with, and why?",
            "将来のキャリア目標", "Where do you see yourself technically and professionally in the next few years?",
            "逆質問", "Do you have any questions for us regarding the team or our technical challenges?"
    );

    private static final Pattern CLEAN_PUNCTUATION = Pattern.compile("[\\s。、？?！!,.\\-]");

    @Autowired
    public InterviewQuestionGenerator(
            ModelManager modelManager,
            @Autowired(required = false) AiProfileService aiProfileService,
            @Autowired(required = false) AiGateway aiGateway) {
        this.modelManager = modelManager;
        this.aiProfileService = aiProfileService;
        this.aiGateway = aiGateway;
    }

    public InterviewQuestionGenerator(ModelManager modelManager) {
        this(modelManager, null, null);
    }

    public InterviewQuestionGenerator() {
        this(null, null, null);
    }

    /**
     * Generates or selects the first question (Self-introduction).
     */
    public String generateFirstQuestion(InterviewConfigDto config) {
        com.tronai.language.ExperienceLanguage language =
                com.tronai.language.ExperienceLanguage.parseOrDefault(config.getLanguage(), com.tronai.language.ExperienceLanguage.JA);
        boolean isEnglish = language != com.tronai.language.ExperienceLanguage.JA;
        String company = config.getCompanyNameJa() != null && !config.getCompanyNameJa().isBlank()
                ? config.getCompanyNameJa()
                : (config.getCompanyName() != null ? config.getCompanyName() : "");

        if (language != com.tronai.language.ExperienceLanguage.JA && language != com.tronai.language.ExperienceLanguage.EN) {
            return openingIn(language, company);
        }
        if (isEnglish) {
            if (!company.isBlank()) {
                return "Thank you for interviewing with " + company + " today. Could you please start by introducing yourself and giving a brief overview of your background?";
            }
            return "Thank you for joining us today. Could you please start by introducing yourself and giving a brief overview of your background?";
        }

        if (!company.isBlank()) {
            return company + "の面接にお越しいただきありがとうございます。1分程度で自己紹介をお願いできますでしょうか？";
        }
        return "候補者さん、本日はありがとうございます。1分程度で自己紹介をお願いします。";
    }

    private static String openingIn(com.tronai.language.ExperienceLanguage language, String company) {
        String name = company == null ? "" : company;
        return switch (language) {
            case SI -> name.isBlank()
                    ? "කරුණාකර ඔබව හඳුන්වා දී ඔබේ පසුබිම කෙටියෙන් කියන්න."
                    : name + " සම්මුඛ පරීක්ෂණයට ස්තූතියි. කරුණාකර ඔබව හඳුන්වා දෙන්න.";
            case BN -> name.isBlank()
                    ? "অনুগ্রহ করে নিজের পরিচয় দিন এবং আপনার পটভূমি সংক্ষেপে বলুন।"
                    : name + " সাক্ষাৎকারে স্বাগতম। অনুগ্রহ করে নিজের পরিচয় দিন।";
            case NE -> name.isBlank()
                    ? "कृपया आफ्नो परिचय दिनुहोस् र आफ्नो पृष्ठभूमि छोटकरीमा भन्नुहोस्।"
                    : name + " को अन्तर्वार्तामा स्वागत छ। कृपया आफ्नो परिचय दिनुहोस्।";
            case MY -> name.isBlank()
                    ? "ကျေးဇူးပြု၍ မိမိကိုယ်ကို မိတ်ဆက်ပြီး နောက်ခံကို အကျဉ်းချုပ် ပြောပြပါ။"
                    : name + " အင်တာဗျူးကို ကြိုဆိုပါတယ်။ ကျေးဇူးပြု၍ မိမိကိုယ်ကို မိတ်ဆက်ပေးပါ။";
            default -> "Please introduce yourself and give a brief overview of your background.";
        };
    }

    /**
     * Generates the next question based on interview context, candidate's answers, and topic progression.
     */
    public String generateNextQuestion(InterviewSession session, String latestAnswer) {
        com.tronai.language.ExperienceLanguage language =
                com.tronai.language.ExperienceLanguage.parseOrDefault(session.getConfig().getLanguage(), com.tronai.language.ExperienceLanguage.JA);
        boolean isEnglish = language != com.tronai.language.ExperienceLanguage.JA;
        String company = session.getConfig().getCompanyNameJa() != null && !session.getConfig().getCompanyNameJa().isBlank()
                ? session.getConfig().getCompanyNameJa()
                : (session.getConfig().getCompanyName() != null ? session.getConfig().getCompanyName() : "");
        String role = session.getConfig().getJobPositionJa() != null && !session.getConfig().getJobPositionJa().isBlank()
                ? session.getConfig().getJobPositionJa()
                : (session.getConfig().getJobPosition() != null ? session.getConfig().getJobPosition() : "エンジニア");

        List<String> asked = session.getAskedQuestions();
        List<String> covered = session.getCoveredTopics();
        String nextTopic = chooseNextTopic(covered);

        // Try AI generation first if modelManager is present
        if (modelManager != null) {
            try {
                String aiQuestion = callModelForQuestion(session, latestAnswer, nextTopic, language, company, role);
                if (aiQuestion != null && isAcceptableQuestion(aiQuestion, asked, latestAnswer, isEnglish)) {
                    session.recordAskedQuestion(aiQuestion, nextTopic);
                    return aiQuestion;
                }
            } catch (Exception e) {
                log.info("AI question generation fell back to deterministic generator: {}", e.getMessage());
            }
        }

        // Deterministic fallback
        String fallback = getFallbackForAnswer(latestAnswer, nextTopic, isEnglish, company, asked);
        session.recordAskedQuestion(fallback, nextTopic);
        return fallback;
    }

    private String callModelForQuestion(InterviewSession session, String latestAnswer, String nextTopic,
                                        com.tronai.language.ExperienceLanguage language, String company, String role) throws Exception {
        ModelProfile profile;
        if (aiProfileService != null && session.getAiProfile() != null) {
            profile = aiProfileService.resolveModel(session.getAiProfile());
        } else {
            profile = modelManager.getDefaultModel();
        }

        AiProvider provider = modelManager.getProvider(profile.getProvider());
        if (provider == null || !provider.isAvailable()) {
            return null;
        }

        String systemPrompt;
        if (language != com.tronai.language.ExperienceLanguage.JA) {
            systemPrompt = "You are an expert interviewer for " + company + " interviewing for the " + role + " position. " +
                    "Ask ONE natural, professional interview question in " + language.englishName() + " focusing on the topic: " + nextTopic + ". " +
                    "Do NOT invent facts not mentioned by the candidate. Do not translate company names, skill names, or quoted words. Keep it under 2 sentences. " +
                    "Respond ONLY with a JSON object: {\"question\": \"your question\"}";
        } else {
            systemPrompt = "あなたは「" + company + "」の「" + role + "」採用面接官です。声に出して読む自然な日本語の質問を1文だけ作成してください。\n" +
                    "観点: 「" + nextTopic + "」\n" +
                    "候補者が話していない事実（学校、会社名、技術数値など）を勝手に創作しないでください。\n" +
                    "80文字以内。です・ます調。JSON形式のみで回答してください: {\"question\": \"質問文\"}";
        }

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.builder().role("system").content(systemPrompt).build());

        // Include last few dialogue turns
        List<InterviewTurn> turns = session.getTurns();
        int startIndex = Math.max(0, turns.size() - 6);
        for (int i = startIndex; i < turns.size(); i++) {
            InterviewTurn turn = turns.get(i);
            String roleName = "candidate".equalsIgnoreCase(turn.getRole()) ? "user" : "assistant";
            messages.add(ChatMessage.builder().role(roleName).content(turn.getContent()).build());
        }

        // Add the latest answer
        if (latestAnswer != null && !latestAnswer.isBlank()) {
            messages.add(ChatMessage.builder().role("user").content(latestAnswer).build());
        }

        ChatGenerationResult response = generateInterviewText(
                session.getAiProfile(),
                AiFeature.INTERVIEW,
                profile,
                provider,
                messages,
                180);
        if (response == null || response.getText() == null || response.getText().isBlank()) {
            return null;
        }

        return extractQuestionFromJson(response.getText());
    }

    private ChatGenerationResult generateInterviewText(
            String aiProfileName,
            AiFeature feature,
            ModelProfile profile,
            AiProvider provider,
            List<ChatMessage> messages,
            int maxTokens) {
        AiProfile logical = safeProfile(aiProfileName);
        if (aiGateway != null && usesGateway(logical)) {
            AiRequestContext context = AiRequestContext.builder()
                    .feature(feature)
                    .mode(toMode(logical))
                    .userId(com.tronai.auth.SecurityUtils.getCurrentUser().map(u -> u.getId()).orElse(null))
                    .messages(messages)
                    .temperature(0.2)
                    .maxTokens(maxTokens)
                    .jsonMode(true)
                    .idempotencyKey(UUID.randomUUID().toString())
                    .build();
            return aiGateway.generate(context).toChatGenerationResult();
        }
        ChatGenerationRequest request = ChatGenerationRequest.builder()
                .model(profile.getProviderModelName())
                .messages(messages)
                .temperature(0.2)
                .numPredict(maxTokens)
                .jsonMode(true)
                .build();
        return provider.generate(request);
    }

    private static boolean usesGateway(AiProfile profile) {
        return profile == AiProfile.SMART || profile == AiProfile.ULTRA || profile == AiProfile.AUTO;
    }

    private static AiMode toMode(AiProfile profile) {
        return switch (profile) {
            case FAST -> AiMode.FAST;
            case QUALITY -> AiMode.QUALITY;
            case SMART -> AiMode.SMART;
            case ULTRA -> AiMode.ULTRA;
            case AUTO -> AiMode.AUTO;
        };
    }

    private static AiProfile safeProfile(String value) {
        try {
            return AiProfile.fromString(value);
        } catch (RuntimeException ex) {
            return AiProfile.FAST;
        }
    }

    private String extractQuestionFromJson(String raw) {
        String clean = raw.trim();
        // Remove markdown backticks if any
        if (clean.startsWith("```")) {
            int firstNewline = clean.indexOf('\n');
            int lastBackticks = clean.lastIndexOf("```");
            if (firstNewline >= 0 && lastBackticks > firstNewline) {
                clean = clean.substring(firstNewline + 1, lastBackticks).trim();
            }
        }
        try {
            JsonNode root = objectMapper.readTree(clean);
            if (root.has("question") && !root.get("question").asText().isBlank()) {
                return root.get("question").asText().trim();
            }
        } catch (Exception ignored) {
        }

        // Regex fallback for {"question": "..."}
        Pattern p = Pattern.compile("\"question\"\\s*:\\s*\"([^\"]+)\"");
        Matcher m = p.matcher(raw);
        if (m.find()) {
            return m.group(1).trim();
        }

        // If no JSON format but plain text question was returned
        if (!clean.startsWith("{") && (clean.endsWith("？") || clean.endsWith("?") || clean.endsWith("か。") || clean.endsWith("ください。"))) {
            return clean;
        }
        return null;
    }

    private boolean isAcceptableQuestion(String question, List<String> askedQuestions, String answer, boolean isEnglish) {
        if (question == null || question.isBlank() || question.length() < 5) {
            return false;
        }
        if (question.contains("<think>") || question.contains("```")) {
            return false;
        }

        // Check repetition
        if (isRepeatedQuestion(question, askedQuestions)) {
            log.info("Rejected question due to similarity with previously asked questions: {}", question);
            return false;
        }

        // Japanese specific validation
        if (!isEnglish) {
            boolean hasJapanese = question.codePoints().anyMatch(cp ->
                    (cp >= 0x3040 && cp <= 0x309F) || (cp >= 0x30A0 && cp <= 0x30FF) || (cp >= 0x4E00 && cp <= 0x9FFF));
            if (!hasJapanese) {
                return false;
            }
            if (!question.endsWith("か？") && !question.endsWith("？") && !question.endsWith("か。") && !question.endsWith("ください。") && !question.endsWith("でしょうか。")) {
                return false;
            }
        }
        return true;
    }

    private boolean isRepeatedQuestion(String newQuestion, List<String> askedQuestions) {
        String cleanedNew = CLEAN_PUNCTUATION.matcher(newQuestion.toLowerCase()).replaceAll("");
        for (String asked : askedQuestions) {
            String cleanedAsked = CLEAN_PUNCTUATION.matcher(asked.toLowerCase()).replaceAll("");
            if (cleanedNew.equals(cleanedAsked) || cleanedNew.contains(cleanedAsked) || cleanedAsked.contains(cleanedNew)) {
                return true;
            }
            // Overlap similarity
            double similarity = computeDiceCoefficient(cleanedNew, cleanedAsked);
            if (similarity >= 0.75) {
                return true;
            }
        }
        return false;
    }

    private double computeDiceCoefficient(String s1, String s2) {
        if (s1.length() < 2 || s2.length() < 2) {
            return s1.equals(s2) ? 1.0 : 0.0;
        }
        Set<String> bigrams1 = new HashSet<>();
        for (int i = 0; i < s1.length() - 1; i++) {
            bigrams1.add(s1.substring(i, i + 2));
        }
        int matches = 0;
        for (int i = 0; i < s2.length() - 1; i++) {
            if (bigrams1.contains(s2.substring(i, i + 2))) {
                matches++;
            }
        }
        return (2.0 * matches) / (bigrams1.size() + (s2.length() - 1));
    }

    private String chooseNextTopic(List<String> coveredTopics) {
        for (String topic : TOPICS_JA) {
            if (!coveredTopics.contains(topic)) {
                return topic;
            }
        }
        return TOPICS_JA.get(TOPICS_JA.size() - 1);
    }

    private String getFallbackForAnswer(String answer, String topic, boolean isEnglish, String company, List<String> asked) {
        boolean isVague = answer == null || answer.trim().length() < 30;

        if (isVague && !asked.contains("その点について、具体的な場面を一つ教えてください。")) {
            return isEnglish
                    ? "Could you elaborate on that with a specific scenario or example?"
                    : "その点について、具体的な場面を一つ教えてください。";
        }

        Map<String, String> fallbacks = isEnglish ? FALLBACK_QUESTIONS_EN : FALLBACK_QUESTIONS_JA;
        String question = fallbacks.getOrDefault(topic, fallbacks.get("技術スキル"));

        if (!isEnglish && "志望動機".equals(topic) && company != null && !company.isBlank()) {
            question = company + "を志望された具体的な理由と、入社後に挑戦したいことを教えてください。";
        }

        // If even the primary fallback repeats, rotate to another topic
        if (isRepeatedQuestion(question, asked)) {
            for (String otherTopic : TOPICS_JA) {
                String candidate = fallbacks.get(otherTopic);
                if (candidate != null && !isRepeatedQuestion(candidate, asked)) {
                    return candidate;
                }
            }
        }
        return question;
    }
}
