package com.howners.gestion.service.dashboard;

import com.howners.gestion.domain.contract.Contract;
import com.howners.gestion.domain.contract.ContractStatus;
import com.howners.gestion.domain.property.Property;
import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.domain.rental.RentalStatus;
import com.howners.gestion.domain.user.Role;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.response.DashboardStatsResponse;
import com.howners.gestion.dto.response.PropertyResponse;
import com.howners.gestion.dto.response.RentalResponse;
import com.howners.gestion.exception.ResourceNotFoundException;
import com.howners.gestion.repository.ContractRepository;
import com.howners.gestion.repository.PropertyRepository;
import com.howners.gestion.repository.RentalRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.service.auth.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final PropertyRepository propertyRepository;
    private final RentalRepository rentalRepository;
    private final ContractRepository contractRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public DashboardStatsResponse getStats() {
        UUID currentUserId = AuthService.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", currentUserId));

        log.debug("Getting dashboard stats for user {} with role {}", currentUserId, currentUser.getRole());

        // Si l'utilisateur est un locataire, retourner des stats spécifiques
        if (currentUser.getRole() == Role.TENANT) {
            return getTenantStats(currentUserId);
        }

        // Sinon, retourner les stats propriétaire/admin
        return getOwnerStats(currentUserId);
    }

    /**
     * Statistiques pour les propriétaires et admins
     */
    private DashboardStatsResponse getOwnerStats(UUID currentUserId) {
        // Récupérer toutes les propriétés de l'utilisateur
        List<Property> properties = propertyRepository.findByOwnerId(currentUserId);
        long totalProperties = properties.size();

        // Récupérer toutes les locations de l'utilisateur
        List<Rental> rentals = rentalRepository.findByOwnerId(currentUserId);

        // Compter les locations par statut
        long activeRentals = rentals.stream()
                .filter(r -> r.getStatus() == RentalStatus.ACTIVE)
                .count();

        long pendingRentals = rentals.stream()
                .filter(r -> r.getStatus() == RentalStatus.PENDING)
                .count();

        long terminatedRentals = rentals.stream()
                .filter(r -> r.getStatus() == RentalStatus.TERMINATED)
                .count();

        // Calculer les revenus mensuels (somme des loyers actifs)
        BigDecimal monthlyRevenue = rentals.stream()
                .filter(r -> r.getStatus() == RentalStatus.ACTIVE)
                .map(Rental::getMonthlyRent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Récupérer la dernière propriété créée
        PropertyResponse latestProperty = properties.stream()
                .max(Comparator.comparing(Property::getCreatedAt))
                .map(PropertyResponse::from)
                .orElse(null);

        // Récupérer la dernière location créée
        RentalResponse latestRental = rentals.stream()
                .max(Comparator.comparing(Rental::getCreatedAt))
                .map(RentalResponse::from)
                .orElse(null);

        DashboardStatsResponse.RecentActivity recentActivity = new DashboardStatsResponse.RecentActivity(
                latestProperty,
                latestRental
        );

        return new DashboardStatsResponse(
                totalProperties,
                activeRentals,
                pendingRentals,
                terminatedRentals,
                monthlyRevenue,
                "EUR",
                recentActivity
        );
    }

    /**
     * Statistiques pour les locataires
     */
    private DashboardStatsResponse getTenantStats(UUID tenantId) {
        // Récupérer les locations du locataire
        List<Rental> rentals = rentalRepository.findByTenantId(tenantId);

        // Compter les locations par statut
        long activeRentals = rentals.stream()
                .filter(r -> r.getStatus() == RentalStatus.ACTIVE)
                .count();

        long pendingRentals = rentals.stream()
                .filter(r -> r.getStatus() == RentalStatus.PENDING)
                .count();

        long terminatedRentals = rentals.stream()
                .filter(r -> r.getStatus() == RentalStatus.TERMINATED)
                .count();

        // Calculer le loyer mensuel total
        BigDecimal monthlyRent = rentals.stream()
                .filter(r -> r.getStatus() == RentalStatus.ACTIVE)
                .map(Rental::getMonthlyRent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Pour un locataire, totalProperties = 0 (ils ne possèdent pas de propriétés)
        long totalProperties = 0L;

        // Récupérer la location la plus récente
        RentalResponse latestRental = rentals.stream()
                .max(Comparator.comparing(Rental::getCreatedAt))
                .map(RentalResponse::from)
                .orElse(null);

        DashboardStatsResponse.RecentActivity recentActivity = new DashboardStatsResponse.RecentActivity(
                null, // Pas de propriété pour un locataire
                latestRental
        );

        return new DashboardStatsResponse(
                totalProperties,
                activeRentals,
                pendingRentals,
                terminatedRentals,
                monthlyRent,
                "EUR",
                recentActivity
        );
    }
}
