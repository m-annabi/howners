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
import com.howners.gestion.dto.email.SignatureDeclinedEmailData;
import com.howners.gestion.dto.email.SignatureRequestEmailData;
import com.howners.gestion.dto.esignature.ESignatureRequest;
import com.howners.gestion.dto.esignature.ESignatureResponse;
import com.howners.gestion.dto.esignature.WebhookEvent;
import com.howners.gestion.exception.esignature.*;
import com.howners.gestion.repository.ContractRepository;
import com.howners.gestion.repository.ContractSignatureRequestRepository;
import com.howners.gestion.repository.ContractVersionRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.security.ContractTokenProvider;
import com.howners.gestion.service.auth.AuthService;
import com.howners.gestion.service.email.EmailService;
import com.howners.gestion.service.esignature.ESignatureProvider;
import com.howners.gestion.service.esignature.ESignatureProviderFactory;
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
import java.util.List;
import java.util.UUID;

/**
 * Service principal pour la gestion des signatures électroniques de contrats
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
    private final ESignatureProviderFactory providerFactory;
    private final EmailService emailService;
    private final ContractTokenProvider tokenProvider;
    private final StorageService storageService;
    private final PdfService pdfService;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Value("${app.backend-url:http://localhost:8080}")
    private String backendUrl;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");
    private static final int DEFAULT_TOKEN_EXPIRATION_DAYS = 30;

    /**
     * Envoie un contrat pour signature électronique
     */
    @Transactional
    public SignatureRequestResponse sendContractForSignature(UUID contractId) {
        log.info("Sending contract {} for electronic signature", contractId);

        // 1. Charger et valider le contrat
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ContractNotFoundException(
                        "Contract not found: " + contractId,
                        "CONTRACT_NOT_FOUND"));

        if (contract.getStatus() != ContractStatus.DRAFT) {
            throw new ContractInvalidStateException(
                    "Contract must be in DRAFT status to be sent for signature. Current status: " + contract.getStatus(),
                    "CONTRACT_INVALID_STATE");
        }

        // Vérifier qu'il n'y a pas déjà une demande active
        if (signatureRequestRepository.hasActiveSignatureRequest(contractId)) {
            throw new ContractInvalidStateException(
                    "Contract already has an active signature request",
                    "CONTRACT_HAS_ACTIVE_REQUEST");
        }

        Rental rental = contract.getRental();
        User tenant = rental.getTenant();
        User owner = rental.getProperty().getOwner();

        // Valider que le locataire est assigné
        if (tenant == null) {
            log.warn("Cannot send contract {} for signature: rental has no tenant assigned", contractId);
            throw new ContractInvalidStateException(
                    "Cannot send contract for signature: no tenant assigned to rental",
                    "CONTRACT_NO_TENANT");
        }

        // 2. Récupérer la version actuelle du contrat pour obtenir le fileKey
        ContractVersion currentVersion = contractVersionRepository
                .findByContractIdAndVersion(contract.getId(), contract.getCurrentVersion())
                .orElseThrow(() -> new DocumentDownloadException(
                        "Contract version not found: " + contract.getCurrentVersion(),
                        "CONTRACT_VERSION_NOT_FOUND"));

        if (currentVersion.getFileKey() == null || currentVersion.getFileKey().isEmpty()) {
            log.error("Contract {} version {} has no fileKey", contract.getId(), contract.getCurrentVersion());
            throw new DocumentDownloadException(
                    "Contract PDF file key is missing for version " + contract.getCurrentVersion(),
                    "CONTRACT_FILE_KEY_MISSING");
        }

        // 3. Générer le token d'accès
        String[] tokenPair = tokenProvider.generateAndHashToken();
        String rawToken = tokenPair[0];
        String hashedToken = tokenPair[1];

        // 4. Télécharger le PDF du contrat depuis MinIO
        byte[] contractPdf;
        try {
            contractPdf = storageService.downloadFile(currentVersion.getFileKey());
            log.info("Downloaded contract PDF with fileKey: {}", currentVersion.getFileKey());
        } catch (Exception e) {
            log.error("Failed to download contract PDF with fileKey: {}", currentVersion.getFileKey(), e);
            throw new DocumentDownloadException(
                    "Failed to download contract PDF from storage",
                    "DOCUMENT_DOWNLOAD_FAILED",
                    e);
        }

        // 4. Créer la demande de signature chez le provider
        ESignatureProvider provider = providerFactory.getProvider();
        String returnUrl = frontendUrl + "/contracts/sign/complete?token=" + rawToken;

        ESignatureRequest esignRequest = ESignatureRequest.builder()
                .documentName("Contrat_" + contract.getContractNumber() + ".pdf")
                .documentContent(contractPdf)
                .signerEmail(tenant.getEmail())
                .signerName(tenant.getFirstName() + " " + tenant.getLastName())
                .returnUrl(returnUrl)
                .expirationDays(DEFAULT_TOKEN_EXPIRATION_DAYS)
                .build();

        ESignatureResponse esignResponse = provider.createSignatureRequest(esignRequest);

        // 5. Créer l'entité ContractSignatureRequest
        ContractSignatureRequest signatureRequest = ContractSignatureRequest.builder()
                .contract(contract)
                .provider(providerFactory.getProviderName())
                .providerEnvelopeId(esignResponse.envelopeId())
                .signer(tenant)
                .signerEmail(tenant.getEmail())
                .accessToken(hashedToken)
                .tokenExpiresAt(LocalDateTime.now().plusDays(DEFAULT_TOKEN_EXPIRATION_DAYS))
                .status(SignatureRequestStatus.PENDING)
                .signingUrl(esignResponse.signingUrl())
                .build();

        // 6. Sauvegarder la demande de signature
        signatureRequest.setStatus(SignatureRequestStatus.SENT);
        signatureRequest.setSentAt(LocalDateTime.now());
        signatureRequest = signatureRequestRepository.save(signatureRequest);

        // 7. Mettre à jour le statut du contrat
        contract.setStatus(ContractStatus.SENT);
        contract.setSentAt(LocalDateTime.now());
        contract.setSignatureProvider(providerFactory.getProviderName());
        contractRepository.save(contract);

        log.info("Contract {} marked as SENT with signature provider {}", contractId, providerFactory.getProviderName());

        // 8. Envoyer l'email au locataire (APRÈS le commit de la transaction)
        String signingLink = frontendUrl + "/contracts/sign?token=" + rawToken;
        String expirationDate = signatureRequest.getTokenExpiresAt().format(DATE_FORMATTER);

        SignatureRequestEmailData emailData = SignatureRequestEmailData.builder()
                .recipientEmail(tenant.getEmail())
                .recipientName(tenant.getFirstName() + " " + tenant.getLastName())
                .ownerName(owner.getFirstName() + " " + owner.getLastName())
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
            // Ne pas throw - la demande de signature est déjà créée et le contrat mis à jour
            // L'email pourra être renvoyé manuellement via la fonction resend
        }

        log.info("Contract {} sent for signature successfully. Signature request ID: {}",
                contractId, signatureRequest.getId());

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
                        "Invalid or expired token",
                        "INVALID_TOKEN"));

        // Valider le token
        if (!tokenProvider.validateToken(token, signatureRequest.getAccessToken())) {
            throw new InvalidTokenException(
                    "Token validation failed",
                    "TOKEN_VALIDATION_FAILED");
        }

        // Vérifier l'expiration
        if (signatureRequest.isTokenExpired()) {
            signatureRequest.setStatus(SignatureRequestStatus.EXPIRED);
            signatureRequestRepository.save(signatureRequest);
            throw new TokenExpiredException(
                    "Token has expired on " + signatureRequest.getTokenExpiresAt(),
                    "TOKEN_EXPIRED");
        }

        // Vérifier le statut
        if (signatureRequest.getStatus() == SignatureRequestStatus.SIGNED) {
            throw new ContractInvalidStateException(
                    "Contract has already been signed",
                    "CONTRACT_ALREADY_SIGNED");
        }

        if (signatureRequest.getStatus() == SignatureRequestStatus.DECLINED) {
            throw new ContractInvalidStateException(
                    "Contract signature was declined",
                    "CONTRACT_DECLINED");
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
                .documentUrl(signatureRequest.getSigningUrl())
                .build();
    }

    /**
     * Traite un webhook provenant du fournisseur de signature
     */
    @Transactional
    public void processWebhook(String provider, String payload, String signature) {
        log.info("Processing webhook from provider: {}", provider);

        ESignatureProvider esignProvider = providerFactory.getProvider();

        // Valider le webhook
        if (!esignProvider.validateWebhook(payload, signature)) {
            log.warn("Invalid webhook signature from provider: {}", provider);
            throw new WebhookValidationException(
                    "Invalid webhook signature from provider: " + provider,
                    "WEBHOOK_VALIDATION_FAILED");
        }

        // Parser le payload
        WebhookEvent event = esignProvider.parseWebhookPayload(payload);
        log.info("Webhook event: {} for envelope: {}", event.eventType(), event.envelopeId());

        // Trouver la demande de signature
        ContractSignatureRequest signatureRequest = signatureRequestRepository
                .findByProviderEnvelopeId(event.envelopeId())
                .orElseThrow(() -> new SignatureRequestNotFoundException(
                        "Signature request not found for envelope: " + event.envelopeId(),
                        "SIGNATURE_REQUEST_NOT_FOUND"));

        // Traiter selon le type d'événement
        switch (event.status().toLowerCase()) {
            case "completed", "signed" -> handleSignatureCompleted(signatureRequest, event);
            case "declined" -> handleSignatureDeclined(signatureRequest, event);
            case "viewed" -> handleSignatureViewed(signatureRequest, event);
            default -> log.info("Unhandled webhook event status: {}", event.status());
        }
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

        // Générer un nouveau token
        String[] tokenPair = tokenProvider.generateAndHashToken();
        String rawToken = tokenPair[0];
        String hashedToken = tokenPair[1];

        signatureRequest.setAccessToken(hashedToken);
        signatureRequest.setTokenExpiresAt(LocalDateTime.now().plusDays(DEFAULT_TOKEN_EXPIRATION_DAYS));
        signatureRequest.setResendCount(signatureRequest.getResendCount() + 1);
        signatureRequest.setLastResentAt(LocalDateTime.now());

        // Envoyer l'email
        Contract contract = signatureRequest.getContract();
        Rental rental = contract.getRental();
        User tenant = rental.getTenant();
        User owner = rental.getProperty().getOwner();

        String signingLink = frontendUrl + "/contracts/sign?token=" + rawToken;
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

        // Annuler chez le provider
        try {
            ESignatureProvider provider = providerFactory.getProvider();
            provider.cancelSignatureRequest(signatureRequest.getProviderEnvelopeId());
        } catch (Exception e) {
            log.error("Failed to cancel signature request at provider", e);
        }

        signatureRequest.setStatus(SignatureRequestStatus.CANCELLED);
        signatureRequestRepository.save(signatureRequest);

        log.info("Signature request cancelled successfully");
    }

    /**
     * Envoi automatique de rappels pour les signatures en attente (> 3 jours, max 3 rappels)
     */
    @Scheduled(cron = "0 0 9 * * *") // Tous les jours à 9h
    @Transactional
    public void sendAutomaticReminders() {
        log.info("Starting automatic signature reminders job");

        LocalDateTime sentBefore = LocalDateTime.now().minusDays(3);
        List<ContractSignatureRequest> requests = signatureRequestRepository
                .findSentRequestsNeedingReminder(sentBefore, 3);

        int remindersSent = 0;
        for (ContractSignatureRequest request : requests) {
            try {
                // Don't send reminder if last reminder was less than 3 days ago
                if (request.getLastReminderAt() != null &&
                        request.getLastReminderAt().isAfter(LocalDateTime.now().minusDays(3))) {
                    continue;
                }

                Contract contract = request.getContract();
                Rental rental = contract.getRental();
                User tenant = request.getSigner();
                User owner = rental.getProperty().getOwner();

                // Generate new token for the reminder link
                String[] tokenPair = tokenProvider.generateAndHashToken();
                String rawToken = tokenPair[0];
                String hashedToken = tokenPair[1];

                request.setAccessToken(hashedToken);
                request.setTokenExpiresAt(LocalDateTime.now().plusDays(DEFAULT_TOKEN_EXPIRATION_DAYS));

                String signingLink = frontendUrl + "/contracts/sign?token=" + rawToken;
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
     * Envoie un contrat pour signature à plusieurs signataires (séquentiel par ordre)
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

        // Get the contract PDF
        ContractVersion currentVersion = contractVersionRepository
                .findByContractIdAndVersion(contract.getId(), contract.getCurrentVersion())
                .orElseThrow(() -> new DocumentDownloadException(
                        "Contract version not found", "CONTRACT_VERSION_NOT_FOUND"));

        byte[] contractPdf;
        try {
            contractPdf = storageService.downloadFile(currentVersion.getFileKey());
        } catch (Exception e) {
            throw new DocumentDownloadException("Failed to download contract PDF", "DOCUMENT_DOWNLOAD_FAILED", e);
        }

        Rental rental = contract.getRental();
        ESignatureProvider provider = providerFactory.getProvider();

        List<SignatureRequestResponse> responses = new java.util.ArrayList<>();

        for (SignerInfo signerInfo : signers) {
            User signer = userRepository.findByEmail(signerInfo.email())
                    .orElseThrow(() -> new ContractNotFoundException(
                            "Signer not found: " + signerInfo.email(), "SIGNER_NOT_FOUND"));

            String[] tokenPair = tokenProvider.generateAndHashToken();
            String rawToken = tokenPair[0];
            String hashedToken = tokenPair[1];

            String returnUrl = frontendUrl + "/contracts/sign/complete?token=" + rawToken;

            ESignatureRequest esignRequest = ESignatureRequest.builder()
                    .documentName("Contrat_" + contract.getContractNumber() + ".pdf")
                    .documentContent(contractPdf)
                    .signerEmail(signerInfo.email())
                    .signerName(signerInfo.name())
                    .returnUrl(returnUrl)
                    .expirationDays(DEFAULT_TOKEN_EXPIRATION_DAYS)
                    .build();

            ESignatureResponse esignResponse = provider.createSignatureRequest(esignRequest);

            ContractSignatureRequest signatureRequest = ContractSignatureRequest.builder()
                    .contract(contract)
                    .provider(providerFactory.getProviderName())
                    .providerEnvelopeId(esignResponse.envelopeId())
                    .signer(signer)
                    .signerEmail(signerInfo.email())
                    .accessToken(hashedToken)
                    .tokenExpiresAt(LocalDateTime.now().plusDays(DEFAULT_TOKEN_EXPIRATION_DAYS))
                    .status(SignatureRequestStatus.SENT)
                    .signingUrl(esignResponse.signingUrl())
                    .signerOrder(signerInfo.order())
                    .build();

            signatureRequest.setSentAt(LocalDateTime.now());
            signatureRequest = signatureRequestRepository.save(signatureRequest);

            // Send email only to first signer (sequential signing)
            if (signerInfo.order() == 1) {
                String signingLink = frontendUrl + "/contracts/sign?token=" + rawToken;
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
        contract.setSignatureProvider(providerFactory.getProviderName());
        contractRepository.save(contract);

        log.info("Contract {} sent for multi-signature to {} signers", contractId, signers.size());
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

    // Méthodes privées

    private void handleSignatureCompleted(ContractSignatureRequest signatureRequest, WebhookEvent event) {
        log.info("Handling signature completion for envelope: {}", event.envelopeId());

        signatureRequest.setStatus(SignatureRequestStatus.SIGNED);
        signatureRequest.setSignedAt(LocalDateTime.now());
        signatureRequest.setIpAddress(event.ipAddress());
        signatureRequest.setUserAgent(event.userAgent());
        signatureRequestRepository.save(signatureRequest);

        // Mettre à jour le contrat
        Contract contract = signatureRequest.getContract();
        contract.setStatus(ContractStatus.SIGNED);
        contract.setSignedAt(LocalDateTime.now());
        contractRepository.save(contract);

        // Télécharger le document signé et le stocker
        try {
            ESignatureProvider provider = providerFactory.getProvider();
            byte[] signedPdfBytes = provider.getSignedDocument(signatureRequest.getProviderEnvelopeId());

            String documentHash = pdfService.calculateHash(signedPdfBytes);
            String fileName = String.format("contract_%s_signed_%d.pdf",
                    contract.getContractNumber(), System.currentTimeMillis());

            String documentKey = storageService.uploadFile(signedPdfBytes, fileName, "application/pdf");

            ContractVersion signedVersion = ContractVersion.builder()
                    .contract(contract)
                    .version(contract.getCurrentVersion() + 1)
                    .fileKey(documentKey)
                    .content("Signed document")
                    .documentHash(documentHash)
                    .build();

            contractVersionRepository.save(signedVersion);
            contract.setCurrentVersion(signedVersion.getVersion());
            contractRepository.save(contract);

            log.info("Signed PDF downloaded and stored for contract {} with fileKey: {}",
                    contract.getId(), documentKey);
        } catch (Exception e) {
            log.error("Failed to download and store signed PDF for contract {}: {}",
                    contract.getId(), e.getMessage(), e);
        }

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

        emailService.sendSignatureCompletedEmail(emailData);

        log.info("Signature completed successfully for contract: {}", contract.getId());
    }

    private void handleSignatureDeclined(ContractSignatureRequest signatureRequest, WebhookEvent event) {
        log.info("Handling signature decline for envelope: {}", event.envelopeId());

        signatureRequest.setStatus(SignatureRequestStatus.DECLINED);
        signatureRequest.setDeclinedAt(LocalDateTime.now());
        signatureRequestRepository.save(signatureRequest);

        // Mettre à jour le contrat
        Contract contract = signatureRequest.getContract();
        contract.setStatus(ContractStatus.DRAFT); // Retour à DRAFT

        // Notifier le propriétaire
        Rental rental = contract.getRental();
        User owner = rental.getProperty().getOwner();

        SignatureDeclinedEmailData emailData = SignatureDeclinedEmailData.builder()
                .recipientEmail(owner.getEmail())
                .recipientName(getFullName(owner))
                .tenantName(getFullName(rental.getTenant()))
                .propertyName(rental.getProperty().getName())
                .contractNumber(contract.getContractNumber())
                .declinedDate(signatureRequest.getDeclinedAt().format(DATETIME_FORMATTER))
                .reason(signatureRequest.getDeclineReason())
                .build();

        emailService.sendSignatureDeclinedEmail(emailData);

        log.info("Signature declined for contract: {}", contract.getId());
    }

    private void handleSignatureViewed(ContractSignatureRequest signatureRequest, WebhookEvent event) {
        if (signatureRequest.getViewedAt() == null) {
            signatureRequest.setStatus(SignatureRequestStatus.VIEWED);
            signatureRequest.setViewedAt(LocalDateTime.now());
            signatureRequestRepository.save(signatureRequest);
            log.info("Contract viewed for the first time");
        }
    }

    private SignatureRequestResponse mapToResponse(ContractSignatureRequest request) {
        return SignatureRequestResponse.builder()
                .id(request.getId())
                .contractId(request.getContract().getId())
                .contractNumber(request.getContract().getContractNumber())
                .provider(request.getProvider())
                .providerEnvelopeId(request.getProviderEnvelopeId())
                .signerEmail(request.getSignerEmail())
                .signerName(getFullName(request.getSigner()))
                .status(request.getStatus())
                .signingUrl(request.getSigningUrl())
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
