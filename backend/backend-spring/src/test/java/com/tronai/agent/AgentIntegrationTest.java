package com.tronai.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tronai.agent.dto.AgentDtos;
import com.tronai.ai.AiProvider;
import com.tronai.ai.ChatGenerationResult;
import com.tronai.auth.dto.RegisterRequest;
import com.tronai.cv.PdfTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AgentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiProvider aiProvider;

    @MockitoBean
    private com.tronai.storage.ObjectStorageService objectStorageService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(aiProvider.getProviderName()).thenReturn("ollama");
        lenient().when(aiProvider.isAvailable()).thenReturn(true);
        lenient().when(aiProvider.generate(any())).thenReturn(ChatGenerationResult.builder().text("{}").build());

        // Register User A
        RegisterRequest reqA = RegisterRequest.builder()
                .email("agent-user-a-" + System.nanoTime() + "@tron.ai")
                .password("passwordA123")
                .displayName("Agent User A")
                .build();
        MvcResult resA = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqA)))
                .andExpect(status().isOk())
                .andReturn();
        tokenA = "Bearer " + objectMapper.readTree(resA.getResponse().getContentAsString()).get("accessToken").asText();

        // Register User B
        RegisterRequest reqB = RegisterRequest.builder()
                .email("agent-user-b-" + System.nanoTime() + "@tron.ai")
                .password("passwordB123")
                .displayName("Agent User B")
                .build();
        MvcResult resB = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqB)))
                .andExpect(status().isOk())
                .andReturn();
        tokenB = "Bearer " + objectMapper.readTree(resB.getResponse().getContentAsString()).get("accessToken").asText();
    }

    @Test
    @DisplayName("User runs autonomous agent preparation without CV and obtains multi-step results")
    void runAgentWithoutCvSuccessfully() throws Exception {
        AgentDtos.AgentRunRequest request = AgentDtos.AgentRunRequest.builder()
                .goal("Prepare me for a Backend Engineer interview at LINE Yahoo.")
                .companyName("LINE Yahoo")
                .jobPosition("Backend Engineer")
                .jobDescription("Java, Spring Boot, REST APIs, SQL, Git, team development, Japanese communication, cloud preferred")
                .language("ja")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/agents/interview-prep/run")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(root.has("runId")).isTrue();
        assertThat(root.get("status").asText()).isEqualTo("completed");
        assertThat(root.get("goal").asText()).contains("LINE Yahoo");

        // Verify multi-step execution trace
        JsonNode steps = root.get("steps");
        assertThat(steps.isArray()).isTrue();
        assertThat(steps.size()).isGreaterThanOrEqualTo(4);

        // Verify result schema
        JsonNode resObj = root.get("result");
        assertThat(resObj.has("candidateStrengths")).isTrue();
        assertThat(resObj.has("skillGaps")).isTrue();
        assertThat(resObj.has("priorityTopics")).isTrue();
        assertThat(resObj.has("interviewStrategy")).isTrue();
        assertThat(resObj.has("practiceQuestions")).isTrue();

        // Verify metrics
        JsonNode metrics = root.get("metrics");
        assertThat(metrics.get("stepsExecuted").asInt()).isGreaterThanOrEqualTo(4);
        assertThat(metrics.get("toolsUsed").size()).isGreaterThanOrEqualTo(3);

        // Security / Privacy: Ensure no chain-of-thought or raw hidden prompts are exposed
        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody).doesNotContain("chain-of-thought");
        assertThat(responseBody).doesNotContain("systemPrompt");
        assertThat(responseBody).doesNotContain("ALLOWLISTED ACTIONS");
    }

    @Test
    @DisplayName("User cannot run agent using another user's CV")
    void cannotUseAnotherUsersCv() throws Exception {
        // User A uploads CV
        byte[] pdfBytes = PdfTestUtils.createPdf("John Doe\nSkills: Java, Spring Boot, MySQL", 1);
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", pdfBytes);

        MvcResult cvRes = mockMvc.perform(multipart("/api/cv/upload")
                        .file(file)
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andReturn();

        long cvId = objectMapper.readTree(cvRes.getResponse().getContentAsString()).get("cvId").asLong();

        // User B attempts to run agent with User A's CV ID
        AgentDtos.AgentRunRequest request = AgentDtos.AgentRunRequest.builder()
                .goal("Prepare for interview")
                .companyName("LINE Yahoo")
                .jobPosition("Backend Engineer")
                .jobDescription("Java")
                .cvId(cvId)
                .build();

        mockMvc.perform(post("/api/v1/agents/interview-prep/run")
                        .header("Authorization", tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("CV not found or access denied"));
    }

    @Test
    @DisplayName("User B cannot access or view User A's Agent Run history or details")
    void userIsolationOnHistory() throws Exception {
        // User A creates a run
        AgentDtos.AgentRunRequest request = AgentDtos.AgentRunRequest.builder()
                .goal("Prepare for LINE Yahoo")
                .companyName("LINE Yahoo")
                .jobPosition("Backend Engineer")
                .jobDescription("Spring Boot, SQL")
                .build();

        MvcResult res = mockMvc.perform(post("/api/v1/agents/interview-prep/run")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String runId = objectMapper.readTree(res.getResponse().getContentAsString()).get("runId").asText();

        // User A can access
        mockMvc.perform(get("/api/v1/agents/runs/" + runId)
                        .header("Authorization", tokenA))
                .andExpect(status().isOk());

        // User B cannot access User A's run
        mockMvc.perform(get("/api/v1/agents/runs/" + runId)
                        .header("Authorization", tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Phase Q: User can launch an interview directly from an Agent Run result")
    void startInterviewFromAgentRun() throws Exception {
        // User A creates a run
        AgentDtos.AgentRunRequest request = AgentDtos.AgentRunRequest.builder()
                .goal("Prepare for LINE Yahoo")
                .companyName("LINE Yahoo")
                .jobPosition("Backend Engineer")
                .jobDescription("Java, Spring Boot, MySQL, Cloud")
                .language("ja")
                .build();

        MvcResult res = mockMvc.perform(post("/api/v1/agents/interview-prep/run")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String runId = objectMapper.readTree(res.getResponse().getContentAsString()).get("runId").asText();

        // Launch interview from run
        MvcResult interviewRes = mockMvc.perform(post("/api/v1/agents/runs/" + runId + "/start-interview")
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode iRoot = objectMapper.readTree(interviewRes.getResponse().getContentAsString());
        assertThat(iRoot.has("interviewId")).isTrue();
        assertThat(iRoot.has("questions")).isTrue();
        assertThat(iRoot.has("transcript")).isTrue();
        assertThat(iRoot.get("questions").isArray()).isTrue();
        assertThat(iRoot.get("transcript").isArray()).isTrue();
    }

    @Test
    @DisplayName("Agent run with QUALITY profile locks profile and returns locked=true")
    void testAgentRunWithQualityProfileLocks() throws Exception {
        AgentDtos.AgentRunRequest request = AgentDtos.AgentRunRequest.builder()
                .goal("Prepare for Sony Backend")
                .companyName("Sony")
                .jobPosition("Backend Engineer")
                .jobDescription("Distributed systems and Java 21")
                .language("ja")
                .aiProfile("QUALITY")
                .build();

        MvcResult res = mockMvc.perform(post("/api/v1/agents/interview-prep/run")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aiProfile").value("QUALITY"))
                .andExpect(jsonPath("$.locked").value(true))
                .andExpect(jsonPath("$.metrics.aiProfile").value("QUALITY"))
                .andReturn();

        String runId = objectMapper.readTree(res.getResponse().getContentAsString()).get("runId").asText();

        // Get run detail also returns QUALITY and locked
        mockMvc.perform(get("/api/v1/agents/runs/" + runId)
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aiProfile").value("QUALITY"))
                .andExpect(jsonPath("$.locked").value(true));
    }
}
