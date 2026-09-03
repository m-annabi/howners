package com.howners.gestion.dto.application;

import com.howners.gestion.domain.application.Application;
import com.howners.gestion.domain.application.ApplicationStatus;
import com.howners.gestion.dto.document.DocumentResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ApplicationResponse(
        UUID id,
        UUID listingId,
        String listingTitle,
        String propertyName,
        UUID propertyId,
        BigDecimal listingPricePerMonth,
        String listingCurrency,
        UUID applicantId,
        String applicantName,
        String applicantEmail,
        String coverLetter,
        LocalDate desiredMoveIn,
        ApplicationStatus status,
        String notes,
        String reviewedByName,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt,
        List<DocumentResponse> documents,
        /** Après un refus : le candidat peut-il candidater à nouveau (annonce publiée + dossier mis à jour) ? */
        boolean canReapply,
        /** Renseignés après acceptation : bail créé + contrat lié, pour le suivi côté candidat
            (timeline « contrat à signer / signé »). Nuls tant que la candidature n'est pas acceptée. */
        UUID rentalId,
        UUID contractId,
        String contractStatus
) {
    public static ApplicationResponse from(Application a) {
        return from(a, List.of());
    }

    public static ApplicationResponse from(Application a, List<DocumentResponse> documents) {
        return from(a, documents, false);
    }

    public static ApplicationResponse from(Application a, List<DocumentResponse> documents, boolean canReapply) {
        return from(a, documents, canReapply, null, null, null);
    }

    public static ApplicationResponse from(Application a, List<DocumentResponse> documents, boolean canReapply,
                                           UUID rentalId, UUID contractId, String contractStatus) {
        return new ApplicationResponse(
                a.getId(),
                a.getListing().getId(),
                a.getListing().getTitle(),
                a.getListing().getProperty().getName(),
                a.getListing().getProperty().getId(),
                a.getListing().getPricePerMonth(),
                a.getListing().getCurrency(),
                a.getApplicant().getId(),
                a.getApplicant().getFullName(),
                a.getApplicant().getEmail(),
                a.getCoverLetter(),
                a.getDesiredMoveIn(),
                a.getStatus(),
                a.getNotes(),
                a.getReviewedBy() != null ? a.getReviewedBy().getFullName() : null,
                a.getReviewedAt(),
                a.getCreatedAt(),
                documents,
                canReapply,
                rentalId,
                contractId,
                contractStatus
        );
    }
}
