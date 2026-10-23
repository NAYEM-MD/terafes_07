package com.tronai.ai.gateway;

import com.tronai.ai.gateway.usage.AiQuotaReservationEntity;
import com.tronai.ai.gateway.usage.AiQuotaReservationRepository;
import com.tronai.ai.gateway.usage.QuotaReservationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiQuotaServiceTest {

    @Mock
    private AiQuotaReservationRepository reservationRepository;

    private AiGatewayProperties properties;
    private AiQuotaService quotaService;

    @BeforeEach
    void setUp() {
        properties = new AiGatewayProperties();
        properties.setDailyUltraCredits(5);
        properties.setUltraEnabled(true);
        quotaService = new AiQuotaService(properties, reservationRepository);
    }

    @Test
    @DisplayName("Sixth Ultra credit is rejected after five reserved credits")
    void sixthCreditRejected() {
        Long userId = 42L;
        LocalDate utcDay = LocalDate.now(ZoneOffset.UTC);
        when(reservationRepository.findByUserIdAndIdempotencyKey(eq(userId), any())).thenReturn(Optional.empty());
        when(reservationRepository.sumSettledCredits(userId, utcDay)).thenReturn(5);

        assertThatThrownBy(() -> quotaService.reserve(
                userId,
                AiFeature.CHAT,
                AiMode.ULTRA,
                "key-6",
                "req-6"))
                .isInstanceOf(AiQuotaExceededException.class);
    }

    @Test
    @DisplayName("Idempotency key returns same reservation without double charge")
    void idempotencyReturnsExistingReservation() {
        Long userId = 7L;
        AiQuotaReservationEntity existing = AiQuotaReservationEntity.builder()
                .id(99L)
                .userId(userId)
                .credits(1)
                .status(QuotaReservationStatus.RESERVED.name())
                .idempotencyKey("idem-1")
                .build();
        when(reservationRepository.findByUserIdAndIdempotencyKey(userId, "idem-1"))
                .thenReturn(Optional.of(existing));

        AiQuotaReservationEntity result = quotaService.reserve(
                userId,
                AiFeature.CHAT,
                AiMode.ULTRA,
                "idem-1",
                "req-1");

        assertThat(result.getId()).isEqualTo(99L);
        verify(reservationRepository, never()).save(any());
        verify(reservationRepository, never()).sumSettledCredits(any(), any());
    }

    @Test
    @DisplayName("First Ultra reservation persists reserved credits")
    void firstReservationSucceeds() {
        Long userId = 3L;
        LocalDate utcDay = LocalDate.now(ZoneOffset.UTC);
        when(reservationRepository.findByUserIdAndIdempotencyKey(userId, "idem-2")).thenReturn(Optional.empty());
        when(reservationRepository.sumSettledCredits(userId, utcDay)).thenReturn(0);
        when(reservationRepository.save(any())).thenAnswer(inv -> {
            AiQuotaReservationEntity entity = inv.getArgument(0);
            entity.setId(1L);
            return entity;
        });

        AiQuotaReservationEntity reservation = quotaService.reserve(
                userId,
                AiFeature.CHAT,
                AiMode.ULTRA,
                "idem-2",
                "req-2");

        assertThat(reservation.getCredits()).isEqualTo(1);
        assertThat(reservation.getStatus()).isEqualTo(QuotaReservationStatus.RESERVED.name());

        ArgumentCaptor<AiQuotaReservationEntity> captor = ArgumentCaptor.forClass(AiQuotaReservationEntity.class);
        verify(reservationRepository).save(captor.capture());
        assertThat(captor.getValue().getCredits()).isEqualTo(1);
    }

    @Test
    @DisplayName("Ten concurrent Ultra chat reserves cannot exceed daily limit")
    void concurrentReservesCannotExceedLimit() throws Exception {
        Long userId = 99L;
        LocalDate utcDay = LocalDate.now(ZoneOffset.UTC);
        java.util.concurrent.atomic.AtomicInteger used = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicLong ids = new java.util.concurrent.atomic.AtomicLong(1);
        when(reservationRepository.findByUserIdAndIdempotencyKey(eq(userId), any())).thenReturn(Optional.empty());
        when(reservationRepository.sumSettledCredits(userId, utcDay)).thenAnswer(inv -> used.get());
        when(reservationRepository.save(any())).thenAnswer(inv -> {
            AiQuotaReservationEntity entity = inv.getArgument(0);
            entity.setId(ids.getAndIncrement());
            used.addAndGet(entity.getCredits());
            return entity;
        });

        int threads = 10;
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(threads);
        java.util.concurrent.atomic.AtomicInteger accepted = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicInteger rejected = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            final int idx = i;
            pool.submit(() -> {
                try {
                    start.await();
                    quotaService.reserve(userId, AiFeature.CHAT, AiMode.ULTRA, "c-" + idx, "r-" + idx);
                    accepted.incrementAndGet();
                } catch (AiQuotaExceededException ex) {
                    rejected.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        done.await(5, java.util.concurrent.TimeUnit.SECONDS);
        pool.shutdownNow();

        assertThat(accepted.get()).isEqualTo(5);
        assertThat(rejected.get()).isEqualTo(5);
        assertThat(used.get()).isEqualTo(5);
    }
}
