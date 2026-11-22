package com.librarymanagement.library.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "libraries")
public class Library {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(length = 500)
    private String address;

    @Column
    private String email;

    @Column(length = 50)
    private String phone;

    @Column(nullable = false, length = 100)
    private String timezone = "UTC";

    @Column(name = "loan_duration_days", nullable = false)
    private int loanDurationDays = 14;

    @Column(name = "max_loans_per_member", nullable = false)
    private int maxLoansPerMember = 5;

    @Column(name = "daily_fine_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal dailyFineRate = new BigDecimal("5.00");

    @Column(name = "max_fine_per_loan", nullable = false, precision = 10, scale = 2)
    private BigDecimal maxFinePerLoan = new BigDecimal("500.00");

    @Column(name = "reservation_hold_hours", nullable = false)
    private int reservationHoldHours = 48;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Library() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public int getLoanDurationDays() { return loanDurationDays; }
    public void setLoanDurationDays(int loanDurationDays) { this.loanDurationDays = loanDurationDays; }
    public int getMaxLoansPerMember() { return maxLoansPerMember; }
    public void setMaxLoansPerMember(int maxLoansPerMember) { this.maxLoansPerMember = maxLoansPerMember; }
    public BigDecimal getDailyFineRate() { return dailyFineRate; }
    public void setDailyFineRate(BigDecimal dailyFineRate) { this.dailyFineRate = dailyFineRate; }
    public BigDecimal getMaxFinePerLoan() { return maxFinePerLoan; }
    public void setMaxFinePerLoan(BigDecimal maxFinePerLoan) { this.maxFinePerLoan = maxFinePerLoan; }
    public int getReservationHoldHours() { return reservationHoldHours; }
    public void setReservationHoldHours(int reservationHoldHours) { this.reservationHoldHours = reservationHoldHours; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
