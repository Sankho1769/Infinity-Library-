package com.librarymanagement.reservations.service;

import com.librarymanagement.catalog.domain.Book;
import com.librarymanagement.catalog.domain.BookCopy;
import com.librarymanagement.catalog.domain.BookCopyStatus;
import com.librarymanagement.catalog.repository.BookCopyRepository;
import com.librarymanagement.catalog.repository.BookRepository;
import com.librarymanagement.common.audit.AuditService;
import com.librarymanagement.common.exception.*;
import com.librarymanagement.common.security.UserPrincipal;
import com.librarymanagement.library.domain.Library;
import com.librarymanagement.library.repository.LibraryRepository;
import com.librarymanagement.members.domain.Member;
import com.librarymanagement.members.repository.MemberRepository;
import com.librarymanagement.reservations.domain.Reservation;
import com.librarymanagement.reservations.domain.ReservationStatus;
import com.librarymanagement.reservations.dto.ReservationDTOs.*;
import com.librarymanagement.reservations.repository.ReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class ReservationService {
    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);

    private final ReservationRepository reservationRepository;
    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final MemberRepository memberRepository;
    private final LibraryRepository libraryRepository;
    private final AuditService auditService;
    private final Clock clock;

    public ReservationService(
            ReservationRepository reservationRepository,
            BookRepository bookRepository,
            BookCopyRepository bookCopyRepository,
            MemberRepository memberRepository,
            LibraryRepository libraryRepository,
            AuditService auditService,
            Clock clock
    ) {
        this.reservationRepository = reservationRepository;
        this.bookRepository = bookRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.memberRepository = memberRepository;
        this.libraryRepository = libraryRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public ReservationResponse createReservation(CreateReservationRequest request, UserPrincipal principal) {
        Instant now = clock.instant();

        Long memberId = resolveMemberId(request.memberId(), principal);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member", memberId));

        if (!member.isBorrowingEligible()) {
            throw new BusinessException(ErrorCode.MEMBER_INACTIVE, "Member is not eligible to place reservations");
        }

        Book book = bookRepository.findById(request.bookId())
                .orElseThrow(() -> new ResourceNotFoundException("Book", request.bookId()));

        if (!book.isActive()) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION, "Book title is archived and cannot be reserved");
        }

        // Invariant: Prevent duplicate active reservations by the same member for the same book title
        boolean alreadyReserved = reservationRepository.existsByMemberIdAndBookIdAndStatusIn(
                member.getId(),
                book.getId(),
                List.of(ReservationStatus.PENDING, ReservationStatus.READY_FOR_PICKUP)
        );
        if (alreadyReserved) {
            throw new ConflictException(ErrorCode.RESERVATION_NOT_ELIGIBLE,
                    "Member already has an active reservation for book: " + book.getTitle());
        }

        // FIFO ordering: assign next queue position
        int nextPosition = reservationRepository.findMaxQueuePositionByBookId(book.getId()) + 1;

        Reservation reservation = new Reservation(member, book, nextPosition, now);
        Reservation saved = reservationRepository.save(reservation);

        auditService.record(
                principal != null ? principal.getUsername() : "MEMBER",
                "RESERVATION_CREATED",
                "RESERVATION",
                saved.getId().toString(),
                null,
                String.format("Created FIFO reservation position #%d for book '%s' (Member: %s)", nextPosition, book.getTitle(), member.getMembershipNumber())
        );

        return ReservationResponse.from(saved);
    }

    @Transactional
    public void cancelReservation(Long reservationId, UserPrincipal principal) {
        Instant now = clock.instant();

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", reservationId));

        // Object-Level Authorization Check
        if (principal != null && "MEMBER".equalsIgnoreCase(principal.getRole())) {
            if (!reservation.getMember().getId().equals(principal.getMemberId())) {
                throw new ForbiddenException(ErrorCode.ACCESS_DENIED, "Access denied: cannot cancel reservation of another member");
            }
        }

        if (reservation.getStatus() != ReservationStatus.PENDING && reservation.getStatus() != ReservationStatus.READY_FOR_PICKUP) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION,
                    "Cannot cancel reservation in status: " + reservation.getStatus());
        }

        BookCopy heldCopy = reservation.getBookCopy();
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setUpdatedAt(now);
        reservationRepository.save(reservation);

        // If a physical copy was allocated to this reservation, reallocate or release it
        if (heldCopy != null) {
            reallocateOrReleaseCopy(heldCopy, now);
        }

        auditService.record(
                principal != null ? principal.getUsername() : "MEMBER",
                "RESERVATION_CANCELLED",
                "RESERVATION",
                reservation.getId().toString(),
                null,
                "Cancelled reservation #" + reservation.getId()
        );
    }

    @Scheduled(fixedRate = 300000) // Runs every 5 minutes
    @Transactional
    public void expireUnclaimedReservations() {
        Instant now = clock.instant();
        List<Reservation> expiredList = reservationRepository.findExpiredHolds(now);

        for (Reservation res : expiredList) {
            log.info("Expiring unclaimed reservation #{} for member {}", res.getId(), res.getMember().getId());
            res.setStatus(ReservationStatus.EXPIRED);
            res.setUpdatedAt(now);
            BookCopy heldCopy = res.getBookCopy();
            res.setBookCopy(null);
            reservationRepository.save(res);

            if (heldCopy != null) {
                reallocateOrReleaseCopy(heldCopy, now);
            }

            auditService.record("SYSTEM", "RESERVATION_EXPIRED", "RESERVATION", res.getId().toString(), null,
                    "Unclaimed reservation hold expired");
        }
    }

    private void reallocateOrReleaseCopy(BookCopy copy, Instant now) {
        Optional<Reservation> next = reservationRepository.findFirstByBookIdAndStatusOrderByQueuePositionAsc(
                copy.getBook().getId(), ReservationStatus.PENDING);

        if (next.isPresent()) {
            Reservation nextRes = next.get();
            Library library = libraryRepository.findAll().stream().findFirst().orElse(null);
            int holdHours = library != null ? library.getReservationHoldHours() : 48;

            nextRes.setBookCopy(copy);
            nextRes.setStatus(ReservationStatus.READY_FOR_PICKUP);
            nextRes.setHoldExpiresAt(now.plus(holdHours, ChronoUnit.HOURS));
            nextRes.setUpdatedAt(now);
            reservationRepository.save(nextRes);

            copy.setStatus(BookCopyStatus.RESERVED);
            copy.setUpdatedAt(now);
            bookCopyRepository.save(copy);
        } else {
            copy.setStatus(BookCopyStatus.AVAILABLE);
            copy.setUpdatedAt(now);
            bookCopyRepository.save(copy);
        }
    }

    @Transactional(readOnly = true)
    public ReservationResponse getReservationById(Long reservationId, UserPrincipal principal) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", reservationId));

        if (principal != null && "MEMBER".equalsIgnoreCase(principal.getRole())) {
            if (!reservation.getMember().getId().equals(principal.getMemberId())) {
                throw new ForbiddenException(ErrorCode.ACCESS_DENIED, "Access denied: cannot access reservation of another member");
            }
        }

        return ReservationResponse.from(reservation);
    }

    @Transactional(readOnly = true)
    public Page<ReservationResponse> getReservations(Long memberId, ReservationStatus status, Pageable pageable) {
        Page<Reservation> page;
        if (memberId != null) {
            page = reservationRepository.findByMemberId(memberId, pageable);
        } else if (status != null) {
            page = reservationRepository.findByStatus(status, pageable);
        } else {
            page = reservationRepository.findAll(pageable);
        }
        return page.map(ReservationResponse::from);
    }

    private Long resolveMemberId(Long requestedMemberId, UserPrincipal principal) {
        if (principal != null && "MEMBER".equalsIgnoreCase(principal.getRole())) {
            if (principal.getMemberId() == null) {
                throw new BusinessException(ErrorCode.MEMBER_INACTIVE, "User is not linked to any registered member");
            }
            return principal.getMemberId();
        }
        if (requestedMemberId != null) {
            return requestedMemberId;
        }
        if (principal != null && principal.getMemberId() != null) {
            return principal.getMemberId();
        }
        throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Member ID is required");
    }
}
