package com.howners.gestion.service.accounting;

/**
 * Marqueur pour le résultat d'un calcul fiscal. Chaque juridiction/régime fournit sa
 * propre implémentation (ex. {@code LmnpResult} pour la France LMNP réel). Permet au
 * moteur d'être générique tout en laissant chaque régime exposer ses données propres.
 */
public interface FiscalResult {
}
