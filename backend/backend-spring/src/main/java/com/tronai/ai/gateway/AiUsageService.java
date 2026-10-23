package com.tronai.ai.gateway;

import com.tronai.ai.gateway.usage.AiUsageEventEntity;
import com.tronai.ai.gateway.usage.AiUsageEventRepository;
import com.tronai.ai.gateway.usage.UsageEventStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiUsageService {

    private static final Logger log = LoggerFactory.getLogger(AiUsageService.class);

    private final AiUsageEventRepository usageEventRepository;

    public AiUsageService(AiUsageEventRepository usageEventRepository) {
        this.usageEventRepository = usageEventRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(AiRequestContext context, AiGenerationResult result, int ultraCredits) {
        if (context == null || context.getUserId() == null) {
            return;
        }
        AiUsageEventEntity entity = baseEvent(context, result, ultraCredits);
        entity.setStatus(UsageEventStatus.SUCCESS.name());
        saveSafely(entity);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(
            AiRequestContext context,
            AiMode mode,
            GatewayProviderId provider,
            String modelId,
            String errorCategory,
            int ultraCredits) {
        if (context == null || context.getUserId() == null) {
            return;
        }
        AiUsageEventEntity entity = AiUsageEventEntity.builder()
                .userId(context.getUserId())
                .feature(context.getFeature() != null ? context.getFeature().name() : AiFeature.CHAT.name())
                .mode(mode != null ? mode.name() : AiMode.AUTO.name())
                .provider(provider != null ? provider.name() : "UNKNOWN")
                .modelId(modelId != null ? modelId : "")
                .requestId(context.getRequestId())
                .idempotencyKey(safeIdempotency(context))
                .ultraCredits(ultraCredits)
                .status(UsageEventStatus.FAILURE.name())
                .errorCategory(sanitize(errorCategory))
                .build();
        saveSafely(entity);
    }

    private void saveSafely(AiUsageEventEntity entity) {
        try {
            usageEventRepository.save(entity);
        } catch (DataIntegrityViolationException ex) {
            // Telemetry must not fail the product call (e.g. JWT user id without DB row in ITs).
            log.warn("Skipped AI usage event for userId={}: {}", entity.getUserId(), ex.getMostSpecificCause().getMessage());
        }
    }

    private AiUsageEventEntity baseEvent(AiRequestContext context, AiGenerationResult result, int ultraCredits) {
        return AiUsageEventEntity.builder()
                .userId(context.getUserId())
                .feature(context.getFeature() != null ? context.getFeature().name() : AiFeature.CHAT.name())
                .mode(result.getMode() != null ? result.getMode().name() : context.getMode().name())
                .provider(result.getProvider() != null ? result.getProvider().name() : "UNKNOWN")
                .modelId(result.getModelId() != null ? result.getModelId() : "")
                .requestId(context.getRequestId())
                .idempotencyKey(safeIdempotency(context))
                .promptTokens(result.getPromptTokens())
                .completionTokens(result.getCompletionTokens())
                .latencyMs((int) Math.min(Integer.MAX_VALUE, result.getLatencyMs()))
                .ultraCredits(ultraCredits)
                .build();
    }

    private String safeIdempotency(AiRequestContext context) {
        String key = context.getIdempotencyKey();
        if (key != null && !key.isBlank()) {
            return key.trim();
        }
        return null;
    }

    private String sanitize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > 64) {
            return trimmed.substring(0, 64);
        }
        return trimmed;
    }
}
