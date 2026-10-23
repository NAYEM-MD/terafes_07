package com.tronai.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSessionEntity, Long> {

    Optional<ChatSessionEntity> findByUserIdAndClientSessionId(Long userId, String clientSessionId);

    List<ChatSessionEntity> findByUserIdOrderByUpdatedAtDesc(Long userId);

    void deleteByUserIdAndClientSessionId(Long userId, String clientSessionId);
}
