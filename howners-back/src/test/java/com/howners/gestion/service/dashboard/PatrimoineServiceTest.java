package com.howners.gestion.service.dashboard;

import com.howners.gestion.domain.expense.Expense;
import com.howners.gestion.domain.expense.ExpenseCategory;
import com.howners.gestion.domain.property.Property;
import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.domain.rental.RentalStatus;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.analytics.PatrimoineResponse;
import com.howners.gestion.exception.ForbiddenException;
import com.howners.gestion.repository.ExpenseRepository;
import com.howners.gestion.repository.PaymentRepository;
import com.howners.gestion.repository.PropertyRepository;
import com.howners.gestion.repository.RentalRepository;
import com.howners.gestion.security.UserPrincipal;
import com.howners.gestion.service.subscription.FeatureGateService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatrimoineServiceTest {

    @Mock PropertyRepository propertyRepository;
    @Mock PaymentRepository paymentRepository;
    @Mock ExpenseRepository expenseRepository;
    @Mock RentalRepository rentalRepository;
    @Mock FeatureGateService featureGateService;

    @InjectMocks PatrimoineService patrimoineService;

    UUID ownerId;
    Property property;

    @BeforeEach
    void setup() {
        ownerId = UUID.randomUUID();
        User owner = User.builder().id(ownerId).build();
        property = new Property();
        property.setId(UUID.randomUUID());
        property.setOwner(owner);
        property.setName("Bien Test");
        property.setCity("Paris");

        UserPrincipal principal = new UserPrincipal(ownerId, "o@t.fr", "x", "OWNER", true);
        Authentication auth = org.springframework.security.authentication.UsernamePasswordAuthenticationToken
                .authenticated(principal, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        lenient().when(featureGateService.hasFeature(ownerId, "advanced_dashboard")).thenReturn(true);
        lenient().when(propertyRepository.findByOwnerId(ownerId)).thenReturn(List.of(property));
        lenient().when(expenseRepository.findByPropertyId(any())).thenReturn(List.of());
        lenient().when(rentalRepository.findByPropertyId(any())).thenReturn(List.of());
        lenient().when(paymentRepository.sumPaidRentByPropertyAndPeriod(any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);
    }

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    @Test
    void refuseSansFeatureAdvancedDashboard() {
        when(featureGateService.hasFeature(ownerId, "advanced_dashboard")).thenReturn(false);

        assertThatThrownBy(() -> patrimoineService.getPatrimoine())
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void calculeRentabilitesEtCashFlow() {
        property.setPurchasePrice(new BigDecimal("200000"));
        property.setPropertyTax(new BigDecimal("1200"));
        when(paymentRepository.sumPaidRentByPropertyAndPeriod(any(), any(), any()))
                .thenReturn(new BigDecimal("9600.00"));
        when(expenseRepository.findByPropertyId(property.getId())).thenReturn(List.of(
                Expense.builder().category(ExpenseCategory.REPAIR)
                        .amount(new BigDecimal("600.00"))
                        .expenseDate(LocalDate.now().minusMonths(2)).build()
        ));

        PatrimoineResponse patrimoine = patrimoineService.getPatrimoine();

        var bien = patrimoine.biens().get(0);
        assertThat(bien.revenusAnnuels()).isEqualByComparingTo("9600.00");
        assertThat(bien.chargesAnnuelles()).isEqualByComparingTo("1800.00");
        // brute = 9600/200000 = 4.80 % ; nette = 7800/200000 = 3.90 %
        assertThat(bien.rentabiliteBrutePercent()).isEqualByComparingTo("4.80");
        assertThat(bien.rentabiliteNettePercent()).isEqualByComparingTo("3.90");
        // cash-flow mensuel = 7800/12 = 650
        assertThat(bien.cashFlowMensuel()).isEqualByComparingTo("650.00");
    }

    @Test
    void rentabilitesNullesSansPrixDAchat() {
        when(paymentRepository.sumPaidRentByPropertyAndPeriod(any(), any(), any()))
                .thenReturn(new BigDecimal("9600.00"));

        PatrimoineResponse patrimoine = patrimoineService.getPatrimoine();

        var bien = patrimoine.biens().get(0);
        assertThat(bien.rentabiliteBrutePercent()).isNull();
        assertThat(bien.rentabiliteNettePercent()).isNull();
        assertThat(patrimoine.rendementNetMoyenPondere()).isNull();
    }

    @Test
    void calculeLeTauxOccupationAvecPeriodesClippees() {
        // Bail couvrant la moitié de l'année écoulée (terminé il y a ~182 jours)
        Rental rental = Rental.builder()
                .id(UUID.randomUUID())
                .property(property)
                .status(RentalStatus.TERMINATED)
                .startDate(LocalDate.now().minusYears(3))
                .endDate(LocalDate.now().minusDays(183))
                .build();
        when(rentalRepository.findByPropertyId(property.getId())).thenReturn(List.of(rental));

        PatrimoineResponse patrimoine = patrimoineService.getPatrimoine();

        var bien = patrimoine.biens().get(0);
        assertThat(bien.tauxOccupationPercent().doubleValue()).isBetween(45.0, 55.0);
    }

    @Test
    void occupationCompleteAvecBailActifSansFin() {
        Rental rental = Rental.builder()
                .id(UUID.randomUUID())
                .property(property)
                .status(RentalStatus.ACTIVE)
                .startDate(LocalDate.now().minusYears(2))
                .build();
        when(rentalRepository.findByPropertyId(property.getId())).thenReturn(List.of(rental));

        PatrimoineResponse patrimoine = patrimoineService.getPatrimoine();

        assertThat(patrimoine.biens().get(0).tauxOccupationPercent().doubleValue()).isGreaterThan(99.0);
    }
}
