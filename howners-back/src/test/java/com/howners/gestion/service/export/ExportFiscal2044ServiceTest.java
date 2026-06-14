package com.howners.gestion.service.export;

import com.howners.gestion.domain.expense.Expense;
import com.howners.gestion.domain.expense.ExpenseCategory;
import com.howners.gestion.domain.property.Property;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.analytics.Declaration2044Response;
import com.howners.gestion.exception.ForbiddenException;
import com.howners.gestion.repository.ExpenseRepository;
import com.howners.gestion.repository.PaymentRepository;
import com.howners.gestion.repository.PropertyRepository;
import com.howners.gestion.security.UserPrincipal;
import com.howners.gestion.service.contract.PdfService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportFiscal2044ServiceTest {

    @Mock PropertyRepository propertyRepository;
    @Mock PaymentRepository paymentRepository;
    @Mock ExpenseRepository expenseRepository;
    @Mock FeatureGateService featureGateService;
    @Mock PdfService pdfService;

    @InjectMocks ExportFiscal2044Service exportService;

    UUID ownerId;
    Property property;

    @BeforeEach
    void setup() {
        ownerId = UUID.randomUUID();
        User owner = User.builder().id(ownerId).build();
        property = new Property();
        property.setId(UUID.randomUUID());
        property.setOwner(owner);
        property.setName("Appartement Paris");
        property.setAddressLine1("1 rue Test");
        property.setPostalCode("75001");
        property.setCity("Paris");

        UserPrincipal principal = new UserPrincipal(ownerId, "o@t.fr", "x", "OWNER", true);
        Authentication auth = org.springframework.security.authentication.UsernamePasswordAuthenticationToken
                .authenticated(principal, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        lenient().when(featureGateService.hasFeature(ownerId, "tax_export")).thenReturn(true);
    }

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    private Expense expense(ExpenseCategory cat, String montant, int annee) {
        return Expense.builder()
                .category(cat)
                .amount(new BigDecimal(montant))
                .expenseDate(LocalDate.of(annee, 6, 15))
                .build();
    }

    @Test
    void mappeLesCategoriesVersLesLignes2044() {
        when(propertyRepository.findByOwnerId(ownerId)).thenReturn(List.of(property));
        when(paymentRepository.sumPaidRentByPropertyAndPeriod(eq(property.getId()), any(), any()))
                .thenReturn(new BigDecimal("9600.00"));
        when(expenseRepository.findByPropertyId(property.getId())).thenReturn(List.of(
                expense(ExpenseCategory.INSURANCE, "300.00", 2025),       // 222
                expense(ExpenseCategory.REPAIR, "1200.00", 2025),         // 224
                expense(ExpenseCategory.TAX, "900.00", 2025),             // 227
                expense(ExpenseCategory.UTILITIES, "500.00", 2025),       // exclu (récupérable)
                expense(ExpenseCategory.REPAIR, "999.00", 2024)           // exclu (mauvaise année)
        ));

        Declaration2044Response declaration = exportService.genererDeclaration(2025);

        assertThat(declaration.biens()).hasSize(1);
        var bien = declaration.biens().get(0);
        assertThat(bien.revenusBruts()).isEqualByComparingTo("9600.00");
        assertThat(bien.chargesParLigne().get("222")).isEqualByComparingTo("300.00");
        assertThat(bien.chargesParLigne().get("224")).isEqualByComparingTo("1200.00");
        assertThat(bien.chargesParLigne().get("227")).isEqualByComparingTo("900.00");
        assertThat(bien.chargesParLigne()).doesNotContainKey("UTILITIES");
        assertThat(bien.totalCharges()).isEqualByComparingTo("2400.00");
        assertThat(declaration.revenuFoncierNet()).isEqualByComparingTo("7200.00");
    }

    @Test
    void ignoreLesBiensSansActivite() {
        when(propertyRepository.findByOwnerId(ownerId)).thenReturn(List.of(property));
        when(paymentRepository.sumPaidRentByPropertyAndPeriod(any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(expenseRepository.findByPropertyId(any())).thenReturn(List.of());

        Declaration2044Response declaration = exportService.genererDeclaration(2025);

        assertThat(declaration.biens()).isEmpty();
        assertThat(declaration.totalRevenusBruts()).isEqualByComparingTo("0");
    }

    @Test
    void refuseSansFeatureTaxExport() {
        when(featureGateService.hasFeature(ownerId, "tax_export")).thenReturn(false);

        assertThatThrownBy(() -> exportService.genererDeclaration(2025))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("plan Pro");
    }

    @Test
    void genereUnCsvAvecTotaux() {
        when(propertyRepository.findByOwnerId(ownerId)).thenReturn(List.of(property));
        when(paymentRepository.sumPaidRentByPropertyAndPeriod(any(), any(), any()))
                .thenReturn(new BigDecimal("9600.00"));
        when(expenseRepository.findByPropertyId(any())).thenReturn(List.of(
                expense(ExpenseCategory.TAX, "900.00", 2025)));

        String csv = exportService.genererCsv(2025);

        assertThat(csv).contains("211");
        assertThat(csv).contains("227");
        assertThat(csv).contains("Revenu foncier net");
    }
}
