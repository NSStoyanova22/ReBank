package com.rebank.demo.service;

import com.rebank.demo.model.Bank;
import com.rebank.demo.model.BankAccount;
import com.rebank.demo.model.BankEmployee;
import com.rebank.demo.model.BankTransaction;
import com.rebank.demo.model.Client;
import com.rebank.demo.repository.BankAccountRepository;
import com.rebank.demo.repository.BankEmployeeRepository;
import com.rebank.demo.repository.BankRepository;
import com.rebank.demo.repository.BankTransactionRepository;
import com.rebank.demo.repository.ClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Service
public class BankEmployeeService {

    private final BankEmployeeRepository bankEmployees;
    private final BankRepository banks;
    private final ClientRepository clients;
    private final BankAccountRepository bankAccounts;
    private final BankTransactionRepository transactions;
    private final PasswordEncoder passwordEncoder;

    public BankEmployeeService(
            BankEmployeeRepository bankEmployees,
            BankRepository banks,
            ClientRepository clients,
            BankAccountRepository bankAccounts,
            BankTransactionRepository transactions,
            PasswordEncoder passwordEncoder) {
        this.bankEmployees = bankEmployees;
        this.banks = banks;
        this.clients = clients;
        this.bankAccounts = bankAccounts;
        this.transactions = transactions;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public BankEmployee createEmployee(Long bankId, String name, String role) {
        if (isBlank(name)) {
            throw new IllegalArgumentException("Employee name is required.");
        }
        if (isBlank(role)) {
            throw new IllegalArgumentException("Employee role is required.");
        }
        
        Bank bank = banks.findById(bankId)
                .orElseThrow(() -> new IllegalArgumentException("Bank not found."));
        
        String employeeName = name.trim();
        String employeeRole = role.trim();
        
        if (employeeName.length() > 120) {
            throw new IllegalArgumentException("Employee name is too long.");
        }
        if (employeeRole.length() > 80) {
            throw new IllegalArgumentException("Employee role is too long.");
        }
        
        return bankEmployees.save(new BankEmployee(bank, employeeName, employeeRole));
    }

    @Transactional
    public BankEmployee createEmployee(Long bankId, String name, String role, String password) {
        validatePassword(password);
        if (isBlank(name)) {
            throw new IllegalArgumentException("Employee name is required.");
        }
        if (isBlank(role)) {
            throw new IllegalArgumentException("Employee role is required.");
        }

        Bank bank = banks.findById(bankId)
                .orElseThrow(() -> new IllegalArgumentException("Bank not found."));
        String employeeName = name.trim();
        String employeeRole = role.trim();
        if (employeeName.length() > 120) {
            throw new IllegalArgumentException("Employee name is too long.");
        }
        if (employeeRole.length() > 80) {
            throw new IllegalArgumentException("Employee role is too long.");
        }

        return bankEmployees.save(new BankEmployee(
                bank,
                employeeName,
                employeeRole,
                passwordEncoder.encode(password)));
    }

    @Transactional(readOnly = true)
    public BankEmployee authenticate(Long employeeId, String password) {
        BankEmployee employee = getEmployeeById(employeeId);
        if (employee.getPasswordHash() == null
                || isBlank(password)
                || !passwordEncoder.matches(password, employee.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid employee credentials.");
        }
        return employee;
    }

    @Transactional(readOnly = true)
    public List<BankEmployee> getAllEmployees() {
        return bankEmployees.findAll();
    }

    @Transactional(readOnly = true)
    public BankEmployee getEmployeeById(Long id) {
        return bankEmployees.findById(id)
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
        if (!bankEmployees.existsById(id)) {
            throw new IllegalArgumentException("Employee not found.");
        }
        bankEmployees.deleteById(id);
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

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
