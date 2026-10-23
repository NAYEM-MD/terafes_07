package com.tronai.voice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoiceSessionRepository extends JpaRepository<VoiceSessionEntity, String> {
    Optional<VoiceSessionEntity> findByIdAndUserId(String id, Long userId);
}
