package com.howners.gestion.service.payment;

import com.howners.gestion.domain.payment.Payment;
import com.howners.gestion.domain.payment.PaymentStatus;
import com.howners.gestion.domain.payment.PaymentType;
import com.howners.gestion.domain.property.Property;
import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.domain.user.Role;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.payment.CreatePaymentRequest;
import com.howners.gestion.dto.payment.PaymentResponse;
import com.howners.gestion.exception.BadRequestException;
import com.howners.gestion.exception.ForbiddenException;
import com.howners.gestion.exception.ResourceNotFoundException;
import com.howners.gestion.repository.PaymentRepository;
import com.howners.gestion.repository.RentalRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.security.UserPrincipal;
import com.howners.gestion.service.audit.AuditService;
import com.howners.gestion.service.receipt.ReceiptService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private RentalRepository rentalRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;
    @Mock private ReceiptService receiptService;

    @InjectMocks
    private PaymentService paymentService;

    private UUID ownerId;
    private UUID tenantId;
    private User owner;
    private User tenant;
    private Property property;
    private Rental rental;
    private Payment pendingPayment;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        tenantId = UUID.randomUUID();

        owner = User.builder()
                .id(ownerId)
                .email("owner@test.com")
                .passwordHash("hash")
                .firstName("Jean")
                .lastName("Propriétaire")
                .role(Role.OWNER)
                .enabled(true)
                .build();

        tenant = User.builder()
                .id(tenantId)
                .email("tenant@test.com")
                .passwordHash("hash")
                .firstName("Marie")
                .lastName("Locataire")
                .role(Role.TENANT)
                .enabled(true)
                .build();

        property = Property.builder()
                .id(UUID.randomUUID())
                .owner(owner)
                .name("Appartement Paris")
                .build();

        rental = Rental.builder()
                .id(UUID.randomUUID())
                .property(property)
                .tenant(tenant)
                .monthlyRent(BigDecimal.valueOf(850))
                .build();

        pendingPayment = Payment.builder()
                .id(UUID.randomUUID())
                .rental(rental)
                .payer(tenant)
                .paymentType(PaymentType.RENT)
                .amount(BigDecimal.valueOf(850))
                .currency("EUR")
                .status(PaymentStatus.PENDING)
                .dueDate(LocalDate.now().plusDays(5))
                .createdAt(LocalDateTime.now())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // --- findByCurrentUser ---

    @Test
    void findByCurrentUser_asOwner_returnsOwnerPayments() {
        setCurrentUser(owner);
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(paymentRepository.findByOwnerId(ownerId)).thenReturn(List.of(pendingPayment));

        List<PaymentResponse> result = paymentService.findByCurrentUser();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).amount()).isEqualTo(BigDecimal.valueOf(850));
    }

    @Test
    void findByCurrentUser_asTenant_returnsTenantPayments() {
        setCurrentUser(tenant);
        when(userRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(paymentRepository.findByPayerId(tenantId)).thenReturn(List.of(pendingPayment));

        List<PaymentResponse> result = paymentService.findByCurrentUser();

        assertThat(result).hasSize(1);
    }

    @Test
    void findByCurrentUser_asAdmin_returnsAllPayments() {
        User admin = User.builder()
                .id(UUID.randomUUID())
                .email("admin@test.com")
                .passwordHash("hash")
                .role(Role.ADMIN)
                .enabled(true)
                .build();
        setCurrentUser(admin);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(paymentRepository.findAll()).thenReturn(List.of(pendingPayment));

        List<PaymentResponse> result = paymentService.findByCurrentUser();

        assertThat(result).hasSize(1);
        verify(paymentRepository).findAll();
    }

    // --- createPayment ---

    @Test
    void createPayment_asOwner_createsPayment() {
        setCurrentUser(owner);
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(rentalRepository.findById(rental.getId())).thenReturn(Optional.of(rental));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> {
            Payment p = i.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        CreatePaymentRequest request = new CreatePaymentRequest(
                rental.getId(), PaymentType.RENT, BigDecimal.valueOf(850),
                "EUR", LocalDate.now().plusMonths(1), "transfer"
        );

        PaymentResponse result = paymentService.createPayment(request);

        assertThat(result).isNotNull();
        assertThat(result.amount()).isEqualTo(BigDecimal.valueOf(850));
        assertThat(result.status()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void createPayment_asUnauthorizedUser_throwsForbidden() {
        User other = User.builder()
                .id(UUID.randomUUID())
                .email("other@test.com")
                .passwordHash("hash")
                .role(Role.OWNER)
                .enabled(true)
                .build();
        setCurrentUser(other);
        when(userRepository.findById(other.getId())).thenReturn(Optional.of(other));
        when(rentalRepository.findById(rental.getId())).thenReturn(Optional.of(rental));

        CreatePaymentRequest request = new CreatePaymentRequest(
                rental.getId(), PaymentType.RENT, BigDecimal.valueOf(850),
                "EUR", LocalDate.now().plusMonths(1), "transfer"
        );

        assertThatThrownBy(() -> paymentService.createPayment(request))
                .isInstanceOf(ForbiddenException.class);
    }

    // --- confirmPayment ---

    @Test
    void confirmPayment_pendingPayment_marksPaid() {
        setCurrentUser(owner);
        when(paymentRepository.findById(pendingPayment.getId())).thenReturn(Optional.of(pendingPayment));
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        PaymentResponse result = paymentService.confirmPayment(pendingPayment.getId());

        assertThat(result.status()).isEqualTo(PaymentStatus.PAID);
        assertThat(pendingPayment.getPaidAt()).isNotNull();
    }

    @Test
    void confirmPayment_alreadyPaid_throwsBadRequest() {
        setCurrentUser(owner);
        pendingPayment.setStatus(PaymentStatus.PAID);
        when(paymentRepository.findById(pendingPayment.getId())).thenReturn(Optional.of(pendingPayment));
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> paymentService.confirmPayment(pendingPayment.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot be confirmed");
    }

    @Test
    void confirmPayment_latePayment_marksPaid() {
        setCurrentUser(owner);
        pendingPayment.setStatus(PaymentStatus.LATE);
        when(paymentRepository.findById(pendingPayment.getId())).thenReturn(Optional.of(pendingPayment));
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        PaymentResponse result = paymentService.confirmPayment(pendingPayment.getId());

        assertThat(result.status()).isEqualTo(PaymentStatus.PAID);
    }

    // --- findById ---

    @Test
    void findById_notFound_throwsResourceNotFound() {
        setCurrentUser(owner);
        UUID fakeId = UUID.randomUUID();
        when(paymentRepository.findById(fakeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.findById(fakeId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findById_unauthorizedUser_throwsForbidden() {
        User other = User.builder()
                .id(UUID.randomUUID())
                .email("other@test.com")
                .passwordHash("hash")
                .role(Role.OWNER)
                .enabled(true)
                .build();
        setCurrentUser(other);
        when(paymentRepository.findById(pendingPayment.getId())).thenReturn(Optional.of(pendingPayment));
        when(userRepository.findById(other.getId())).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> paymentService.findById(pendingPayment.getId()))
                .isInstanceOf(ForbiddenException.class);
    }

    // --- markOverduePayments ---

    @Test
    void markOverduePayments_marksOverdueAsLate() {
        Payment overdue = Payment.builder()
                .id(UUID.randomUUID())
                .rental(rental)
                .payer(tenant)
                .paymentType(PaymentType.RENT)
                .amount(BigDecimal.valueOf(850))
                .status(PaymentStatus.PENDING)
                .dueDate(LocalDate.now().minusDays(5))
                .build();

        when(paymentRepository.findOverduePayments(LocalDate.now())).thenReturn(List.of(overdue));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        paymentService.markOverduePayments();

        assertThat(overdue.getStatus()).isEqualTo(PaymentStatus.LATE);
        verify(paymentRepository).save(overdue);
    }

    private void setCurrentUser(User user) {
        UserPrincipal principal = UserPrincipal.create(user);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
