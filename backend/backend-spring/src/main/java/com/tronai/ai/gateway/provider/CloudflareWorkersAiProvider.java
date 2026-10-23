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
import java.util.stream.Stream;

@Component
public class CloudflareWorkersAiProvider implements GatewayProvider {

    private static final Logger log = LoggerFactory.getLogger(CloudflareWorkersAiProvider.class);
    private static final String BASE = "https://api.cloudflare.com/client/v4";

    private final AiGatewayProperties properties;
    private final ObjectMapper objectMapper;

    public CloudflareWorkersAiProvider(AiGatewayProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    @Override
    public GatewayProviderId id() {
        return GatewayProviderId.CLOUDFLARE_WORKERS_AI;
    }

    @Override
    public boolean isConfigured() {
        return hasText(properties.getCloudflareApiToken()) && hasText(properties.getCloudflareAccountId());
    }

    @Override
    public ProviderHealthStatus health() {
        if (!hasText(properties.getCloudflareApiToken()) && !hasText(properties.getCloudflareAccountId())) {
            return ProviderHealthStatus.DISABLED;
        }
        if (hasText(properties.getCloudflareApiToken()) && !hasText(properties.getCloudflareAccountId())) {
            return ProviderHealthStatus.UNAVAILABLE;
        }
        return ProviderHealthStatus.CONFIGURED;
    }

    @Override
    public String healthDetail() {
        if (hasText(properties.getCloudflareApiToken()) && !hasText(properties.getCloudflareAccountId())) {
            return "Cloudflare API token is set but account id is missing";
        }
        return "";
    }

    @Override
    public List<String> knownModels() {
        return Stream.of(properties.getCloudflareModelFast())
                .filter(CloudflareWorkersAiProvider::hasText)
                .distinct()
                .toList();
    }

    @Override
    public AiGenerationResult generate(AiRequestContext context, String modelId) {
        if (!isConfigured()) {
            if (hasText(properties.getCloudflareApiToken()) && !hasText(properties.getCloudflareAccountId())) {
                throw new GatewayProviderException(
                        GatewayProviderException.UNAVAILABLE, 503, "Cloudflare account id is missing");
            }
            throw new GatewayProviderException(GatewayProviderException.UNAVAILABLE, 503, "Cloudflare Workers AI is not configured");
        }
        String model = hasText(modelId) ? modelId : properties.getCloudflareModelFast();
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
        body.put("messages", toMessages(context.getMessages()));
        if (context.getMaxTokens() != null && context.getMaxTokens() > 0) {
            body.put("max_tokens", context.getMaxTokens());
        }

        try {
            String raw = client.post()
                    .uri("/accounts/{accountId}/ai/run/{model}",
                            properties.getCloudflareAccountId().trim(),
                            model)
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(h -> h.setBearerAuth(properties.getCloudflareApiToken().trim()))
                    .body(body)
                    .retrieve()
                    .body(String.class);

            long latency = System.currentTimeMillis() - startMs;
            return parseResponse(raw, context, model, latency);
        } catch (RestClientResponseException e) {
            throw mapHttpError(e);
        } catch (ResourceAccessException e) {
            if (e.getCause() instanceof SocketTimeoutException) {
                throw new GatewayProviderException(GatewayProviderException.TIMEOUT, 408, "Cloudflare AI timed out", e);
            }
            throw new GatewayProviderException(GatewayProviderException.UNAVAILABLE, 503, "Cloudflare AI unreachable", e);
        } catch (GatewayProviderException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Cloudflare Workers AI request failed: {}", e.getMessage());
            throw new GatewayProviderException(GatewayProviderException.UPSTREAM_ERROR, 502, "Cloudflare AI request failed", e);
        }
    }

    @SuppressWarnings("unchecked")
    private AiGenerationResult parseResponse(String raw, AiRequestContext context, String model, long latency)
            throws Exception {
        if (raw == null || raw.isBlank()) {
            throw new GatewayProviderException(GatewayProviderException.UPSTREAM_ERROR, 502, "Empty Cloudflare response");
        }
        Map<String, Object> root = objectMapper.readValue(raw, Map.class);
        Object resultObj = root.get("result");
        String text = "";
        if (resultObj instanceof Map<?, ?> result) {
            if (result.get("response") != null) {
                text = result.get("response").toString();
            } else if (result.get("result") != null) {
                text = result.get("result").toString();
            }
        } else if (resultObj != null) {
            text = resultObj.toString();
        }
        return AiGenerationResult.builder()
                .text(text)
                .finishReason("stop")
                .latencyMs(latency)
                .provider(GatewayProviderId.CLOUDFLARE_WORKERS_AI)
                .modelId(model)
                .mode(context.getMode())
                .feature(context.getFeature())
                .requestId(context.getRequestId())
                .build();
    }

    private static List<Map<String, String>> toMessages(List<ChatMessage> messages) {
        List<Map<String, String>> out = new ArrayList<>();
        if (messages == null) {
            return out;
        }
        for (ChatMessage m : messages) {
            if (m == null) {
                continue;
            }
            out.add(Map.of(
                    "role", m.getRole() != null ? m.getRole() : "user",
                    "content", m.getContent() != null ? m.getContent() : ""));
        }
        return out;
    }

    private GatewayProviderException mapHttpError(RestClientResponseException e) {
        int status = e.getStatusCode().value();
        if (status == 401 || status == 403) {
            return new GatewayProviderException(
                    GatewayProviderException.INVALID_CREDENTIAL, status, "Invalid Cloudflare credentials");
        }
        if (status == 429) {
            return new GatewayProviderException(GatewayProviderException.RATE_LIMITED, status, "Cloudflare AI rate limited");
        }
        if (status == 402) {
            return new GatewayProviderException(GatewayProviderException.PAYMENT_REQUIRED, status, "Cloudflare payment required");
        }
        if (status == 400) {
            return new GatewayProviderException(GatewayProviderException.BAD_REQUEST, status, "Invalid Cloudflare AI request");
        }
        return new GatewayProviderException(GatewayProviderException.UPSTREAM_ERROR, status, "Cloudflare AI HTTP error");
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
