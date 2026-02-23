package com.howners.gestion.exception;

import com.howners.gestion.dto.response.ErrorResponse;
import com.howners.gestion.exception.esignature.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ===== E-Signature Exception Handlers =====

    @ExceptionHandler(ContractNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleContractNotFoundException(ContractNotFoundException ex) {
        log.warn("Contract not found: {}", ex.getMessage());
        return buildESignatureErrorResponse(ex, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ContractInvalidStateException.class)
    public ResponseEntity<Map<String, Object>> handleContractInvalidStateException(ContractInvalidStateException ex) {
        log.warn("Contract invalid state: {}", ex.getMessage());
        return buildESignatureErrorResponse(ex, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SignatureRequestNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleSignatureRequestNotFoundException(SignatureRequestNotFoundException ex) {
        log.warn("Signature request not found: {}", ex.getMessage());
        return buildESignatureErrorResponse(ex, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<Map<String, Object>> handleTokenExpiredException(TokenExpiredException ex) {
        log.warn("Token expired: {}", ex.getMessage());
        return buildESignatureErrorResponse(ex, HttpStatus.GONE);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidTokenException(InvalidTokenException ex) {
        log.warn("Invalid token: {}", ex.getMessage());
        return buildESignatureErrorResponse(ex, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(DocumentDownloadException.class)
    public ResponseEntity<Map<String, Object>> handleDocumentDownloadException(DocumentDownloadException ex) {
        log.error("Document download failed: {}", ex.getMessage(), ex);
        return buildESignatureErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(EmailSendException.class)
    public ResponseEntity<Map<String, Object>> handleEmailSendException(EmailSendException ex) {
        log.error("Email send failed: {}", ex.getMessage(), ex);
        return buildESignatureErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ESignatureException.class)
    public ResponseEntity<Map<String, Object>> handleESignatureException(ESignatureException ex) {
        log.error("E-signature error: {}", ex.getMessage(), ex);
        return buildESignatureErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<Map<String, Object>> buildESignatureErrorResponse(ESignatureException ex, HttpStatus status) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now().toString());
        errorResponse.put("status", status.value());
        errorResponse.put("error", ex.getErrorCode());
        errorResponse.put("message", ex.getMessage());
        return ResponseEntity.status(status).body(errorResponse);
    }

    // ===== Subscription Exception Handlers =====

    @ExceptionHandler(PlanLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handlePlanLimitExceeded(
            PlanLimitExceededException ex,
            WebRequest request) {
        log.warn("Plan limit exceeded: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.PAYMENT_REQUIRED.value(),
                "Payment Required",
                ex.getMessage(),
                request.getDescription(false).replace("uri=", "")
        );

        return new ResponseEntity<>(error, HttpStatus.PAYMENT_REQUIRED);
    }

    // ===== General Exception Handlers =====

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex,
            WebRequest request) {
        log.warn("Business exception: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage(),
                request.getDescription(false).replace("uri=", "")
        );

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex,
            WebRequest request) {
        log.error("Resource not found: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage(),
                request.getDescription(false).replace("uri=", "")
        );
        
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(
            ForbiddenException ex,
            WebRequest request) {
        log.error("Forbidden: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                ex.getMessage(),
                request.getDescription(false).replace("uri=", "")
        );
        
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(
            BadRequestException ex,
            WebRequest request) {
        log.error("Bad request: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage(),
                request.getDescription(false).replace("uri=", "")
        );
        
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(
            ConflictException ex,
            WebRequest request) {
        log.error("Conflict: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "Conflict",
                ex.getMessage(),
                request.getDescription(false).replace("uri=", "")
        );
        
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            WebRequest request) {
        log.error("Access denied: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                "Vous n'avez pas les droits nécessaires pour effectuer cette action",
                request.getDescription(false).replace("uri=", "")
        );
        
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex,
            WebRequest request) {
        log.error("Bad credentials: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                "Email ou mot de passe incorrect",
                request.getDescription(false).replace("uri=", "")
        );
        
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex,
            WebRequest request) {
        log.error("Validation error: {}", ex.getMessage());
        
        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            validationErrors.put(fieldName, errorMessage);
        });
        
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Error",
                "Les données fournies sont invalides",
                request.getDescription(false).replace("uri=", ""),
                validationErrors
        );
        
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(
            ResponseStatusException ex,
            WebRequest request) {
        log.error("Response status exception: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
                ex.getStatusCode().value(),
                ex.getStatusCode().toString(),
                ex.getReason() != null ? ex.getReason() : "Une erreur est survenue",
                request.getDescription(false).replace("uri=", "")
        );
        
        return new ResponseEntity<>(error, ex.getStatusCode());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(
            RuntimeException ex,
            WebRequest request) {
        log.error("Runtime exception: {}", ex.getMessage(), ex);

        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "Une erreur inattendue est survenue",
                request.getDescription(false).replace("uri=", "")
        );

        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex,
            WebRequest request) {
        log.error("Unexpected exception: {}", ex.getMessage(), ex);

        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "Une erreur inattendue est survenue",
                request.getDescription(false).replace("uri=", "")
        );

        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
