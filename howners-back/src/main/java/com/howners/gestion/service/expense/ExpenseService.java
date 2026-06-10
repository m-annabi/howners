package com.howners.gestion.service.expense;

import com.howners.gestion.domain.document.Document;
import com.howners.gestion.domain.document.DocumentType;
import com.howners.gestion.domain.expense.Expense;
import com.howners.gestion.domain.property.Property;
import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.domain.user.Role;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.expense.CreateExpenseRequest;
import com.howners.gestion.dto.expense.ExpenseResponse;
import com.howners.gestion.dto.expense.UpdateExpenseRequest;
import com.howners.gestion.exception.ForbiddenException;
import com.howners.gestion.exception.ResourceNotFoundException;
import com.howners.gestion.repository.DocumentRepository;
import com.howners.gestion.repository.ExpenseRepository;
import com.howners.gestion.repository.PropertyRepository;
import com.howners.gestion.repository.RentalRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.service.auth.AuthService;
import com.howners.gestion.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final PropertyRepository propertyRepository;
    private final RentalRepository rentalRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final StorageService storageService;

    @Transactional(readOnly = true)
    public List<ExpenseResponse> findByCurrentUser() {
        UUID currentUserId = AuthService.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId.toString()));

        if (currentUser.getRole() == Role.ADMIN) {
            return expenseRepository.findAll().stream()
                    .map(ExpenseResponse::from)
                    .collect(Collectors.toList());
        }

        return expenseRepository.findByOwnerId(currentUserId).stream()
                .map(ExpenseResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ExpenseResponse findById(UUID expenseId) {
        Expense expense = findExpenseAndCheckAccess(expenseId);
        return ExpenseResponse.from(expense);
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> findByPropertyId(UUID propertyId) {
        UUID currentUserId = AuthService.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId.toString()));

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property", "id", propertyId.toString()));

        if (currentUser.getRole() != Role.ADMIN && !property.getOwner().getId().equals(currentUserId)) {
            throw new ForbiddenException("You are not authorized to access expenses for this property");
        }

        return expenseRepository.findByPropertyId(propertyId).stream()
                .map(ExpenseResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public ExpenseResponse create(CreateExpenseRequest request, MultipartFile justificatif) throws IOException {
        UUID currentUserId = AuthService.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId.toString()));

        Property property = null;
        Rental rental = null;

        if (request.propertyId() != null) {
            property = propertyRepository.findById(request.propertyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Property", "id", request.propertyId().toString()));
            if (!property.getOwner().getId().equals(currentUserId) && currentUser.getRole() != Role.ADMIN) {
                throw new ForbiddenException("You are not authorized to add expenses to this property");
            }
        }

        if (request.rentalId() != null) {
            rental = rentalRepository.findById(request.rentalId())
                    .orElseThrow(() -> new ResourceNotFoundException("Rental", "id", request.rentalId().toString()));
            if (property == null) {
                property = rental.getProperty();
            }
        }

        Document document = null;
        if (justificatif != null && !justificatif.isEmpty()) {
            String fileKey = storageService.uploadFile(
                    justificatif.getBytes(),
                    justificatif.getOriginalFilename(),
                    justificatif.getContentType()
            );
            document = Document.builder()
                    .property(property)
                    .rental(rental)
                    .uploader(currentUser)
                    .documentType(DocumentType.OTHER)
                    .fileName(justificatif.getOriginalFilename())
                    .filePath(fileKey)
                    .fileKey(fileKey)
                    .fileSize(justificatif.getSize())
                    .mimeType(justificatif.getContentType())
                    .description("Justificatif de dépense")
                    .build();
            document = documentRepository.save(document);
        }

        Expense expense = Expense.builder()
                .property(property)
                .rental(rental)
                .category(request.category())
                .description(request.description())
                .amount(request.amount())
                .currency("EUR")
                .expenseDate(request.expenseDate())
                .document(document)
                .createdBy(currentUser)
                .build();

        expense = expenseRepository.save(expense);
        log.info("Expense created with id {} for property {}", expense.getId(),
                property != null ? property.getId() : "none");

        return ExpenseResponse.from(expense);
    }

    @Transactional
    public ExpenseResponse update(UUID expenseId, UpdateExpenseRequest request) {
        Expense expense = findExpenseAndCheckAccess(expenseId);

        if (request.category() != null) expense.setCategory(request.category());
        if (request.description() != null) expense.setDescription(request.description());
        if (request.amount() != null) expense.setAmount(request.amount());
        if (request.expenseDate() != null) expense.setExpenseDate(request.expenseDate());

        if (request.propertyId() != null) {
            Property property = propertyRepository.findById(request.propertyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Property", "id", request.propertyId().toString()));
            expense.setProperty(property);
        }

        if (request.rentalId() != null) {
            Rental rental = rentalRepository.findById(request.rentalId())
                    .orElseThrow(() -> new ResourceNotFoundException("Rental", "id", request.rentalId().toString()));
            expense.setRental(rental);
        }

        expense = expenseRepository.save(expense);
        log.info("Expense {} updated", expenseId);

        return ExpenseResponse.from(expense);
    }

    @Transactional
    public void delete(UUID expenseId) {
        Expense expense = findExpenseAndCheckAccess(expenseId);
        expenseRepository.delete(expense);
        log.info("Expense {} deleted", expenseId);
    }

    private Expense findExpenseAndCheckAccess(UUID expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", "id", expenseId.toString()));

        UUID currentUserId = AuthService.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId.toString()));

        if (currentUser.getRole() == Role.ADMIN) return expense;

        if (expense.getProperty() != null && !expense.getProperty().getOwner().getId().equals(currentUserId)) {
            throw new ForbiddenException("You are not authorized to access this expense");
        }

        return expense;
    }
}
