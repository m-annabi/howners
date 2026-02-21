package com.howners.gestion.service.search;

import com.howners.gestion.domain.search.FurnishedPreference;
import com.howners.gestion.domain.search.TenantSearchProfile;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.search.CreateTenantSearchProfileRequest;
import com.howners.gestion.dto.search.TenantSearchProfileResponse;
import com.howners.gestion.exception.ResourceNotFoundException;
import com.howners.gestion.repository.TenantSearchProfileRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.service.auth.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantSearchProfileService {

    private final TenantSearchProfileRepository profileRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public TenantSearchProfileResponse getMyProfile() {
        UUID tenantId = AuthService.getCurrentUserId();
        TenantSearchProfile profile = profileRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Search profile not found"));
        return TenantSearchProfileResponse.from(profile);
    }

    @Transactional
    public TenantSearchProfileResponse createOrUpdate(CreateTenantSearchProfileRequest request) {
        UUID tenantId = AuthService.getCurrentUserId();
        User tenant = userRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        TenantSearchProfile profile = profileRepository.findByTenantId(tenantId)
                .orElse(TenantSearchProfile.builder().tenant(tenant).build());

        profile.setDesiredCity(request.desiredCity());
        profile.setDesiredDepartment(request.desiredDepartment());
        profile.setDesiredPostalCode(request.desiredPostalCode());
        profile.setBudgetMin(request.budgetMin());
        profile.setBudgetMax(request.budgetMax());
        profile.setDesiredPropertyType(request.desiredPropertyType());
        profile.setMinSurface(request.minSurface());
        profile.setMinBedrooms(request.minBedrooms());
        profile.setFurnishedPreference(request.furnishedPreference() != null ? request.furnishedPreference() : FurnishedPreference.NO_PREFERENCE);
        profile.setDesiredMoveIn(request.desiredMoveIn());
        profile.setDescription(request.description());

        profile = profileRepository.save(profile);
        log.info("Search profile saved for tenant {}", tenantId);
        return TenantSearchProfileResponse.from(profile);
    }

    @Transactional
    public TenantSearchProfileResponse activate() {
        UUID tenantId = AuthService.getCurrentUserId();
        TenantSearchProfile profile = profileRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Search profile not found"));
        profile.setIsActive(true);
        profile = profileRepository.save(profile);
        log.info("Search profile activated for tenant {}", tenantId);
        return TenantSearchProfileResponse.from(profile);
    }

    @Transactional
    public TenantSearchProfileResponse deactivate() {
        UUID tenantId = AuthService.getCurrentUserId();
        TenantSearchProfile profile = profileRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Search profile not found"));
        profile.setIsActive(false);
        profile = profileRepository.save(profile);
        log.info("Search profile deactivated for tenant {}", tenantId);
        return TenantSearchProfileResponse.from(profile);
    }
}
