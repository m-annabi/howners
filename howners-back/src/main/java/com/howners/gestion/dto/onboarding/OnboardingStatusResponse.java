package com.howners.gestion.dto.onboarding;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Reponse globale du statut d'onboarding pour un proprietaire.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingStatusResponse {

    private boolean completed;
    private List<OnboardingStepResponse> steps;
    private int completionPercent;
}
