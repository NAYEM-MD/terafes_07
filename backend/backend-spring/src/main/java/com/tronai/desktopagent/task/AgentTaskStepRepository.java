package com.tronai.desktopagent.task;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentTaskStepRepository extends JpaRepository<AgentTaskStepEntity, String> {

    List<AgentTaskStepEntity> findByTaskIdOrderByStepIndexAsc(String taskId);

    int countByTaskId(String taskId);
}
