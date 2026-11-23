package com.librarymanagement.circulation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.librarymanagement.catalog.domain.BookCopy;
import com.librarymanagement.catalog.domain.BookCopyStatus;
import com.librarymanagement.catalog.repository.BookCopyRepository;
import com.librarymanagement.catalog.repository.BookRepository;
import com.librarymanagement.circulation.domain.Loan;
import com.librarymanagement.circulation.domain.LoanStatus;
import com.librarymanagement.circulation.dto.CirculationDTOs.*;
import com.librarymanagement.circulation.repository.LoanRepository;
import com.librarymanagement.common.audit.AuditService;
import com.librarymanagement.common.exception.*;
import com.librarymanagement.common.idempotency.IdempotencyRecord;
import com.librarymanagement.common.idempotency.IdempotencyRepository;
import com.librarymanagement.fines.domain.Fine;
import com.librarymanagement.fines.repository.FineRepository;
import com.librarymanagement.library.domain.Library;
import com.librarymanagement.library.repository.LibraryRepository;
import com.librarymanagement.members.domain.Member;
import com.librarymanagement.members.repository.MemberRepository;
import com.librarymanagement.reservations.domain.Reservation;
import com.librarymanagement.reservations.domain.ReservationStatus;
import com.librarymanagement.reservations.repository.ReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Service
public class CirculationService {
    private static final Logger log = LoggerFactory.getLogger(CirculationService.class);

