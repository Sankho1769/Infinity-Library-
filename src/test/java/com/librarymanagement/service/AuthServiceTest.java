package com.librarymanagement.service;

import com.librarymanagement.auth.domain.RefreshToken;
import com.librarymanagement.auth.domain.UserAccount;
import com.librarymanagement.auth.domain.UserRole;
import com.librarymanagement.auth.dto.AuthDTOs.*;
import com.librarymanagement.auth.repository.RefreshTokenRepository;
import com.librarymanagement.auth.repository.UserAccountRepository;
import com.librarymanagement.auth.service.AuthService;
import com.librarymanagement.common.audit.AuditService;
import com.librarymanagement.common.exception.ForbiddenException;
import com.librarymanagement.common.exception.UnauthorizedException;
import com.librarymanagement.common.security.AppPasswordEncoder;
import com.librarymanagement.common.security.JwtTokenProvider;
import com.librarymanagement.common.security.UserPrincipal;
import com.librarymanagement.members.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserAccountRepository userAccountRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private AppPasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private AuditService auditService;

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-22T10:00:00Z"), ZoneId.of("UTC"));
    private AuthService authService;

    private UserAccount user;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userAccountRepository,
                refreshTokenRepository,
                memberRepository,
                passwordEncoder,
                jwtTokenProvider,
                auditService,
                clock
        );

        user = new UserAccount("librarian_jane", "$argon2id$hash", UserRole.LIBRARIAN, null);
        user.setId(5L);
        user.setActive(true);
    }

    @Test
    @DisplayName("Successful login yields valid JWT access token and secure refresh token")
    void testLoginSuccess() {
        when(userAccountRepository.findByUsernameWithMember("librarian_jane")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password@123", "$argon2id$hash")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(any(UserPrincipal.class))).thenReturn("mock.jwt.token");
        when(jwtTokenProvider.getRefreshTokenExpirationMs()).thenReturn(604800000L);

        LoginRequest request = new LoginRequest("librarian_jane", "Password@123");
        TokenResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("mock.jwt.token");
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.role()).isEqualTo("LIBRARIAN");

        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Login with invalid password throws UnauthorizedException")
    void testLoginInvalidPassword() {
        when(userAccountRepository.findByUsernameWithMember("librarian_jane")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword", "$argon2id$hash")).thenReturn(false);

        LoginRequest request = new LoginRequest("librarian_jane", "WrongPassword");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid username or password");
    }

    @Test
    @DisplayName("Login with deactivated account throws ForbiddenException")
    void testLoginDeactivatedAccount() {
        user.setActive(false);
        when(userAccountRepository.findByUsernameWithMember("librarian_jane")).thenReturn(Optional.of(user));

        LoginRequest request = new LoginRequest("librarian_jane", "Password@123");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("account is suspended or disabled");
    }

    @Test
    @DisplayName("SECURITY CONTROL: Refresh token reuse detection revokes all user sessions")
    void testRefreshTokenReuseDetection() {
        RefreshToken stolenToken = new RefreshToken(user, "stolenHash", clock.instant().plusSeconds(3600));
        stolenToken.setRevoked(true); // Token was already rotated and revoked!

        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(stolenToken));

        RefreshTokenRequest request = new RefreshTokenRequest("stolenRawTokenValue");

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Compromised refresh token detected");

        // Verifies defense-in-depth: all active tokens for this user are invalidated
        verify(refreshTokenRepository).revokeAllByUserId(user.getId());
    }
}
