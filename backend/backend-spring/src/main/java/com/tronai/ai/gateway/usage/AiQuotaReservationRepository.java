package com.tronai.ai.gateway.usage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface AiQuotaReservationRepository extends JpaRepository<AiQuotaReservationEntity, Long> {

    Optional<AiQuotaReservationEntity> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    @Query("""
            SELECT COALESCE(SUM(r.credits), 0)
            FROM AiQuotaReservationEntity r
            WHERE r.userId = :userId
              AND r.utcDay = :utcDay
              AND r.status IN ('RESERVED', 'SETTLED')
            """)
    int sumSettledCredits(@Param("userId") Long userId, @Param("utcDay") LocalDate utcDay);
}
