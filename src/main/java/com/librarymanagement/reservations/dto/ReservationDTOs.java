package com.librarymanagement.reservations.dto;

import com.librarymanagement.reservations.domain.Reservation;
import com.librarymanagement.reservations.domain.ReservationStatus;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public class ReservationDTOs {

    public record CreateReservationRequest(
            @NotNull(message = "Book ID is required")
            Long bookId,

            Long memberId
    ) {}

    public record ReservationResponse(
            Long id,
            Long memberId,
            String memberName,
            Long bookId,
            String bookTitle,
            Long bookCopyId,
            String barcode,
            int queuePosition,
            ReservationStatus status,
            Instant requestDate,
            Instant holdExpiresAt
    ) {
        public static ReservationResponse from(Reservation reservation) {
            return new ReservationResponse(
                    reservation.getId(),
                    reservation.getMember().getId(),
                    reservation.getMember().getName(),
                    reservation.getBook().getId(),
                    reservation.getBook().getTitle(),
                    reservation.getBookCopy() != null ? reservation.getBookCopy().getId() : null,
                    reservation.getBookCopy() != null ? reservation.getBookCopy().getBarcode() : null,
                    reservation.getQueuePosition(),
                    reservation.getStatus(),
                    reservation.getRequestDate(),
                    reservation.getHoldExpiresAt()
            );
        }
    }
}
