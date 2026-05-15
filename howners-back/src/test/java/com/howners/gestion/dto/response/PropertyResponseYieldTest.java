package com.howners.gestion.dto.response;

import com.howners.gestion.domain.property.Property;
import com.howners.gestion.domain.property.PropertyType;
import com.howners.gestion.domain.user.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PropertyResponseYieldTest {

    private Property baseProperty(BigDecimal purchasePrice) {
        return Property.builder()
                .name("Test")
                .propertyType(PropertyType.APARTMENT)
                .owner(User.builder().id(java.util.UUID.randomUUID()).build())
                .purchasePrice(purchasePrice)
                .build();
    }

    @Test
    void grossYield_isNull_whenNoRent() {
        PropertyResponse r = PropertyResponse.from(baseProperty(new BigDecimal("200000")), null);
        assertThat(r.currentMonthlyRent()).isNull();
        assertThat(r.grossYieldPercent()).isNull();
    }

    @Test
    void grossYield_isNull_whenNoPurchasePrice() {
        PropertyResponse r = PropertyResponse.from(baseProperty(null), new BigDecimal("800"));
        assertThat(r.currentMonthlyRent()).isEqualByComparingTo("800");
        assertThat(r.grossYieldPercent()).isNull();
    }

    @Test
    void grossYield_computesAnnualOverPurchasePrice() {
        // 850 € * 12 = 10 200 / 195 000 * 100 = 5.23 %
        PropertyResponse r = PropertyResponse.from(baseProperty(new BigDecimal("195000")), new BigDecimal("850"));
        assertThat(r.grossYieldPercent()).isEqualByComparingTo("5.23");
    }

    @Test
    void grossYield_roundsHalfUpToTwoDecimals() {
        // 1750 * 12 = 21 000 / 520 000 = 4.0384615... → 4.04
        PropertyResponse r = PropertyResponse.from(baseProperty(new BigDecimal("520000")), new BigDecimal("1750"));
        assertThat(r.grossYieldPercent()).isEqualByComparingTo("4.04");
    }

    @Test
    void grossYield_isNull_whenPurchasePriceZero() {
        // Avoid division by zero — the helper must guard.
        PropertyResponse r = PropertyResponse.from(baseProperty(BigDecimal.ZERO), new BigDecimal("800"));
        assertThat(r.grossYieldPercent()).isNull();
    }
}
