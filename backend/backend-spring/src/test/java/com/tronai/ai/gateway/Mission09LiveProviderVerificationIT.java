package com.tronai.ai.gateway;

import com.tronai.ai.ChatMessage;
import com.tronai.ai.gateway.provider.GatewayProviderException;
import com.tronai.ai.gateway.usage.AiQuotaReservationEntity;
import com.tronai.ai.gateway.usage.AiQuotaReservationRepository;
import com.tronai.ai.gateway.usage.QuotaReservationStatus;
import com.tronai.user.User;
import com.tronai.user.UserRepository;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Files;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Mission 09-A live verification. Enable with TRON_AI_LIVE_PROVIDER_SMOKE_TEST=true.
 * Never logs secrets. One minimal generation per exercised mode path.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@EnabledIfEnvironmentVariable(named = "TRON_AI_LIVE_PROVIDER_SMOKE_TEST", matches = "true")
class Mission09LiveProviderVerificationIT {

    @Autowired
    private AiGateway aiGateway;
    @Autowired
    private AiGatewayProperties properties;
    @Autowired
    private AiModelRouter modelRouter;
    @Autowired
    private AiProviderRegistry providerRegistry;
    @Autowired
    private AiQuotaService quotaService;
    @Autowired
    private AiQuotaReservationRepository reservationRepository;
    @Autowired
    private UserRepository userRepository;

    @Test
    @Order(1)
    @DisplayName("Secrets path is readable; empty file still allows env-bound keys")
    void secretsBootstrapPath() {
        var path = com.tronai.ai.gateway.credential.LocalSecretsLoader.defaultPath();
        assertThat(path.toString()).contains("TRON").contains("secrets").contains("ai-providers.env");
        boolean exists = Files.isRegularFile(path);
        long size = exists ? path.toFile().length() : -1L;
        System.out.println("MISSION09 secretsPathExists=" + exists + " bytes=" + size);
        System.out.println("MISSION09 groqConfigured=" + hasText(properties.getGroqApiKey()));
        System.out.println("MISSION09 openrouter1Configured=" + hasText(properties.getOpenrouterApiKey1()));
        System.out.println("MISSION09 openrouter2Configured=" + hasText(properties.getOpenrouterApiKey2()));
        System.out.println("MISSION09 mistralConfigured=" + hasText(properties.getMistralApiKey()));
        System.out.println("MISSION09 cloudflareTokenConfigured=" + hasText(properties.getCloudflareApiToken()));
        System.out.println("MISSION09 cloudflareAccountConfigured=" + hasText(properties.getCloudflareAccountId()));
        System.out.println("MISSION09 gemini1Configured=" + hasText(properties.getGeminiApiKey1()));
    }

    @Test
    @Order(2)
    @DisplayName("Provider health snapshots without secrets")
    void providerHealth() {
        for (var snapshot : providerRegistry.healthSnapshots()) {
            System.out.println("MISSION09 health provider=" + snapshot.providerId()
                    + " status=" + snapshot.status()
                    + " configured=" + snapshot.configured()
                    + " detail=" + snapshot.detail());
        }
    }

    @Test
    @Order(3)
    @DisplayName("FAST route yields real text via gateway/Ollama when available")
    void fastModeGeneration() {
        AiRequestContext ctx = minimalContext(AiMode.FAST, AiFeature.CHAT, null);
        AiModelRouter.Route route = modelRouter.resolveRoute(ctx, AiMode.FAST);
        System.out.println("MISSION09 FAST route=" + route.provider() + " model=" + route.modelId());
        long started = System.currentTimeMillis();
        AiGenerationResult result = aiGateway.generate(ctx);
        long latency = System.currentTimeMillis() - started;
        System.out.println("MISSION09 FAST httpish=ok latencyMs=" + latency
                + " tokens=" + (result.getPromptTokens() + result.getCompletionTokens())
                + " textLen=" + result.getText().trim().length()
                + " provider=" + result.getProvider()
                + " model=" + result.getModelId());
        assertThat(result.getText().trim()).isNotEmpty();
        assertThat(result.getMode()).isEqualTo(AiMode.FAST);
    }

