package com.howners.gestion.controller;

import com.howners.gestion.dto.referral.ApplyReferralRequest;
import com.howners.gestion.dto.referral.ReferralCodeResponse;
import com.howners.gestion.dto.referral.ReferralStatsResponse;
import com.howners.gestion.dto.referral.ReferralSummary;
import com.howners.gestion.service.referral.ReferralService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/my-code")
    public ResponseEntity<ReferralCodeResponse> getMyCode() {
        return ResponseEntity.ok(referralService.getMyCode());
    }

    @GetMapping("/stats")
    public ResponseEntity<ReferralStatsResponse> getStats() {
        return ResponseEntity.ok(referralService.getReferralStats());
    }

    @PostMapping("/apply")
    public ResponseEntity<Void> applyReferralCode(@Valid @RequestBody ApplyReferralRequest request) {
        referralService.applyReferralCode(request.code());
        return ResponseEntity.ok().build();
    }
}
