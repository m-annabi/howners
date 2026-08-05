package com.howners.gestion.service.accounting;

import com.howners.gestion.domain.accounting.FiscalActivity;
import com.howners.gestion.service.accounting.AccountingEntryGenerator.JournalEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Fichier des Écritures Comptables (FEC) au format normalisé DGFiP : 18 colonnes,
 * séparateur pipe, dates en AAAAMMJJ, montants avec virgule décimale, écritures
 * numérotées séquentiellement par journal. Généré à partir des écritures
 * auto-produites — total débit = total crédit par construction.
 */
@Service
@RequiredArgsConstructor
public class FecExportService {

    private static final DateTimeFormatter FEC_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String SEP = "|";

    /**
     * Nom réglementaire : {SIREN}FEC{date de clôture AAAAMMJJ}.txt quand le SIREN est
     * connu (9 premiers chiffres du SIRET), sinon un nom lisible de repli.
     */
    public String fileName(FiscalActivity activity, int year) {
        String siret = activity.getSiret();
        if (siret != null && siret.length() >= 9) {
            return siret.substring(0, 9) + "FEC" + year + "1231.txt";
        }
        return "FEC-" + year + ".txt";
    }

    public byte[] generate(FiscalActivity activity, int year, List<JournalEntry> entries) {
        StringBuilder sb = new StringBuilder();
        // En-tête : 18 colonnes obligatoires
        sb.append(String.join(SEP,
                "JournalCode", "JournalLib", "EcritureNum", "EcritureDate", "CompteNum",
                "CompteLib", "CompAuxNum", "CompAuxLib", "PieceRef", "PieceDate",
                "EcritureLib", "Debit", "Credit", "EcritureLet", "DateLet",
                "ValidDate", "Montantdevise", "Idevise")).append("\n");

        LocalDate cloture = LocalDate.of(year, 12, 31);
        // EcritureNum identifie une écriture (transaction) : toutes les lignes d'une même
        // pièce partagent le même numéro, pour que le contrôle d'équilibre par écriture de la
        // DGFiP (Σdébit = Σcrédit par EcritureNum) soit satisfait.
        java.util.Map<String, Integer> numParJournal = new java.util.HashMap<>();
        java.util.Map<String, String> ecritureNumParPiece = new java.util.HashMap<>();
        for (JournalEntry e : entries) {
            String clePiece = e.journalCode() + "|PIECE|" + e.pieceRef();
            String ecritureNum = ecritureNumParPiece.computeIfAbsent(clePiece,
                    k -> e.journalCode() + "-" + numParJournal.merge(e.journalCode(), 1, Integer::sum));

            // Le sens d'une écriture est porté par la colonne (Debit / Credit), jamais par le
            // signe : un montant négatif est basculé dans la colonne opposée en valeur absolue.
            BigDecimal net = nz(e.debit()).subtract(nz(e.credit()));
            BigDecimal debit = net.signum() >= 0 ? net : BigDecimal.ZERO;
            BigDecimal credit = net.signum() < 0 ? net.negate() : BigDecimal.ZERO;

            sb.append(String.join(SEP,
                    e.journalCode(),
                    e.journalLib(),
                    ecritureNum,
                    e.date().format(FEC_DATE),
                    e.compteNum(),
                    e.compteLib(),
                    "",                 // CompAuxNum
                    "",                 // CompAuxLib
                    e.pieceRef(),
                    e.pieceDate().format(FEC_DATE),
                    sanitize(e.libelle()),
                    montant(debit),
                    montant(credit),
                    "",                 // EcritureLet
                    "",                 // DateLet
                    cloture.format(FEC_DATE),
                    "",                 // Montantdevise
                    ""                  // Idevise
            )).append("\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static String montant(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(2, java.math.RoundingMode.HALF_UP)
                .toPlainString().replace('.', ',');
    }

    private static String sanitize(String s) {
        if (s == null) return "";
        return s.replace(SEP, " ").replace("\n", " ").replace("\r", " ").trim();
    }
}
