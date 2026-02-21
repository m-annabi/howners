package com.howners.gestion.service.esignature;

import com.howners.gestion.dto.esignature.*;

/**
 * Interface pour les fournisseurs de signature électronique
 */
public interface ESignatureProvider {

    /**
     * Crée une demande de signature électronique
     *
     * @param request les informations de la demande
     * @return la réponse avec l'ID de l'enveloppe et l'URL de signature
     */
    ESignatureResponse createSignatureRequest(ESignatureRequest request);

    /**
     * Récupère le statut d'une signature
     *
     * @param envelopeId l'ID de l'enveloppe
     * @return le statut de la signature
     */
    SignatureStatus getSignatureStatus(String envelopeId);

    /**
     * Télécharge le document signé
     *
     * @param envelopeId l'ID de l'enveloppe
     * @return le contenu du document signé
     */
    byte[] getSignedDocument(String envelopeId);

    /**
     * Génère une URL de signature embarquée (embedded signing)
     *
     * @param envelopeId l'ID de l'enveloppe
     * @param returnUrl l'URL de retour après signature
     * @return l'URL de signature
     */
    String generateEmbeddedSigningUrl(String envelopeId, String returnUrl);

    /**
     * Valide un webhook provenant du fournisseur
     *
     * @param payload le contenu du webhook
     * @param signature la signature du webhook
     * @return true si le webhook est valide
     */
    boolean validateWebhook(String payload, String signature);

    /**
     * Parse le contenu d'un webhook
     *
     * @param payload le contenu du webhook
     * @return l'événement parsé
     */
    WebhookEvent parseWebhookPayload(String payload);

    /**
     * Annule une demande de signature
     *
     * @param envelopeId l'ID de l'enveloppe
     */
    void cancelSignatureRequest(String envelopeId);
}
