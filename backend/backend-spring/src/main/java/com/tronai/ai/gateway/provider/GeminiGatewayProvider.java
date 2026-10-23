package com.tronai.ai.gateway.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tronai.ai.ChatMessage;
import com.tronai.ai.gateway.AiGatewayProperties;
import com.tronai.ai.gateway.AiGenerationResult;
import com.tronai.ai.gateway.AiRequestContext;
import com.tronai.ai.gateway.GatewayProviderId;
import com.tronai.ai.gateway.ProviderHealthStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Component
public class GeminiGatewayProvider implements GatewayProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiGatewayProvider.class);
    private static final String BASE = "https://generativelanguage.googleapis.com/v1beta";

    private final AiGatewayProperties properties;
    private final ObjectMapper objectMapper;
    private final AtomicInteger keyRotation = new AtomicInteger(0);

    public GeminiGatewayProvider(AiGatewayProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    @Override
    public GatewayProviderId id() {
        return GatewayProviderId.GEMINI;
    }

    @Override
    public boolean isConfigured() {
        return !apiKeys().isEmpty();
    }

    @Override
    public ProviderHealthStatus health() {
        return isConfigured() ? ProviderHealthStatus.CONFIGURED : ProviderHealthStatus.DISABLED;
    }

    @Override
    public List<String> knownModels() {
        return Stream.of(properties.getGeminiModelSmart(), properties.getGeminiModelUltra())
                .filter(GeminiGatewayProvider::hasText)
                .distinct()
                .toList();
    }

    @Override
    public AiGenerationResult generate(AiRequestContext context, String modelId) {
        List<String> keys = apiKeys();
        if (keys.isEmpty()) {
            throw new GatewayProviderException(GatewayProviderException.UNAVAILABLE, 503, "Gemini is not configured");
        }
        String model = hasText(modelId) ? modelId : properties.getGeminiModelSmart();
        int start = Math.floorMod(keyRotation.getAndIncrement(), keys.size());
        GatewayProviderException last = null;
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get((start + i) % keys.size());
            try {
                return callGemini(context, model, key);
            } catch (GatewayProviderException e) {
                last = e;
                if (!GatewayProviderException.RATE_LIMITED.equals(e.getCategory())
                        && !GatewayProviderException.INVALID_CREDENTIAL.equals(e.getCategory())) {
                    throw e;
                }
            }
        }
        throw last != null ? last : new GatewayProviderException(
                GatewayProviderException.UNAVAILABLE, 503, "Gemini request failed");
    }

    @SuppressWarnings("unchecked")
    private AiGenerationResult callGemini(AiRequestContext context, String model, String apiKey) {
        long startMs = System.currentTimeMillis();
        int timeout = properties.getRequestTimeoutSeconds();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(Math.min(10, timeout)));
        factory.setReadTimeout(Duration.ofSeconds(timeout));

        RestClient client = RestClient.builder()
                .baseUrl(BASE)
                .requestFactory(factory)
                .build();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("contents", toGeminiContents(context.getMessages()));
        Map<String, Object> generationConfig = new LinkedHashMap<>();
        if (context.getTemperature() != null) {
            generationConfig.put("temperature", context.getTemperature());
        }
        if (context.getMaxTokens() != null && context.getMaxTokens() > 0) {
            generationConfig.put("maxOutputTokens", context.getMaxTokens());
        }
        if (context.isJsonMode()) {
            generationConfig.put("responseMimeType", "application/json");
        }
        if (!generationConfig.isEmpty()) {
            body.put("generationConfig", generationConfig);
        }

        try {
            String raw = client.post()
                    .uri("/models/{model}:generateContent?key={key}", model, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            long latency = System.currentTimeMillis() - startMs;
            Map<String, Object> root = objectMapper.readValue(raw, Map.class);
            String text = extractGeminiText(root);
            String finishReason = extractFinishReason(root);
            int promptTokens = extractUsage(root, "promptTokenCount");
            int completionTokens = extractUsage(root, "candidatesTokenCount");

            AiGenerationResult result = AiGenerationResult.builder()
                    .text(text)
                    .finishReason(finishReason)
                    .truncated("MAX_TOKENS".equalsIgnoreCase(finishReason))
                    .promptTokens(promptTokens)
                    .completionTokens(completionTokens)
                    .latencyMs(latency)
                    .provider(GatewayProviderId.GEMINI)
                    .modelId(model)
                    .mode(context.getMode())
                    .feature(context.getFeature())
                    .requestId(context.getRequestId())
                    .build();
            return result;
        } catch (RestClientResponseException e) {
            throw mapHttpError(e);
        } catch (ResourceAccessException e) {
            if (e.getCause() instanceof SocketTimeoutException) {
                throw new GatewayProviderException(GatewayProviderException.TIMEOUT, 408, "Gemini timed out", e);
            }
            throw new GatewayProviderException(GatewayProviderException.UNAVAILABLE, 503, "Gemini unreachable", e);
        } catch (GatewayProviderException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Gemini request failed: {}", e.getMessage());
            throw new GatewayProviderException(GatewayProviderException.UPSTREAM_ERROR, 502, "Gemini request failed", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static String extractGeminiText(Map<String, Object> root) {
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) root.get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            return "";
        }
        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        if (content == null) {
            return "";
        }
        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
        if (parts == null || parts.isEmpty() || parts.get(0).get("text") == null) {
            return "";
        }
        return parts.get(0).get("text").toString();
    }

    @SuppressWarnings("unchecked")
    private static String extractFinishReason(Map<String, Object> root) {
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) root.get("candidates");
        if (candidates == null || candidates.isEmpty() || candidates.get(0).get("finishReason") == null) {
            return "STOP";
        }
        return candidates.get(0).get("finishReason").toString();
    }

    @SuppressWarnings("unchecked")
    private static int extractUsage(Map<String, Object> root, String field) {
        Map<String, Object> usage = (Map<String, Object>) root.get("usageMetadata");
        if (usage != null && usage.get(field) instanceof Number n) {
            return n.intValue();
        }
        return 0;
    }

    private static List<Map<String, Object>> toGeminiContents(List<ChatMessage> messages) {
        List<Map<String, Object>> contents = new ArrayList<>();
        if (messages == null) {
            return contents;
        }
        for (ChatMessage message : messages) {
            if (message == null) {
                continue;
            }
            String role = message.getRole() != null ? message.getRole().trim().toLowerCase() : "user";
            String geminiRole = "assistant".equals(role) || "model".equals(role) ? "model" : "user";
            String text = message.getContent() != null ? message.getContent() : "";
            contents.add(Map.of(
                    "role", geminiRole,
                    "parts", List.of(Map.of("text", text))));
        }
        return contents;
    }

    private List<String> apiKeys() {
        List<String> keys = new ArrayList<>(4);
        addKey(keys, properties.getGeminiApiKey1());
        addKey(keys, properties.getGeminiApiKey2());
        addKey(keys, properties.getGeminiApiKey3());
        addKey(keys, properties.getGeminiApiKey4());
        return keys;
    }

    private static void addKey(List<String> keys, String value) {
        if (hasText(value)) {
            keys.add(value.trim());
        }
    }

    private GatewayProviderException mapHttpError(RestClientResponseException e) {
        int status = e.getStatusCode().value();
        String snippet = e.getResponseBodyAsString();
        if (snippet != null && snippet.length() > 200) {
            snippet = snippet.substring(0, 200) + "…";
        }
        if (status == 401 || status == 403) {
            return new GatewayProviderException(
                    GatewayProviderException.INVALID_CREDENTIAL, status, "Invalid Gemini credentials");
        }
        if (status == 429) {
            return new GatewayProviderException(GatewayProviderException.RATE_LIMITED, status, "Gemini rate limited");
        }
        if (status == 402) {
            return new GatewayProviderException(GatewayProviderException.PAYMENT_REQUIRED, status, "Gemini payment required");
        }
        if (status == 400) {
            return new GatewayProviderException(GatewayProviderException.BAD_REQUEST, status, "Invalid Gemini request: " + snippet);
        }
        return new GatewayProviderException(GatewayProviderException.UPSTREAM_ERROR, status, "Gemini HTTP error");
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
