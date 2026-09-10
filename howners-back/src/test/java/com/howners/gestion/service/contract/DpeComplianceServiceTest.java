package com.howners.gestion.service.contract;

import com.howners.gestion.domain.property.Property;
import com.howners.gestion.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Règles DPE de l'audit de conformité (T-03 : DPE obligatoire/valide ; T-05 : décence
 * énergétique loi Climat et gel des loyers F/G). Année de référence : 2026 → G interdit.
 */
class DpeComplianceServiceTest {

    private final DpeComplianceService service = new DpeComplianceService();

    private Property property(String dpe, LocalDate dpeDate) {
        return Property.builder().dpeRating(dpe).dpeDate(dpeDate).build();
    }

    // ---- Mise en location (assertCanRentOut) ----

    @Test
    void rentOut_dpeManquant_bloque() {
        assertThatThrownBy(() -> service.assertCanRentOut(property(null, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("DPE");
    }

    @Test
    void rentOut_dpePerime_bloque() {
        assertThatThrownBy(() -> service.assertCanRentOut(property("C", LocalDate.now().minusYears(11))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("valide");
    }

    @Test
    void rentOut_classeG_interditeDepuis2025_bloque() {
        assertThatThrownBy(() -> service.assertCanRentOut(property("G", LocalDate.now().minusYears(1))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("indécent");
    }

    @Test
    void rentOut_classeF_encoreLouableEn2026_ok() {
        // F n'est interdit qu'en 2028 : autorisé aujourd'hui.
        assertThatCode(() -> service.assertCanRentOut(property("F", LocalDate.now().minusYears(1))))
                .doesNotThrowAnyException();
    }

    @Test
    void rentOut_classeC_ok() {
        assertThatCode(() -> service.assertCanRentOut(property("C", LocalDate.now().minusYears(2))))
                .doesNotThrowAnyException();
    }

    @Test
    void rentOut_dpeSansDate_accepteSiClasseDecente() {
        assertThatCode(() -> service.assertCanRentOut(property("D", null)))
                .doesNotThrowAnyException();
    }

    // ---- Révision de loyer (assertCanReviseRent) ----

    @Test
    void revise_passoireF_gelee() {
        assertThatThrownBy(() -> service.assertCanReviseRent(property("F", null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("gelé");
    }

    @Test
    void revise_passoireG_gelee() {
        assertThatThrownBy(() -> service.assertCanReviseRent(property("G", null)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void revise_classeE_autorisee() {
        assertThatCode(() -> service.assertCanReviseRent(property("E", null)))
                .doesNotThrowAnyException();
    }
}
