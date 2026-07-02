package com.howners.gestion.service.accounting;

import com.howners.gestion.domain.accounting.FiscalActivity;
import com.howners.gestion.domain.accounting.FiscalJurisdiction;
import com.howners.gestion.domain.accounting.FiscalRegime;

import java.util.List;

/**
 * Moteur fiscal d'une juridiction + régime. Toute la logique spécifique à un pays
 * (calcul du résultat, documents produits) passe par cette interface, ce qui rend le
 * module extensible : ajouter la Suisse = fournir une nouvelle implémentation, sans
 * toucher au contrôleur ni au domaine commun.
 */
public interface FiscalEngine {

    FiscalJurisdiction jurisdiction();

    FiscalRegime regime();

    /** Calcule le résultat fiscal d'un exercice (année civile). */
    FiscalResult computeResult(FiscalActivity activity, int year);

    /** Produit les documents de l'exercice (bilan, compte de résultat, FEC…). */
    List<GeneratedDocument> generateDocuments(FiscalActivity activity, int year);
}
