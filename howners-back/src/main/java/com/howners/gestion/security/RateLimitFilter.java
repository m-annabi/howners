package com.howners.gestion.security;

import com.howners.gestion.config.RateLimitConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitConfig rateLimitConfig;

    private final Map<String, RateLimitBucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!rateLimitConfig.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientKey = getClientKey(request);
        RateLimitBucket bucket = buckets.computeIfAbsent(clientKey, k -> new RateLimitBucket());

        if (!bucket.tryConsume(rateLimitConfig.getRequestsPerMinute())) {
            log.warn("Rate limit exceeded for client: {}", clientKey);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too many requests. Please try again later.\"}");
            return;
        }

        response.setHeader("X-RateLimit-Limit", String.valueOf(rateLimitConfig.getRequestsPerMinute()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(
                Math.max(0, rateLimitConfig.getRequestsPerMinute() - bucket.getCount())));

        filterChain.doFilter(request, response);
    }

    private String getClientKey(HttpServletRequest request) {
        // Use authenticated user if available, otherwise fall back to IP
        String user = request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : null;
        if (user != null) {
            return "user:" + user;
        }

        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return "ip:" + forwarded.split(",")[0].trim();
        }
        return "ip:" + request.getRemoteAddr();
    }

    private static class RateLimitBucket {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long windowStart = System.currentTimeMillis();

        boolean tryConsume(int limit) {
            long now = System.currentTimeMillis();
            // Reset window every minute
            if (now - windowStart > 60_000) {
                count.set(0);
                windowStart = now;
            }
            return count.incrementAndGet() <= limit;
        }

        int getCount() {
            return count.get();
        }
    }
}
