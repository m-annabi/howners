package com.howners.gestion.dto.application;

import com.howners.gestion.domain.application.Application;
import com.howners.gestion.domain.application.ApplicationStatus;
import com.howners.gestion.dto.document.DocumentResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ApplicationResponse(
        UUID id,
        UUID listingId,
        String listingTitle,
        String propertyName,
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
        List<DocumentResponse> documents
) {
    public static ApplicationResponse from(Application a) {
        return from(a, List.of());
    }

    public static ApplicationResponse from(Application a, List<DocumentResponse> documents) {
        return new ApplicationResponse(
                a.getId(),
                a.getListing().getId(),
                a.getListing().getTitle(),
                a.getListing().getProperty().getName(),
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
                documents
        );
    }
}
