package com.tronai.interview;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewSessionRepository extends JpaRepository<InterviewSessionEntity, String> {

    Optional<InterviewSessionEntity> findByIdAndUserId(String id, Long userId);

    List<InterviewSessionEntity> findByUserIdOrderByStartedAtDesc(Long userId);
}
