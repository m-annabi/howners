package com.howners.gestion.controller;

import com.howners.gestion.dto.accounting.AccountingDtos.*;
import com.howners.gestion.service.accounting.AccountingService;
import com.howners.gestion.service.accounting.GeneratedDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Module comptable & fiscal (LMNP réel, France). Réservé OWNER/ADMIN + plan PRO+
 * (contrôle de feature dans le service).
 */
@RestController
@RequestMapping("/api/accounting")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OWNER','ADMIN')")
public class AccountingController {

    private final AccountingService accountingService;

    @GetMapping("/activity")
    public ResponseEntity<ActivityResponse> getActivity() {
        return ResponseEntity.ok(accountingService.getActivity());
    }

    @PostMapping("/activity")
    public ResponseEntity<ActivityResponse> configureActivity(@RequestBody ConfigureActivityRequest req) {
        return ResponseEntity.ok(accountingService.configureActivity(req));
    }

    @GetMapping("/assets")
    public ResponseEntity<List<AssetResponse>> listAssets() {
        return ResponseEntity.ok(accountingService.listAssets());
    }

    @PostMapping("/assets")
    public ResponseEntity<AssetResponse> addAsset(@RequestBody CreateAssetRequest req) {
        return ResponseEntity.ok(accountingService.addAsset(req));
    }

    @DeleteMapping("/assets/{id}")
    public ResponseEntity<Void> deleteAsset(@PathVariable UUID id) {
        accountingService.deleteAsset(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/assets/suggestions")
    public ResponseEntity<List<AssetSuggestion>> suggestions() {
        return ResponseEntity.ok(accountingService.suggestAssets());
    }

    @PostMapping("/assets/import")
    public ResponseEntity<List<AssetResponse>> importSuggestions(@RequestBody ImportSuggestionsRequest req) {
        return ResponseEntity.ok(accountingService.importSuggestions(req));
    }

    @GetMapping("/loans")
    public ResponseEntity<List<LoanResponse>> listLoans() {
        return ResponseEntity.ok(accountingService.listLoans());
    }

    @PostMapping("/loans")
    public ResponseEntity<LoanResponse> addLoan(@RequestBody CreateLoanRequest req) {
        return ResponseEntity.ok(accountingService.addLoan(req));
    }

    @DeleteMapping("/loans/{id}")
    public ResponseEntity<Void> deleteLoan(@PathVariable UUID id) {
        accountingService.deleteLoan(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/result")
    public ResponseEntity<ResultResponse> result(@RequestParam int year) {
        return ResponseEntity.ok(accountingService.preview(year));
    }

    /** Liasse complète (PDF + FEC) en ZIP. */
    @GetMapping("/liasse")
    public ResponseEntity<byte[]> liasse(@RequestParam int year) {
        byte[] zip = accountingService.liasseZip(year);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=liasse-lmnp-" + year + ".zip")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(zip);
    }

    /** Document individuel : type = pdf | fec. */
    @GetMapping("/document")
    public ResponseEntity<byte[]> document(@RequestParam int year, @RequestParam String type) {
        List<GeneratedDocument> docs = accountingService.documents(year);
        boolean wantPdf = "pdf".equalsIgnoreCase(type);
        GeneratedDocument doc = docs.stream()
                .filter(d -> wantPdf ? d.contentType().contains("pdf") : d.contentType().contains("plain"))
                .findFirst().orElseThrow();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + doc.filename())
                .contentType(MediaType.parseMediaType(doc.contentType()))
                .body(doc.content());
    }
}
