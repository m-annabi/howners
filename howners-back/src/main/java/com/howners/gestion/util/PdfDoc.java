package com.howners.gestion.util;

import java.util.List;
import java.util.Map;

/**
 * Bibliothèque de composants HTML pour les PDF (en-tête, parties, tableaux, montants,
 * note légale…). Couplée à la feuille de styles unique injectée par {@code PdfService},
 * elle donne une identité homogène à TOUS les documents et évite que chaque générateur
 * réinvente sa mise en page avec des styles inline.
 *
 * <p>Principe : un service compose son document en concaténant ces blocs, puis passe le
 * résultat à {@code pdfService.generatePdf(html, null)} — le titre/bandeau/pied de page
 * sont ajoutés par PdfService. Le formatage des valeurs (montants, adresses) reste dans
 * {@link PdfFormat}.
 */
public final class PdfDoc {

    private PdfDoc() {}

    /**
     * En-tête de document : grand titre centré + référence optionnelle (« N° … »).
     * Remplace le titre passé à PdfService quand le document a une référence à afficher.
     */
    public static String header(String title, String reference) {
        StringBuilder sb = new StringBuilder("<div class=\"doc-header\"><h1>")
                .append(escape(title)).append("</h1>");
        if (reference != null && !reference.isBlank()) {
            sb.append("<p class=\"doc-ref\">N° ").append(escape(reference)).append("</p>");
        }
        return sb.append("</div>").toString();
    }

    /** Ligne d'accroche grisée sous le titre (base légale, nature du document…). */
    public static String lead(String text) {
        return "<p class=\"doc-lead\">" + escape(text) + "</p>";
    }

    /**
     * Bloc « parties » en deux colonnes sans bordure. Chaque corps peut contenir du HTML
     * déjà construit (nom + {@link PdfFormat#blocAdresse}). Les libellés sont mis en forme.
     */
    public static String parties(String leftLabel, String leftBodyHtml,
                                 String rightLabel, String rightBodyHtml) {
        return "<table class=\"parties\"><tr>"
                + "<td><span class=\"party-label\">" + escape(leftLabel) + "</span><br/>" + leftBodyHtml + "</td>"
                + "<td><span class=\"party-label\">" + escape(rightLabel) + "</span><br/>" + rightBodyHtml + "</td>"
                + "</tr></table>";
    }

    /**
     * Tableau clé/valeur (fiche descriptive) : 1re colonne = libellé, 2e = valeur.
     * L'ordre d'itération de la map est préservé (utiliser une LinkedHashMap).
     */
    public static String kvTable(Map<String, String> rows) {
        StringBuilder sb = new StringBuilder("<table class=\"kv\">");
        for (Map.Entry<String, String> e : rows.entrySet()) {
            sb.append("<tr><th>").append(escape(e.getKey())).append("</th><td>")
              .append(escape(e.getValue())).append("</td></tr>");
        }
        return sb.append("</table>").toString();
    }

    /**
     * Tableau de données générique. La dernière colonne est alignée à droite
     * (montants). Les valeurs sont échappées.
     */
    public static String dataTable(List<String> headers, List<List<String>> rows) {
        StringBuilder sb = new StringBuilder("<table><tr>");
        for (int i = 0; i < headers.size(); i++) {
            sb.append(i == headers.size() - 1 ? "<th class=\"text-right\">" : "<th>")
              .append(escape(headers.get(i))).append("</th>");
        }
        sb.append("</tr>");
        for (List<String> row : rows) {
            sb.append("<tr>");
            for (int i = 0; i < row.size(); i++) {
                sb.append(i == row.size() - 1 ? "<td class=\"text-right\">" : "<td>")
                  .append(escape(row.get(i))).append("</td>");
            }
            sb.append("</tr>");
        }
        return sb.append("</table>").toString();
    }

    /**
     * Tableau de montants étroit et centré (libellé / montant), avec une ligne de total
     * en gras et filet supérieur. Les montants sont déjà formatés par l'appelant.
     */
    public static String amounts(Map<String, String> lines, String totalLabel, String totalValue) {
        StringBuilder sb = new StringBuilder("<table class=\"amounts\">");
        for (Map.Entry<String, String> e : lines.entrySet()) {
            sb.append("<tr><td>").append(escape(e.getKey())).append("</td>")
              .append("<td class=\"text-right\">").append(escape(e.getValue())).append("</td></tr>");
        }
        sb.append("<tr class=\"total-row\"><td>").append(escape(totalLabel)).append("</td>")
          .append("<td class=\"text-right\">").append(escape(totalValue)).append("</td></tr>");
        return sb.append("</table>").toString();
    }

    /** Pastille d'état colorée (payé / dû / en retard). {@code kind} = paid|due|overdue. */
    public static String badge(String label, String kind) {
        return "<span class=\"badge badge-" + kind + "\">" + escape(label) + "</span>";
    }

    /** Mention légale en petits caractères grisés, en bas de document. */
    public static String legalNote(String text) {
        return "<p class=\"legal-note\">" + escape(text) + "</p>";
    }

    /** Paragraphe simple (texte déjà échappé si HTML voulu ; ici échappé par sécurité). */
    public static String p(String text) {
        return "<p>" + escape(text) + "</p>";
    }

    /**
     * Échappe le texte destiné au HTML. Les blocs qui contiennent volontairement du HTML
     * (corps de {@link #parties}, adresses) sont passés séparément et NON ré-échappés.
     */
    public static String escape(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }
}
