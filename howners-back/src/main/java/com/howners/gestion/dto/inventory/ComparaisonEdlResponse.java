package com.howners.gestion.dto.inventory;

import com.howners.gestion.domain.inventory.StatutComparaison;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ComparaisonEdlResponse(
        UUID id,
        UUID rentalId,
        UUID edlEntreeId,
        UUID edlSortieId,
        LocalDate dateEntree,
        LocalDate dateSortie,
        List<PieceComparee> pieces,
        List<CompteurCompare> compteurs,
        Integer clesEntree,
        Integer clesSortie,
        List<RetenueDepot> retenues,
        BigDecimal totalRetenues,
        BigDecimal depositAmount,
        BigDecimal soldeARestituer,
        StatutComparaison statut,
        UUID documentId
) {
    public record PieceComparee(
            String nom,
            String etatEntree,
            String etatSortie,
            String commentairesEntree,
            String commentairesSortie,
            boolean degradee,
            boolean nonComparable
    ) {}

    public record CompteurCompare(
            String type,
            String releveEntree,
            String releveSortie
    ) {}

    public record RetenueDepot(
            String piece,
            String etatEntree,
            String etatSortie,
            String motif,
            BigDecimal montant
    ) {}
}
