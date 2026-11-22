package com.librarymanagement.service;

import com.librarymanagement.catalog.domain.BookCopyStatus;
import com.librarymanagement.catalog.repository.BookCopyRepository;
import com.librarymanagement.catalog.repository.BookRepository;
import com.librarymanagement.circulation.domain.LoanStatus;
import com.librarymanagement.circulation.repository.LoanRepository;
import com.librarymanagement.fines.repository.FineRepository;
import com.librarymanagement.library.domain.Library;
import com.librarymanagement.library.repository.LibraryRepository;
import com.librarymanagement.reports.dto.ReportDTOs.*;
import com.librarymanagement.reports.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock private BookRepository bookRepository;
    @Mock private BookCopyRepository bookCopyRepository;
    @Mock private LoanRepository loanRepository;
    @Mock private FineRepository fineRepository;
    @Mock private LibraryRepository libraryRepository;

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-22T10:00:00Z"), ZoneId.of("UTC"));
    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(
                bookRepository,
                bookCopyRepository,
                loanRepository,
                fineRepository,
                libraryRepository,
                clock
        );
    }

    @Test
    @DisplayName("Generate real-time inventory report with copy breakdown")
    void testInventoryReport() {
        when(bookRepository.countByActive(true)).thenReturn(10L);
        when(bookCopyRepository.count()).thenReturn(50L);
        when(bookCopyRepository.countByStatus(BookCopyStatus.AVAILABLE)).thenReturn(35L);
        when(bookCopyRepository.countByStatus(BookCopyStatus.ON_LOAN)).thenReturn(10L);
        when(bookCopyRepository.countByStatus(BookCopyStatus.RESERVED)).thenReturn(3L);
        when(bookCopyRepository.countByStatus(BookCopyStatus.LOST)).thenReturn(1L);
        when(bookCopyRepository.countByStatus(BookCopyStatus.UNDER_MAINTENANCE)).thenReturn(1L);
        when(bookCopyRepository.countByStatus(BookCopyStatus.WITHDRAWN)).thenReturn(0L);

        InventoryReport report = reportService.getInventoryReport();

        assertThat(report.totalTitles()).isEqualTo(10L);
        assertThat(report.totalCopies()).isEqualTo(50L);
        assertThat(report.availableCopies()).isEqualTo(35L);
        assertThat(report.onLoanCopies()).isEqualTo(10L);
    }

    @Test
    @DisplayName("Generate circulation metrics report")
    void testCirculationReport() {
        when(loanRepository.countByStatus(LoanStatus.ACTIVE)).thenReturn(15L);
        when(loanRepository.countByStatus(LoanStatus.RETURNED)).thenReturn(85L);
        when(loanRepository.countOverdueLoans(any(Instant.class))).thenReturn(2L);
        when(loanRepository.countDistinctBorrowingMembers(LoanStatus.ACTIVE)).thenReturn(12L);

        CirculationReport report = reportService.getCirculationReport();

        assertThat(report.totalActiveLoans()).isEqualTo(15L);
        assertThat(report.totalReturnedLoans()).isEqualTo(85L);
        assertThat(report.totalOverdueLoans()).isEqualTo(2L);
        assertThat(report.activeBorrowingMembersCount()).isEqualTo(12L);
    }

    @Test
    @DisplayName("Generate financial fine summary report")
    void testFineSummaryReport() {
        when(fineRepository.sumTotalAccruedFines()).thenReturn(new BigDecimal("1250.00"));
        when(fineRepository.sumTotalCollectedFines()).thenReturn(new BigDecimal("900.00"));
        when(fineRepository.sumTotalWaivedFines()).thenReturn(new BigDecimal("50.00"));
        when(fineRepository.sumTotalOutstandingFines()).thenReturn(new BigDecimal("300.00"));
        when(fineRepository.countUnpaidFines()).thenReturn(6L);

        FineSummaryReport report = reportService.getFineSummaryReport();

        assertThat(report.totalFinesAccrued()).isEqualByComparingTo("1250.00");
        assertThat(report.totalFinesCollected()).isEqualByComparingTo("900.00");
        assertThat(report.totalFinesWaived()).isEqualByComparingTo("50.00");
        assertThat(report.totalFinesOutstanding()).isEqualByComparingTo("300.00");
        assertThat(report.totalUnpaidFinesCount()).isEqualTo(6L);
    }
}
