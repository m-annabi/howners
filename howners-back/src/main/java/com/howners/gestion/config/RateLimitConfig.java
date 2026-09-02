package com.howners.gestion.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "rate-limit")
@Data
public class RateLimitConfig {

    private boolean enabled = true;
    private int requestsPerMinute = 60;
    // Limite stricte, séparée, pour les endpoints d'authentification (anti brute-force login/reset).
    private int authRequestsPerMinute = 10;
}
