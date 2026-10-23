package com.tronai.auth.oauth2;

import com.tronai.auth.AuthService;
import com.tronai.auth.identity.GoogleIdentityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);

    private final GoogleIdentityService googleIdentityService;

    @Value("${TRON_FRONTEND_URL:${frontend.url:http://localhost:5174}}")
    private String frontendUrl;

    public OAuth2AuthenticationSuccessHandler(GoogleIdentityService googleIdentityService) {
        this.googleIdentityService = googleIdentityService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String baseFrontend = cleanBaseUrl(frontendUrl);
        try {
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
            String sub = null;
            String email = null;
            boolean emailVerified = false;
            String displayName = null;

            if (oAuth2User instanceof OidcUser oidcUser) {
                sub = oidcUser.getSubject();
                email = oidcUser.getEmail();
                Boolean verified = oidcUser.getEmailVerified();
                emailVerified = Boolean.TRUE.equals(verified);
                displayName = oidcUser.getFullName();
            }

            if (sub == null) {
                sub = oAuth2User.getAttribute("sub");
            }
            if (email == null) {
                email = oAuth2User.getAttribute("email");
            }
            if (!emailVerified && oAuth2User.getAttributes().containsKey("email_verified")) {
                Object ev = oAuth2User.getAttribute("email_verified");
                if (ev instanceof Boolean b) {
                    emailVerified = b;
                } else if (ev != null) {
                    emailVerified = Boolean.parseBoolean(ev.toString());
                }
            }
            if (displayName == null) {
                displayName = oAuth2User.getAttribute("name");
            }

            AuthService.AuthResult result = googleIdentityService.handleGoogleLogin(sub, email, emailVerified, displayName);

            // Set secure HttpOnly refresh cookie on response
            response.addHeader(HttpHeaders.SET_COOKIE, result.cookie().toString());

            // Redirect browser to frontend callback - token is never in URL
            String targetUrl = baseFrontend + "/auth/callback?provider=google&status=success";
            response.sendRedirect(targetUrl);
        } catch (Exception e) {
            log.error("Failed to complete Google OAuth authentication", e);
            String safeMsg = e.getMessage() != null ? e.getMessage() : "Google authentication failed";
            String targetUrl = baseFrontend + "/auth/callback?provider=google&status=error&message="
                    + URLEncoder.encode(safeMsg, StandardCharsets.UTF_8);
            response.sendRedirect(targetUrl);
        }
    }

    private static String cleanBaseUrl(String url) {
        if (url == null || url.isBlank()) {
            return "http://localhost:5174";
        }
        String trimmed = url.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
