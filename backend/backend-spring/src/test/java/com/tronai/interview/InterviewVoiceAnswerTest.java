package com.tronai.interview;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tronai.ai.AiProvider;
import com.tronai.ai.ChatGenerationResult;
import com.tronai.interview.dto.InterviewConfigDto;
import com.tronai.interview.dto.InterviewStartResponse;
import com.tronai.stt.SpeechToTextProvider;
import com.tronai.stt.SttTranscriptionResult;
import com.tronai.tts.TextToSpeechProvider;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@org.springframework.security.test.context.support.WithMockUser
class InterviewVoiceAnswerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.tronai.auth.JwtService jwtService;

    @Autowired
    private com.tronai.user.UserRepository userRepository;

    private String authHeader;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private SpeechToTextProvider speechToTextProvider;

    @MockitoBean
    private TextToSpeechProvider textToSpeechProvider;

    @MockitoBean
    private AiProvider aiProvider;

    private static final byte[] FAKE_WAV = new byte[]{
            'R', 'I', 'F', 'F', 36, 0, 0, 0, 'W', 'A', 'V', 'E', 'f', 'm', 't', ' '
    };

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        com.tronai.user.User u = userRepository.findByEmailIgnoreCase("voice-tester@tron.ai")
                .orElseGet(() -> userRepository.save(com.tronai.user.User.builder()
                        .email("voice-tester@tron.ai")
                        .displayName("Voice Tester")
                        .role("USER")
                        .enabled(true)
                        .createdAt(java.time.Instant.now())
                        .updatedAt(java.time.Instant.now())
                        .build()));
        authHeader = "Bearer " + jwtService.generateAccessToken(u.getId(), u.getEmail(), u.getRole(), u.getDisplayName());
        org.mockito.Mockito.lenient().when(textToSpeechProvider.isAvailable()).thenReturn(true);
    }

    @Test
    @DisplayName("Voice answer advances interview question and produces synthesized question audio")
    void voiceAnswerAdvancesQuestion() throws Exception {
        when(aiProvider.generate(any()))
                .thenReturn(ChatGenerationResult.builder()
                        .text("これまでに最も困難だった技術的な課題は何ですか？")
                        .truncated(false)
                        .build());

        when(speechToTextProvider.transcribe(any(), any(), any(), any(), any()))
                .thenReturn(new SttTranscriptionResult("大学でマイクロサービスの開発を経験しました。", 3.0, "ja"));

        when(textToSpeechProvider.synthesize(anyString())).thenReturn(FAKE_WAV);

        // 1. Start interview with 2 questions
        InterviewConfigDto config = new InterviewConfigDto();
        config.setQuestionCount(2);
        config.setCompanyName("LINEヤフー");
        config.setJobPosition("バックエンドエンジニア");

        MvcResult startResult = mockMvc.perform(post("/api/v1/interviews/start")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(config)))
                .andExpect(status().isOk())
                .andReturn();

        InterviewStartResponse startResponse = objectMapper.readValue(
                startResult.getResponse().getContentAsString(), InterviewStartResponse.class
        );
        String interviewId = startResponse.getInterviewId();

        // 2. Submit voice answer for Q0
        MockMultipartFile file = new MockMultipartFile(
                "file", "answer.wav", "audio/wav", FAKE_WAV
        );

        mockMvc.perform(multipart("/api/v1/interviews/{id}/voice-answer", interviewId)
                        .file(file)
                        .header("Authorization", authHeader)
                        .param("question_index", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interviewId").value(interviewId))
                .andExpect(jsonPath("$.transcript").value("大学でマイクロサービスの開発を経験しました。"))
                .andExpect(jsonPath("$.nextQuestionIndex").value(1))
                .andExpect(jsonPath("$.isFinished").value(false))
                .andExpect(jsonPath("$.audio_url").isNotEmpty())
                .andExpect(jsonPath("$.audioContentType").value("audio/wav"))
                .andExpect(jsonPath("$.latency_ms.stt").exists())
                .andExpect(jsonPath("$.latency_ms.llm").exists())
                .andExpect(jsonPath("$.latency_ms.tts").exists());

        // 3. Stale index check: submitting Q0 again must fail with 409 Conflict
        mockMvc.perform(multipart("/api/v1/interviews/{id}/voice-answer", interviewId)
                        .file(file)
                        .header("Authorization", authHeader)
                        .param("question_index", "0"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("This interview turn was already submitted"));

        // 4. Submit voice answer for Q1 -> completes the 2-question interview
        mockMvc.perform(multipart("/api/v1/interviews/{id}/voice-answer", interviewId)
                        .file(file)
                        .header("Authorization", authHeader)
                        .param("question_index", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextQuestionIndex").value(2))
                .andExpect(jsonPath("$.isFinished").value(true))
                .andExpect(jsonPath("$.audio_url").value(""));
    }

    @Test
    @DisplayName("Voice answer rejects empty audio with HTTP 400")
    void voiceAnswerRejectsEmptyAudio() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.wav", "audio/wav", new byte[0]
        );

        mockMvc.perform(multipart("/api/v1/interviews/{id}/voice-answer", "test-session")
                        .file(file)
                        .header("Authorization", authHeader)
                        .param("question_index", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("The audio file is empty"));
    }

    @Test
    @DisplayName("Voice answer returns 404 for nonexistent interview")
    void voiceAnswerUnknownInterview404() throws Exception {
        when(speechToTextProvider.transcribe(any(), any(), any(), any(), any()))
                .thenReturn(new SttTranscriptionResult("回答テキスト", 2.0, "ja"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "answer.wav", "audio/wav", FAKE_WAV
        );

        mockMvc.perform(multipart("/api/v1/interviews/{id}/voice-answer", "unknown-id")
                        .file(file)
                        .header("Authorization", authHeader)
                        .param("question_index", "0"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Interview session was not found"));
    }
}
