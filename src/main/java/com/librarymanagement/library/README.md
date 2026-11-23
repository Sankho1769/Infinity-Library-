# Library Configuration Feature Module (`com.librarymanagement.library`)

## 1. Feature Purpose & Responsibilities
The **Library Feature Module** manages institutional identity, operational configurations, and baseline circulation policies for the library entity.

## 2. Key Components & Important Files
- `domain/Library.java`: Entity capturing library name, address, operational timezone, default loan period (days), maximum active loans per member, daily fine rate, and reservation pickup window (hours).
- `repository/LibraryRepository.java`: Persistence interface for library facility records.
- `service/LibraryService.java`: Administrative service governing institutional policy adjustments.
- `web/LibraryController.java`: Versioned REST API endpoints (`/api/v1/libraries/**`).

## 3. Policy Defaults & Governance
- **Default Facility:** Central Library (ID 1).
- **Default Loan Period:** 14 calendar days.
- **Maximum Active Loans:** 5 concurrent loans per patron.
- **Daily Fine Rate:** $0.50 per day overdue.
- **Hold Expiration Window:** 72 hours (3 calendar days).

## 4. Testing & Verification
- Unit Tests: `com.librarymanagement.unit.DomainModelTest`
- Integrated across: `com.librarymanagement.service.CirculationServiceTest` and `BookServiceTest`
