package com.howners.gestion.service.application;

import com.howners.gestion.domain.application.Application;
import com.howners.gestion.domain.application.ApplicationStatus;
import com.howners.gestion.domain.listing.Listing;
import com.howners.gestion.domain.listing.ListingStatus;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.application.ApplicationResponse;
import com.howners.gestion.dto.application.CreateApplicationRequest;
import com.howners.gestion.dto.application.ReviewApplicationRequest;
import com.howners.gestion.dto.document.DocumentResponse;
import com.howners.gestion.exception.BadRequestException;
import com.howners.gestion.exception.ResourceNotFoundException;
import com.howners.gestion.repository.ApplicationRepository;
import com.howners.gestion.repository.DocumentRepository;
import com.howners.gestion.repository.ListingRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.service.auth.AuthService;
import com.howners.gestion.service.document.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Transactional(readOnly = true)
    public List<ApplicationResponse> findReceivedApplications() {
        UUID currentUserId = AuthService.getCurrentUserId();
        return applicationRepository.findByOwnerId(currentUserId)
                .stream().map(this::toResponseWithDocuments).toList();
    }

    @Transactional(readOnly = true)
    public ApplicationResponse findById(UUID id) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
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
        return toResponseWithDocuments(application);
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

    private ApplicationResponse toResponseWithDocuments(Application application) {
        List<DocumentResponse> documents = documentRepository.findByApplicationId(application.getId())
                .stream().map(documentService::toResponse).toList();
        return ApplicationResponse.from(application, documents);
    }
}
