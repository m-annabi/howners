package com.howners.gestion.controller;

import com.howners.gestion.dto.analytics.Declaration2044Response;
import com.howners.gestion.service.export.ExportFiscal2044Service;
import com.howners.gestion.service.export.ExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Year;

@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;
    private final ExportFiscal2044Service exportFiscal2044Service;

    @GetMapping("/financial")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<byte[]> exportFinancial(
            @RequestParam(defaultValue = "#{T(java.time.Year).now().getValue()}") int year,
            @RequestParam(defaultValue = "csv") String format) {

        String csv = exportService.generateFinancialCsv(year);
        byte[] content = csv.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        String filename = "export-comptable-" + year + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .contentLength(content.length)
                .body(content);
    }

    @GetMapping("/fiscal-2044/apercu")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<Declaration2044Response> apercuFiscal2044(@RequestParam int annee) {
        return ResponseEntity.ok(exportFiscal2044Service.genererDeclaration(annee));
    }

    @GetMapping("/fiscal-2044")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<byte[]> exportFiscal2044(
            @RequestParam int annee,
            @RequestParam(defaultValue = "pdf") String format) {

        if ("csv".equalsIgnoreCase(format)) {
            byte[] content = exportFiscal2044Service.genererCsv(annee)
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"declaration-2044-" + annee + ".csv\"")
                    .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                    .contentLength(content.length)
                    .body(content);
        }

        byte[] pdf = exportFiscal2044Service.genererPdf(annee);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"declaration-2044-" + annee + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }
}
