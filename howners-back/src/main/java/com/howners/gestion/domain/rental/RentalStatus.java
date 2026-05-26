package com.howners.gestion.domain.rental;

public enum RentalStatus {
    VACANT,       // Libre — pas de locataire, pas publié
    LISTED,       // Publié en annonce — candidatures ouvertes
    PENDING,      // Locataire sélectionné — contrat en attente de signature
    ACTIVE,       // Contrat signé — location en cours
    EXITING,      // Sortie programmée — ancien locataire encore présent, relocation en cours
    TERMINATED,   // Terminée (sortie de locataire)
    CANCELLED     // Annulée
}
