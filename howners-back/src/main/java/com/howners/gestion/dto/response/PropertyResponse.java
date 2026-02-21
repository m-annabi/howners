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
        String dpeRating,
        String gesRating,
        Integer constructionYear,
        Integer floorNumber,
        Integer totalFloors,
        HeatingType heatingType,
        Boolean hasParking,
        Boolean hasElevator,
        Boolean isFurnished,
        PropertyCondition propertyCondition,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PropertyResponse from(Property property) {
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
                property.getDpeRating(),
                property.getGesRating(),
                property.getConstructionYear(),
                property.getFloorNumber(),
                property.getTotalFloors(),
                property.getHeatingType(),
                property.getHasParking(),
                property.getHasElevator(),
                property.getIsFurnished(),
                property.getPropertyCondition(),
                property.getCreatedAt(),
                property.getUpdatedAt()
        );
    }
}
