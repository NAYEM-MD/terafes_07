package com.tronai.desktopagent.task;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentTaskMessageRepository extends JpaRepository<AgentTaskMessageEntity, String> {

    List<AgentTaskMessageEntity> findByTaskIdOrderByCreatedAtAsc(String taskId);
}
