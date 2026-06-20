package com.howners.gestion.service.rental;

import com.howners.gestion.domain.audit.AuditAction;
import com.howners.gestion.domain.listing.Listing;
import com.howners.gestion.domain.listing.ListingStatus;
import com.howners.gestion.domain.property.Property;
import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.domain.rental.RentalStatus;
import com.howners.gestion.domain.user.Role;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.listing.ListingResponse;
import com.howners.gestion.dto.request.CreateRentalRequest;
import com.howners.gestion.dto.request.ExitTenantRequest;
import com.howners.gestion.dto.request.PublishRentalRequest;
import com.howners.gestion.dto.request.UpdateRentalRequest;
import com.howners.gestion.dto.response.RentalResponse;
import com.howners.gestion.dto.response.UserResponse;
import com.howners.gestion.exception.BadRequestException;
import com.howners.gestion.exception.BusinessException;
import com.howners.gestion.exception.ResourceNotFoundException;
import com.howners.gestion.repository.ContractRepository;
import com.howners.gestion.repository.ListingRepository;
import com.howners.gestion.repository.PaymentRepository;
import com.howners.gestion.repository.PropertyRepository;
import com.howners.gestion.repository.RentalRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.service.audit.AuditService;
import com.howners.gestion.service.auth.AuthService;
import com.howners.gestion.service.email.EmailService;
import com.howners.gestion.service.subscription.FeatureGateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final AuditService auditService;
    private final ContractRepository contractRepository;
    private final PaymentRepository paymentRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final FeatureGateService featureGateService;
    private final ListingRepository listingRepository;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Transactional(readOnly = true)
    public List<RentalResponse> findAllByCurrentUser() {
        UUID currentUserId = AuthService.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId.toString()));

        List<Rental> rentals;
        if (currentUser.getRole() == Role.ADMIN) {
            rentals = rentalRepository.findAll();
        } else if (currentUser.getRole() == Role.TENANT) {
            rentals = rentalRepository.findByTenantId(currentUserId);
        } else {
            rentals = rentalRepository.findByOwnerId(currentUserId);
        }

        return rentals.stream().map(RentalResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<RentalResponse> findAllByCurrentUser(Pageable pageable) {
        UUID currentUserId = AuthService.getCurrentUserId();
        log.debug("Finding all rentals (paginated) for user {}", currentUserId);

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId.toString()));

        Page<Rental> rentals;
        if (currentUser.getRole() == Role.ADMIN) {
            rentals = rentalRepository.findAll(pageable);
        } else if (currentUser.getRole() == Role.TENANT) {
            rentals = rentalRepository.findByTenantId(currentUserId, pageable);
        } else {
            rentals = rentalRepository.findByOwnerId(currentUserId, pageable);
        }

        return rentals.map(RentalResponse::from);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findMyTenants() {
        UUID currentUserId = AuthService.getCurrentUserId();
        return userRepository.findTenantsByOwnerId(currentUserId).stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RentalResponse> findByTenant(UUID tenantId) {
        UUID currentUserId = AuthService.getCurrentUserId();
        return rentalRepository.findByOwnerIdAndTenantId(currentUserId, tenantId)
                .stream().map(RentalResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public RentalResponse findById(UUID rentalId) {
        return RentalResponse.from(findRentalByIdAndCheckAccess(rentalId));
    }

    @Transactional
    public RentalResponse create(CreateRentalRequest request) {
        UUID currentUserId = AuthService.getCurrentUserId();
        featureGateService.assertCanCreate(currentUserId, "RENTALS");
        log.info("Creating rental for property {} by user {}", request.propertyId(), currentUserId);

        Property property = propertyRepository.findById(request.propertyId())
                .orElseThrow(() -> new ResourceNotFoundException("Property", "id", request.propertyId().toString()));

        if (!property.getOwner().getId().equals(currentUserId)) {
            throw new BusinessException("You don't have permission to create a rental for this property");
        }

        Rental rental = Rental.builder()
                .property(property)
                .status(RentalStatus.VACANT)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .monthlyRent(request.monthlyRent())
                .currency(request.currency())
                .depositAmount(request.depositAmount())
                .charges(request.charges())
                .paymentDay(request.paymentDay())
                .assuranceExpiration(request.assuranceExpiration())
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

        LocalDate startDate = request.startDate() != null ? request.startDate() : rental.getStartDate();
        LocalDate endDate = request.endDate() != null ? request.endDate() : rental.getEndDate();
        if (endDate != null && startDate != null && endDate.isBefore(startDate)) {
            throw new BusinessException("End date must be after start date");
        }

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
        if (request.assuranceExpiration() != null) {
            rental.setAssuranceExpiration(request.assuranceExpiration());
        }

        rental = rentalRepository.save(rental);
        return RentalResponse.from(rental);
    }

    @Transactional
    public void delete(UUID rentalId) {
        Rental rental = findRentalByIdAndCheckAccess(rentalId);

        long contractCount = contractRepository.findByRentalId(rentalId).size();
        if (contractCount > 0) {
            throw new BusinessException(
                String.format("Cannot delete rental. %d contract(s) are associated.", contractCount));
        }

        long pendingPayments = paymentRepository.findByRentalIdAndStatus(
                rentalId, com.howners.gestion.domain.payment.PaymentStatus.PENDING).size();
        if (pendingPayments > 0) {
            throw new BusinessException(
                String.format("Cannot delete rental. %d pending payment(s) exist.", pendingPayments));
        }

        rentalRepository.delete(rental);
        log.info("Rental {} deleted", rentalId);
    }

    @Transactional
    public ListingResponse publish(UUID rentalId, PublishRentalRequest request) {
        Rental rental = findRentalByIdAndCheckAccess(rentalId);

        if (rental.getStatus() != RentalStatus.VACANT) {
            throw new BadRequestException("Only a VACANT rental can be published as a listing");
        }

        Listing listing = listingRepository.findByRentalId(rentalId).orElse(null);
        if (listing == null) {
            listing = Listing.builder()
                    .property(rental.getProperty())
                    .rental(rental)
                    .title(request.title())
                    .description(request.description())
                    .pricePerMonth(rental.getMonthlyRent())
                    .currency(rental.getCurrency())
                    .availableFrom(request.availableFrom())
                    .status(ListingStatus.PUBLISHED)
                    .publishedAt(java.time.LocalDateTime.now())
                    .build();
        } else {
            listing.setTitle(request.title());
            listing.setDescription(request.description());
            listing.setPricePerMonth(rental.getMonthlyRent());
            listing.setAvailableFrom(request.availableFrom());
            listing.setStatus(ListingStatus.PUBLISHED);
            listing.setPublishedAt(java.time.LocalDateTime.now());
        }
        listing = listingRepository.save(listing);

        rental.setStatus(RentalStatus.LISTED);
        rentalRepository.save(rental);

        log.info("Rental {} published as listing {}", rentalId, listing.getId());
        auditService.logAction(AuditAction.UPDATE, "Rental", rentalId);
        return ListingResponse.from(listing);
    }

    @Transactional
    public RentalResponse exitTenant(UUID rentalId, ExitTenantRequest request) {
        Rental rental = findRentalByIdAndCheckAccess(rentalId);

        if (rental.getStatus() != RentalStatus.ACTIVE) {
            throw new BadRequestException("Only an ACTIVE rental can have a tenant exit planned");
        }
        if (rental.getTenant() == null) {
            throw new BadRequestException("No tenant assigned to this rental");
        }

        rental.setEndDate(request.exitDate());
        rental.setStatus(RentalStatus.EXITING);

        Listing listing = listingRepository.findByRentalId(rentalId).orElse(null);
        if (listing == null) {
            listing = Listing.builder()
                    .property(rental.getProperty())
                    .rental(rental)
                    .title(rental.getProperty().getName())
                    .pricePerMonth(rental.getMonthlyRent())
                    .currency(rental.getCurrency())
                    .availableFrom(request.exitDate())
                    .status(ListingStatus.PUBLISHED)
                    .publishedAt(java.time.LocalDateTime.now())
                    .build();
        } else {
            listing.setPricePerMonth(rental.getMonthlyRent());
            listing.setAvailableFrom(request.exitDate());
            listing.setStatus(ListingStatus.PUBLISHED);
            listing.setPublishedAt(java.time.LocalDateTime.now());
        }
        listingRepository.save(listing);

        rental = rentalRepository.save(rental);
        log.info("Rental {} planned exit on {}, listing published", rentalId, request.exitDate());
        auditService.logAction(AuditAction.UPDATE, "Rental", rentalId);
        return RentalResponse.from(rental);
    }

    @Transactional
    public RentalResponse confirmExit(UUID rentalId) {
        Rental rental = findRentalByIdAndCheckAccess(rentalId);

        if (rental.getStatus() != RentalStatus.EXITING) {
            throw new BadRequestException("Only an EXITING rental can be confirmed");
        }

        com.howners.gestion.domain.user.User nextTenant = null;
        if (rental.getApplication() != null &&
                rental.getApplication().getStatus() == com.howners.gestion.domain.application.ApplicationStatus.ACCEPTED) {
            nextTenant = rental.getApplication().getApplicant();
        }

        rental.setTenant(nextTenant);
        rental.setApplication(nextTenant != null ? rental.getApplication() : null);
        rental.setStartDate(rental.getEndDate());
        rental.setEndDate(null);

        if (nextTenant != null) {
            rental.setStatus(RentalStatus.ACTIVE);
            log.info("Rental {} exit confirmed, new tenant {} activated", rentalId, nextTenant.getId());
        } else {
            rental.setStatus(RentalStatus.VACANT);
            log.info("Rental {} exit confirmed, now vacant", rentalId);
        }

        rental = rentalRepository.save(rental);
        auditService.logAction(AuditAction.UPDATE, "Rental", rentalId);
        return RentalResponse.from(rental);
    }

    public Rental findRentalByIdAndCheckAccess(UUID rentalId) {
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new ResourceNotFoundException("Rental", "id", rentalId.toString()));

        UUID currentUserId = AuthService.getCurrentUserId();
        boolean isOwner = rental.getProperty().getOwner().getId().equals(currentUserId);
        boolean isTenant = rental.getTenant() != null && rental.getTenant().getId().equals(currentUserId);

        if (!isOwner && !isTenant) {
            throw new BusinessException("You don't have permission to access this rental");
        }

        return rental;
    }
}
