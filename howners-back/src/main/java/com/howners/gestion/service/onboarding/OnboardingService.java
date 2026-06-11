package com.howners.gestion.service.onboarding;

import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.onboarding.OnboardingStatusResponse;
import com.howners.gestion.dto.onboarding.OnboardingStepResponse;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service de calcul du statut d'onboarding pour les proprietaires.
 * Evalue la progression de l'utilisateur a travers les etapes cles :
 * profil, premier bien, premier locataire, premier contrat.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OnboardingService {

    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final RentalRepository rentalRepository;
    private final ContractRepository contractRepository;

    /**
     * Retourne le statut d'onboarding de l'utilisateur courant.
     */
    @Transactional(readOnly = true)
    public OnboardingStatusResponse getStatus() {
        UUID currentUserId = AuthService.getCurrentUserId();
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", currentUserId));

        log.debug("Calcul du statut d'onboarding pour l'utilisateur {}", currentUserId);

        List<OnboardingStepResponse> steps = new ArrayList<>();

        // Etape 1 : Profil complet (prenom, nom, telephone)
        boolean profileComplete = isProfileComplete(user);
        steps.add(OnboardingStepResponse.builder()
                .key("profile_complete")
                .label("Compléter votre profil")
                .done(profileComplete)
                .link("/profile")
                .build());

        // Etape 2 : Premier bien ajoute
        boolean hasProperty = !propertyRepository.findByOwnerId(currentUserId).isEmpty();
        steps.add(OnboardingStepResponse.builder()
                .key("first_property")
                .label("Ajouter votre premier bien")
                .done(hasProperty)
                .link("/properties/new")
                .build());

        // Etape 3 : Premier locataire (location creee)
        boolean hasTenant = !rentalRepository.findByOwnerId(currentUserId).isEmpty();
        steps.add(OnboardingStepResponse.builder()
                .key("first_tenant")
                .label("Inviter votre premier locataire")
                .done(hasTenant)
                .link("/rentals")
                .build());

        // Etape 4 : Premier contrat cree
        boolean hasContract = !contractRepository.findByOwnerId(currentUserId).isEmpty();
        steps.add(OnboardingStepResponse.builder()
                .key("first_contract")
                .label("Créer votre premier contrat")
                .done(hasContract)
                .link("/contracts/new")
                .build());

        // Calcul du pourcentage de completion
        long doneCount = steps.stream().filter(OnboardingStepResponse::isDone).count();
        int completionPercent = (int) Math.round((double) doneCount / steps.size() * 100);
        boolean allCompleted = doneCount == steps.size();

        return OnboardingStatusResponse.builder()
                .completed(allCompleted)
                .steps(steps)
                .completionPercent(completionPercent)
                .build();
    }

    /**
     * Verifie si le profil utilisateur est complet (prenom, nom et telephone renseignes).
     */
    private boolean isProfileComplete(User user) {
        return user.getFirstName() != null && !user.getFirstName().isBlank()
                && user.getLastName() != null && !user.getLastName().isBlank()
                && user.getPhone() != null && !user.getPhone().isBlank();
    }
}
