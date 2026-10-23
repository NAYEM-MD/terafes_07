package com.tronai.auth.identity;

import com.tronai.auth.AuthService;
import com.tronai.auth.oauth2.GoogleOAuthCredentials;
import com.tronai.user.User;
import com.tronai.user.UserRepository;
import com.tronai.user.UserSettings;
import com.tronai.user.UserSettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class GoogleIdentityService {

    private static final Logger log = LoggerFactory.getLogger(GoogleIdentityService.class);
    public static final String PROVIDER_GOOGLE = "GOOGLE";

    private final UserIdentityRepository identityRepository;
    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final AuthService authService;

    @Value("${GOOGLE_CLIENT_ID:${oauth2.google.client-id:}}")
    private String googleClientId;

    @Value("${GOOGLE_CLIENT_SECRET:${oauth2.google.client-secret:}}")
    private String googleClientSecret;

    public GoogleIdentityService(UserIdentityRepository identityRepository,
                                 UserRepository userRepository,
                                 UserSettingsRepository userSettingsRepository,
                                 @org.springframework.context.annotation.Lazy AuthService authService) {
        this.identityRepository = identityRepository;
        this.userRepository = userRepository;
        this.userSettingsRepository = userSettingsRepository;
        this.authService = authService;
    }

    public boolean isGoogleAuthEnabled() {
        return GoogleOAuthCredentials.isConfigured(googleClientId, googleClientSecret);
    }

    /**
     * Resolves or links a TRON user account based on Google identity claims.
     * Policy:
     * A. Existing GOOGLE identity with same provider_subject -> login existing TRON user.
     * B. No Google identity, but Google reports VERIFIED email matching exactly one existing TRON user
     *    -> securely attach GOOGLE identity to that user.
     * C. No matching user -> create TRON user -> create GOOGLE identity.
     * D. Ambiguous/conflicting identity -> fail safely.
     */
    @Transactional
    public AuthService.AuthResult handleGoogleLogin(String sub, String email, boolean emailVerified, String displayName) {
        if (sub == null || sub.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Google subject ID is required");
        }
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Google email is required");
        }
        String normalizedEmail = email.trim().toLowerCase();
        String safeName = (displayName != null && !displayName.isBlank()) ? displayName.trim() : normalizedEmail.split("@")[0];

        // Case A: Existing identity by (provider, provider_subject)
        Optional<UserIdentity> existingIdentityOpt = identityRepository.findByProviderAndProviderSubject(PROVIDER_GOOGLE, sub.trim());
        if (existingIdentityOpt.isPresent()) {
            UserIdentity identity = existingIdentityOpt.get();
            User user = userRepository.findById(identity.getUserId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User associated with Google identity not found"));

            if (!user.isEnabled()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is disabled");
            }

            identity.setEmailAtLogin(normalizedEmail);
            identity.setEmailVerified(emailVerified);
            identity.setLastLoginAt(Instant.now());
            identityRepository.save(identity);

            user.setLastLoginAt(Instant.now());
            userRepository.save(user);

            log.info("Google sign-in for existing linked user ID: {}", user.getId());
            return authService.createAuthResult(user);
        }

        // Check if an existing TRON user has this email
        Optional<User> existingUserOpt = userRepository.findByEmailIgnoreCase(normalizedEmail);
        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            if (!emailVerified) {
                log.warn("Rejected unverified Google email link attempt");
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Google email is not verified; cannot link to existing account");
            }
            if (!existingUser.isEnabled()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is disabled");
            }
            boolean conflictingGoogle = identityRepository.findByUserId(existingUser.getId()).stream()
                    .anyMatch(identity -> PROVIDER_GOOGLE.equals(identity.getProvider())
                            && !sub.trim().equals(identity.getProviderSubject()));
            if (conflictingGoogle) {
                log.warn("Rejected conflicting Google identity for user ID: {}", existingUser.getId());
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Google account cannot be linked to this TRON user");
            }

            UserIdentity newIdentity = UserIdentity.builder()
                    .userId(existingUser.getId())
                    .provider(PROVIDER_GOOGLE)
                    .providerSubject(sub.trim())
                    .emailAtLogin(normalizedEmail)
                    .emailVerified(true)
                    .createdAt(Instant.now())
                    .lastLoginAt(Instant.now())
                    .build();
            identityRepository.save(newIdentity);

            existingUser.setLastLoginAt(Instant.now());
            userRepository.save(existingUser);

            log.info("Attached Google identity to existing verified user ID: {}", existingUser.getId());
            return authService.createAuthResult(existingUser);
        }

        if (!emailVerified) {
            log.warn("Rejected unverified Google sign-in for a new account");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Google email is not verified");
        }

        // Case C: Verified email with no matching user -> Create new TRON user + Google identity
        User newUser = User.builder()
                .email(normalizedEmail)
                .passwordHash(null)
                .displayName(safeName)
                .role("USER")
                .enabled(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .lastLoginAt(Instant.now())
                .build();
        newUser = userRepository.save(newUser);

        UserSettings settings = UserSettings.builder()
                .user(newUser)
                .preferredLanguage("ja")
                .preferredModel("qwen3:8b")
                .build();
        userSettingsRepository.save(settings);

        UserIdentity identity = UserIdentity.builder()
                .userId(newUser.getId())
                .provider(PROVIDER_GOOGLE)
                .providerSubject(sub.trim())
                .emailAtLogin(normalizedEmail)
                .emailVerified(emailVerified)
                .createdAt(Instant.now())
                .lastLoginAt(Instant.now())
                .build();
        identityRepository.save(identity);

        log.info("Created new TRON user ID: {} from Google sign-in", newUser.getId());
        return authService.createAuthResult(newUser);
    }

    public List<String> getUserAuthProviders(Long userId, User user) {
        List<String> providers = new ArrayList<>();
        if (user != null && user.getPasswordHash() != null && !user.getPasswordHash().isBlank()) {
            providers.add("PASSWORD");
        }
        if (userId != null) {
            List<UserIdentity> identities = identityRepository.findByUserId(userId);
            for (UserIdentity id : identities) {
                if (!providers.contains(id.getProvider())) {
                    providers.add(id.getProvider());
                }
            }
        }
        if (providers.isEmpty()) {
            providers.add("PASSWORD");
        }
        return providers;
    }
}
