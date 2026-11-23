# Auth Feature Module (`com.librarymanagement.auth`)

## 1. Feature Purpose & Responsibilities
The **Auth Feature Module** delivers enterprise identity and access management (IAM). It implements Argon2id password hashing, short-lived JSON Web Token (JWT) issuance, and refresh token rotation with automated token family reuse revocation.

## 2. Key Components & Important Files
- `domain/UserAccount.java`: System user record linked to a member profile, role authority, and enabled status.
- `domain/RefreshToken.java`: Secure random token record tracking expiry date, hash, and revocation status.
- `domain/UserRole.java`: Role authorities (`ROLE_ADMIN`, `ROLE_LIBRARIAN`, `ROLE_MEMBER`).
- `repository/UserAccountRepository.java` & `RefreshTokenRepository.java`: Identity persistence layer.
- `service/AuthService.java`: Authentication coordinator handling login, token rotation, and replay attack revocation.
- `web/AuthController.java`: Versioned REST API endpoints (`/api/v1/auth/**`).

## 3. Cryptographic Invariants & Replay Detection
1. **Argon2id Hashing:** Password hashing configured with 65,536 KB memory cost, 3 iterations, and 1 parallelism thread.
2. **Short-Lived Access Tokens:** Signed HMAC-SHA256 JWTs with a 15-minute validity window (900,000 ms).
3. **Token Family Reuse Detection:** On refresh, the presented token is immediately revoked and replaced. If an already-revoked refresh token is presented, the system detects a potential replay/theft attack and immediately revokes all active refresh tokens for that user account.

## 4. Testing & Verification
- Unit Tests: `com.librarymanagement.unit.SecurityCryptoTest`
- Service Tests: `com.librarymanagement.service.AuthServiceTest`
