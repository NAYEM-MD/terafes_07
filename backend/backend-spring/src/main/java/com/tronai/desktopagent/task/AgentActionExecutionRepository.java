package com.tronai.desktopagent.task;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentActionExecutionRepository extends JpaRepository<AgentActionExecutionEntity, String> {

    List<AgentActionExecutionEntity> findByTaskIdOrderByCreatedAtAsc(String taskId);
}
