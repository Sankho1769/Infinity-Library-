# Members Feature Module (`com.librarymanagement.members`)

## 1. Feature Purpose & Responsibilities
The **Members Feature Module** oversees patron registration, membership lifecycle governance, borrowing quotas, and patron confidentiality. It acts as the primary defense against Broken Object-Level Authorization (BOLA/IDOR).

## 2. Key Components & Important Files
- `domain/Member.java`: Patron profile tracking `membershipNumber`, `name`, `email`, `phone`, `status`, `membershipStartDate`, and `membershipExpiryDate`.
- `domain/MemberStatus.java`: Status lifecycle (`ACTIVE`, `SUSPENDED`, `EXPIRED`, `INACTIVE`).
- `repository/MemberRepository.java`: Custom queries by membership number, email, and status.
- `service/MemberService.java`: Business operations for member onboarding, profile updates, and privilege suspension/reactivation.
- `web/MemberController.java`: Versioned REST API endpoints (`/api/v1/members/**`).

## 3. Security & BOLA/IDOR Defenses
Patron personal subresources (loans, fines, reservations) are protected by Spring Security method-level authorization:
```java
@PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN') or (hasRole('MEMBER') and #id == authentication.principal.memberId)")
```
If a logged-in patron attempts to query another member's confidential loans, the expression evaluates to `false`, immediately returning `403 Forbidden`.

## 4. Testing & Verification
- Integration Tests: `com.librarymanagement.integration.SecurityAuthorizationIntegrationTest`
- Service Tests: `com.librarymanagement.service.MemberServiceTest`
