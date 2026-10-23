package com.tronai.ai.model;

import com.tronai.ai.AiProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ModelEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiProvider aiProvider;

    @BeforeEach
    void setUp() {
        lenient().when(aiProvider.getProviderName()).thenReturn("ollama");
    }

    @Test
    @DisplayName("GET /api/v1/models returns HTTP 200 with registered model metadata and availability true")
    void getModelsReturns200WithMetadata() throws Exception {
        when(aiProvider.isAvailable()).thenReturn(true);

        mockMvc.perform(get("/api/v1/models").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.models").isArray())
                .andExpect(jsonPath("$.models[?(@.id == 'qwen3-8b')].displayName").value("Qwen3 8B"))
                .andExpect(jsonPath("$.models[?(@.id == 'qwen3-8b')].provider").value("OLLAMA"))
                .andExpect(jsonPath("$.models[?(@.id == 'qwen3-8b')].enabled").value(true))
                .andExpect(jsonPath("$.models[?(@.id == 'qwen3-8b')].available").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/models reflects available=false when Ollama is unreachable")
    void getModelsReflectsUnavailableWhenProviderFails() throws Exception {
        when(aiProvider.isAvailable()).thenReturn(false);

        mockMvc.perform(get("/api/v1/models").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.models[0].available").value(false));
    }
}
