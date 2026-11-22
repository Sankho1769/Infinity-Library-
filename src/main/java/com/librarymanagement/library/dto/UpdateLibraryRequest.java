package com.librarymanagement.library.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record UpdateLibraryRequest(
        @NotBlank(message = "Library name cannot be blank")
        String name,

        String address,
        String email,
        String phone,

        @Min(value = 1, message = "Loan duration must be at least 1 day")
        int loanDurationDays,

        @Min(value = 1, message = "Max loans per member must be at least 1")
        int maxLoansPerMember,

        @DecimalMin(value = "0.0", inclusive = true, message = "Fine rate cannot be negative")
        BigDecimal dailyFineRate,

        @DecimalMin(value = "0.0", inclusive = true, message = "Max fine cannot be negative")
        BigDecimal maxFinePerLoan,

        @Min(value = 1, message = "Reservation hold hours must be at least 1")
        int reservationHoldHours
) {}
