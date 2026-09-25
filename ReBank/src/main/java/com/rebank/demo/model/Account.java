package com.rebank.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "accounts")
public class Account {

    public enum AccountType {
        CLIENT,
        EMPLOYEE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountType accountType = AccountType.CLIENT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_bank_id")
    private Bank employeeBank;

    @Column(length = 80)
    private String employeeRole;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Account() {
    }

    public Account(String username, String email, String passwordHash) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public Account(String username, String email, String passwordHash, Bank employeeBank, String employeeRole) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.accountType = AccountType.EMPLOYEE;
        this.employeeBank = employeeBank;
        this.employeeRole = employeeRole;
    }

    @PrePersist
    void markCreated() {
        if (accountType == null) {
            accountType = AccountType.CLIENT;
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public AccountType getAccountType() {
        return accountType == null ? AccountType.CLIENT : accountType;
    }

    public Bank getEmployeeBank() {
        return employeeBank;
    }

    public String getEmployeeRole() {
        return employeeRole;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
