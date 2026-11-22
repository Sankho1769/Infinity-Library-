package com.librarymanagement.catalog.repository;

import com.librarymanagement.catalog.domain.BookCopy;
import com.librarymanagement.catalog.domain.BookCopyStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookCopyRepository extends JpaRepository<BookCopy, Long> {
    Optional<BookCopy> findByBarcode(String barcode);
    boolean existsByBarcode(String barcode);
    List<BookCopy> findByBookId(Long bookId);
    long countByBookIdAndStatus(Long bookId, BookCopyStatus status);
    long countByBookId(Long bookId);
    long countByStatus(BookCopyStatus status);

    // CRITICAL CONCURRENCY CONTROL:
    // Pessimistic Write Lock on physical book copy to avoid race conditions during concurrent issuance
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT bc FROM BookCopy bc WHERE bc.id = :id")
    Optional<BookCopy> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<BookCopy> findFirstByBookIdAndStatusOrderByIdAsc(Long bookId, BookCopyStatus status);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE BookCopy bc SET bc.status = :newStatus, bc.updatedAt = :now WHERE bc.id = :id AND bc.status = :expectedStatus")
    int updateStatusIfExpected(
            @Param("id") Long id,
            @Param("expectedStatus") BookCopyStatus expectedStatus,
            @Param("newStatus") BookCopyStatus newStatus,
            @Param("now") java.time.Instant now
    );
}
