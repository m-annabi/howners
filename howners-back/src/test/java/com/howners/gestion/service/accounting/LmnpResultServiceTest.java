package com.howners.gestion.service.accounting;

import com.howners.gestion.domain.accounting.AmortizableAsset;
import com.howners.gestion.domain.accounting.AssetType;
import com.howners.gestion.domain.accounting.FiscalActivity;
import com.howners.gestion.domain.accounting.Loan;
import com.howners.gestion.domain.expense.Expense;
import com.howners.gestion.domain.expense.ExpenseCategory;
import com.howners.gestion.domain.property.Property;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.repository.AmortizableAssetRepository;
import com.howners.gestion.repository.ExpenseRepository;
import com.howners.gestion.repository.LoanRepository;
import com.howners.gestion.repository.PaymentRepository;
import com.howners.gestion.repository.PropertyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LmnpResultServiceTest {

    @Mock private PropertyRepository propertyRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private AmortizableAssetRepository assetRepository;
    @Mock private LoanRepository loanRepository;

    private LmnpResultService service;

    private UUID ownerId;
    private FiscalActivity activity;
    private Property property;

    @BeforeEach
    void setUp() {
        service = new LmnpResultService(propertyRepository, paymentRepository, expenseRepository,
                assetRepository, new AmortizationService(), loanRepository, new LoanScheduleService());
        ownerId = UUID.randomUUID();
        User owner = User.builder().id(ownerId).email("o@test.com").firstName("O").lastName("W").build();
        property = Property.builder().id(UUID.randomUUID()).name("Bien").owner(owner).build();
        activity = FiscalActivity.builder().id(UUID.randomUUID()).owner(owner)
                .startDate(LocalDate.of(2024, 1, 1)).build();

        when(propertyRepository.findByOwnerId(ownerId)).thenReturn(List.of(property));
    }

    // Immobilisation dont l'annuité de l'exercice cible vaut exactement `annuite` (durée 1 an).
    private AmortizableAsset assetAnnuite(String base) {
        return AmortizableAsset.builder().type(AssetType.MOBILIER).label("Mob")
                .base(new BigDecimal(base)).durationYears(1).startDate(LocalDate.of(2024, 1, 1)).build();
    }

    private Expense charge(String amount, ExpenseCategory cat) {
        return Expense.builder().amount(new BigDecimal(amount)).category(cat)
                .expenseDate(LocalDate.of(2024, 6, 1)).property(property).build();
    }

    private void stub(BigDecimal recettes, List<Expense> charges, List<AmortizableAsset> assets) {
        when(paymentRepository.sumPaidRentByPropertyAndPeriod(eq(property.getId()), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(recettes);
        when(expenseRepository.findByOwnerId(ownerId)).thenReturn(charges);
        when(assetRepository.findByActivityId(activity.getId())).thenReturn(assets);
    }

    @Test
    void amortissementEntierementDeductible_quandBeneficeSuffisant() {
        // recettes 10000, charges 3000, dotation 5000 -> avant amort 7000 -> amort déductible 5000 -> fiscal 2000
        stub(new BigDecimal("10000"), List.of(charge("3000", ExpenseCategory.MAINTENANCE)),
                List.of(assetAnnuite("5000")));

        LmnpResult r = service.compute(activity, 2024);

        assertThat(r.recettes()).isEqualByComparingTo("10000");
        assertThat(r.totalCharges()).isEqualByComparingTo("3000");
        assertThat(r.resultatAvantAmortissement()).isEqualByComparingTo("7000");
        assertThat(r.amortissementDeductible()).isEqualByComparingTo("5000.00");
        assertThat(r.amortissementDiffereCumul()).isEqualByComparingTo("0");
        assertThat(r.resultatFiscal()).isEqualByComparingTo("2000.00");
    }

    @Test
    void amortissementPlafonne_etDiffere_quandBeneficeInsuffisant() {
        // recettes 6000, charges 3000, dotation 5000 -> avant amort 3000 -> amort déductible 3000 -> différé 2000 -> fiscal 0
        stub(new BigDecimal("6000"), List.of(charge("3000", ExpenseCategory.MAINTENANCE)),
                List.of(assetAnnuite("5000")));

        LmnpResult r = service.compute(activity, 2024);

        assertThat(r.resultatAvantAmortissement()).isEqualByComparingTo("3000");
        assertThat(r.amortissementDeductible()).isEqualByComparingTo("3000.00");
        assertThat(r.amortissementDiffereCumul()).isEqualByComparingTo("2000.00");
        assertThat(r.resultatFiscal()).isEqualByComparingTo("0.00");
    }

    @Test
    void bilanEquilibre_actifEgalPassif() {
        stub(new BigDecimal("12000"), List.of(charge("2000", ExpenseCategory.INSURANCE)),
                List.of(assetAnnuite("5000")));
        activity.setOpeningCash(new BigDecimal("1000"));

        LmnpResult r = service.compute(activity, 2024);

        assertThat(r.totalActif()).isEqualByComparingTo(r.totalPassif());
    }

    @Test
    void categoriesImmobilisees_exclues_desCharges() {
        // FURNISHING ne doit PAS compter comme charge déductible (c'est une immobilisation)
        stub(new BigDecimal("8000"),
                List.of(charge("2000", ExpenseCategory.MAINTENANCE), charge("3000", ExpenseCategory.FURNISHING)),
                List.of());

        LmnpResult r = service.compute(activity, 2024);

        assertThat(r.totalCharges()).isEqualByComparingTo("2000"); // FURNISHING exclu
    }

    @Test
    void empruntInteretsDeductibles_capitalExclu_etBilanEquilibre() {
        // Emprunt 100 000 € sur 20 ans à 2 %, assurance 30 €/mois.
        Loan loan = Loan.builder().principal(new BigDecimal("100000")).annualRate(new BigDecimal("2.000"))
                .durationMonths(240).startDate(LocalDate.of(2024, 1, 1))
                .insuranceMonthly(new BigDecimal("30")).label("Prêt acquisition").build();
        stub(new BigDecimal("12000"), List.of(charge("2000", ExpenseCategory.INSURANCE)), List.of());
        when(loanRepository.findByActivityId(activity.getId())).thenReturn(List.of(loan));

        LmnpResult r = service.compute(activity, 2024);

        // Les intérêts et l'assurance sont des charges déductibles ; le capital ne l'est pas.
        assertThat(r.chargesParPoste()).containsKey("Intérêts d'emprunt");
        assertThat(r.chargesParPoste().get("Intérêts d'emprunt")).isGreaterThan(BigDecimal.ZERO);
        assertThat(r.chargesParPoste().get("Assurance emprunteur")).isEqualByComparingTo("360"); // 30 × 12
        // Capital restant dû reporté au passif, inférieur au principal (une année remboursée).
        assertThat(r.dettesEmprunt()).isGreaterThan(BigDecimal.ZERO)
                .isLessThan(new BigDecimal("100000"));
        // Le bilan reste équilibré malgré la dette.
        assertThat(r.totalActif()).isEqualByComparingTo(r.totalPassif());
    }
}
