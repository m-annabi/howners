package com.howners.gestion.service.rating;

import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.rating.TenantScoreResponse;
import com.howners.gestion.dto.rating.TenantScoreResponse.PaymentStats;
import com.howners.gestion.dto.rating.TenantScoreResponse.RiskLevel;
import com.howners.gestion.dto.rating.TenantScoreResponse.ScoreBreakdown;
import com.howners.gestion.exception.ResourceNotFoundException;
import com.howners.gestion.repository.PaymentRepository;
import com.howners.gestion.repository.RentalRepository;
import com.howners.gestion.repository.TenantRatingRepository;
import com.howners.gestion.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantScoringService {

    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final TenantRatingRepository ratingRepository;
    private final RentalRepository rentalRepository;

    private static final double PAYMENT_WEIGHT = 0.40;
    private static final double RATING_WEIGHT = 0.40;
    private static final double LEASE_DURATION_WEIGHT = 0.10;
    private static final double COMMUNICATION_WEIGHT = 0.10;

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public TenantScoreResponse calculateScore(UUID tenantId) {
        User tenant = userRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        // 1. Payment score (40%) - based on on-time payment percentage
        long totalPayments = paymentRepository.countByTenantId(tenantId);
        long onTimePayments = paymentRepository.countOnTimePaymentsByTenantId(tenantId);
        long latePayments = paymentRepository.countLatePaymentsByTenantId(tenantId);

        double paymentScore;
        double onTimePercentage;
        if (totalPayments == 0) {
            paymentScore = 50.0; // Neutral score if no payment history
            onTimePercentage = 0;
        } else {
            onTimePercentage = (double) onTimePayments / totalPayments * 100;
            paymentScore = onTimePercentage;
        }

        // 2. Rating score (40%) - average of all ratings (1-5 scale -> 0-100)
        double ratingScore;
        Object[] rawRatingData = ratingRepository.getAverageRatingsByTenantId(tenantId);
        // JPA may wrap the result row in an outer Object[]
        Object[] ratingData = (rawRatingData != null && rawRatingData.length == 1 && rawRatingData[0] instanceof Object[])
                ? (Object[]) rawRatingData[0] : rawRatingData;
        if (ratingData != null && ratingData[0] != null) {
            Double avgPayment = (Double) ratingData[0];
            Double avgPropertyRespect = (Double) ratingData[1];
            Double avgCommunication = (Double) ratingData[2];
            Double avgOverall = (Double) ratingData[3];
            ratingScore = (avgOverall / 5.0) * 100;
        } else {
            ratingScore = 50.0; // Neutral if no ratings
        }

        // 3. Lease duration score (10%) - longer leases = higher score
        double leaseDurationScore = calculateLeaseDurationScore(tenantId);

        // 4. Communication score (10%) - from communication_rating in tenant_ratings
        double communicationScore;
        if (ratingData != null && ratingData[2] != null) {
            Double avgCommunication = (Double) ratingData[2];
            communicationScore = (avgCommunication / 5.0) * 100;
        } else {
            communicationScore = 50.0;
        }

        // Calculate final weighted score
        double weightedScore = (paymentScore * PAYMENT_WEIGHT)
                + (ratingScore * RATING_WEIGHT)
                + (leaseDurationScore * LEASE_DURATION_WEIGHT)
                + (communicationScore * COMMUNICATION_WEIGHT);

        int finalScore = (int) Math.round(Math.min(100, Math.max(0, weightedScore)));

        RiskLevel riskLevel;
        if (finalScore >= 70) {
            riskLevel = RiskLevel.LOW;
        } else if (finalScore >= 40) {
            riskLevel = RiskLevel.MEDIUM;
        } else {
            riskLevel = RiskLevel.HIGH;
        }

        ScoreBreakdown breakdown = new ScoreBreakdown(
                Math.round(paymentScore * 10) / 10.0,
                Math.round(ratingScore * 10) / 10.0,
                Math.round(leaseDurationScore * 10) / 10.0,
                Math.round(communicationScore * 10) / 10.0
        );

        PaymentStats paymentStats = new PaymentStats(
                totalPayments,
                onTimePayments,
                latePayments,
                Math.round(onTimePercentage * 10) / 10.0
        );

        log.info("Tenant {} score calculated: {} (risk: {})", tenantId, finalScore, riskLevel);

        return new TenantScoreResponse(
                tenantId,
                tenant.getFullName(),
                finalScore,
                riskLevel,
                breakdown,
                paymentStats
        );
    }

    @Transactional(readOnly = true)
    public List<TenantScoreResponse> compareScores(List<UUID> tenantIds) {
        return tenantIds.stream()
                .map(this::calculateScore)
                .toList();
    }

    private double calculateLeaseDurationScore(UUID tenantId) {
        List<Rental> rentals = rentalRepository.findByTenantId(tenantId);
        if (rentals.isEmpty()) {
            return 50.0;
        }

        long totalDays = 0;
        for (Rental rental : rentals) {
            LocalDate start = rental.getStartDate();
            LocalDate end = rental.getEndDate() != null ? rental.getEndDate() : LocalDate.now();
            totalDays += ChronoUnit.DAYS.between(start, end);
        }

        long totalMonths = totalDays / 30;

        // Score: 0 months = 0, 6 months = 50, 12+ months = 100
        if (totalMonths >= 12) return 100.0;
        if (totalMonths >= 6) return 50.0 + ((totalMonths - 6) * 50.0 / 6.0);
        return totalMonths * 50.0 / 6.0;
    }
}
