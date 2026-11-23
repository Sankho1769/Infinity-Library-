# Reports Feature Module (`com.librarymanagement.reports`)

## 1. Feature Purpose & Responsibilities
The **Reports Feature Module** computes live executive analytics and operational intelligence. It guarantees that inventory, circulation, and financial metrics are calculated directly from active database records rather than relying on stale counter columns.

## 2. Key Components & Important Files
- `dto/ReportDTOs.java`: Strongly-typed Java records (`InventoryReportResponse`, `CirculationReportResponse`, `FinancialReportResponse`).
- `service/ReportService.java`: Read-only aggregate service executing real-time SQL count and sum queries across copy, loan, and fine repositories.
- `web/ReportController.java`: Versioned REST API endpoints (`/api/v1/reports/**`) restricted to `LIBRARIAN` and `ADMIN` roles.

## 3. Real-Time Derived Analytics
- **Inventory Intelligence:** Total bibliographic titles, total physical copies, copies available on shelves, copies currently on loan, and copies reserved.
- **Circulation Metrics:** Active loans, loans currently overdue, historical returned count, and active borrowing patrons.
- **Financial Balances:** Total penalties accrued, total collected payments, total authorized waivers, and outstanding receivables.

## 4. Testing & Verification
- Service Tests: `com.librarymanagement.service.ReportServiceTest`
