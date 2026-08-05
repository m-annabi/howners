package com.howners.gestion.service.accounting;

import com.howners.gestion.domain.accounting.AmortizableAsset;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Amortissement linéaire des immobilisations. La première année est calculée au
 * prorata temporis en base 30/360 (convention comptable : mois de 30 jours, année de
 * 360) à compter de la date de mise en service. Le plan court donc sur
 * {@code durationYears + 1} années civiles dès que la mise en service n'est pas au
 * 1er janvier : la fraction non amortie la 1re année est reportée sur une année
 * complémentaire finale (et non concentrée sur la dernière année pleine, ce qui
 * produirait une annuité supérieure à l'annuité linéaire). La dernière année reçoit le
 * solde résiduel pour que le cumul égale exactement la base.
 */
@Service
public class AmortizationService {

    private static final BigDecimal JOURS_AN = BigDecimal.valueOf(360);

    /** Une ligne du plan d'amortissement d'une immobilisation pour un exercice. */
    public record AmortizationLine(int year, BigDecimal base, BigDecimal annuite,
                                   BigDecimal cumul, BigDecimal vnc) {}

    /** Dernière année civile amortie (année complémentaire du prorata de 1re année). */
    private static int lastYear(AmortizableAsset asset) {
        return asset.getStartDate().getYear() + asset.getDurationYears();
    }

    /**
     * Fraction d'année amortie la 1re année en base 30/360, en comptant le jour de mise
     * en service. Mise en service au 1er janvier → 1 (année pleine) ; au 1er juillet →
     * 0,5. Bornée à [0, 1].
     */
    private static BigDecimal firstYearFraction(LocalDate start) {
        int mois = start.getMonthValue();
        int jour = Math.min(start.getDayOfMonth(), 30);
        int joursEcoules = 30 * (mois - 1) + (jour - 1); // jours 30/360 avant la mise en service
        int joursRestants = Math.max(0, 360 - joursEcoules);
        return BigDecimal.valueOf(joursRestants).divide(JOURS_AN, 10, RoundingMode.HALF_UP);
    }

    /** Dotation (annuité) d'une immobilisation pour l'exercice donné. */
    public BigDecimal annuite(AmortizableAsset asset, int year) {
        int firstYear = asset.getStartDate().getYear();
        int lastYear = lastYear(asset);
        if (year < firstYear || year > lastYear) {
            return BigDecimal.ZERO;
        }
        BigDecimal annuellePleine = asset.getBase()
                .divide(BigDecimal.valueOf(asset.getDurationYears()), 2, RoundingMode.HALF_UP);

        if (year == lastYear) {
            // Année complémentaire : solde résiduel (fraction de 1re année reportée +
            // reliquats d'arrondi). Nul si la mise en service est au 1er janvier.
            return asset.getBase().subtract(cumul(asset, year - 1)).max(BigDecimal.ZERO);
        }
        if (year == firstYear) {
            // Prorata temporis 30/360, borné à l'annuité pleine.
            BigDecimal prorata = annuellePleine.multiply(firstYearFraction(asset.getStartDate()))
                    .setScale(2, RoundingMode.HALF_UP);
            return prorata.min(annuellePleine);
        }
        return annuellePleine;
    }

    /** Cumul des amortissements jusqu'à l'exercice inclus (plafonné à la base). */
    public BigDecimal cumul(AmortizableAsset asset, int year) {
        int firstYear = asset.getStartDate().getYear();
        BigDecimal total = BigDecimal.ZERO;
        for (int y = firstYear; y <= year; y++) {
            total = total.add(annuite(asset, y));
        }
        return total.min(asset.getBase());
    }

    /** Valeur nette comptable à la fin de l'exercice. */
    public BigDecimal vnc(AmortizableAsset asset, int year) {
        return asset.getBase().subtract(cumul(asset, year)).max(BigDecimal.ZERO);
    }

    /** Plan complet d'une immobilisation (une ligne par exercice amorti). */
    public List<AmortizationLine> plan(AmortizableAsset asset) {
        List<AmortizationLine> lines = new ArrayList<>();
        int firstYear = asset.getStartDate().getYear();
        int lastYear = lastYear(asset);
        for (int y = firstYear; y <= lastYear; y++) {
            BigDecimal annuite = annuite(asset, y);
            // L'année complémentaire est omise du plan si elle est nulle (mise en service au 1er janvier).
            if (y == lastYear && annuite.signum() == 0) break;
            lines.add(new AmortizationLine(y, asset.getBase(), annuite,
                    cumul(asset, y), vnc(asset, y)));
        }
        return lines;
    }

    /** Somme des annuités de toutes les immobilisations pour un exercice. */
    public BigDecimal dotationExercice(List<AmortizableAsset> assets, int year) {
        return assets.stream()
                .map(a -> annuite(a, year))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
