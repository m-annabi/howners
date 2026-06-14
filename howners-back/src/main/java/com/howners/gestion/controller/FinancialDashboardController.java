package com.howners.gestion.controller;

import com.howners.gestion.dto.analytics.PatrimoineResponse;
import com.howners.gestion.dto.response.FinancialDashboardResponse;
import com.howners.gestion.service.dashboard.PatrimoineService;
import com.howners.gestion.service.payment.FinancialDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/financial")
@RequiredArgsConstructor
public class FinancialDashboardController {

    private final FinancialDashboardService financialDashboardService;
    private final PatrimoineService patrimoineService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<FinancialDashboardResponse> getFinancialDashboard() {
        return ResponseEntity.ok(financialDashboardService.getFinancialDashboard());
    }

    @GetMapping("/patrimoine")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<PatrimoineResponse> getPatrimoine() {
        return ResponseEntity.ok(patrimoineService.getPatrimoine());
    }
}
