package com.howners.gestion.service.contract;

import com.howners.gestion.domain.property.Property;
import com.howners.gestion.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Garde-fous liés à la performance énergétique (DPE), issus de l'audit de conformité des baux
 * (AUDIT-BAUX-CONFORMITE.md, items T-03 et T-05).
 *
 * <ul>
 *   <li><b>T-03</b> — le DPE est une annexe obligatoire et opposable : un bail ne peut partir en
 *       signature sans DPE valide sur le bien.</li>
 *   <li><b>T-05</b> — loi Climat et résilience : les logements classés G ne peuvent plus être mis
 *       en location (interdiction depuis le 1er janvier 2025) ; le loyer des passoires thermiques
 *       (F et G) ne peut pas être révisé à la hausse (gel depuis août 2022).</li>
 * </ul>
 *
 * Calendrier d'interdiction de louer (décence énergétique) appliqué selon l'année courante :
 * G dès 2025, F dès 2028, E dès 2034.
 */
@Service
@Slf4j
public class DpeComplianceService {

    private static final int DPE_VALIDITE_ANNEES = 10;

    /**
     * Vérifie qu'un bien peut être mis en location (envoi d'un bail en signature) : DPE présent,
     * non périmé, et logement énergétiquement décent au regard du calendrier en vigueur.
     * Lève une {@link BadRequestException} explicite sinon.
     */
    public void assertCanRentOut(Property property) {
        String dpe = normalize(property);

        // T-03 : DPE manquant.
        if (dpe == null) {
            throw new BadRequestException(
                    "Le diagnostic de performance énergétique (DPE) du bien est manquant. "
                            + "Il est obligatoire et opposable : renseignez l'étiquette DPE du bien avant d'envoyer le bail.");
        }

        // T-03 : DPE périmé (validité 10 ans).
        LocalDate dpeDate = property.getDpeDate();
        if (dpeDate != null && dpeDate.plusYears(DPE_VALIDITE_ANNEES).isBefore(LocalDate.now())) {
            throw new BadRequestException(
                    "Le DPE du bien a plus de " + DPE_VALIDITE_ANNEES + " ans (réalisé le " + dpeDate
                            + ") et n'est plus valide. Faites établir un nouveau DPE avant d'envoyer le bail.");
        }

        // T-05 : interdiction de louer les passoires selon le calendrier de la loi Climat.
        int year = LocalDate.now().getYear();
        String interdit = classeInterditeALaLocation(year);
        if (dpe.compareTo(interdit) >= 0) {
            throw new BadRequestException(
                    "Le logement est classé " + dpe + " au DPE. Depuis " + anneeInterdiction(dpe)
                            + ", un logement classé " + dpe + " est considéré comme énergétiquement indécent "
                            + "et ne peut pas être mis en location (loi Climat et résilience). "
                            + "Des travaux de rénovation sont nécessaires avant la mise en location.");
        }
    }

    /**
     * Vérifie qu'une révision de loyer est permise : elle est gelée pour les passoires thermiques
     * (classes F et G). Lève une {@link BadRequestException} sinon.
     */
    public void assertCanReviseRent(Property property) {
        String dpe = normalize(property);
        if (dpe != null && (dpe.equals("F") || dpe.equals("G"))) {
            throw new BadRequestException(
                    "La révision du loyer est impossible : le logement est classé " + dpe
                            + " au DPE. Le loyer des passoires thermiques (F et G) est gelé depuis le "
                            + "24 août 2022 (loi Climat et résilience) tant que le bien n'est pas rénové.");
        }
    }

    /** Étiquette DPE normalisée (A–G majuscule) ou null si absente/invalide. */
    private String normalize(Property property) {
        if (property == null || property.getDpeRating() == null) {
            return null;
        }
        String r = property.getDpeRating().trim().toUpperCase();
        return r.length() == 1 && r.compareTo("A") >= 0 && r.compareTo("G") <= 0 ? r : null;
    }

    /** Classe (incluse) interdite à la location pour l'année donnée : G→2025, F→2028, E→2034. */
    private String classeInterditeALaLocation(int year) {
        if (year >= 2034) return "E";
        if (year >= 2028) return "F";
        return "G"; // depuis 2025
    }

    private int anneeInterdiction(String classe) {
        return switch (classe) {
            case "E" -> 2034;
            case "F" -> 2028;
            default -> 2025; // G
        };
    }
}
