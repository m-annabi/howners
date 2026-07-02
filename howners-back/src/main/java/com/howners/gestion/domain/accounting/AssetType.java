package com.howners.gestion.domain.accounting;

/**
 * Nature d'une immobilisation amortissable et sa durée d'amortissement linéaire par
 * défaut (années), conforme aux usages LMNP. Le terrain n'est jamais amortissable et
 * n'apparaît donc pas ici.
 */
public enum AssetType {
    BATIMENT(30, "Immeuble (bâti)"),
    MOBILIER(7, "Mobilier et équipements"),
    TRAVAUX(12, "Travaux et agencements"),
    FRAIS(5, "Frais d'acquisition");

    private final int defaultDurationYears;
    private final String label;

    AssetType(int defaultDurationYears, String label) {
        this.defaultDurationYears = defaultDurationYears;
        this.label = label;
    }

    public int getDefaultDurationYears() {
        return defaultDurationYears;
    }

    public String getLabel() {
        return label;
    }
}
