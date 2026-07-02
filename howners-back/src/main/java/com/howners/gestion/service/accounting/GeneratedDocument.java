package com.howners.gestion.service.accounting;

/**
 * Document généré par un moteur fiscal (PDF, FEC…), prêt à être téléchargé ou groupé
 * dans une liasse ZIP.
 */
public record GeneratedDocument(String label, String filename, String contentType, byte[] content) {
}
