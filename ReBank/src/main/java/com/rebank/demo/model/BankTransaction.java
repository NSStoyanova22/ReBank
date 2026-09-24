package com.rebank.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "bank_transactions", indexes = @Index(name = "idx_bank_transactions_account_created", columnList = "bank_account_id,created_at"))
public class BankTransaction {

    public enum Type {
        DEPOSIT,
        WITHDRAWAL,
        TRANSFER
    }

    public enum Direction {
        CREDIT,
        DEBIT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bank_account_id", nullable = false)
    private BankAccount bankAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counterparty_bank_account_id")
    private BankAccount counterpartyBankAccount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Type type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Direction direction;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @Column(nullable = false, length = 200)
    private String description;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected BankTransaction() {
    }

    public BankTransaction(
            BankAccount bankAccount,
            BankAccount counterpartyBankAccount,
            Type type,
            Direction direction,
            BigDecimal amount,
            BigDecimal balanceAfter,
            String description) {
        this.bankAccount = bankAccount;
        this.counterpartyBankAccount = counterpartyBankAccount;
        this.type = type;
        this.direction = direction;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
    }

    @PrePersist
    void markCreated() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public BankAccount getBankAccount() {
        return bankAccount;
    }

    public BankAccount getCounterpartyBankAccount() {
        return counterpartyBankAccount;
    }

    public Type getType() {
        return type;
    }

    public Direction getDirection() {
        return direction;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
