package com.tronai.auth;

import com.tronai.auth.identity.GoogleIdentityService;
import com.tronai.auth.identity.UserIdentity;
import com.tronai.auth.identity.UserIdentityRepository;
import com.tronai.auth.oauth2.GoogleOAuthCredentials;
import com.tronai.user.User;
import com.tronai.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GoogleAuthAndIdentityTest {

    @Autowired private GoogleIdentityService googleIdentityService;
    @Autowired private UserRepository userRepository;
    @Autowired private UserIdentityRepository userIdentityRepository;
    @Autowired private AuthService authService;
    @Autowired private JwtService jwtService;
    @Autowired private MockMvc mockMvc;
    @Autowired private ClientRegistrationRepository clientRegistrationRepository;

    private String randomEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    @Test
    @DisplayName("1. New Google identity creates new TRON user and Google identity")
    void newGoogleIdentityCreatesTronUser() {
        String sub = "google-sub-" + UUID.randomUUID();
        String email = randomEmail();

        AuthService.AuthResult result = googleIdentityService.handleGoogleLogin(sub, email, true, "Alice In Wonderland");

        assertThat(result).isNotNull();
        assertThat(result.response().getAccessToken()).isNotBlank();
        assertThat(result.cookie().getValue()).isNotBlank();
        assertThat(result.response().getUser().getEmail()).isEqualTo(email.toLowerCase());

        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertThat(user.getDisplayName()).isEqualTo("Alice In Wonderland");

        UserIdentity identity = userIdentityRepository.findByProviderAndProviderSubject("GOOGLE", sub).orElseThrow();
        assertThat(identity.getUserId()).isEqualTo(user.getId());
        assertThat(identity.getEmailAtLogin()).isEqualTo(email.toLowerCase());
        assertThat(identity.isEmailVerified()).isTrue();
    }

    @Test
    @DisplayName("2. Returning Google subject maps to existing TRON user without duplicate accounts")
    void returningGoogleSubjectMapsToSameUser() {
        String sub = "google-sub-" + UUID.randomUUID();
        String email = randomEmail();

        AuthService.AuthResult first = googleIdentityService.handleGoogleLogin(sub, email, true, "Bob Developer");
        Long firstUserId = first.response().getUser().getId();

        // Second login with same Google sub, perhaps different display name or email case
        AuthService.AuthResult second = googleIdentityService.handleGoogleLogin(sub, email.toUpperCase(), true, "Bob Updated");
        Long secondUserId = second.response().getUser().getId();

        assertThat(secondUserId).isEqualTo(firstUserId);
        assertThat(userRepository.count()).isPositive();

        List<UserIdentity> identities = userIdentityRepository.findByUserId(firstUserId);
        assertThat(identities).hasSize(1);
    }

    @Test
    @DisplayName("3. Verified email links existing local user according to policy")
    void verifiedEmailLinksExistingLocalUser() {
        String email = randomEmail();
        User localUser = userRepository.save(User.builder()
                .email(email)
                .passwordHash("hashed-password-123")
                .displayName("Local User")
                .role("USER")
                .enabled(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

        String sub = "google-sub-" + UUID.randomUUID();
        AuthService.AuthResult result = googleIdentityService.handleGoogleLogin(sub, email, true, "Local User Google");

        assertThat(result.response().getUser().getId()).isEqualTo(localUser.getId());

        UserIdentity identity = userIdentityRepository.findByProviderAndProviderSubject("GOOGLE", sub).orElseThrow();
        assertThat(identity.getUserId()).isEqualTo(localUser.getId());
        assertThat(identity.isEmailVerified()).isTrue();

        List<String> providers = googleIdentityService.getUserAuthProviders(localUser.getId(), localUser);
        assertThat(providers).contains("PASSWORD", "GOOGLE");
    }

    @Test
    @DisplayName("4. Unverified email is not trusted for account linking")
    void unverifiedEmailNotTrustedForLinking() {
        String email = randomEmail();
        userRepository.save(User.builder()
                .email(email)
                .passwordHash("hashed-password-123")
                .displayName("Local User")
                .role("USER")
                .enabled(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

        String sub = "google-sub-" + UUID.randomUUID();

        // Attempt link with unverified email -> must throw exception
        assertThatThrownBy(() -> googleIdentityService.handleGoogleLogin(sub, email, false, "Imposter"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Google email is not verified");

        // Identity must not have been created
        assertThat(userIdentityRepository.findByProviderAndProviderSubject("GOOGLE", sub)).isEmpty();
    }

    @Test
    @DisplayName("5. GET /api/v1/auth/providers is public and reports the real Google configuration")
    void authProvidersEndpointWorks() throws Exception {
        boolean googleEnabled = googleIdentityService.isGoogleAuthEnabled();
        mockMvc.perform(get("/api/v1/auth/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.password.enabled").value(true))
                .andExpect(jsonPath("$.google.enabled").value(googleEnabled));
    }

    @Test
    @DisplayName("5b. Google client registration follows real configuration and never uses the removed fallback")
    void googleRegistrationFollowsRealConfiguration() throws Exception {
        var registration = clientRegistrationRepository.findByRegistrationId("google");
        if (googleIdentityService.isGoogleAuthEnabled()) {
            assertThat(registration).isNotNull();
            assertThat(GoogleOAuthCredentials.isRemovedFake(registration.getClientId(), registration.getClientSecret())).isFalse();
            mockMvc.perform(get("/oauth2/authorization/google"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(header().string(HttpHeaders.LOCATION, containsString("https://accounts.google.com/")));
        } else {
            assertThat(registration).isNull();
            mockMvc.perform(get("/oauth2/authorization/google"))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    @DisplayName("5c. Password login remains available and public auth bootstrap is not rejected")
    void passwordLoginRemainsAvailable() throws Exception {
        String email = randomEmail();
        authService.register(com.tronai.auth.dto.RegisterRequest.builder()
                .email(email)
                .password("correct-horse")
                .displayName("Password User")
                .build());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"correct-horse\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Refresh token is missing"));
        mockMvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("5d. Unverified Google email cannot create a TRON account")
    void unverifiedEmailCannotCreateAccount() {
        String sub = "google-sub-" + UUID.randomUUID();
        String email = randomEmail();
        assertThatThrownBy(() -> googleIdentityService.handleGoogleLogin(sub, email, false, "New Person"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not verified");
        assertThat(userRepository.findByEmailIgnoreCase(email)).isEmpty();
        assertThat(userIdentityRepository.findByProviderAndProviderSubject("GOOGLE", sub)).isEmpty();
    }

    @Test
    @DisplayName("5e. A second Google subject cannot take over an already linked account")
    void conflictingGoogleSubjectIsRejected() {
        String email = randomEmail();
        String firstSub = "google-sub-" + UUID.randomUUID();
        googleIdentityService.handleGoogleLogin(firstSub, email, true, "Owner");
        String secondSub = "google-sub-" + UUID.randomUUID();
        assertThatThrownBy(() -> googleIdentityService.handleGoogleLogin(secondSub, email, true, "Other"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("cannot be linked");
        assertThat(userIdentityRepository.findByProviderAndProviderSubject("GOOGLE", secondSub)).isEmpty();
        assertThat(userIdentityRepository.findByProviderAndProviderSubject("GOOGLE", firstSub)).isPresent();
    }

    @Test
    @DisplayName("6. GET /api/v1/auth/me returns user authProviders accurately")
    void authMeReturnsAuthProviders() throws Exception {
        String email = randomEmail();
        User user = userRepository.save(User.builder()
                .email(email)
                .passwordHash("hashed_pwd")
                .displayName("Multi Provider")
                .role("USER")
                .enabled(true)
                .build());

        String token = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole(), user.getDisplayName());

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.authProviders").isArray())
                .andExpect(jsonPath("$.authProviders[0]").value("PASSWORD"));
    }

    @Test
    @DisplayName("7. GET /api/v1/auth/me without authentication returns 401 Authentication required")
    void authMeWithoutAuthReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Authentication required"));
    }
}
