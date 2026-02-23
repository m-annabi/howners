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
import com.howners.gestion.exception.BadRequestException;
import com.howners.gestion.exception.ConflictException;
import com.howners.gestion.exception.ForbiddenException;
import com.howners.gestion.exception.ResourceNotFoundException;
import com.howners.gestion.repository.ContractRepository;
import com.howners.gestion.repository.DocumentRepository;
import com.howners.gestion.repository.SignatureRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.service.auth.AuthService;
import com.howners.gestion.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

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

    @Transactional
    public SignatureResponse createSignature(CreateSignatureRequest request) {
        UUID currentUserId = AuthService.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", currentUserId));

        Contract contract = contractRepository.findById(request.contractId())
                .orElseThrow(() -> new ResourceNotFoundException("Contrat", "id", request.contractId()));

        if (contract.getStatus() != ContractStatus.SENT) {
            throw new BadRequestException("Le contrat doit être en statut 'ENVOYÉ' pour pouvoir être signé");
        }

        UUID tenantId = contract.getRental().getTenant() != null
                ? contract.getRental().getTenant().getId() : null;

        if (!currentUserId.equals(tenantId)) {
            throw new ForbiddenException("Seul le locataire peut signer ce contrat");
        }

        boolean alreadySigned = signatureRepository.findByContractId(contract.getId()).stream()
                .anyMatch(s -> s.getSigner().getId().equals(currentUserId)
                        && s.getStatus() == SignatureStatus.SIGNED);

        if (alreadySigned) {
            throw new ConflictException("Vous avez déjà signé ce contrat");
        }

        // Décoder et uploader l'image de signature
        byte[] signatureImageBytes = Base64.getDecoder().decode(request.signatureData());
        String fileName = String.format("signature_%s_%s.png", contract.getContractNumber(), currentUserId);
        String signatureImageKey = storageService.uploadFile(signatureImageBytes, fileName, "image/png");
        log.info("Signature image uploaded: {}", signatureImageKey);

        Document signatureDocument = Document.builder()
                .fileName("signature_" + currentUser.getEmail() + ".png")
                .filePath(signatureImageKey)
                .fileKey(signatureImageKey)
                .fileUrl(storageService.generatePresignedUrl(signatureImageKey))
                .fileSize((long) signatureImageBytes.length)
                .mimeType("image/png")
                .documentType(DocumentType.SIGNATURE)
                .uploader(currentUser)
                .uploadedAt(LocalDateTime.now())
                .build();

        signatureDocument = documentRepository.save(signatureDocument);

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

        contract.setStatus(ContractStatus.SIGNED);
        contract.setSignedAt(LocalDateTime.now());
        contractRepository.save(contract);

        log.info("Contract {} signed by user {}", contract.getContractNumber(), currentUser.getEmail());
        return SignatureResponse.from(signature);
    }

    public List<SignatureResponse> getContractSignatures(UUID contractId) {
        UUID currentUserId = AuthService.getCurrentUserId();

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract", "id", contractId));

        UUID ownerId = contract.getRental().getProperty().getOwner().getId();
        UUID tenantId = contract.getRental().getTenant() != null
                ? contract.getRental().getTenant().getId() : null;

        if (!ownerId.equals(currentUserId) && !currentUserId.equals(tenantId)) {
            throw new ForbiddenException("You are not authorized to view signatures for this contract");
        }

        return signatureRepository.findByContractId(contractId).stream()
                .map(SignatureResponse::from)
                .toList();
    }

    public List<SignatureResponse> getMySignatures() {
        UUID currentUserId = AuthService.getCurrentUserId();
        return signatureRepository.findBySignerId(currentUserId).stream()
                .map(SignatureResponse::from)
                .toList();
    }

    public SignatureResponse getSignature(UUID signatureId) {
        UUID currentUserId = AuthService.getCurrentUserId();

        Signature signature = signatureRepository.findById(signatureId)
                .orElseThrow(() -> new ResourceNotFoundException("Signature", "id", signatureId));

        UUID ownerId = signature.getContract().getRental().getProperty().getOwner().getId();
        UUID signerId = signature.getSigner().getId();

        if (!ownerId.equals(currentUserId) && !signerId.equals(currentUserId)) {
            throw new ForbiddenException("You are not authorized to view this signature");
        }

        return SignatureResponse.from(signature);
    }
}
