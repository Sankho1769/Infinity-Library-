package com.librarymanagement.reservations.repository;

import com.librarymanagement.reservations.domain.Reservation;
import com.librarymanagement.reservations.domain.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByBookIdAndStatusOrderByQueuePositionAsc(Long bookId, ReservationStatus status);

    Optional<Reservation> findFirstByBookIdAndStatusOrderByQueuePositionAsc(Long bookId, ReservationStatus status);

    Optional<Reservation> findFirstByBookIdAndMemberIdAndStatusIn(Long bookId, Long memberId, Collection<ReservationStatus> statuses);

    boolean existsByMemberIdAndBookIdAndStatusIn(Long memberId, Long bookId, Collection<ReservationStatus> statuses);

    List<Reservation> findByMemberId(Long memberId);

    Page<Reservation> findByMemberId(Long memberId, Pageable pageable);

    Page<Reservation> findByStatus(ReservationStatus status, Pageable pageable);

    long countByMemberIdAndStatus(Long memberId, ReservationStatus status);

    long countByStatus(ReservationStatus status);

    @Query("SELECT COALESCE(MAX(r.queuePosition), 0) FROM Reservation r WHERE r.book.id = :bookId AND r.status IN ('PENDING', 'READY_FOR_PICKUP')")
    int findMaxQueuePositionByBookId(@Param("bookId") Long bookId);

    @Query("SELECT r FROM Reservation r WHERE r.status = 'READY_FOR_PICKUP' AND r.holdExpiresAt < :now")
    List<Reservation> findExpiredHolds(@Param("now") Instant now);
}
