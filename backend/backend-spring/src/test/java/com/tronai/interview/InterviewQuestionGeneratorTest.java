package com.tronai.interview;

import com.tronai.ai.AiProvider;
import com.tronai.ai.ChatGenerationRequest;
import com.tronai.ai.ChatGenerationResult;
import com.tronai.ai.model.ModelManager;
import com.tronai.interview.dto.InterviewConfigDto;
import com.tronai.interview.generator.InterviewQuestionGenerator;
import com.tronai.interview.session.InterviewSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class InterviewQuestionGeneratorTest {

    private ModelManager modelManager;
    private AiProvider aiProvider;
    private InterviewQuestionGenerator generator;

    @BeforeEach
    void setUp() {
        modelManager = Mockito.mock(ModelManager.class);
        aiProvider = Mockito.mock(AiProvider.class);
        com.tronai.ai.model.ModelProfile profile = com.tronai.ai.model.ModelProfile.builder()
                .id("qwen3-8b")
                .provider(com.tronai.ai.model.ModelProviderType.OLLAMA)
                .providerModelName("qwen3:8b")
                .enabled(true)
                .build();
        when(modelManager.getDefaultModel()).thenReturn(profile);
        when(modelManager.getProvider(com.tronai.ai.model.ModelProviderType.OLLAMA)).thenReturn(aiProvider);
        when(aiProvider.isAvailable()).thenReturn(true);
        generator = new InterviewQuestionGenerator(modelManager);
    }

    @Test
    @DisplayName("Generates natural Japanese first question containing self-introduction request")
    void japaneseFirstQuestion() {
        InterviewConfigDto config = new InterviewConfigDto();
        config.setCompanyName("LINEヤフー株式会社");
        config.setLanguage("japanese");

        String q = generator.generateFirstQuestion(config);

        assertThat(q).contains("LINEヤフー株式会社");
        assertThat(q).contains("自己紹介");
    }

    @Test
    @DisplayName("Generates natural English first question containing introduction request")
    void englishFirstQuestion() {
        InterviewConfigDto config = new InterviewConfigDto();
        config.setCompanyName("Sony");
        config.setLanguage("english");

        String q = generator.generateFirstQuestion(config);

        assertThat(q).contains("Sony");
        assertThat(q).contains("introducing yourself");
    }

    @Test
    @DisplayName("Prevents Q1 repetition and advances topic")
    void preventsRepetitionAndAdvances() {
        InterviewConfigDto config = new InterviewConfigDto();
        config.setLanguage("japanese");
        InterviewSession session = new InterviewSession("sess-1", config, 5);
        session.recordAskedQuestion("1分程度で自己紹介をお願いできますか？", "自己紹介");

        // When LLM generates identical question to Q1
        when(aiProvider.generate(any(ChatGenerationRequest.class))).thenReturn(
                ChatGenerationResult.builder().text("{\"question\": \"1分程度で自己紹介をお願いできますか？\"}").build()
        );

        String nextQ = generator.generateNextQuestion(session, "私は大学でJavaとSpring Bootを学んだ山田です。");

        // Should reject duplicate and pick another topic fallback
        assertThat(nextQ).isNotEqualTo("1分程度で自己紹介をお願いできますか？");
        assertThat(nextQ).isNotEmpty();
    }

    @Test
    @DisplayName("Vague candidate answer triggers clarification fallback")
    void vagueAnswerTriggersClarification() {
        when(aiProvider.isAvailable()).thenReturn(false); // test deterministic fallback

        InterviewConfigDto config = new InterviewConfigDto();
        config.setLanguage("japanese");
        InterviewSession session = new InterviewSession("sess-2", config, 5);

        String nextQ = generator.generateNextQuestion(session, "はい。");

        assertThat(nextQ).contains("具体的な場面");
    }

    @Test
    @DisplayName("Offline or failing LLM gracefully falls back to deterministic question")
    void offlineLlmFallback() {
        when(aiProvider.isAvailable()).thenReturn(false);

        InterviewConfigDto config = new InterviewConfigDto();
        config.setLanguage("japanese");
        InterviewSession session = new InterviewSession("sess-3", config, 5);

        String nextQ = generator.generateNextQuestion(session, "バックエンドAPIのマイクロサービス開発を担当しました。");

        assertThat(nextQ).isNotEmpty();
        assertThat(nextQ).matches(".*(？|\\?|か。|ください。|でしょうか。)$");
    }
}
