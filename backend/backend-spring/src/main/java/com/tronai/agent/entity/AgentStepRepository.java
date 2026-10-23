package com.tronai.agent.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentStepRepository extends JpaRepository<AgentStepEntity, Long> {
    List<AgentStepEntity> findByRunIdOrderByStepNumberAsc(String runId);
}
