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

    // Borne dure sur le nombre de compteurs en mémoire : sans elle, chaque clé distincte crée une
    // entrée jamais évincée → épuisement mémoire possible.
    private static final int MAX_BUCKETS = 50_000;

    private final Map<String, RateLimitBucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!rateLimitConfig.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Les endpoints d'authentification ont une limite stricte et un espace de clés séparé,
        // pour freiner le brute-force sans pénaliser le trafic applicatif normal.
        boolean auth = isAuthEndpoint(request);
        int limit = auth ? rateLimitConfig.getAuthRequestsPerMinute() : rateLimitConfig.getRequestsPerMinute();
        String clientKey = (auth ? "auth:" : "app:") + getClientKey(request);
        evictIfNeeded();
        RateLimitBucket bucket = buckets.computeIfAbsent(clientKey, k -> new RateLimitBucket());

        if (!bucket.tryConsume(limit)) {
            log.warn("Rate limit exceeded for client: {}", clientKey);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too many requests. Please try again later.\"}");
            return;
        }

        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, limit - bucket.getCount())));

        filterChain.doFilter(request, response);
    }

    /** Empêche la map de compteurs de croître sans borne : évince les fenêtres périmées, purge en dernier recours. */
    private void evictIfNeeded() {
        if (buckets.size() <= MAX_BUCKETS) {
            return;
        }
        long now = System.currentTimeMillis();
        buckets.entrySet().removeIf(e -> now - e.getValue().getWindowStart() > 120_000);
        if (buckets.size() > MAX_BUCKETS) {
            buckets.clear();
        }
    }

    /** Endpoints sensibles au brute-force (connexion, inscription, réinitialisation de mot de passe). */
    private boolean isAuthEndpoint(HttpServletRequest request) {
        String p = request.getServletPath();
        if (p == null) {
            return false;
        }
        return p.equals("/api/auth/login")
                || p.equals("/api/auth/register")
                || p.contains("password")
                || p.contains("forgot")
                || p.contains("reset");
    }

    private String getClientKey(HttpServletRequest request) {
        // Use authenticated user if available, otherwise fall back to IP
        String user = request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : null;
        if (user != null) {
            return "user:" + user;
        }

        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            // Derrière un unique proxy de confiance (Caddy), l'IP réelle du client est la DERNIÈRE
            // valeur de X-Forwarded-For — celle que Caddy ajoute lui-même. Prendre la première
            // laisserait le client usurper sa clé (rotation d'en-tête = contournement du rate limit,
            // et explosion de la map de buckets). NB : à revoir si un second proxy/CDN est ajouté devant Caddy.
            String[] parts = forwarded.split(",");
            return "ip:" + parts[parts.length - 1].trim();
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

        long getWindowStart() {
            return windowStart;
        }
    }
}
