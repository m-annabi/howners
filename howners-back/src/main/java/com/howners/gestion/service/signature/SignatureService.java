package com.howners.gestion.service.signature;

import com.howners.gestion.domain.contract.Contract;
import com.howners.gestion.domain.contract.ContractStatus;
import com.howners.gestion.domain.document.Document;
import com.howners.gestion.domain.document.DocumentType;
import com.howners.gestion.domain.signature.Signature;
import com.howners.gestion.domain.signature.SignatureStatus;
import com.howners.gestion.domain.signature.SignatureType;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.signature.CreateSignatureRequest;
import com.howners.gestion.dto.signature.SignatureResponse;
import com.howners.gestion.repository.ContractRepository;
import com.howners.gestion.repository.DocumentRepository;
import com.howners.gestion.repository.SignatureRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.service.auth.AuthService;
import com.howners.gestion.service.storage.StorageService;
import com.howners.gestion.exception.BadRequestException;
import com.howners.gestion.exception.ConflictException;
import com.howners.gestion.exception.ForbiddenException;
import com.howners.gestion.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SignatureService {

    private final SignatureRepository signatureRepository;
    private final ContractRepository contractRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;

    /**
     * Créer une signature pour un contrat
     */
    @Transactional
    public SignatureResponse createSignature(CreateSignatureRequest request) {
        UUID currentUserId = AuthService.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", currentUserId));

        // Récupérer le contrat
        Contract contract = contractRepository.findById(request.contractId())
                .orElseThrow(() -> new ResourceNotFoundException("Contrat", "id", request.contractId()));

        // Vérifier que le contrat est en statut SENT
        if (contract.getStatus() != ContractStatus.SENT) {
            throw new BadRequestException("Le contrat doit être en statut 'ENVOYÉ' pour pouvoir être signé");
        }

        // Vérifier que l'utilisateur est le locataire du contrat
        UUID tenantId = contract.getRental().getTenant() != null ?
                contract.getRental().getTenant().getId() : null;

        if (!currentUserId.equals(tenantId)) {
            throw new ForbiddenException("Seul le locataire peut signer ce contrat");
        }

        // Vérifier qu'il n'y a pas déjà une signature pour ce contrat et cet utilisateur
        List<Signature> existingSignatures = signatureRepository.findByContractId(contract.getId());
        boolean alreadySigned = existingSignatures.stream()
                .anyMatch(s -> s.getSigner().getId().equals(currentUserId) &&
                        s.getStatus() == SignatureStatus.SIGNED);

        if (alreadySigned) {
            throw new ConflictException("Vous avez déjà signé ce contrat");
        }

        // Décoder et uploader l'image de signature
        String signatureImageKey = null;
        try {
            byte[] signatureImageBytes = Base64.getDecoder().decode(request.signatureData());
            String fileName = String.format("signature_%s_%s.png",
                    contract.getContractNumber(),
                    currentUserId);
            signatureImageKey = storageService.uploadFile(signatureImageBytes, fileName, "image/png");
            log.info("Signature image uploaded: {}", signatureImageKey);
        } catch (Exception e) {
            log.error("Error uploading signature image: {}", e.getMessage());
            throw new RuntimeException("Failed to save signature image", e);
        }

        // Créer le document de signature
        String signatureFileName = "signature_" + currentUser.getEmail() + ".png";
        byte[] signatureBytes = Base64.getDecoder().decode(request.signatureData());

        Document signatureDocument = Document.builder()
                .fileName(signatureFileName)
                .filePath(signatureImageKey)  // Pour compatibilité
                .fileKey(signatureImageKey)   // Clé MinIO
                .fileUrl(storageService.generatePresignedUrl(signatureImageKey))
                .fileSize((long) signatureBytes.length)
                .mimeType("image/png")
                .documentType(DocumentType.SIGNATURE)
                .uploader(currentUser)
                .uploadedAt(LocalDateTime.now())
                .build();

        signatureDocument = documentRepository.save(signatureDocument);

        // Créer la signature
        Signature signature = Signature.builder()
                .document(signatureDocument)
                .contract(contract)
                .signer(currentUser)
                .signatureType(SignatureType.SIMPLE)
                .provider("INTERNAL")
                .signatureData(request.signatureData())
                .ipAddress(request.ipAddress())
                .userAgent(request.userAgent())
                .signedAt(LocalDateTime.now())
                .status(SignatureStatus.SIGNED)
                .build();

        signature = signatureRepository.save(signature);

        // Mettre à jour le statut du contrat
        contract.setStatus(ContractStatus.SIGNED);
        contract.setSignedAt(LocalDateTime.now());
        contractRepository.save(contract);

        log.info("Contract {} signed by user {}", contract.getContractNumber(), currentUser.getEmail());

        return SignatureResponse.from(signature);
    }

    /**
     * Récupérer les signatures d'un contrat
     */
    public List<SignatureResponse> getContractSignatures(UUID contractId) {
        UUID currentUserId = AuthService.getCurrentUserId();

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        // Vérifier les permissions (propriétaire ou locataire)
        UUID ownerId = contract.getRental().getProperty().getOwner().getId();
        UUID tenantId = contract.getRental().getTenant() != null ?
                contract.getRental().getTenant().getId() : null;

        if (!ownerId.equals(currentUserId) && !currentUserId.equals(tenantId)) {
            throw new RuntimeException("You are not authorized to view signatures for this contract");
        }

        return signatureRepository.findByContractId(contractId).stream()
                .map(SignatureResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer les signatures d'un utilisateur
     */
    public List<SignatureResponse> getMySignatures() {
        UUID currentUserId = AuthService.getCurrentUserId();

        return signatureRepository.findBySignerId(currentUserId).stream()
                .map(SignatureResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer une signature par ID
     */
    public SignatureResponse getSignature(UUID signatureId) {
        UUID currentUserId = AuthService.getCurrentUserId();

        Signature signature = signatureRepository.findById(signatureId)
                .orElseThrow(() -> new RuntimeException("Signature not found"));

        // Vérifier les permissions
        UUID ownerId = signature.getContract().getRental().getProperty().getOwner().getId();
        UUID signerId = signature.getSigner().getId();

        if (!ownerId.equals(currentUserId) && !signerId.equals(currentUserId)) {
            throw new RuntimeException("You are not authorized to view this signature");
        }

        return SignatureResponse.from(signature);
    }
}
