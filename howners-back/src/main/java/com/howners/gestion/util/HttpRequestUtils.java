package com.howners.gestion.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Utilitaire pour l'extraction d'informations des requêtes HTTP.
 */
public final class HttpRequestUtils {

    private HttpRequestUtils() {}

    /**
     * Récupère l'adresse IP du client en tenant compte des proxies (X-Forwarded-For, X-Real-IP).
     */
    public static String getClientIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("X-Real-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }
        return ipAddress;
    }
}
