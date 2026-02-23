package com.howners.gestion.service.dashboard;

import com.howners.gestion.domain.application.ApplicationStatus;
import com.howners.gestion.domain.property.Property;
import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.domain.rental.RentalStatus;
import com.howners.gestion.domain.search.InvitationStatus;
import com.howners.gestion.domain.search.TenantSearchProfile;
import com.howners.gestion.domain.user.Role;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.response.DashboardStatsResponse;
import com.howners.gestion.dto.response.PropertyResponse;
import com.howners.gestion.dto.response.RentalResponse;
import com.howners.gestion.exception.ResourceNotFoundException;
import com.howners.gestion.repository.*;
import com.howners.gestion.service.auth.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final PropertyRepository propertyRepository;
    private final RentalRepository rentalRepository;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final TenantInvitationRepository tenantInvitationRepository;
    private final TenantSearchProfileRepository tenantSearchProfileRepository;
    private final MessageRepository messageRepository;

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

        return DashboardStatsResponse.forOwner(
                totalProperties, activeRentals, pendingRentals,
                terminatedRentals, monthlyRevenue, "EUR", recentActivity
        );
    }

    /**
     * Statistiques pour les locataires
     */
    private DashboardStatsResponse getTenantStats(UUID tenantId) {
        // Candidatures
        var applications = applicationRepository.findByApplicantIdOrderByCreatedAtDesc(tenantId);
        long totalApplications = applications.size();
        long pendingApplications = applications.stream()
                .filter(a -> a.getStatus() == ApplicationStatus.SUBMITTED || a.getStatus() == ApplicationStatus.UNDER_REVIEW)
                .count();

        // Invitations
        var invitations = tenantInvitationRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
        long totalInvitations = invitations.size();
        long pendingInvitations = invitations.stream()
                .filter(i -> i.getStatus() == InvitationStatus.PENDING)
                .count();

        // Profil de recherche
        boolean searchProfileActive = tenantSearchProfileRepository.findByTenantId(tenantId)
                .map(TenantSearchProfile::getIsActive)
                .orElse(false);

        // Messages non lus
        long unreadMessages = messageRepository.countUnreadByRecipientId(tenantId);

        // Locations du locataire (pour le loyer)
        List<Rental> rentals = rentalRepository.findByTenantId(tenantId);
        long activeRentals = rentals.stream()
                .filter(r -> r.getStatus() == RentalStatus.ACTIVE).count();
        long pendingRentals = rentals.stream()
                .filter(r -> r.getStatus() == RentalStatus.PENDING).count();
        long terminatedRentals = rentals.stream()
                .filter(r -> r.getStatus() == RentalStatus.TERMINATED).count();
        BigDecimal monthlyRent = rentals.stream()
                .filter(r -> r.getStatus() == RentalStatus.ACTIVE)
                .map(Rental::getMonthlyRent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        DashboardStatsResponse.RecentActivity recentActivity = new DashboardStatsResponse.RecentActivity(
                null, null
        );

        DashboardStatsResponse.TenantInfo tenantInfo = new DashboardStatsResponse.TenantInfo(
                totalApplications, pendingApplications,
                pendingInvitations, totalInvitations,
                searchProfileActive, unreadMessages
        );

        return DashboardStatsResponse.forTenant(
                activeRentals, pendingRentals, terminatedRentals,
                monthlyRent, "EUR", recentActivity, tenantInfo
        );
    }
}
