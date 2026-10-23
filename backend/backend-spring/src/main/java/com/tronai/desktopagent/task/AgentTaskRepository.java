package com.tronai.desktopagent.task;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AgentTaskRepository extends JpaRepository<AgentTaskEntity, String> {

    List<AgentTaskEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<AgentTaskEntity> findByIdAndUserId(String id, Long userId);

    long countByDeviceIdAndStatusIn(Long deviceId, List<TaskStatus> statuses);
}
