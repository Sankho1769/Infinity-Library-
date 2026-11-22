package com.librarymanagement.reports.dto;

import com.librarymanagement.circulation.dto.CirculationDTOs.LoanResponse;

import java.math.BigDecimal;
import java.util.List;

public class ReportDTOs {

    public record InventoryReport(
            long totalTitles,
            long totalCopies,
            long availableCopies,
            long onLoanCopies,
            long reservedCopies,
            long lostCopies,
            long underMaintenanceCopies,
            long withdrawnCopies
    ) {}

    public record CirculationReport(
            long totalActiveLoans,
            long totalReturnedLoans,
            long totalOverdueLoans,
            long activeBorrowingMembersCount
    ) {}

    public record OverdueLoanItem(
            Long loanId,
            Long memberId,
            String memberName,
            String memberEmail,
            Long bookId,
            String bookTitle,
            String barcode,
            long daysOverdue,
            BigDecimal estimatedFine
    ) {}

    public record OverdueReport(
            long totalOverdueLoans,
            BigDecimal totalEstimatedFines,
            List<OverdueLoanItem> overdueLoans
    ) {}

    public record FineSummaryReport(
            BigDecimal totalFinesAccrued,
            BigDecimal totalFinesCollected,
            BigDecimal totalFinesWaived,
            BigDecimal totalFinesOutstanding,
            long totalUnpaidFinesCount
    ) {}
}
