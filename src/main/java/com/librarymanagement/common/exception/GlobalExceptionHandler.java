package com.librarymanagement.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                "urn:library:error:not-found",
                "Resource Not Found",
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                request.getRequestURI(),
                ex.getErrorCode().getCode(),
                null
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex, HttpServletRequest request) {
        log.warn("Conflict condition: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                "urn:library:error:conflict",
                "Business Conflict",
                HttpStatus.CONFLICT.value(),
                ex.getMessage(),
                request.getRequestURI(),
                ex.getErrorCode().getCode(),
                null
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        log.warn("Business rule violation: {}", ex.getMessage());
        HttpStatus status = switch (ex.getErrorCode()) {
            case RESOURCE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case BOOK_NOT_AVAILABLE, LOAN_LIMIT_EXCEEDED, LOAN_ALREADY_RETURNED,
                 CONCURRENT_MODIFICATION, FINE_OUTSTANDING_EXCEEDED, RESERVATION_NOT_ELIGIBLE, IDEMPOTENCY_CONFLICT -> HttpStatus.CONFLICT;
            case MEMBER_INACTIVE, ACCESS_DENIED -> HttpStatus.FORBIDDEN;
            case UNAUTHORIZED_OPERATION -> HttpStatus.UNAUTHORIZED;
            default -> HttpStatus.BAD_REQUEST;
        };

        ErrorResponse error = new ErrorResponse(
                "urn:library:error:business-rule",
                "Business Invariant Violation",
                status.value(),
                ex.getMessage(),
                request.getRequestURI(),
                ex.getErrorCode().getCode(),
                null
        );
        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }

        ErrorResponse error = new ErrorResponse(
                "urn:library:error:validation",
                "Invalid Request Payload",
                HttpStatus.BAD_REQUEST.value(),
                "One or more fields failed validation requirements.",
                request.getRequestURI(),
                ErrorCode.VALIDATION_FAILED.getCode(),
                fieldErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied for URI {}: {}", request.getRequestURI(), ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                "urn:library:error:forbidden",
                "Forbidden",
                HttpStatus.FORBIDDEN.value(),
                "You do not possess the required privileges to access this resource or perform this operation.",
                request.getRequestURI(),
                ErrorCode.ACCESS_DENIED.getCode(),
                null
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        log.warn("Authentication failed: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                "urn:library:error:unauthorized",
                "Unauthorized",
                HttpStatus.UNAUTHORIZED.value(),
                ex.getMessage(),
                request.getRequestURI(),
                ErrorCode.UNAUTHORIZED_OPERATION.getCode(),
                null
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception processing request {}", request.getRequestURI(), ex);
        ErrorResponse error = new ErrorResponse(
                "urn:library:error:internal",
                "Internal Server Error",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected server error occurred. Please contact the system administrator.",
                request.getRequestURI(),
                ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
