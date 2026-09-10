package com.howners.gestion.dto.contract;

/**
 * Mentions structurantes du bail saisies via un formulaire plutôt qu'en texte libre dans le
 * modèle (audit T-06/T-04). Ces valeurs alimentent les variables {{contract.*}} substituées
 * dans le contenu du contrat, en remplacement des crochets [à compléter].
 *
 * Tous les champs sont optionnels : un champ vide laisse le texte par défaut du modèle inchangé.
 */
public record ContractDetails(
        // T-06 — mentions structurantes
        String regimeJuridique,       // "monopropriété" | "copropriété"
        String typeHabitat,           // "immeuble collectif" | "immeuble individuel"
        String periodeConstruction,   // ex. "1949-1974"
        String regimeCharges,         // "provisions" | "forfait"

        // T-04 — encadrement des loyers (zones tendues)
        Boolean zoneEncadree,
        String loyerReference,
        String loyerReferenceMajore,
        String complementLoyer,
        String justificationComplement
) {
}
