package com.howners.gestion.service.accounting;

import com.howners.gestion.domain.accounting.FiscalActivity;
import com.howners.gestion.domain.accounting.FiscalJurisdiction;
import com.howners.gestion.domain.accounting.FiscalRegime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Moteur fiscal France — LMNP meublé au réel. Assemble le résultat, la liasse PDF et
 * le FEC. Seule implémentation de {@link FiscalEngine} en V1 ; l'ajout d'une juridiction
 * (Suisse…) se fait en fournissant un nouveau bean sans toucher au reste.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FranceLmnpReelEngine implements FiscalEngine {

    private final LmnpResultService resultService;
    private final LmnpDocumentService documentService;
    private final AccountingEntryGenerator entryGenerator;
    private final FecExportService fecExportService;

    @Override
    public FiscalJurisdiction jurisdiction() {
        return FiscalJurisdiction.FR;
    }

    @Override
    public FiscalRegime regime() {
        return FiscalRegime.LMNP_REEL;
    }

    @Override
    public FiscalResult computeResult(FiscalActivity activity, int year) {
        return resultService.compute(activity, year);
    }

    @Override
    public List<GeneratedDocument> generateDocuments(FiscalActivity activity, int year) {
        List<GeneratedDocument> docs = new ArrayList<>();
        LmnpResult result = resultService.compute(activity, year);
        try {
            byte[] pdf = documentService.generateLiassePdf(activity, result);
            docs.add(new GeneratedDocument("Liasse LMNP " + year,
                    "liasse-lmnp-" + year + ".pdf", "application/pdf", pdf));
        } catch (Exception e) {
            log.error("Échec génération liasse PDF LMNP {} : {}", year, e.getMessage());
            throw new RuntimeException("Échec de la génération de la liasse LMNP", e);
        }
        byte[] fec = fecExportService.generate(activity, year, entryGenerator.generate(activity, year));
        docs.add(new GeneratedDocument("FEC " + year,
                "FEC-" + year + ".txt", "text/plain", fec));
        return docs;
    }
}
