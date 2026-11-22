package com.librarymanagement.reports.service;

import com.librarymanagement.catalog.domain.BookCopyStatus;
import com.librarymanagement.catalog.repository.BookCopyRepository;
import com.librarymanagement.catalog.repository.BookRepository;
import com.librarymanagement.circulation.domain.Loan;
import com.librarymanagement.circulation.domain.LoanStatus;
import com.librarymanagement.circulation.repository.LoanRepository;
import com.librarymanagement.fines.repository.FineRepository;
import com.librarymanagement.library.domain.Library;
import com.librarymanagement.library.repository.LibraryRepository;
import com.librarymanagement.reports.dto.ReportDTOs.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final LoanRepository loanRepository;
    private final FineRepository fineRepository;
    private final LibraryRepository libraryRepository;
    private final Clock clock;

    public ReportService(
            BookRepository bookRepository,
            BookCopyRepository bookCopyRepository,
            LoanRepository loanRepository,
            FineRepository fineRepository,
            LibraryRepository libraryRepository,
            Clock clock
    ) {
        this.bookRepository = bookRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.loanRepository = loanRepository;
        this.fineRepository = fineRepository;
        this.libraryRepository = libraryRepository;
        this.clock = clock;
    }

    public InventoryReport getInventoryReport() {
        return new InventoryReport(
                bookRepository.countByActive(true),
                bookCopyRepository.count(),
                bookCopyRepository.countByStatus(BookCopyStatus.AVAILABLE),
                bookCopyRepository.countByStatus(BookCopyStatus.ON_LOAN),
                bookCopyRepository.countByStatus(BookCopyStatus.RESERVED),
                bookCopyRepository.countByStatus(BookCopyStatus.LOST),
                bookCopyRepository.countByStatus(BookCopyStatus.UNDER_MAINTENANCE),
                bookCopyRepository.countByStatus(BookCopyStatus.WITHDRAWN)
        );
    }

    public CirculationReport getCirculationReport() {
        Instant now = clock.instant();
        return new CirculationReport(
                loanRepository.countByStatus(LoanStatus.ACTIVE),
                loanRepository.countByStatus(LoanStatus.RETURNED),
                loanRepository.countOverdueLoans(now),
                loanRepository.countDistinctBorrowingMembers(LoanStatus.ACTIVE)
        );
    }

    public OverdueReport getOverdueReport() {
        Instant now = clock.instant();
        Library library = getLibraryPolicy();
        ZoneId zoneId = ZoneId.of(library.getTimezone());
        LocalDate today = now.atZone(zoneId).toLocalDate();

        List<Loan> overdueLoans = loanRepository.findOverdueLoans(now);
        List<OverdueLoanItem> items = new ArrayList<>();
        BigDecimal totalFines = BigDecimal.ZERO;

        for (Loan loan : overdueLoans) {
            LocalDate dueDate = loan.getDueDate().atZone(zoneId).toLocalDate();
            long daysOverdue = Math.max(0, ChronoUnit.DAYS.between(dueDate, today));
            BigDecimal fine = library.getDailyFineRate()
                    .multiply(BigDecimal.valueOf(daysOverdue))
                    .min(library.getMaxFinePerLoan());

            totalFines = totalFines.add(fine);
            items.add(new OverdueLoanItem(
                    loan.getId(),
                    loan.getMember().getId(),
                    loan.getMember().getName(),
                    loan.getMember().getEmail(),
                    loan.getBookCopy().getBook().getId(),
                    loan.getBookCopy().getBook().getTitle(),
                    loan.getBookCopy().getBarcode(),
                    daysOverdue,
                    fine
            ));
        }

        return new OverdueReport(items.size(), totalFines, items);
    }

    public FineSummaryReport getFineSummaryReport() {
        return new FineSummaryReport(
                fineRepository.sumTotalAccruedFines(),
                fineRepository.sumTotalCollectedFines(),
                fineRepository.sumTotalWaivedFines(),
                fineRepository.sumTotalOutstandingFines(),
                fineRepository.countUnpaidFines()
        );
    }

    private Library getLibraryPolicy() {
        return libraryRepository.findAll().stream().findFirst()
                .orElseGet(() -> {
                    Library fallback = new Library();
                    fallback.setDailyFineRate(new BigDecimal("5.00"));
                    fallback.setMaxFinePerLoan(new BigDecimal("500.00"));
                    fallback.setTimezone("UTC");
                    return fallback;
                });
    }
}
