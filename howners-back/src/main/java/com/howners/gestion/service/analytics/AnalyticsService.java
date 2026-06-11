package com.howners.gestion.service.analytics;

import com.howners.gestion.domain.property.Property;
import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.domain.rental.RentalStatus;
import com.howners.gestion.dto.analytics.AnalyticsSummaryResponse;
import com.howners.gestion.repository.PaymentRepository;
import com.howners.gestion.repository.PropertyRepository;
import com.howners.gestion.repository.RentalRepository;
import com.howners.gestion.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final PaymentRepository paymentRepository;
    private final PropertyRepository propertyRepository;
    private final RentalRepository rentalRepository;

    public AnalyticsSummaryResponse getSummary() {
        UUID ownerId = getCurrentUserId();

        List<Property> properties = propertyRepository.findByOwnerId(ownerId);
        List<Rental> rentals = rentalRepository.findByOwnerId(ownerId);

        long totalProperties = properties.size();
        long activeRentals = rentals.stream()
                .filter(r -> r.getStatus() == RentalStatus.ACTIVE)
                .count();

        // Taux d'occupation
        double occupancyRate = totalProperties > 0
                ? (double) activeRentals / totalProperties * 100.0
                : 0.0;
        occupancyRate = Math.round(occupancyRate * 10.0) / 10.0;

        // Revenus totaux (tous les paiements reçus)
        BigDecimal totalRevenue = paymentRepository.sumPaidAmountByOwnerAndPeriod(
                ownerId,
                LocalDateTime.of(2000, 1, 1, 0, 0),
                LocalDateTime.now()
        );

        // Revenus mensuels des 6 derniers mois
        List<AnalyticsSummaryResponse.MonthlyRevenue> monthlyRevenue = computeMonthlyRevenue(ownerId);

        // Biens vacants (sans location active)
        Set<UUID> rentedPropertyIds = rentals.stream()
                .filter(r -> r.getStatus() == RentalStatus.ACTIVE)
                .map(r -> r.getProperty().getId())
                .collect(Collectors.toSet());

        List<String> vacantProperties = properties.stream()
                .filter(p -> !rentedPropertyIds.contains(p.getId()))
                .map(Property::getName)
                .toList();

        return new AnalyticsSummaryResponse(
                totalRevenue,
                occupancyRate,
                totalProperties,
                activeRentals,
                monthlyRevenue,
                vacantProperties
        );
    }

    private List<AnalyticsSummaryResponse.MonthlyRevenue> computeMonthlyRevenue(UUID ownerId) {
        List<AnalyticsSummaryResponse.MonthlyRevenue> result = new ArrayList<>();
        YearMonth current = YearMonth.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

        for (int i = 5; i >= 0; i--) {
            YearMonth month = current.minusMonths(i);
            LocalDateTime from = month.atDay(1).atStartOfDay();
            LocalDateTime to = month.plusMonths(1).atDay(1).atStartOfDay();
            BigDecimal amount = paymentRepository.sumPaidAmountByOwnerAndPeriod(ownerId, from, to);
            result.add(new AnalyticsSummaryResponse.MonthlyRevenue(
                    month.format(formatter),
                    amount
            ));
        }

        return result;
    }

    private UUID getCurrentUserId() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return principal.getId();
    }
}
