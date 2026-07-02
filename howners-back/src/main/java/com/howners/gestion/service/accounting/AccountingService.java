package com.howners.gestion.service.accounting;

import com.howners.gestion.domain.accounting.AmortizableAsset;
import com.howners.gestion.domain.accounting.AssetType;
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
    private final com.howners.gestion.repository.LoanRepository loanRepository;
    private final PropertyRepository propertyRepository;
    private final com.howners.gestion.repository.ExpenseRepository expenseRepository;
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
                .base(req.base()).startDate(effectiveStart(activity, req.startDate())).durationYears(duration).build();
        return AssetResponse.from(assetRepository.save(asset));
    }

    /**
     * L'amortissement démarre à la mise en location : une immobilisation acquise avant le
     * début d'activité est amortie à partir du début d'activité (évite un amortissement
     * antérieur à l'apport, qui déséquilibrerait le bilan).
     */
    private java.time.LocalDate effectiveStart(FiscalActivity activity, java.time.LocalDate date) {
        return date != null && date.isBefore(activity.getStartDate()) ? activity.getStartDate() : date;
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

    /** Immobilisations suggérées à partir des dépenses et biens existants (hors déjà importées). */
    @Transactional(readOnly = true)
    public List<AssetSuggestion> suggestAssets() {
        FiscalActivity activity = requireActivity();
        UUID ownerId = activity.getOwner().getId();
        List<AmortizableAsset> existing = assetRepository.findByActivityId(activity.getId());

        java.util.Set<UUID> importedExpenses = existing.stream()
                .map(AmortizableAsset::getSourceExpenseId).filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        java.util.Set<String> importedProperty = existing.stream()
                .filter(a -> a.getSourcePropertyId() != null)
                .map(a -> a.getSourcePropertyId() + "|" + a.getType().name())
                .collect(java.util.stream.Collectors.toSet());

        List<AssetSuggestion> out = new java.util.ArrayList<>();

        // Depuis les dépenses (mobilier / travaux)
        for (var e : expenseRepository.findByOwnerId(ownerId)) {
            AssetType type = switch (e.getCategory()) {
                case FURNISHING -> AssetType.MOBILIER;
                case RENOVATION -> AssetType.TRAVAUX;
                default -> null;
            };
            if (type == null || e.getExpenseDate() == null || e.getAmount() == null) continue;
            if (importedExpenses.contains(e.getId())) continue;
            String label = e.getDescription() != null && !e.getDescription().isBlank()
                    ? e.getDescription() : type.getLabel();
            out.add(new AssetSuggestion("EXPENSE", e.getId(), type, type.getLabel(),
                    label, e.getAmount(), e.getExpenseDate(), type.getDefaultDurationYears()));
        }

        // Depuis les biens (bâti + frais d'acquisition)
        for (Property p : propertyRepository.findByOwnerId(ownerId)) {
            if (p.getAcquisitionDate() == null) continue;
            if (p.getPurchasePrice() != null && !importedProperty.contains(p.getId() + "|BATIMENT")) {
                java.math.BigDecimal land = p.getLandValue() != null ? p.getLandValue() : java.math.BigDecimal.ZERO;
                java.math.BigDecimal bati = p.getPurchasePrice().subtract(land);
                if (bati.signum() > 0) {
                    out.add(new AssetSuggestion("PROPERTY_BUILDING", p.getId(), AssetType.BATIMENT,
                            AssetType.BATIMENT.getLabel(), "Bâti — " + p.getName(), bati,
                            p.getAcquisitionDate(), AssetType.BATIMENT.getDefaultDurationYears()));
                }
            }
            if (p.getNotaryFees() != null && p.getNotaryFees().signum() > 0
                    && !importedProperty.contains(p.getId() + "|FRAIS")) {
                out.add(new AssetSuggestion("PROPERTY_FEES", p.getId(), AssetType.FRAIS,
                        AssetType.FRAIS.getLabel(), "Frais d'acquisition — " + p.getName(), p.getNotaryFees(),
                        p.getAcquisitionDate(), AssetType.FRAIS.getDefaultDurationYears()));
            }
        }
        return out;
    }

    /** Crée les immobilisations correspondant aux suggestions retenues (valeurs re-dérivées côté serveur). */
    @Transactional
    public List<AssetResponse> importSuggestions(ImportSuggestionsRequest request) {
        FiscalActivity activity = requireActivity();
        if (request == null || request.items() == null || request.items().isEmpty())
            throw new BadRequestException("Aucune immobilisation à importer.");

        List<AssetSuggestion> suggestions = suggestAssets();
        List<AssetResponse> created = new java.util.ArrayList<>();
        for (ImportItem item : request.items()) {
            AssetSuggestion s = suggestions.stream()
                    .filter(x -> x.sourceType().equals(item.sourceType()) && x.sourceId().equals(item.sourceId()))
                    .findFirst().orElse(null);
            if (s == null) continue; // déjà importée ou source disparue
            int duration = item.durationYears() != null && item.durationYears() > 0
                    ? item.durationYears() : s.durationYears();
            AmortizableAsset asset = AmortizableAsset.builder()
                    .activity(activity).type(s.type()).label(s.label()).base(s.base())
                    .startDate(effectiveStart(activity, s.startDate())).durationYears(duration).build();
            if ("EXPENSE".equals(s.sourceType())) asset.setSourceExpenseId(s.sourceId());
            else {
                asset.setSourcePropertyId(s.sourceId());
                propertyRepository.findById(s.sourceId()).ifPresent(asset::setProperty);
            }
            created.add(AssetResponse.from(assetRepository.save(asset)));
        }
        return created;
    }

    // --- Emprunts ---

    @Transactional(readOnly = true)
    public List<LoanResponse> listLoans() {
        FiscalActivity activity = requireActivity();
        return loanRepository.findByActivityId(activity.getId()).stream().map(LoanResponse::from).toList();
    }

    @Transactional
    public LoanResponse addLoan(CreateLoanRequest req) {
        FiscalActivity activity = requireActivity();
        if (req.principal() == null || req.annualRate() == null || req.durationMonths() == null || req.startDate() == null)
            throw new BadRequestException("Capital, taux, durée et date sont requis.");
        Property property = null;
        if (req.propertyId() != null) {
            property = propertyRepository.findById(req.propertyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Property", "id", req.propertyId().toString()));
            if (!property.getOwner().getId().equals(activity.getOwner().getId()))
                throw new ForbiddenException("Ce bien ne vous appartient pas.");
        }
        com.howners.gestion.domain.accounting.Loan loan = com.howners.gestion.domain.accounting.Loan.builder()
                .activity(activity).property(property)
                .label(req.label() != null && !req.label().isBlank() ? req.label() : "Emprunt")
                .principal(req.principal()).annualRate(req.annualRate())
                .durationMonths(req.durationMonths()).startDate(req.startDate())
                .insuranceMonthly(req.insuranceMonthly()).build();
        return LoanResponse.from(loanRepository.save(loan));
    }

    @Transactional
    public void deleteLoan(UUID loanId) {
        FiscalActivity activity = requireActivity();
        com.howners.gestion.domain.accounting.Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", "id", loanId.toString()));
        if (!loan.getActivity().getId().equals(activity.getId()))
            throw new ForbiddenException("Cet emprunt ne vous appartient pas.");
        loanRepository.delete(loan);
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
