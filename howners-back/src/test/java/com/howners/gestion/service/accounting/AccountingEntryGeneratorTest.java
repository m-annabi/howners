package com.howners.gestion.service.accounting;

import com.howners.gestion.domain.accounting.AmortizableAsset;
import com.howners.gestion.domain.accounting.AssetType;
import com.howners.gestion.domain.accounting.FiscalActivity;
import com.howners.gestion.domain.accounting.Loan;
import com.howners.gestion.domain.expense.Expense;
import com.howners.gestion.domain.expense.ExpenseCategory;
import com.howners.gestion.domain.payment.Payment;
import com.howners.gestion.domain.payment.PaymentStatus;
import com.howners.gestion.domain.payment.PaymentType;
import com.howners.gestion.domain.property.Property;
import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.repository.AmortizableAssetRepository;
import com.howners.gestion.repository.ExpenseRepository;
import com.howners.gestion.repository.LoanRepository;
import com.howners.gestion.repository.PaymentRepository;
import com.howners.gestion.repository.PropertyRepository;
import com.howners.gestion.service.accounting.AccountingEntryGenerator.JournalEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccountingEntryGeneratorTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private AmortizableAssetRepository assetRepository;
    @Mock private LoanRepository loanRepository;
    @Mock private PropertyRepository propertyRepository;

    private AccountingEntryGenerator generator;
    private FecExportService fecExportService;

    private UUID ownerId;
    private FiscalActivity activity;
    private Property meuble;
    private Property nu;

    @BeforeEach
    void setUp() {
        LoanScheduleService loanScheduleService = new LoanScheduleService();
        AmortizationService amortizationService = new AmortizationService();
        LmnpResultService lmnpResultService = new LmnpResultService(propertyRepository, paymentRepository,
                expenseRepository, assetRepository, amortizationService, loanRepository, loanScheduleService);
        generator = new AccountingEntryGenerator(paymentRepository, expenseRepository, assetRepository,
                amortizationService, lmnpResultService, loanRepository, loanScheduleService);
        fecExportService = new FecExportService();

        ownerId = UUID.randomUUID();
        User owner = User.builder().id(ownerId).email("o@test.com").firstName("O").lastName("W").build();
        meuble = Property.builder().id(UUID.randomUUID()).name("Meublé").owner(owner).isFurnished(true).build();
        nu = Property.builder().id(UUID.randomUUID()).name("Nu").owner(owner).isFurnished(false).build();
        activity = FiscalActivity.builder().id(UUID.randomUUID()).owner(owner)
                .startDate(LocalDate.of(2024, 1, 1)).openingCash(new BigDecimal("5000")).build();

        when(propertyRepository.findByOwnerId(ownerId)).thenReturn(List.of(meuble, nu));
        when(paymentRepository.sumPaidRentAndChargesByPropertyAndPeriod(eq(meuble.getId()), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("1100"));

        Payment loyer = payment(meuble, PaymentType.RENT, "1000", LocalDateTime.of(2024, 3, 5, 10, 0));
        Payment provisions = payment(meuble, PaymentType.CHARGES, "100", LocalDateTime.of(2024, 3, 5, 10, 0));
        Payment loyerNu = payment(nu, PaymentType.RENT, "999", LocalDateTime.of(2024, 3, 6, 10, 0));
        when(paymentRepository.findByOwnerId(ownerId)).thenReturn(List.of(loyer, provisions, loyerNu));

        Expense entretien = expense(meuble, ExpenseCategory.MAINTENANCE, "300", LocalDate.of(2024, 4, 10));
        Expense chargeNu = expense(nu, ExpenseCategory.MAINTENANCE, "400", LocalDate.of(2024, 4, 11));
        Expense mobilier = expense(meuble, ExpenseCategory.FURNISHING, "500", LocalDate.of(2024, 4, 12));
        when(expenseRepository.findByOwnerId(ownerId)).thenReturn(List.of(entretien, chargeNu, mobilier));

        AmortizableAsset asset = AmortizableAsset.builder().id(UUID.randomUUID()).type(AssetType.MOBILIER)
                .label("Mobilier").base(new BigDecimal("6000")).durationYears(3)
                .startDate(LocalDate.of(2024, 1, 1)).build();
        when(assetRepository.findByActivityId(activity.getId())).thenReturn(List.of(asset));

        Loan loan = Loan.builder().id(UUID.randomUUID()).principal(new BigDecimal("10000"))
                .annualRate(new BigDecimal("2.000")).durationMonths(120)
                .startDate(LocalDate.of(2024, 1, 1)).label("Prêt").build();
        when(loanRepository.findByActivityId(activity.getId())).thenReturn(List.of(loan));
    }

    private Payment payment(Property p, PaymentType type, String amount, LocalDateTime paidAt) {
        Rental rental = Rental.builder().id(UUID.randomUUID()).property(p).build();
        return Payment.builder().id(UUID.randomUUID()).rental(rental).paymentType(type)
                .amount(new BigDecimal(amount)).status(PaymentStatus.PAID).paidAt(paidAt).build();
    }

    private Expense expense(Property p, ExpenseCategory cat, String amount, LocalDate date) {
        return Expense.builder().id(UUID.randomUUID()).property(p).category(cat)
                .amount(new BigDecimal(amount)).expenseDate(date).build();
    }

    @Test
    void ecrituresEquilibrees_etChronologiques() {
        List<JournalEntry> entries = generator.generate(activity, 2024);

        BigDecimal debit = entries.stream().map(JournalEntry::debit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credit = entries.stream().map(JournalEntry::credit).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(debit).isEqualByComparingTo(credit);

        for (int i = 1; i < entries.size(); i++) {
            assertThat(entries.get(i).date()).isAfterOrEqualTo(entries.get(i - 1).date());
        }
    }

    @Test
    void produits706et708_etPerimetreMeuble() {
        List<JournalEntry> entries = generator.generate(activity, 2024);

        assertThat(entries).anyMatch(e -> "706".equals(e.compteNum()) && e.credit().compareTo(new BigDecimal("1000")) == 0);
        assertThat(entries).anyMatch(e -> "708".equals(e.compteNum()) && e.credit().compareTo(new BigDecimal("100")) == 0);
        // Bien nu et dépense immobilisée exclus
        assertThat(entries).noneMatch(e -> e.debit().compareTo(new BigDecimal("999")) == 0
                || e.credit().compareTo(new BigDecimal("999")) == 0);
        assertThat(entries).noneMatch(e -> e.debit().compareTo(new BigDecimal("400")) == 0);
        assertThat(entries).noneMatch(e -> e.debit().compareTo(new BigDecimal("500")) == 0);
        // Emprunt initial au passif d'ouverture
        assertThat(entries).anyMatch(e -> "AN".equals(e.journalCode()) && "164".equals(e.compteNum())
                && e.credit().compareTo(new BigDecimal("10000")) == 0);
    }

    @Test
    void deuxiemeExercice_equilibre_avecANIssuDeLaVeille() {
        List<JournalEntry> entries = generator.generate(activity, 2025);

        BigDecimal debit = entries.stream().map(JournalEntry::debit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credit = entries.stream().map(JournalEntry::credit).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(debit).isEqualByComparingTo(credit);
    }

    @Test
    void fec_numeroteParJournal_etNomReglementaire() {
        List<JournalEntry> entries = generator.generate(activity, 2024);
        String fec = new String(fecExportService.generate(activity, 2024, entries), StandardCharsets.UTF_8);

        assertThat(fec).contains("|AN-1|").contains("|VE-1|").contains("|OD-1|");
        String[] lines = fec.split("\n");
        assertThat(lines[0]).startsWith("JournalCode|JournalLib|EcritureNum");

        assertThat(fecExportService.fileName(activity, 2024)).isEqualTo("FEC-2024.txt");
        activity.setSiret("12345678901234");
        assertThat(fecExportService.fileName(activity, 2024)).isEqualTo("123456789FEC20241231.txt");
    }
}
