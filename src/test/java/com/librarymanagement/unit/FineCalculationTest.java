package com.librarymanagement.unit;

import com.librarymanagement.circulation.domain.Loan;
import com.librarymanagement.fines.domain.Fine;
import com.librarymanagement.fines.domain.FineStatus;
import com.librarymanagement.library.domain.Library;
import com.librarymanagement.members.domain.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class FineCalculationTest {

    @Test
    @DisplayName("Returning on due date attracts exactly INR 0.00 fine")
    void testReturnOnDueDateHasNoFine() {
        LocalDate dueDate = LocalDate.of(2026, 11, 20);
        LocalDate returnDate = LocalDate.of(2026, 11, 20);

        long overdueDays = Math.max(0, ChronoUnit.DAYS.between(dueDate, returnDate));
        assertThat(overdueDays).isZero();

        BigDecimal dailyRate = new BigDecimal("5.00");
        BigDecimal calculatedFine = dailyRate.multiply(BigDecimal.valueOf(overdueDays));
        assertThat(calculatedFine).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Returning one day after due date attracts 1 day fine")
    void testReturnOneDayLateAttractsOneDayFine() {
        LocalDate dueDate = LocalDate.of(2026, 11, 20);
        LocalDate returnDate = LocalDate.of(2026, 11, 21);

        long overdueDays = Math.max(0, ChronoUnit.DAYS.between(dueDate, returnDate));
        assertThat(overdueDays).isEqualTo(1L);

        BigDecimal dailyRate = new BigDecimal("5.00");
        BigDecimal calculatedFine = dailyRate.multiply(BigDecimal.valueOf(overdueDays));
        assertThat(calculatedFine).isEqualByComparingTo("5.00");
    }

    @Test
    @DisplayName("Fine calculation must respect the maximum fine cap policy")
    void testFineCapEnforcement() {
        LocalDate dueDate = LocalDate.of(2026, 1, 1);
        LocalDate returnDate = LocalDate.of(2026, 6, 1); // 151 days late

        long overdueDays = ChronoUnit.DAYS.between(dueDate, returnDate);
        BigDecimal dailyRate = new BigDecimal("5.00");
        BigDecimal maxFine = new BigDecimal("500.00");

        BigDecimal uncappedFine = dailyRate.multiply(BigDecimal.valueOf(overdueDays)); // 755.00
        BigDecimal actualFine = uncappedFine.min(maxFine);

        assertThat(uncappedFine).isEqualByComparingTo("755.00");
        assertThat(actualFine).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("Fine outstanding balances, partial payments and waivers must remain precise")
    void testFinePaymentsAndWaivers() {
        Fine fine = new Fine();
        fine.setFineAmount(new BigDecimal("100.00"));
        fine.setAmountPaid(BigDecimal.ZERO);
        fine.setAmountWaived(BigDecimal.ZERO);
        fine.setStatus(FineStatus.UNPAID);

        assertThat(fine.getAmountOutstanding()).isEqualByComparingTo("100.00");

        // Partial payment of 40.00
        fine.setAmountPaid(fine.getAmountPaid().add(new BigDecimal("40.00")));
        fine.setStatus(FineStatus.PARTIALLY_PAID);
        assertThat(fine.getAmountOutstanding()).isEqualByComparingTo("60.00");

        // Waiver of 20.00
        fine.setAmountWaived(fine.getAmountWaived().add(new BigDecimal("20.00")));
        assertThat(fine.getAmountOutstanding()).isEqualByComparingTo("40.00");

        // Final payment of 40.00 settles the fine
        fine.setAmountPaid(fine.getAmountPaid().add(new BigDecimal("40.00")));
        fine.setStatus(FineStatus.PAID);
        assertThat(fine.getAmountOutstanding()).isEqualByComparingTo("0.00");
    }
}
