package com.howners.gestion.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Résolution de l'adresse IP réelle du client, unique pour toute l'application.
 *
 * L'IP est conservée comme preuve (signature de contrat, journal d'audit, consentements
 * RGPD) : elle doit être fiable. Derrière l'unique proxy de confiance (Caddy), l'IP réelle
 * est la DERNIÈRE valeur de X-Forwarded-For — celle que Caddy ajoute lui-même. Prendre la
 * première valeur (ou faire confiance à X-Real-IP) laisserait l'appelant forger l'IP
 * enregistrée en envoyant lui-même un en-tête arbitraire.
 */
public final class ClientIp {

    private ClientIp() {
    }

    /** IP réelle du client pour la requête donnée. */
    public static String resolve(HttpServletRequest request) {
        String ip = null;
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String[] parts = forwarded.split(",");
            ip = parts[parts.length - 1].trim();
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * IP réelle du client pour la requête HTTP courante (services sans accès direct à la
     * requête) ; {@code null} hors contexte web (jobs planifiés, tests).
     */
    public static String resolveFromCurrentRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs != null ? resolve(attrs.getRequest()) : null;
        } catch (Exception e) {
            return null;
        }
    }
}
