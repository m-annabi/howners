package com.howners.gestion.service.rental;

import com.howners.gestion.domain.expense.Expense;
import com.howners.gestion.domain.expense.ExpenseCategory;
import com.howners.gestion.domain.payment.Payment;
import com.howners.gestion.domain.payment.PaymentStatus;
import com.howners.gestion.domain.payment.PaymentType;
import com.howners.gestion.domain.property.Property;
import com.howners.gestion.domain.rental.ChargeRegularisation;
import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.domain.rental.RentalStatus;
import com.howners.gestion.domain.user.Role;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.rental.RegularisationResponse;
import com.howners.gestion.exception.BusinessException;
import com.howners.gestion.repository.ChargeRegularisationRepository;
import com.howners.gestion.repository.DocumentRepository;
import com.howners.gestion.repository.ExpenseRepository;
import com.howners.gestion.repository.PaymentRepository;
import com.howners.gestion.repository.RentalRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.security.UserPrincipal;
import com.howners.gestion.service.contract.PdfService;
import com.howners.gestion.service.email.EmailService;
import com.howners.gestion.service.notification.NotificationService;
import com.howners.gestion.service.storage.StorageService;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegularisationChargesServiceTest {

    @Mock ChargeRegularisationRepository regularisationRepository;
    @Mock RentalRepository rentalRepository;
    @Mock PaymentRepository paymentRepository;
    @Mock ExpenseRepository expenseRepository;
    @Mock UserRepository userRepository;
    @Mock DocumentRepository documentRepository;
    @Mock PdfService pdfService;
    @Mock StorageService storageService;
    @Mock EmailService emailService;
    @Mock NotificationService notificationService;

    @InjectMocks RegularisationChargesService regularisationService;

    UUID ownerId;
    UUID rentalId;
    UUID propertyId;
    Rental rental;

    @BeforeEach
    void setup() {
        ownerId = UUID.randomUUID();
        rentalId = UUID.randomUUID();
        propertyId = UUID.randomUUID();
        User owner = User.builder().id(ownerId).email("o@t.fr").firstName("O").lastName("W")
                .role(Role.OWNER).build();
        Property property = new Property();
        property.setId(propertyId);
        property.setOwner(owner);
        property.setName("Bien Test");
        rental = Rental.builder()
                .id(rentalId)
                .property(property)
                .status(RentalStatus.ACTIVE)
                .startDate(LocalDate.of(2024, 1, 1))
                .monthlyRent(new BigDecimal("800.00"))
                .charges(new BigDecimal("50.00"))
                .build();

        UserPrincipal principal = new UserPrincipal(ownerId, "o@t.fr", "x", "OWNER", true);
        Authentication auth = org.springframework.security.authentication.UsernamePasswordAuthenticationToken
                .authenticated(principal, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        lenient().when(userRepository.findById(ownerId)).thenReturn(Optional.of(User.builder()
                .id(ownerId).role(Role.OWNER).build()));
        lenient().when(rentalRepository.findById(rentalId)).thenReturn(Optional.of(rental));
        lenient().when(regularisationRepository.save(any(ChargeRegularisation.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    private Expense expense(ExpenseCategory cat, String montant, LocalDate date) {
        Expense e = Expense.builder()
                .category(cat)
                .amount(new BigDecimal(montant))
                .expenseDate(date)
                .build();
        return e;
    }

    @Test
    void calculeAvecPaiementsChargesDistincts() {
        Payment charge = Payment.builder()
                .paymentType(PaymentType.CHARGES)
                .status(PaymentStatus.PAID)
                .amount(new BigDecimal("600.00"))
                .dueDate(LocalDate.of(2025, 6, 1))
                .build();
        when(regularisationRepository.findByRentalIdAndAnnee(rentalId, 2025)).thenReturn(Optional.empty());
        when(paymentRepository.findByRentalId(rentalId)).thenReturn(List.of(charge));
        when(expenseRepository.findByPropertyId(propertyId)).thenReturn(List.of(
                expense(ExpenseCategory.UTILITIES, "300.00", LocalDate.of(2025, 3, 10)),
                expense(ExpenseCategory.CONDO_FEES, "450.00", LocalDate.of(2025, 7, 5)),
                expense(ExpenseCategory.RENOVATION, "5000.00", LocalDate.of(2025, 5, 1)) // non récupérable
        ));

        RegularisationResponse response = regularisationService.calculer(rentalId, 2025);

        assertThat(response.provisionsEncaissees()).isEqualByComparingTo("600.00");
        assertThat(response.chargesReelles()).isEqualByComparingTo("750.00");
        assertThat(response.solde()).isEqualByComparingTo("150.00");
    }

    @Test
    void fallbackSurForfaitMensuelSansPaiementsCharges() {
        when(regularisationRepository.findByRentalIdAndAnnee(rentalId, 2025)).thenReturn(Optional.empty());
        when(paymentRepository.findByRentalId(rentalId)).thenReturn(List.of());
        when(expenseRepository.findByPropertyId(propertyId)).thenReturn(List.of(
                expense(ExpenseCategory.UTILITIES, "400.00", LocalDate.of(2025, 4, 1))
        ));

        RegularisationResponse response = regularisationService.calculer(rentalId, 2025);

        // 50 € × 12 mois = 600 € de provisions ; solde = 400 − 600 = −200 (à restituer)
        assertThat(response.provisionsEncaissees()).isEqualByComparingTo("600.00");
        assertThat(response.solde()).isEqualByComparingTo("-200.00");
    }

    @Test
    void refuseLeDoublonParAnnee() {
        when(regularisationRepository.findByRentalIdAndAnnee(rentalId, 2025))
                .thenReturn(Optional.of(ChargeRegularisation.builder().rental(rental).annee(2025).build()));

        assertThatThrownBy(() -> regularisationService.calculer(rentalId, 2025))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("existe déjà");
    }

    @Test
    void refuseSiLocationNonOccupeeSurLAnnee() {
        rental.setStartDate(LocalDate.of(2026, 3, 1));
        when(regularisationRepository.findByRentalIdAndAnnee(rentalId, 2024)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> regularisationService.calculer(rentalId, 2024))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("occupée");
    }
}
