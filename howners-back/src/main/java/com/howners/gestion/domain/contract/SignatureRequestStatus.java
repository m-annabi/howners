package com.howners.gestion.domain.contract;

/**
 * Statut d'une demande de signature électronique
 */
public enum SignatureRequestStatus {
    /**
     * Demande créée mais pas encore envoyée
     */
    PENDING,

    /**
     * Email envoyé au signataire
     */
    SENT,

    /**
     * Le signataire a consulté le contrat
     */
    VIEWED,

    /**
     * Le contrat a été signé
     */
    SIGNED,

    /**
     * Le signataire a refusé de signer
     */
    DECLINED,

    /**
     * La demande a été annulée par le propriétaire
     */
    CANCELLED,

    /**
     * La demande a expiré
     */
    EXPIRED
}
