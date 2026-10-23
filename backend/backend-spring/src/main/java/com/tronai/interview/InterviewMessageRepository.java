package com.tronai.interview;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterviewMessageRepository extends JpaRepository<InterviewMessageEntity, Long> {

    List<InterviewMessageEntity> findBySessionIdOrderByIdAsc(String sessionId);
}
