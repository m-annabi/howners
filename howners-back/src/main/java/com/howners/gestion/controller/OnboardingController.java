package com.howners.gestion.controller;

import com.howners.gestion.dto.onboarding.OnboardingStatusResponse;
import com.howners.gestion.service.onboarding.OnboardingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controleur pour le suivi de l'onboarding des proprietaires.
 */
@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    /**
     * Retourne le statut d'onboarding de l'utilisateur courant.
     */
    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<OnboardingStatusResponse> getStatus() {
        return ResponseEntity.ok(onboardingService.getStatus());
    }
}
