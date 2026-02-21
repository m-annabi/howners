package com.howners.gestion.domain.rental;

public enum RentalStatus {
    PENDING,      // En attente (contrat pas encore signé)
    ACTIVE,       // Active
    TERMINATED,   // Terminée
    CANCELLED     // Annulée
}
