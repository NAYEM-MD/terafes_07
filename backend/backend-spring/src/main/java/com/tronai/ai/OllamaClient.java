package com.tronai.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Ollama HTTP Client using Spring RestClient.
 */
@Component
public class OllamaClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaClient.class);

    private static final Pattern THINK_BLOCK = Pattern.compile("<think>.*?</think>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern THINK_OPEN = Pattern.compile("<think>.*", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private final OllamaProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Autowired
    public OllamaClient(OllamaProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()));

        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    public OllamaClient(OllamaProperties properties) {
        this(properties, new ObjectMapper());
    }

    /**
     * Check whether the local Ollama instance is healthy and responding.
     */
    public boolean checkHealth() {
        try {
            SimpleClientHttpRequestFactory quickFactory = new SimpleClientHttpRequestFactory();
            quickFactory.setConnectTimeout(Duration.ofSeconds(2));
            quickFactory.setReadTimeout(Duration.ofSeconds(2));

            RestClient quickClient = RestClient.builder()
                    .baseUrl(properties.getBaseUrl())
                    .requestFactory(quickFactory)
                    .build();

            quickClient.get()
                    .uri("/api/tags")
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.debug("Ollama health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Execute chat completion via Ollama /api/chat.
     */
    public ChatGenerationResult chat(ChatGenerationRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        String targetModel = (request.getModel() != null && !request.getModel().isBlank())
                ? request.getModel().trim()
                : properties.getModel();
        payload.put("model", targetModel);

        List<Map<String, String>> msgList = new ArrayList<>();
        if (request.getMessages() != null) {
            for (ChatMessage m : request.getMessages()) {
                msgList.add(Map.of("role", m.getRole(), "content", m.getContent()));
            }
        }
        payload.put("messages", msgList);
        payload.put("think", false);
        payload.put("stream", false);
        payload.put("keep_alive", "30m");

        int numPredict = request.getNumPredict() != null ? request.getNumPredict() : 1024;
        double temperature = request.getTemperature() != null ? request.getTemperature() : 0.4;

        Map<String, Object> options = new LinkedHashMap<>();
        options.put("num_predict", numPredict);
        options.put("temperature", temperature);
        options.put("num_ctx", properties.getNumCtx());
        options.put("repeat_penalty", 1.15);
        options.put("repeat_last_n", 128);
        payload.put("options", options);

        if (request.isJsonMode()) {
            payload.put("format", "json");
        }

        try {
            String rawJson = restClient.post()
                    .uri("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON, MediaType.ALL)
                    .body(payload)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        log.error("Ollama /api/chat returned status {}", resp.getStatusCode());
                        throw new AiProviderUnavailableException("Ollama request failed with status: " + resp.getStatusCode());
                    })
                    .body(String.class);

            if (rawJson == null || rawJson.isBlank()) {
                throw new AiProviderUnavailableException("Ollama returned an empty response body");
            }

            Map<String, Object> responseBody = objectMapper.readValue(rawJson, Map.class);

            Map<String, Object> message = (Map<String, Object>) responseBody.get("message");
            String rawContent = message != null && message.get("content") != null
                    ? message.get("content").toString()
                    : "";
            // Qwen3 may put tokens in "thinking" when content is blank despite think=false.
            if ((rawContent == null || rawContent.isBlank()) && message != null && message.get("thinking") != null) {
                rawContent = message.get("thinking").toString();
            }

            String spokenText = cleanSpokenText(rawContent);
            if (spokenText.isBlank() && rawContent != null && !rawContent.isBlank()) {
                spokenText = rawContent.replaceAll("\\s+", " ").trim();
            }

            String reason = responseBody.get("done_reason") != null
                    ? responseBody.get("done_reason").toString()
                    : "stop";

            int evalCount = responseBody.get("eval_count") instanceof Number
                    ? ((Number) responseBody.get("eval_count")).intValue()
                    : 0;

            int promptEvalCount = responseBody.get("prompt_eval_count") instanceof Number
                    ? ((Number) responseBody.get("prompt_eval_count")).intValue()
                    : 0;

            boolean truncated = "length".equalsIgnoreCase(reason) || (numPredict > 0 && evalCount >= numPredict);
            String finishReason = truncated ? "length" : ("stop".equalsIgnoreCase(reason) ? "stop" : reason);

            return ChatGenerationResult.builder()
                    .text(spokenText)
                    .finishReason(finishReason)
                    .truncated(truncated)
                    .evalCount(evalCount)
                    .promptEvalCount(promptEvalCount)
                    .build();

        } catch (ResourceAccessException e) {
            if (e.getCause() instanceof SocketTimeoutException) {
                log.warn("Ollama chat timed out after {}s", properties.getTimeoutSeconds());
                throw new AiProviderTimeoutException("Ollama timed out", e);
            }
            log.warn("Ollama connection failed: {}", e.getMessage());
            throw new AiProviderUnavailableException("Ollama is unavailable", e);
        } catch (RestClientResponseException e) {
            log.warn("Ollama HTTP error {}: {}", e.getStatusCode(), e.getMessage());
            throw new AiProviderUnavailableException("Ollama is unavailable: " + e.getStatusCode(), e);
        } catch (org.springframework.web.client.RestClientException e) {
            log.warn("Ollama RestClient error: {}", e.getMessage());
            throw new AiProviderUnavailableException("Ollama request failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.warn("Ollama processing error: {}", e.getMessage());
            throw new AiProviderUnavailableException("Ollama error: " + e.getMessage(), e);
        }
    }

    public static String cleanSpokenText(String content) {
        if (content == null) {
            return "";
        }
        String text = THINK_BLOCK.matcher(content).replaceAll("");
        text = THINK_OPEN.matcher(text).replaceAll("").trim();
        return text.isEmpty() ? content.trim() : text;
    }
}
