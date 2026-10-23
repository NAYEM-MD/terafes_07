package com.tronai.supervisor;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupervisorMilestoneRepository extends JpaRepository<SupervisorMilestoneEntity, String> {
    List<SupervisorMilestoneEntity> findByPlanIdOrderByMilestoneIndexAsc(String planId);
}
