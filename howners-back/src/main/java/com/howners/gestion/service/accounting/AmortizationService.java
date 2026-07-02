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
 * prorata temporis (en jours à compter de la date de mise en service) ; la dernière
 * année reçoit le solde résiduel pour que le cumul égale exactement la base.
 */
@Service
public class AmortizationService {

    /** Une ligne du plan d'amortissement d'une immobilisation pour un exercice. */
    public record AmortizationLine(int year, BigDecimal base, BigDecimal annuite,
                                   BigDecimal cumul, BigDecimal vnc) {}

    /** Dotation (annuité) d'une immobilisation pour l'exercice donné. */
    public BigDecimal annuite(AmortizableAsset asset, int year) {
        int firstYear = asset.getStartDate().getYear();
        int lastYear = firstYear + asset.getDurationYears() - 1; // année du solde
        if (year < firstYear || year > lastYear) {
            return BigDecimal.ZERO;
        }
        BigDecimal annuellePleine = asset.getBase()
                .divide(BigDecimal.valueOf(asset.getDurationYears()), 2, RoundingMode.HALF_UP);

        if (year == lastYear) {
            // Solde : base - cumul des années précédentes (évite les arrondis résiduels).
            return asset.getBase().subtract(cumul(asset, year - 1)).max(BigDecimal.ZERO);
        }
        if (year == firstYear) {
            // Prorata temporis en jours sur 360 (convention comptable), capé à l'année pleine.
            long jours = java.time.temporal.ChronoUnit.DAYS.between(
                    asset.getStartDate(), LocalDate.of(firstYear, 12, 31)) + 1;
            BigDecimal prorata = annuellePleine
                    .multiply(BigDecimal.valueOf(jours))
                    .divide(BigDecimal.valueOf(360), 2, RoundingMode.HALF_UP);
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
        int lastYear = firstYear + asset.getDurationYears() - 1;
        for (int y = firstYear; y <= lastYear; y++) {
            lines.add(new AmortizationLine(y, asset.getBase(), annuite(asset, y),
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
