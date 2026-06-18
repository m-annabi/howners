package com.howners.gestion.service.rgpd;

import com.howners.gestion.domain.document.DocumentType;

import java.util.Set;

/**
 * Politique de conservation appliquée lors d'un effacement RGPD (art. 17).
 *
 * Certains documents relèvent d'une obligation légale de conservation et bénéficient de
 * l'exception de l'art. 17-3 (respect d'une obligation légale / constatation, exercice ou
 * défense de droits en justice) — ils sont CONSERVÉS (mis sous legal hold), non supprimés.
 * Tout le reste est considéré comme une pièce personnelle (PII) et SUPPRIMÉ.
 *
 * Allowlist de conservation (raisons) :
 *  - CONTRACT        : bail signé, valeur probante (≈5 ans après fin de bail)
 *  - INVOICE         : facture, obligation comptable (10 ans)
 *  - RECEIPT         : quittance de loyer, preuve de paiement
 *  - INVENTORY       : état des lieux, preuve en cas de litige sur le dépôt de garantie
 *  - MISE_EN_DEMEURE : preuve en cas de contentieux
 *  - SIGNATURE       : élément constitutif d'un acte signé
 *  - PHOTOS          : données rattachées au bien, non PII de la personne
 *
 * Sont donc SUPPRIMÉS (pièces personnelles) : pièce d'identité, justificatifs de revenus /
 * de domicile, relevés bancaires, avis d'imposition, contrats de travail, et tout type non
 * listé (OTHER inclus) — par défaut protecteur de la vie privée.
 */
public final class DocumentRetentionPolicy {

    private static final Set<DocumentType> RETAINED_ON_ERASURE = Set.of(
            DocumentType.CONTRACT,
            DocumentType.INVOICE,
            DocumentType.RECEIPT,
            DocumentType.INVENTORY,
            DocumentType.MISE_EN_DEMEURE,
            DocumentType.SIGNATURE,
            DocumentType.PHOTOS
    );

    private DocumentRetentionPolicy() {}

    /** true = document conservé sous obligation légale ; false = pièce personnelle à supprimer. */
    public static boolean isRetainedOnErasure(DocumentType type) {
        return type != null && RETAINED_ON_ERASURE.contains(type);
    }
}
