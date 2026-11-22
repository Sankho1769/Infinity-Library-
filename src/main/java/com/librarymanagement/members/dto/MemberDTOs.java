package com.librarymanagement.members.dto;

import com.librarymanagement.members.domain.Member;
import com.librarymanagement.members.domain.MemberStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;

public class MemberDTOs {

    public record RegisterMemberRequest(
            @NotBlank(message = "Name is required")
            String name,

            @NotBlank(message = "Email is required")
            @Email(message = "Valid email is required")
            String email,

            String phone,

            Integer membershipDurationMonths
    ) {}

    public record UpdateMemberRequest(
            @NotBlank(message = "Name is required")
            String name,

            @Email(message = "Valid email is required")
            String email,

            String phone,

            MemberStatus status
    ) {}

    public record MemberResponse(
            Long id,
            String membershipNumber,
            String name,
            String email,
            String phone,
            MemberStatus status,
            LocalDate membershipStartDate,
            LocalDate membershipExpiryDate,
            boolean eligibleToBorrow
    ) {
        public static MemberResponse from(Member member) {
            return new MemberResponse(
                    member.getId(),
                    member.getMembershipNumber(),
                    member.getName(),
                    member.getEmail(),
                    member.getPhone(),
                    member.getStatus(),
                    member.getMembershipStartDate(),
                    member.getMembershipExpiryDate(),
                    member.isBorrowingEligible()
            );
        }
    }

    public record MemberDetailResponse(
            MemberResponse profile,
            int activeLoansCount,
            int activeReservationsCount,
            BigDecimal totalOutstandingFines
    ) {}
}
