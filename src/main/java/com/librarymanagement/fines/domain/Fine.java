package com.librarymanagement.fines.domain;

import com.librarymanagement.circulation.domain.Loan;
import com.librarymanagement.members.domain.Member;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "fines")
public class Fine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_id", nullable = false, unique = true)
    private Loan loan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "fine_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal fineAmount;

    @Column(name = "amount_paid", nullable = false, precision = 10, scale = 2)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(name = "amount_waived", nullable = false, precision = 10, scale = 2)
    private BigDecimal amountWaived = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FineStatus status = FineStatus.UNPAID;

    @Column(name = "accrual_date", nullable = false)
    private LocalDate accrualDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "fine", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<FineTransaction> transactions = new ArrayList<>();

    public Fine() {}

    public Fine(Loan loan, Member member, BigDecimal fineAmount, LocalDate accrualDate) {
        this.loan = loan;
        this.member = member;
        this.fineAmount = fineAmount;
        this.amountPaid = BigDecimal.ZERO;
        this.amountWaived = BigDecimal.ZERO;
        this.status = FineStatus.UNPAID;
        this.accrualDate = accrualDate;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public BigDecimal getAmountOutstanding() {
        return fineAmount.subtract(amountPaid).subtract(amountWaived);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Loan getLoan() { return loan; }
    public void setLoan(Loan loan) { this.loan = loan; }
    public Member getMember() { return member; }
    public void setMember(Member member) { this.member = member; }
    public BigDecimal getFineAmount() { return fineAmount; }
    public void setFineAmount(BigDecimal fineAmount) { this.fineAmount = fineAmount; }
    public BigDecimal getAmountPaid() { return amountPaid; }
    public void setAmountPaid(BigDecimal amountPaid) { this.amountPaid = amountPaid; }
    public BigDecimal getAmountWaived() { return amountWaived; }
    public void setAmountWaived(BigDecimal amountWaived) { this.amountWaived = amountWaived; }
    public FineStatus getStatus() { return status; }
    public void setStatus(FineStatus status) { this.status = status; }
    public LocalDate getAccrualDate() { return accrualDate; }
    public void setAccrualDate(LocalDate accrualDate) { this.accrualDate = accrualDate; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public List<FineTransaction> getTransactions() { return transactions; }
    public void setTransactions(List<FineTransaction> transactions) { this.transactions = transactions; }
}