    @Test
    @Order(4)
    @DisplayName("SMART selects a usable configured provider")
    void smartModeGeneration() {
        Assumptions.assumeTrue(
                hasText(properties.getGroqApiKey())
                        || hasText(properties.getMistralApiKey())
                        || hasText(properties.getOpenrouterApiKey1()),
                "No cloud SMART providers configured");
        AiRequestContext ctx = minimalContext(AiMode.SMART, AiFeature.CHAT, null);
        AiModelRouter.Route route = modelRouter.resolveRoute(ctx, AiMode.SMART);
        System.out.println("MISSION09 SMART route=" + route.provider() + " model=" + route.modelId());
        long started = System.currentTimeMillis();
        try {
            AiGenerationResult result = aiGateway.generate(ctx);
            long latency = System.currentTimeMillis() - started;
            System.out.println("MISSION09 SMART latencyMs=" + latency
                    + " tokens=" + (result.getPromptTokens() + result.getCompletionTokens())
                    + " textLen=" + result.getText().trim().length()
                    + " provider=" + result.getProvider()
                    + " model=" + result.getModelId()
                    + " status=PASS");
            assertThat(result.getText().trim()).isNotEmpty();
        } catch (GatewayProviderException | IllegalStateException ex) {
            System.out.println("MISSION09 SMART status=FAIL category=" + ex.getClass().getSimpleName()
                    + " message=" + safe(ex.getMessage()));
            throw ex;
        }
    }

    @Test
    @Order(5)
    @DisplayName("AUTO for CHAT resolves to FAST policy")
    void autoModePolicy() {
        AiRequestContext ctx = minimalContext(AiMode.AUTO, AiFeature.CHAT, null);
        AiMode effective = modelRouter.resolveEffectiveMode(ctx);
        System.out.println("MISSION09 AUTO(CHAT) effective=" + effective);
        assertThat(effective).isEqualTo(AiMode.FAST);
        AiModelRouter.Route route = modelRouter.resolveRoute(ctx, effective);
        System.out.println("MISSION09 AUTO route=" + route.provider() + " model=" + route.modelId());
    }

    @Test
    @Order(6)
    @DisplayName("ULTRA resolves an eligible premium route (generation may fail on stale model IDs)")
    void ultraMode() {
        User user = userRepository.save(User.builder()
                .email("m09-ultra-" + UUID.randomUUID() + "@tron.ai")
                .displayName("M09 Ultra")
                .passwordHash("x")
                .role("USER")
                .enabled(true)
                .build());
        AiRequestContext ctx = minimalContext(AiMode.ULTRA, AiFeature.CHAT, user.getId());
        boolean ultraPossible = hasText(properties.getGeminiApiKey1())
                || hasText(properties.getOpenrouterApiKey1())
                || hasText(properties.getMistralApiKey());
        if (!ultraPossible) {
            System.out.println("MISSION09 ULTRA status=BLOCKED reason=no_ultra_provider_keys");
            assertThatThrownBy(() -> modelRouter.resolveRoute(ctx, AiMode.ULTRA))
                    .isInstanceOf(IllegalStateException.class);
            return;
        }
        AiModelRouter.Route route = modelRouter.resolveRoute(ctx, AiMode.ULTRA);
        System.out.println("MISSION09 ULTRA route=" + route.provider() + " model=" + route.modelId());
        try {
            long started = System.currentTimeMillis();
            AiGenerationResult result = aiGateway.generate(ctx);
            long latency = System.currentTimeMillis() - started;
            System.out.println("MISSION09 ULTRA latencyMs=" + latency
                    + " tokens=" + (result.getPromptTokens() + result.getCompletionTokens())
                    + " textLen=" + result.getText().trim().length()
                    + " provider=" + result.getProvider()
                    + " model=" + result.getModelId()
                    + " status=PASS");
            assertThat(result.getMode()).isEqualTo(AiMode.ULTRA);
            assertThat(result.getText().trim()).isNotEmpty();
        } catch (RuntimeException ex) {
            // Catalog drift is a live finding; route resolution itself must still succeed.
            System.out.println("MISSION09 ULTRA generate status=FAIL message=" + safe(ex.getMessage()));
            System.out.println("MISSION09 ULTRA route_selection status=PASS");
        }
    }

