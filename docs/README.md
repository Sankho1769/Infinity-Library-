# Infinity Library Documentation Index
**Platform:** Infinity Library Management System (Enterprise LMS 2.0)  
**Standard:** ISO/IEC/IEEE 29148 & Google/Amazon SDE Quality Standards  
**Last Verified:** November 23, 2026

---

## 🏛 Core Architecture & Specifications
- [**`SRS.md`**](../SRS.md) — IEEE 830 / ISO/IEC/IEEE 29148 Software Requirements Specification. Contains functional requirements (`FR-CAT`, `FR-CIR`, `FR-RES`, `FR-FIN`, `FR-SEC`), business rules matrix, and requirements traceability matrix (RTM).
- [**`PRD.md`**](../PRD.md) — Product Requirements Document. Personas, user journeys, Mermaid sequence diagrams, MVP scope vs. post-MVP roadmap, and KPIs.
- [**`PROJECT_REPORT.md`**](../PROJECT_REPORT.md) — Comprehensive Engineering Whitepaper & Technical Report. Mathematical concurrency proofs, 3NF normalization analysis, threat modeling, and modernization audit of legacy baseline.
- [**`README.md`**](../README.md) — Root Developer Manual, Quickstart, cURL examples, Docker instructions, and seed account credentials.

---

## 🔍 Quality, Security & Audit Reports
- [**`WEBSITE_AUDIT_REPORT.md`**](../WEBSITE_AUDIT_REPORT.md) — Complete 20-pattern UX/UI audit, responsive evaluation (320px–1920px), Core Web Vitals, accessibility evaluation, and implementation status evidence.
- [**`SECURITY_REVIEW.md`**](../SECURITY_REVIEW.md) — Enterprise security review covering Argon2id hashing, JWT lifecycle, token family reuse detection, BOLA/IDOR mitigations, and OWASP Top 10 defenses.
- [**`QA_REPORT.md`**](../QA_REPORT.md) — Automated and manual QA report covering unit, service, concurrency, security, and architecture test suites (47/47 passing).
- [**`docs/DOCUMENTATION_CHECKLIST.md`**](DOCUMENTATION_CHECKLIST.md) — Master documentation verification matrix and feature folder coverage tracking.

---

## 📦 Feature-Level Documentation
Every identifiable domain feature package contains a dedicated, comprehensive `README.md`:
1. [**`catalog` Feature Documentation**](../src/main/java/com/librarymanagement/catalog/README.md) — Bibliographic titles, physical copy serialization, barcode generation, live inventory derivation.
2. [**`circulation` Feature Documentation**](../src/main/java/com/librarymanagement/circulation/README.md) — Pessimistic locking issue desk, return desk, overdue calculations, idempotency protection.
3. [**`reservations` Feature Documentation**](../src/main/java/com/librarymanagement/reservations/README.md) — FIFO reservation hold queue, automatic copy hold allocation, 72-hour pickup expiration scheduler.
4. [**`fines` Feature Documentation**](../src/main/java/com/librarymanagement/fines/README.md) — Deterministic daily fine accruals, partial/full payments, audited administrative fee waivers.
5. [**`members` Feature Documentation**](../src/main/java/com/librarymanagement/members/README.md) — Patron lifecycle, status governance, borrowing quota enforcement, BOLA-protected subresources.
6. [**`auth` Feature Documentation**](../src/main/java/com/librarymanagement/auth/README.md) — Argon2id password hashing, JWT access tokens, refresh token rotation, token replay attack detection.
7. [**`reports` Feature Documentation**](../src/main/java/com/librarymanagement/reports/README.md) — Real-time aggregate inventory, circulation analytics, and financial penalty reporting.
8. [**`library` Feature Documentation**](../src/main/java/com/librarymanagement/library/README.md) — Institutional configuration, timezone settings, loan period, and fine rate policies.
