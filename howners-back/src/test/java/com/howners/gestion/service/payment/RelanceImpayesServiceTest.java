package com.howners.gestion.service.payment;

import com.howners.gestion.domain.document.Document;
import com.howners.gestion.domain.payment.Payment;
import com.howners.gestion.domain.payment.PaymentStatus;
import com.howners.gestion.domain.payment.PaymentType;
import com.howners.gestion.domain.property.Property;
import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.domain.user.Role;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.exception.BadRequestException;
import com.howners.gestion.repository.DocumentRepository;
import com.howners.gestion.repository.PaymentRepository;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelanceImpayesServiceTest {

    @Mock PaymentRepository paymentRepository;
    @Mock UserRepository userRepository;
    @Mock DocumentRepository documentRepository;
    @Mock PdfService pdfService;
    @Mock StorageService storageService;
    @Mock EmailService emailService;
    @Mock NotificationService notificationService;

    @InjectMocks RelanceImpayesService relanceService;

    UUID ownerId;
    Payment payment;

    @BeforeEach
    void setup() {
        ownerId = UUID.randomUUID();
        User owner = User.builder().id(ownerId).email("o@t.fr").firstName("O").lastName("W").role(Role.OWNER).build();
        User tenant = User.builder().id(UUID.randomUUID()).email("t@t.fr").firstName("T").lastName("E").role(Role.TENANT).build();
        Property property = new Property();
        property.setOwner(owner);
        property.setName("Bien Test");
        Rental rental = Rental.builder().id(UUID.randomUUID()).property(property).tenant(tenant).build();
        payment = Payment.builder()
                .id(UUID.randomUUID())
                .rental(rental)
                .payer(tenant)
                .paymentType(PaymentType.RENT)
                .amount(new BigDecimal("800.00"))
                .status(PaymentStatus.LATE)
                .dueDate(LocalDate.now().minusDays(10))
                .relanceNiveau(0)
                .build();

        UserPrincipal principal = new UserPrincipal(ownerId, "o@t.fr", "x", "OWNER", true);
        Authentication auth = org.springframework.security.authentication.UsernamePasswordAuthenticationToken
                .authenticated(principal, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        lenient().when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        lenient().when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    @Test
    void jobEnvoieRelanceNiveau1ApresJ5() {
        when(paymentRepository.findLatePaymentsForRelance(any(LocalDate.class), anyInt()))
                .thenAnswer(inv -> (int) inv.getArgument(1) == 1 ? List.of(payment) : List.of());

        relanceService.traiterRelances();

        assertThat(payment.getRelanceNiveau()).isEqualTo(1);
        assertThat(payment.getDerniereRelanceLe()).isNotNull();
        verify(emailService).sendNotificationEmail(any());
        verify(paymentRepository).save(payment);
    }

    @Test
    void jobEnvoieMiseEnDemeureNiveau2ApresJ15() throws Exception {
        payment.setRelanceNiveau(1);
        when(paymentRepository.findLatePaymentsForRelance(any(LocalDate.class), anyInt()))
                .thenAnswer(inv -> (int) inv.getArgument(1) == 2 ? List.of(payment) : List.of());
        when(pdfService.generatePdf(anyString(), any())).thenReturn(new byte[]{1, 2, 3});
        when(pdfService.calculateHash(any())).thenReturn("hash");
        when(storageService.uploadFile(any(), anyString(), anyString())).thenReturn("filekey");
        when(storageService.generatePresignedUrl("filekey")).thenReturn("http://url");
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        relanceService.traiterRelances();

        assertThat(payment.getRelanceNiveau()).isEqualTo(2);
        verify(documentRepository).save(any(Document.class));
        verify(emailService).sendNotificationEmail(any());
    }

    @Test
    void relanceManuelleRefuseAuNiveau2() {
        payment.setRelanceNiveau(2);
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> relanceService.relancerManuellement(payment.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("mise en demeure");
        verify(emailService, never()).sendNotificationEmail(any());
    }

    @Test
    void relanceManuelleRefuseSurPaiementPaye() {
        payment.setStatus(PaymentStatus.PAID);
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> relanceService.relancerManuellement(payment.getId()))
                .isInstanceOf(BadRequestException.class);
    }
}
