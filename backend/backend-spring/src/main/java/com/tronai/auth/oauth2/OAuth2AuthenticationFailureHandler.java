package com.tronai.auth.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthenticationFailureHandler.class);

    @Value("${TRON_FRONTEND_URL:${frontend.url:http://localhost:5174}}")
    private String frontendUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        log.warn("OAuth2 authentication failure: {}", exception.getMessage());
        String baseFrontend = frontendUrl != null && !frontendUrl.isBlank() ? frontendUrl.trim() : "http://localhost:5174";
        while (baseFrontend.endsWith("/")) {
            baseFrontend = baseFrontend.substring(0, baseFrontend.length() - 1);
        }
        String safeDetail = exception.getMessage() != null ? exception.getMessage() : "OAuth authentication failed";
        String targetUrl = baseFrontend + "/auth/callback?provider=google&status=error&message="
                + URLEncoder.encode(safeDetail, StandardCharsets.UTF_8);
        response.sendRedirect(targetUrl);
    }
}
