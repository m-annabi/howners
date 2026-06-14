package com.howners.gestion.dto.rental;

import com.howners.gestion.domain.rental.ChargeRegularisation;
import com.howners.gestion.domain.rental.StatutRegularisation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record RegularisationResponse(
        UUID id,
        UUID rentalId,
        String propertyName,
        Integer annee,
        BigDecimal provisionsEncaissees,
        BigDecimal chargesReelles,
        BigDecimal solde,
        Map<String, Object> detail,
        StatutRegularisation statut,
        UUID documentId,
        LocalDateTime createdAt
) {
    public static RegularisationResponse from(ChargeRegularisation regul) {
        return new RegularisationResponse(
                regul.getId(),
                regul.getRental().getId(),
                regul.getRental().getProperty() != null ? regul.getRental().getProperty().getName() : null,
                regul.getAnnee(),
                regul.getProvisionsEncaissees(),
                regul.getChargesReelles(),
                regul.getSolde(),
                regul.getDetail(),
                regul.getStatut(),
                regul.getDocument() != null ? regul.getDocument().getId() : null,
                regul.getCreatedAt()
        );
    }
}
