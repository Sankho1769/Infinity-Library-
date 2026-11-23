# Catalog Feature Module (`com.librarymanagement.catalog`)

## 1. Feature Purpose & Responsibilities
The **Catalog Feature Module** manages the central bibliographic holdings and the serialized physical copy inventory of the library. It enforces the fundamental architectural principle separating abstract bibliographic titles (`Book`) from barcode-tagged physical items (`BookCopy`).

## 2. Key Components & Important Files
- `domain/Book.java`: JPA entity capturing title, ISBN, author, publisher, publication year, genre, language, and description.
- `domain/BookCopy.java`: JPA entity tracking individual physical copies with a unique accession barcode (`CPY-<bookId>-<UUID>`) and status lifecycle.
- `domain/BookCopyStatus.java`: Enumeration of valid physical copy states (`AVAILABLE`, `ON_LOAN`, `RESERVED`, `LOST`, `UNDER_MAINTENANCE`, `WITHDRAWN`).
- `repository/BookRepository.java`: Spring Data JPA interface with custom search and duplicate ISBN checks.
- `repository/BookCopyRepository.java`: Includes pessimistic row locking query (`findByBarcodeForUpdate`) and live count queries.
- `service/BookService.java`: Transactional business logic handling title registration and batch physical copy creation.
- `web/BookController.java`: Versioned REST API endpoints (`/api/v1/books/**`).

## 3. Business Rules & Data Flow
1. **Dynamic Inventory Calculation:** Persisted manual counter columns are prohibited. Availability counts (`totalCopies`, `availableCopies`, `issuedCopies`) are derived dynamically via indexed SQL aggregates.
2. **ISBN Uniqueness:** ISBN-10 or ISBN-13 uniqueness is enforced at the database schema level.
3. **Batch Accession:** Registering a book automatically instantiates $N$ physical `BookCopy` records assigned to the default library facility.

## 4. Testing & Verification
- Unit Tests: `com.librarymanagement.unit.DomainModelTest`
- Service Tests: `com.librarymanagement.service.BookServiceTest`
- Verified: Duplicate ISBN throws `ConflictException` (409), copy count aggregation is 100% accurate.
