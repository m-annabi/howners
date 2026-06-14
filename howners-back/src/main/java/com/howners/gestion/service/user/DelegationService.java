package com.howners.gestion.service.user;

import com.howners.gestion.domain.notification.NotificationType;
import com.howners.gestion.domain.rental.RentalStatus;
import com.howners.gestion.domain.user.CompteDelegation;
import com.howners.gestion.domain.user.StatutDelegation;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.user.ApercuCompteResponse;
import com.howners.gestion.dto.user.CreateDelegationRequest;
import com.howners.gestion.dto.user.DelegationResponse;
import com.howners.gestion.exception.BadRequestException;
import com.howners.gestion.exception.ForbiddenException;
import com.howners.gestion.exception.ResourceNotFoundException;
import com.howners.gestion.repository.CompteDelegationRepository;
import com.howners.gestion.repository.PropertyRepository;
import com.howners.gestion.repository.RentalRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.service.auth.AuthService;
import com.howners.gestion.service.notification.NotificationService;
import com.howners.gestion.service.payment.FinancialDashboardService;
import com.howners.gestion.service.subscription.FeatureGateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DelegationService {

    private final CompteDelegationRepository delegationRepository;
    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final RentalRepository rentalRepository;
    private final FeatureGateService featureGateService;
    private final FinancialDashboardService financialDashboardService;
    private final NotificationService notificationService;

    @Transactional
    public DelegationResponse inviterParEmail(CreateDelegationRequest request) {
        UUID agenceUserId = AuthService.getCurrentUserId();

        if (!featureGateService.hasFeature(agenceUserId, "multi_account")) {
            throw new ForbiddenException(
                    "La délégation de compte est réservée au plan Agence. Passez au plan Agence pour inviter des collaborateurs.");
        }

        User agence = userRepository.findById(agenceUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", agenceUserId.toString()));

        User delegue = userRepository.findByEmail(request.email().toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucun utilisateur avec cet email. Le collaborateur doit d'abord créer un compte Howners."));

        if (delegue.getId().equals(agenceUserId)) {
            throw new BadRequestException("Vous ne pouvez pas vous déléguer votre propre compte.");
        }

        CompteDelegation delegation = delegationRepository
                .findByAgenceIdAndDelegueId(agenceUserId, delegue.getId())
                .map(existing -> {
                    if (existing.getStatut() == StatutDelegation.ACTIVE) {
                        throw new BadRequestException("Une délégation active existe déjà pour cet utilisateur.");
                    }
                    existing.setStatut(StatutDelegation.ACTIVE);
                    existing.setUpdatedAt(LocalDateTime.now());
                    return existing;
                })
                .orElseGet(() -> CompteDelegation.builder()
                        .agence(agence)
                        .delegue(delegue)
                        .statut(StatutDelegation.ACTIVE)
                        .build());

        delegation = delegationRepository.save(delegation);

        notificationService.create(
                delegue.getId(),
                NotificationType.SYSTEM,
                "Accès délégué accordé",
                agence.getFullName() + " vous a donné un accès en lecture seule à son compte.",
                "/delegations");

        log.info("Délégation créée : agence {} -> délégué {}", agenceUserId, delegue.getId());
        return DelegationResponse.from(delegation);
    }

    @Transactional
    public void revoquer(UUID delegationId) {
        UUID agenceUserId = AuthService.getCurrentUserId();
        CompteDelegation delegation = delegationRepository.findById(delegationId)
                .orElseThrow(() -> new ResourceNotFoundException("Delegation", "id", delegationId.toString()));

        if (!delegation.getAgence().getId().equals(agenceUserId)) {
            throw new ForbiddenException("Vous ne pouvez révoquer que vos propres délégations.");
        }

        delegation.setStatut(StatutDelegation.REVOQUEE);
        delegation.setUpdatedAt(LocalDateTime.now());
        delegationRepository.save(delegation);
        log.info("Délégation {} révoquée", delegationId);
    }

    @Transactional(readOnly = true)
    public List<DelegationResponse> mesDelegations() {
        UUID agenceUserId = AuthService.getCurrentUserId();
        return delegationRepository.findByAgenceIdOrderByCreatedAtDesc(agenceUserId).stream()
                .map(DelegationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DelegationResponse> delegationsRecues() {
        UUID delegueUserId = AuthService.getCurrentUserId();
        return delegationRepository
                .findByDelegueIdAndStatutOrderByCreatedAtDesc(delegueUserId, StatutDelegation.ACTIVE).stream()
                .map(DelegationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ApercuCompteResponse getApercuCompte(UUID delegationId) {
        UUID currentUserId = AuthService.getCurrentUserId();
        CompteDelegation delegation = delegationRepository.findById(delegationId)
                .orElseThrow(() -> new ResourceNotFoundException("Delegation", "id", delegationId.toString()));

        if (!delegation.getDelegue().getId().equals(currentUserId)) {
            throw new ForbiddenException("Cette délégation ne vous est pas destinée.");
        }
        if (delegation.getStatut() != StatutDelegation.ACTIVE) {
            throw new ForbiddenException("Cette délégation a été révoquée.");
        }

        UUID agenceUserId = delegation.getAgence().getId();
        long totalProperties = propertyRepository.findByOwnerId(agenceUserId).size();
        long activeRentals = rentalRepository.findByOwnerId(agenceUserId).stream()
                .filter(r -> r.getStatus() == RentalStatus.ACTIVE)
                .count();

        return new ApercuCompteResponse(
                agenceUserId,
                delegation.getAgence().getFullName(),
                delegation.getAgence().getEmail(),
                totalProperties,
                activeRentals,
                financialDashboardService.getFinancialDashboard(agenceUserId)
        );
    }
}
