package com.howners.gestion.service.search;

import com.howners.gestion.domain.listing.Listing;
import com.howners.gestion.domain.search.InvitationStatus;
import com.howners.gestion.domain.search.TenantInvitation;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.search.CreateInvitationRequest;
import com.howners.gestion.dto.search.InvitationResponse;
import com.howners.gestion.exception.BadRequestException;
import com.howners.gestion.exception.ResourceNotFoundException;
import com.howners.gestion.repository.ListingRepository;
import com.howners.gestion.repository.TenantInvitationRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.service.auth.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvitationService {

    private final TenantInvitationRepository invitationRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;

    @Transactional
    public InvitationResponse invite(CreateInvitationRequest request) {
        UUID ownerId = AuthService.getCurrentUserId();
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Listing listing = listingRepository.findById(request.listingId())
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));

        if (!listing.getProperty().getOwner().getId().equals(ownerId)) {
            throw new BadRequestException("You can only invite tenants to your own listings");
        }

        if (invitationRepository.existsByListingIdAndTenantId(request.listingId(), request.tenantId())) {
            throw new BadRequestException("An invitation has already been sent to this tenant for this listing");
        }

        User tenant = userRepository.findById(request.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        TenantInvitation invitation = TenantInvitation.builder()
                .listing(listing)
                .tenant(tenant)
                .owner(owner)
                .message(request.message())
                .build();

        invitation = invitationRepository.save(invitation);
        log.info("Invitation sent from owner {} to tenant {} for listing {}", ownerId, request.tenantId(), request.listingId());
        return InvitationResponse.from(invitation);
    }

    @Transactional(readOnly = true)
    public List<InvitationResponse> getMyReceivedInvitations() {
        UUID tenantId = AuthService.getCurrentUserId();
        return invitationRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream().map(InvitationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<InvitationResponse> getMySentInvitations() {
        UUID ownerId = AuthService.getCurrentUserId();
        return invitationRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId)
                .stream().map(InvitationResponse::from).toList();
    }

    @Transactional
    public InvitationResponse updateStatus(UUID id, InvitationStatus status) {
        TenantInvitation invitation = invitationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

        UUID currentUserId = AuthService.getCurrentUserId();
        if (!invitation.getTenant().getId().equals(currentUserId)) {
            throw new BadRequestException("Only the invited tenant can update the invitation status");
        }

        invitation.setStatus(status);
        invitation = invitationRepository.save(invitation);
        log.info("Invitation {} status updated to {}", id, status);
        return InvitationResponse.from(invitation);
    }
}
