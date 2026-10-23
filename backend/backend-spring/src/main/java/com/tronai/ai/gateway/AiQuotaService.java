package com.tronai.ai.gateway;

import com.tronai.ai.gateway.usage.AiQuotaReservationEntity;
import com.tronai.ai.gateway.usage.AiQuotaReservationRepository;
import com.tronai.ai.gateway.usage.QuotaReservationStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AiQuotaService {

    private final AiGatewayProperties properties;
    private final AiQuotaReservationRepository reservationRepository;
    private final ConcurrentHashMap<Long, Object> userLocks = new ConcurrentHashMap<>();

    public AiQuotaService(AiGatewayProperties properties, AiQuotaReservationRepository reservationRepository) {
        this.properties = properties;
        this.reservationRepository = reservationRepository;
    }

    @Transactional
    public AiQuotaReservationEntity reserve(
            Long userId,
            AiFeature feature,
            AiMode mode,
            String idempotencyKey,
            String requestId) {
        if (userId == null) {
            throw new IllegalStateException("Authentication required for Ultra AI mode");
        }
        String key = normalizeIdempotencyKey(idempotencyKey, requestId);
        LocalDate utcDay = LocalDate.now(ZoneOffset.UTC);
        Object lock = userLocks.computeIfAbsent(userId, id -> new Object());
        synchronized (lock) {
            return reservationRepository.findByUserIdAndIdempotencyKey(userId, key)
                    .orElseGet(() -> createReservation(userId, feature, mode, key, requestId, utcDay));
        }
    }

    @Transactional
    public void settle(Long reservationId) {
        reservationRepository.findById(reservationId).ifPresent(reservation -> {
            if (QuotaReservationStatus.SETTLED.name().equals(reservation.getStatus())) {
                return;
            }
            reservation.setStatus(QuotaReservationStatus.SETTLED.name());
            reservation.setSettledAt(Instant.now());
            reservationRepository.save(reservation);
        });
    }

    @Transactional
    public void release(Long reservationId) {
        reservationRepository.findById(reservationId).ifPresent(reservation -> {
            if (QuotaReservationStatus.RELEASED.name().equals(reservation.getStatus())) {
                return;
            }
            reservation.setStatus(QuotaReservationStatus.RELEASED.name());
            reservationRepository.save(reservation);
        });
    }

    @Transactional(readOnly = true)
    public Map<String, Object> usageSummary(Long userId) {
        LocalDate utcDay = LocalDate.now(ZoneOffset.UTC);
        int dailyLimit = properties.getDailyUltraCredits();
        int usedCredits = reservationRepository.sumSettledCredits(userId, utcDay);
        int remaining = Math.max(0, dailyLimit - usedCredits);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("dailyLimit", dailyLimit);
        summary.put("usedCredits", usedCredits);
        summary.put("remaining", remaining);
        summary.put("utcDay", utcDay.toString());
        summary.put("ultraEnabled", properties.isUltraEnabled());
        return summary;
    }

    private AiQuotaReservationEntity createReservation(
            Long userId,
            AiFeature feature,
            AiMode mode,
            String idempotencyKey,
            String requestId,
            LocalDate utcDay) {
        int credits = 0;
        if (mode != null && mode.consumesUltraCredits() && properties.isUltraEnabled()) {
            credits = feature != null ? feature.defaultUltraCredits() : AiFeature.CHAT.defaultUltraCredits();
            int used = reservationRepository.sumSettledCredits(userId, utcDay);
            if (used + credits > properties.getDailyUltraCredits()) {
                throw new AiQuotaExceededException("Daily Ultra credit limit reached");
            }
        }

        AiQuotaReservationEntity entity = AiQuotaReservationEntity.builder()
                .userId(userId)
                .utcDay(utcDay)
                .feature(feature != null ? feature.name() : AiFeature.CHAT.name())
                .mode(mode != null ? mode.name() : AiMode.ULTRA.name())
                .credits(credits)
                .status(QuotaReservationStatus.RESERVED.name())
                .idempotencyKey(idempotencyKey)
                .requestId(requestId != null ? requestId : idempotencyKey)
                .build();
        return reservationRepository.save(entity);
    }

    private String normalizeIdempotencyKey(String idempotencyKey, String requestId) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            return idempotencyKey.trim();
        }
        if (requestId != null && !requestId.isBlank()) {
            return requestId.trim();
        }
        throw new IllegalArgumentException("idempotencyKey or requestId is required for quota reservation");
    }
}
