package com.tronai.agent;

import com.tronai.agent.model.AgentState;
import com.tronai.agent.model.AgentToolContext;
import com.tronai.agent.model.AgentToolResult;
import com.tronai.agent.tools.*;
import com.tronai.ai.AiProvider;
import com.tronai.ai.ChatGenerationRequest;
import com.tronai.ai.ChatGenerationResult;
import com.tronai.ai.model.ModelManager;
import com.tronai.ai.model.ModelProfile;
import com.tronai.ai.model.ModelProviderType;
import com.tronai.cv.CvDocumentEntity;
import com.tronai.cv.CvDocumentRepository;
import com.tronai.cv.CvProfileEntity;
import com.tronai.cv.CvProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class AgentToolsUnitTest {

    private ModelManager modelManager;
    private AiProvider mockProvider;
    private CvDocumentRepository cvDocumentRepository;
    private CvProfileRepository cvProfileRepository;

    @BeforeEach
    void setUp() {
        modelManager = Mockito.mock(ModelManager.class);
        mockProvider = Mockito.mock(AiProvider.class);
        cvDocumentRepository = Mockito.mock(CvDocumentRepository.class);
        cvProfileRepository = Mockito.mock(CvProfileRepository.class);

        ModelProfile profile = ModelProfile.builder()
                .id("qwen3-8b")
                .provider(ModelProviderType.OLLAMA)
                .providerModelName("qwen3:8b")
                .build();

        when(modelManager.getDefaultModel()).thenReturn(profile);
        when(modelManager.getProvider(ModelProviderType.OLLAMA)).thenReturn(mockProvider);
    }

    @Test
    @DisplayName("CvAnalysisTool skips safely without failing when no CV is attached")
    void testCvAnalysisToolSkippedWithoutCv() {
        CvAnalysisTool tool = new CvAnalysisTool(cvDocumentRepository, cvProfileRepository);

        AgentState state = AgentState.builder()
                .runId("run-no-cv")
                .cvId(null)
                .build();

        AgentToolResult result = tool.execute(AgentToolContext.builder().state(state).build());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getStatus()).isEqualTo("skipped");
        assertThat(result.getSummary()).contains("No candidate CV was supplied");
    }

    @Test
    @DisplayName("CvAnalysisTool loads verified profile when valid CV is attached")
    void testCvAnalysisToolWithCv() {
        CvAnalysisTool tool = new CvAnalysisTool(cvDocumentRepository, cvProfileRepository);

        CvDocumentEntity doc = CvDocumentEntity.builder()
                .id(50L)
                .originalFilename("resume.pdf")
                .status("COMPLETED")
                .build();

        CvProfileEntity profile = CvProfileEntity.builder()
                .id(1L)
                .document(doc)
                .skillsJson("[\"Java\", \"Spring Boot\", \"MySQL\"]")
                .educationJson("[{\"institution\": \"Tokyo Tech\", \"degree\": \"B.S. CS\"}]")
                .workExperienceJson("[{\"company\": \"Rakuten\", \"role\": \"Backend Engineer\"}]")
                .build();

        when(cvDocumentRepository.findById(50L)).thenReturn(Optional.of(doc));
        when(cvProfileRepository.findByDocumentId(50L)).thenReturn(Optional.of(profile));

        AgentState state = AgentState.builder()
                .runId("run-with-cv")
                .cvId(50L)
                .build();

        AgentToolResult result = tool.execute(AgentToolContext.builder().state(state).build());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getObservationKey()).isEqualTo("candidateProfile");
        assertThat(result.getData()).isInstanceOf(Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertThat(data.get("skills")).asList().contains("Java", "Spring Boot", "MySQL");
        assertThat(data.get("education")).asList().isNotEmpty();
    }

    @Test
    @DisplayName("JobAnalysisTool extracts requirements using fallback when model fails")
    void testJobAnalysisToolDeterministicFallback() {
        when(mockProvider.generate(any(ChatGenerationRequest.class)))
                .thenThrow(new RuntimeException("Ollama unavailable"));

        JobAnalysisTool tool = new JobAnalysisTool(modelManager);

        AgentState state = AgentState.builder()
                .companyName("LINE Yahoo")
                .jobPosition("Backend Engineer")
                .jobDescription("Java, Spring Boot, REST API, SQL, Git, team development, Japanese communication, cloud preferred")
                .language("ja")
                .build();

        AgentToolResult result = tool.execute(AgentToolContext.builder().state(state).build());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getObservationKey()).isEqualTo("jobAnalysis");

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertThat(data.get("requiredSkills")).asList().isNotEmpty();
        assertThat(data.get("responsibilities")).asList().isNotEmpty();
    }

    @Test
    @DisplayName("GapAnalysisTool strictly isolates matching skills from missing gaps")
    void testGapAnalysisToolEvidenceRule() {
        when(mockProvider.generate(any(ChatGenerationRequest.class)))
                .thenThrow(new RuntimeException("Fallback trigger"));

        GapAnalysisTool tool = new GapAnalysisTool(modelManager);

        AgentState state = AgentState.builder()
                .companyName("LINE Yahoo")
                .jobPosition("Backend Engineer")
                .build();

        // Candidate knows Java and Spring Boot
        state.addObservation("candidateProfile", Map.of(
                "skills", List.of("Java", "Spring Boot", "MySQL")
        ));

        // Job requires Java, Spring Boot, SQL, AWS, Kubernetes
        state.addObservation("jobAnalysis", Map.of(
                "requiredSkills", List.of("Java", "Spring Boot", "SQL", "AWS", "Kubernetes"),
                "preferredSkills", List.of("Cloud Infrastructure")
        ));

        AgentToolResult result = tool.execute(AgentToolContext.builder().state(state).build());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getObservationKey()).isEqualTo("gapAnalysis");

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        List<?> matching = (List<?>) data.get("matchingSkills");
        List<?> missing = (List<?>) data.get("missingSkills");

        assertThat(matching).asList().contains("Java", "Spring Boot");
        assertThat(missing).asList().contains("AWS", "Kubernetes");
        // AWS must not be marked matching because candidate has no evidence
        assertThat(matching).asList().doesNotContain("AWS");
    }

    @Test
    @DisplayName("QuestionGenerationTool produces 5 questions targeting missing gaps")
    void testQuestionGenerationToolProduces5Questions() {
        when(mockProvider.generate(any(ChatGenerationRequest.class)))
                .thenThrow(new RuntimeException("Fallback trigger"));

        QuestionGenerationTool tool = new QuestionGenerationTool(modelManager);

        AgentState state = AgentState.builder()
                .companyName("LINE Yahoo")
                .jobPosition("Backend Engineer")
                .language("ja")
                .build();

        state.addObservation("gapAnalysis", Map.of(
                "missingSkills", List.of("AWS", "Kubernetes"),
                "strongAreas", List.of("Java", "Spring Boot")
        ));

        AgentToolResult result = tool.execute(AgentToolContext.builder().state(state).build());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getObservationKey()).isEqualTo("practiceQuestions");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> questions = (List<Map<String, Object>>) result.getData();
        assertThat(questions).hasSize(5);

        for (Map<String, Object> q : questions) {
            assertThat(q.get("question")).isNotNull();
            assertThat(q.get("category")).isNotNull();
            assertThat(q.get("difficulty")).isNotNull();
            assertThat(q.get("whyThisQuestionMatters")).isNotNull();
            assertThat(q.get("preparationHint")).isNotNull();
        }
    }
}
