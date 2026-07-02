package com.howners.gestion.service.accounting;

import com.howners.gestion.domain.accounting.FiscalActivity;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.service.contract.PdfService;
import com.howners.gestion.util.PdfFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Produit la liasse LMNP en PDF : compte de résultat, bilan simplifié, tableau des
 * amortissements, détermination du résultat fiscal et correspondance vers les
 * formulaires 2031 / 2033 / 2042-C-PRO — mise en forme via {@link PdfService}.
 */
@Service
@RequiredArgsConstructor
public class LmnpDocumentService {

    private final PdfService pdfService;

    public byte[] generateLiassePdf(FiscalActivity activity, LmnpResult r) throws IOException {
        return pdfService.generatePdf(buildHtml(activity, r),
                "Liasse LMNP — exercice " + r.year());
    }

    private String buildHtml(FiscalActivity activity, LmnpResult r) {
        User owner = activity.getOwner();
        StringBuilder h = new StringBuilder();

        h.append("<p style=\"text-align:center;color:#666;\">Location meublée non professionnelle (LMNP) — régime réel · Bénéfices industriels et commerciaux</p>");
        h.append("<p><strong>Exploitant :</strong> ").append(owner.getFullName());
        String adr = PdfFormat.adressePostale(owner);
        if (adr != null) h.append("<br/><span style=\"color:#555;\">").append(adr).append("</span>");
        if (activity.getSiret() != null && !activity.getSiret().isBlank()) {
            h.append("<br/><span style=\"color:#555;\">SIRET : ").append(activity.getSiret()).append("</span>");
        }
        h.append("</p>");
        h.append("<p><strong>Exercice :</strong> du 01/01/").append(r.year()).append(" au 31/12/").append(r.year()).append("</p>");
        if (activity.getSiret() == null || activity.getSiret().isBlank()) {
            h.append("<p class=\"legal-note\">SIRET non renseigné : il est obligatoire sur la déclaration 2031 et donne son nom réglementaire au FEC. Renseignez-le dans la configuration de l'activité.</p>");
        }

        // --- Compte de résultat ---
        h.append("<h2>Compte de résultat <span style=\"font-weight:normal;color:#888;font-size:9pt;\">(façon 2033-B)</span></h2>");
        h.append("<table><tr><th>Poste</th><th class=\"text-right\">Montant</th></tr>");
        h.append(row("Recettes locatives encaissées (loyers et provisions pour charges)", r.recettes(), true));
        for (Map.Entry<String, BigDecimal> c : r.chargesParPoste().entrySet()) {
            h.append(row("&nbsp;&nbsp;" + c.getKey(), c.getValue().negate(), false));
        }
        h.append(row("Total des charges déductibles", r.totalCharges().negate(), false));
        h.append(row("Résultat avant amortissements", r.resultatAvantAmortissement(), true));
        h.append(row("Dotations aux amortissements (comptable)", r.dotationComptable().negate(), false));
        h.append(row("<strong>Résultat comptable</strong>", r.resultatComptable(), true));
        h.append("</table>");

        // --- Détermination du résultat fiscal ---
        h.append("<h2>Détermination du résultat fiscal</h2>");
        h.append("<table><tr><th>Élément</th><th class=\"text-right\">Montant</th></tr>");
        h.append(row("Résultat avant amortissements", r.resultatAvantAmortissement(), false));
        h.append(row("Amortissements déductibles (règle de non-déficit)", r.amortissementDeductible().negate(), false));
        if (r.deficitAnterieurImpute() != null && r.deficitAnterieurImpute().signum() > 0) {
            h.append(row("Déficits BIC antérieurs imputés (report 10 ans)", r.deficitAnterieurImpute().negate(), false));
        }
        h.append(row("<strong>Résultat fiscal (base imposable BIC)</strong>", r.resultatFiscal(), true));
        h.append(row("Amortissements différés reportés (stock fin d'exercice)", r.amortissementDiffereCumul(), false));
        h.append(row("Déficits BIC restant reportables", r.deficitReportable(), false));
        h.append("</table>");
        h.append("<p class=\"legal-note\">Règle LMNP : l'amortissement n'est déductible qu'à hauteur du bénéfice avant amortissement ; l'excédent est reporté sans limite de durée (art. 39 C du CGI). Les déficits BIC non professionnels s'imputent sur les bénéfices de même nature des dix années suivantes.</p>");

        // --- Tableau des amortissements ---
        h.append("<h2>Tableau des amortissements <span style=\"font-weight:normal;color:#888;font-size:9pt;\">(façon 2033-C)</span></h2>");
        if (r.lignesAmortissement().isEmpty()) {
            h.append("<p><em>Aucune immobilisation.</em></p>");
        } else {
            h.append("<table><tr><th>Immobilisation</th><th class=\"text-right\">Base</th><th class=\"text-right\">Dotation ")
                    .append(r.year()).append("</th><th class=\"text-right\">Cumul</th><th class=\"text-right\">VNC</th></tr>");
            for (LmnpResult.AssetAmortLine l : r.lignesAmortissement()) {
                h.append("<tr><td>").append(l.asset().getType().getLabel()).append(" — ").append(l.asset().getLabel())
                        .append("</td><td class=\"text-right\">").append(PdfFormat.montant(l.base()))
                        .append("</td><td class=\"text-right\">").append(PdfFormat.montant(l.annuite()))
                        .append("</td><td class=\"text-right\">").append(PdfFormat.montant(l.cumul()))
                        .append("</td><td class=\"text-right\">").append(PdfFormat.montant(l.vnc())).append("</td></tr>");
            }
            h.append("</table>");
        }

        // --- Bilan simplifié ---
        h.append("<h2>Bilan simplifié au 31/12/").append(r.year())
                .append(" <span style=\"font-weight:normal;color:#888;font-size:9pt;\">(façon 2033-A)</span></h2>");
        h.append("<table><tr><th>ACTIF</th><th class=\"text-right\">Montant</th></tr>");
        h.append(row("Immobilisations (valeur nette comptable)", r.vncImmobilisations(), false));
        h.append(row("Trésorerie", r.tresorerie(), false));
        h.append(row("<strong>Total actif</strong>", r.totalActif(), true));
        h.append("</table>");
        h.append("<table><tr><th>PASSIF</th><th class=\"text-right\">Montant</th></tr>");
        h.append(row("Capital de l'exploitant", r.capitalExploitant(), false));
        h.append(row("Report à nouveau", r.reportANouveau(), false));
        h.append(row("Résultat de l'exercice", r.resultatComptable(), false));
        if (r.dettesEmprunt() != null && r.dettesEmprunt().signum() != 0) {
            h.append(row("Emprunts et dettes assimilées (capital restant dû)", r.dettesEmprunt(), false));
        }
        h.append(row("<strong>Total passif</strong>", r.totalPassif(), true));
        h.append("</table>");
        if (r.totalActif().subtract(r.totalPassif()).abs().compareTo(new BigDecimal("0.05")) > 0) {
            h.append("<p class=\"legal-note\">Écart actif/passif détecté — vérifiez le bilan d'ouverture (trésorerie initiale) et les dates des immobilisations et emprunts.</p>");
        }

        // --- Où reporter ces montants ---
        h.append("<h2>Où reporter ces montants</h2>");
        h.append("<table><tr><th>Montant</th><th class=\"text-right\"></th><th>Formulaire / rubrique</th></tr>");
        boolean benefice = r.resultatFiscal().signum() >= 0;
        h.append(reportRow("Résultat fiscal de l'exercice", r.resultatFiscal(),
                benefice
                        ? "2031 (résultat fiscal) et 2042-C-PRO — « Locations meublées non professionnelles, régime réel », bénéfice : case 5NA (5OA/5PA selon le déclarant)"
                        : "2031 (résultat fiscal) et 2042-C-PRO — « Locations meublées non professionnelles, régime réel », déficit de l'année : case 5NY (5OY/5PY selon le déclarant)"));
        h.append(reportRow("Recettes et charges par nature", r.recettes(),
                "2033-B — compte de résultat simplifié (produits d'exploitation, charges externes, impôts et taxes, charges financières, dotations)"));
        h.append(reportRow("Immobilisations, amortissements, VNC", r.vncImmobilisations(),
                "2033-A (actif immobilisé) et 2033-C (immobilisations et amortissements)"));
        if (r.dettesEmprunt() != null && r.dettesEmprunt().signum() != 0) {
            h.append(reportRow("Capital restant dû des emprunts", r.dettesEmprunt(),
                    "2033-A — passif, « emprunts et dettes assimilées »"));
        }
        if (r.amortissementDiffereCumul().signum() > 0) {
            h.append(reportRow("Amortissements différés en report", r.amortissementDiffereCumul(),
                    "Suivi extra-comptable à joindre (amortissements non déduits, art. 39 C du CGI)"));
        }
        if (r.deficitReportable().signum() > 0) {
            h.append(reportRow("Déficits BIC reportables", r.deficitReportable(),
                    "Suivi des déficits non professionnels (imputables 10 ans sur les bénéfices de même nature)"));
        }
        h.append("</table>");
        h.append("<p class=\"legal-note\">Les numéros de case (5NA, 5NY…) correspondent au premier déclarant : vérifiez-les sur le millésime de votre formulaire.</p>");

        // --- Points d'attention ---
        if (r.avertissements() != null && !r.avertissements().isEmpty()) {
            h.append("<h2>Points d'attention</h2><ul>");
            for (String note : r.avertissements()) {
                h.append("<li>").append(note).append("</li>");
            }
            h.append("</ul>");
        }
        h.append("<p class=\"legal-note\">La cotisation foncière des entreprises (CFE) reste due par le loueur en meublé (exonération possible l'année de début d'activité et cas particuliers de l'art. 1459 du CGI) : elle est appelée directement par le service des impôts et n'apparaît pas dans cette liasse si elle n'a pas été saisie en dépense.</p>");

        h.append("<p class=\"legal-note\">Document d'aide généré par Howners : il ne remplace pas la liasse fiscale officielle (formulaires 2031 et 2033) ni un conseil comptable. Reportez ces montants sur votre déclaration ou remettez-les à votre comptable.</p>");
        return h.toString();
    }

    private String row(String label, BigDecimal value, boolean strong) {
        String v = PdfFormat.montant(value);
        return "<tr><td>" + label + "</td><td class=\"text-right\">"
                + (strong ? "<strong>" + v + "</strong>" : v) + "</td></tr>";
    }

    private String reportRow(String label, BigDecimal value, String destination) {
        return "<tr><td>" + label + "</td><td class=\"text-right\">" + PdfFormat.montant(value)
                + "</td><td style=\"color:#555;\">" + destination + "</td></tr>";
    }
}
