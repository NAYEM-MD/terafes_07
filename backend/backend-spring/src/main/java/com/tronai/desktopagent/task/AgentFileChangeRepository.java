package com.tronai.desktopagent.task;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentFileChangeRepository extends JpaRepository<AgentFileChangeEntity, Long> {

    List<AgentFileChangeEntity> findByTaskIdOrderByCreatedAtAsc(String taskId);
}
