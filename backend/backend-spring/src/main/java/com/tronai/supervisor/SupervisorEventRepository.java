package com.tronai.supervisor;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupervisorEventRepository extends JpaRepository<SupervisorEventEntity, String> {
    List<SupervisorEventEntity> findByPlanIdOrderByCreatedAtAsc(String planId);
}
