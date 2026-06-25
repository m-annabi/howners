package com.howners.gestion.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Validates critical configuration on application startup
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StartupConfigValidator implements ApplicationListener<ApplicationReadyEvent> {

    private final DocuSignProperties docuSignProperties;
    private final StorageProperties storageProperties;
    private final Environment environment;

    // Fragments trahissant une valeur d'exemple laissée en place (rejetés en prod).
    private static final List<String> PLACEHOLDER_MARKERS = List.of(
            "changeme", "change-me", "change-this", "placeholder", "example",
            "your-", "xxxxx", "todo", "a-changer", "secret-in-production");
    // Valeurs par défaut connues (docker-compose / .env.example) — interdites en prod.
    private static final List<String> WEAK_DB_PASSWORDS = List.of(
            "howners_pass", "postgres", "password", "admin", "root");
    private static final List<String> WEAK_MINIO_SECRETS = List.of(
            "minioadmin123", "minioadmin", "password");

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("Validating application configuration...");

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        errors.addAll(validateJwtConfig());
        warnings.addAll(validateDocuSignConfig());  // DocuSign is optional
        warnings.addAll(validateEmailConfig());     // Email is optional
        errors.addAll(validateStorageConfig());
        if (isProd()) {
            errors.addAll(validateProductionSecrets());  // secrets forts obligatoires en prod
        }

        if (!warnings.isEmpty()) {
            log.warn("⚠️  Optional configuration incomplete (some features may not be available):\n  - " + String.join("\n  - ", warnings));
        }

        if (!errors.isEmpty()) {
            String errorMessage = "Configuration validation failed:\n- " + String.join("\n- ", errors);
            log.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }

        log.info("✅ Required configuration validated successfully");
    }

    private List<String> validateJwtConfig() {
        List<String> errors = new ArrayList<>();

        String jwtSecret = environment.getProperty("jwt.secret");
        String[] activeProfiles = environment.getActiveProfiles();
        boolean isProd = Arrays.asList(activeProfiles).contains("prod");

        if (isBlank(jwtSecret)) {
            errors.add("JWT_SECRET is missing");
        } else {
            if (isProd && jwtSecret.equals("change-this-secret-in-production-it-must-be-at-least-256-bits-long-for-hs256-algorithm")) {
                errors.add("JWT_SECRET is using the default fallback value — this is not safe for production");
            }
            if (jwtSecret.length() < 64) {
                errors.add("JWT_SECRET must be at least 64 characters long");
            }
        }

        if (errors.isEmpty()) {
            log.info("✅ JWT configuration is valid");
        }

        return errors;
    }

    private List<String> validateDocuSignConfig() {
        List<String> errors = new ArrayList<>();

        if (isBlank(docuSignProperties.getIntegrationKey())) {
            errors.add("DOCUSIGN_INTEGRATION_KEY is missing");
        }

        if (isBlank(docuSignProperties.getImpersonatedUserGuid())) {
            errors.add("DOCUSIGN_USER_ID is missing");
        }

        if (isBlank(docuSignProperties.getAccountId())) {
            errors.add("DOCUSIGN_ACCOUNT_ID is missing");
        }

        if (isBlank(docuSignProperties.getPrivateKey())) {
            errors.add("DOCUSIGN_PRIVATE_KEY is missing");
        } else {
            // Valider le format de la clé privée
            String privateKey = docuSignProperties.getPrivateKey().replace("\\n", "\n");
            if (!privateKey.contains("BEGIN RSA PRIVATE KEY") && !privateKey.contains("BEGIN PRIVATE KEY")) {
                errors.add("DOCUSIGN_PRIVATE_KEY format is invalid (must be PEM format)");
            }
        }

        if (errors.isEmpty()) {
            log.info("✅ DocuSign configuration is valid");
        }

        return errors;
    }

    private List<String> validateEmailConfig() {
        List<String> errors = new ArrayList<>();

        String host = environment.getProperty("spring.mail.host");
        String port = environment.getProperty("spring.mail.port");
        String username = environment.getProperty("spring.mail.username");
        String password = environment.getProperty("spring.mail.password");

        if (isBlank(host)) {
            errors.add("SMTP_HOST is missing");
        }

        if (isBlank(port)) {
            errors.add("SMTP_PORT is missing");
        }

        if (isBlank(username)) {
            errors.add("SMTP_USERNAME is missing");
        }

        if (isBlank(password)) {
            errors.add("SMTP_PASSWORD is missing");
        }

        if (errors.isEmpty()) {
            log.info("✅ SMTP email configuration is valid");
        }

        return errors;
    }

    private List<String> validateStorageConfig() {
        List<String> errors = new ArrayList<>();

        if (storageProperties.getS3() == null) {
            errors.add("S3 configuration is missing");
            return errors;
        }

        StorageProperties.S3Properties s3 = storageProperties.getS3();

        if (isBlank(s3.getEndpoint())) {
            errors.add("S3_ENDPOINT is missing");
        }

        if (isBlank(s3.getAccessKey())) {
            errors.add("S3_ACCESS_KEY is missing");
        }

        if (isBlank(s3.getSecretKey())) {
            errors.add("S3_SECRET_KEY is missing");
        }

        if (isBlank(s3.getBucket())) {
            errors.add("S3_BUCKET_NAME is missing");
        }

        if (errors.isEmpty()) {
            log.info("✅ S3/MinIO storage configuration is valid");
        }

        return errors;
    }

    /**
     * En profil prod : refuse de démarrer si un secret sensible est manquant, trop court,
     * ou laissé sur une valeur par défaut / d'exemple. Complète les validations ci-dessus.
     */
    private List<String> validateProductionSecrets() {
        List<String> errors = new ArrayList<>();

        // Mot de passe PostgreSQL
        String dbPassword = environment.getProperty("spring.datasource.password");
        if (isBlank(dbPassword)) {
            errors.add("POSTGRES_PASSWORD is missing");
        } else if (WEAK_DB_PASSWORDS.contains(dbPassword.toLowerCase()) || looksLikePlaceholder(dbPassword)) {
            errors.add("POSTGRES_PASSWORD uses a default/example value — set a strong unique secret for production");
        } else if (dbPassword.length() < 12) {
            errors.add("POSTGRES_PASSWORD is too short (< 12 chars) for production");
        }

        // Secret MinIO/S3 (le cas 'manquant' est déjà couvert par validateStorageConfig)
        String s3Secret = storageProperties.getS3() != null ? storageProperties.getS3().getSecretKey() : null;
        if (!isBlank(s3Secret)) {
            if (WEAK_MINIO_SECRETS.contains(s3Secret.toLowerCase()) || looksLikePlaceholder(s3Secret)) {
                errors.add("MINIO_ROOT_PASSWORD uses the default/example value — set a strong unique secret for production");
            } else if (s3Secret.length() < 12) {
                errors.add("MINIO_ROOT_PASSWORD is too short (< 12 chars) for production");
            }
        }

        // Stripe : une clé de test en prod est une erreur de configuration
        String stripeKey = environment.getProperty("stripe.api-key");
        if (!isBlank(stripeKey) && stripeKey.startsWith("sk_test_")) {
            errors.add("STRIPE_SECRET_KEY is a TEST key (sk_test_…) — use a live key (sk_live_…) in production");
        }

        // JWT : longueur/format déjà validés ; on rejette en plus les valeurs d'exemple
        String jwtSecret = environment.getProperty("jwt.secret");
        if (!isBlank(jwtSecret) && looksLikePlaceholder(jwtSecret)) {
            errors.add("JWT_SECRET uses a placeholder/example value — generate a strong unique secret for production");
        }

        if (errors.isEmpty()) {
            log.info("✅ Production secrets validated (no default/placeholder values)");
        }
        return errors;
    }

    private boolean isProd() {
        return Arrays.asList(environment.getActiveProfiles()).contains("prod");
    }

    private boolean looksLikePlaceholder(String value) {
        if (value == null) {
            return false;
        }
        String v = value.toLowerCase();
        return PLACEHOLDER_MARKERS.stream().anyMatch(v::contains);
    }

    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
}
