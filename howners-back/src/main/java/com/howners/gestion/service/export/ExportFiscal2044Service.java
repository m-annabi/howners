package com.howners.gestion.service.export;

import com.howners.gestion.domain.expense.Expense;
import com.howners.gestion.domain.expense.ExpenseCategory;
import com.howners.gestion.domain.property.Property;
import com.howners.gestion.dto.analytics.Declaration2044Response;
import com.howners.gestion.exception.ForbiddenException;
import com.howners.gestion.repository.ExpenseRepository;
import com.howners.gestion.repository.PaymentRepository;
import com.howners.gestion.repository.PropertyRepository;
import com.howners.gestion.service.auth.AuthService;
import com.howners.gestion.service.contract.PdfService;
import com.howners.gestion.service.subscription.FeatureGateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Aide à la déclaration des revenus fonciers — formulaire 2044, régime réel.
 *
 * Mapping des catégories de dépenses vers les lignes 2044 :
 * - 221 (frais d'administration)         : MANAGEMENT_FEES, LEGAL
 * - 222 (primes d'assurance)             : INSURANCE
 * - 224 (réparations et entretien)       : MAINTENANCE, REPAIR, RENOVATION
 *   ⚠ les travaux de (re)construction/agrandissement ne sont PAS déductibles — disclaimer dans l'export.
 * - 227 (taxes foncières)                : TAX
 * - 229 (charges de copropriété)         : CONDO_FEES
 * Les autres catégories (UTILITIES, CLEANING, FURNISHING, OTHER) sont récupérables sur le
 * locataire ou non déductibles : exclues, listées en annexe.
 *
 * Revenus ligne 211 = loyers ENCAISSÉS (paidAt) sur l'année civile, hors charges (type RENT).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExportFiscal2044Service {

    static final Map<ExpenseCategory, String> MAPPING_2044 = Map.of(
            ExpenseCategory.MANAGEMENT_FEES, "221",
            ExpenseCategory.LEGAL, "221",
            ExpenseCategory.INSURANCE, "222",
            ExpenseCategory.MAINTENANCE, "224",
            ExpenseCategory.REPAIR, "224",
            ExpenseCategory.RENOVATION, "224",
            ExpenseCategory.TAX, "227",
            ExpenseCategory.CONDO_FEES, "229"
    );

    private static final Map<String, String> LIBELLES_LIGNES = Map.of(
            "221", "Frais d'administration et de gestion",
            "222", "Primes d'assurance",
            "224", "Dépenses de réparation, d'entretien et d'amélioration",
            "227", "Taxes foncières et taxes annexes",
            "229", "Provisions pour charges de copropriété"
    );

    private final PropertyRepository propertyRepository;
    private final PaymentRepository paymentRepository;
    private final ExpenseRepository expenseRepository;
    private final FeatureGateService featureGateService;
    private final PdfService pdfService;

    @Transactional(readOnly = true)
    public Declaration2044Response genererDeclaration(int annee) {
        UUID ownerId = AuthService.getCurrentUserId();
        assertTaxExportAllowed(ownerId);

        LocalDate debut = LocalDate.of(annee, 1, 1);
        LocalDate fin = LocalDate.of(annee + 1, 1, 1);

        List<Declaration2044Response.BienDeclaration> biens = new ArrayList<>();
        Map<String, BigDecimal> totauxParLigne = new LinkedHashMap<>();
        BigDecimal totalRevenus = BigDecimal.ZERO;
        BigDecimal totalCharges = BigDecimal.ZERO;

        for (Property property : propertyRepository.findByOwnerId(ownerId)) {
            BigDecimal revenus = paymentRepository.sumPaidRentByPropertyAndPeriod(
                    property.getId(), debut.atStartOfDay(), fin.atStartOfDay());

            Map<String, BigDecimal> chargesParLigne = new LinkedHashMap<>();
            BigDecimal chargesBien = BigDecimal.ZERO;
            for (Expense e : expenseRepository.findByPropertyId(property.getId())) {
                if (e.getExpenseDate() == null || e.getExpenseDate().getYear() != annee) continue;
                String ligne = MAPPING_2044.get(e.getCategory());
                if (ligne == null) continue;
                chargesParLigne.merge(ligne, e.getAmount(), BigDecimal::add);
                chargesBien = chargesBien.add(e.getAmount());
                totauxParLigne.merge(ligne, e.getAmount(), BigDecimal::add);
            }

            if (revenus.compareTo(BigDecimal.ZERO) == 0 && chargesBien.compareTo(BigDecimal.ZERO) == 0) {
                continue; // bien sans activité sur l'année
            }

            String adresse = String.format("%s, %s %s",
                    property.getAddressLine1() != null ? property.getAddressLine1() : "",
                    property.getPostalCode() != null ? property.getPostalCode() : "",
                    property.getCity() != null ? property.getCity() : "");

            biens.add(new Declaration2044Response.BienDeclaration(
                    property.getId(),
                    property.getName(),
                    adresse,
                    revenus,
                    chargesParLigne,
                    chargesBien,
                    revenus.subtract(chargesBien)
            ));
            totalRevenus = totalRevenus.add(revenus);
            totalCharges = totalCharges.add(chargesBien);
        }

        return new Declaration2044Response(
                annee, biens, totauxParLigne,
                totalRevenus, totalCharges,
                totalRevenus.subtract(totalCharges));
    }

    @Transactional(readOnly = true)
    public byte[] genererPdf(int annee) {
        Declaration2044Response declaration = genererDeclaration(annee);
        try {
            return pdfService.generatePdf(buildPdfHtml(declaration), null);
        } catch (IOException e) {
            throw new RuntimeException("Échec de génération du PDF fiscal 2044", e);
        }
    }

    @Transactional(readOnly = true)
    public String genererCsv(int annee) {
        Declaration2044Response declaration = genererDeclaration(annee);
        StringBuilder csv = new StringBuilder();
        csv.append("Bien;Adresse;Ligne 2044;Libellé;Montant (EUR)\n");
        for (var bien : declaration.biens()) {
            csv.append(String.format("%s;%s;211;Loyers bruts encaissés;%.2f%n",
                    escape(bien.nom()), escape(bien.adresse()), bien.revenusBruts()));
            bien.chargesParLigne().forEach((ligne, montant) -> csv.append(String.format(
                    "%s;%s;%s;%s;%.2f%n",
                    escape(bien.nom()), escape(bien.adresse()), ligne,
                    LIBELLES_LIGNES.getOrDefault(ligne, ligne), montant)));
        }
        csv.append(String.format(";;TOTAL 211;Total revenus bruts;%.2f%n", declaration.totalRevenusBruts()));
        declaration.totauxParLigne().forEach((ligne, montant) -> csv.append(String.format(
                ";;TOTAL %s;%s;%.2f%n", ligne, LIBELLES_LIGNES.getOrDefault(ligne, ligne), montant)));
        csv.append(String.format(";;420;Revenu foncier net (avant imputation déficits);%.2f%n",
                declaration.revenuFoncierNet()));
        return csv.toString();
    }

    private void assertTaxExportAllowed(UUID ownerId) {
        if (!featureGateService.hasFeature(ownerId, "tax_export")) {
            throw new ForbiddenException(
                    "L'export fiscal 2044 est disponible à partir du plan Pro. Passez au plan supérieur pour l'activer.");
        }
    }

    private String escape(String value) {
        return value == null ? "" : value.replace(";", ",");
    }

    private String buildPdfHtml(Declaration2044Response declaration) {
        StringBuilder biensHtml = new StringBuilder();
        for (var bien : declaration.biens()) {
            StringBuilder lignes = new StringBuilder();
            bien.chargesParLigne().forEach((ligne, montant) -> lignes.append(String.format(
                    "<tr><td style=\"padding: 6px;\">Ligne %s — %s</td><td style=\"padding: 6px; text-align: right;\">%.2f €</td></tr>",
                    ligne, LIBELLES_LIGNES.getOrDefault(ligne, ligne), montant)));

            biensHtml.append(String.format("""
                    <h3 style="margin-top: 20px;">%s</h3>
                    <p style="font-size: 9pt; color: #666;">%s</p>
                    <table style="width: 95%%; margin-left: auto; margin-right: auto;">
                        <tr><td style="padding: 6px;"><strong>Ligne 211 — Loyers bruts encaissés</strong></td><td style="padding: 6px; text-align: right;"><strong>%.2f €</strong></td></tr>
                        %s
                        <tr style="border-top: 1px solid #999;"><td style="padding: 6px;"><strong>Revenu net du bien</strong></td><td style="padding: 6px; text-align: right;"><strong>%.2f €</strong></td></tr>
                    </table>
                    """, bien.nom(), bien.adresse(), bien.revenusBruts(), lignes, bien.revenuNet()));
        }

        StringBuilder totaux = new StringBuilder();
        declaration.totauxParLigne().forEach((ligne, montant) -> totaux.append(String.format(
                "<tr><td style=\"padding: 6px;\">Ligne %s — %s</td><td style=\"padding: 6px; text-align: right;\">%.2f €</td></tr>",
                ligne, LIBELLES_LIGNES.getOrDefault(ligne, ligne), montant)));

        return """
                <div style="text-align: center; margin-bottom: 30px;">
                    <h1 style="font-size: 16pt; margin-bottom: 5px;">AIDE À LA DÉCLARATION 2044</h1>
                    <p style="font-size: 10pt; color: #666;">Revenus fonciers %d — régime réel</p>
                </div>

                <p><strong>Date d'édition :</strong> %s</p>

                %s

                <h3 style="margin-top: 25px;">Totaux à reporter sur le formulaire 2044</h3>
                <table style="width: 95%%; margin-left: auto; margin-right: auto;">
                    <tr><td style="padding: 6px;"><strong>Ligne 211 — Total des loyers bruts encaissés</strong></td><td style="padding: 6px; text-align: right;"><strong>%.2f €</strong></td></tr>
                    %s
                    <tr style="border-top: 2px solid #333;"><td style="padding: 6px;"><strong>Revenu foncier net (avant imputation des déficits)</strong></td><td style="padding: 6px; text-align: right;"><strong>%.2f €</strong></td></tr>
                </table>

                <p style="margin-top: 30px; font-size: 9pt; color: #666; font-style: italic;">
                    Document d'aide généré par Howners : il ne remplace pas la déclaration officielle et ne constitue
                    pas un conseil fiscal. Vérifiez l'éligibilité de chaque dépense (les travaux de construction,
                    reconstruction ou agrandissement ne sont pas déductibles, ligne 224). Les charges récupérables
                    sur le locataire ne sont pas déductibles et sont exclues de ce document. En cas de doute,
                    rapprochez-vous d'un expert-comptable ou des services fiscaux.
                </p>
                """.formatted(
                declaration.annee(),
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                biensHtml,
                declaration.totalRevenusBruts(),
                totaux,
                declaration.revenuFoncierNet());
    }
}
