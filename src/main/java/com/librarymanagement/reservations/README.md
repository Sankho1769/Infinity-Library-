# Reservations Feature Module (`com.librarymanagement.reservations`)

## 1. Feature Purpose & Responsibilities
The **Reservations Feature Module** manages patron title hold requests when all physical copies of a title are checked out or unavailable. It guarantees fair, queue-ordered allocation of newly returned books using a First-In-First-Out (FIFO) algorithm.

## 2. Key Components & Important Files
- `domain/Reservation.java`: Entity capturing `member_id`, `book_id`, `allocatedCopy`, `queuePosition`, `reservationDate`, `holdExpirationDate`, and `status`.
- `domain/ReservationStatus.java`: Lifecycle states (`PENDING`, `READY_FOR_PICKUP`, `FULFILLED`, `CANCELLED`, `EXPIRED`).
- `repository/ReservationRepository.java`: Queries for active member holds, FIFO queue ordering, and expired pickup holds.
- `service/ReservationService.java`: Business service handling hold placement, queue rebalancing on cancellation, and scheduled expiration cleanup.
- `web/ReservationController.java`: Versioned REST API endpoints (`/api/v1/reservations/**`).

## 3. Business Rules
1. **Hold Eligibility:** A member may only place a reservation if all physical copies of the book are currently checked out (`ON_LOAN`) or reserved.
2. **Duplicate Prevention:** A patron cannot hold multiple active reservations for the same book title (enforced at both application and database engine levels via partial unique index).
3. **72-Hour Pickup Window:** When a copy becomes available, the hold status transitions to `READY_FOR_PICKUP` with a 72-hour expiration deadline. Unclaimed copies automatically cascade to the next queued patron.

## 4. Testing & Verification
- Service Tests: `com.librarymanagement.service.ReservationServiceTest`
- Unit Tests: `com.librarymanagement.unit.DomainModelTest`
