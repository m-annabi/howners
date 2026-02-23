package com.howners.gestion.service.application;

import com.howners.gestion.domain.application.Application;
import com.howners.gestion.domain.application.ApplicationStatus;
import com.howners.gestion.domain.audit.AuditAction;
import com.howners.gestion.domain.listing.Listing;
import com.howners.gestion.domain.listing.ListingStatus;
import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.domain.rental.RentalStatus;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.application.ApplicationResponse;
import com.howners.gestion.dto.application.CreateApplicationRequest;
import com.howners.gestion.dto.application.CreateRentalFromApplicationRequest;
import com.howners.gestion.dto.application.ReviewApplicationRequest;
import com.howners.gestion.dto.document.DocumentResponse;
import com.howners.gestion.dto.email.ApplicationReviewedEmailData;
import com.howners.gestion.dto.response.RentalResponse;
import com.howners.gestion.exception.BadRequestException;
import com.howners.gestion.exception.ResourceNotFoundException;
import com.howners.gestion.repository.ApplicationRepository;
import com.howners.gestion.repository.DocumentRepository;
import com.howners.gestion.repository.ListingRepository;
import com.howners.gestion.repository.RentalRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.service.audit.AuditService;
import com.howners.gestion.service.auth.AuthService;
import com.howners.gestion.service.document.DocumentService;
import com.howners.gestion.service.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final DocumentService documentService;
    private final RentalRepository rentalRepository;
    private final EmailService emailService;
    private final AuditService auditService;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Transactional
    public ApplicationResponse submit(CreateApplicationRequest request) {
        UUID currentUserId = AuthService.getCurrentUserId();
        User applicant = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Listing listing = listingRepository.findById(request.listingId())
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));

        if (listing.getStatus() != ListingStatus.PUBLISHED) {
            throw new BadRequestException("Listing is not published");
        }

        if (applicationRepository.existsByListingIdAndApplicantId(request.listingId(), currentUserId)) {
            throw new BadRequestException("You have already applied to this listing");
        }

        Application application = Application.builder()
                .listing(listing)
                .applicant(applicant)
                .coverLetter(request.coverLetter())
                .desiredMoveIn(request.desiredMoveIn())
                .status(ApplicationStatus.SUBMITTED)
                .build();

        application = applicationRepository.save(application);
        log.info("Application submitted by {} for listing {}", currentUserId, request.listingId());
        return ApplicationResponse.from(application);
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> findMyApplications() {
        UUID currentUserId = AuthService.getCurrentUserId();
        return applicationRepository.findByApplicantIdOrderByCreatedAtDesc(currentUserId)
                .stream().map(this::toResponseWithDocuments).toList();
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> findByListingId(UUID listingId) {
        return applicationRepository.findByListingIdOrderByCreatedAtDesc(listingId)
                .stream().map(this::toResponseWithDocuments).toList();
    }

    @Transactional
    public List<ApplicationResponse> findReceivedApplications() {
        UUID currentUserId = AuthService.getCurrentUserId();
        List<Application> applications = applicationRepository.findByOwnerId(currentUserId);

        // Auto-escalate SUBMITTED → UNDER_REVIEW when owner views the list
        for (Application app : applications) {
            if (app.getStatus() == ApplicationStatus.SUBMITTED) {
                app.setStatus(ApplicationStatus.UNDER_REVIEW);
                applicationRepository.save(app);
                log.info("Application {} auto-escalated to UNDER_REVIEW", app.getId());
            }
        }

        return applications.stream().map(this::toResponseWithDocuments).toList();
    }

    @Transactional
    public ApplicationResponse findById(UUID id) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        // Auto-escalate to UNDER_REVIEW when the property owner views a SUBMITTED application
        if (application.getStatus() == ApplicationStatus.SUBMITTED) {
            UUID currentUserId = AuthService.getCurrentUserId();
            UUID ownerId = application.getListing().getProperty().getOwner().getId();
            if (ownerId.equals(currentUserId)) {
                application.setStatus(ApplicationStatus.UNDER_REVIEW);
                application = applicationRepository.save(application);
                log.info("Application {} auto-escalated to UNDER_REVIEW", id);
            }
        }

        return toResponseWithDocuments(application);
    }

    @Transactional
    public ApplicationResponse review(UUID id, ReviewApplicationRequest request) {
        UUID currentUserId = AuthService.getCurrentUserId();
        User reviewer = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        if (application.getStatus() != ApplicationStatus.SUBMITTED &&
                application.getStatus() != ApplicationStatus.UNDER_REVIEW) {
            throw new BadRequestException("Application cannot be reviewed in current status");
        }

        application.setStatus(request.status());
        application.setNotes(request.notes());
        application.setReviewedBy(reviewer);
        application.setReviewedAt(LocalDateTime.now());

        application = applicationRepository.save(application);
        log.info("Application {} reviewed: {}", id, request.status());

        // Send notification email
        sendReviewNotificationEmail(application, reviewer);

        // Audit log
        auditService.logAction(AuditAction.UPDATE, "Application", id);

        return toResponseWithDocuments(application);
    }

    @Transactional
    public RentalResponse createRentalFromApplication(UUID applicationId, CreateRentalFromApplicationRequest request) {
        UUID currentUserId = AuthService.getCurrentUserId();

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        if (application.getStatus() != ApplicationStatus.ACCEPTED) {
            throw new BadRequestException("Application must be accepted before creating a rental");
        }

        if (!application.getListing().getProperty().getOwner().getId().equals(currentUserId)) {
            throw new BadRequestException("You can only create rentals for your own properties");
        }

        if (rentalRepository.existsByApplicationId(applicationId)) {
            throw new BadRequestException("A rental has already been created for this application");
        }

        Rental rental = Rental.builder()
                .property(application.getListing().getProperty())
                .tenant(application.getApplicant())
                .application(application)
                .rentalType(request.rentalType())
                .status(RentalStatus.PENDING)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .monthlyRent(request.monthlyRent())
                .currency(request.currency() != null ? request.currency() : "EUR")
                .depositAmount(request.depositAmount())
                .charges(request.charges())
                .paymentDay(request.paymentDay())
                .build();

        rental = rentalRepository.save(rental);
        log.info("Rental created from application {} for property {}", applicationId, rental.getProperty().getId());

        // Auto-close the listing since a rental has been created
        Listing listing = application.getListing();
        if (listing.getStatus() == ListingStatus.PUBLISHED || listing.getStatus() == ListingStatus.PAUSED) {
            listing.setStatus(ListingStatus.CLOSED);
            listingRepository.save(listing);
            log.info("Listing {} auto-closed after rental creation", listing.getId());
        }

        auditService.logAction(AuditAction.CREATE, "Rental", rental.getId());

        return RentalResponse.from(rental);
    }

    @Transactional
    public ApplicationResponse withdraw(UUID id) {
        UUID currentUserId = AuthService.getCurrentUserId();
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        if (!application.getApplicant().getId().equals(currentUserId)) {
            throw new BadRequestException("You can only withdraw your own applications");
        }

        application.setStatus(ApplicationStatus.WITHDRAWN);
        application = applicationRepository.save(application);
        log.info("Application {} withdrawn", id);
        return toResponseWithDocuments(application);
    }

    private void sendReviewNotificationEmail(Application application, User reviewer) {
        try {
            ApplicationReviewedEmailData data = ApplicationReviewedEmailData.builder()
                    .recipientEmail(application.getApplicant().getEmail())
                    .recipientName(application.getApplicant().getFullName())
                    .ownerName(reviewer.getFullName())
                    .propertyName(application.getListing().getProperty().getName())
                    .listingTitle(application.getListing().getTitle())
                    .status(application.getStatus().name())
                    .notes(application.getNotes())
                    .dashboardUrl(frontendUrl + "/dashboard")
                    .build();

            if (application.getStatus() == ApplicationStatus.ACCEPTED) {
                emailService.sendApplicationAcceptedEmail(data);
            } else if (application.getStatus() == ApplicationStatus.REJECTED) {
                emailService.sendApplicationRejectedEmail(data);
            }
        } catch (Exception e) {
            log.error("Failed to send review notification email for application {}", application.getId(), e);
        }
    }

    private ApplicationResponse toResponseWithDocuments(Application application) {
        List<DocumentResponse> documents = documentRepository.findByApplicationId(application.getId())
                .stream().map(documentService::toResponse).toList();
        return ApplicationResponse.from(application, documents);
    }
}
