package com.howners.gestion.service.contract;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.element.Image;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfService {

    /**
     * Génère un PDF à partir d'un contenu HTML (provenant de l'éditeur Quill)
     */
    public byte[] generatePdf(String content, String title) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            log.info("Generating PDF - title: '{}', content length: {} chars", title,
                    content != null ? content.length() : 0);

            // Construire un document HTML complet avec CSS
            String htmlDocument = buildHtmlDocument(content, title);

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
    private String buildHtmlDocument(String content, String title) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/>");
        html.append("<style>");
        html.append("body { font-family: Helvetica, Arial, sans-serif; font-size: 10pt; line-height: 1.5; margin: 40px; color: #333; }");
        html.append("h1 { font-size: 16pt; text-align: center; margin-bottom: 20px; color: #000; }");
        html.append("h2 { font-size: 13pt; margin-top: 15px; margin-bottom: 8px; color: #000; }");
        html.append("h3 { font-size: 11pt; margin-top: 12px; margin-bottom: 6px; color: #000; }");
        html.append("p { margin: 4px 0; text-align: justify; }");
        html.append("ul, ol { margin: 5px 0; padding-left: 20px; }");
        html.append("li { margin: 2px 0; }");
        html.append("strong, b { font-weight: bold; }");
        html.append("em, i { font-style: italic; }");
        html.append("u { text-decoration: underline; }");
        html.append("table { width: 100%; border-collapse: collapse; margin: 10px 0; }");
        html.append("td, th { border: 1px solid #ccc; padding: 5px 8px; }");
        html.append(".title { font-size: 16pt; font-weight: bold; text-align: center; margin-bottom: 20px; }");
        html.append("</style>");
        html.append("</head><body>");

        // Ajouter le titre si fourni
        if (title != null && !title.isEmpty()) {
            html.append("<h1>").append(escapeHtml(title)).append("</h1>");
        }

        // Ajouter le contenu HTML tel quel (provient de Quill)
        html.append(content);

        html.append("</body></html>");
        return html.toString();
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

    /**
     * Applique une image de signature sur la dernière page d'un PDF existant
     *
     * @param originalPdf le contenu du PDF original
     * @param signatureImageBytes l'image de la signature (PNG)
     * @param signerName le nom du signataire
     * @return le PDF avec la signature appliquée
     */
    public byte[] applySignatureToPdf(byte[] originalPdf, byte[] signatureImageBytes, String signerName) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(originalPdf));
             PdfWriter writer = new PdfWriter(outputStream);
             PdfDocument pdfDoc = new PdfDocument(reader, writer)) {

            PdfPage lastPage = pdfDoc.getLastPage();
            PdfCanvas canvas = new PdfCanvas(lastPage);
            float pageWidth = lastPage.getPageSize().getWidth();
            float pageHeight = lastPage.getPageSize().getHeight();

            // Zone de signature en bas à droite de la dernière page
            float sigBlockX = pageWidth - 250;
            float sigBlockY = 60;

            // Ligne de séparation au-dessus de la signature
            canvas.setLineWidth(0.5f)
                    .moveTo(sigBlockX, sigBlockY + 80)
                    .lineTo(sigBlockX + 200, sigBlockY + 80)
                    .stroke();

            // Texte "Signature du locataire"
            canvas.beginText()
                    .setFontAndSize(com.itextpdf.kernel.font.PdfFontFactory.createFont(), 8)
                    .moveText(sigBlockX, sigBlockY + 85)
                    .showText("Signature du locataire")
                    .endText();

            // Image de la signature
            Image signatureImage = new Image(ImageDataFactory.create(signatureImageBytes));
            float sigWidth = 180;
            float sigHeight = signatureImage.getImageHeight() * (sigWidth / signatureImage.getImageWidth());
            if (sigHeight > 60) {
                sigHeight = 60;
                sigWidth = signatureImage.getImageWidth() * (sigHeight / signatureImage.getImageHeight());
            }

            canvas.addImageAt(
                    ImageDataFactory.create(signatureImageBytes),
                    sigBlockX + 10,
                    sigBlockY + 15,
                    false
            );

            // Nom et date sous la signature
            String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm"));
            canvas.beginText()
                    .setFontAndSize(com.itextpdf.kernel.font.PdfFontFactory.createFont(), 7)
                    .moveText(sigBlockX, sigBlockY + 5)
                    .showText(signerName + " - " + dateStr)
                    .endText();

            log.info("Signature applied to PDF for signer: {}", signerName);
        }

        return outputStream.toByteArray();
    }
}
