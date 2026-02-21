package com.howners.gestion.service.tenant;

import com.howners.gestion.domain.contract.Contract;
import com.howners.gestion.domain.document.Document;
import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.request.UpdateProfileRequest;
import com.howners.gestion.dto.contract.ContractResponse;
import com.howners.gestion.dto.document.DocumentResponse;
import com.howners.gestion.dto.response.RentalResponse;
import com.howners.gestion.dto.response.UserResponse;
import com.howners.gestion.exception.ResourceNotFoundException;
import com.howners.gestion.repository.ContractRepository;
import com.howners.gestion.repository.DocumentRepository;
import com.howners.gestion.repository.RentalRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.service.auth.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TenantService {

    private final UserRepository userRepository;
    private final RentalRepository rentalRepository;
    private final ContractRepository contractRepository;
    private final DocumentRepository documentRepository;

    /**
     * Récupérer le profil du locataire connecté
     */
    public UserResponse getMyProfile() {
        UUID currentUserId = AuthService.getCurrentUserId();
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", currentUserId));
        
        return UserResponse.from(user);
    }

    /**
     * Mettre à jour le profil du locataire
     */
    @Transactional
    public UserResponse updateMyProfile(UpdateProfileRequest request) {
        UUID currentUserId = AuthService.getCurrentUserId();
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", currentUserId));

        // Mise à jour des informations
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());
        
        if (request.phone() != null && !request.phone().isBlank()) {
            user.setPhone(request.phone());
        }

        user = userRepository.save(user);
        log.info("Tenant profile updated: {}", user.getEmail());

        return UserResponse.from(user);
    }

    /**
     * Récupérer toutes les locations du locataire connecté
     */
    public List<RentalResponse> getMyRentals() {
        UUID currentUserId = AuthService.getCurrentUserId();
        
        List<Rental> rentals = rentalRepository.findByTenantId(currentUserId);
        log.info("Found {} rentals for tenant {}", rentals.size(), currentUserId);

        return rentals.stream()
                .map(RentalResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer tous les contrats du locataire connecté
     */
    public List<ContractResponse> getMyContracts() {
        UUID currentUserId = AuthService.getCurrentUserId();
        
        // Récupérer les locations du locataire
        List<Rental> rentals = rentalRepository.findByTenantId(currentUserId);
        List<UUID> rentalIds = rentals.stream()
                .map(Rental::getId)
                .collect(Collectors.toList());

        if (rentalIds.isEmpty()) {
            return List.of();
        }

        // Récupérer les contrats associés à ces locations
        List<Contract> contracts = contractRepository.findByRentalIdIn(rentalIds);
        log.info("Found {} contracts for tenant {}", contracts.size(), currentUserId);

        return contracts.stream()
                .map(ContractResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer tous les documents du locataire connecté
     */
    public List<DocumentResponse> getMyDocuments() {
        UUID currentUserId = AuthService.getCurrentUserId();
        
        List<Document> documents = documentRepository.findByUploaderId(currentUserId);
        log.info("Found {} documents for tenant {}", documents.size(), currentUserId);

        return documents.stream()
                .map(DocumentResponse::from)
                .collect(Collectors.toList());
    }
}
