package com.howners.gestion.service.accounting;

import com.howners.gestion.domain.accounting.AssetType;
import com.howners.gestion.domain.accounting.FiscalActivity;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.accounting.AccountingDtos.CreateAssetRequest;
import com.howners.gestion.dto.accounting.AccountingDtos.CreateLoanRequest;
import com.howners.gestion.exception.BadRequestException;
import com.howners.gestion.repository.AmortizableAssetRepository;
import com.howners.gestion.repository.ExpenseRepository;
import com.howners.gestion.repository.FiscalActivityRepository;
import com.howners.gestion.repository.LoanRepository;
import com.howners.gestion.repository.PropertyRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.security.UserPrincipal;
import com.howners.gestion.service.subscription.FeatureGateService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccountingServiceTest {

    @Mock FiscalActivityRepository activityRepository;
    @Mock AmortizableAssetRepository assetRepository;
    @Mock LoanRepository loanRepository;
    @Mock LoanScheduleService loanScheduleService;
    @Mock PropertyRepository propertyRepository;
    @Mock ExpenseRepository expenseRepository;
    @Mock UserRepository userRepository;
    @Mock FiscalEngineResolver engineResolver;
    @Mock FeatureGateService featureGateService;

    @InjectMocks AccountingService accountingService;

    private UUID ownerId;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        User owner = User.builder().id(ownerId).email("o@t.fr").firstName("O").lastName("W").build();
        FiscalActivity activity = FiscalActivity.builder().id(UUID.randomUUID()).owner(owner)
                .startDate(LocalDate.of(2024, 1, 1)).build();

        UserPrincipal principal = new UserPrincipal(ownerId, "o@t.fr", "x", "OWNER", true);
        Authentication auth = org.springframework.security.authentication.UsernamePasswordAuthenticationToken
                .authenticated(principal, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(featureGateService.hasFeature(ownerId, "tax_export")).thenReturn(true);
        when(activityRepository.findByOwnerId(ownerId)).thenReturn(Optional.of(activity));
    }

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    private CreateLoanRequest loan(String principal, String rate, Integer months) {
        return new CreateLoanRequest("Prêt", new BigDecimal(principal), new BigDecimal(rate),
                months, LocalDate.of(2024, 1, 1), null, null);
    }

    @Test
    void addLoan_dureeNulle_rejetee() {
        assertThatThrownBy(() -> accountingService.addLoan(loan("100000", "2.0", 0)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("durée");
    }

    @Test
    void addLoan_capitalNegatif_rejete() {
        assertThatThrownBy(() -> accountingService.addLoan(loan("-5000", "2.0", 120)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("capital");
    }

    @Test
    void addLoan_tauxNegatif_rejete() {
        assertThatThrownBy(() -> accountingService.addLoan(loan("100000", "-1.0", 120)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("taux");
    }

    @Test
    void addLoan_assuranceNegative_rejetee() {
        CreateLoanRequest req = new CreateLoanRequest("Prêt", new BigDecimal("100000"),
                new BigDecimal("2.0"), 120, LocalDate.of(2024, 1, 1), new BigDecimal("-10"), null);
        assertThatThrownBy(() -> accountingService.addLoan(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("assurance");
    }

    @Test
    void addAsset_baseNegative_rejetee() {
        CreateAssetRequest req = new CreateAssetRequest(AssetType.MOBILIER, "Mob",
                new BigDecimal("-100"), LocalDate.of(2024, 1, 1), null, null);
        assertThatThrownBy(() -> accountingService.addAsset(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("positive");
    }
}
