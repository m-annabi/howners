package com.howners.gestion.service.rental;

import com.howners.gestion.domain.contract.Contract;
import com.howners.gestion.domain.payment.Payment;
import com.howners.gestion.domain.payment.PaymentStatus;
import com.howners.gestion.domain.property.Property;
import com.howners.gestion.domain.property.PropertyType;
import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.domain.rental.RentalStatus;
import com.howners.gestion.domain.rental.RentalType;
import com.howners.gestion.domain.user.Role;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.response.RentalResponse;
import com.howners.gestion.exception.BusinessException;
import com.howners.gestion.exception.ResourceNotFoundException;
import com.howners.gestion.repository.ContractRepository;
import com.howners.gestion.repository.PaymentRepository;
import com.howners.gestion.repository.PropertyRepository;
import com.howners.gestion.repository.RentalRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.security.UserPrincipal;
import com.howners.gestion.service.audit.AuditService;
import com.howners.gestion.service.email.EmailService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RentalServiceTest {

    @Mock private RentalRepository rentalRepository;
    @Mock private PropertyRepository propertyRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;
    @Mock private ContractRepository contractRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;

    @InjectMocks
    private RentalService rentalService;

    private UUID ownerId;
    private User owner;
    private Property property;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();

        owner = User.builder()
                .id(ownerId)
                .email("owner@test.com")
                .firstName("Marie")
                .lastName("Martin")
                .passwordHash("hash")
                .role(Role.OWNER)
                .enabled(true)
                .build();

        property = Property.builder()
                .id(UUID.randomUUID())
                .owner(owner)
                .name("Apt Test")
                .propertyType(PropertyType.APARTMENT)
                .city("Paris")
                .postalCode("75001")
                .country("FR")
                .createdAt(LocalDateTime.now())
                .build();

        setCurrentUser(owner);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setCurrentUser(User user) {
        UserPrincipal principal = UserPrincipal.create(user);
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private Rental buildRental(RentalStatus status) {
        return Rental.builder()
                .id(UUID.randomUUID())
                .property(property)
                .tenant(User.builder().id(UUID.randomUUID()).email("tenant@test.com")
                        .passwordHash("hash").role(Role.TENANT).enabled(true).build())
                .rentalType(RentalType.LONG_TERM)
                .status(status)
                .startDate(LocalDate.now())
                .monthlyRent(new BigDecimal("800.00"))
                .currency("EUR")
                .createdAt(LocalDateTime.now())
                .build();
    }

    // --- findAllByCurrentUser ---

    @Test
    void findAllByCurrentUser_shouldReturnList() {
        Rental r = buildRental(RentalStatus.ACTIVE);
        when(rentalRepository.findByOwnerId(ownerId)).thenReturn(List.of(r));

        List<RentalResponse> result = rentalService.findAllByCurrentUser();

        assertThat(result).hasSize(1);
        verify(rentalRepository).findByOwnerId(ownerId);
    }

    // --- findById ---

    @Test
    void findById_asOwner_shouldSucceed() {
        Rental r = buildRental(RentalStatus.ACTIVE);
        when(rentalRepository.findById(r.getId())).thenReturn(Optional.of(r));

        RentalResponse result = rentalService.findById(r.getId());

        assertThat(result).isNotNull();
    }

    @Test
    void findById_notFound_shouldThrow() {
        UUID id = UUID.randomUUID();
        when(rentalRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rentalService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findById_notOwnerOrTenant_shouldThrow() {
        User otherOwner = User.builder()
                .id(UUID.randomUUID())
                .email("other@test.com")
                .passwordHash("hash")
                .role(Role.OWNER)
                .enabled(true)
                .build();

        Property otherProperty = Property.builder()
                .id(UUID.randomUUID())
                .owner(otherOwner)
                .name("Other Apt")
                .propertyType(PropertyType.APARTMENT)
                .city("Paris")
                .postalCode("75001")
                .country("FR")
                .createdAt(LocalDateTime.now())
                .build();

        Rental r = Rental.builder()
                .id(UUID.randomUUID())
                .property(otherProperty)
                .tenant(User.builder().id(UUID.randomUUID()).email("t@test.com")
                        .passwordHash("hash").role(Role.TENANT).enabled(true).build())
                .rentalType(RentalType.LONG_TERM)
                .status(RentalStatus.ACTIVE)
                .startDate(LocalDate.now())
                .monthlyRent(new BigDecimal("800.00"))
                .createdAt(LocalDateTime.now())
                .build();

        when(rentalRepository.findById(r.getId())).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> rentalService.findById(r.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("permission");
    }

    // --- delete ---

    @Test
    void delete_noContractsNoPayments_shouldSucceed() {
        Rental r = buildRental(RentalStatus.TERMINATED);
        when(rentalRepository.findById(r.getId())).thenReturn(Optional.of(r));
        when(contractRepository.findByRentalId(r.getId())).thenReturn(Collections.emptyList());
        when(paymentRepository.findByRentalId(r.getId())).thenReturn(Collections.emptyList());

        rentalService.delete(r.getId());

        verify(rentalRepository).delete(r);
    }

    @Test
    void delete_withContracts_shouldThrow() {
        Rental r = buildRental(RentalStatus.ACTIVE);
        when(rentalRepository.findById(r.getId())).thenReturn(Optional.of(r));
        when(contractRepository.findByRentalId(r.getId())).thenReturn(List.of(mock(Contract.class)));

        assertThatThrownBy(() -> rentalService.delete(r.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("contract");

        verify(rentalRepository, never()).delete(any());
    }

    @Test
    void delete_withPendingPayments_shouldThrow() {
        Rental r = buildRental(RentalStatus.ACTIVE);
        Payment pendingPayment = Payment.builder()
                .id(UUID.randomUUID())
                .rental(r)
                .status(PaymentStatus.PENDING)
                .amount(new BigDecimal("800.00"))
                .build();

        when(rentalRepository.findById(r.getId())).thenReturn(Optional.of(r));
        when(contractRepository.findByRentalId(r.getId())).thenReturn(Collections.emptyList());
        when(paymentRepository.findByRentalId(r.getId())).thenReturn(List.of(pendingPayment));

        assertThatThrownBy(() -> rentalService.delete(r.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("payment");

        verify(rentalRepository, never()).delete(any());
    }

    @Test
    void delete_withLatePayments_shouldThrow() {
        Rental r = buildRental(RentalStatus.ACTIVE);
        Payment latePayment = Payment.builder()
                .id(UUID.randomUUID())
                .rental(r)
                .status(PaymentStatus.LATE)
                .amount(new BigDecimal("800.00"))
                .build();

        when(rentalRepository.findById(r.getId())).thenReturn(Optional.of(r));
        when(contractRepository.findByRentalId(r.getId())).thenReturn(Collections.emptyList());
        when(paymentRepository.findByRentalId(r.getId())).thenReturn(List.of(latePayment));

        assertThatThrownBy(() -> rentalService.delete(r.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("payment");

        verify(rentalRepository, never()).delete(any());
    }

    @Test
    void delete_withPaidPayments_shouldSucceed() {
        Rental r = buildRental(RentalStatus.TERMINATED);
        Payment paidPayment = Payment.builder()
                .id(UUID.randomUUID())
                .rental(r)
                .status(PaymentStatus.PAID)
                .amount(new BigDecimal("800.00"))
                .build();

        when(rentalRepository.findById(r.getId())).thenReturn(Optional.of(r));
        when(contractRepository.findByRentalId(r.getId())).thenReturn(Collections.emptyList());
        when(paymentRepository.findByRentalId(r.getId())).thenReturn(List.of(paidPayment));

        rentalService.delete(r.getId());

        verify(rentalRepository).delete(r);
    }
}
