package com.howners.gestion.service.search;

import com.howners.gestion.domain.listing.Listing;
import com.howners.gestion.domain.search.FurnishedPreference;
import com.howners.gestion.domain.search.TenantSearchProfile;
import com.howners.gestion.dto.search.TenantSearchResultResponse.CompatibilityBreakdown;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class MatchingService {

    public int calculateCompatibility(TenantSearchProfile profile, Listing listing) {
        CompatibilityBreakdown breakdown = calculateBreakdown(profile, listing);
        return breakdown.zoneScore() + breakdown.budgetScore() + breakdown.propertyTypeScore()
                + breakdown.surfaceScore() + breakdown.bedroomScore() + breakdown.furnishedScore();
    }

    public CompatibilityBreakdown calculateBreakdown(TenantSearchProfile profile, Listing listing) {
        int zoneScore = calculateZoneScore(profile, listing);
        int budgetScore = calculateBudgetScore(profile, listing);
        int propertyTypeScore = calculatePropertyTypeScore(profile, listing);
        int surfaceScore = calculateSurfaceScore(profile, listing);
        int bedroomScore = calculateBedroomScore(profile, listing);
        int furnishedScore = calculateFurnishedScore(profile, listing);
        return new CompatibilityBreakdown(zoneScore, budgetScore, propertyTypeScore, surfaceScore, bedroomScore, furnishedScore);
    }

    private int calculateZoneScore(TenantSearchProfile profile, Listing listing) {
        String profileCity = profile.getDesiredCity();
        String profileDept = profile.getDesiredDepartment();
        String profilePostal = profile.getDesiredPostalCode();
        String listingCity = listing.getProperty().getCity();
        String listingDept = listing.getProperty().getDepartment();
        String listingPostal = listing.getProperty().getPostalCode();

        boolean hasZonePref = (profileCity != null && !profileCity.isBlank())
                || (profileDept != null && !profileDept.isBlank())
                || (profilePostal != null && !profilePostal.isBlank());

        if (!hasZonePref) return 15;

        if (profileCity != null && !profileCity.isBlank()
                && listingCity != null && profileCity.equalsIgnoreCase(listingCity)) {
            return 30;
        }

        if (profilePostal != null && !profilePostal.isBlank()
                && listingPostal != null && listingPostal.startsWith(profilePostal)) {
            return 20;
        }

        if (profileDept != null && !profileDept.isBlank()
                && listingDept != null && profileDept.equalsIgnoreCase(listingDept)) {
            return 15;
        }

        return 0;
    }

    private int calculateBudgetScore(TenantSearchProfile profile, Listing listing) {
        BigDecimal price = listing.getPricePerMonth();
        BigDecimal budgetMin = profile.getBudgetMin();
        BigDecimal budgetMax = profile.getBudgetMax();

        if (budgetMin == null && budgetMax == null) return 15;
        if (price == null) return 15;

        boolean inRange = true;
        if (budgetMin != null && price.compareTo(budgetMin) < 0) inRange = false;
        if (budgetMax != null && price.compareTo(budgetMax) > 0) inRange = false;

        if (inRange) return 30;

        // Check 20% tolerance
        boolean inTolerance = true;
        if (budgetMin != null) {
            BigDecimal toleranceMin = budgetMin.multiply(BigDecimal.valueOf(0.80));
            if (price.compareTo(toleranceMin) < 0) inTolerance = false;
        }
        if (budgetMax != null) {
            BigDecimal toleranceMax = budgetMax.multiply(BigDecimal.valueOf(1.20));
            if (price.compareTo(toleranceMax) > 0) inTolerance = false;
        }

        return inTolerance ? 15 : 0;
    }

    private int calculatePropertyTypeScore(TenantSearchProfile profile, Listing listing) {
        if (profile.getDesiredPropertyType() == null) return 8;
        if (listing.getProperty().getPropertyType() == null) return 8;
        return profile.getDesiredPropertyType() == listing.getProperty().getPropertyType() ? 15 : 0;
    }

    private int calculateSurfaceScore(TenantSearchProfile profile, Listing listing) {
        if (profile.getMinSurface() == null) return 5;
        if (listing.getProperty().getSurfaceArea() == null) return 5;
        return listing.getProperty().getSurfaceArea().compareTo(profile.getMinSurface()) >= 0 ? 10 : 0;
    }

    private int calculateBedroomScore(TenantSearchProfile profile, Listing listing) {
        if (profile.getMinBedrooms() == null) return 5;
        if (listing.getProperty().getBedrooms() == null) return 5;
        return listing.getProperty().getBedrooms() >= profile.getMinBedrooms() ? 10 : 0;
    }

    private int calculateFurnishedScore(TenantSearchProfile profile, Listing listing) {
        FurnishedPreference pref = profile.getFurnishedPreference();
        if (pref == null || pref == FurnishedPreference.NO_PREFERENCE) return 3;

        Boolean isFurnished = listing.getProperty().getIsFurnished();
        if (isFurnished == null) return 3;

        if (pref == FurnishedPreference.FURNISHED_ONLY && isFurnished) return 5;
        if (pref == FurnishedPreference.UNFURNISHED_ONLY && !isFurnished) return 5;

        return 0;
    }
}
