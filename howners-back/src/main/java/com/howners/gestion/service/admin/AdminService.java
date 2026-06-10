package com.howners.gestion.service.admin;

import com.howners.gestion.domain.subscription.SubscriptionStatus;
import com.howners.gestion.domain.subscription.UserSubscription;
import com.howners.gestion.domain.user.Role;
import com.howners.gestion.dto.admin.AdminStatsResponse;
import com.howners.gestion.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final RentalRepository rentalRepository;
    private final ContractRepository contractRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;

    public AdminStatsResponse getStats() {
        long totalUsers = userRepository.count();
        long totalOwners = userRepository.findByRole(Role.OWNER).size();
        long totalTenants = userRepository.findByRole(Role.TENANT).size();
        long totalProperties = propertyRepository.count();
        long totalRentals = rentalRepository.count();
        long totalContracts = contractRepository.count();

        // Nouveaux utilisateurs ce mois-ci
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime monthStart = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = currentMonth.plusMonths(1).atDay(1).atStartOfDay();
        long newUsersThisMonth = userRepository.findAll().stream()
                .filter(u -> u.getCreatedAt() != null
                        && !u.getCreatedAt().isBefore(monthStart)
                        && u.getCreatedAt().isBefore(monthEnd))
                .count();

        // Abonnements actifs par plan
        List<UserSubscription> activeSubscriptions = userSubscriptionRepository.findAll().stream()
                .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
                .toList();

        Map<String, Long> subscriptionsByPlan = activeSubscriptions.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getPlan().getName().name(),
                        LinkedHashMap::new,
                        Collectors.counting()
                ));

        // MRR — somme des prix mensuels des abonnements actifs
        BigDecimal mrr = activeSubscriptions.stream()
                .map(s -> s.getPlan().getMonthlyPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new AdminStatsResponse(
                totalUsers,
                totalOwners,
                totalTenants,
                totalProperties,
                totalRentals,
                totalContracts,
                newUsersThisMonth,
                subscriptionsByPlan,
                mrr
        );
    }
}
