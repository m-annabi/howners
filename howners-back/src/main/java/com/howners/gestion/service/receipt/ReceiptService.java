package com.howners.gestion.service.receipt;

import com.howners.gestion.domain.document.Document;
import com.howners.gestion.domain.document.DocumentType;
import com.howners.gestion.domain.payment.Payment;
import com.howners.gestion.util.PdfDoc;
import com.howners.gestion.util.PdfFormat;
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
import com.howners.gestion.repository.RentalRepository;
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
    private final RentalRepository rentalRepository;
    private final com.howners.gestion.service.document.DocumentSequenceService documentSequenceService;
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
        String receiptNumber = generateReceiptNumber(rental.getProperty().getOwner().getId());

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

        Document document = generateAndStorePdf(rental, payment, receiptNumber, periodStart, periodEnd).document();

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

        String documentUrl = storageService.generatePresignedUrl(document.getFileKey());
        return ReceiptResponse.from(receipt, documentUrl);
    }

    private record GeneratedPdf(Document document, byte[] bytes) {}

    private GeneratedPdf generateAndStorePdf(Rental rental, Payment payment, String receiptNumber,
                                             LocalDate periodStart, LocalDate periodEnd) {
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
        return new GeneratedPdf(documentRepository.save(document), pdfBytes);
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
        // Les quittances (et leurs URLs S3 pré-signées) ne sont accessibles qu'au
        // propriétaire du bien, au locataire du bail, ou à un admin.
        com.howners.gestion.domain.rental.Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new ResourceNotFoundException("Rental", "id", rentalId.toString()));
        UUID currentUserId = AuthService.getCurrentUserId();
        UUID ownerId = rental.getProperty() != null && rental.getProperty().getOwner() != null
                ? rental.getProperty().getOwner().getId() : null;
        UUID tenantId = rental.getTenant() != null ? rental.getTenant().getId() : null;
        boolean isAdmin = userRepository.findById(currentUserId)
                .map(u -> u.getRole() == Role.ADMIN).orElse(false);
        if (!currentUserId.equals(ownerId) && !currentUserId.equals(tenantId) && !isAdmin) {
            throw new ForbiddenException("Vous n'êtes pas autorisé à accéder à ce bail.");
        }
        return receiptRepository.findByRentalId(rentalId).stream()
                .map(r -> toResponseWithUrl(r))
                .collect(Collectors.toList());
    }

    @Transactional
    public byte[] downloadReceiptPdf(UUID receiptId) throws IOException {
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt", "id", receiptId.toString()));
        checkAccess(receipt);

        // Quittance enregistrée sans PDF (reprise de données) : génération à la volée
        if (receipt.getDocument() == null || receipt.getDocument().getFileKey() == null) {
            GeneratedPdf pdf = generateAndStorePdf(receipt.getRental(), receipt.getPayment(),
                    receipt.getReceiptNumber(), receipt.getPeriodStart(), receipt.getPeriodEnd());
            receipt.setDocument(pdf.document());
            receiptRepository.save(receipt);
            log.info("Receipt {} PDF generated lazily on download", receipt.getReceiptNumber());
            return pdf.bytes();
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

    /**
     * Numéro de quittance séquentiel par bailleur et par année (Q-2026-0001),
     * alloué atomiquement — même mécanique que les factures (changelog 093).
     */
    private String generateReceiptNumber(UUID ownerId) {
        int year = LocalDate.now().getYear();
        long seq = documentSequenceService.next(ownerId,
                com.howners.gestion.service.document.DocumentSequenceService.RECEIPT, year);
        return String.format("Q-%d-%04d", year, seq);
    }

    private String buildQuittanceHtml(Rental rental, Payment payment, String receiptNumber,
                                       LocalDate periodStart, LocalDate periodEnd) {
        User owner = rental.getProperty().getOwner();
        User tenant = rental.getTenant();
        var property = rental.getProperty();

        String ownerName = owner.getFullName();
        String tenantName = tenant != null ? tenant.getFullName() : "Locataire non renseigné";
        String address = PdfFormat.adressePostale(property);
        String ville = property.getCity() != null && !property.getCity().isBlank() ? property.getCity() : "";

        String rentAmount = PdfFormat.montant(rental.getMonthlyRent());
        String chargesAmount = PdfFormat.montant(rental.getCharges());
        String totalAmount = PdfFormat.montant(payment.getAmount());
        String paidDate = payment.getPaidAt() != null ? payment.getPaidAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : LocalDate.now().format(FR_DATE);

        java.util.LinkedHashMap<String, String> lignes = new java.util.LinkedHashMap<>();
        lignes.put("Loyer", rentAmount);
        lignes.put("Charges", chargesAmount);

        return PdfDoc.header("Quittance de loyer", receiptNumber)
                + PdfDoc.parties(
                        "Bailleur", PdfDoc.escape(ownerName) + PdfFormat.blocAdresse(owner),
                        "Locataire", PdfDoc.escape(tenantName) + PdfFormat.blocAdresse(tenant))
                + "<p><strong>Adresse du bien :</strong> " + PdfDoc.escape(address) + "</p>"
                + "<p class=\"mt-1\">Je soussigné(e), <strong>" + PdfDoc.escape(ownerName)
                + "</strong>, propriétaire du logement désigné ci-dessus, déclare avoir reçu de <strong>"
                + PdfDoc.escape(tenantName) + "</strong> la somme indiquée ci-dessous, en paiement du loyer et des charges "
                + "du logement pour la période du <strong>" + periodStart.format(FR_DATE)
                + "</strong> au <strong>" + periodEnd.format(FR_DATE) + "</strong>.</p>"
                + PdfDoc.amounts(lignes, "Total réglé", totalAmount)
                + "<p class=\"mt-2\">Cette quittance vaut preuve du paiement pour la période indiquée. "
                + "Date du paiement : <strong>" + PdfDoc.escape(paidDate) + "</strong>.</p>"
                + "<p class=\"mt-3\">Fait à " + PdfDoc.escape(ville.isEmpty() ? "—" : ville)
                + ", le " + LocalDate.now().format(FR_DATE) + ".</p>"
                + PdfDoc.legalNote("Cette quittance annule tous les reçus qui auraient pu être établis "
                + "précédemment en cas de paiement partiel du loyer. Elle ne préjuge pas de l'existence "
                + "d'une dette locative antérieure.");
    }
}
