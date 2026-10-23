package com.tronai.ai.gateway.usage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiProviderHealthRepository extends JpaRepository<AiProviderHealthEntity, String> {
}
