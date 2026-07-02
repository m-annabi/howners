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
        java.util.Map<String, Integer> numParJournal = new java.util.HashMap<>();
        for (JournalEntry e : entries) {
            int num = numParJournal.merge(e.journalCode(), 1, Integer::sum);
            sb.append(String.join(SEP,
                    e.journalCode(),
                    e.journalLib(),
                    e.journalCode() + "-" + num,
                    e.date().format(FEC_DATE),
                    e.compteNum(),
                    e.compteLib(),
                    "",                 // CompAuxNum
                    "",                 // CompAuxLib
                    e.pieceRef(),
                    e.pieceDate().format(FEC_DATE),
                    sanitize(e.libelle()),
                    montant(e.debit()),
                    montant(e.credit()),
                    "",                 // EcritureLet
                    "",                 // DateLet
                    cloture.format(FEC_DATE),
                    "",                 // Montantdevise
                    ""                  // Idevise
            )).append("\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
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
