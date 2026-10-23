package com.tronai.desktopagent.approval;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AgentApprovalRepository extends JpaRepository<AgentApprovalEntity, String> {

    Optional<AgentApprovalEntity> findByIdAndUserId(String id, Long userId);

    Optional<AgentApprovalEntity> findByIdAndTaskIdAndUserId(String id, String taskId, Long userId);

    List<AgentApprovalEntity> findByTaskIdOrderByCreatedAtAsc(String taskId);

    Optional<AgentApprovalEntity> findByActionIdAndStatus(String actionId, ApprovalStatus status);
}
