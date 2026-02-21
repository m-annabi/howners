package com.howners.gestion.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for DocuSign
 */
@Configuration
@ConfigurationProperties(prefix = "esignature.docusign")
@Validated
@Data
public class DocuSignProperties {

    private String integrationKey;

    private String userId;

    private String accountId;

    private String basePath;

    private String oauthBasePath;

    private String privateKey;

    private String impersonatedUserGuid;

    @NotNull(message = "DocuSign token expiration is required")
    private Integer expiresInSeconds = 3600;

    private String webhookSecret; // Optional - for webhook HMAC validation

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }
}
