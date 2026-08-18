package com.howners.gestion.controller;

import com.howners.gestion.domain.affiliate.AffiliateCategory;
import com.howners.gestion.domain.affiliate.AffiliatePartner;
import com.howners.gestion.repository.AffiliatePartnerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/affiliates")
@RequiredArgsConstructor
public class AffiliateController {

    private final AffiliatePartnerRepository repository;

    /** Public read — visible by any user (logged-in or not). */
    @GetMapping
    public ResponseEntity<List<AffiliatePartner>> list(
            @RequestParam(required = false) AffiliateCategory category) {
        if (category != null) {
            return ResponseEntity.ok(repository.findByCategoryAndIsActiveTrueOrderByDisplayOrderAsc(category));
        }
        return ResponseEntity.ok(repository.findByIsActiveTrueOrderByDisplayOrderAsc());
    }

    /** Admin CRUD — to add or update commercial partners. */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AffiliatePartner> create(@RequestBody AffiliatePartner partner) {
        partner.setId(null);
        return ResponseEntity.ok(repository.save(partner));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AffiliatePartner> update(@PathVariable UUID id, @RequestBody AffiliatePartner update) {
        return repository.findById(id).map(existing -> {
            // Fusion : un champ absent (null) ne remplace pas la valeur existante, sinon une
            // mise à jour partielle écraserait une colonne obligatoire (affiliate_url…) → 500.
            if (update.getName() != null) existing.setName(update.getName());
            if (update.getSlug() != null) existing.setSlug(update.getSlug());
            if (update.getCategory() != null) existing.setCategory(update.getCategory());
            if (update.getTagline() != null) existing.setTagline(update.getTagline());
            if (update.getDescription() != null) existing.setDescription(update.getDescription());
            if (update.getAffiliateUrl() != null) existing.setAffiliateUrl(update.getAffiliateUrl());
            if (update.getCommissionRate() != null) existing.setCommissionRate(update.getCommissionRate());
            if (update.getLogoUrl() != null) existing.setLogoUrl(update.getLogoUrl());
            if (update.getIsActive() != null) existing.setIsActive(update.getIsActive());
            if (update.getDisplayOrder() != null) existing.setDisplayOrder(update.getDisplayOrder());
            return ResponseEntity.ok(repository.save(existing));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
