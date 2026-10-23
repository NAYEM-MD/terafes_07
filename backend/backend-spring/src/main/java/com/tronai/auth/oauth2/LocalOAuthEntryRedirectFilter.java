package com.tronai.auth.oauth2;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * The registered Google callback is http://localhost:8080/login/oauth2/code/google.
 * The web app often calls 127.0.0.1. Those hosts do not share cookies, so the
 * authorization request must start on localhost.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LocalOAuthEntryRedirectFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        if ("127.0.0.1".equalsIgnoreCase(request.getServerName())
                && uri != null
                && uri.startsWith("/oauth2/authorization/")
                && request.getServerPort() == 8080) {
            String target = "http://localhost:8080" + uri;
            if (request.getQueryString() != null && !request.getQueryString().isBlank()) {
                target = target + "?" + request.getQueryString();
            }
            response.sendRedirect(target);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
