package com.librarymanagement.library.dto;

import com.librarymanagement.library.domain.Library;
import java.math.BigDecimal;

public record LibraryResponse(
        Long id,
        String name,
        String code,
        String address,
        String email,
        String phone,
        String timezone,
        int loanDurationDays,
        int maxLoansPerMember,
        BigDecimal dailyFineRate,
        BigDecimal maxFinePerLoan,
        int reservationHoldHours,
        boolean active
) {
    public static LibraryResponse from(Library lib) {
        return new LibraryResponse(
                lib.getId(),
                lib.getName(),
                lib.getCode(),
                lib.getAddress(),
                lib.getEmail(),
                lib.getPhone(),
                lib.getTimezone(),
                lib.getLoanDurationDays(),
                lib.getMaxLoansPerMember(),
                lib.getDailyFineRate(),
                lib.getMaxFinePerLoan(),
                lib.getReservationHoldHours(),
                lib.isActive()
        );
    }
}
