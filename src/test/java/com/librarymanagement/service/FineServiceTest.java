package com.librarymanagement.service;

import com.librarymanagement.circulation.domain.Loan;
import com.librarymanagement.common.audit.AuditService;
import com.librarymanagement.common.exception.BusinessException;
import com.librarymanagement.fines.domain.Fine;
import com.librarymanagement.fines.domain.FineStatus;
import com.librarymanagement.fines.domain.FineTransaction;
import com.librarymanagement.fines.dto.FineDTOs.*;
import com.librarymanagement.fines.repository.FineRepository;
import com.librarymanagement.fines.repository.FineTransactionRepository;
import com.librarymanagement.fines.service.FineService;
import com.librarymanagement.members.domain.Member;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FineServiceTest {

    @Mock private FineRepository fineRepository;
    @Mock private FineTransactionRepository fineTransactionRepository;
    @Mock private AuditService auditService;

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-22T10:00:00Z"), ZoneId.of("UTC"));
    private FineService fineService;

    private Fine fine;
    private Member member;
    private Loan loan;

    @BeforeEach
    void setUp() {
        fineService = new FineService(fineRepository, fineTransactionRepository, auditService, clock);

        member = new Member();
        member.setId(1L);
        member.setName("Elena Rostova");

        loan = new Loan();
        loan.setId(10L);

        fine = new Fine(loan, member, new BigDecimal("50.00"), LocalDate.of(2026, 11, 20));
        fine.setId(100L);
    }

    @Test
    @DisplayName("Record partial payment reduces balance and updates status to PARTIALLY_PAID")
    void testRecordPartialPayment() {
        when(fineRepository.findById(100L)).thenReturn(Optional.of(fine));
        when(fineRepository.save(any(Fine.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PayFineRequest request = new PayFineRequest(new BigDecimal("20.00"));
        FineResponse response = fineService.payFine(100L, request, "librarian");

        assertThat(response).isNotNull();
        assertThat(response.amountPaid()).isEqualByComparingTo("20.00");
        assertThat(response.amountOutstanding()).isEqualByComparingTo("30.00");
        assertThat(response.status()).isEqualTo(FineStatus.PARTIALLY_PAID);

        verify(fineTransactionRepository).save(any(FineTransaction.class));
        verify(fineRepository).save(fine);
    }

    @Test
    @DisplayName("Record full payment completely settles fine with status PAID")
    void testRecordFullPaymentSettlesFine() {
        when(fineRepository.findById(100L)).thenReturn(Optional.of(fine));
        when(fineRepository.save(any(Fine.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PayFineRequest request = new PayFineRequest(new BigDecimal("50.00"));
        FineResponse response = fineService.payFine(100L, request, "librarian");

        assertThat(response.amountPaid()).isEqualByComparingTo("50.00");
        assertThat(response.amountOutstanding()).isEqualByComparingTo("0.00");
        assertThat(response.status()).isEqualTo(FineStatus.PAID);
    }

    @Test
    @DisplayName("Reject fine payment exceeding outstanding balance")
    void testRejectPaymentExceedingBalance() {
        when(fineRepository.findById(100L)).thenReturn(Optional.of(fine));

        PayFineRequest request = new PayFineRequest(new BigDecimal("60.00")); // Balance is 50.00

        assertThatThrownBy(() -> fineService.payFine(100L, request, "librarian"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("exceeds outstanding fine amount");
    }

    @Test
    @DisplayName("Authorized waiver with mandatory reason successfully settles fine with status WAIVED")
    void testAuthorizedWaiverWithReason() {
        when(fineRepository.findById(100L)).thenReturn(Optional.of(fine));
        when(fineRepository.save(any(Fine.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WaiveFineRequest request = new WaiveFineRequest(new BigDecimal("50.00"), "Medical emergency exemption authorized by director");
        FineResponse response = fineService.waiveFine(100L, request, "admin");

        assertThat(response.amountWaived()).isEqualByComparingTo("50.00");
        assertThat(response.amountOutstanding()).isEqualByComparingTo("0.00");
        assertThat(response.status()).isEqualTo(FineStatus.WAIVED);

        verify(fineTransactionRepository).save(any(FineTransaction.class));
    }
}
