package com.librarymanagement.fines.service;

import com.librarymanagement.common.audit.AuditService;
import com.librarymanagement.common.exception.*;
import com.librarymanagement.common.security.UserPrincipal;
import com.librarymanagement.fines.domain.Fine;
import com.librarymanagement.fines.domain.FineStatus;
import com.librarymanagement.fines.domain.FineTransaction;
import com.librarymanagement.fines.domain.FineTransactionType;
import com.librarymanagement.fines.dto.FineDTOs.*;
import com.librarymanagement.fines.repository.FineRepository;
import com.librarymanagement.fines.repository.FineTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;

@Service
public class FineService {
    private static final Logger log = LoggerFactory.getLogger(FineService.class);

    private final FineRepository fineRepository;
    private final FineTransactionRepository fineTransactionRepository;
    private final AuditService auditService;
    private final Clock clock;

    public FineService(
            FineRepository fineRepository,
            FineTransactionRepository fineTransactionRepository,
            AuditService auditService,
            Clock clock
    ) {
        this.fineRepository = fineRepository;
        this.fineTransactionRepository = fineTransactionRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public FineResponse payFine(Long fineId, PayFineRequest request, String actor) {
        Instant now = clock.instant();

        Fine fine = fineRepository.findById(fineId)
                .orElseThrow(() -> new ResourceNotFoundException("Fine", fineId));

        if (fine.getStatus() == FineStatus.PAID || fine.getStatus() == FineStatus.WAIVED) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION, "Fine is already settled (Status: " + fine.getStatus() + ")");
        }

        BigDecimal outstanding = fine.getAmountOutstanding();
        if (request.amount().compareTo(outstanding) > 0) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION,
                    String.format("Payment amount %s exceeds outstanding fine amount of %s", request.amount(), outstanding));
        }

        fine.setAmountPaid(fine.getAmountPaid().add(request.amount()));
        fine.setUpdatedAt(now);

        if (fine.getAmountOutstanding().compareTo(BigDecimal.ZERO) == 0) {
            fine.setStatus(FineStatus.PAID);
        } else {
            fine.setStatus(FineStatus.PARTIALLY_PAID);
        }

        FineTransaction tx = new FineTransaction(fine, FineTransactionType.PAYMENT, request.amount(), actor, "Administrative fine payment");
        fine.getTransactions().add(tx);
        fineTransactionRepository.save(tx);
        Fine savedFine = fineRepository.save(fine);

        auditService.record(
                actor != null ? actor : "STAFF",
                "FINE_PAYMENT",
                "FINE",
                fine.getId().toString(),
                null,
                String.format("Recorded fine payment of %s for fine ID %d (Remaining: %s)", request.amount(), fine.getId(), fine.getAmountOutstanding())
        );

        return FineResponse.from(savedFine);
    }

    @Transactional
    public FineResponse waiveFine(Long fineId, WaiveFineRequest request, String actor) {
        Instant now = clock.instant();

        Fine fine = fineRepository.findById(fineId)
                .orElseThrow(() -> new ResourceNotFoundException("Fine", fineId));

        if (fine.getStatus() == FineStatus.PAID || fine.getStatus() == FineStatus.WAIVED) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION, "Fine is already settled (Status: " + fine.getStatus() + ")");
        }

        BigDecimal outstanding = fine.getAmountOutstanding();
        if (request.amount().compareTo(outstanding) > 0) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION,
                    String.format("Waiver amount %s exceeds outstanding fine balance of %s", request.amount(), outstanding));
        }

        fine.setAmountWaived(fine.getAmountWaived().add(request.amount()));
        fine.setUpdatedAt(now);

        if (fine.getAmountOutstanding().compareTo(BigDecimal.ZERO) == 0) {
            fine.setStatus(FineStatus.WAIVED);
        } else {
            fine.setStatus(FineStatus.PARTIALLY_PAID);
        }

        FineTransaction tx = new FineTransaction(fine, FineTransactionType.WAIVER, request.amount(), actor, request.reason());
        fine.getTransactions().add(tx);
        fineTransactionRepository.save(tx);
        Fine savedFine = fineRepository.save(fine);

        auditService.record(
                actor != null ? actor : "STAFF",
                "FINE_WAIVED",
                "FINE",
                fine.getId().toString(),
                null,
                String.format("Authorized waiver of %s for fine ID %d. Reason: '%s'", request.amount(), fine.getId(), request.reason())
        );

        return FineResponse.from(savedFine);
    }

    @Transactional(readOnly = true)
    public FineResponse getFineById(Long fineId, UserPrincipal principal) {
        Fine fine = fineRepository.findById(fineId)
                .orElseThrow(() -> new ResourceNotFoundException("Fine", fineId));

        if (principal != null && "MEMBER".equalsIgnoreCase(principal.getRole())) {
            if (!fine.getMember().getId().equals(principal.getMemberId())) {
                throw new ForbiddenException(ErrorCode.ACCESS_DENIED, "Access denied: cannot view fine details of another member");
            }
        }

        return FineResponse.from(fine);
    }

    @Transactional(readOnly = true)
    public Page<FineResponse> getFines(Long memberId, FineStatus status, Pageable pageable) {
        Page<Fine> page;
        if (memberId != null) {
            page = fineRepository.findByMemberId(memberId, pageable);
        } else if (status != null) {
            page = fineRepository.findByStatus(status, pageable);
        } else {
            page = fineRepository.findAll(pageable);
        }
        return page.map(FineResponse::from);
    }
}