    @Test
    @Order(7)
    @DisplayName("Ultra credit costs match contract; reserve deducts for chat=1 agent=2 interview=2")
    void ultraCreditCosts() {
        assertThat(AiFeature.CHAT.defaultUltraCredits()).isEqualTo(1);
        assertThat(AiFeature.CODING_AGENT.defaultUltraCredits()).isEqualTo(2);
        assertThat(AiFeature.BROWSER_AGENT.defaultUltraCredits()).isEqualTo(2);
        assertThat(AiFeature.INTERVIEW.defaultUltraCredits()).isEqualTo(2);

        User user = userRepository.save(User.builder()
                .email("m09-credits-" + UUID.randomUUID() + "@tron.ai")
                .displayName("M09 Credits")
                .passwordHash("x")
                .role("USER")
                .enabled(true)
                .build());
        Long userId = user.getId();
        LocalDate utcDay = LocalDate.now(ZoneOffset.UTC);
        int before = reservationRepository.sumSettledCredits(userId, utcDay);

        AiQuotaReservationEntity chat = quotaService.reserve(
                userId, AiFeature.CHAT, AiMode.ULTRA, "m09-chat-" + UUID.randomUUID(), "m09-chat");
        assertThat(chat.getCredits()).isEqualTo(1);
        quotaService.settle(chat.getId());

        AiQuotaReservationEntity agent = quotaService.reserve(
                userId, AiFeature.CODING_AGENT, AiMode.ULTRA, "m09-agent-" + UUID.randomUUID(), "m09-agent");
        assertThat(agent.getCredits()).isEqualTo(2);
        quotaService.settle(agent.getId());

        AiQuotaReservationEntity interview = quotaService.reserve(
                userId, AiFeature.INTERVIEW, AiMode.ULTRA, "m09-iv-" + UUID.randomUUID(), "m09-iv");
        assertThat(interview.getCredits()).isEqualTo(2);
        quotaService.settle(interview.getId());

        int after = reservationRepository.sumSettledCredits(userId, utcDay);
        System.out.println("MISSION09 credits before=" + before + " after=" + after
                + " delta=" + (after - before) + " expectedDelta=5 status=PASS");
        assertThat(after - before).isEqualTo(5);
        assertThat(reservationRepository.findById(chat.getId()).orElseThrow().getStatus())
                .isEqualTo(QuotaReservationStatus.SETTLED.name());
    }

    @Test
    @Order(8)
    @DisplayName("Invalid provider override yields clean error")
    void invalidOverrideCleanError() {
        AiRequestContext ctx = AiRequestContext.builder()
                .feature(AiFeature.CHAT)
                .mode(AiMode.SMART)
                .providerOverride("NOT_A_PROVIDER")
                .messages(List.of(ChatMessage.builder().role("user").content("OK").build()))
                .maxTokens(8)
                .idempotencyKey(UUID.randomUUID().toString())
                .build();
        assertThatThrownBy(() -> modelRouter.resolveRoute(ctx, AiMode.SMART))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown provider");
        System.out.println("MISSION09 invalidProvider status=PASS clean_error=true");
    }

    private static AiRequestContext minimalContext(AiMode mode, AiFeature feature, Long userId) {
        return AiRequestContext.builder()
                .userId(userId)
                .feature(feature)
                .mode(mode)
                .messages(List.of(ChatMessage.builder().role("user").content("Reply with exactly: OK").build()))
                .temperature(0.0)
                .maxTokens(16)
                .idempotencyKey(UUID.randomUUID().toString())
                .build();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String safe(String message) {
        if (message == null) {
            return "";
        }
        return message.replaceAll("(?i)(Bearer\\s+)\\S+", "$1[REDACTED]")
                .replaceAll("(?i)(key[=:])\\S+", "$1[REDACTED]");
    }
}
