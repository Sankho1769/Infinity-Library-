package com.librarymanagement.auth.service;

import com.librarymanagement.auth.domain.RefreshToken;
import com.librarymanagement.auth.domain.UserAccount;
import com.librarymanagement.auth.dto.AuthDTOs.*;
import com.librarymanagement.auth.repository.RefreshTokenRepository;
import com.librarymanagement.auth.repository.UserAccountRepository;
import com.librarymanagement.common.audit.AuditService;
import com.librarymanagement.common.exception.*;
import com.librarymanagement.common.security.AppPasswordEncoder;
import com.librarymanagement.common.security.JwtTokenProvider;
import com.librarymanagement.common.security.UserPrincipal;
import com.librarymanagement.members.domain.Member;
import com.librarymanagement.members.repository.MemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserAccountRepository userAccountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberRepository memberRepository;
    private final AppPasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuditService auditService;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            UserAccountRepository userAccountRepository,
            RefreshTokenRepository refreshTokenRepository,
            MemberRepository memberRepository,
            AppPasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            AuditService auditService,
            Clock clock
    ) {
        this.userAccountRepository = userAccountRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        UserAccount user = userAccountRepository.findByUsernameWithMember(request.username())
                .orElseThrow(() -> new UnauthorizedException(ErrorCode.UNAUTHORIZED_OPERATION, "Invalid username or password"));

        if (!user.isActive()) {
            throw new ForbiddenException(ErrorCode.MEMBER_INACTIVE, "User account is suspended or disabled");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("Failed login attempt for username: {}", request.username());
            throw new UnauthorizedException(ErrorCode.UNAUTHORIZED_OPERATION, "Invalid username or password");
        }

        UserPrincipal principal = new UserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                user.getRole().name(),
                user.getMember() != null ? user.getMember().getId() : null,
                user.isActive()
        );

        String accessToken = jwtTokenProvider.generateAccessToken(principal);
        String rawRefreshToken = generateSecureTokenString();
        String tokenHash = hashToken(rawRefreshToken);

        Instant expiresAt = clock.instant().plusMillis(jwtTokenProvider.getRefreshTokenExpirationMs());
        RefreshToken refreshTokenEntity = new RefreshToken(user, tokenHash, expiresAt);
        refreshTokenRepository.save(refreshTokenEntity);

        auditService.record(user.getUsername(), "AUTH_LOGIN", "USER", user.getId().toString(), null, "User logged in successfully");

        return TokenResponse.of(
                accessToken,
                rawRefreshToken,
                jwtTokenProvider.getRefreshTokenExpirationMs() / 1000,
                user.getUsername(),
                user.getRole().name(),
                principal.getMemberId()
        );
    }

    @Transactional
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        String tokenHash = hashToken(request.refreshToken());

        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException(ErrorCode.UNAUTHORIZED_OPERATION, "Invalid refresh token"));

        UserAccount user = storedToken.getUserAccount();

        // Token Reuse Detection (OWASP ASVS 5.0.0 & RFC 6749)
        if (storedToken.isRevoked()) {
            log.warn("SECURITY ALERT: Attempted reuse of revoked refresh token for user: {}", user.getUsername());
            refreshTokenRepository.revokeAllByUserId(user.getId());
            auditService.record(user.getUsername(), "REFRESH_TOKEN_REUSE_DETECTED", "USER", user.getId().toString(), null,
                    "Compromised refresh token reused. All active sessions invalidated.");
            throw new UnauthorizedException(ErrorCode.UNAUTHORIZED_OPERATION, "Compromised refresh token detected. All sessions revoked.");
        }

        if (storedToken.isExpired(clock.instant())) {
            storedToken.setRevoked(true);
            refreshTokenRepository.save(storedToken);
            throw new UnauthorizedException(ErrorCode.UNAUTHORIZED_OPERATION, "Refresh token has expired. Please log in again.");
        }

        if (!user.isActive()) {
            throw new ForbiddenException(ErrorCode.MEMBER_INACTIVE, "User account is suspended");
        }

        // Generate new rotated token pair
        String newRawRefreshToken = generateSecureTokenString();
        String newTokenHash = hashToken(newRawRefreshToken);

        storedToken.setRevoked(true);
        storedToken.setReplacedByTokenHash(newTokenHash);
        refreshTokenRepository.save(storedToken);

        Instant expiresAt = clock.instant().plusMillis(jwtTokenProvider.getRefreshTokenExpirationMs());
        RefreshToken newRefreshTokenEntity = new RefreshToken(user, newTokenHash, expiresAt);
        refreshTokenRepository.save(newRefreshTokenEntity);

        UserPrincipal principal = new UserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                user.getRole().name(),
                user.getMember() != null ? user.getMember().getId() : null,
                user.isActive()
        );

        String newAccessToken = jwtTokenProvider.generateAccessToken(principal);

        return TokenResponse.of(
                newAccessToken,
                newRawRefreshToken,
                jwtTokenProvider.getRefreshTokenExpirationMs() / 1000,
                user.getUsername(),
                user.getRole().name(),
                principal.getMemberId()
        );
    }

    @Transactional
    public void logout(String rawRefreshToken, String currentUsername) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            String tokenHash = hashToken(rawRefreshToken);
            refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
            });
        }
        auditService.record(currentUsername != null ? currentUsername : "ANONYMOUS", "AUTH_LOGOUT", "USER", null, null, "User logged out");
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userAccountRepository.existsByUsername(request.username())) {
            throw new ConflictException(ErrorCode.VALIDATION_FAILED, "Username '" + request.username() + "' is already registered");
        }

        Member member = null;
        if (request.memberId() != null) {
            member = memberRepository.findById(request.memberId())
                    .orElseThrow(() -> new ResourceNotFoundException("Member", request.memberId()));
        }

        String encodedPassword = passwordEncoder.encode(request.password());
        UserAccount account = new UserAccount(request.username(), encodedPassword, request.role(), member);
        UserAccount saved = userAccountRepository.save(account);

        auditService.record("ADMIN", "USER_CREATED", "USER", saved.getId().toString(), null, "Created user: " + saved.getUsername());
        return UserResponse.from(saved);
    }

    private String generateSecureTokenString() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
