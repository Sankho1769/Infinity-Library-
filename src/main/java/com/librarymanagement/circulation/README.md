# Circulation Feature Module (`com.librarymanagement.circulation`)

## 1. Feature Purpose & Responsibilities
The **Circulation Feature Module** orchestrates borrowing and return transactions for physical book copies. It provides mathematically proven concurrency safety against double-checkout race conditions using explicit pessimistic database row locking.

## 2. Key Components & Important Files
- `domain/Loan.java`: Transactional loan contract capturing `member_id`, `book_copy_id`, `issueDate`, `dueDate`, `returnDate`, status (`ACTIVE`, `RETURNED`, `OVERDUE`), and historical policy snapshots (`loanPeriodDaysApplied`, `dailyFineRateApplied`).
- `domain/LoanStatus.java`: Enumeration of loan lifecycle states (`ACTIVE`, `RETURNED`, `OVERDUE`).
- `repository/LoanRepository.java`: Custom JPA queries including active loans by member and overdue loan identification.
- `service/CirculationService.java`: Transactional coordinator enforcing pessimistic locking, policy snapshotting, return processing, and FIFO reservation cascading.
- `web/LoanController.java`: Versioned REST API endpoints (`/api/v1/loans/**`).

## 3. Concurrency Control & Invariants
1. **Pessimistic Row Lock (`SELECT ... FOR UPDATE`):** When issuing a book, `CirculationService` locks the physical `BookCopy` row before validating availability and member quota. Concurrent attempts to issue the same copy fail gracefully with HTTP 409 Conflict.
2. **Partial Unique Index:** Protected in production by PostgreSQL partial index `uq_active_loan_per_copy ON loans(book_copy_id) WHERE status = 'ACTIVE'`.
3. **Automated FIFO Return Cascading:** Returning an item automatically allocates the physical copy to the earliest pending reservation in the hold queue with a 72-hour pickup expiration window.
4. **Idempotency Protection:** Circulation mutation endpoints accept client-generated `Idempotency-Key` headers preventing duplicate transactions on network retry.

## 4. Testing & Verification
- Concurrency Tests: `com.librarymanagement.integration.ConcurrencyIssuingIntegrationTest` (10 concurrent threads issuing 1 copy $\rightarrow$ 1 success, 9 409 Conflicts).
- Integration Tests: `com.librarymanagement.integration.CirculationIntegrationTest`
- Service Tests: `com.librarymanagement.service.CirculationServiceTest`
