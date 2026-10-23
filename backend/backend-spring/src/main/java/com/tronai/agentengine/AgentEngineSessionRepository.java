package com.tronai.agentengine;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AgentEngineSessionRepository extends JpaRepository<AgentEngineSessionEntity, String> {
    List<AgentEngineSessionEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<AgentEngineSessionEntity> findByIdAndUserId(String id, Long userId);
}
