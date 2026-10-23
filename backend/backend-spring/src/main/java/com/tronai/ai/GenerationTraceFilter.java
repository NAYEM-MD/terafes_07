package com.tronai.ai;

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
 * Drops any generation trace left on a pooled request thread.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GenerationTraceFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        GenerationTraceHolder.clear();
        try {
            filterChain.doFilter(request, response);
        } finally {
            GenerationTraceHolder.clear();
        }
    }
}
