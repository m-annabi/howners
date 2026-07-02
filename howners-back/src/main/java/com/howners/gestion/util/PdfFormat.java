package com.howners.gestion.util;

import com.howners.gestion.domain.property.Property;
import com.howners.gestion.domain.user.User;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Helpers de formatage partagés par les générateurs de PDF, pour un rendu homogène
 * et « prêt à l'emploi » (montants en euros à la française, adresses null-safe).
 */
public final class PdfFormat {

    private PdfFormat() {}

    private static final DecimalFormatSymbols FR_SYMBOLS = new DecimalFormatSymbols(Locale.FRANCE);

    /** Formate un montant en euros à la française : 1234.5 → "1 234,50 €". */
    public static String montant(BigDecimal value) {
        if (value == null) return "—";
        DecimalFormat df = new DecimalFormat("#,##0.00", FR_SYMBOLS);
        df.setGroupingSize(3);
        return df.format(value) + " €";
    }

    /** Idem à partir d'un double (compat builders existants). */
    public static String montant(double value) {
        return montant(BigDecimal.valueOf(value));
    }

    /**
     * Adresse postale complète et null-safe d'un bien, sur une ou plusieurs lignes.
     * Évite les rendus « null, Ville » ou les virgules orphelines.
     */
    public static String adressePostale(Property property) {
        if (property == null) return "—";
        StringBuilder sb = new StringBuilder();
        append(sb, property.getAddressLine1());
        append(sb, property.getAddressLine2());
        String cp = safe(property.getPostalCode());
        String ville = safe(property.getCity());
        String cpVille = (cp + " " + ville).trim();
        append(sb, cpVille.isEmpty() ? null : cpVille);
        append(sb, property.getCountry());
        return sb.length() == 0 ? "—" : sb.toString();
    }

    /**
     * Adresse postale d'un utilisateur (bailleur/locataire), null-safe.
     * Renvoie null si l'utilisateur n'a renseigné aucune adresse (l'appelant peut alors
     * masquer le bloc plutôt qu'afficher un tiret).
     */
    public static String adressePostale(User user) {
        if (user == null) return null;
        StringBuilder sb = new StringBuilder();
        append(sb, user.getAddressLine1());
        append(sb, user.getAddressLine2());
        String cpVille = (safe(user.getPostalCode()) + " " + safe(user.getCity())).trim();
        append(sb, cpVille.isEmpty() ? null : cpVille);
        append(sb, user.getCountry());
        return sb.length() == 0 ? null : sb.toString();
    }

    /**
     * Suffixe HTML « &lt;br/&gt;adresse » (grisé) si l'utilisateur a une adresse, sinon
     * chaîne vide — pour insérer proprement l'adresse sous le nom d'une partie.
     */
    public static String blocAdresse(User user) {
        String addr = adressePostale(user);
        return addr != null ? "<br/><span style=\"color:#555;\">" + addr + "</span>" : "";
    }

    private static void append(StringBuilder sb, String part) {
        String p = safe(part);
        if (p.isEmpty()) return;
        if (sb.length() > 0) sb.append(", ");
        sb.append(p);
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
