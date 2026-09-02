package com.howners.gestion.service.document;

import com.howners.gestion.domain.document.Document;
import com.howners.gestion.domain.document.DocumentType;
import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.repository.DocumentRepository;
import com.howners.gestion.service.contract.PdfService;
import com.howners.gestion.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Archivage d'un PDF généré par la plateforme (quittance, mise en demeure, courrier de
 * révision, décompte de charges, état des lieux, avenant…) : envoi vers le stockage S3,
 * empreinte SHA-256 et enregistrement du Document rattaché au bail.
 * Remplace le bloc « uploadFile + Document.builder() + save » dupliqué dans chaque service.
 */
@Service
@RequiredArgsConstructor
public class GeneratedDocumentService {

    private static final String PDF = "application/pdf";

    private final StorageService storageService;
    private final DocumentRepository documentRepository;
    private final PdfService pdfService;

    /**
     * @param rental      bail auquel rattacher le document (le bien est déduit du bail)
     * @param uploader    utilisateur porté comme auteur du document
     * @param type        type fonctionnel du document
     * @param storageName nom transmis au stockage (peut contenir un préfixe de dossier, ex. « edl/… »)
     * @param fileName    nom de fichier présenté à l'utilisateur
     * @param pdfBytes    contenu du PDF
     * @param description description libre (nullable)
     */
    public Document storePdf(Rental rental, User uploader, DocumentType type,
                             String storageName, String fileName, byte[] pdfBytes, String description) {
        String fileKey = storageService.uploadFile(pdfBytes, storageName, PDF);
        Document document = Document.builder()
                .rental(rental)
                .property(rental != null ? rental.getProperty() : null)
                .uploader(uploader)
                .documentType(type)
                .fileName(fileName)
                .filePath(fileKey)   // colonne NOT NULL héritée du schéma initial
                .fileKey(fileKey)
                .fileSize((long) pdfBytes.length)
                .mimeType(PDF)
                .documentHash(pdfService.calculateHash(pdfBytes))
                .description(description)
                .build();
        return documentRepository.save(document);
    }

    /** Variante où le nom de stockage est le nom de fichier. */
    public Document storePdf(Rental rental, User uploader, DocumentType type,
                             String fileName, byte[] pdfBytes, String description) {
        return storePdf(rental, uploader, type, fileName, fileName, pdfBytes, description);
    }
}
