package com.howners.gestion.dto.rental;

import com.howners.gestion.domain.rental.RentRevision;
import com.howners.gestion.domain.rental.StatutRevision;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record RevisionLoyerResponse(
        UUID id,
        UUID rentalId,
        String propertyName,
        BigDecimal ancienLoyer,
        BigDecimal nouveauLoyer,
        IrlIndiceResponse indiceAncien,
        IrlIndiceResponse indiceNouveau,
        LocalDate dateRevision,
        LocalDate dateEffet,
        StatutRevision statut,
        UUID documentId,
        LocalDateTime createdAt
) {
    public static RevisionLoyerResponse from(RentRevision revision) {
        return new RevisionLoyerResponse(
                revision.getId(),
                revision.getRental().getId(),
                revision.getRental().getProperty() != null ? revision.getRental().getProperty().getName() : null,
                revision.getAncienLoyer(),
                revision.getNouveauLoyer(),
                revision.getIndiceAncien() != null ? IrlIndiceResponse.from(revision.getIndiceAncien()) : null,
                revision.getIndiceNouveau() != null ? IrlIndiceResponse.from(revision.getIndiceNouveau()) : null,
                revision.getDateRevision(),
                revision.getDateEffet(),
                revision.getStatut(),
                revision.getDocument() != null ? revision.getDocument().getId() : null,
                revision.getCreatedAt()
        );
    }
}
