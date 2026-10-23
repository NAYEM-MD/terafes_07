package com.tronai.ai.gateway;

import com.tronai.ai.gateway.usage.AiProviderHealthEntity;
import com.tronai.ai.gateway.usage.AiProviderHealthRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class AiProviderHealthService {

    private final AiProviderRegistry providerRegistry;
    private final AiProviderHealthRepository healthRepository;

    public AiProviderHealthService(
            AiProviderRegistry providerRegistry,
            AiProviderHealthRepository healthRepository) {
        this.providerRegistry = providerRegistry;
        this.healthRepository = healthRepository;
    }

    @Transactional(readOnly = true)
    public List<AiProviderRegistry.ProviderHealthSnapshot> refreshSnapshots() {
        return providerRegistry.healthSnapshots();
    }

    @Transactional
    public List<AiProviderRegistry.ProviderHealthSnapshot> refreshAndPersist() {
        List<AiProviderRegistry.ProviderHealthSnapshot> snapshots = providerRegistry.healthSnapshots();
        Instant now = Instant.now();
        for (AiProviderRegistry.ProviderHealthSnapshot snapshot : snapshots) {
            AiProviderHealthEntity entity = AiProviderHealthEntity.builder()
                    .provider(snapshot.providerId().name())
                    .status(snapshot.status().name())
                    .detail(snapshot.detail())
                    .checkedAt(now)
                    .build();
            healthRepository.save(entity);
        }
        return snapshots;
    }

    @Transactional(readOnly = true)
    public List<PublicProviderHealthView> publicViews() {
        List<PublicProviderHealthView> views = new ArrayList<>();
        for (AiProviderRegistry.ProviderHealthSnapshot snapshot : providerRegistry.healthSnapshots()) {
            views.add(new PublicProviderHealthView(
                    snapshot.providerId(),
                    snapshot.status(),
                    snapshot.detail(),
                    snapshot.configured(),
                    snapshot.knownModels()));
        }
        return views;
    }

    public record PublicProviderHealthView(
            GatewayProviderId providerId,
            ProviderHealthStatus status,
            String detail,
            boolean configured,
            List<String> knownModels) {
    }
}
