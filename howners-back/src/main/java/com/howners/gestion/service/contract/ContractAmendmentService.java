package com.howners.gestion.service.contract;

import com.howners.gestion.domain.audit.AuditAction;
import com.howners.gestion.domain.contract.AmendmentStatus;
import com.howners.gestion.domain.contract.Contract;
import com.howners.gestion.domain.contract.ContractAmendment;
import com.howners.gestion.domain.contract.ContractStatus;
import com.howners.gestion.domain.document.Document;
import com.howners.gestion.domain.document.DocumentType;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.contract.AmendmentResponse;
import com.howners.gestion.dto.contract.CreateAmendmentRequest;
import com.howners.gestion.exception.BadRequestException;
import com.howners.gestion.exception.ResourceNotFoundException;
import com.howners.gestion.repository.ContractAmendmentRepository;
import com.howners.gestion.repository.ContractRepository;
import com.howners.gestion.repository.DocumentRepository;
import com.howners.gestion.repository.RentalRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.service.audit.AuditService;
import com.howners.gestion.service.auth.AuthService;
import com.howners.gestion.service.storage.StorageService;
import com.howners.gestion.util.UserDisplayUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContractAmendmentService {

    private final ContractAmendmentRepository amendmentRepository;
    private final ContractRepository contractRepository;
    private final RentalRepository rentalRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final PdfService pdfService;
    private final StorageService storageService;
    private final AuditService auditService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Transactional(readOnly = true)
    public List<AmendmentResponse> findByContractId(UUID contractId) {
        return amendmentRepository.findByContractIdOrderByAmendmentNumberDesc(contractId)
                .stream()
                .map(AmendmentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AmendmentResponse findById(UUID id) {
        ContractAmendment amendment = amendmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Amendment not found"));
        return AmendmentResponse.from(amendment);
    }

    @Transactional
    public AmendmentResponse create(UUID contractId, CreateAmendmentRequest request) throws IOException {
        UUID currentUserId = AuthService.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found"));

        if (contract.getStatus() != ContractStatus.ACTIVE && contract.getStatus() != ContractStatus.SIGNED) {
            throw new BadRequestException("Contract must be ACTIVE or SIGNED to create an amendment");
        }

        int nextNumber = amendmentRepository.countByContractId(contractId) + 1;

        ContractAmendment amendment = ContractAmendment.builder()
                .contract(contract)
                .amendmentNumber(nextNumber)
                .reason(request.reason())
                .changes(request.changes())
                .previousRent(request.previousRent())
                .newRent(request.newRent())
                .effectiveDate(request.effectiveDate())
                .status(AmendmentStatus.DRAFT)
                .createdBy(currentUser)
                .build();

        String html = buildAmendmentHtml(amendment, contract);
        byte[] pdfBytes = pdfService.generatePdf(html, "Avenant n°" + nextNumber);

        String fileKey = storageService.uploadFile(pdfBytes,
                "amendments/avenant-" + contract.getContractNumber() + "-" + nextNumber + ".pdf",
                "application/pdf");

        Document doc = Document.builder()
                .rental(contract.getRental())
                .fileName("avenant-" + contract.getContractNumber() + "-" + nextNumber + ".pdf")
                .fileKey(fileKey)
                .mimeType("application/pdf")
                .fileSize((long) pdfBytes.length)
                .documentType(DocumentType.CONTRACT)
                .uploader(currentUser)
                .build();
        doc = documentRepository.save(doc);
        amendment.setDocument(doc);

        amendment = amendmentRepository.save(amendment);
        auditService.logAction(AuditAction.CREATE, "ContractAmendment", amendment.getId());
        log.info("Amendment {} created for contract {}", nextNumber, contract.getContractNumber());

        return AmendmentResponse.from(amendment);
    }

    @Transactional
    public AmendmentResponse sign(UUID amendmentId) {
        ContractAmendment amendment = amendmentRepository.findById(amendmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Amendment not found"));

        if (amendment.getStatus() != AmendmentStatus.DRAFT && amendment.getStatus() != AmendmentStatus.SENT) {
            throw new BadRequestException("Amendment must be DRAFT or SENT to sign");
        }

        amendment.setStatus(AmendmentStatus.SIGNED);
        amendment.setSignedAt(LocalDateTime.now());

        if (amendment.getNewRent() != null && amendment.getNewRent().compareTo(BigDecimal.ZERO) > 0) {
            var rental = amendment.getContract().getRental();
            rental.setMonthlyRent(amendment.getNewRent());
            rentalRepository.save(rental);
            log.info("Rental {} rent updated from {} to {}",
                    rental.getId(), amendment.getPreviousRent(), amendment.getNewRent());
        }

        amendment = amendmentRepository.save(amendment);
        auditService.logAction(AuditAction.CONTRACT_SIGNED, "ContractAmendment", amendment.getId());
        return AmendmentResponse.from(amendment);
    }

    @Transactional(readOnly = true)
    public byte[] downloadPdf(UUID amendmentId) throws IOException {
        ContractAmendment amendment = amendmentRepository.findById(amendmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Amendment not found"));

        if (amendment.getDocument() == null) {
            throw new BadRequestException("No PDF document for this amendment");
        }

        return storageService.downloadFile(amendment.getDocument().getFileKey());
    }

    private String buildAmendmentHtml(ContractAmendment amendment, Contract contract) {
        String ownerName = escapeHtml(UserDisplayUtils.getFullName(contract.getRental().getProperty().getOwner()));
        String tenantName = escapeHtml(UserDisplayUtils.getFullName(contract.getRental().getTenant()));
        String propertyName = escapeHtml(contract.getRental().getProperty().getName());

        StringBuilder html = new StringBuilder();
        html.append("<div style='font-family: Arial, sans-serif; padding: 40px;'>");
        html.append("<h1 style='text-align: center;'>AVENANT N°").append(amendment.getAmendmentNumber()).append("</h1>");
        html.append("<h2 style='text-align: center;'>au contrat ").append(escapeHtml(contract.getContractNumber())).append("</h2>");
        html.append("<hr/>");

        html.append("<p><strong>Entre :</strong></p>");
        html.append("<p>Le bailleur : <strong>").append(ownerName).append("</strong></p>");
        html.append("<p>Et le locataire : <strong>").append(tenantName).append("</strong></p>");
        html.append("<p>Pour le bien situé : <strong>").append(propertyName).append("</strong></p>");
        html.append("<hr/>");

        html.append("<h3>Objet de l'avenant</h3>");
        html.append("<p>").append(escapeHtml(amendment.getReason())).append("</p>");

        if (amendment.getPreviousRent() != null && amendment.getNewRent() != null) {
            html.append("<h3>Modification du loyer</h3>");
            html.append("<p>Ancien loyer : <strong>").append(formatAmount(amendment.getPreviousRent())).append("</strong></p>");
            html.append("<p>Nouveau loyer : <strong>").append(formatAmount(amendment.getNewRent())).append("</strong></p>");
        }

        html.append("<p><strong>Date d'effet :</strong> ").append(amendment.getEffectiveDate()).append("</p>");
        html.append("<p><strong>Date de rédaction :</strong> ")
                .append(LocalDateTime.now().format(DATE_FORMATTER))
                .append("</p>");

        html.append("<div style='margin-top: 60px;'>");
        html.append("<table style='width: 100%;'><tr>");
        html.append("<td style='width: 50%;'><p>Le Bailleur</p><br/><br/><p>").append(ownerName).append("</p></td>");
        html.append("<td style='width: 50%;'><p>Le Locataire</p><br/><br/><p>").append(tenantName).append("</p></td>");
        html.append("</tr></table>");
        html.append("</div>");
        html.append("</div>");

        return html.toString();
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }

    private static String formatAmount(BigDecimal amount) {
        if (amount == null) return "0,00 €";
        return String.format("%,.2f €", amount);
    }
}
