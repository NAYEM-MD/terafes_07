package com.tronai.agent.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AgentResultRepository extends JpaRepository<AgentResultEntity, Long> {
    Optional<AgentResultEntity> findByRunId(String runId);
}
