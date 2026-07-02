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
 * amortissements et détermination du résultat fiscal — mise en forme via {@link PdfService}.
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
        h.append("</p>");
        h.append("<p><strong>Exercice :</strong> du 01/01/").append(r.year()).append(" au 31/12/").append(r.year()).append("</strong></p>");

        // --- Compte de résultat ---
        h.append("<h2>Compte de résultat</h2>");
        h.append("<table><tr><th>Poste</th><th class=\"text-right\">Montant</th></tr>");
        h.append(row("Recettes locatives (loyers encaissés)", r.recettes(), true));
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
        h.append(row("<strong>Résultat fiscal (base imposable BIC)</strong>", r.resultatFiscal(), true));
        h.append(row("Amortissements différés reportés (fin d'exercice)", r.amortissementDiffereCumul(), false));
        h.append(row("Déficit BIC reportable (cumul)", r.deficitReportable(), false));
        h.append("</table>");
        h.append("<p class=\"legal-note\">Règle LMNP : l'amortissement n'est déductible qu'à hauteur du bénéfice avant amortissement ; l'excédent est reporté sans limite de durée (art. 39 C du CGI).</p>");

        // --- Tableau des amortissements ---
        h.append("<h2>Tableau des amortissements</h2>");
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
        h.append("<h2>Bilan simplifié au 31/12/").append(r.year()).append("</h2>");
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
            h.append(row("Emprunts (capital restant dû)", r.dettesEmprunt(), false));
        }
        h.append(row("<strong>Total passif</strong>", r.totalPassif(), true));
        h.append("</table>");
        if (r.totalActif().subtract(r.totalPassif()).abs().compareTo(new BigDecimal("0.05")) > 0) {
            h.append("<p class=\"legal-note\">Écart actif/passif détecté — vérifiez le bilan d'ouverture (apport et trésorerie initiale).</p>");
        }

        h.append("<p class=\"legal-note\">Document d'aide généré par Howners : il ne remplace pas la liasse fiscale officielle (formulaires 2031 et 2033) ni un conseil comptable. Reportez ces montants sur votre déclaration ou remettez-les à votre comptable.</p>");
        return h.toString();
    }

    private String row(String label, BigDecimal value, boolean strong) {
        String v = PdfFormat.montant(value);
        return "<tr><td>" + label + "</td><td class=\"text-right\">"
                + (strong ? "<strong>" + v + "</strong>" : v) + "</td></tr>";
    }
}
