package com.howners.gestion.controller;

import com.howners.gestion.dto.contract.ContractPublicView;
import com.howners.gestion.service.contract.ContractESignatureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller public pour l'accès aux contrats via token (sans authentification)
 */
@RestController
@RequestMapping("/api/public/contracts")
@RequiredArgsConstructor
@Slf4j
public class PublicContractController {

    private final ContractESignatureService esignatureService;

    /**
     * Récupère un contrat par son token d'accès
     *
     * GET /api/public/contracts/token/{token}
     *
     * Accessible sans authentification pour permettre aux locataires de voir leur contrat
     */
    @GetMapping("/token/{token}")
    public ResponseEntity<ContractPublicView> getContractByToken(@PathVariable String token) {
        log.info("Public request to view contract by token");

        try {
            ContractPublicView contract = esignatureService.getContractByToken(token);
            return ResponseEntity.ok(contract);
        } catch (Exception e) {
            log.error("Failed to get contract by token", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Obtient l'URL de redirection vers DocuSign pour signer
     *
     * POST /api/public/contracts/token/{token}/redirect
     *
     * Cette méthode est appelée quand le locataire clique sur "Signer le contrat"
     * Elle retourne l'URL DocuSign où rediriger l'utilisateur
     */
    @PostMapping("/token/{token}/redirect")
    public ResponseEntity<RedirectResponse> getSigningRedirect(
            @PathVariable String token,
            @RequestParam(required = false) String returnUrl) {
        log.info("Public request to get signing redirect URL");

        try {
            ContractPublicView contract = esignatureService.getContractByToken(token);

            // L'URL de signature est déjà dans documentUrl
            String signingUrl = contract.documentUrl();

            return ResponseEntity.ok(new RedirectResponse(signingUrl));
        } catch (Exception e) {
            log.error("Failed to get signing redirect", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * DTO pour la réponse de redirection
     */
    public record RedirectResponse(String signingUrl) {}
}