    private final LoanRepository loanRepository;
    private final BookCopyRepository bookCopyRepository;
    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;
    private final LibraryRepository libraryRepository;
    private final ReservationRepository reservationRepository;
    private final FineRepository fineRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final AuditService auditService;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    public CirculationService(
            LoanRepository loanRepository,
            BookCopyRepository bookCopyRepository,
            BookRepository bookRepository,
            MemberRepository memberRepository,
            LibraryRepository libraryRepository,
            ReservationRepository reservationRepository,
            FineRepository fineRepository,
            IdempotencyRepository idempotencyRepository,
            AuditService auditService,
            Clock clock,
            ObjectMapper objectMapper
    ) {
        this.loanRepository = loanRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.bookRepository = bookRepository;
        this.memberRepository = memberRepository;
        this.libraryRepository = libraryRepository;
        this.reservationRepository = reservationRepository;
        this.fineRepository = fineRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.auditService = auditService;
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public LoanResponse issueBook(IssueBookRequest request, String idempotencyKey, String actor) {
        Instant now = clock.instant();

        // Check Idempotency Key if provided
        String requestHash = null;
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            requestHash = hashPayload(request);
            Optional<IdempotencyRecord> existingRecord = idempotencyRepository.findByIdempotencyKeyAndExpiresAtAfter(idempotencyKey, now);
            if (existingRecord.isPresent()) {
                IdempotencyRecord record = existingRecord.get();
                if (!record.getRequestHash().equals(requestHash)) {
                    throw new ConflictException(ErrorCode.IDEMPOTENCY_CONFLICT, "Idempotency key reused with different request payload");
                }
                try {
                    return objectMapper.readValue(record.getResponseBody(), LoanResponse.class);
                } catch (JsonProcessingException e) {
                    log.error("Failed to deserialize cached idempotency response: {}", e.getMessage());
                }
            }
        }

        // 1. Verify Member exists
        Member member = memberRepository.findById(request.memberId())
                .orElseThrow(() -> new ResourceNotFoundException("Member", request.memberId()));

        // 2. Verify Member is active and not expired
        if (!member.isBorrowingEligible()) {
            throw new BusinessException(ErrorCode.MEMBER_INACTIVE, "Member is inactive, suspended, or membership has expired");
        }

        // Fetch operational policy
        Library library = getLibraryPolicy();

        // 3. Verify borrowing limits
        long activeLoansCount = loanRepository.countByMemberIdAndStatus(member.getId(), LoanStatus.ACTIVE);
        if (activeLoansCount >= library.getMaxLoansPerMember()) {
            throw new BusinessException(ErrorCode.LOAN_LIMIT_EXCEEDED,
                    String.format("Member has reached maximum borrowing limit of %d books (current active loans: %d)",
                            library.getMaxLoansPerMember(), activeLoansCount));
        }

        // 4. Check outstanding fine restrictions
        BigDecimal outstandingFines = fineRepository.sumOutstandingFinesByMemberId(member.getId());
        if (outstandingFines.compareTo(library.getMaxFinePerLoan()) > 0) {
            throw new BusinessException(ErrorCode.FINE_OUTSTANDING_EXCEEDED,
                    String.format("Member has outstanding unpaid fines of %s exceeding permissible threshold", outstandingFines));
        }

        // 5 & 6. Identify physical copy with Pessimistic Write Lock
        BookCopy copy;
        if (request.bookCopyId() != null) {
            copy = bookCopyRepository.findByIdForUpdate(request.bookCopyId())
                    .orElseThrow(() -> new ResourceNotFoundException("BookCopy", request.bookCopyId()));

            // If copy is RESERVED, verify if reserved for this member
            if (copy.getStatus() == BookCopyStatus.RESERVED) {
                Optional<Reservation> reservationOpt = reservationRepository.findFirstByBookIdAndMemberIdAndStatusIn(
                        copy.getBook().getId(), member.getId(), List.of(ReservationStatus.READY_FOR_PICKUP));
                if (reservationOpt.isEmpty()) {
                    throw new ConflictException(ErrorCode.BOOK_NOT_AVAILABLE, "This copy is reserved for another member.");
                }
            } else if (copy.getStatus() != BookCopyStatus.AVAILABLE) {
                throw new ConflictException(ErrorCode.BOOK_NOT_AVAILABLE,
                        "Book copy is not available for issue. Current status: " + copy.getStatus());
            }
        } else if (request.barcode() != null && !request.barcode().isBlank()) {
            BookCopy found = bookCopyRepository.findByBarcode(request.barcode().trim())
                    .orElseThrow(() -> new ResourceNotFoundException("Book copy with barcode", request.barcode()));
            copy = bookCopyRepository.findByIdForUpdate(found.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("BookCopy", found.getId()));

            if (copy.getStatus() == BookCopyStatus.RESERVED) {
                Optional<Reservation> reservationOpt = reservationRepository.findFirstByBookIdAndMemberIdAndStatusIn(
                        copy.getBook().getId(), member.getId(), List.of(ReservationStatus.READY_FOR_PICKUP));
                if (reservationOpt.isEmpty()) {
                    throw new ConflictException(ErrorCode.BOOK_NOT_AVAILABLE, "This copy is reserved for another member.");
                }
            } else if (copy.getStatus() != BookCopyStatus.AVAILABLE) {
                throw new ConflictException(ErrorCode.BOOK_NOT_AVAILABLE,
                        "Book copy is not available for issue. Current status: " + copy.getStatus());
            }
        } else if (request.bookId() != null) {
            copy = bookCopyRepository.findFirstByBookIdAndStatusOrderByIdAsc(request.bookId(), BookCopyStatus.AVAILABLE)
                    .orElseThrow(() -> new ConflictException(ErrorCode.BOOK_NOT_AVAILABLE,
                            "No available copies found for book ID: " + request.bookId()));
        } else {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Either bookCopyId, barcode, or bookId must be provided");
        }

        // 7. Verify copy is not already on active loan
        if (loanRepository.existsByBookCopyIdAndStatus(copy.getId(), LoanStatus.ACTIVE)) {
            throw new ConflictException(ErrorCode.BOOK_NOT_AVAILABLE, "Physical copy already has an active loan");
        }

        // 8. Update physical copy status
        copy.setStatus(BookCopyStatus.ON_LOAN);
        copy.setUpdatedAt(now);
        bookCopyRepository.save(copy);

        // 9. Calculate due date using library policy snapshot
        Instant dueDate = now.plus(library.getLoanDurationDays(), ChronoUnit.DAYS);

        // 10. Create and persist loan
        Loan loan = new Loan(
                member,
                copy,
                now,
                dueDate,
                library.getLoanDurationDays(),
                library.getDailyFineRate()
        );
        Loan savedLoan = loanRepository.save(loan);

        // 11. If member had an active reservation for this book title, mark it fulfilled
        reservationRepository.findFirstByBookIdAndMemberIdAndStatusIn(
                copy.getBook().getId(),
                member.getId(),
                List.of(ReservationStatus.PENDING, ReservationStatus.READY_FOR_PICKUP)
        ).ifPresent(res -> {
            res.setStatus(ReservationStatus.FULFILLED);
            res.setUpdatedAt(now);
            reservationRepository.save(res);
        });

        // 12. Create audit event
        auditService.record(
                actor != null ? actor : "SYSTEM",
                "BOOK_ISSUED",
                "LOAN",
                savedLoan.getId().toString(),
                idempotencyKey,
                String.format("Issued copy barcode '%s' to member '%s' (Loan ID: %d)", copy.getBarcode(), member.getMembershipNumber(), savedLoan.getId())
        );

        LoanResponse response = LoanResponse.from(savedLoan);

        // Record Idempotency Cache
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            try {
                String responseBody = objectMapper.writeValueAsString(response);
                IdempotencyRecord record = new IdempotencyRecord(idempotencyKey, requestHash, 200, responseBody, now.plus(24, ChronoUnit.HOURS));
                idempotencyRepository.save(record);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize idempotency response: {}", e.getMessage());
            }
        }

