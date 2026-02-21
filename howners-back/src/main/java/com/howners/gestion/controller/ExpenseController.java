package com.howners.gestion.controller;

import com.howners.gestion.dto.expense.CreateExpenseRequest;
import com.howners.gestion.dto.expense.ExpenseResponse;
import com.howners.gestion.dto.expense.UpdateExpenseRequest;
import com.howners.gestion.service.expense.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<List<ExpenseResponse>> getAllExpenses() {
        return ResponseEntity.ok(expenseService.findByCurrentUser());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ExpenseResponse> getExpense(@PathVariable UUID id) {
        return ResponseEntity.ok(expenseService.findById(id));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ExpenseResponse> createExpense(
            @Valid @RequestPart("expense") CreateExpenseRequest request,
            @RequestPart(value = "justificatif", required = false) MultipartFile justificatif
    ) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED).body(expenseService.create(request, justificatif));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ExpenseResponse> updateExpense(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateExpenseRequest request
    ) {
        return ResponseEntity.ok(expenseService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<Void> deleteExpense(@PathVariable UUID id) {
        expenseService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/property/{propertyId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<List<ExpenseResponse>> getExpensesByProperty(@PathVariable UUID propertyId) {
        return ResponseEntity.ok(expenseService.findByPropertyId(propertyId));
    }
}
