# Infinity Library Management System (ILMS)
### Enterprise Modular Monolith | Java 25 LTS & Spring Boot 4.2.x | 3-Part Persona Architecture

![Build Status](https://img.shields.io/badge/build-passing-brightgreen?style=for-the-badge&logo=github-actions)
![Java](https://img.shields.io/badge/Java-25%20LTS-0B3048?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.2.0-2A5344?style=for-the-badge&logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-19%20%2F%2018-0B3048?style=for-the-badge&logo=postgresql&logoColor=white)
![Security](https://img.shields.io/badge/Security-Argon2id%20%2B%20JWT-B9F12F?style=for-the-badge&logo=springsecurity&logoColor=black)
![Architecture](https://img.shields.io/badge/Architecture-Modular%20Monolith-2A5344?style=for-the-badge)
![Tests](https://img.shields.io/badge/Tests-47%20Passed%20(100%25)-brightgreen?style=for-the-badge)
![UI / UX](https://img.shields.io/badge/UX-3--Part%20Persona%20Suite-0B3048?style=for-the-badge)

---

## 🎨 Design Identity & Palette
The system strictly adheres to an executive design palette across documentation, API consoles, and its responsive web interfaces:
- **Primary Teal (`#2A5344`):** Core domain logic, primary actions, and patron branding.
- **Deep Navy (`#0B3048`):** Security filters, database transactions, and structural framing.
- **Neon Lime (`#B9F12F`):** Badges, active states, key metric highlights, and particle accents.
- **Warm Off-White (`#F4F2EC`):** Backgrounds, clean reading surfaces, and cards.

---

## 📸 System Showcase & Visual Tour

### 1. Unified Authentication Portal & 1-Click Demo Access
*A glassmorphic authentication gateway providing credential sign-in, show/hide password toggle, and instant 1-click preset logins for evaluators.*

![Authentication Portal](docs/screenshots/01-auth-portal-login.png)

---

### 2. Part 1: Normal User (Patron Experience)
*Featuring a **Holographic Digital Library Pass** with dynamic CSS shimmer gradient (`@keyframes holoShimmer`), animated avatar monogram, real-time borrowing quota meter (`0 / 5 Books Borrowed`), active loans tracking, and personal hold status.*

![Patron Digital Pass & Dashboard](docs/screenshots/02-patron-digital-pass.png)

---

### 3. Real-Time Catalog Reservation with Confetti FX
*Patrons can browse the catalog and place title reservations when copies are checked out. Real-time hold placements trigger a 2D physics Canvas particle confetti burst and update waitlist priority instantly.*

![Catalog Reservation & Confetti Burst](docs/screenshots/03-catalog-reserve-confetti.png)

---

### 4. Role-Based Access Control & Security Barrier
*Patrons attempting to navigate to staff circulation or executive governance tools are intercepted with a security barrier dialog and guided to sign in with authorized credentials.*

![Role Access Barrier](docs/screenshots/04-role-access-barrier.png)

---

### 5. Part 2: Librarian (Circulation & Staff Desk)
*Circulation desk with rapid 1-click checkout presets, live copy & patron dropdowns, and an active loans table with 1-click returns and automated fine assessments.*

![Librarian Circulation Desk](docs/screenshots/05-librarian-circulation-desk.png)

---

### 6. Part 3: Administrator (Executive Leadership Suite)
*Executive dashboard aggregating real-time institutional KPIs: physical inventory counts, active circulation, overdue alerts, and financial fine balances.*

![Admin Executive Dashboard](docs/screenshots/06-admin-executive-dashboard.png)

---

### 7. Dynamic Institutional Policy Configuration
*Live policy management interacting directly with `PUT /api/v1/libraries/1`. Administrators can configure standard loan periods, maximum borrower quotas, daily overdue penalty rates, and hold windows with immediate system-wide enforcement.*

![Admin Policy Controls](docs/screenshots/07-admin-policy-controls.png)

---

### 8. Member Directory & Account Governance
*Complete member roster with instant account governance controls (`POST /api/v1/members/{id}/suspend` and `reactivate`) to manage borrowing privileges in real time.*

![Member Governance](docs/screenshots/08-admin-member-governance.png)

---

### 9. Self-Service Patron Onboarding & Pass Issuance
*Prospective patrons can register online, automatically minting an institutional membership pass (e.g. `MEM-2026-DB92B5`) with immediate digital borrowing rights.*

![Patron Self-Registration](docs/screenshots/09-patron-self-registration.png)

---

### 10. Responsive Mobile Viewports (320px – 768px)
*Optimized for all screen sizes with an off-canvas drawer navigation menu and a thumb-accessible sticky bottom navigation bar.*

| Off-Canvas Drawer Navigation | Clean Mobile Dashboard with Bottom Nav |
| :---: | :---: |
| ![Mobile Drawer](docs/screenshots/10-mobile-responsive-drawer.png) | ![Mobile Bottom Nav](docs/screenshots/11-mobile-dashboard-bottom-nav.png) |

---

## 📖 Executive Summary
The **Infinity Library Management System (ILMS)** is a production-grade, enterprise backend system engineered with an emphasis on **transactional integrity, concurrency control, application security, and domain maintainability**.

Designed to replace legacy library management architectures prone to race conditions and inventory drift, ILMS implements:
- **Physical Copy Inventory Isolation:** Clean distinction between abstract bibliographic records (`Book`) and barcoded physical inventory copies (`BookCopy`).
- **High-Concurrency Pessimistic Row Locking:** `SELECT ... FOR UPDATE` row locks eliminate double-issue race conditions under heavy concurrent borrowing.
- **FIFO Reservation Cascading:** Automated allocation of returned copies to the next waiting member in line with hold expiration time-boxing.
- **Audited Financial Accounting:** Deterministic fine accrual with immutable transaction logs for payments and administrative waivers.
- **Modern Security Architecture:** Argon2id password hashing, short-lived JWT access tokens, and refresh token rotation with automated token family reuse revocation.
- **RFC 9457 Problem Details:** Unified, standardized error representations across all endpoints.
- **Three Persona Architecture:** Tailored interfaces for Normal Users (Patrons), Circulation Librarians, and Executive Administrators with zero framework jargon in the UI.

---

## 📚 Official Documentation Suite
In accordance with project governance, the root directory hosts comprehensive engineering documents:
1. [**SRS.md**](file:///c:/Users/hp/Desktop/Infinity-Library-/SRS.md) — IEEE 830 / ISO/IEC/IEEE 29148 Software Requirements Specification.
2. [**PRD.md**](file:///c:/Users/hp/Desktop/Infinity-Library-/PRD.md) — Product Requirements Document (Personas, user journeys, KPIs).
3. [**PROJECT_REPORT.md**](file:///c:/Users/hp/Desktop/Infinity-Library-/PROJECT_REPORT.md) — Engineering Technical Report (Concurrency proofs, database normalization, threat model, test suite breakdown).
4. [**SECURITY_REVIEW.md**](file:///c:/Users/hp/Desktop/Infinity-Library-/SECURITY_REVIEW.md) — Application Security & Threat Audit Report.
5. [**QA_REPORT.md**](file:///c:/Users/hp/Desktop/Infinity-Library-/QA_REPORT.md) — Quality Assurance & Test Verification Report.

---

## 🏛 System Architecture

ILMS is organized as a **Modular Monolith** using **package-by-feature** encapsulation:

```mermaid
flowchart TD
    subgraph Clients["Clients & Gateways (#F4F2EC Warm Off-White)"]
        UI["Unified Web Application (Patron / Librarian / Admin)"]
        Swagger["OpenAPI 3.1 Swagger UI Console"]
    end

    subgraph Security["Spring Security & Auth (#0B3048 Deep Navy)"]
        JWT["JwtAuthenticationFilter (Bearer Token Validator)"]
        Argon["Argon2id Password Encoder (m=65536, t=3, p=1)"]
        RBAC["Method Security (@PreAuthorize / HasRole)"]
    end

    subgraph CoreModules["Domain Feature Modules (#2A5344 Teal)"]
        CAT["Catalog Module (Book, BookCopy)"]
        CIR["Circulation Module (Loan, Pessimistic Lock)"]
        RES["Reservation Module (FIFO Queue, Hold Expiration)"]
        FIN["Fines Module (Deterministic Accrual, Transactions)"]
        MEM["Members Module (Governance, Quotas, Passes)"]
        REP["Reports Module (Dynamic Aggregates)"]
    end

    subgraph Storage["PostgreSQL 19/18 (#0B3048 Deep Navy)"]
        PG1[("Physical Copies & Books")]
        PG2[("Active Loans (Partial Unique Indexes)")]
        PG3[("FIFO Reservations")]
        PG4[("Audit Ledger & Idempotency Keys")]
    end

    Clients --> Security
    Security --> CoreModules
    CoreModules --> Storage
```

---

## 🚀 Quickstart Guide

### 1. Prerequisites
- **Java 25 LTS** (or compatible JDK 25/26 with `--release 25`)
- **Maven 3.9+** (or use the included `./mvnw`)
- **Docker & Docker Compose** (for containerized deployment)

### 2. Running with Docker Compose (Recommended)
Launch PostgreSQL and the containerized Spring Boot backend in one command:
```bash
docker-compose up --build
```
Once initialized:
- **Unified Web Application (All 3 Personas):** `http://localhost:8080/`
- **Branded Swagger UI API Console:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI 3.1 Spec (JSON):** `http://localhost:8080/api/docs`
- **Actuator Health:** `http://localhost:8080/actuator/health`

### 3. Running Locally with Maven
To run using an embedded H2 database (in PostgreSQL compatibility mode):
```bash
# Unix / macOS
./mvnw spring-boot:run

# Windows PowerShell
.\mvnw.cmd spring-boot:run
```

To run against a local PostgreSQL instance:
```bash
export SPRING_PROFILES_ACTIVE=prod
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=infinity_library
export DB_USERNAME=postgres
export DB_PASSWORD=your_password
.\mvnw.cmd spring-boot:run
```

---

## 🔐 Pre-Seeded Default Accounts

The database migration (`V2__seed_initial_data.sql`) seeds default users with verified Argon2id-hashed credentials:

| Username | Password | Role | Persona & Capabilities |
| :--- | :--- | :--- | :--- |
| `member_elena` | `Password@123` | `MEMBER` | **Dr. Elena Rostova** — Digital pass, active loans, catalog holds |
| `librarian` | `Password@123` | `LIBRARIAN` | **Librarian Jane** — Circulation desk, checkouts, returns, catalog registration |
| `admin` | `Password@123` | `ADMIN` | **Director Sarah Sterling** — Institutional policies, member governance, audits |

*All three accounts can be signed into manually or tested instantly via 1-click demo buttons on the login portal.*

---

## 🧪 Automated Testing & Verification

The project includes 47 tests across Unit, Service, Concurrency, Security, and Architecture layers:

```bash
# Execute full test suite with JaCoCo coverage report
.\mvnw.cmd clean test

# Execute package verification including ArchUnit and packaging
.\mvnw.cmd clean verify
```

### Test Suite Structure
| Test Suite | File | Focus | Tests |
| :--- | :--- | :--- | :--- |
| **Concurrency Integration** | `ConcurrencyIssuingIntegrationTest.java` | 10 simultaneous threads checking out 1 copy | 2 |
| **Security & BOLA** | `SecurityAuthorizationIntegrationTest.java` | Role-Based Access & IDOR protection | 3 |
| **Circulation Lifecycle** | `CirculationIntegrationTest.java` | Full checkout, return, duplicate return rejection | 3 |
| **Architecture Integrity**| `ArchitectureRulesTest.java` | ArchUnit package boundary rules | 3 |
| **Service Layer Tests** | `*ServiceTest.java` | Circulation, Auth, Reservations, Fines, Catalog, Members, Reports | 24 |
| **Domain & Crypto Units** | `*Test.java` | Argon2id verification, JWT rotation, Fine calculations | 11 |
| **Context Bootstrap** | `LibraryManagementApplicationTests.java` | Full Spring application context verification | 1 |
| **Total** | | **All 47 Tests Pass Green** | **47** |

---

## 📡 REST API Reference

All endpoints are versioned under `/api/v1` and produce/consume `application/json`.

### 1. Authentication (`/api/v1/auth`)
| Method | Endpoint | Role | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/login` | Public | Authenticate with username/password; returns JWT + Refresh Token |
| `POST` | `/api/v1/auth/refresh` | Public | Rotate refresh token and obtain new short-lived access token |
| `POST` | `/api/v1/auth/logout` | Authenticated | Revoke refresh token and invalidate active session |
| `GET`  | `/api/v1/auth/me` | Authenticated | Retrieve profile and assigned roles of authenticated user |
| `POST` | `/api/v1/auth/users` | `ADMIN` | Provision a new system user account |

### 2. Catalog & Books (`/api/v1/books`)
| Method | Endpoint | Role | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/books` | Public | Search and list books with pagination and live inventory counts |
| `GET` | `/api/v1/books/{id}` | Public | Retrieve book details with available/issued copy counts |
| `POST` | `/api/v1/books` | `LIBRARIAN`, `ADMIN` | Register bibliographic title and batch-create physical copies |
| `POST` | `/api/v1/books/{id}/copies` | `LIBRARIAN`, `ADMIN` | Add additional physical copies with generated barcodes |

### 3. Circulation & Loans (`/api/v1/loans`)
| Method | Endpoint | Role | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/loans/issue` | `LIBRARIAN`, `ADMIN` | Issue physical copy to member (pessimistic lock protected) |
| `POST` | `/api/v1/loans/return` | `LIBRARIAN`, `ADMIN` | Process physical copy return, calculate fines, auto-allocate holds |
| `GET` | `/api/v1/loans/active` | `LIBRARIAN`, `ADMIN` | List all currently active loans across the library |
| `GET` | `/api/v1/loans/overdue` | `LIBRARIAN`, `ADMIN` | List all active loans currently exceeding due date |

### 4. Reservations (`/api/v1/reservations`)
| Method | Endpoint | Role | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/reservations` | `MEMBER`, `LIBRARIAN` | Place a title reservation when all copies are checked out |
| `POST` | `/api/v1/reservations/{id}/cancel` | `MEMBER`, `LIBRARIAN` | Cancel hold and cascade allocated copy to next patron |
| `GET` | `/api/v1/reservations/book/{bookId}`| `LIBRARIAN`, `ADMIN` | Inspect FIFO reservation queue for a book |

### 5. Fines & Accounting (`/api/v1/fines`)
| Method | Endpoint | Role | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/fines/{id}/pay` | `LIBRARIAN`, `ADMIN` | Record partial or full fine payment with audit transaction |
| `POST` | `/api/v1/fines/{id}/waive` | `LIBRARIAN`, `ADMIN` | Waive fine with mandatory audit justification reason |
| `GET` | `/api/v1/fines/member/{memberId}` | `MEMBER` (Self), Staff | Retrieve member's outstanding fine ledger |

### 6. Members & Governance (`/api/v1/members`)
| Method | Endpoint | Role | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/members` | Public / Staff | Register new library member / patron |
| `GET` | `/api/v1/members/{id}/loans` | `MEMBER` (Self), Staff | Get member active and past loans (BOLA protected) |
| `GET` | `/api/v1/members/{id}/fines` | `MEMBER` (Self), Staff | Get member fines (BOLA protected) |
| `GET` | `/api/v1/members/{id}/reservations`| `MEMBER` (Self), Staff | Get member reservations (BOLA protected) |
| `POST`| `/api/v1/members/{id}/suspend` | `ADMIN` | Suspend borrowing privileges for a member |
| `POST`| `/api/v1/members/{id}/reactivate` | `ADMIN` | Reactivate member borrowing privileges |

### 7. Institutional Policies & Settings (`/api/v1/libraries`)
| Method | Endpoint | Role | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/libraries/{id}` | Public | Retrieve library institutional policies and settings |
| `PUT` | `/api/v1/libraries/{id}` | `ADMIN` | Update loan durations, max quotas, daily fine rates, and hold windows |

### 8. Executive Reports (`/api/v1/reports`)
| Method | Endpoint | Role | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/reports/inventory` | `LIBRARIAN`, `ADMIN` | Database-derived inventory counts (total, available, on loan) |
| `GET` | `/api/v1/reports/circulation`| `LIBRARIAN`, `ADMIN` | Active loans, overdue loans, returned loans, borrowing members |
| `GET` | `/api/v1/reports/fines` | `LIBRARIAN`, `ADMIN` | Financial totals: accrued, collected, waived, and outstanding |

---

## 💻 Sample API Usage with cURL

### 1. Authenticate as Librarian
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"librarian", "password":"Password@123"}'
```
*Response:*
```json
{
  "success": true,
  "message": "Authentication successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsIn...",
    "refreshToken": "UOF1zC4z3glaBaG-AbGiJ7uM0RU82WbIOAbsIcTfz2M",
    "tokenType": "Bearer",
    "expiresIn": 604800,
    "username": "librarian",
    "role": "LIBRARIAN",
    "memberId": null
  }
}
```

### 2. Issue a Book Copy (with Idempotency Key)
```bash
curl -X POST http://localhost:8080/api/v1/loans/issue \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: idem-checkout-001" \
  -d '{
    "memberId": 1,
    "barcode": "CPY-9781449373320-001"
  }'
```

### 3. Return a Book Copy
```bash
curl -X POST http://localhost:8080/api/v1/loans/return \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "barcode": "CPY-9781449373320-001"
  }'
```

### 4. Update Institutional Policies (Admin Only)
```bash
curl -X PUT http://localhost:8080/api/v1/libraries/1 \
  -H "Authorization: Bearer <ADMIN_ACCESS_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Infinity Central Academic Library",
    "loanDurationDays": 21,
    "maxLoansPerMember": 5,
    "dailyFineRate": 5.00,
    "maxFinePerLoan": 500.00,
    "reservationHoldHours": 48
  }'
```

---

## ⚙️ Environment Variables Reference

| Variable | Description | Default Value | Production Recommended |
| :--- | :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `prod` | `prod` |
| `DB_HOST` | PostgreSQL hostname | `localhost` | `postgres` or RDS hostname |
| `DB_PORT` | PostgreSQL port | `5432` | `5432` |
| `DB_NAME` | Database schema name | `infinity_library` | `infinity_library` |
| `DB_USERNAME` | Database username | `library_admin` | Dedicated non-root user |
| `DB_PASSWORD` | Database password | `library_secure_password` | High-entropy vault secret |
| `JWT_SECRET` | 256-bit signing key | Default secret | Min 32 chars random secret |
| `JWT_EXPIRATION_MS` | Access token lifespan | `900000` (15 min) | `900000` |
| `JWT_REFRESH_EXPIRATION_MS` | Refresh token lifespan | `604800000` (7 days) | `604800000` |
| `SERVER_PORT` | HTTP server listening port | `8080` | `8080` |

---

## 📄 License
This project is open-source portfolio software licensed under the **Apache License 2.0**.
