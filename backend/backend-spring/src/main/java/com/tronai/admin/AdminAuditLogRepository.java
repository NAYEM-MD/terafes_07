package com.tronai.admin;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLogEntity, Long> {
    List<AdminAuditLogEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
