package com.librarymanagement.auth.dto;

import com.librarymanagement.auth.domain.UserAccount;
import com.librarymanagement.auth.domain.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AuthDTOs {

    public record LoginRequest(
            @NotBlank(message = "Username is required")
            String username,

            @NotBlank(message = "Password is required")
            String password
    ) {}

    public record TokenResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresIn,
            String username,
            String role,
            Long memberId
    ) {
        public static TokenResponse of(String accessToken, String refreshToken, long expiresIn, String username, String role, Long memberId) {
            return new TokenResponse(accessToken, refreshToken, "Bearer", expiresIn, username, role, memberId);
        }
    }

    public record RefreshTokenRequest(
            @NotBlank(message = "Refresh token is required")
            String refreshToken
    ) {}

    public record LogoutRequest(
            String refreshToken
    ) {}

    public record CreateUserRequest(
            @NotBlank(message = "Username is required")
            @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
            String username,

            @NotBlank(message = "Password is required")
            @Size(min = 8, message = "Password must be at least 8 characters")
            String password,

            @NotNull(message = "Role is required")
            UserRole role,

            Long memberId
    ) {}

    public record UserResponse(
            Long id,
            String username,
            UserRole role,
            Long memberId,
            boolean active
    ) {
        public static UserResponse from(UserAccount user) {
            return new UserResponse(
                    user.getId(),
                    user.getUsername(),
                    user.getRole(),
                    user.getMember() != null ? user.getMember().getId() : null,
                    user.isActive()
            );
        }
    }
}
