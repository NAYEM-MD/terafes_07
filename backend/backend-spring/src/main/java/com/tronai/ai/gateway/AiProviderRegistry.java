package com.tronai.ai.gateway;

import com.tronai.ai.gateway.provider.GatewayProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AiProviderRegistry {

    private final Map<GatewayProviderId, GatewayProvider> providers;

    public AiProviderRegistry(List<GatewayProvider> providerList) {
        Map<GatewayProviderId, GatewayProvider> map = new EnumMap<>(GatewayProviderId.class);
        if (providerList != null) {
            for (GatewayProvider provider : providerList) {
                map.put(provider.id(), provider);
            }
        }
        this.providers = Map.copyOf(map);
    }

    public Optional<GatewayProvider> find(GatewayProviderId id) {
        return Optional.ofNullable(providers.get(id));
    }

    public List<GatewayProvider> all() {
        return new ArrayList<>(providers.values());
    }

    public List<ProviderHealthSnapshot> healthSnapshots() {
        List<ProviderHealthSnapshot> snapshots = new ArrayList<>(providers.size());
        for (GatewayProvider provider : providers.values()) {
            snapshots.add(new ProviderHealthSnapshot(
                    provider.id(),
                    provider.health(),
                    provider.healthDetail(),
                    provider.isConfigured(),
                    provider.knownModels()));
        }
        return snapshots;
    }

    public record ProviderHealthSnapshot(
            GatewayProviderId providerId,
            ProviderHealthStatus status,
            String detail,
            boolean configured,
            List<String> knownModels) {
    }
}
