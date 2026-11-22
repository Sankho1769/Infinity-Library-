package com.librarymanagement.fines.dto;

import com.librarymanagement.fines.domain.Fine;
import com.librarymanagement.fines.domain.FineStatus;
import com.librarymanagement.fines.domain.FineTransaction;
import com.librarymanagement.fines.domain.FineTransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

public class FineDTOs {

    public record PayFineRequest(
            @NotNull(message = "Payment amount is required")
            @DecimalMin(value = "0.01", message = "Payment amount must be greater than zero")
            BigDecimal amount
    ) {}

    public record WaiveFineRequest(
            @NotNull(message = "Waiver amount is required")
            @DecimalMin(value = "0.01", message = "Waiver amount must be greater than zero")
            BigDecimal amount,

            @NotBlank(message = "Mandatory justification reason is required for audit compliance")
            String reason
    ) {}

    public record FineTransactionResponse(
            Long id,
            FineTransactionType transactionType,
            BigDecimal amount,
            String actorUsername,
            String reason,
            Instant transactionDate
    ) {
        public static FineTransactionResponse from(FineTransaction tx) {
            return new FineTransactionResponse(
                    tx.getId(),
                    tx.getTransactionType(),
                    tx.getAmount(),
                    tx.getActorUsername(),
                    tx.getReason(),
                    tx.getTransactionDate()
            );
        }
    }

    public record FineResponse(
            Long id,
            Long loanId,
            Long memberId,
            String memberName,
            BigDecimal fineAmount,
            BigDecimal amountPaid,
            BigDecimal amountWaived,
            BigDecimal amountOutstanding,
            FineStatus status,
            LocalDate accrualDate,
            List<FineTransactionResponse> transactions
    ) {
        public static FineResponse from(Fine fine) {
            List<FineTransactionResponse> txList = (fine.getTransactions() != null)
                    ? fine.getTransactions().stream().map(FineTransactionResponse::from).toList()
                    : Collections.emptyList();

            return new FineResponse(
                    fine.getId(),
                    fine.getLoan().getId(),
                    fine.getMember().getId(),
                    fine.getMember().getName(),
                    fine.getFineAmount(),
                    fine.getAmountPaid(),
                    fine.getAmountWaived(),
                    fine.getAmountOutstanding(),
                    fine.getStatus(),
                    fine.getAccrualDate(),
                    txList
            );
        }
    }
}
