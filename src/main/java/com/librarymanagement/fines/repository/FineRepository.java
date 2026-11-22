package com.librarymanagement.fines.repository;

import com.librarymanagement.fines.domain.Fine;
import com.librarymanagement.fines.domain.FineStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface FineRepository extends JpaRepository<Fine, Long> {
    Optional<Fine> findByLoanId(Long loanId);

    List<Fine> findByMemberId(Long memberId);

    Page<Fine> findByMemberId(Long memberId, Pageable pageable);

    Page<Fine> findByStatus(FineStatus status, Pageable pageable);

    long countByMemberIdAndStatus(Long memberId, FineStatus status);

    long countByStatus(FineStatus status);

    @Query("SELECT COALESCE(SUM(f.fineAmount - f.amountPaid - f.amountWaived), 0.00) FROM Fine f WHERE f.member.id = :memberId AND f.status IN ('UNPAID', 'PARTIALLY_PAID')")
    BigDecimal sumOutstandingFinesByMemberId(@Param("memberId") Long memberId);

    @Query("SELECT COALESCE(SUM(f.fineAmount - f.amountPaid - f.amountWaived), 0.00) FROM Fine f WHERE f.status IN ('UNPAID', 'PARTIALLY_PAID')")
    BigDecimal sumTotalOutstandingFines();

    @Query("SELECT COALESCE(SUM(f.fineAmount), 0.00) FROM Fine f")
    BigDecimal sumTotalAccruedFines();

    @Query("SELECT COALESCE(SUM(f.amountPaid), 0.00) FROM Fine f")
    BigDecimal sumTotalCollectedFines();

    @Query("SELECT COALESCE(SUM(f.amountWaived), 0.00) FROM Fine f")
    BigDecimal sumTotalWaivedFines();

    @Query("SELECT COUNT(f) FROM Fine f WHERE f.status IN ('UNPAID', 'PARTIALLY_PAID')")
    long countUnpaidFines();
}
