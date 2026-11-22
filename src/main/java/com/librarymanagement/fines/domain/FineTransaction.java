package com.librarymanagement.fines.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "fine_transactions")
public class FineTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fine_id", nullable = false)
    private Fine fine;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private FineTransactionType transactionType;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "actor_username", nullable = false, length = 100)
    private String actorUsername;

    @Column(length = 500)
    private String reason;

    @Column(name = "transaction_date", nullable = false)
    private Instant transactionDate = Instant.now();

    public FineTransaction() {}

    public FineTransaction(Fine fine, FineTransactionType transactionType, BigDecimal amount, String actorUsername, String reason) {
        this.fine = fine;
        this.transactionType = transactionType;
        this.amount = amount;
        this.actorUsername = actorUsername;
        this.reason = reason;
        this.transactionDate = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Fine getFine() { return fine; }
    public void setFine(Fine fine) { this.fine = fine; }
    public FineTransactionType getTransactionType() { return transactionType; }
    public void setTransactionType(FineTransactionType transactionType) { this.transactionType = transactionType; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getActorUsername() { return actorUsername; }
    public void setActorUsername(String actorUsername) { this.actorUsername = actorUsername; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Instant getTransactionDate() { return transactionDate; }
    public void setTransactionDate(Instant transactionDate) { this.transactionDate = transactionDate; }
}
