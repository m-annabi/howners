package com.howners.gestion.service.esignature;

import com.docusign.esign.api.EnvelopesApi;
import com.docusign.esign.client.ApiClient;
import com.docusign.esign.client.ApiException;
import com.docusign.esign.client.auth.OAuth;
import com.docusign.esign.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.howners.gestion.config.DocuSignProperties;
import com.howners.gestion.dto.esignature.*;
import com.howners.gestion.exception.esignature.ESignatureProviderException;
import com.howners.gestion.exception.esignature.WebhookValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * Implémentation DocuSign du provider de signature électronique
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocuSignProvider implements ESignatureProvider {

    private final DocuSignProperties docuSignProperties;
    private final ObjectMapper objectMapper;

    private ApiClient apiClient;

    /**
     * Initialise le client API DocuSign avec authentification JWT
     */
    private ApiClient getApiClient() {
        if (apiClient == null) {
            apiClient = new ApiClient(docuSignProperties.getBasePath());
            apiClient.setOAuthBasePath(docuSignProperties.getOauthBasePath());

            try {
                // Authentification JWT
                String rawPrivateKey = docuSignProperties.getPrivateKey();
                log.info("DocuSign private key: {}", rawPrivateKey != null && !rawPrivateKey.isEmpty() ? "configured" : "MISSING");

                if (rawPrivateKey == null || rawPrivateKey.isEmpty()) {
                    throw new ESignatureProviderException(
                            "DocuSign private key is not configured. Please set DOCUSIGN_PRIVATE_KEY environment variable.",
                            "DOCUSIGN_CONFIG_ERROR");
                }

                // Remplacer les \n littéraux par de vraies nouvelles lignes
                String privateKey = rawPrivateKey.replace("\\n", "\n");
                byte[] privateKeyBytes = privateKey.getBytes();
                List<String> scopes = Arrays.asList(
                        OAuth.Scope_SIGNATURE,
                        OAuth.Scope_IMPERSONATION
                );

                OAuth.OAuthToken oAuthToken = apiClient.requestJWTUserToken(
                        docuSignProperties.getIntegrationKey(),
                        docuSignProperties.getImpersonatedUserGuid(),
                        scopes,
                        privateKeyBytes,
                        docuSignProperties.getExpiresInSeconds()
                );

                apiClient.setAccessToken(oAuthToken.getAccessToken(), oAuthToken.getExpiresIn());

                log.info("DocuSign API client initialized successfully");
            } catch (ESignatureProviderException e) {
                throw e; // Re-throw our own exceptions
            } catch (Exception e) {
                log.error("Failed to initialize DocuSign API client", e);
                throw new ESignatureProviderException(
                        "Failed to authenticate with DocuSign: " + e.getMessage(),
                        "DOCUSIGN_AUTH_ERROR",
                        e);
            }
        }
        return apiClient;
    }

    @Override
    public ESignatureResponse createSignatureRequest(ESignatureRequest request) {
        log.info("Creating DocuSign signature request for: {}", request.signerEmail());

        try {
            ApiClient client = getApiClient();
            EnvelopesApi envelopesApi = new EnvelopesApi(client);

            // Créer le document
            Document document = new Document();
            document.setDocumentBase64(Base64.getEncoder().encodeToString(request.documentContent()));
            document.setName(request.documentName());
            document.setFileExtension("pdf");
            document.setDocumentId("1");

            // Créer le signataire
            Signer signer = new Signer();
            signer.setEmail(request.signerEmail());
            signer.setName(request.signerName());
            signer.setRecipientId("1");
            signer.setRoutingOrder("1");
            signer.setClientUserId("1"); // Pour embedded signing

            // Ajouter un onglet de signature
            SignHere signHere = new SignHere();
            signHere.setDocumentId("1");
            signHere.setPageNumber("1");
            signHere.setXPosition("100");
            signHere.setYPosition("100");

            Tabs tabs = new Tabs();
            tabs.setSignHereTabs(Arrays.asList(signHere));
            signer.setTabs(tabs);

            // Créer les destinataires
            Recipients recipients = new Recipients();
            recipients.setSigners(Arrays.asList(signer));

            // Créer l'enveloppe
            EnvelopeDefinition envelopeDefinition = new EnvelopeDefinition();
            envelopeDefinition.setEmailSubject("Signature de votre contrat de location");
            envelopeDefinition.setDocuments(Arrays.asList(document));
            envelopeDefinition.setRecipients(recipients);
            envelopeDefinition.setStatus("sent");

            // Définir l'expiration
            if (request.expirationDays() != null) {
                Notification notification = new Notification();
                notification.setUseAccountDefaults("false");
                Expirations expirations = new Expirations();
                expirations.setExpireEnabled("true");
                expirations.setExpireAfter(String.valueOf(request.expirationDays()));
                expirations.setExpireWarn(String.valueOf(request.expirationDays() - 3));
                notification.setExpirations(expirations);
                envelopeDefinition.setNotification(notification);
            }

            // Créer l'enveloppe
            EnvelopeSummary envelopeSummary = envelopesApi.createEnvelope(
                    docuSignProperties.getAccountId(),
                    envelopeDefinition
            );

            String envelopeId = envelopeSummary.getEnvelopeId();
            log.info("DocuSign envelope created with ID: {}", envelopeId);

            // Générer l'URL de signature embarquée
            String signingUrl = generateEmbeddedSigningUrl(envelopeId, request.returnUrl());

            return ESignatureResponse.builder()
                    .envelopeId(envelopeId)
                    .signingUrl(signingUrl)
                    .status(envelopeSummary.getStatus())
                    .build();

        } catch (ApiException e) {
            log.error("DocuSign API error: {}", e.getMessage(), e);
            throw new ESignatureProviderException(
                    "DocuSign API error: " + e.getMessage(),
                    "DOCUSIGN_API_ERROR",
                    e);
        } catch (Exception e) {
            log.error("Unexpected error creating signature request", e);
            throw new ESignatureProviderException(
                    "Failed to create signature request: " + e.getMessage(),
                    "DOCUSIGN_ERROR",
                    e);
        }
    }

    @Override
    public SignatureStatus getSignatureStatus(String envelopeId) {
        log.info("Getting signature status for envelope: {}", envelopeId);

        try {
            ApiClient client = getApiClient();
            EnvelopesApi envelopesApi = new EnvelopesApi(client);

            Envelope envelope = envelopesApi.getEnvelope(
                    docuSignProperties.getAccountId(),
                    envelopeId
            );

            LocalDateTime sentDateTime = envelope.getSentDateTime() != null
                    ? LocalDateTime.parse(envelope.getSentDateTime())
                    : null;

            LocalDateTime completedDateTime = envelope.getCompletedDateTime() != null
                    ? LocalDateTime.parse(envelope.getCompletedDateTime())
                    : null;

            return SignatureStatus.builder()
                    .envelopeId(envelopeId)
                    .status(envelope.getStatus())
                    .sentDateTime(sentDateTime)
                    .completedDateTime(completedDateTime)
                    .signerEmail(envelope.getEmailSubject())
                    .build();

        } catch (ApiException e) {
            log.error("Failed to get signature status for envelope: {}", envelopeId, e);
            throw new ESignatureProviderException(
                    "Failed to get signature status from DocuSign: " + e.getMessage(),
                    "DOCUSIGN_API_ERROR",
                    e);
        }
    }

    @Override
    public byte[] getSignedDocument(String envelopeId) {
        log.info("Downloading signed document for envelope: {}", envelopeId);

        try {
            ApiClient client = getApiClient();
            EnvelopesApi envelopesApi = new EnvelopesApi(client);

            byte[] document = envelopesApi.getDocument(
                    docuSignProperties.getAccountId(),
                    envelopeId,
                    "combined" // Get all documents combined
            );

            log.info("Successfully downloaded signed document for envelope: {}", envelopeId);
            return document;

        } catch (ApiException e) {
            log.error("Failed to download signed document for envelope: {}", envelopeId, e);
            throw new ESignatureProviderException(
                    "Failed to download signed document from DocuSign: " + e.getMessage(),
                    "DOCUSIGN_API_ERROR",
                    e);
        }
    }

    @Override
    public String generateEmbeddedSigningUrl(String envelopeId, String returnUrl) {
        log.info("Generating embedded signing URL for envelope: {}", envelopeId);

        try {
            ApiClient client = getApiClient();
            EnvelopesApi envelopesApi = new EnvelopesApi(client);

            RecipientViewRequest viewRequest = new RecipientViewRequest();
            viewRequest.setReturnUrl(returnUrl);
            viewRequest.setAuthenticationMethod("none");
            viewRequest.setClientUserId("1");
            viewRequest.setUserName("Signer");
            viewRequest.setEmail("signer@example.com");

            ViewUrl viewUrl = envelopesApi.createRecipientView(
                    docuSignProperties.getAccountId(),
                    envelopeId,
                    viewRequest
            );

            log.info("Generated embedded signing URL for envelope: {}", envelopeId);
            return viewUrl.getUrl();

        } catch (ApiException e) {
            log.error("Failed to generate embedded signing URL for envelope: {}", envelopeId, e);
            throw new ESignatureProviderException(
                    "Failed to generate signing URL from DocuSign: " + e.getMessage(),
                    "DOCUSIGN_API_ERROR",
                    e);
        }
    }

    @Override
    public boolean validateWebhook(String payload, String signature) {
        log.debug("Validating DocuSign webhook with HMAC");

        if (signature == null || signature.isEmpty()) {
            log.warn("Webhook signature is missing");
            return false;
        }

        try {
            // DocuSign envoie la signature dans l'header X-DocuSign-Signature-1
            // Format: HMAC-SHA256(connectKey + payload)
            String connectKey = docuSignProperties.getWebhookSecret();

            if (connectKey == null || connectKey.isEmpty()) {
                log.error("DocuSign webhook secret (connect key) is not configured");
                return false;
            }

            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(connectKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmac.init(secretKey);

            byte[] hash = hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computedSignature = Base64.getEncoder().encodeToString(hash);

            boolean valid = MessageDigest.isEqual(
                    computedSignature.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8)
            );

            if (!valid) {
                log.warn("Webhook signature validation failed - potential security issue");
            } else {
                log.debug("Webhook signature validated successfully");
            }

            return valid;

        } catch (Exception e) {
            log.error("Error validating webhook signature", e);
            return false;
        }
    }

    @Override
    public WebhookEvent parseWebhookPayload(String payload) {
        log.info("Parsing DocuSign webhook payload");

        try {
            // Parse le JSON du webhook DocuSign
            var jsonNode = objectMapper.readTree(payload);
            var data = jsonNode.get("data");
            var envelopeSummary = data.get("envelopeSummary");

            String envelopeId = envelopeSummary.get("envelopeId").asText();
            String status = envelopeSummary.get("status").asText();
            String eventType = jsonNode.get("event").asText();

            return WebhookEvent.builder()
                    .envelopeId(envelopeId)
                    .eventType(eventType)
                    .status(status)
                    .eventDateTime(LocalDateTime.now())
                    .build();

        } catch (IOException e) {
            log.error("Failed to parse webhook payload", e);
            throw new WebhookValidationException(
                    "Failed to parse webhook payload: " + e.getMessage(),
                    "WEBHOOK_PARSE_ERROR",
                    e);
        }
    }

    @Override
    public void cancelSignatureRequest(String envelopeId) {
        log.info("Cancelling signature request for envelope: {}", envelopeId);

        try {
            ApiClient client = getApiClient();
            EnvelopesApi envelopesApi = new EnvelopesApi(client);

            Envelope envelope = new Envelope();
            envelope.setStatus("voided");
            envelope.setVoidedReason("Cancelled by owner");

            envelopesApi.update(
                    docuSignProperties.getAccountId(),
                    envelopeId,
                    envelope
            );

            log.info("Successfully cancelled signature request for envelope: {}", envelopeId);

        } catch (ApiException e) {
            log.error("Failed to cancel signature request for envelope: {}", envelopeId, e);
            throw new ESignatureProviderException(
                    "Failed to cancel signature request in DocuSign: " + e.getMessage(),
                    "DOCUSIGN_API_ERROR",
                    e);
        }
    }
}
