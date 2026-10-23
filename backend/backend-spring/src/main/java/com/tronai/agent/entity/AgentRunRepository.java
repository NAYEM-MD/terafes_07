package com.tronai.agent.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgentRunRepository extends JpaRepository<AgentRunEntity, String> {
    List<AgentRunEntity> findByUserIdOrderByStartedAtDesc(Long userId);
    Optional<AgentRunEntity> findByIdAndUserId(String id, Long userId);
}
