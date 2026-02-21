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

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("Validating application configuration...");

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        warnings.addAll(validateDocuSignConfig());  // DocuSign is optional
        warnings.addAll(validateEmailConfig());     // Email is optional
        errors.addAll(validateStorageConfig());

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

    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
}
