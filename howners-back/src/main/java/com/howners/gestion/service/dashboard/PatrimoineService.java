package com.howners.gestion.service.dashboard;

import com.howners.gestion.domain.expense.Expense;
import com.howners.gestion.domain.property.Property;
import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.domain.rental.RentalStatus;
import com.howners.gestion.dto.analytics.PatrimoineResponse;
import com.howners.gestion.exception.ForbiddenException;
import com.howners.gestion.repository.ExpenseRepository;
import com.howners.gestion.repository.PaymentRepository;
import com.howners.gestion.repository.PropertyRepository;
import com.howners.gestion.repository.RentalRepository;
import com.howners.gestion.service.auth.AuthService;
import com.howners.gestion.service.subscription.FeatureGateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Indicateurs patrimoniaux sur 12 mois glissants :
 * - revenus = loyers (type RENT) encaissés ;
 * - charges = taxes/assurance/copropriété du bien (annualisées) + dépenses enregistrées ;
 * - rentabilité brute/nette rapportée au prix d'achat (null si non renseigné) ;
 * - taux d'occupation = jours couverts par un bail ACTIF ou TERMINÉ sur les 365 derniers jours.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PatrimoineService {

    private final PropertyRepository propertyRepository;
    private final PaymentRepository paymentRepository;
    private final ExpenseRepository expenseRepository;
    private final RentalRepository rentalRepository;
    private final FeatureGateService featureGateService;

    @Transactional(readOnly = true)
    public PatrimoineResponse getPatrimoine() {
        UUID ownerId = AuthService.getCurrentUserId();
        if (!featureGateService.hasFeature(ownerId, "advanced_dashboard")) {
            throw new ForbiddenException(
                    "Le dashboard patrimonial est disponible à partir du plan Pro. Passez au plan supérieur pour l'activer.");
        }

        LocalDate now = LocalDate.now();
        LocalDate ilYaUnAn = now.minusDays(365);

        List<PatrimoineResponse.BienPatrimoine> biens = new ArrayList<>();
        BigDecimal valeurTotale = BigDecimal.ZERO;
        BigDecimal revenusTotaux = BigDecimal.ZERO;
        BigDecimal chargesTotales = BigDecimal.ZERO;

        for (Property property : propertyRepository.findByOwnerId(ownerId)) {
            BigDecimal revenus = paymentRepository.sumPaidRentByPropertyAndPeriod(
                    property.getId(), ilYaUnAn.atStartOfDay(), now.plusDays(1).atStartOfDay());

            BigDecimal chargesFixes = nz(property.getPropertyTax())
                    .add(nz(property.getBusinessTax()))
                    .add(nz(property.getHomeInsurance()))
                    .add(nz(property.getCondoFees()).multiply(BigDecimal.valueOf(12)));

            BigDecimal depenses = expenseRepository.findByPropertyId(property.getId()).stream()
                    .filter(e -> e.getExpenseDate() != null && !e.getExpenseDate().isBefore(ilYaUnAn))
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal charges = chargesFixes.add(depenses);
            BigDecimal cashFlowMensuel = revenus.subtract(charges)
                    .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);

            BigDecimal rentabiliteBrute = null;
            BigDecimal rentabiliteNette = null;
            if (property.getPurchasePrice() != null && property.getPurchasePrice().signum() > 0) {
                rentabiliteBrute = revenus.multiply(BigDecimal.valueOf(100))
                        .divide(property.getPurchasePrice(), 2, RoundingMode.HALF_UP);
                rentabiliteNette = revenus.subtract(charges).multiply(BigDecimal.valueOf(100))
                        .divide(property.getPurchasePrice(), 2, RoundingMode.HALF_UP);
            }

            BigDecimal occupation = tauxOccupation(property.getId(), ilYaUnAn, now);

            biens.add(new PatrimoineResponse.BienPatrimoine(
                    property.getId(),
                    property.getName(),
                    property.getCity(),
                    property.getPurchasePrice(),
                    revenus,
                    charges,
                    cashFlowMensuel,
                    rentabiliteBrute,
                    rentabiliteNette,
                    occupation
            ));

            valeurTotale = valeurTotale.add(nz(property.getPurchasePrice()));
            revenusTotaux = revenusTotaux.add(revenus);
            chargesTotales = chargesTotales.add(charges);
        }

        BigDecimal cashFlowTotal = revenusTotaux.subtract(chargesTotales)
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);

        BigDecimal rendementMoyen = null;
        if (valeurTotale.signum() > 0) {
            rendementMoyen = revenusTotaux.subtract(chargesTotales)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(valeurTotale, 2, RoundingMode.HALF_UP);
        }

        return new PatrimoineResponse(
                biens, valeurTotale, revenusTotaux, chargesTotales, cashFlowTotal, rendementMoyen);
    }

    private BigDecimal tauxOccupation(UUID propertyId, LocalDate debut, LocalDate fin) {
        long totalJours = ChronoUnit.DAYS.between(debut, fin);
        if (totalJours <= 0) return BigDecimal.ZERO;

        // Jours couverts par au moins un bail (périodes clippées, fusion grossière par max)
        boolean[] couverts = new boolean[(int) totalJours];
        for (Rental rental : rentalRepository.findByPropertyId(propertyId)) {
            if (rental.getStatus() != RentalStatus.ACTIVE && rental.getStatus() != RentalStatus.TERMINATED) {
                continue;
            }
            LocalDate start = rental.getStartDate() != null && rental.getStartDate().isAfter(debut)
                    ? rental.getStartDate() : debut;
            LocalDate end = rental.getEndDate() != null && rental.getEndDate().isBefore(fin)
                    ? rental.getEndDate() : fin;
            if (start.isAfter(end)) continue;
            int from = (int) ChronoUnit.DAYS.between(debut, start);
            int to = (int) Math.min(ChronoUnit.DAYS.between(debut, end), totalJours - 1);
            for (int i = Math.max(from, 0); i <= to; i++) {
                couverts[i] = true;
            }
        }
        long joursOccupes = 0;
        for (boolean c : couverts) if (c) joursOccupes++;

        return BigDecimal.valueOf(joursOccupes * 100L)
                .divide(BigDecimal.valueOf(totalJours), 1, RoundingMode.HALF_UP);
    }

    private static BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
