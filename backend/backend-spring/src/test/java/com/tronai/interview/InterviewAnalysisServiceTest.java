package com.tronai.interview;

import com.tronai.ai.AiProvider;
import com.tronai.ai.ChatGenerationRequest;
import com.tronai.ai.ChatGenerationResult;
import com.tronai.ai.model.ModelManager;
import com.tronai.interview.dto.InterviewAnalysisResultDto;
import com.tronai.interview.dto.InterviewConfigDto;
import com.tronai.interview.service.InterviewAnalysisService;
import com.tronai.interview.session.InterviewSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class InterviewAnalysisServiceTest {

    private ModelManager modelManager;
    private AiProvider aiProvider;
    private InterviewAnalysisService analysisService;

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
        analysisService = new InterviewAnalysisService(modelManager);
    }

    @Test
    @DisplayName("Empty candidate answers return baseline evaluation without crashing")
    void emptyCandidateAnswers() {
        InterviewConfigDto config = new InterviewConfigDto();
        InterviewSession session = new InterviewSession("sess-empty", config, 5);

        InterviewAnalysisResultDto result = analysisService.analyze(session);

        assertThat(result.getInterviewId()).isEqualTo("sess-empty");
        assertThat(result.getOverallScore()).isEqualTo(50);
        assertThat(result.getAiOverallFeedback()).contains("候補者の回答がまだないため");
    }

    @Test
    @DisplayName("Valid AI JSON output parses correctly and clamps scores to 0-100")
    void validAiAnalysisParses() {
        String json = """
                {
                  "overallScore": 88,
                  "communicationScore": 85,
                  "japaneseScore": 90,
                  "technicalScore": 92,
                  "confidenceScore": 84,
                  "radarData": [
                    {"metric": "Japanese Fluency", "metricJa": "日本語", "score": 90, "benchmark": 75},
                    {"metric": "Technical Logic", "metricJa": "論理性", "score": 92, "benchmark": 70}
                  ],
                  "strongPoints": ["論理的な説明", "Javaの深い知識"],
                  "weakPoints": ["数値の具体化"],
                  "aiOverallFeedback": "大変素晴らしい面接パフォーマンスでした。",
                  "starSuggestion": {
                    "situation": "状況S",
                    "task": "課題T",
                    "action": "行動A",
                    "result": "成果R"
                  }
                }
                """;
        when(aiProvider.generate(any(ChatGenerationRequest.class))).thenReturn(
                ChatGenerationResult.builder().text(json).build()
        );

        InterviewConfigDto config = new InterviewConfigDto();
        InterviewSession session = new InterviewSession("sess-valid", config, 5);
        session.addTurn("interviewer", "自己紹介をお願いします。");
        session.addTurn("candidate", "Javaエンジニアの山田です。API開発を行ってきました。");

        InterviewAnalysisResultDto result = analysisService.analyze(session);

        assertThat(result.getOverallScore()).isEqualTo(88);
        assertThat(result.getTechnicalScore()).isEqualTo(92);
        assertThat(result.getStrongPoints()).contains("論理的な説明");
        assertThat(result.getStarSuggestion().getSituation()).isEqualTo("状況S");
    }

    @Test
    @DisplayName("Malformed AI output falls back to deterministic analysis without throwing error")
    void malformedAiOutputFallback() {
        when(aiProvider.generate(any(ChatGenerationRequest.class))).thenReturn(
                ChatGenerationResult.builder().text("THIS IS NOT JSON").build()
        );

        InterviewConfigDto config = new InterviewConfigDto();
        InterviewSession session = new InterviewSession("sess-bad", config, 5);
        session.addTurn("interviewer", "自己紹介をお願いします。");
        session.addTurn("candidate", "Spring BootでRESTful APIを設計・構築し、MySQLのクエリチューニングを実施しました。");

        InterviewAnalysisResultDto result = analysisService.analyze(session);

        assertThat(result.getOverallScore()).isBetween(60, 95);
        assertThat(result.getRadarData()).isNotEmpty();
        assertThat(result.getStrongPoints()).isNotEmpty();
        assertThat(result.getWeakPoints()).isNotEmpty();
        assertThat(result.getAiOverallFeedback()).isNotEmpty();
    }

    @Test
    @DisplayName("Ollama unavailable falls back smoothly to deterministic analysis")
    void ollamaUnavailableFallback() {
        when(aiProvider.isAvailable()).thenReturn(false);

        InterviewConfigDto config = new InterviewConfigDto();
        InterviewSession session = new InterviewSession("sess-unavail", config, 5);
        session.addTurn("interviewer", "質問1");
        session.addTurn("candidate", "回答1です。十分に詳細な回答です。");

        InterviewAnalysisResultDto result = analysisService.analyze(session);

        assertThat(result.getOverallScore()).isGreaterThan(0);
        assertThat(result.getRadarData()).hasSize(5);
    }
}
