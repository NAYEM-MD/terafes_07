package com.tronai.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

    List<ChatMessageEntity> findBySessionIdOrderByCreatedAtAsc(Long sessionId);

    boolean existsBySessionId(Long sessionId);
}
