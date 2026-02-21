package com.howners.gestion.dto.request;

import com.howners.gestion.domain.property.PropertyType;
import com.howners.gestion.domain.property.HeatingType;
import com.howners.gestion.domain.property.PropertyCondition;
import com.howners.gestion.dto.AddressDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record CreatePropertyRequest(
        @NotBlank(message = "Property name is required")
        String name,

        @NotNull(message = "Property type is required")
        PropertyType propertyType,

        @Valid
        @NotNull(message = "Address is required")
        AddressDTO address,

        @Min(value = 0, message = "Surface area must be positive")
        BigDecimal surfaceArea,

        @Min(value = 0, message = "Bedrooms must be positive")
        Integer bedrooms,

        @Min(value = 0, message = "Bathrooms must be positive")
        Integer bathrooms,

        String description,

        @Min(value = 0, message = "Condo fees must be positive")
        BigDecimal condoFees,

        @Min(value = 0, message = "Property tax must be positive")
        BigDecimal propertyTax,

        @Min(value = 0, message = "Business tax must be positive")
        BigDecimal businessTax,

        @Min(value = 0, message = "Home insurance must be positive")
        BigDecimal homeInsurance,

        @Min(value = 0, message = "Purchase price must be positive")
        BigDecimal purchasePrice,

        @Pattern(regexp = "[A-G]", message = "DPE rating must be between A and G")
        String dpeRating,

        @Pattern(regexp = "[A-G]", message = "GES rating must be between A and G")
        String gesRating,

        @Min(value = 1800, message = "Construction year must be after 1800")
        Integer constructionYear,

        Integer floorNumber,

        @Min(value = 1, message = "Total floors must be at least 1")
        Integer totalFloors,

        HeatingType heatingType,

        Boolean hasParking,

        Boolean hasElevator,

        Boolean isFurnished,

        PropertyCondition propertyCondition
) {}
