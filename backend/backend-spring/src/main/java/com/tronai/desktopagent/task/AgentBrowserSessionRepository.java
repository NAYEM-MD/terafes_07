package com.tronai.desktopagent.task;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AgentBrowserSessionRepository extends JpaRepository<AgentBrowserSessionEntity, String> {

    Optional<AgentBrowserSessionEntity> findFirstByTaskIdOrderByCreatedAtDesc(String taskId);
}
