package com.howners.gestion.service.contract;

import com.howners.gestion.domain.contract.Contract;
import com.howners.gestion.domain.contract.ContractStatus;
import com.howners.gestion.domain.contract.ContractTemplate;
import com.howners.gestion.domain.contract.ContractVersion;
import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.domain.user.Role;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.contract.ContractResponse;
import com.howners.gestion.dto.contract.ContractVersionResponse;
import com.howners.gestion.dto.contract.CreateContractRequest;
import com.howners.gestion.dto.contract.UpdateContractRequest;
import com.howners.gestion.repository.ContractRepository;
import com.howners.gestion.repository.ContractTemplateRepository;
import com.howners.gestion.repository.ContractVersionRepository;
import com.howners.gestion.repository.RentalRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.domain.audit.AuditAction;
import com.howners.gestion.service.audit.AuditService;
import com.howners.gestion.service.auth.AuthService;
import com.howners.gestion.exception.BadRequestException;
import com.howners.gestion.exception.ForbiddenException;
import com.howners.gestion.exception.ResourceNotFoundException;
import com.howners.gestion.service.storage.StorageService;
import com.howners.gestion.util.UserDisplayUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ContractService {

    private final ContractRepository contractRepository;
    private final ContractVersionRepository contractVersionRepository;
    private final ContractTemplateRepository contractTemplateRepository;
    private final RentalRepository rentalRepository;
    private final UserRepository userRepository;
    private final ContractTemplateService contractTemplateService;
    private final PdfService pdfService;
    private final StorageService storageService;
    private final AuditService auditService;
    private final com.howners.gestion.service.subscription.FeatureGateService featureGateService;

    /**
     * Crée un nouveau contrat à partir d'une location
     */
    @Transactional
    public ContractResponse createContract(CreateContractRequest request) throws IOException {
        UUID currentUserId = AuthService.getCurrentUserId();
        featureGateService.assertCanCreate(currentUserId, "CONTRACTS");

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId.toString()));

        // Récupérer la location
        Rental rental = rentalRepository.findById(request.rentalId())
                .orElseThrow(() -> new ResourceNotFoundException("Rental", "id", request.rentalId().toString()));

        // Vérifier que l'utilisateur est bien le propriétaire
        if (!rental.getProperty().getOwner().getId().equals(currentUserId) && !isAdmin(currentUserId)) {
            throw new ForbiddenException("You are not authorized to create a contract for this rental");
        }

        // Générer le contenu du contrat
        String contractContent;

        // Si customContent fourni, remplir les variables puis l'utiliser
        if (request.customContent() != null && !request.customContent().isBlank()) {
            log.info("Using custom content for contract, length={}", request.customContent().length());
            log.debug("Raw customContent: {}", request.customContent());
            contractContent = contractTemplateService.fillCustomContent(request.customContent(), rental);
            log.debug("Filled customContent: {}", contractContent);
        } else {
            // Récupérer le template (ou utiliser le template par défaut)
            ContractTemplate template;
            if (request.templateId() != null) {
                template = contractTemplateRepository.findById(request.templateId())
                        .orElseThrow(() -> new ResourceNotFoundException("Template", "id", request.templateId().toString()));
            } else {
                template = contractTemplateService.getDefaultTemplate(rental.getRentalType());
            }

            // Générer le contenu avec le template
            contractContent = contractTemplateService.fillTemplate(template, rental);
        }

        // Créer le contrat
        String contractNumber = generateContractNumber();
        Contract contract = Contract.builder()
                .contractNumber(contractNumber)
                .rental(rental)
                .status(ContractStatus.DRAFT)
                .currentVersion(1)
                .createdBy(currentUser)
                .build();

        contract = contractRepository.save(contract);
        log.info("Contract created with number: {}", contractNumber);

        // Générer le PDF
        String fileName = pdfService.generateFileName(contractNumber, 1);
        byte[] pdfBytes = pdfService.generatePdf(contractContent, "Contrat de location - " + contractNumber);
        String pdfHash = pdfService.calculateHash(pdfBytes);

        // Uploader le PDF
        String documentKey = storageService.uploadFile(pdfBytes, fileName, "application/pdf");

        // Créer la première version (sans documentUrl, utilisera fileKey à la volée)
        ContractVersion version = ContractVersion.builder()
                .contract(contract)
                .version(1)
                .content(contractContent)
                .fileKey(documentKey)    // Stocker la clé MinIO
                .documentUrl(null)       // Ne plus stocker l'URL
                .documentHash(pdfHash)
                .createdBy(currentUser)
                .build();

        contractVersionRepository.save(version);

        // Ne plus stocker documentUrl dans le contrat (sera généré à la volée)
        contract.setDocumentUrl(null);
        contractRepository.save(contract);

        log.info("Contract version 1 created with PDF for contract: {}", contractNumber);
        auditService.logAction(AuditAction.CREATE, "Contract", contract.getId());
        featureGateService.incrementUsage(currentUserId, "CONTRACTS");

        return toResponse(contract);
    }

    /**
     * Met à jour un contrat (contenu ou statut)
     */
    @Transactional
    public ContractResponse updateContract(UUID contractId, UpdateContractRequest request) throws IOException {
        UUID currentUserId = AuthService.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId.toString()));

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract", "id", contractId.toString()));

        // Vérifier les permissions
        if (!contract.getRental().getProperty().getOwner().getId().equals(currentUserId) && !isAdmin(currentUserId)) {
            throw new ForbiddenException("You are not authorized to update this contract");
        }

        boolean needsNewVersion = false;

        // Mettre à jour le contenu si fourni
        if (request.customContent() != null && !request.customContent().isBlank()) {
            // Créer une nouvelle version avec le contenu modifié
            int newVersion = contract.getCurrentVersion() + 1;

            // IMPORTANT: Remplir les variables avant de générer le PDF
            // Cela permet de remplacer les variables comme {{tenant.name}} par les vraies valeurs
            String filledContent = contractTemplateService.fillCustomContent(
                    request.customContent(),
                    contract.getRental()
            );

            // Générer le nouveau PDF avec le contenu rempli
            String fileName = pdfService.generateFileName(contract.getContractNumber(), newVersion);
            byte[] pdfBytes = pdfService.generatePdf(filledContent,
                    "Contrat de location - " + contract.getContractNumber() + " v" + newVersion);
            String pdfHash = pdfService.calculateHash(pdfBytes);

            // Uploader le nouveau PDF
            String documentKey = storageService.uploadFile(pdfBytes, fileName, "application/pdf");

            // Créer la nouvelle version avec le contenu rempli (variables remplacées)
            ContractVersion version = ContractVersion.builder()
                    .contract(contract)
                    .version(newVersion)
                    .content(filledContent)  // Stocker le contenu avec variables remplacées
                    .fileKey(documentKey)    // Stocker la clé MinIO
                    .documentUrl(null)       // Ne plus stocker l'URL
                    .documentHash(pdfHash)
                    .createdBy(currentUser)
                    .build();

            contractVersionRepository.save(version);

            contract.setCurrentVersion(newVersion);
            contract.setDocumentUrl(null);  // Ne plus stocker l'URL
            needsNewVersion = true;

            log.info("Contract version {} created for contract: {}", newVersion, contract.getContractNumber());
        }

        // Mettre à jour le statut si fourni
        if (request.status() != null && request.status() != contract.getStatus()) {
            contract.setStatus(request.status());

            // Mettre à jour les timestamps selon le statut
            if (request.status() == ContractStatus.SENT) {
                contract.setSentAt(LocalDateTime.now());
            } else if (request.status() == ContractStatus.SIGNED) {
                contract.setSignedAt(LocalDateTime.now());
            }

            log.info("Contract status updated to {} for contract: {}", request.status(), contract.getContractNumber());
        }

        contractRepository.save(contract);

        return toResponse(contract);
    }

    /**
     * Récupère un contrat par ID
     */
    public ContractResponse getContract(UUID contractId) {
        UUID currentUserId = AuthService.getCurrentUserId();

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract", "id", contractId.toString()));

        // Vérifier les permissions (propriétaire ou locataire)
        UUID ownerId = contract.getRental().getProperty().getOwner().getId();
        UUID tenantId = contract.getRental().getTenant() != null ?
                contract.getRental().getTenant().getId() : null;

        if (!ownerId.equals(currentUserId) && !currentUserId.equals(tenantId) && !isAdmin(currentUserId)) {
            throw new ForbiddenException("You are not authorized to view this contract");
        }

        return toResponse(contract);
    }

    /**
     * Récupère tous les contrats du propriétaire connecté
     * Les ADMIN voient tous les contrats
     */
    public List<ContractResponse> getMyContracts() {
        UUID currentUserId = AuthService.getCurrentUserId();

        // Récupérer l'utilisateur pour vérifier son rôle
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId.toString()));

        // Si ADMIN, retourner tous les contrats
        if (currentUser.getRole() == Role.ADMIN) {
            log.info("Admin user fetching all contracts");
            return contractRepository.findAll().stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        }

        // Sinon, retourner seulement les contrats du propriétaire
        return contractRepository.findByOwnerId(currentUserId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupère les contrats d'une location
     */
    public List<ContractResponse> getContractsByRental(UUID rentalId) {
        UUID currentUserId = AuthService.getCurrentUserId();

        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new ResourceNotFoundException("Rental", "id", rentalId.toString()));

        // Vérifier les permissions
        UUID ownerId = rental.getProperty().getOwner().getId();
        if (!ownerId.equals(currentUserId) && !isAdmin(currentUserId)) {
            throw new ForbiddenException("You are not authorized to view contracts for this rental");
        }

        return contractRepository.findByRentalId(rentalId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupère toutes les versions d'un contrat
     */
    public List<ContractVersionResponse> getContractVersions(UUID contractId) {
        UUID currentUserId = AuthService.getCurrentUserId();

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract", "id", contractId.toString()));

        // Vérifier les permissions
        UUID ownerId = contract.getRental().getProperty().getOwner().getId();
        if (!ownerId.equals(currentUserId) && !isAdmin(currentUserId)) {
            throw new ForbiddenException("You are not authorized to view versions for this contract");
        }

        return contractVersionRepository.findByContractIdOrderByVersionDesc(contractId).stream()
                .map(v -> {
                    String versionDocUrl = null;
                    if (v.getFileKey() != null) {
                        versionDocUrl = storageService.generatePresignedUrl(v.getFileKey());
                    }
                    return ContractVersionResponse.from(v, versionDocUrl);
                })
                .collect(Collectors.toList());
    }

    /**
     * Télécharge le PDF de la version actuelle d'un contrat
     */
    public byte[] downloadPdf(UUID contractId) throws IOException {
        UUID currentUserId = AuthService.getCurrentUserId();

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract", "id", contractId.toString()));

        // Vérifier les permissions (propriétaire ou locataire)
        UUID ownerId = contract.getRental().getProperty().getOwner().getId();
        UUID tenantId = contract.getRental().getTenant() != null ?
                contract.getRental().getTenant().getId() : null;

        if (!ownerId.equals(currentUserId) && !currentUserId.equals(tenantId) && !isAdmin(currentUserId)) {
            throw new ForbiddenException("You are not authorized to download this contract");
        }

        // Récupérer la version actuelle
        ContractVersion currentVersion = contractVersionRepository
                .findByContractIdAndVersion(contract.getId(), contract.getCurrentVersion())
                .orElseThrow(() -> new ResourceNotFoundException("ContractVersion", "contractId", contractId.toString()));

        if (currentVersion.getFileKey() == null) {
            throw new BadRequestException("No PDF available for this contract");
        }

        log.info("Downloading PDF for contract: {} version: {}", contract.getContractNumber(), contract.getCurrentVersion());
        return storageService.downloadFile(currentVersion.getFileKey());
    }

    /**
     * Supprime un contrat (soft delete en changeant le statut)
     */
    @Transactional
    public void deleteContract(UUID contractId) {
        UUID currentUserId = AuthService.getCurrentUserId();

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract", "id", contractId.toString()));

        // Vérifier les permissions
        UUID ownerId = contract.getRental().getProperty().getOwner().getId();
        if (!ownerId.equals(currentUserId) && !isAdmin(currentUserId)) {
            throw new ForbiddenException("You are not authorized to delete this contract");
        }

        // On ne peut supprimer que les contrats en DRAFT
        if (contract.getStatus() != ContractStatus.DRAFT) {
            throw new BadRequestException("Only draft contracts can be deleted");
        }

        contract.setStatus(ContractStatus.CANCELLED);
        contractRepository.save(contract);

        log.info("Contract cancelled: {}", contract.getContractNumber());
    }

    /**
     * Génère un numéro de contrat unique
     */
    private String generateContractNumber() {
        String year = String.valueOf(LocalDateTime.now().getYear());
        String random = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return String.format("CTR-%s-%s", year, random);
    }

    /**
     * Convertit un Contract en ContractResponse avec URL présignée générée à la volée
     */
    private ContractResponse toResponse(Contract contract) {
        // Récupérer la version actuelle pour obtenir le fileKey
        ContractVersion currentVersion = contractVersionRepository
                .findByContractIdAndVersion(contract.getId(), contract.getCurrentVersion())
                .orElse(null);

        String documentUrl = null;
        if (currentVersion != null && currentVersion.getFileKey() != null) {
            // Générer l'URL présignée à la volée
            documentUrl = storageService.generatePresignedUrl(currentVersion.getFileKey());
        }

        // Créer le response avec l'URL régénérée
        return new ContractResponse(
                contract.getId(),
                contract.getContractNumber(),
                contract.getRental().getId(),
                contract.getRental().getProperty().getName(),
                UserDisplayUtils.getFullName(contract.getRental().getTenant()),
                contract.getStatus(),
                contract.getCurrentVersion(),
                documentUrl,  // URL présignée générée à la volée
                contract.getCreatedAt(),
                contract.getUpdatedAt(),
                contract.getSentAt(),
                contract.getSignedAt()
        );
    }

    private boolean isAdmin(UUID userId) {
        User user = userRepository.findById(userId).orElse(null);
        return user != null && user.getRole() == Role.ADMIN;
    }

}
