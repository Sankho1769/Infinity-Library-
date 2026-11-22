package com.librarymanagement.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.librarymanagement.catalog.domain.Book;
import com.librarymanagement.catalog.domain.BookCopy;
import com.librarymanagement.catalog.domain.BookCopyStatus;
import com.librarymanagement.catalog.repository.BookCopyRepository;
import com.librarymanagement.catalog.repository.BookRepository;
import com.librarymanagement.circulation.domain.Loan;
import com.librarymanagement.circulation.domain.LoanStatus;
import com.librarymanagement.circulation.dto.CirculationDTOs.*;
import com.librarymanagement.circulation.repository.LoanRepository;
import com.librarymanagement.circulation.service.CirculationService;
import com.librarymanagement.common.audit.AuditService;
import com.librarymanagement.common.exception.BusinessException;
import com.librarymanagement.common.exception.ConflictException;
import com.librarymanagement.common.exception.ErrorCode;
import com.librarymanagement.common.idempotency.IdempotencyRepository;
import com.librarymanagement.fines.domain.Fine;
import com.librarymanagement.fines.repository.FineRepository;
import com.librarymanagement.library.domain.Library;
import com.librarymanagement.library.repository.LibraryRepository;
import com.librarymanagement.members.domain.Member;
import com.librarymanagement.members.domain.MemberStatus;
import com.librarymanagement.members.repository.MemberRepository;
import com.librarymanagement.reservations.domain.Reservation;
import com.librarymanagement.reservations.domain.ReservationStatus;
import com.librarymanagement.reservations.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CirculationServiceTest {

    @Mock private LoanRepository loanRepository;
    @Mock private BookCopyRepository bookCopyRepository;
    @Mock private BookRepository bookRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private LibraryRepository libraryRepository;
    @Mock private ReservationRepository reservationRepository;
    @Mock private FineRepository fineRepository;
    @Mock private IdempotencyRepository idempotencyRepository;
    @Mock private AuditService auditService;

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-22T10:00:00Z"), ZoneId.of("UTC"));
    private final ObjectMapper objectMapper = new ObjectMapper();

    private CirculationService circulationService;
    private Library library;
    private Member activeMember;
    private Book book;
    private BookCopy availableCopy;

    @BeforeEach
    void setUp() {
        circulationService = new CirculationService(
                loanRepository,
                bookCopyRepository,
                bookRepository,
                memberRepository,
                libraryRepository,
                reservationRepository,
                fineRepository,
                idempotencyRepository,
                auditService,
                clock,
                objectMapper
        );

        library = new Library();
        library.setId(1L);
        library.setName("Central Library");
        library.setTimezone("UTC");
        library.setLoanDurationDays(14);
        library.setMaxLoansPerMember(5);
        library.setDailyFineRate(new BigDecimal("5.00"));
        library.setMaxFinePerLoan(new BigDecimal("500.00"));
        library.setReservationHoldHours(48);

        activeMember = new Member();
        activeMember.setId(10L);
        activeMember.setMembershipNumber("MEM-001");
        activeMember.setName("Elena Rostova");
        activeMember.setStatus(MemberStatus.ACTIVE);
        activeMember.setMembershipExpiryDate(LocalDate.now().plusMonths(6));

        book = new Book();
        book.setId(100L);
        book.setTitle("Designing Data-Intensive Applications");
        book.setIsbn("978-1449373320");

        availableCopy = new BookCopy();
        availableCopy.setId(500L);
        availableCopy.setBook(book);
        availableCopy.setBarcode("DDIA-CPY-01");
        availableCopy.setStatus(BookCopyStatus.AVAILABLE);
    }

    @Test
    @DisplayName("Successfully issue an available book copy to an eligible member")
    void testIssueBookSuccess() {
        when(memberRepository.findById(10L)).thenReturn(Optional.of(activeMember));
        when(libraryRepository.findAll()).thenReturn(List.of(library));
        when(loanRepository.countByMemberIdAndStatus(10L, LoanStatus.ACTIVE)).thenReturn(0L);
        when(fineRepository.sumOutstandingFinesByMemberId(10L)).thenReturn(BigDecimal.ZERO);
        when(bookCopyRepository.findByIdForUpdate(500L)).thenReturn(Optional.of(availableCopy));
        when(loanRepository.existsByBookCopyIdAndStatus(500L, LoanStatus.ACTIVE)).thenReturn(false);

        Loan savedLoan = new Loan(activeMember, availableCopy, clock.instant(), clock.instant().plusSeconds(14 * 86400), 14, new BigDecimal("5.00"));
        savedLoan.setId(1L);
        when(loanRepository.save(any(Loan.class))).thenReturn(savedLoan);

        IssueBookRequest request = new IssueBookRequest(10L, 500L, null);
        LoanResponse response = circulationService.issueBook(request, null, "librarian");

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.barcode()).isEqualTo("DDIA-CPY-01");
        assertThat(availableCopy.getStatus()).isEqualTo(BookCopyStatus.ON_LOAN);

        verify(bookCopyRepository).save(availableCopy);
        verify(loanRepository).save(any(Loan.class));
    }

    @Test
    @DisplayName("Reject book issuing when member status is SUSPENDED or EXPIRED")
    void testIssueBookRejectsInactiveMember() {
        activeMember.setStatus(MemberStatus.SUSPENDED);
        when(memberRepository.findById(10L)).thenReturn(Optional.of(activeMember));

        IssueBookRequest request = new IssueBookRequest(10L, 500L, null);

        assertThatThrownBy(() -> circulationService.issueBook(request, null, "librarian"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Member is inactive, suspended, or membership has expired");

        verifyNoInteractions(loanRepository);
    }

    @Test
    @DisplayName("Reject book issuing when member reaches borrowing quota limit")
    void testIssueBookRejectsExceededLoanQuota() {
        when(memberRepository.findById(10L)).thenReturn(Optional.of(activeMember));
        when(libraryRepository.findAll()).thenReturn(List.of(library));
        when(loanRepository.countByMemberIdAndStatus(10L, LoanStatus.ACTIVE)).thenReturn(5L); // Max limit is 5

        IssueBookRequest request = new IssueBookRequest(10L, 500L, null);

        assertThatThrownBy(() -> circulationService.issueBook(request, null, "librarian"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("reached maximum borrowing limit");

        verify(bookCopyRepository, never()).findByIdForUpdate(any());
    }

    @Test
    @DisplayName("Reject issuing when selected copy is not available (concurrency protection)")
    void testIssueBookRejectsUnavailableCopy() {
        availableCopy.setStatus(BookCopyStatus.ON_LOAN); // Already issued by another transaction

        when(memberRepository.findById(10L)).thenReturn(Optional.of(activeMember));
        when(libraryRepository.findAll()).thenReturn(List.of(library));
        when(loanRepository.countByMemberIdAndStatus(10L, LoanStatus.ACTIVE)).thenReturn(1L);
        when(fineRepository.sumOutstandingFinesByMemberId(10L)).thenReturn(BigDecimal.ZERO);
        when(bookCopyRepository.findByIdForUpdate(500L)).thenReturn(Optional.of(availableCopy));

        IssueBookRequest request = new IssueBookRequest(10L, 500L, null);

        assertThatThrownBy(() -> circulationService.issueBook(request, null, "librarian"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Book copy is not available for issue");
    }

    @Test
    @DisplayName("Successfully return book, calculate overdue fine, and allocate to pending FIFO reservation")
    void testReturnBookWithOverdueAndPendingReservation() {
        Instant issueDate = Instant.parse("2026-11-01T10:00:00Z");
        Instant dueDate = Instant.parse("2026-11-15T10:00:00Z"); // Due 7 days ago (current time is 2026-11-22)
        Loan loan = new Loan(activeMember, availableCopy, issueDate, dueDate, 14, new BigDecimal("5.00"));
        loan.setId(42L);

        when(loanRepository.findById(42L)).thenReturn(Optional.of(loan));
        when(libraryRepository.findAll()).thenReturn(List.of(library));

        Fine savedFine = new Fine(loan, activeMember, new BigDecimal("35.00"), LocalDate.of(2026, 11, 22));
        savedFine.setId(99L);
        when(fineRepository.save(any(Fine.class))).thenReturn(savedFine);

        // Pending FIFO reservation waiting
        Member waitingMember = new Member();
        waitingMember.setId(20L);
        Reservation waitingReservation = new Reservation(waitingMember, book, 1, Instant.now());
        when(reservationRepository.findFirstByBookIdAndStatusOrderByQueuePositionAsc(100L, ReservationStatus.PENDING))
                .thenReturn(Optional.of(waitingReservation));

        ReturnBookRequest request = new ReturnBookRequest(42L, null);
        ReturnBookResponse response = circulationService.returnBook(request, "librarian");

        assertThat(response).isNotNull();
        assertThat(response.overdue()).isTrue();
        assertThat(response.overdueDays()).isEqualTo(7L);
        assertThat(response.fineAmount()).isEqualByComparingTo("35.00");
        assertThat(response.allocatedToReservation()).isTrue();

        assertThat(loan.getStatus()).isEqualTo(LoanStatus.RETURNED);
        assertThat(availableCopy.getStatus()).isEqualTo(BookCopyStatus.RESERVED);
        assertThat(waitingReservation.getStatus()).isEqualTo(ReservationStatus.READY_FOR_PICKUP);
    }

    @Test
    @DisplayName("Returning a book twice is rejected as LOAN_ALREADY_RETURNED")
    void testReturnBookTwiceFails() {
        Loan closedLoan = new Loan(activeMember, availableCopy, Instant.now().minusSeconds(86400), Instant.now(), 14, new BigDecimal("5.00"));
        closedLoan.setId(42L);
        closedLoan.setStatus(LoanStatus.RETURNED); // Already returned

        when(loanRepository.findById(42L)).thenReturn(Optional.of(closedLoan));

        ReturnBookRequest request = new ReturnBookRequest(42L, null);

        assertThatThrownBy(() -> circulationService.returnBook(request, "librarian"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already been closed and returned");
    }
}
