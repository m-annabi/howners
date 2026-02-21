package com.howners.gestion.service.search;

import com.howners.gestion.domain.listing.Listing;
import com.howners.gestion.domain.property.PropertyType;
import com.howners.gestion.domain.search.TenantSearchProfile;
import com.howners.gestion.dto.rating.TenantScoreResponse;
import com.howners.gestion.dto.search.TenantSearchProfileResponse;
import com.howners.gestion.dto.search.TenantSearchResultResponse;
import com.howners.gestion.dto.search.TenantSearchResultResponse.CompatibilityBreakdown;
import com.howners.gestion.exception.ResourceNotFoundException;
import com.howners.gestion.repository.ListingRepository;
import com.howners.gestion.repository.TenantSearchProfileRepository;
import com.howners.gestion.service.rating.TenantScoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantDiscoveryService {

    private final TenantSearchProfileRepository profileRepository;
    private final ListingRepository listingRepository;
    private final TenantScoringService tenantScoringService;
    private final MatchingService matchingService;

    @Transactional(readOnly = true)
    public List<TenantSearchResultResponse> searchTenants(
            String city, String department, String postalCode,
            BigDecimal budgetMin, BigDecimal budgetMax,
            PropertyType propertyType, UUID listingId, String sortBy) {

        boolean hasCity = city != null && !city.isBlank();
        boolean hasDept = department != null && !department.isBlank();
        boolean hasPostal = postalCode != null && !postalCode.isBlank();

        List<TenantSearchProfile> profiles = profileRepository.searchActiveProfiles(
                hasCity ? city : "",
                hasDept ? department : "",
                hasPostal ? postalCode : "",
                budgetMin,
                budgetMax,
                propertyType
        );

        Listing listing = null;
        if (listingId != null) {
            listing = listingRepository.findById(listingId).orElse(null);
        }

        final Listing refListing = listing;

        List<TenantSearchResultResponse> results = profiles.stream().map(profile -> {
            TenantScoreResponse tenantScore = null;
            try {
                tenantScore = tenantScoringService.calculateScore(profile.getTenant().getId());
            } catch (Exception e) {
                log.warn("Could not calculate score for tenant {}", profile.getTenant().getId());
            }

            Integer compatibilityScore = null;
            CompatibilityBreakdown compatibility = null;
            if (refListing != null) {
                compatibilityScore = matchingService.calculateCompatibility(profile, refListing);
                compatibility = matchingService.calculateBreakdown(profile, refListing);
            }

            return new TenantSearchResultResponse(
                    TenantSearchProfileResponse.from(profile),
                    tenantScore,
                    compatibilityScore,
                    compatibility
            );
        }).toList();

        // Sort
        if ("score".equals(sortBy)) {
            results = results.stream()
                    .sorted(Comparator.comparingInt((TenantSearchResultResponse r) ->
                            r.tenantScore() != null ? r.tenantScore().score() : 0).reversed())
                    .toList();
        } else if ("relevance".equals(sortBy)) {
            results = results.stream()
                    .sorted(Comparator.comparingInt((TenantSearchResultResponse r) ->
                            r.compatibilityScore() != null ? r.compatibilityScore() : 0).reversed())
                    .toList();
        }

        return results;
    }

    @Transactional(readOnly = true)
    public TenantSearchResultResponse getTenantProfile(UUID profileId, UUID listingId) {
        TenantSearchProfile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant search profile not found"));

        TenantScoreResponse tenantScore = null;
        try {
            tenantScore = tenantScoringService.calculateScore(profile.getTenant().getId());
        } catch (Exception e) {
            log.warn("Could not calculate score for tenant {}", profile.getTenant().getId());
        }

        Integer compatibilityScore = null;
        CompatibilityBreakdown compatibility = null;
        if (listingId != null) {
            Listing listing = listingRepository.findById(listingId).orElse(null);
            if (listing != null) {
                compatibilityScore = matchingService.calculateCompatibility(profile, listing);
                compatibility = matchingService.calculateBreakdown(profile, listing);
            }
        }

        return new TenantSearchResultResponse(
                TenantSearchProfileResponse.from(profile),
                tenantScore,
                compatibilityScore,
                compatibility
        );
    }
}
