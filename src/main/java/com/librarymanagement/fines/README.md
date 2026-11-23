# Fines Feature Module (`com.librarymanagement.fines`)

## 1. Feature Purpose & Responsibilities
The **Fines Feature Module** provides deterministic financial accounting for overdue library loans. It implements immutable audit tracking for all penalty settlements, partial payments, and authorized staff fee waivers.

## 2. Key Components & Important Files
- `domain/Fine.java`: Aggregate fine record tracking `member_id`, `loan_id`, `amount`, `paidAmount`, and status (`UNPAID`, `PAID`, `PARTIALLY_PAID`, `WAIVED`).
- `domain/FineTransaction.java`: Immutable ledger record capturing `fine_id`, `transactionAmount`, `transactionType` (`PAYMENT`, `WAIVER`), `processedBy`, `transactionTime`, and `justificationReason`.
- `repository/FineRepository.java` & `FineTransactionRepository.java`: Persistence interfaces for fines and immutable transaction records.
- `service/FineService.java`: Business logic governing overdue accrual, partial payments, and mandatory waiver justification.
- `web/FineController.java`: Versioned REST API endpoints (`/api/v1/fines/**`).

## 3. Financial Invariants
1. **Deterministic Calculation:** Overdue fines are calculated deterministically:
   $$\text{Fine} = \max(0, \text{Overdue Days}) \times \text{Daily Fine Rate}$$
   Negative fine amounts are mathematically impossible and rejected.
2. **Mandatory Waiver Justification:** Fee waivers require a non-blank justification reason recorded in the audit trail.
3. **Immutable Ledger:** Once written, `fine_transactions` records can never be updated or deleted.

## 4. Testing & Verification
- Unit Tests: `com.librarymanagement.unit.FineCalculationTest`
- Service Tests: `com.librarymanagement.service.FineServiceTest`
