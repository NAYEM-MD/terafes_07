package com.tronai.supervisor;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupervisorPlanRepository extends JpaRepository<SupervisorPlanEntity, String> {
    List<SupervisorPlanEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<SupervisorPlanEntity> findByIdAndUserId(String id, Long userId);
}
