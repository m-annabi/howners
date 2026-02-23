package com.howners.gestion.service.contract;

import com.howners.gestion.domain.contract.Contract;
import com.howners.gestion.domain.contract.ContractSignatureRequest;
import com.howners.gestion.domain.contract.ContractStatus;
import com.howners.gestion.domain.contract.ContractVersion;
import com.howners.gestion.domain.contract.SignatureRequestStatus;
import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.contract.ContractPublicView;
import com.howners.gestion.dto.contract.SignatureRequestResponse;
import com.howners.gestion.dto.contract.SignatureTrackingDashboard;
import com.howners.gestion.dto.contract.SignerInfo;
import com.howners.gestion.dto.email.SignatureCompletedEmailData;
import com.howners.gestion.dto.email.SignatureRequestEmailData;
import com.howners.gestion.exception.esignature.*;
import com.howners.gestion.repository.ContractRepository;
import com.howners.gestion.repository.ContractSignatureRequestRepository;
import com.howners.gestion.repository.ContractVersionRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.security.ContractTokenProvider;
import com.howners.gestion.service.auth.AuthService;
import com.howners.gestion.service.email.EmailService;
import com.howners.gestion.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Service principal pour la gestion des signatures électroniques de contrats.
 * Signature directe sur le PDF (sans provider externe).
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ContractESignatureService {

    private final ContractRepository contractRepository;
    private final ContractSignatureRequestRepository signatureRequestRepository;
    private final ContractVersionRepository contractVersionRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final ContractTokenProvider tokenProvider;
    private final StorageService storageService;
    private final PdfService pdfService;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Value("${esignature.token-expiration-days:30}")
    private int tokenExpirationDays;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");

    /**
     * Envoie un contrat pour signature électronique.
     * Génère un token d'accès et envoie un email au locataire avec un lien de signature directe.
     */
    @Transactional
    public SignatureRequestResponse sendContractForSignature(UUID contractId) {
        log.info("Sending contract {} for signature", contractId);

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ContractNotFoundException(
                        "Contract not found: " + contractId, "CONTRACT_NOT_FOUND"));

        if (contract.getStatus() != ContractStatus.DRAFT) {
            throw new ContractInvalidStateException(
                    "Contract must be in DRAFT status to be sent for signature. Current status: " + contract.getStatus(),
                    "CONTRACT_INVALID_STATE");
        }

        if (signatureRequestRepository.hasActiveSignatureRequest(contractId)) {
            throw new ContractInvalidStateException(
                    "Contract already has an active signature request",
                    "CONTRACT_HAS_ACTIVE_REQUEST");
        }

        Rental rental = contract.getRental();
        User tenant = rental.getTenant();
        User owner = rental.getProperty().getOwner();

        if (tenant == null) {
            throw new ContractInvalidStateException(
                    "Cannot send contract for signature: no tenant assigned to rental",
                    "CONTRACT_NO_TENANT");
        }

        // Vérifier que le PDF existe
        ContractVersion currentVersion = contractVersionRepository
                .findByContractIdAndVersion(contract.getId(), contract.getCurrentVersion())
                .orElseThrow(() -> new DocumentDownloadException(
                        "Contract version not found: " + contract.getCurrentVersion(),
                        "CONTRACT_VERSION_NOT_FOUND"));

        if (currentVersion.getFileKey() == null || currentVersion.getFileKey().isEmpty()) {
            throw new DocumentDownloadException(
                    "Contract PDF file key is missing for version " + contract.getCurrentVersion(),
                    "CONTRACT_FILE_KEY_MISSING");
        }

        // Générer le token d'accès
        String[] tokenPair = tokenProvider.generateAndHashToken();
        String rawToken = tokenPair[0];
        String hashedToken = tokenPair[1];

        // Créer la demande de signature
        ContractSignatureRequest signatureRequest = ContractSignatureRequest.builder()
                .contract(contract)
                .provider("DIRECT")
                .signer(tenant)
                .signerEmail(tenant.getEmail())
                .accessToken(hashedToken)
                .tokenExpiresAt(LocalDateTime.now().plusDays(tokenExpirationDays))
                .status(SignatureRequestStatus.SENT)
                .build();

        signatureRequest.setSentAt(LocalDateTime.now());
        signatureRequest = signatureRequestRepository.save(signatureRequest);

        // Mettre à jour le statut du contrat
        contract.setStatus(ContractStatus.SENT);
        contract.setSentAt(LocalDateTime.now());
        contract.setSignatureProvider("DIRECT");
        contractRepository.save(contract);

        log.info("Contract {} marked as SENT for direct signature", contractId);

        // Envoyer l'email au locataire
        String signingLink = frontendUrl + "/sign?token=" + rawToken;
        String expirationDate = signatureRequest.getTokenExpiresAt().format(DATE_FORMATTER);

        SignatureRequestEmailData emailData = SignatureRequestEmailData.builder()
                .recipientEmail(tenant.getEmail())
                .recipientName(getFullName(tenant))
                .ownerName(getFullName(owner))
                .propertyName(rental.getProperty().getName())
                .propertyAddress(getPropertyAddress(rental))
                .contractNumber(contract.getContractNumber())
                .signingUrl(signingLink)
                .expirationDate(expirationDate)
                .build();

        try {
            emailService.sendSignatureRequestEmail(emailData);
            log.info("Signature request email sent to {}", tenant.getEmail());
        } catch (Exception e) {
            log.error("Failed to send signature email, but signature request was created successfully", e);
        }

        return mapToResponse(signatureRequest);
    }

    /**
     * Récupère un contrat par son token d'accès (pour la vue publique)
     */
    public ContractPublicView getContractByToken(String token) {
        log.info("Getting contract by access token");

        ContractSignatureRequest signatureRequest = signatureRequestRepository
                .findByAccessTokenWithDetails(token)
                .orElseThrow(() -> new InvalidTokenException(
                        "Invalid or expired token", "INVALID_TOKEN"));

        if (!tokenProvider.validateToken(token, signatureRequest.getAccessToken())) {
            throw new InvalidTokenException("Token validation failed", "TOKEN_VALIDATION_FAILED");
        }

        if (signatureRequest.isTokenExpired()) {
            signatureRequest.setStatus(SignatureRequestStatus.EXPIRED);
            signatureRequestRepository.save(signatureRequest);
            throw new TokenExpiredException(
                    "Token has expired on " + signatureRequest.getTokenExpiresAt(), "TOKEN_EXPIRED");
        }

        if (signatureRequest.getStatus() == SignatureRequestStatus.SIGNED) {
            throw new ContractInvalidStateException("Contract has already been signed", "CONTRACT_ALREADY_SIGNED");
        }

        Contract contract = signatureRequest.getContract();
        Rental rental = contract.getRental();

        return ContractPublicView.builder()
                .contractId(contract.getId())
                .contractNumber(contract.getContractNumber())
                .status(contract.getStatus())
                .propertyName(rental.getProperty().getName())
                .propertyAddress(getPropertyAddress(rental))
                .ownerName(getFullName(rental.getProperty().getOwner()))
                .tenantName(getFullName(rental.getTenant()))
                .rentalStartDate(rental.getStartDate())
                .rentalEndDate(rental.getEndDate())
                .monthlyRent(formatAmount(rental.getMonthlyRent()))
                .createdAt(contract.getCreatedAt())
                .documentUrl(null)
                .build();
    }

    /**
     * Signe un contrat via le token public.
     * Reçoit l'image de la signature, l'applique sur le PDF, et stocke le résultat.
     */
    @Transactional
    public void signContractByToken(String token, String signatureDataBase64, String signerName, String ipAddress, String userAgent) {
        log.info("Signing contract by token for signer: {}", signerName);

        ContractSignatureRequest signatureRequest = signatureRequestRepository
                .findByAccessTokenWithDetails(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired token", "INVALID_TOKEN"));

        if (!tokenProvider.validateToken(token, signatureRequest.getAccessToken())) {
            throw new InvalidTokenException("Token validation failed", "TOKEN_VALIDATION_FAILED");
        }

        if (signatureRequest.isTokenExpired()) {
            signatureRequest.setStatus(SignatureRequestStatus.EXPIRED);
            signatureRequestRepository.save(signatureRequest);
            throw new TokenExpiredException(
                    "Token has expired on " + signatureRequest.getTokenExpiresAt(), "TOKEN_EXPIRED");
        }

        if (signatureRequest.getStatus() == SignatureRequestStatus.SIGNED) {
            throw new ContractInvalidStateException("Contract has already been signed", "CONTRACT_ALREADY_SIGNED");
        }

        Contract contract = signatureRequest.getContract();

        // Télécharger le PDF original
        ContractVersion currentVersion = contractVersionRepository
                .findByContractIdAndVersion(contract.getId(), contract.getCurrentVersion())
                .orElseThrow(() -> new DocumentDownloadException(
                        "Contract version not found", "CONTRACT_VERSION_NOT_FOUND"));

        byte[] originalPdf;
        try {
            originalPdf = storageService.downloadFile(currentVersion.getFileKey());
        } catch (Exception e) {
            throw new DocumentDownloadException("Failed to download contract PDF", "DOCUMENT_DOWNLOAD_FAILED", e);
        }

        // Décoder l'image de signature et l'appliquer sur le PDF
        byte[] signatureImageBytes = Base64.getDecoder().decode(signatureDataBase64);
        byte[] signedPdfBytes;
        try {
            signedPdfBytes = pdfService.applySignatureToPdf(originalPdf, signatureImageBytes, signerName);
        } catch (Exception e) {
            log.error("Failed to apply signature to PDF", e);
            throw new RuntimeException("Failed to apply signature to PDF", e);
        }

        // Stocker le PDF signé
        String documentHash = pdfService.calculateHash(signedPdfBytes);
        String fileName = String.format("contract_%s_signed_%d.pdf",
                contract.getContractNumber(), System.currentTimeMillis());
        String documentKey = storageService.uploadFile(signedPdfBytes, fileName, "application/pdf");

        // Créer une nouvelle version du contrat
        ContractVersion signedVersion = ContractVersion.builder()
                .contract(contract)
                .version(contract.getCurrentVersion() + 1)
                .fileKey(documentKey)
                .content("Document signé par " + signerName)
                .documentHash(documentHash)
                .build();
        contractVersionRepository.save(signedVersion);

        // Mettre à jour le contrat
        contract.setCurrentVersion(signedVersion.getVersion());
        contract.setStatus(ContractStatus.SIGNED);
        contract.setSignedAt(LocalDateTime.now());
        contractRepository.save(contract);

        // Mettre à jour la demande de signature
        signatureRequest.setStatus(SignatureRequestStatus.SIGNED);
        signatureRequest.setSignedAt(LocalDateTime.now());
        signatureRequest.setIpAddress(ipAddress);
        signatureRequest.setUserAgent(userAgent);
        signatureRequestRepository.save(signatureRequest);

        // Notifier le propriétaire
        Rental rental = contract.getRental();
        User owner = rental.getProperty().getOwner();

        SignatureCompletedEmailData emailData = SignatureCompletedEmailData.builder()
                .recipientEmail(owner.getEmail())
                .recipientName(getFullName(owner))
                .tenantName(getFullName(rental.getTenant()))
                .propertyName(rental.getProperty().getName())
                .contractNumber(contract.getContractNumber())
                .signedDate(signatureRequest.getSignedAt().format(DATETIME_FORMATTER))
                .contractViewUrl(frontendUrl + "/contracts/" + contract.getId())
                .build();

        try {
            emailService.sendSignatureCompletedEmail(emailData);
        } catch (Exception e) {
            log.error("Failed to send signature completed email", e);
        }

        log.info("Contract {} signed successfully by {}", contract.getContractNumber(), signerName);
    }

    /**
     * Renvoie une demande de signature
     */
    @Transactional
    public void resendSignatureRequest(UUID signatureRequestId) {
        log.info("Resending signature request: {}", signatureRequestId);

        ContractSignatureRequest signatureRequest = signatureRequestRepository
                .findByIdWithDetails(signatureRequestId)
                .orElseThrow(() -> new SignatureRequestNotFoundException(
                        "Signature request not found: " + signatureRequestId,
                        "SIGNATURE_REQUEST_NOT_FOUND"));

        if (!signatureRequest.canBeResent()) {
            throw new ContractInvalidStateException(
                    "Signature request cannot be resent in current status: " + signatureRequest.getStatus(),
                    "SIGNATURE_REQUEST_CANNOT_BE_RESENT");
        }

        String[] tokenPair = tokenProvider.generateAndHashToken();
        String rawToken = tokenPair[0];
        String hashedToken = tokenPair[1];

        signatureRequest.setAccessToken(hashedToken);
        signatureRequest.setTokenExpiresAt(LocalDateTime.now().plusDays(tokenExpirationDays));
        signatureRequest.setResendCount(signatureRequest.getResendCount() + 1);
        signatureRequest.setLastResentAt(LocalDateTime.now());

        Contract contract = signatureRequest.getContract();
        Rental rental = contract.getRental();
        User tenant = rental.getTenant();
        User owner = rental.getProperty().getOwner();

        String signingLink = frontendUrl + "/sign?token=" + rawToken;
        String expirationDate = signatureRequest.getTokenExpiresAt().format(DATE_FORMATTER);

        SignatureRequestEmailData emailData = SignatureRequestEmailData.builder()
                .recipientEmail(tenant.getEmail())
                .recipientName(getFullName(tenant))
                .ownerName(getFullName(owner))
                .propertyName(rental.getProperty().getName())
                .propertyAddress(getPropertyAddress(rental))
                .contractNumber(contract.getContractNumber())
                .signingUrl(signingLink)
                .expirationDate(expirationDate)
                .build();

        emailService.sendSignatureRequestEmail(emailData);
        signatureRequestRepository.save(signatureRequest);

        log.info("Signature request resent successfully");
    }

    /**
     * Annule une demande de signature
     */
    @Transactional
    public void cancelSignatureRequest(UUID signatureRequestId) {
        log.info("Cancelling signature request: {}", signatureRequestId);

        ContractSignatureRequest signatureRequest = signatureRequestRepository
                .findById(signatureRequestId)
                .orElseThrow(() -> new SignatureRequestNotFoundException(
                        "Signature request not found: " + signatureRequestId,
                        "SIGNATURE_REQUEST_NOT_FOUND"));

        if (!signatureRequest.canBeCancelled()) {
            throw new ContractInvalidStateException(
                    "Signature request cannot be cancelled in current status: " + signatureRequest.getStatus(),
                    "SIGNATURE_REQUEST_CANNOT_BE_CANCELLED");
        }

        signatureRequest.setStatus(SignatureRequestStatus.CANCELLED);
        signatureRequestRepository.save(signatureRequest);

        log.info("Signature request cancelled successfully");
    }

    /**
     * Envoi automatique de rappels pour les signatures en attente (> 3 jours, max 3 rappels)
     */
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void sendAutomaticReminders() {
        log.info("Starting automatic signature reminders job");

        LocalDateTime sentBefore = LocalDateTime.now().minusDays(3);
        List<ContractSignatureRequest> requests = signatureRequestRepository
                .findSentRequestsNeedingReminder(sentBefore, 3);

        int remindersSent = 0;
        for (ContractSignatureRequest request : requests) {
            try {
                if (request.getLastReminderAt() != null &&
                        request.getLastReminderAt().isAfter(LocalDateTime.now().minusDays(3))) {
                    continue;
                }

                Contract contract = request.getContract();
                Rental rental = contract.getRental();
                User tenant = request.getSigner();
                User owner = rental.getProperty().getOwner();

                String[] tokenPair = tokenProvider.generateAndHashToken();
                String rawToken = tokenPair[0];
                String hashedToken = tokenPair[1];

                request.setAccessToken(hashedToken);
                request.setTokenExpiresAt(LocalDateTime.now().plusDays(tokenExpirationDays));

                String signingLink = frontendUrl + "/sign?token=" + rawToken;
                String expirationDate = request.getTokenExpiresAt().format(DATE_FORMATTER);

                SignatureRequestEmailData emailData = SignatureRequestEmailData.builder()
                        .recipientEmail(tenant.getEmail())
                        .recipientName(getFullName(tenant))
                        .ownerName(getFullName(owner))
                        .propertyName(rental.getProperty().getName())
                        .propertyAddress(getPropertyAddress(rental))
                        .contractNumber(contract.getContractNumber())
                        .signingUrl(signingLink)
                        .expirationDate(expirationDate)
                        .build();

                emailService.sendSignatureRequestEmail(emailData);

                request.setReminderCount(request.getReminderCount() + 1);
                request.setLastReminderAt(LocalDateTime.now());
                signatureRequestRepository.save(request);

                remindersSent++;
                log.info("Reminder {} sent for signature request {} (contract {})",
                        request.getReminderCount(), request.getId(), contract.getContractNumber());
            } catch (Exception e) {
                log.error("Failed to send reminder for signature request {}", request.getId(), e);
            }
        }

        log.info("Automatic reminders job completed. {} reminders sent out of {} candidates",
                remindersSent, requests.size());
    }

    /**
     * Récupère le tableau de bord de suivi des signatures pour le propriétaire courant
     */
    public SignatureTrackingDashboard getSignatureTrackingDashboard() {
        UUID currentUserId = AuthService.getCurrentUserId();

        List<ContractSignatureRequest> allRequests = signatureRequestRepository.findByOwnerId(currentUserId);
        List<Object[]> statusCounts = signatureRequestRepository.countByStatusForOwner(currentUserId);

        long pending = 0, sent = 0, viewed = 0, signed = 0, declined = 0, expired = 0;
        for (Object[] row : statusCounts) {
            SignatureRequestStatus status = (SignatureRequestStatus) row[0];
            long count = (long) row[1];
            switch (status) {
                case PENDING -> pending = count;
                case SENT -> sent = count;
                case VIEWED -> viewed = count;
                case SIGNED -> signed = count;
                case DECLINED -> declined = count;
                case EXPIRED -> expired = count;
                default -> {}
            }
        }

        List<SignatureRequestResponse> recent = allRequests.stream()
                .limit(20)
                .map(this::mapToResponse)
                .toList();

        return new SignatureTrackingDashboard(
                allRequests.size(),
                pending, sent, viewed, signed, declined, expired,
                recent
        );
    }

    /**
     * Envoie un contrat pour signature à plusieurs signataires
     */
    @Transactional
    public List<SignatureRequestResponse> sendContractForMultiSignature(UUID contractId, List<SignerInfo> signers) {
        log.info("Sending contract {} for multi-signature with {} signers", contractId, signers.size());

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ContractNotFoundException(
                        "Contract not found: " + contractId, "CONTRACT_NOT_FOUND"));

        if (contract.getStatus() != ContractStatus.DRAFT) {
            throw new ContractInvalidStateException(
                    "Contract must be in DRAFT status for multi-signature", "CONTRACT_INVALID_STATE");
        }

        if (signatureRequestRepository.hasActiveSignatureRequest(contractId)) {
            throw new ContractInvalidStateException(
                    "Contract already has an active signature request", "CONTRACT_HAS_ACTIVE_REQUEST");
        }

        Rental rental = contract.getRental();
        List<SignatureRequestResponse> responses = new java.util.ArrayList<>();

        for (SignerInfo signerInfo : signers) {
            User signer = userRepository.findByEmail(signerInfo.email())
                    .orElseThrow(() -> new ContractNotFoundException(
                            "Signer not found: " + signerInfo.email(), "SIGNER_NOT_FOUND"));

            String[] tokenPair = tokenProvider.generateAndHashToken();
            String rawToken = tokenPair[0];
            String hashedToken = tokenPair[1];

            ContractSignatureRequest signatureRequest = ContractSignatureRequest.builder()
                    .contract(contract)
                    .provider("DIRECT")
                    .signer(signer)
                    .signerEmail(signerInfo.email())
                    .accessToken(hashedToken)
                    .tokenExpiresAt(LocalDateTime.now().plusDays(tokenExpirationDays))
                    .status(SignatureRequestStatus.SENT)
                    .signerOrder(signerInfo.order())
                    .build();

            signatureRequest.setSentAt(LocalDateTime.now());
            signatureRequest = signatureRequestRepository.save(signatureRequest);

            if (signerInfo.order() == 1) {
                String signingLink = frontendUrl + "/sign?token=" + rawToken;
                User owner = rental.getProperty().getOwner();

                SignatureRequestEmailData emailData = SignatureRequestEmailData.builder()
                        .recipientEmail(signerInfo.email())
                        .recipientName(signerInfo.name())
                        .ownerName(getFullName(owner))
                        .propertyName(rental.getProperty().getName())
                        .propertyAddress(getPropertyAddress(rental))
                        .contractNumber(contract.getContractNumber())
                        .signingUrl(signingLink)
                        .expirationDate(signatureRequest.getTokenExpiresAt().format(DATE_FORMATTER))
                        .build();

                try {
                    emailService.sendSignatureRequestEmail(emailData);
                } catch (Exception e) {
                    log.error("Failed to send signature email to {}", signerInfo.email(), e);
                }
            }

            responses.add(mapToResponse(signatureRequest));
        }

        contract.setStatus(ContractStatus.SENT);
        contract.setSentAt(LocalDateTime.now());
        contract.setSignatureProvider("DIRECT");
        contractRepository.save(contract);

        return responses;
    }

    /**
     * Récupère le statut de la signature d'un contrat
     */
    @Transactional(readOnly = true)
    public SignatureRequestResponse getSignatureStatus(UUID contractId) {
        ContractSignatureRequest request = signatureRequestRepository
                .findByContractId(contractId)
                .orElseThrow(() -> new SignatureRequestNotFoundException(
                        "No signature request found for contract " + contractId,
                        "SIGNATURE_REQUEST_NOT_FOUND"));
        return mapToResponse(request);
    }

    /**
     * Télécharge le PDF du contrat associé au token (pour prévisualisation publique)
     */
    public byte[] downloadContractPdfByToken(String token) {
        ContractSignatureRequest signatureRequest = signatureRequestRepository
                .findByAccessTokenWithDetails(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired token", "INVALID_TOKEN"));

        if (!tokenProvider.validateToken(token, signatureRequest.getAccessToken())) {
            throw new InvalidTokenException("Token validation failed", "TOKEN_VALIDATION_FAILED");
        }

        if (signatureRequest.isTokenExpired()) {
            throw new TokenExpiredException("Token has expired", "TOKEN_EXPIRED");
        }

        Contract contract = signatureRequest.getContract();
        ContractVersion currentVersion = contractVersionRepository
                .findByContractIdAndVersion(contract.getId(), contract.getCurrentVersion())
                .orElseThrow(() -> new DocumentDownloadException(
                        "Contract version not found", "CONTRACT_VERSION_NOT_FOUND"));

        return storageService.downloadFile(currentVersion.getFileKey());
    }

    // === Private helpers ===

    private SignatureRequestResponse mapToResponse(ContractSignatureRequest request) {
        return SignatureRequestResponse.builder()
                .id(request.getId())
                .contractId(request.getContract().getId())
                .contractNumber(request.getContract().getContractNumber())
                .provider(request.getProvider())
                .signerEmail(request.getSignerEmail())
                .signerName(getFullName(request.getSigner()))
                .status(request.getStatus())
                .sentAt(request.getSentAt())
                .viewedAt(request.getViewedAt())
                .signedAt(request.getSignedAt())
                .declinedAt(request.getDeclinedAt())
                .tokenExpiresAt(request.getTokenExpiresAt())
                .resendCount(request.getResendCount())
                .declineReason(request.getDeclineReason())
                .reminderCount(request.getReminderCount())
                .lastReminderAt(request.getLastReminderAt())
                .signerOrder(request.getSignerOrder())
                .build();
    }

    private String getFullName(User user) {
        return user.getFirstName() + " " + user.getLastName();
    }

    private String getPropertyAddress(Rental rental) {
        var property = rental.getProperty();
        return String.format("%s, %s %s",
                property.getAddressLine1(),
                property.getPostalCode(),
                property.getCity());
    }

    private String formatAmount(BigDecimal amount) {
        return String.format("%.2f €", amount);
    }
}
