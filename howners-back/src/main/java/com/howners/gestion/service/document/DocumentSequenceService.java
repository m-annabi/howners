package com.howners.gestion.service.document;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Séquences de numérotation de documents (table document_sequences,
 * changelogs 093-094), par portée, type et année. La portée dépend du type :
 * le bailleur pour les factures et quittances (numéros continus par émetteur,
 * art. 242 nonies A du CGI pour les factures), le contrat pour les avenants.
 */
@Service
public class DocumentSequenceService {

    public static final String INVOICE = "INVOICE";
    public static final String RECEIPT = "RECEIPT";
    public static final String AMENDMENT = "AMENDMENT";
    public static final String REGULARISATION = "REGULARISATION";

    /** Valeur de seq_year pour les séquences non annuelles (avenants). */
    public static final int NO_YEAR = 0;

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
    public long next(UUID scopeId, String docKind, int year) {
        Number seq = (Number) entityManager.createNativeQuery(
                        "INSERT INTO document_sequences (scope_id, doc_kind, seq_year, next_value) "
                        + "VALUES (:scopeId, :kind, :year, 2) "
                        + "ON CONFLICT (scope_id, doc_kind, seq_year) "
                        + "DO UPDATE SET next_value = document_sequences.next_value + 1 "
                        + "RETURNING next_value - 1")
                .setParameter("scopeId", scopeId)
                .setParameter("kind", docKind)
                .setParameter("year", year)
                .getSingleResult();
        return seq.longValue();
    }
}
