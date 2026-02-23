package com.howners.gestion.util;

import com.howners.gestion.domain.user.User;

/**
 * Utilitaire pour l'affichage des noms d'utilisateurs.
 */
public final class UserDisplayUtils {

    private UserDisplayUtils() {}

    /**
     * Retourne le nom complet d'un utilisateur (prénom + nom).
     * Gère les cas null de manière sûre.
     */
    public static String getFullName(User user) {
        if (user == null) return "";
        String firstName = user.getFirstName() != null ? user.getFirstName() : "";
        String lastName = user.getLastName() != null ? user.getLastName() : "";
        return (firstName + " " + lastName).trim();
    }
}
