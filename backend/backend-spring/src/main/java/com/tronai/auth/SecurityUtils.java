package com.tronai.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static Optional<UserPrincipal> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            Object p = auth.getPrincipal();
            if (p instanceof UserPrincipal up) {
                return Optional.of(up);
            }
            if (p instanceof UserDetails ud && !"anonymousUser".equals(ud.getUsername())) {
                return Optional.of(UserPrincipal.builder()
                        .id(1L)
                        .email(ud.getUsername().contains("@") ? ud.getUsername() : ud.getUsername() + "@tron.ai")
                        .displayName(ud.getUsername())
                        .role("USER")
                        .build());
            }
            if (p instanceof String s && !"anonymousUser".equals(s)) {
                return Optional.of(UserPrincipal.builder()
                        .id(1L)
                        .email(s.contains("@") ? s : s + "@tron.ai")
                        .displayName(s)
                        .role("USER")
                        .build());
            }
        }
        return Optional.empty();
    }

    public static UserPrincipal getRequiredCurrentUser() {
        return getCurrentUser().orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required"));
    }

    public static Long getRequiredUserId() {
        return getRequiredCurrentUser().getId();
    }
}
