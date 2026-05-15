package com.howners.gestion.service.ai;

import com.howners.gestion.domain.property.Property;
import com.howners.gestion.domain.property.PropertyType;
import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.domain.rental.RentalType;
import com.howners.gestion.dto.ai.DraftLeaseRequest;
import com.howners.gestion.dto.ai.DraftLeaseResponse;
import com.howners.gestion.exception.ResourceNotFoundException;
import com.howners.gestion.repository.RentalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiLeaseServiceTest {

    @Mock RentalRepository rentalRepository;
    @InjectMocks AiLeaseService aiLeaseService;

    Rental rental;
    UUID rentalId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(aiLeaseService, "openaiApiKey", "");        // mock path
        ReflectionTestUtils.setField(aiLeaseService, "openaiModel", "gpt-4o-mini");

        Property property = Property.builder()
                .name("Studio Suresnes Centre")
                .city("Suresnes")
                .propertyType(PropertyType.STUDIO)
                .build();
        rental = Rental.builder()
                .id(rentalId)
                .property(property)
                .monthlyRent(new BigDecimal("850"))
                .charges(new BigDecimal("50"))
                .depositAmount(new BigDecimal("1700"))
                .rentalType(RentalType.LONG_TERM)
                .startDate(LocalDate.now())
                .build();
    }

    @Test
    void draft_returnsMockContent_whenNoApiKey() {
        when(rentalRepository.findById(rentalId)).thenReturn(Optional.of(rental));

        DraftLeaseResponse resp = aiLeaseService.draft(new DraftLeaseRequest(
                rentalId, false, 36, 3, false, null));

        assertThat(resp.engine()).isEqualTo("mock");
        assertThat(resp.disclaimer()).contains("professionnel du droit");
        assertThat(resp.content())
                .contains("CONTRAT DE BAIL")
                .contains("Studio Suresnes Centre")
                .contains("850")           // loyer
                .contains("1700");         // dépôt
    }

    @Test
    void draft_includesCustomClauses_whenProvided() {
        when(rentalRepository.findById(rentalId)).thenReturn(Optional.of(rental));

        DraftLeaseResponse resp = aiLeaseService.draft(new DraftLeaseRequest(
                rentalId, true, 12, 1, true, "Pas de fumeurs."));

        assertThat(resp.content())
                .contains("MEUBLÉE")                       // furnished branch
                .contains("animaux familiers sont autorisés")
                .contains("Pas de fumeurs.")
                .contains("CLAUSES PARTICULIÈRES");
    }

    @Test
    void draft_throwsResourceNotFound_whenRentalMissing() {
        UUID missing = UUID.randomUUID();
        when(rentalRepository.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> aiLeaseService.draft(new DraftLeaseRequest(missing, false, 36, 3, false, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void draft_defaultsLeaseDurationByType_whenMissing() {
        when(rentalRepository.findById(rentalId)).thenReturn(Optional.of(rental));

        DraftLeaseResponse unfurnished = aiLeaseService.draft(new DraftLeaseRequest(
                rentalId, false, null, null, null, null));
        DraftLeaseResponse furnished = aiLeaseService.draft(new DraftLeaseRequest(
                rentalId, true, null, null, null, null));

        // Default durations: 36 months unfurnished (loi de 89), 12 months furnished.
        assertThat(unfurnished.content()).contains("durée de 36 mois");
        assertThat(furnished.content()).contains("durée de 12 mois");
    }
}
