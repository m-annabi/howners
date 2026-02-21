package com.howners.gestion.service.receipt;

import com.howners.gestion.domain.document.Document;
import com.howners.gestion.domain.document.DocumentType;
import com.howners.gestion.domain.payment.Payment;
import com.howners.gestion.domain.payment.PaymentStatus;
import com.howners.gestion.domain.receipt.Receipt;
import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.domain.user.Role;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.email.ReceiptEmailData;
import com.howners.gestion.dto.receipt.ReceiptResponse;
import com.howners.gestion.exception.BadRequestException;
import com.howners.gestion.exception.ForbiddenException;
import com.howners.gestion.exception.ResourceNotFoundException;
import com.howners.gestion.repository.DocumentRepository;
import com.howners.gestion.repository.PaymentRepository;
import com.howners.gestion.repository.ReceiptRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.service.auth.AuthService;
import com.howners.gestion.service.contract.PdfService;
import com.howners.gestion.service.email.EmailService;
import com.howners.gestion.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReceiptService {

    private final ReceiptRepository receiptRepository;
    private final PaymentRepository paymentRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final PdfService pdfService;
    private final StorageService storageService;
    private final EmailService emailService;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    private static final DateTimeFormatter FR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Transactional
    public ReceiptResponse generateReceipt(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId.toString()));

        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new BadRequestException("Cannot generate receipt: payment is not PAID");
        }

        // Check if receipt already exists
        if (receiptRepository.findByPaymentId(paymentId).isPresent()) {
            throw new BadRequestException("Receipt already exists for this payment");
        }

        Rental rental = payment.getRental();
        String receiptNumber = generateReceiptNumber();

        // Determine period from payment due date or current month
        LocalDate periodStart;
        LocalDate periodEnd;
        if (payment.getDueDate() != null) {
            periodStart = payment.getDueDate().withDayOfMonth(1);
            periodEnd = periodStart.plusMonths(1).minusDays(1);
        } else {
            periodStart = LocalDate.now().withDayOfMonth(1);
            periodEnd = periodStart.plusMonths(1).minusDays(1);
        }

        // Generate PDF
        String htmlContent = buildQuittanceHtml(rental, payment, receiptNumber, periodStart, periodEnd);
        byte[] pdfBytes;
        try {
            pdfBytes = pdfService.generatePdf(htmlContent, null);
        } catch (IOException e) {
            log.error("Failed to generate receipt PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate receipt PDF", e);
        }

        // Upload to MinIO
        String fileName = String.format("quittance_%s_%d.pdf", receiptNumber, System.currentTimeMillis());
        String fileKey = storageService.uploadFile(pdfBytes, fileName, "application/pdf");

        // Create Document record
        User owner = rental.getProperty().getOwner();
        Document document = Document.builder()
                .rental(rental)
                .property(rental.getProperty())
                .uploader(owner)
                .documentType(DocumentType.RECEIPT)
                .fileName(fileName)
                .filePath(fileKey)
                .fileKey(fileKey)
                .fileSize((long) pdfBytes.length)
                .mimeType("application/pdf")
                .documentHash(pdfService.calculateHash(pdfBytes))
                .description("Quittance de loyer - " + receiptNumber)
                .build();
        document = documentRepository.save(document);

        // Create Receipt
        Receipt receipt = Receipt.builder()
                .rental(rental)
                .payment(payment)
                .receiptNumber(receiptNumber)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .document(document)
                .build();
        receipt = receiptRepository.save(receipt);

        log.info("Receipt {} generated for payment {}", receiptNumber, paymentId);

        // Send email notification to tenant
        try {
            User tenant = rental.getTenant();
            if (tenant != null && tenant.getEmail() != null) {
                var property = rental.getProperty();
                String address = String.format("%s, %s %s",
                        property.getAddressLine1() != null ? property.getAddressLine1() : "",
                        property.getPostalCode() != null ? property.getPostalCode() : "",
                        property.getCity() != null ? property.getCity() : "");
                String periodLabel = periodStart.format(FR_DATE) + " - " + periodEnd.format(FR_DATE);

                emailService.sendReceiptEmail(ReceiptEmailData.builder()
                        .recipientEmail(tenant.getEmail())
                        .recipientName(tenant.getFullName())
                        .ownerName(rental.getProperty().getOwner().getFullName())
                        .propertyName(property.getName())
                        .propertyAddress(address)
                        .receiptNumber(receiptNumber)
                        .periodLabel(periodLabel)
                        .totalAmount(String.format("%.2f", payment.getAmount()))
                        .currency(payment.getCurrency() != null ? payment.getCurrency() : "EUR")
                        .receiptViewUrl(frontendUrl + "/receipts/" + receipt.getId())
                        .build());
            }
        } catch (Exception e) {
            log.error("Failed to send receipt email: {}", e.getMessage());
        }

        String documentUrl = storageService.generatePresignedUrl(fileKey);
        return ReceiptResponse.from(receipt, documentUrl);
    }

    @Transactional(readOnly = true)
    public List<ReceiptResponse> findByCurrentUser() {
        UUID currentUserId = AuthService.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId.toString()));

        if (currentUser.getRole() == Role.ADMIN) {
            return receiptRepository.findAll().stream()
                    .map(r -> toResponseWithUrl(r))
                    .collect(Collectors.toList());
        }

        if (currentUser.getRole() == Role.TENANT) {
            return receiptRepository.findByTenantId(currentUserId).stream()
                    .map(r -> toResponseWithUrl(r))
                    .collect(Collectors.toList());
        }

        return receiptRepository.findByOwnerId(currentUserId).stream()
                .map(r -> toResponseWithUrl(r))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReceiptResponse findById(UUID receiptId) {
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt", "id", receiptId.toString()));
        checkAccess(receipt);
        return toResponseWithUrl(receipt);
    }

    @Transactional(readOnly = true)
    public List<ReceiptResponse> findByRentalId(UUID rentalId) {
        return receiptRepository.findByRentalId(rentalId).stream()
                .map(r -> toResponseWithUrl(r))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public byte[] downloadReceiptPdf(UUID receiptId) throws IOException {
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt", "id", receiptId.toString()));
        checkAccess(receipt);

        if (receipt.getDocument() == null || receipt.getDocument().getFileKey() == null) {
            throw new BadRequestException("No PDF available for this receipt");
        }

        return storageService.downloadFile(receipt.getDocument().getFileKey());
    }

    private ReceiptResponse toResponseWithUrl(Receipt receipt) {
        String url = null;
        if (receipt.getDocument() != null && receipt.getDocument().getFileKey() != null) {
            url = storageService.generatePresignedUrl(receipt.getDocument().getFileKey());
        }
        return ReceiptResponse.from(receipt, url);
    }

    private void checkAccess(Receipt receipt) {
        UUID currentUserId = AuthService.getCurrentUserId();
        UUID ownerId = receipt.getRental().getProperty().getOwner().getId();
        UUID tenantId = receipt.getRental().getTenant() != null ? receipt.getRental().getTenant().getId() : null;

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId.toString()));

        if (!ownerId.equals(currentUserId) && !currentUserId.equals(tenantId) && currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenException("You are not authorized to access this receipt");
        }
    }

    private String generateReceiptNumber() {
        String year = String.valueOf(LocalDate.now().getYear());
        String random = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return String.format("QTL-%s-%s", year, random);
    }

    private String buildQuittanceHtml(Rental rental, Payment payment, String receiptNumber,
                                       LocalDate periodStart, LocalDate periodEnd) {
        User owner = rental.getProperty().getOwner();
        User tenant = rental.getTenant();
        var property = rental.getProperty();

        String ownerName = owner.getFullName();
        String tenantName = tenant != null ? tenant.getFullName() : "N/A";
        String address = String.format("%s, %s %s",
                property.getAddressLine1() != null ? property.getAddressLine1() : "",
                property.getPostalCode() != null ? property.getPostalCode() : "",
                property.getCity() != null ? property.getCity() : "");

        String rentAmount = String.format("%.2f", rental.getMonthlyRent());
        String chargesAmount = rental.getCharges() != null ? String.format("%.2f", rental.getCharges()) : "0.00";
        String totalAmount = String.format("%.2f", payment.getAmount());
        String paidDate = payment.getPaidAt() != null ? payment.getPaidAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : LocalDate.now().format(FR_DATE);

        return """
                <div style="text-align: center; margin-bottom: 30px;">
                    <h1 style="font-size: 18pt; margin-bottom: 5px;">QUITTANCE DE LOYER</h1>
                    <p style="font-size: 10pt; color: #666;">N° %s</p>
                </div>

                <table style="width: 100%%; border: none; margin-bottom: 20px;">
                    <tr>
                        <td style="border: none; width: 50%%; vertical-align: top;">
                            <strong>Bailleur :</strong><br/>
                            %s<br/>
                        </td>
                        <td style="border: none; width: 50%%; vertical-align: top;">
                            <strong>Locataire :</strong><br/>
                            %s<br/>
                        </td>
                    </tr>
                </table>

                <p><strong>Adresse du bien :</strong> %s</p>

                <p style="margin-top: 15px;">Je soussigné(e), <strong>%s</strong>, propriétaire du logement désigné ci-dessus, déclare avoir reçu de <strong>%s</strong> la somme indiquée ci-dessous, en paiement du loyer et des charges du logement pour la période du <strong>%s</strong> au <strong>%s</strong>.</p>

                <table style="margin-top: 20px; width: 80%%; margin-left: auto; margin-right: auto;">
                    <tr><td style="padding: 8px;"><strong>Loyer</strong></td><td style="padding: 8px; text-align: right;">%s €</td></tr>
                    <tr><td style="padding: 8px;"><strong>Charges</strong></td><td style="padding: 8px; text-align: right;">%s €</td></tr>
                    <tr style="border-top: 2px solid #333;"><td style="padding: 8px;"><strong>Total</strong></td><td style="padding: 8px; text-align: right;"><strong>%s €</strong></td></tr>
                </table>

                <p style="margin-top: 25px;">Date du paiement : <strong>%s</strong></p>

                <p style="margin-top: 30px; font-size: 9pt; color: #666; font-style: italic;">
                    Cette quittance annule tous les reçus qui auraient pu être établis précédemment en cas de paiement partiel du loyer. Elle ne préjuge pas de l'existence d'une dette locative antérieure.
                </p>
                """.formatted(receiptNumber, ownerName, tenantName, address, ownerName, tenantName,
                periodStart.format(FR_DATE), periodEnd.format(FR_DATE),
                rentAmount, chargesAmount, totalAmount, paidDate);
    }
}
