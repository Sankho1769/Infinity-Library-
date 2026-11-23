package com.librarymanagement.circulation.dto;

import com.librarymanagement.circulation.domain.Loan;
import com.librarymanagement.circulation.domain.LoanStatus;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public class CirculationDTOs {

    public record IssueBookRequest(
            @NotNull(message = "Member ID is required")
            Long memberId,

            Long bookCopyId,

            Long bookId,

            String barcode
    ) {
        public IssueBookRequest(Long memberId, Long bookCopyId, Long bookId) {
            this(memberId, bookCopyId, bookId, null);
        }
    }

    public record ReturnBookRequest(
            Long loanId,
            String barcode
    ) {}

    public record LoanResponse(
            Long id,
            Long memberId,
            String memberName,
            Long bookId,
            String bookTitle,
            Long bookCopyId,
            String barcode,
            Instant issueDate,
            Instant dueDate,
            Instant returnDate,
            LoanStatus status,
            int loanDurationDays,
            BigDecimal dailyFineRateSnapshot
    ) {
        public static LoanResponse from(Loan loan) {
            return new LoanResponse(
                    loan.getId(),
                    loan.getMember().getId(),
                    loan.getMember().getName(),
                    loan.getBookCopy().getBook().getId(),
                    loan.getBookCopy().getBook().getTitle(),
                    loan.getBookCopy().getId(),
                    loan.getBookCopy().getBarcode(),
                    loan.getIssueDate(),
                    loan.getDueDate(),
                    loan.getReturnDate(),
                    loan.getStatus(),
                    loan.getLoanDurationDays(),
                    loan.getDailyFineRateSnapshot()
            );
        }
    }

    public record ReturnBookResponse(
            LoanResponse loan,
            boolean overdue,
            long overdueDays,
            BigDecimal fineAmount,
            Long fineId,
            boolean allocatedToReservation
    ) {}
}
