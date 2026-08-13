package com.howners.gestion.service.document;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Séquences de numérotation de documents, par bailleur, type et année
 * (table document_sequences, changelog 093). Garantit des numéros
 * chronologiques continus par émetteur (factures : art. 242 nonies A du CGI).
 */
@Service
public class DocumentSequenceService {

    public static final String INVOICE = "INVOICE";
    public static final String RECEIPT = "RECEIPT";

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Alloue le prochain numéro de la séquence (1, 2, 3…). L'UPSERT est
     * atomique : deux allocations concurrentes ne peuvent pas rendre le même
     * numéro. Propagation MANDATORY : l'allocation doit partager la
     * transaction de la création du document, pour que l'incrément soit
     * annulé avec elle en cas d'échec (pas de trou dans la séquence).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public long next(UUID ownerId, String docKind, int year) {
        Number seq = (Number) entityManager.createNativeQuery(
                        "INSERT INTO document_sequences (owner_id, doc_kind, seq_year, next_value) "
                        + "VALUES (:ownerId, :kind, :year, 2) "
                        + "ON CONFLICT (owner_id, doc_kind, seq_year) "
                        + "DO UPDATE SET next_value = document_sequences.next_value + 1 "
                        + "RETURNING next_value - 1")
                .setParameter("ownerId", ownerId)
                .setParameter("kind", docKind)
                .setParameter("year", year)
                .getSingleResult();
        return seq.longValue();
    }
}
