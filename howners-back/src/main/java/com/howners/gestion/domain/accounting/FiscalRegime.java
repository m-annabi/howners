package com.howners.gestion.domain.accounting;

/**
 * Régime fiscal d'une activité. La V1 ne gère que le LMNP meublé au réel (BIC),
 * seul régime produisant un vrai bilan. Les autres sont réservés (V2+).
 */
public enum FiscalRegime {
    LMNP_REEL
    // REVENUS_FONCIERS_REEL, SCI_IS, CH_* (V2)
}
