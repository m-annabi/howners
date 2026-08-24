package com.howners.gestion.dto.response;

import com.howners.gestion.domain.property.Property;
import com.howners.gestion.domain.property.PropertyType;
import com.howners.gestion.domain.property.HeatingType;
import com.howners.gestion.domain.property.PropertyCondition;
import com.howners.gestion.dto.AddressDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PropertyResponse(
        UUID id,
        UUID ownerId,
        String name,
        PropertyType propertyType,
        AddressDTO address,
        BigDecimal surfaceArea,
        Integer bedrooms,
        Integer bathrooms,
        String description,
        BigDecimal condoFees,
        BigDecimal propertyTax,
        BigDecimal businessTax,
        BigDecimal homeInsurance,
        BigDecimal purchasePrice,
        java.time.LocalDate acquisitionDate,
        BigDecimal landValue,
        BigDecimal notaryFees,
        String dpeRating,
        java.time.LocalDate dpeDate,
        String gesRating,
        Integer constructionYear,
        Integer floorNumber,
        Integer totalFloors,
        HeatingType heatingType,
        Boolean hasParking,
        Boolean hasElevator,
        Boolean isFurnished,
        java.util.List<String> amenities,
        PropertyCondition propertyCondition,
        BigDecimal currentMonthlyRent,
        BigDecimal grossYieldPercent,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PropertyResponse from(Property property) {
        return from(property, null);
    }

    public static PropertyResponse from(Property property, BigDecimal currentMonthlyRent) {
        BigDecimal yield = null;
        if (currentMonthlyRent != null
                && property.getPurchasePrice() != null
                && property.getPurchasePrice().signum() > 0) {
            BigDecimal annualIncome = currentMonthlyRent.multiply(BigDecimal.valueOf(12));
            yield = annualIncome
                    .multiply(BigDecimal.valueOf(100))
                    .divide(property.getPurchasePrice(), 2, java.math.RoundingMode.HALF_UP);
        }
        return new PropertyResponse(
                property.getId(),
                property.getOwner().getId(),
                property.getName(),
                property.getPropertyType(),
                new AddressDTO(
                        property.getAddressLine1(),
                        property.getAddressLine2(),
                        property.getCity(),
                        property.getPostalCode(),
                        property.getDepartment(),
                        property.getCountry()
                ),
                property.getSurfaceArea(),
                property.getBedrooms(),
                property.getBathrooms(),
                property.getDescription(),
                property.getCondoFees(),
                property.getPropertyTax(),
                property.getBusinessTax(),
                property.getHomeInsurance(),
                property.getPurchasePrice(),
                property.getAcquisitionDate(),
                property.getLandValue(),
                property.getNotaryFees(),
                property.getDpeRating(),
                property.getDpeDate(),
                property.getGesRating(),
                property.getConstructionYear(),
                property.getFloorNumber(),
                property.getTotalFloors(),
                property.getHeatingType(),
                property.getHasParking(),
                property.getHasElevator(),
                property.getIsFurnished(),
                parseJsonList(property.getAmenities()),
                property.getPropertyCondition(),
                currentMonthlyRent,
                yield,
                property.getCreatedAt(),
                property.getUpdatedAt()
        );
    }

    /** Désérialise la colonne JSON des équipements (tolérante : liste vide si absent/illisible). */
    private static java.util.List<String> parseJsonList(String raw) {
        if (raw == null || raw.isBlank()) return java.util.List.of();
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(raw, new com.fasterxml.jackson.core.type.TypeReference<java.util.List<String>>() {});
        } catch (Exception e) {
            return java.util.List.of();
        }
    }
}
