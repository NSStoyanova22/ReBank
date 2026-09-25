package com.rebank.demo.service;

import com.rebank.demo.model.Account;
import com.rebank.demo.model.Account.AccountType;
import com.rebank.demo.model.Bank;
import com.rebank.demo.model.BankAccount;
import com.rebank.demo.model.BankTransaction;
import com.rebank.demo.model.Client;
import com.rebank.demo.repository.AccountRepository;
import com.rebank.demo.repository.BankAccountRepository;
import com.rebank.demo.repository.BankRepository;
import com.rebank.demo.repository.BankTransactionRepository;
import com.rebank.demo.repository.ClientRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BankEmployeeService {

    private final AccountRepository accounts;
    private final BankRepository banks;
    private final ClientRepository clients;
    private final BankAccountRepository bankAccounts;
    private final BankTransactionRepository transactions;
    private final PasswordEncoder passwordEncoder;

    public BankEmployeeService(
            AccountRepository accounts,
            BankRepository banks,
            ClientRepository clients,
            BankAccountRepository bankAccounts,
            BankTransactionRepository transactions,
            PasswordEncoder passwordEncoder) {
        this.accounts = accounts;
        this.banks = banks;
        this.clients = clients;
        this.bankAccounts = bankAccounts;
        this.transactions = transactions;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Account createEmployee(Long bankId, String username, String email, String role, String password) {
        validatePassword(password);
        if (isBlank(username)) {
            throw new IllegalArgumentException("Username is required.");
        }
        if (isBlank(email)) {
            throw new IllegalArgumentException("Email is required.");
        }
        if (isBlank(role)) {
            throw new IllegalArgumentException("Employee role is required.");
        }

        String employeeUsername = username.trim();
        String employeeEmail = email.trim().toLowerCase();
        String employeeRole = role.trim();
        if (employeeUsername.length() > 50) {
            throw new IllegalArgumentException("Username is too long.");
        }
        if (employeeEmail.length() > 120) {
            throw new IllegalArgumentException("Email is too long.");
        }
        if (employeeRole.length() > 80) {
            throw new IllegalArgumentException("Employee role is too long.");
        }
        if (accounts.existsByUsernameIgnoreCase(employeeUsername)) {
            throw new IllegalArgumentException("Username is already taken.");
        }
        if (accounts.existsByEmailIgnoreCase(employeeEmail)) {
            throw new IllegalArgumentException("Email is already registered.");
        }

        Bank bank = banks.findById(bankId)
                .orElseThrow(() -> new IllegalArgumentException("Bank not found."));
        return accounts.save(new Account(
                employeeUsername,
                employeeEmail,
                passwordEncoder.encode(password),
                bank,
                employeeRole));
    }

    @Transactional(readOnly = true)
    public Account authenticate(String usernameOrEmail, String password) {
        if (isBlank(usernameOrEmail) || isBlank(password)) {
            throw new IllegalArgumentException("Invalid employee credentials.");
        }
        Account employee = findAccount(usernameOrEmail.trim())
                .filter(account -> account.getAccountType() == AccountType.EMPLOYEE)
                .orElseThrow(() -> new IllegalArgumentException("Invalid employee credentials."));
        if (!passwordEncoder.matches(password, employee.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid employee credentials.");
        }
        return employee;
    }

    @Transactional(readOnly = true)
    public List<Account> getAllEmployees() {
        return accounts.findByAccountTypeOrderByCreatedAtAscIdAsc(AccountType.EMPLOYEE);
    }

    @Transactional(readOnly = true)
    public Account getEmployeeById(Long id) {
        return accounts.findByIdAndAccountType(id, AccountType.EMPLOYEE)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found."));
    }

    @Transactional(readOnly = true)
    public List<Client> getAllClients() {
        return clients.findAll();
    }

    @Transactional(readOnly = true)
    public List<BankAccount> getAllBankAccounts() {
        return bankAccounts.findAll();
    }

    @Transactional(readOnly = true)
    public List<BankAccount> getBankAccountsByClient(Long clientId) {
        Client client = clients.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client not found."));
        return bankAccounts.findByClientAccountIdOrderByCreatedAtAscIdAsc(client.getAccount().getId());
    }

    @Transactional
    public void deleteEmployee(Long id) {
        accounts.delete(getEmployeeById(id));
    }

    @Transactional(readOnly = true)
    public List<BankTransaction> getAllTransactions() {
        return transactions.findAllByOrderByCreatedAtDescIdDesc();
    }

    @Transactional
    public void deleteTransaction(Long transactionId) {
        if (!transactions.existsById(transactionId)) {
            throw new IllegalArgumentException("Transaction not found.");
        }
        transactions.deleteById(transactionId);
    }

    private void validatePassword(String password) {
        if (isBlank(password) || password.length() < 6) {
            throw new IllegalArgumentException("Employee password must be at least 6 characters.");
        }
    }

    private Optional<Account> findAccount(String usernameOrEmail) {
        if (usernameOrEmail.contains("@")) {
            return accounts.findByEmailIgnoreCase(usernameOrEmail);
        }
        return accounts.findByUsernameIgnoreCase(usernameOrEmail);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
