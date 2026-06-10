package com.howners.gestion.controller;

import com.howners.gestion.dto.referral.ReferralSummary;
import com.howners.gestion.service.referral.ReferralService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/referrals")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ReferralController {

    private final ReferralService referralService;

    @GetMapping("/me")
    public ResponseEntity<ReferralSummary> getMySummary() {
        return ResponseEntity.ok(referralService.getMySummary());
    }
}
