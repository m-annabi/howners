package com.howners.gestion.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Provider pour la génération et validation de tokens sécurisés pour l'accès aux contrats
 */
@Component
@Slf4j
public class ContractTokenProvider {

    private static final int TOKEN_LENGTH = 32; // 32 bytes = 256 bits
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    /**
     * Génère un token unique et sécurisé
     * Utilise SecureRandom pour générer 32 bytes aléatoires encodés en Base64 URL-safe
     *
     * @return le token généré (non hashé)
     */
    public String generateToken() {
        byte[] randomBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(randomBytes);

        // Utilise Base64 URL-safe pour éviter les caractères problématiques dans les URLs
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        log.debug("Generated new contract access token (length: {})", token.length());
        return token;
    }

    /**
     * Hash un token avec BCrypt
     * Le hash sera stocké en base de données
     *
     * @param token le token en clair
     * @return le hash du token
     */
    public String hashToken(String token) {
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }

        String hash = passwordEncoder.encode(token);
        log.debug("Token hashed successfully");
        return hash;
    }

    /**
     * Valide un token en le comparant avec son hash
     *
     * @param rawToken le token en clair fourni par l'utilisateur
     * @param hashedToken le hash du token stocké en base
     * @return true si le token est valide
     */
    public boolean validateToken(String rawToken, String hashedToken) {
        if (rawToken == null || hashedToken == null) {
            log.warn("Token validation failed: null token or hash");
            return false;
        }

        try {
            boolean matches = passwordEncoder.matches(rawToken, hashedToken);
            if (matches) {
                log.debug("Token validated successfully");
            } else {
                log.warn("Token validation failed: token does not match hash");
            }
            return matches;
        } catch (Exception e) {
            log.error("Error validating token", e);
            return false;
        }
    }

    /**
     * Génère un token et retourne à la fois le token brut et son hash
     * Utile pour créer une nouvelle demande de signature
     *
     * @return un tableau avec [0] = token brut, [1] = token hashé
     */
    public String[] generateAndHashToken() {
        String rawToken = generateToken();
        String hashedToken = hashToken(rawToken);
        return new String[]{rawToken, hashedToken};
    }
}
