package com.howners.gestion.service.rental;

import com.howners.gestion.domain.audit.AuditAction;
import com.howners.gestion.domain.property.Property;
import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.domain.rental.RentalStatus;
import com.howners.gestion.domain.user.Role;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.request.CreateRentalRequest;
import com.howners.gestion.dto.request.UpdateRentalRequest;
import com.howners.gestion.dto.response.RentalResponse;
import com.howners.gestion.dto.response.UserResponse;
import com.howners.gestion.dto.email.WelcomeTenantEmailData;
import com.howners.gestion.exception.BusinessException;
import com.howners.gestion.exception.ResourceNotFoundException;
import com.howners.gestion.repository.ContractRepository;
import com.howners.gestion.repository.PropertyRepository;
import com.howners.gestion.repository.RentalRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.service.auth.AuthService;
import com.howners.gestion.service.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RentalService {

    private final RentalRepository rentalRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final com.howners.gestion.service.audit.AuditService auditService;
    private final ContractRepository contractRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Transactional(readOnly = true)
    public List<RentalResponse> findAllByCurrentUser() {
        UUID currentUserId = AuthService.getCurrentUserId();
        log.debug("Finding all rentals for user {}", currentUserId);

        List<Rental> rentals = rentalRepository.findByOwnerId(currentUserId);
        return rentals.stream()
                .map(RentalResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findMyTenants() {
        UUID currentUserId = AuthService.getCurrentUserId();
        return userRepository.findTenantsByOwnerId(currentUserId).stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RentalResponse findById(UUID rentalId) {
        Rental rental = findRentalByIdAndCheckAccess(rentalId);
        return RentalResponse.from(rental);
    }

    @Transactional
    public RentalResponse create(CreateRentalRequest request) {
        UUID currentUserId = AuthService.getCurrentUserId();
        log.info("Creating rental for property {} by user {}", request.propertyId(), currentUserId);

        // Vérifier que la propriété existe et appartient à l'utilisateur
        Property property = propertyRepository.findById(request.propertyId())
                .orElseThrow(() -> new ResourceNotFoundException("Property", "id", request.propertyId().toString()));

        if (!property.getOwner().getId().equals(currentUserId)) {
            throw new BusinessException("You don't have permission to create a rental for this property");
        }

        // Valider les dates
        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new BusinessException("End date must be after start date");
        }

        // Gérer le locataire
        User tenant = null;
        if (request.tenantId() != null) {
            // Locataire existant
            tenant = userRepository.findById(request.tenantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Tenant", "id", request.tenantId().toString()));
        } else if (request.tenantEmail() != null) {
            // Créer un nouveau locataire
            tenant = createTenant(request, property);
        }

        // Créer la location
        Rental rental = Rental.builder()
                .property(property)
                .tenant(tenant)
                .rentalType(request.rentalType())
                .status(RentalStatus.PENDING)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .monthlyRent(request.monthlyRent())
                .currency(request.currency())
                .depositAmount(request.depositAmount())
                .charges(request.charges())
                .paymentDay(request.paymentDay())
                .build();

        rental = rentalRepository.save(rental);
        log.info("Rental created with id {}", rental.getId());
        auditService.logAction(AuditAction.CREATE, "Rental", rental.getId());

        return RentalResponse.from(rental);
    }

    @Transactional
    public RentalResponse update(UUID rentalId, UpdateRentalRequest request) {
        Rental rental = findRentalByIdAndCheckAccess(rentalId);
        log.info("Updating rental {}", rentalId);

        // Valider les dates si modifiées
        LocalDate startDate = request.startDate() != null ? request.startDate() : rental.getStartDate();
        LocalDate endDate = request.endDate() != null ? request.endDate() : rental.getEndDate();
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new BusinessException("End date must be after start date");
        }

        // Mettre à jour les champs
        if (request.rentalType() != null) {
            rental.setRentalType(request.rentalType());
        }
        if (request.status() != null) {
            rental.setStatus(request.status());
        }
        if (request.startDate() != null) {
            rental.setStartDate(request.startDate());
        }
        if (request.endDate() != null) {
            rental.setEndDate(request.endDate());
        }
        if (request.monthlyRent() != null) {
            rental.setMonthlyRent(request.monthlyRent());
        }
        if (request.currency() != null) {
            rental.setCurrency(request.currency());
        }
        if (request.depositAmount() != null) {
            rental.setDepositAmount(request.depositAmount());
        }
        if (request.charges() != null) {
            rental.setCharges(request.charges());
        }
        if (request.paymentDay() != null) {
            rental.setPaymentDay(request.paymentDay());
        }

        rental = rentalRepository.save(rental);
        log.info("Rental {} updated successfully", rentalId);

        return RentalResponse.from(rental);
    }

    @Transactional
    public void delete(UUID rentalId) {
        Rental rental = findRentalByIdAndCheckAccess(rentalId);
        log.info("Deleting rental {}", rentalId);

        // Vérifier qu'il n'y a pas de contrats liés
        long contractCount = contractRepository.findByRentalId(rentalId).size();
        if (contractCount > 0) {
            throw new BusinessException(
                String.format("Cannot delete rental. %d contract(s) are associated with this rental. Please delete contracts first.", contractCount)
            );
        }

        // TODO: Vérifier qu'il n'y a pas de paiements en cours
        rentalRepository.delete(rental);
        log.info("Rental {} deleted successfully", rentalId);
    }

    private Rental findRentalByIdAndCheckAccess(UUID rentalId) {
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new ResourceNotFoundException("Rental", "id", rentalId.toString()));

        UUID currentUserId = AuthService.getCurrentUserId();

        // Le propriétaire du bien ou le locataire peut accéder à la location
        boolean isOwner = rental.getProperty().getOwner().getId().equals(currentUserId);
        boolean isTenant = rental.getTenant() != null && rental.getTenant().getId().equals(currentUserId);

        if (!isOwner && !isTenant) {
            throw new BusinessException("You don't have permission to access this rental");
        }

        return rental;
    }

    private User createTenant(CreateRentalRequest request, Property property) {
        // Vérifier si l'email existe déjà
        if (userRepository.existsByEmail(request.tenantEmail())) {
            throw new BusinessException("A user with this email already exists");
        }

        log.info("Creating new tenant with email {}", request.tenantEmail());

        // Générer un mot de passe temporaire
        String tempPassword = UUID.randomUUID().toString().substring(0, 12);

        User tenant = User.builder()
                .email(request.tenantEmail())
                .passwordHash(passwordEncoder.encode(tempPassword))
                .firstName(request.tenantFirstName())
                .lastName(request.tenantLastName())
                .phone(request.tenantPhone())
                .role(Role.TENANT)
                .enabled(true)
                .build();

        tenant = userRepository.save(tenant);
        log.info("Tenant created with id {}", tenant.getId());

        // Envoyer un email de bienvenue au locataire avec ses identifiants
        try {
            String ownerName = property.getOwner().getFirstName() + " " + property.getOwner().getLastName();
            String tenantName = request.tenantFirstName() + " " + request.tenantLastName();
            String loginUrl = frontendUrl + "/login";

            WelcomeTenantEmailData emailData = WelcomeTenantEmailData.builder()
                    .recipientEmail(request.tenantEmail())
                    .recipientName(tenantName)
                    .ownerName(ownerName)
                    .propertyName(property.getName())
                    .tempPassword(tempPassword)
                    .loginUrl(loginUrl)
                    .build();

            emailService.sendWelcomeTenantEmail(emailData);
            log.info("Welcome email sent to tenant {}", request.tenantEmail());
        } catch (Exception e) {
            log.error("Failed to send welcome email to tenant {}, but tenant was created successfully", request.tenantEmail(), e);
        }

        return tenant;
    }
}
