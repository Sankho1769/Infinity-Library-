package com.librarymanagement.service;

import com.librarymanagement.catalog.domain.Book;
import com.librarymanagement.catalog.domain.BookCopy;
import com.librarymanagement.catalog.domain.BookCopyStatus;
import com.librarymanagement.catalog.repository.BookCopyRepository;
import com.librarymanagement.catalog.repository.BookRepository;
import com.librarymanagement.common.audit.AuditService;
import com.librarymanagement.common.exception.ConflictException;
import com.librarymanagement.common.security.UserPrincipal;
import com.librarymanagement.library.domain.Library;
import com.librarymanagement.library.repository.LibraryRepository;
import com.librarymanagement.members.domain.Member;
import com.librarymanagement.members.domain.MemberStatus;
import com.librarymanagement.members.repository.MemberRepository;
import com.librarymanagement.reservations.domain.Reservation;
import com.librarymanagement.reservations.domain.ReservationStatus;
import com.librarymanagement.reservations.dto.ReservationDTOs.*;
import com.librarymanagement.reservations.repository.ReservationRepository;
import com.librarymanagement.reservations.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class ReservationServiceTest {

    @Mock private ReservationRepository reservationRepository;
    @Mock private BookRepository bookRepository;
    @Mock private BookCopyRepository bookCopyRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private LibraryRepository libraryRepository;
    @Mock private AuditService auditService;

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-22T10:00:00Z"), ZoneId.of("UTC"));
    private ReservationService reservationService;

    private Member member;
    private Book book;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        reservationService = new ReservationService(
                reservationRepository,
                bookRepository,
                bookCopyRepository,
                memberRepository,
                libraryRepository,
                auditService,
                clock
        );

        member = new Member();
        member.setId(10L);
        member.setName("Marcus Vance");
        member.setMembershipNumber("MEM-002");
        member.setStatus(MemberStatus.ACTIVE);
        member.setMembershipExpiryDate(LocalDate.now().plusMonths(6));

        book = new Book();
        book.setId(100L);
        book.setTitle("Clean Architecture");
        book.setActive(true);

        principal = new UserPrincipal(1L, "marcus", "pass", "MEMBER", 10L, true);
    }

    @Test
    @DisplayName("Create reservation enforces FIFO queue positioning")
    void testCreateReservationFifoOrdering() {
        when(memberRepository.findById(10L)).thenReturn(Optional.of(member));
        when(bookRepository.findById(100L)).thenReturn(Optional.of(book));
        when(reservationRepository.existsByMemberIdAndBookIdAndStatusIn(eq(10L), eq(100L), any())).thenReturn(false);
        when(reservationRepository.findMaxQueuePositionByBookId(100L)).thenReturn(2); // Existing queue has 2 holds

        Reservation savedReservation = new Reservation(member, book, 3, clock.instant());
        savedReservation.setId(55L);
        when(reservationRepository.save(any(Reservation.class))).thenReturn(savedReservation);

        CreateReservationRequest request = new CreateReservationRequest(100L, 10L);
        ReservationResponse response = reservationService.createReservation(request, principal);

        assertThat(response).isNotNull();
        assertThat(response.queuePosition()).isEqualTo(3);
        assertThat(response.status()).isEqualTo(ReservationStatus.PENDING);
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    @DisplayName("Reject duplicate active reservation for same member and book")
    void testRejectDuplicateActiveReservation() {
        when(memberRepository.findById(10L)).thenReturn(Optional.of(member));
        when(bookRepository.findById(100L)).thenReturn(Optional.of(book));
        when(reservationRepository.existsByMemberIdAndBookIdAndStatusIn(eq(10L), eq(100L), any())).thenReturn(true);

        CreateReservationRequest request = new CreateReservationRequest(100L, 10L);

        assertThatThrownBy(() -> reservationService.createReservation(request, principal))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already has an active reservation");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cancel reservation releases copy to next pending hold or AVAILABLE")
    void testCancelReservationReallocatesCopy() {
        BookCopy copy = new BookCopy();
        copy.setId(201L);
        copy.setBook(book);
        copy.setStatus(BookCopyStatus.RESERVED);

        Reservation res = new Reservation(member, book, 1, clock.instant());
        res.setId(77L);
        res.setStatus(ReservationStatus.READY_FOR_PICKUP);
        res.setBookCopy(copy);

        when(reservationRepository.findById(77L)).thenReturn(Optional.of(res));

        Member nextMember = new Member();
        nextMember.setId(11L);
        Reservation nextRes = new Reservation(nextMember, book, 2, clock.instant());
        when(reservationRepository.findFirstByBookIdAndStatusOrderByQueuePositionAsc(100L, ReservationStatus.PENDING))
                .thenReturn(Optional.of(nextRes));

        Library lib = new Library();
        lib.setReservationHoldHours(48);
        when(libraryRepository.findAll()).thenReturn(List.of(lib));

        reservationService.cancelReservation(77L, principal);

        assertThat(res.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(nextRes.getStatus()).isEqualTo(ReservationStatus.READY_FOR_PICKUP);
        assertThat(nextRes.getBookCopy()).isEqualTo(copy);
        assertThat(copy.getStatus()).isEqualTo(BookCopyStatus.RESERVED);
    }
}
