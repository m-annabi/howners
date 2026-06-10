package com.howners.gestion.dto.onboarding;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represente une etape individuelle du parcours d'onboarding.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingStepResponse {

    private String key;
    private String label;
    private boolean done;
    private String link;
}
