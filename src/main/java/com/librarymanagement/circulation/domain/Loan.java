package com.librarymanagement.circulation.domain;

import com.librarymanagement.catalog.domain.BookCopy;
import com.librarymanagement.members.domain.Member;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "loans")
public class Loan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_copy_id", nullable = false)
    private BookCopy bookCopy;

    @Column(name = "issue_date", nullable = false)
    private Instant issueDate;

    @Column(name = "due_date", nullable = false)
    private Instant dueDate;

    @Column(name = "return_date")
    private Instant returnDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LoanStatus status = LoanStatus.ACTIVE;

    @Column(name = "loan_duration_days", nullable = false)
    private int loanDurationDays;

    @Column(name = "daily_fine_rate_snapshot", nullable = false, precision = 10, scale = 2)
    private BigDecimal dailyFineRateSnapshot;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Loan() {}

    public Loan(Member member, BookCopy bookCopy, Instant issueDate, Instant dueDate, int loanDurationDays, BigDecimal dailyFineRateSnapshot) {
        this.member = member;
        this.bookCopy = bookCopy;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.loanDurationDays = loanDurationDays;
        this.dailyFineRateSnapshot = dailyFineRateSnapshot;
        this.status = LoanStatus.ACTIVE;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Member getMember() { return member; }
    public void setMember(Member member) { this.member = member; }
    public BookCopy getBookCopy() { return bookCopy; }
    public void setBookCopy(BookCopy bookCopy) { this.bookCopy = bookCopy; }
    public Instant getIssueDate() { return issueDate; }
    public void setIssueDate(Instant issueDate) { this.issueDate = issueDate; }
    public Instant getDueDate() { return dueDate; }
    public void setDueDate(Instant dueDate) { this.dueDate = dueDate; }
    public Instant getReturnDate() { return returnDate; }
    public void setReturnDate(Instant returnDate) { this.returnDate = returnDate; }
    public LoanStatus getStatus() { return status; }
    public void setStatus(LoanStatus status) { this.status = status; }
    public int getLoanDurationDays() { return loanDurationDays; }
    public void setLoanDurationDays(int loanDurationDays) { this.loanDurationDays = loanDurationDays; }
    public BigDecimal getDailyFineRateSnapshot() { return dailyFineRateSnapshot; }
    public void setDailyFineRateSnapshot(BigDecimal dailyFineRateSnapshot) { this.dailyFineRateSnapshot = dailyFineRateSnapshot; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
