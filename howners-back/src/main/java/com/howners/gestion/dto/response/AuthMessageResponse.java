package com.howners.gestion.dto.response;

/**
 * Réponse générique (sans jeton) pour les flux d'authentification qui ne doivent rien révéler :
 * inscription, renvoi de vérification, etc. Le message est volontairement neutre et identique
 * quel que soit l'état réel du compte, afin de ne pas exposer l'existence d'une adresse.
 */
public record AuthMessageResponse(String message) {}
