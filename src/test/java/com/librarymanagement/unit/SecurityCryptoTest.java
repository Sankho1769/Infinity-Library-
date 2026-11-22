package com.librarymanagement.unit;

import com.librarymanagement.common.security.AppPasswordEncoder;
import com.librarymanagement.common.security.JwtTokenProvider;
import com.librarymanagement.common.security.UserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityCryptoTest {

    private final AppPasswordEncoder passwordEncoder = new AppPasswordEncoder();
    private final JwtTokenProvider tokenProvider = new JwtTokenProvider(
            "test_jwt_secret_key_minimum_256_bits_length_for_hmac_sha256_algorithm_2026",
            60000,
            120000
    );

    @Test
    @DisplayName("Argon2id encoder must hash passwords securely with salt and verify matches")
    void testArgon2idPasswordEncoder() {
        String rawPassword = "StrongPassword@2026!";
        String encoded1 = passwordEncoder.encode(rawPassword);
        String encoded2 = passwordEncoder.encode(rawPassword);

        // Standard Argon2id string format
        assertThat(encoded1).startsWith("$argon2id$");
        assertThat(encoded2).startsWith("$argon2id$");

        // Salt ensures two hashes of identical plaintexts are distinct
        assertThat(encoded1).isNotEqualTo(encoded2);

        // Correct verification
        assertThat(passwordEncoder.matches(rawPassword, encoded1)).isTrue();
        assertThat(passwordEncoder.matches(rawPassword, encoded2)).isTrue();

        // Rejection of invalid passwords
        assertThat(passwordEncoder.matches("WrongPassword", encoded1)).isFalse();
    }

    @Test
    @DisplayName("JWT Token Provider generates, validates, and extracts principal claims correctly")
    void testJwtTokenGenerationAndClaims() {
        UserPrincipal principal = new UserPrincipal(101L, "elena_r", "secret", "MEMBER", 55L, true);

        String token = tokenProvider.generateAccessToken(principal);
        assertThat(token).isNotBlank();

        assertThat(tokenProvider.validateToken(token)).isTrue();
        assertThat(tokenProvider.getUsernameFromToken(token)).isEqualTo("elena_r");
        assertThat(tokenProvider.getRoleFromToken(token)).isEqualTo("MEMBER");
        assertThat(tokenProvider.getMemberIdFromToken(token)).isEqualTo(55L);

        // Rejection of tampered token
        String tamperedToken = token + "corrupted";
        assertThat(tokenProvider.validateToken(tamperedToken)).isFalse();
    }
}