        return response;
    }

    @Transactional
    public ReturnBookResponse returnBook(ReturnBookRequest request, String actor) {
        Instant now = clock.instant();

        // 1. Locate active loan
        Loan loan;
        if (request.loanId() != null) {
            loan = loanRepository.findById(request.loanId())
                    .orElseThrow(() -> new ResourceNotFoundException("Loan", request.loanId()));
        } else if (request.barcode() != null && !request.barcode().isBlank()) {
            loan = loanRepository.findByBookCopyBarcodeAndStatus(request.barcode(), LoanStatus.ACTIVE)
                    .orElseThrow(() -> new ResourceNotFoundException("Active loan for barcode", request.barcode()));
        } else {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Either loanId or barcode must be provided");
        }

        // 2. Verify loan is ACTIVE (prevent duplicate return)
        if (loan.getStatus() != LoanStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.LOAN_ALREADY_RETURNED, "This loan has already been closed and returned");
        }

        Library library = getLibraryPolicy();
        ZoneId zoneId = ZoneId.of(library.getTimezone());
        LocalDate dueLocalDate = loan.getDueDate().atZone(zoneId).toLocalDate();
        LocalDate returnLocalDate = now.atZone(zoneId).toLocalDate();

        // 3. Overdue and fine calculation
        long overdueDays = ChronoUnit.DAYS.between(dueLocalDate, returnLocalDate);
        boolean overdue = overdueDays > 0;
        BigDecimal fineAmount = BigDecimal.ZERO;
        Long fineId = null;

        if (overdue) {
            fineAmount = library.getDailyFineRate()
                    .multiply(BigDecimal.valueOf(overdueDays))
                    .min(library.getMaxFinePerLoan());

            if (fineAmount.compareTo(BigDecimal.ZERO) > 0) {
                Fine fine = new Fine(loan, loan.getMember(), fineAmount, returnLocalDate);
                Fine savedFine = fineRepository.save(fine);
                fineId = savedFine.getId();
            }
            loan.setStatus(LoanStatus.OVERDUE);
        }

        // 4. Close the loan
        loan.setReturnDate(now);
        loan.setStatus(LoanStatus.RETURNED);
        loan.setUpdatedAt(now);
        loanRepository.save(loan);

        // 5. Check pending reservations (FIFO Allocation)
        BookCopy copy = loan.getBookCopy();
        Optional<Reservation> nextReservation = reservationRepository.findFirstByBookIdAndStatusOrderByQueuePositionAsc(
                copy.getBook().getId(), ReservationStatus.PENDING);

        boolean allocatedToReservation = false;
        if (nextReservation.isPresent()) {
            Reservation reservation = nextReservation.get();
            reservation.setBookCopy(copy);
            reservation.setStatus(ReservationStatus.READY_FOR_PICKUP);
            reservation.setHoldExpiresAt(now.plus(library.getReservationHoldHours(), ChronoUnit.HOURS));
            reservation.setUpdatedAt(now);
            reservationRepository.save(reservation);

            copy.setStatus(BookCopyStatus.RESERVED);
            copy.setUpdatedAt(now);
            bookCopyRepository.save(copy);
            allocatedToReservation = true;

            log.info("Returned copy {} allocated to pending reservation #{} for member {}",
                    copy.getBarcode(), reservation.getId(), reservation.getMember().getId());
        } else {
            copy.setStatus(BookCopyStatus.AVAILABLE);
            copy.setUpdatedAt(now);
            bookCopyRepository.save(copy);
        }

        // 6. Record audit event
        auditService.record(
                actor != null ? actor : "SYSTEM",
                "BOOK_RETURNED",
                "LOAN",
                loan.getId().toString(),
                null,
                String.format("Returned copy barcode '%s'. Overdue: %b (Days: %d, Fine: %s)", copy.getBarcode(), overdue, Math.max(0, overdueDays), fineAmount)
        );

        return new ReturnBookResponse(
                LoanResponse.from(loan),
                overdue,
                Math.max(0, overdueDays),
                fineAmount,
                fineId,
                allocatedToReservation
        );
    }

    @Transactional(readOnly = true)
    public LoanResponse getLoanById(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", loanId));
        return LoanResponse.from(loan);
    }

    @Transactional(readOnly = true)
    public Page<LoanResponse> getLoans(Long memberId, LoanStatus status, Pageable pageable) {
        Page<Loan> page;
        if (memberId != null && status != null) {
            page = loanRepository.findByMemberIdAndStatus(memberId, status, pageable);
        } else if (memberId != null) {
            page = loanRepository.findByMemberId(memberId, pageable);
        } else if (status != null) {
            page = loanRepository.findByStatus(status, pageable);
        } else {
            page = loanRepository.findAll(pageable);
        }
        return page.map(LoanResponse::from);
    }

    private Library getLibraryPolicy() {
        return libraryRepository.findAll().stream().findFirst()
                .orElseGet(() -> {
                    Library fallback = new Library();
                    fallback.setLoanDurationDays(14);
                    fallback.setMaxLoansPerMember(5);
                    fallback.setDailyFineRate(new BigDecimal("5.00"));
                    fallback.setMaxFinePerLoan(new BigDecimal("500.00"));
                    fallback.setReservationHoldHours(48);
                    fallback.setTimezone("UTC");
                    return fallback;
                });
    }

    private String hashPayload(Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            return String.valueOf(payload.hashCode());
        }
    }
}
