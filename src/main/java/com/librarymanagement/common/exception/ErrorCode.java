package com.librarymanagement.common.exception;

public enum ErrorCode {
    RESOURCE_NOT_FOUND("LMS-404-01", "Requested resource does not exist"),
    VALIDATION_FAILED("LMS-400-01", "Request validation failed"),
    INVALID_OPERATION("LMS-400-02", "Operation violates business invariants"),
    BOOK_NOT_AVAILABLE("LMS-409-01", "No available physical copy for this book"),
    MEMBER_INACTIVE("LMS-403-01", "Member account is not active or has expired"),
    LOAN_LIMIT_EXCEEDED("LMS-409-02", "Member has reached maximum borrowing limit"),
    LOAN_ALREADY_RETURNED("LMS-409-03", "This loan has already been closed and returned"),
    CONCURRENT_MODIFICATION("LMS-409-04", "Resource was concurrently modified or locked by another transaction"),
    RESERVATION_NOT_ELIGIBLE("LMS-409-05", "Member is not eligible to reserve this book"),
    UNAUTHORIZED_OPERATION("LMS-401-01", "Authentication required or token expired"),
    ACCESS_DENIED("LMS-403-02", "Insufficient permissions for requested resource"),
    FINE_OUTSTANDING_EXCEEDED("LMS-409-06", "Outstanding fines exceed permissible threshold"),
    IDEMPOTENCY_CONFLICT("LMS-409-07", "Conflicting idempotent request payload"),
    INTERNAL_SERVER_ERROR("LMS-500-01", "An unexpected internal server error occurred");

    private final String code;
    private final String defaultMessage;

    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
