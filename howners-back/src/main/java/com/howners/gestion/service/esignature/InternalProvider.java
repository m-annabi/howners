package com.howners.gestion.service.esignature;

import com.howners.gestion.dto.esignature.ESignatureRequest;
import com.howners.gestion.dto.esignature.ESignatureResponse;
import com.howners.gestion.dto.esignature.SignatureStatus;
import com.howners.gestion.dto.esignature.WebhookEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Provider de signature interne (canvas HTML5). Aucune dépendance externe :
 * la "demande" est créée localement, le tenant signe via /sign?token=… avec
 * le canvas, et l'URL de signature est posée a posteriori par ContractESignatureService
 * (qui possède le raw token généré par ContractTokenProvider).
 *
 * Utilisé comme fallback automatique quand DocuSign n'est pas configuré et
 * comme option explicite via {@code esignature.provider=internal}.
 */
@Component
@Slf4j
public class InternalProvider implements ESignatureProvider {

    @Override
    public ESignatureResponse createSignatureRequest(ESignatureRequest request) {
        log.info("Creating internal canvas signature request for: {}", request.signerEmail());
        // signingUrl=null → le service le calcule à partir du raw token (frontend /sign?token=...)
        return ESignatureResponse.builder()
                .envelopeId(null)
                .signingUrl(null)
                .status("PENDING")
                .build();
    }

    @Override
    public SignatureStatus getSignatureStatus(String envelopeId) {
        // Le statut est maintenu localement dans contract_signature_requests ;
        // pas besoin d'aller chercher chez un provider externe.
        return SignatureStatus.builder().status("PENDING").build();
    }

    @Override
    public byte[] getSignedDocument(String envelopeId) {
        // Pas applicable au flow canvas : le document signé est stocké via StorageService.
        throw new UnsupportedOperationException("Internal provider does not deliver signed documents externally — fetch via the contract version's fileKey instead.");
    }

    @Override
    public String generateEmbeddedSigningUrl(String envelopeId, String returnUrl) {
        // Pas d'embed external — l'URL est posée par ContractESignatureService.
        return null;
    }

    @Override
    public boolean validateWebhook(String payload, String signature) {
        // Pas de webhook entrant pour le provider interne.
        return false;
    }

    @Override
    public WebhookEvent parseWebhookPayload(String payload) {
        throw new UnsupportedOperationException("Internal provider has no webhooks.");
    }

    @Override
    public void cancelSignatureRequest(String envelopeId) {
        // Annulation gérée localement via ContractSignatureRequest.status = CANCELLED.
        log.debug("Internal provider: cancellation is a local state change, nothing to do here.");
    }
}
