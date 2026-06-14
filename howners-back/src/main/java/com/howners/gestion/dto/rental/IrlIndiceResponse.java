package com.howners.gestion.dto.rental;

import com.howners.gestion.domain.rental.IrlIndice;

import java.math.BigDecimal;
import java.util.UUID;

public record IrlIndiceResponse(
        UUID id,
        Integer annee,
        Integer trimestre,
        BigDecimal valeur
) {
    public static IrlIndiceResponse from(IrlIndice indice) {
        return new IrlIndiceResponse(indice.getId(), indice.getAnnee(), indice.getTrimestre(), indice.getValeur());
    }
}
