package com.librarymanagement.circulation.repository;

import com.librarymanagement.circulation.domain.Loan;
import com.librarymanagement.circulation.domain.LoanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    Optional<Loan> findByBookCopyIdAndStatus(Long bookCopyId, LoanStatus status);

    @Query("SELECT l FROM Loan l WHERE l.bookCopy.barcode = :barcode AND l.status = :status")
    Optional<Loan> findByBookCopyBarcodeAndStatus(@Param("barcode") String barcode, @Param("status") LoanStatus status);

    List<Loan> findByMemberId(Long memberId);

    List<Loan> findByMemberIdAndStatus(Long memberId, LoanStatus status);

    Page<Loan> findByMemberId(Long memberId, Pageable pageable);

    Page<Loan> findByMemberIdAndStatus(Long memberId, LoanStatus status, Pageable pageable);

    Page<Loan> findByStatus(LoanStatus status, Pageable pageable);

    long countByMemberIdAndStatus(Long memberId, LoanStatus status);

    boolean existsByBookCopyIdAndStatus(Long bookCopyId, LoanStatus status);

    long countByStatus(LoanStatus status);

    @Query("SELECT COUNT(DISTINCT l.member.id) FROM Loan l WHERE l.status = :status")
    long countDistinctBorrowingMembers(@Param("status") LoanStatus status);

    @Query("SELECT COUNT(l) FROM Loan l WHERE l.status = 'ACTIVE' AND l.dueDate < :now")
    long countOverdueLoans(@Param("now") Instant now);

    @Query("SELECT l FROM Loan l WHERE l.status = 'ACTIVE' AND l.dueDate < :now")
    List<Loan> findOverdueLoans(@Param("now") Instant now);

    @Query("SELECT l FROM Loan l WHERE l.status = 'ACTIVE' AND l.dueDate < :now")
    Page<Loan> findOverdueLoans(@Param("now") Instant now, Pageable pageable);
}
