package com.howners.gestion.service.invoice;

import com.howners.gestion.domain.invoice.Invoice;
import com.howners.gestion.util.PdfFormat;
import com.howners.gestion.domain.invoice.InvoiceStatus;
import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.domain.rental.RentalStatus;
import com.howners.gestion.domain.user.Role;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.invoice.CreateInvoiceRequest;
import com.howners.gestion.dto.invoice.InvoiceResponse;
import com.howners.gestion.exception.ForbiddenException;
import com.howners.gestion.exception.ResourceNotFoundException;
import com.howners.gestion.repository.InvoiceRepository;
import com.howners.gestion.repository.RentalRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.service.auth.AuthService;
import com.howners.gestion.service.contract.PdfService;
import com.howners.gestion.service.document.DocumentSequenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
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
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final RentalRepository rentalRepository;
    private final UserRepository userRepository;
    private final PdfService pdfService;
    private final DocumentSequenceService documentSequenceService;

    private static final DateTimeFormatter FR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Transactional(readOnly = true)
    public List<InvoiceResponse> findByCurrentUser() {
        UUID currentUserId = AuthService.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId.toString()));

        if (currentUser.getRole() == Role.ADMIN) {
            return invoiceRepository.findAll().stream()
                    .map(InvoiceResponse::from)
                    .collect(Collectors.toList());
        }

        if (currentUser.getRole() == Role.TENANT) {
            // Tenant sees invoices for their rentals
            return rentalRepository.findByTenantId(currentUserId).stream()
                    .flatMap(rental -> invoiceRepository.findByRentalId(rental.getId()).stream())
                    .map(InvoiceResponse::from)
                    .collect(Collectors.toList());
        }

        return invoiceRepository.findByOwnerId(currentUserId).stream()
                .map(InvoiceResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InvoiceResponse findById(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", invoiceId.toString()));
        checkAccess(invoice);
        return InvoiceResponse.from(invoice);
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> findByRentalId(UUID rentalId) {
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new ResourceNotFoundException("Rental", "id", rentalId.toString()));
        checkRentalAccess(rental);
        return invoiceRepository.findByRentalId(rentalId).stream()
                .map(InvoiceResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public InvoiceResponse createInvoice(CreateInvoiceRequest request) {
        UUID currentUserId = AuthService.getCurrentUserId();

        Rental rental = rentalRepository.findById(request.rentalId())
                .orElseThrow(() -> new ResourceNotFoundException("Rental", "id", request.rentalId().toString()));

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId.toString()));

        if (!rental.getProperty().getOwner().getId().equals(currentUserId) && currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenException("You are not authorized to create invoices for this rental");
        }

        String invoiceNumber = generateInvoiceNumber(rental.getProperty().getOwner().getId());

        Invoice invoice = Invoice.builder()
                .rental(rental)
                .invoiceNumber(invoiceNumber)
                .invoiceType(request.invoiceType())
                .amount(request.amount())
                .currency("EUR")
                .issueDate(request.issueDate())
                .dueDate(request.dueDate())
                .status(InvoiceStatus.ISSUED)
                .build();

        invoice = invoiceRepository.save(invoice);
        log.info("Invoice {} created for rental {}", invoiceNumber, rental.getId());

        return InvoiceResponse.from(invoice);
    }

    @Transactional(readOnly = true)
    public byte[] generateInvoicePdf(UUID invoiceId) throws IOException {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", invoiceId.toString()));
        checkAccess(invoice);

        String html = buildInvoiceHtml(invoice);
        return pdfService.generatePdf(html, null);
    }

    @Scheduled(cron = "0 0 1 1 * ?")
    @Transactional
    public void generateMonthlyInvoices() {
        log.info("Starting monthly invoice generation");
        List<Rental> activeRentals = rentalRepository.findByStatus(RentalStatus.ACTIVE);

        for (Rental rental : activeRentals) {
            try {
                String invoiceNumber = generateInvoiceNumber(rental.getProperty().getOwner().getId());
                LocalDate now = LocalDate.now();

                Invoice invoice = Invoice.builder()
                        .rental(rental)
                        .invoiceNumber(invoiceNumber)
                        .invoiceType(com.howners.gestion.domain.invoice.InvoiceType.RENT)
                        .amount(rental.getMonthlyRent())
                        .currency(rental.getCurrency())
                        .issueDate(now)
                        .dueDate(now.plusDays(rental.getPaymentDay() != null ? rental.getPaymentDay() : 5))
                        .status(InvoiceStatus.ISSUED)
                        .build();

                invoiceRepository.save(invoice);
                log.info("Monthly invoice {} generated for rental {}", invoiceNumber, rental.getId());
            } catch (Exception e) {
                log.error("Failed to generate monthly invoice for rental {}: {}", rental.getId(), e.getMessage());
            }
        }
        log.info("Monthly invoice generation completed");
    }

    private void checkRentalAccess(Rental rental) {
        UUID currentUserId = AuthService.getCurrentUserId();
        UUID ownerId = rental.getProperty().getOwner().getId();
        UUID tenantId = rental.getTenant() != null ? rental.getTenant().getId() : null;

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId.toString()));

        if (!ownerId.equals(currentUserId) && !currentUserId.equals(tenantId) && currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenException("You are not authorized to access invoices for this rental");
        }
    }

    private void checkAccess(Invoice invoice) {
        UUID currentUserId = AuthService.getCurrentUserId();
        UUID ownerId = invoice.getRental().getProperty().getOwner().getId();
        UUID tenantId = invoice.getRental().getTenant() != null ? invoice.getRental().getTenant().getId() : null;

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId.toString()));

        if (!ownerId.equals(currentUserId) && !currentUserId.equals(tenantId) && currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenException("You are not authorized to access this invoice");
        }
    }

    /**
     * Alloue le prochain numéro de facture du bailleur pour l'année en cours :
     * séquence chronologique continue par émetteur (art. 242 nonies A du CGI),
     * au format INV-2026-0001. L'UPSERT est atomique : deux créations
     * concurrentes ne peuvent pas obtenir le même numéro, et l'incrément est
     * annulé avec la transaction si la création de la facture échoue (pas de
     * trou dans la séquence).
     */
    private String generateInvoiceNumber(UUID ownerId) {
        int year = LocalDate.now().getYear();
        long seq = documentSequenceService.next(ownerId, DocumentSequenceService.INVOICE, year);
        return String.format("INV-%d-%04d", year, seq);
    }

    private String buildInvoiceHtml(Invoice invoice) {
        Rental rental = invoice.getRental();
        var property = rental.getProperty();
        User owner = property.getOwner();
        User tenant = rental.getTenant();

        return """
                <div style="text-align: center; margin-bottom: 24px;">
                    <h1 style="margin-bottom: 4px;">FACTURE</h1>
                    <p style="font-size: 10pt; color: #666;">N° %s</p>
                </div>

                <table style="border: none; margin-bottom: 16px;">
                    <tr>
                        <td style="border: none; width: 50%%; vertical-align: top;">
                            <strong>Émetteur (bailleur) :</strong><br/>%s%s
                        </td>
                        <td style="border: none; width: 50%%; vertical-align: top;">
                            <strong>Destinataire (locataire) :</strong><br/>%s%s
                        </td>
                    </tr>
                </table>

                <p><strong>Bien concerné :</strong> %s — %s</p>
                <p><strong>Date d'émission :</strong> %s &nbsp;·&nbsp; <strong>Date d'échéance :</strong> %s</p>

                <table style="margin-top: 16px;">
                    <tr>
                        <th>Description</th>
                        <th class="text-right">Montant</th>
                    </tr>
                    <tr>
                        <td>%s</td>
                        <td class="text-right">%s</td>
                    </tr>
                    <tr>
                        <td><strong>Total à payer</strong></td>
                        <td class="text-right"><strong>%s</strong></td>
                    </tr>
                </table>

                <p class="legal-note">TVA non applicable, article 293 B du CGI. Facture émise entre particuliers dans le cadre d'une location à usage d'habitation.</p>
                """.formatted(
                invoice.getInvoiceNumber(),
                owner.getFullName(),
                PdfFormat.blocAdresse(owner),
                tenant != null ? tenant.getFullName() : "Locataire non renseigné",
                tenant != null ? PdfFormat.blocAdresse(tenant) : "",
                property.getName(),
                PdfFormat.adressePostale(property),
                invoice.getIssueDate().format(FR_DATE),
                invoice.getDueDate() != null ? invoice.getDueDate().format(FR_DATE) : "à réception",
                libelleType(invoice.getInvoiceType()),
                PdfFormat.montant(invoice.getAmount()),
                PdfFormat.montant(invoice.getAmount())
        );
    }

    private String libelleType(com.howners.gestion.domain.invoice.InvoiceType type) {
        if (type == null) return "Prestation";
        return switch (type) {
            case RENT -> "Loyer";
            case CHARGES -> "Charges locatives";
            case DEPOSIT -> "Dépôt de garantie";
            default -> "Autre prestation";
        };
    }
}
