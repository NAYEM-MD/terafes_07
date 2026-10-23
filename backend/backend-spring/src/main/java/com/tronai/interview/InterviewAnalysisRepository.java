package com.tronai.interview;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InterviewAnalysisRepository extends JpaRepository<InterviewAnalysisEntity, Long> {

    Optional<InterviewAnalysisEntity> findBySessionId(String sessionId);
}
