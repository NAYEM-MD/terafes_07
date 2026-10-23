package com.tronai.ai.gateway.usage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface AiUsageEventRepository extends JpaRepository<AiUsageEventEntity, Long> {

    Optional<AiUsageEventEntity> findByRequestId(String requestId);

    long countByCreatedAtAfter(Instant after);
}
