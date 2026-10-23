package com.tronai.auth.oauth2;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import java.util.Collections;
import java.util.Iterator;

/**
 * Registers the Google OAuth client only when both real environment values exist.
 * Spring still starts when Google is absent. No placeholder client is registered.
 */
@Configuration
public class OAuth2ClientConfig {

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(
            @Value("${GOOGLE_CLIENT_ID:${oauth2.google.client-id:}}") String googleClientId,
            @Value("${GOOGLE_CLIENT_SECRET:${oauth2.google.client-secret:}}") String googleClientSecret,
            @Value("${GOOGLE_REDIRECT_URI:${oauth2.google.redirect-uri:http://localhost:8080/login/oauth2/code/google}}") String redirectUri) {
        if (!GoogleOAuthCredentials.isConfigured(googleClientId, googleClientSecret)) {
            return new AbsentClientRegistrationRepository();
        }
        ClientRegistration googleRegistration = CommonOAuth2Provider.GOOGLE.getBuilder("google")
                .clientId(googleClientId.trim())
                .clientSecret(googleClientSecret.trim())
                .redirectUri(redirectUri)
                .scope("openid", "profile", "email")
                .build();
        return new org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository(googleRegistration);
    }

    /**
     * Present so Security can start with Google disabled. It contains no clients.
     */
    static final class AbsentClientRegistrationRepository implements ClientRegistrationRepository, Iterable<ClientRegistration> {
        @Override
        public ClientRegistration findByRegistrationId(String registrationId) {
            return null;
        }

        @Override
        public Iterator<ClientRegistration> iterator() {
            return Collections.emptyIterator();
        }
    }
}
