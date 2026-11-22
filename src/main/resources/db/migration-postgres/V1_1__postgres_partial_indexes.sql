-- ============================================================================
-- PostgreSQL-Specific Data Integrity Constraints & Partial Unique Indexes
-- Schema Version: 1.1 (Flyway V1_1)
-- ============================================================================

-- Ensure at database level that no physical copy can have more than one ACTIVE loan concurrently
CREATE UNIQUE INDEX IF NOT EXISTS uq_active_loan_per_copy ON loans(book_copy_id) WHERE status = 'ACTIVE';

-- Prevent duplicate active reservations by the same member for the same book title
CREATE UNIQUE INDEX IF NOT EXISTS uq_active_reservation_per_member_book ON reservations(member_id, book_id) WHERE status IN ('PENDING', 'READY_FOR_PICKUP');
