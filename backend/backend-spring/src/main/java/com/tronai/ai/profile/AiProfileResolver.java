package com.tronai.ai.profile;

import com.tronai.auth.SecurityUtils;
import com.tronai.user.UserSettings;
import com.tronai.user.UserSettingsRepository;
import org.springframework.stereotype.Service;

/**
 * Resolves the effective AI profile using the Mission 08/10 hierarchy:
 * explicit request → saved preferredAiProfile → default (FAST).
 */
@Service
public class AiProfileResolver {

    private final UserSettingsRepository userSettingsRepository;
    private final AiProfileService aiProfileService;

    public AiProfileResolver(UserSettingsRepository userSettingsRepository, AiProfileService aiProfileService) {
        this.userSettingsRepository = userSettingsRepository;
        this.aiProfileService = aiProfileService;
    }

    public AiProfile resolve(String explicitProfile) {
        if (explicitProfile != null && !explicitProfile.isBlank()) {
            return aiProfileService.parseProfile(explicitProfile);
        }
        Long userId = SecurityUtils.getCurrentUser().map(u -> u.getId()).orElse(null);
        if (userId != null) {
            String saved = userSettingsRepository.findByUserId(userId)
                    .map(UserSettings::getPreferredAiProfile)
                    .orElse(null);
            if (saved != null && !saved.isBlank()) {
                try {
                    return aiProfileService.parseProfile(saved);
                } catch (RuntimeException ignored) {
                    // fall through to default
                }
            }
        }
        return AiProfile.FAST;
    }

    public AiProfile resolveForUser(Long userId, String explicitProfile) {
        if (explicitProfile != null && !explicitProfile.isBlank()) {
            return aiProfileService.parseProfile(explicitProfile);
        }
        if (userId != null) {
            String saved = userSettingsRepository.findByUserId(userId)
                    .map(UserSettings::getPreferredAiProfile)
                    .orElse(null);
            if (saved != null && !saved.isBlank()) {
                try {
                    return aiProfileService.parseProfile(saved);
                } catch (RuntimeException ignored) {
                    // fall through
                }
            }
        }
        return AiProfile.FAST;
    }
}
