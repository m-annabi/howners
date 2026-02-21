package com.howners.gestion.service.receipt;

import com.howners.gestion.domain.document.Document;
import com.howners.gestion.domain.document.DocumentType;
import com.howners.gestion.domain.payment.Payment;
import com.howners.gestion.domain.payment.PaymentStatus;
import com.howners.gestion.domain.property.Property;
import com.howners.gestion.domain.receipt.Receipt;
import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.domain.user.Role;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.email.ReceiptEmailData;
import com.howners.gestion.dto.receipt.ReceiptResponse;
import com.howners.gestion.exception.BadRequestException;
import com.howners.gestion.exception.ForbiddenException;
import com.howners.gestion.repository.DocumentRepository;
import com.howners.gestion.repository.PaymentRepository;
import com.howners.gestion.repository.ReceiptRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.security.UserPrincipal;
import com.howners.gestion.service.contract.PdfService;
import com.howners.gestion.service.email.EmailService;
import com.howners.gestion.service.storage.StorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReceiptServiceTest {

    @Mock private ReceiptRepository receiptRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private DocumentRepository documentRepository;
    @Mock private UserRepository userRepository;
    @Mock private PdfService pdfService;
    @Mock private StorageService storageService;
    @Mock private EmailService emailService;

    @InjectMocks
    private ReceiptService receiptService;

    private UUID ownerId;
    private UUID tenantId;
    private UUID adminId;
    private User ownerUser;
    private User tenantUser;
    private User adminUser;
    private Property property;
    private Rental rental;
    private Payment paidPayment;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(receiptService, "frontendUrl", "http://localhost:4200");

        ownerId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
        adminId = UUID.randomUUID();

        ownerUser = User.builder()
                .id(ownerId)
                .email("owner@test.com")
                .firstName("Marie")
                .lastName("Martin")
                .passwordHash("hash")
                .role(Role.OWNER)
                .enabled(true)
                .build();

        tenantUser = User.builder()
                .id(tenantId)
                .email("tenant@test.com")
                .firstName("Jean")
                .lastName("Dupont")
                .passwordHash("hash")
                .role(Role.TENANT)
                .enabled(true)
                .build();

        adminUser = User.builder()
                .id(adminId)
                .email("admin@test.com")
                .firstName("Admin")
                .lastName("User")
                .passwordHash("hash")
                .role(Role.ADMIN)
                .enabled(true)
                .build();

        property = Property.builder()
                .id(UUID.randomUUID())
                .owner(ownerUser)
                .name("Appartement Paris")
                .addressLine1("10 rue de la Paix")
                .postalCode("75001")
                .city("Paris")
                .build();

        rental = Rental.builder()
                .id(UUID.randomUUID())
                .property(property)
                .tenant(tenantUser)
                .monthlyRent(new BigDecimal("850.00"))
                .charges(new BigDecimal("50.00"))
                .currency("EUR")
                .build();

        paidPayment = Payment.builder()
                .id(UUID.randomUUID())
                .rental(rental)
                .amount(new BigDecimal("900.00"))
                .currency("EUR")
                .status(PaymentStatus.PAID)
                .dueDate(LocalDate.of(2026, 1, 15))
                .paidAt(LocalDateTime.of(2026, 1, 15, 10, 0))
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setCurrentUser(UUID id, String email, String role) {
        UserPrincipal principal = UserPrincipal.create(
                User.builder()
                        .id(id)
                        .email(email)
                        .passwordHash("hash")
                        .role(Role.valueOf(role))
                        .enabled(true)
                        .build()
        );
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // --- generateReceipt ---

    @Test
    void generateReceipt_withPaidPayment_shouldCreateReceiptAndDocument() throws IOException {
        setCurrentUser(ownerId, "owner@test.com", "OWNER");
        UUID paymentId = paidPayment.getId();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(paidPayment));
        when(receiptRepository.findByPaymentId(paymentId)).thenReturn(Optional.empty());
        when(pdfService.generatePdf(anyString(), any())).thenReturn("pdf-content".getBytes());
        when(pdfService.calculateHash(any())).thenReturn("hash123");
        when(storageService.uploadFile(any(), anyString(), anyString())).thenReturn("file-key-123");
        when(storageService.generatePresignedUrl("file-key-123")).thenReturn("https://presigned-url");
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> {
            Document doc = inv.getArgument(0);
            doc.setId(UUID.randomUUID());
            return doc;
        });
        when(receiptRepository.save(any(Receipt.class))).thenAnswer(inv -> {
            Receipt r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        ReceiptResponse response = receiptService.generateReceipt(paymentId);

        assertThat(response).isNotNull();
        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("900.00"));
        assertThat(response.periodStart()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(response.periodEnd()).isEqualTo(LocalDate.of(2026, 1, 31));
        assertThat(response.documentUrl()).isEqualTo("https://presigned-url");

        verify(documentRepository).save(any(Document.class));
        verify(receiptRepository).save(any(Receipt.class));
        verify(storageService).uploadFile(any(), anyString(), eq("application/pdf"));
    }

    @Test
    void generateReceipt_withUnpaidPayment_shouldThrow() {
        UUID paymentId = UUID.randomUUID();
        Payment unpaidPayment = Payment.builder()
                .id(paymentId)
                .rental(rental)
                .amount(new BigDecimal("900.00"))
                .status(PaymentStatus.PENDING)
                .build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(unpaidPayment));

        assertThatThrownBy(() -> receiptService.generateReceipt(paymentId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("payment is not PAID");
    }

    @Test
    void generateReceipt_alreadyExists_shouldThrow() {
        UUID paymentId = paidPayment.getId();
        Receipt existing = Receipt.builder().id(UUID.randomUUID()).build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(paidPayment));
        when(receiptRepository.findByPaymentId(paymentId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> receiptService.generateReceipt(paymentId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Receipt already exists");
    }

    // --- findByCurrentUser ---

    @Test
    void findByCurrentUser_asOwner_shouldReturnOwnerReceipts() {
        setCurrentUser(ownerId, "owner@test.com", "OWNER");

        Document doc = Document.builder().id(UUID.randomUUID()).fileKey("key1").build();
        Receipt receipt = Receipt.builder()
                .id(UUID.randomUUID())
                .rental(rental)
                .payment(paidPayment)
                .receiptNumber("QTL-2026-AAAAAAAA")
                .periodStart(LocalDate.of(2026, 1, 1))
                .periodEnd(LocalDate.of(2026, 1, 31))
                .amount(new BigDecimal("900.00"))
                .currency("EUR")
                .document(doc)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(ownerUser));
        when(receiptRepository.findByOwnerId(ownerId)).thenReturn(List.of(receipt));
        when(storageService.generatePresignedUrl("key1")).thenReturn("https://url");

        List<ReceiptResponse> result = receiptService.findByCurrentUser();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).receiptNumber()).isEqualTo("QTL-2026-AAAAAAAA");
        verify(receiptRepository).findByOwnerId(ownerId);
        verify(receiptRepository, never()).findByTenantId(any());
    }

    @Test
    void findByCurrentUser_asTenant_shouldReturnTenantReceipts() {
        setCurrentUser(tenantId, "tenant@test.com", "TENANT");

        Document doc = Document.builder().id(UUID.randomUUID()).fileKey("key1").build();
        Receipt receipt = Receipt.builder()
                .id(UUID.randomUUID())
                .rental(rental)
                .payment(paidPayment)
                .receiptNumber("QTL-2026-BBBBBBBB")
                .periodStart(LocalDate.of(2026, 1, 1))
                .periodEnd(LocalDate.of(2026, 1, 31))
                .amount(new BigDecimal("900.00"))
                .currency("EUR")
                .document(doc)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(tenantId)).thenReturn(Optional.of(tenantUser));
        when(receiptRepository.findByTenantId(tenantId)).thenReturn(List.of(receipt));
        when(storageService.generatePresignedUrl("key1")).thenReturn("https://url");

        List<ReceiptResponse> result = receiptService.findByCurrentUser();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).receiptNumber()).isEqualTo("QTL-2026-BBBBBBBB");
        verify(receiptRepository).findByTenantId(tenantId);
        verify(receiptRepository, never()).findByOwnerId(any());
    }

    @Test
    void findByCurrentUser_asAdmin_shouldReturnAll() {
        setCurrentUser(adminId, "admin@test.com", "ADMIN");

        Document doc = Document.builder().id(UUID.randomUUID()).fileKey("key1").build();
        Receipt receipt = Receipt.builder()
                .id(UUID.randomUUID())
                .rental(rental)
                .payment(paidPayment)
                .receiptNumber("QTL-2026-CCCCCCCC")
                .periodStart(LocalDate.of(2026, 1, 1))
                .periodEnd(LocalDate.of(2026, 1, 31))
                .amount(new BigDecimal("900.00"))
                .currency("EUR")
                .document(doc)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
        when(receiptRepository.findAll()).thenReturn(List.of(receipt));
        when(storageService.generatePresignedUrl("key1")).thenReturn("https://url");

        List<ReceiptResponse> result = receiptService.findByCurrentUser();

        assertThat(result).hasSize(1);
        verify(receiptRepository).findAll();
        verify(receiptRepository, never()).findByOwnerId(any());
        verify(receiptRepository, never()).findByTenantId(any());
    }

    // --- findById ---

    @Test
    void findById_asOwner_shouldWork() {
        setCurrentUser(ownerId, "owner@test.com", "OWNER");

        Document doc = Document.builder().id(UUID.randomUUID()).fileKey("key1").build();
        UUID receiptId = UUID.randomUUID();
        Receipt receipt = Receipt.builder()
                .id(receiptId)
                .rental(rental)
                .payment(paidPayment)
                .receiptNumber("QTL-2026-DDDDDDDD")
                .periodStart(LocalDate.of(2026, 1, 1))
                .periodEnd(LocalDate.of(2026, 1, 31))
                .amount(new BigDecimal("900.00"))
                .currency("EUR")
                .document(doc)
                .createdAt(LocalDateTime.now())
                .build();

        when(receiptRepository.findById(receiptId)).thenReturn(Optional.of(receipt));
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(ownerUser));
        when(storageService.generatePresignedUrl("key1")).thenReturn("https://url");

        ReceiptResponse result = receiptService.findById(receiptId);

        assertThat(result).isNotNull();
        assertThat(result.receiptNumber()).isEqualTo("QTL-2026-DDDDDDDD");
    }

    @Test
    void findById_asTenant_shouldWork() {
        setCurrentUser(tenantId, "tenant@test.com", "TENANT");

        Document doc = Document.builder().id(UUID.randomUUID()).fileKey("key1").build();
        UUID receiptId = UUID.randomUUID();
        Receipt receipt = Receipt.builder()
                .id(receiptId)
                .rental(rental)
                .payment(paidPayment)
                .receiptNumber("QTL-2026-EEEEEEEE")
                .periodStart(LocalDate.of(2026, 1, 1))
                .periodEnd(LocalDate.of(2026, 1, 31))
                .amount(new BigDecimal("900.00"))
                .currency("EUR")
                .document(doc)
                .createdAt(LocalDateTime.now())
                .build();

        when(receiptRepository.findById(receiptId)).thenReturn(Optional.of(receipt));
        when(userRepository.findById(tenantId)).thenReturn(Optional.of(tenantUser));
        when(storageService.generatePresignedUrl("key1")).thenReturn("https://url");

        ReceiptResponse result = receiptService.findById(receiptId);

        assertThat(result).isNotNull();
        assertThat(result.receiptNumber()).isEqualTo("QTL-2026-EEEEEEEE");
    }

    @Test
    void findById_unauthorized_shouldThrow() {
        UUID unrelatedUserId = UUID.randomUUID();
        setCurrentUser(unrelatedUserId, "other@test.com", "TENANT");

        User unrelatedUser = User.builder()
                .id(unrelatedUserId)
                .email("other@test.com")
                .passwordHash("hash")
                .role(Role.TENANT)
                .enabled(true)
                .build();

        Document doc = Document.builder().id(UUID.randomUUID()).fileKey("key1").build();
        UUID receiptId = UUID.randomUUID();
        Receipt receipt = Receipt.builder()
                .id(receiptId)
                .rental(rental)
                .payment(paidPayment)
                .receiptNumber("QTL-2026-FFFFFFFF")
                .periodStart(LocalDate.of(2026, 1, 1))
                .periodEnd(LocalDate.of(2026, 1, 31))
                .amount(new BigDecimal("900.00"))
                .currency("EUR")
                .document(doc)
                .createdAt(LocalDateTime.now())
                .build();

        when(receiptRepository.findById(receiptId)).thenReturn(Optional.of(receipt));
        when(userRepository.findById(unrelatedUserId)).thenReturn(Optional.of(unrelatedUser));

        assertThatThrownBy(() -> receiptService.findById(receiptId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("not authorized");
    }

    // --- Email notification ---

    @Test
    void generateReceipt_shouldSendEmail() throws IOException {
        setCurrentUser(ownerId, "owner@test.com", "OWNER");
        UUID paymentId = paidPayment.getId();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(paidPayment));
        when(receiptRepository.findByPaymentId(paymentId)).thenReturn(Optional.empty());
        when(pdfService.generatePdf(anyString(), any())).thenReturn("pdf-content".getBytes());
        when(pdfService.calculateHash(any())).thenReturn("hash123");
        when(storageService.uploadFile(any(), anyString(), anyString())).thenReturn("file-key-123");
        when(storageService.generatePresignedUrl("file-key-123")).thenReturn("https://presigned-url");
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> {
            Document doc = inv.getArgument(0);
            doc.setId(UUID.randomUUID());
            return doc;
        });
        when(receiptRepository.save(any(Receipt.class))).thenAnswer(inv -> {
            Receipt r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        receiptService.generateReceipt(paymentId);

        verify(emailService).sendReceiptEmail(argThat(data ->
                data.recipientEmail().equals("tenant@test.com") &&
                data.recipientName().contains("Jean") &&
                data.ownerName().contains("Marie") &&
                data.receiptNumber() != null &&
                data.totalAmount().contains("900") &&
                data.receiptViewUrl().startsWith("http://localhost:4200/receipts/")
        ));
    }

    @Test
    void generateReceipt_emailFailure_shouldNotBlockGeneration() throws IOException {
        setCurrentUser(ownerId, "owner@test.com", "OWNER");
        UUID paymentId = paidPayment.getId();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(paidPayment));
        when(receiptRepository.findByPaymentId(paymentId)).thenReturn(Optional.empty());
        when(pdfService.generatePdf(anyString(), any())).thenReturn("pdf-content".getBytes());
        when(pdfService.calculateHash(any())).thenReturn("hash123");
        when(storageService.uploadFile(any(), anyString(), anyString())).thenReturn("file-key-123");
        when(storageService.generatePresignedUrl("file-key-123")).thenReturn("https://presigned-url");
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> {
            Document doc = inv.getArgument(0);
            doc.setId(UUID.randomUUID());
            return doc;
        });
        when(receiptRepository.save(any(Receipt.class))).thenAnswer(inv -> {
            Receipt r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });
        doThrow(new RuntimeException("SMTP error")).when(emailService).sendReceiptEmail(any(ReceiptEmailData.class));

        ReceiptResponse response = receiptService.generateReceipt(paymentId);

        // Receipt should still be created despite email failure
        assertThat(response).isNotNull();
        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("900.00"));
        verify(receiptRepository).save(any(Receipt.class));
        verify(emailService).sendReceiptEmail(any(ReceiptEmailData.class));
    }
}
