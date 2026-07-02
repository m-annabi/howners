package com.howners.gestion.service.accounting;

import com.howners.gestion.domain.accounting.AmortizableAsset;
import com.howners.gestion.domain.accounting.FiscalActivity;
import com.howners.gestion.domain.accounting.FiscalJurisdiction;
import com.howners.gestion.domain.accounting.FiscalRegime;
import com.howners.gestion.domain.property.Property;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.accounting.AccountingDtos.*;
import com.howners.gestion.exception.BadRequestException;
import com.howners.gestion.exception.ForbiddenException;
import com.howners.gestion.exception.ResourceNotFoundException;
import com.howners.gestion.repository.AmortizableAssetRepository;
import com.howners.gestion.repository.FiscalActivityRepository;
import com.howners.gestion.repository.PropertyRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.service.auth.AuthService;
import com.howners.gestion.service.subscription.FeatureGateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Façade du module comptable : configuration de l'activité, gestion des immobilisations,
 * aperçu du résultat et production des documents (via le moteur fiscal résolu).
 */
@Service
@RequiredArgsConstructor
public class AccountingService {

    private final FiscalActivityRepository activityRepository;
    private final AmortizableAssetRepository assetRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final FiscalEngineResolver engineResolver;
    private final FeatureGateService featureGateService;

    private void assertEntitled(UUID ownerId) {
        if (!featureGateService.hasFeature(ownerId, "tax_export")) {
            throw new ForbiddenException("Le module comptable est réservé aux plans PRO et supérieurs.");
        }
    }

    @Transactional
    public ActivityResponse configureActivity(ConfigureActivityRequest req) {
        UUID ownerId = AuthService.getCurrentUserId();
        assertEntitled(ownerId);
        if (req.startDate() == null) throw new BadRequestException("La date de début d'activité est requise.");
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", ownerId.toString()));

        FiscalActivity activity = activityRepository.findByOwnerId(ownerId).orElseGet(() ->
                FiscalActivity.builder().owner(owner)
                        .jurisdiction(FiscalJurisdiction.FR).regime(FiscalRegime.LMNP_REEL).build());
        activity.setStartDate(req.startDate());
        activity.setOpeningCash(req.openingCash());
        activity.setApportInitial(req.apportInitial());
        activity.setActive(true);
        return ActivityResponse.from(activityRepository.save(activity));
    }

    @Transactional(readOnly = true)
    public ActivityResponse getActivity() {
        UUID ownerId = AuthService.getCurrentUserId();
        assertEntitled(ownerId);
        return activityRepository.findByOwnerId(ownerId).map(ActivityResponse::from).orElse(null);
    }

    private FiscalActivity requireActivity() {
        UUID ownerId = AuthService.getCurrentUserId();
        assertEntitled(ownerId);
        return activityRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new BadRequestException("Configurez d'abord votre activité comptable."));
    }

    @Transactional(readOnly = true)
    public List<AssetResponse> listAssets() {
        FiscalActivity activity = requireActivity();
        return assetRepository.findByActivityId(activity.getId()).stream().map(AssetResponse::from).toList();
    }

    @Transactional
    public AssetResponse addAsset(CreateAssetRequest req) {
        FiscalActivity activity = requireActivity();
        if (req.type() == null || req.base() == null || req.startDate() == null)
            throw new BadRequestException("Type, base et date de mise en service sont requis.");
        Property property = null;
        if (req.propertyId() != null) {
            property = propertyRepository.findById(req.propertyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Property", "id", req.propertyId().toString()));
            if (!property.getOwner().getId().equals(activity.getOwner().getId()))
                throw new ForbiddenException("Ce bien ne vous appartient pas.");
        }
        int duration = req.durationYears() != null && req.durationYears() > 0
                ? req.durationYears() : req.type().getDefaultDurationYears();
        AmortizableAsset asset = AmortizableAsset.builder()
                .activity(activity).property(property).type(req.type())
                .label(req.label() != null ? req.label() : req.type().getLabel())
                .base(req.base()).startDate(req.startDate()).durationYears(duration).build();
        return AssetResponse.from(assetRepository.save(asset));
    }

    @Transactional
    public void deleteAsset(UUID assetId) {
        FiscalActivity activity = requireActivity();
        AmortizableAsset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset", "id", assetId.toString()));
        if (!asset.getActivity().getId().equals(activity.getId()))
            throw new ForbiddenException("Cette immobilisation ne vous appartient pas.");
        assetRepository.delete(asset);
    }

    @Transactional(readOnly = true)
    public ResultResponse preview(int year) {
        FiscalActivity activity = requireActivity();
        LmnpResult result = (LmnpResult) engineResolver.resolve(activity).computeResult(activity, year);
        return ResultResponse.from(result);
    }

    @Transactional(readOnly = true)
    public List<GeneratedDocument> documents(int year) {
        FiscalActivity activity = requireActivity();
        return engineResolver.resolve(activity).generateDocuments(activity, year);
    }

    /** ZIP « liasse » regroupant tous les documents de l'exercice. */
    @Transactional(readOnly = true)
    public byte[] liasseZip(int year) {
        List<GeneratedDocument> docs = documents(year);
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ZipOutputStream zip = new ZipOutputStream(bos)) {
            for (GeneratedDocument d : docs) {
                zip.putNextEntry(new ZipEntry(d.filename()));
                zip.write(d.content());
                zip.closeEntry();
            }
            zip.finish();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Échec de la génération de la liasse ZIP", e);
        }
    }
}
