package com.howners.gestion.service.contract;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfService {

    /**
     * Génère un PDF à partir d'un contenu HTML (provenant de l'éditeur Quill)
     */
    public byte[] generatePdf(String content, String title) throws IOException {
        return generatePdf(content, title, null);
    }

    /**
     * Génère un PDF à partir d'un contenu HTML, avec un bloc HTML additionnel
     * (ex. encart de signature) inséré après le contenu, avant le pied de page.
     */
    public byte[] generatePdf(String content, String title, String appendixHtml) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            log.info("Generating PDF - title: '{}', content length: {} chars", title,
                    content != null ? content.length() : 0);

            // Construire un document HTML complet avec CSS
            String htmlDocument = buildHtmlDocument(content, title, appendixHtml);

            log.debug("Full HTML document length: {} chars", htmlDocument.length());

            // Convertir HTML en PDF via iText html2pdf
            ConverterProperties properties = new ConverterProperties();
            HtmlConverter.convertToPdf(htmlDocument, outputStream, properties);

            byte[] pdfBytes = outputStream.toByteArray();
            log.info("PDF generated successfully - size: {} bytes", pdfBytes.length);
            return pdfBytes;

        } catch (Exception e) {
            log.error("Error generating PDF: {}", e.getMessage(), e);
            throw new IOException("Failed to generate PDF", e);
        }
    }

    /**
     * Construit un document HTML complet avec styles CSS pour le PDF
     */
    private String buildHtmlDocument(String content, String title, String appendixHtml) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/>");
        html.append("<style>");
        // Page A4 avec marges homogènes. Pied de page en marge de page (répété sur
        // CHAQUE page, jamais dans le flux) : mention de génération à gauche,
        // pagination à droite — évite les dernières pages quasi vides qu'un
        // pied-de-page dans le corps pouvait provoquer.
        String generatedLine = "Document généré par Howners le "
                + java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                + " — howners.com";
        html.append("@page { size: A4; margin: 2cm 2cm 2.4cm 2cm;");
        html.append("  @bottom-left { content: '").append(generatedLine).append("'; font-family: Helvetica, Arial, sans-serif; font-size: 7.5pt; color: #9CA3AF; }");
        html.append("  @bottom-right { content: 'Page ' counter(page) ' / ' counter(pages); font-family: Helvetica, Arial, sans-serif; font-size: 8pt; color: #999; } }");
        html.append("body { font-family: Helvetica, Arial, sans-serif; font-size: 10pt; line-height: 1.5; color: #333; }");
        html.append("h1 { font-size: 17pt; text-align: center; margin: 0 0 6px; color: #1E3A5F; }");
        html.append("h2 { font-size: 13pt; margin: 16px 0 8px; color: #1E3A5F; border-bottom: 1px solid #E5E7EB; padding-bottom: 3px; }");
        html.append("h3 { font-size: 11pt; margin: 12px 0 6px; color: #1E3A5F; }");
        html.append("p { margin: 4px 0; text-align: justify; }");
        html.append("ul, ol { margin: 5px 0; padding-left: 20px; }");
        html.append("li { margin: 2px 0; }");
        html.append("strong, b { font-weight: bold; }");
        html.append("em, i { font-style: italic; }");
        html.append("u { text-decoration: underline; }");
        html.append("table { width: 100%; border-collapse: collapse; margin: 10px 0; font-size: 9.5pt; }");
        html.append("th { background: #F3F4F6; text-align: left; font-weight: bold; color: #374151; }");
        html.append("td, th { border: 1px solid #D1D5DB; padding: 6px 9px; }");
        html.append(".title { font-size: 17pt; font-weight: bold; text-align: center; margin-bottom: 20px; }");
        html.append(".doc-footer { margin-top: 36px; padding-top: 8px; border-top: 1px solid #E5E7EB; font-size: 8pt; color: #9CA3AF; text-align: center; }");
        // ---- Design system PDF (source unique) : identité de marque + blocs réutilisables.
        //      Les générateurs composent avec ces classes (via PdfDoc) plutôt qu'avec des
        //      styles inline, pour un rendu homogène sur TOUS les documents. ----
        // Bandeau de marque en tête de chaque document.
        html.append(".pdf-brand { text-align: center; letter-spacing: 3px; font-size: 10pt; font-weight: bold; color: #1E3A5F; text-transform: uppercase; padding-bottom: 6px; margin-bottom: 14px; border-bottom: 2px solid #1E3A5F; }");
        // En-tête de document : titre + référence (« N° … »).
        html.append(".doc-header { text-align: center; margin-bottom: 22px; }");
        html.append(".doc-header h1 { margin: 0 0 4px; }");
        html.append(".doc-ref { font-size: 10pt; color: #6B7280; margin: 0; text-align: center; }");
        html.append(".doc-lead { text-align: center; color: #6B7280; font-size: 9.5pt; margin: -2px 0 16px; }");
        // Bloc « parties » : deux colonnes sans bordure (bailleur / locataire).
        html.append(".parties { margin: 6px 0 14px; }");
        html.append(".parties td { border: none; vertical-align: top; width: 50%; }");
        html.append(".party-label { color: #6B7280; font-size: 8.5pt; text-transform: uppercase; letter-spacing: .5px; }");
        // Tableau clé/valeur (fiche descriptive) : 1re colonne en libellé grisé.
        html.append(".kv th { width: 38%; }");
        // Tableau de montants : plus étroit, centré, dernière colonne alignée à droite.
        html.append(".amounts { width: 70%; margin-left: auto; margin-right: auto; }");
        html.append(".total-row td { border-top: 2px solid #9CA3AF; font-weight: bold; }");
        html.append(".badge { display: inline-block; padding: 2px 8px; border-radius: 3px; font-size: 8.5pt; font-weight: bold; }");
        html.append(".badge-paid { background: #DCFCE7; color: #166534; }");
        html.append(".badge-due { background: #FEF3C7; color: #92400E; }");
        html.append(".badge-overdue { background: #FEE2E2; color: #991B1B; }");
        html.append(".muted { color: #6B7280; }");
        html.append(".mt-1 { margin-top: 10px; } .mt-2 { margin-top: 18px; } .mt-3 { margin-top: 28px; }");
        // Sous-titre juste sous le titre du contrat (base légale / nature du bail)
        html.append(".contract-lead { text-align: center; color: #6B7280; font-size: 9.5pt; margin: -2px 0 16px; }");
        html.append(".contract-place { margin-top: 20px; }");
        // Bloc de signature (deux colonnes) : pas de bordures, contrairement aux autres tables
        html.append(".signatures { margin-top: 30px; }");
        html.append(".signatures td { border: none; vertical-align: top; }");
        // nowrap : un montant (« 200 000,00 € ») ne doit jamais se couper sur deux lignes
        html.append(".text-right { text-align: right; white-space: nowrap; }");
        html.append(".text-center { text-align: center; }");
        html.append(".legal-note { font-size: 8.5pt; color: #6B7280; font-style: italic; margin-top: 20px; }");
        html.append("</style>");
        html.append("</head><body>");

        // Bandeau de marque, en tête de CHAQUE document (identité homogène).
        html.append("<div class=\"pdf-brand\">Howners</div>");

        // Ajouter le titre si fourni
        if (title != null && !title.isEmpty()) {
            html.append("<h1>").append(escapeHtml(title)).append("</h1>");
        }

        // Le contenu peut être du HTML (éditeur Quill, ou builders internes) OU du texte
        // brut avec des retours à la ligne (templates seedés). En HTML, les \n et espaces
        // multiples sont réduits à un seul espace ; on convertit donc le texte brut en HTML.
        html.append(looksLikeHtml(content) ? content : plainTextToHtml(content));

        if (appendixHtml != null && !appendixHtml.isEmpty()) {
            html.append(appendixHtml);
        }

        html.append("</body></html>");
        return html.toString();
    }

    /**
     * Détecte si le contenu est déjà du HTML structuré (présence d'au moins une balise
     * de bloc ou de saut de ligne). Sinon on le traite comme du texte brut.
     */
    private boolean looksLikeHtml(String content) {
        if (content == null) return true; // rien à convertir
        return content.matches("(?is).*<(p|div|br|table|h[1-6]|ul|ol|li)\\b.*");
    }

    /**
     * Convertit un texte brut en HTML : chaque bloc séparé par une ligne vide devient un
     * &lt;p&gt;, et les retours à la ligne simples deviennent des &lt;br/&gt;.
     */
    private String plainTextToHtml(String content) {
        if (content == null || content.isEmpty()) return "";
        String normalized = content.replace("\r\n", "\n").replace("\r", "\n");
        StringBuilder out = new StringBuilder();
        for (String block : normalized.split("\n[ \t]*\n")) {
            String trimmed = block.strip();
            if (trimmed.isEmpty()) continue;
            // Filet de sécurité pour du texte brut resté hors des templates HTML :
            // une ligne isolée « ARTICLE X - … » devient un vrai titre de section.
            if (!trimmed.contains("\n") && trimmed.matches("(?i)^article\\b.*")) {
                out.append("<h2>").append(escapeHtml(trimmed)).append("</h2>");
                continue;
            }
            String escaped = escapeHtml(trimmed).replace("\n", "<br/>");
            // Préserve les alignements par espaces des templates texte (ex. la ligne
            // « Signature du Bailleur          Signature du Locataire ») : en HTML,
            // les espaces consécutifs seraient réduits à un seul.
            escaped = escaped.replaceAll("(?<= ) ", "&nbsp;");
            out.append("<p>").append(escaped).append("</p>");
        }
        return out.toString();
    }

    /**
     * Échappe les caractères HTML dans le titre
     */
    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }

    /**
     * Calcule le hash SHA-256 d'un fichier PDF
     */
    public String calculateHash(byte[] pdfBytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(pdfBytes);
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            log.error("Error calculating hash: {}", e.getMessage());
            throw new RuntimeException("Failed to calculate PDF hash", e);
        }
    }

    /**
     * Génère un nom de fichier unique pour un PDF de contrat
     */
    public String generateFileName(String contractNumber, Integer version) {
        return String.format("contract_%s_v%d_%d.pdf",
                contractNumber,
                version,
                System.currentTimeMillis());
    }
}
