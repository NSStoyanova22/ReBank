package com.rebank.demo.service;

import com.rebank.demo.model.Bank;
import com.rebank.demo.model.BankAccount;
import com.rebank.demo.model.BankEmployee;
import com.rebank.demo.model.Client;
import com.rebank.demo.repository.BankAccountRepository;
import com.rebank.demo.repository.BankEmployeeRepository;
import com.rebank.demo.repository.BankRepository;
import com.rebank.demo.repository.ClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BankEmployeeService {

    private final BankEmployeeRepository bankEmployees;
    private final BankRepository banks;
    private final ClientRepository clients;
    private final BankAccountRepository bankAccounts;

    public BankEmployeeService(
            BankEmployeeRepository bankEmployees,
            BankRepository banks,
            ClientRepository clients,
            BankAccountRepository bankAccounts) {
        this.bankEmployees = bankEmployees;
        this.banks = banks;
        this.clients = clients;
        this.bankAccounts = bankAccounts;
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

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
