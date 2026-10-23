package com.tronai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Fails production startup when required secrets are missing.
 * Logs only that the check passed. Never logs secret values.
 */
@Component
@Profile("prod")
public class ProductionStartupValidator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ProductionStartupValidator.class);
    private static final String DEV_JWT_MARKER = "development-only";

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${cors.allowed-origins:}")
    private String corsAllowedOrigins;

    @Value("${cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${cookie.same-site:Lax}")
    private String cookieSameSite;

    @Override
    public void run(ApplicationArguments args) {
        if (dbPassword == null || dbPassword.isBlank()) {
            throw new IllegalStateException("DB_PASSWORD is required when the prod profile is active");
        }
        if (jwtSecret == null || jwtSecret.isBlank() || jwtSecret.length() < 32 || jwtSecret.contains(DEV_JWT_MARKER)) {
            throw new IllegalStateException("JWT_SECRET is required when the prod profile is active and must be at least 32 characters");
        }
        if (corsAllowedOrigins == null || corsAllowedOrigins.isBlank() || corsAllowedOrigins.contains("*")) {
            throw new IllegalStateException("CORS_ALLOWED_ORIGINS is required when the prod profile is active and must not be *");
        }
        if ("None".equalsIgnoreCase(cookieSameSite) && !cookieSecure) {
            throw new IllegalStateException("COOKIE_SECURE must be true when COOKIE_SAME_SITE=None");
        }
        log.info("Production startup checks passed for database password, JWT secret, CORS, and cookie settings");
    }
}
