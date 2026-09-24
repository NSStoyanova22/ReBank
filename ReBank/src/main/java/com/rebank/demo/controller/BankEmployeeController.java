package com.rebank.demo.controller;

import com.rebank.demo.model.BankAccount;
import com.rebank.demo.model.BankEmployee;
import com.rebank.demo.model.Client;
import com.rebank.demo.service.BankEmployeeService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class BankEmployeeController {

    private final BankEmployeeService bankEmployeeService;

    public BankEmployeeController(BankEmployeeService bankEmployeeService) {
        this.bankEmployeeService = bankEmployeeService;
    }

    @PostMapping("/employees")
    @ResponseBody
    public ResponseEntity<String> createEmployee(
            @RequestParam Long bankId,
            @RequestParam String name,
            @RequestParam String role,
            @RequestParam String password) {
        try {
            BankEmployee employee = bankEmployeeService.createEmployee(bankId, name, role, password);
            return ResponseEntity.ok("Employee created with ID: " + employee.getId());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @GetMapping("/employees")
    @ResponseBody
    public ResponseEntity<String> getAllEmployees() {
        List<BankEmployee> employees = bankEmployeeService.getAllEmployees();
        if (employees.isEmpty()) {
            return ResponseEntity.ok("No employees found.");
        }
        StringBuilder response = new StringBuilder();
        for (BankEmployee employee : employees) {
            response.append("ID: ")
                    .append(employee.getId())
                    .append(" | Name: ")
                    .append(employee.getName())
                    .append(" | Role: ")
                    .append(employee.getRole())
                    .append(" | Bank: ")
                    .append(employee.getBank().getName())
                    .append(System.lineSeparator());
        }
        return ResponseEntity.ok(response.toString());
    }

    @GetMapping("/employees/{id}")
    @ResponseBody
    public ResponseEntity<String> getEmployee(@PathVariable Long id) {
        try {
            BankEmployee employee = bankEmployeeService.getEmployeeById(id);
            return ResponseEntity.ok(
                    "ID: " + employee.getId() +
                    " | Name: " + employee.getName() +
                    " | Role: " + employee.getRole() +
                    " | Bank: " + employee.getBank().getName());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @DeleteMapping("/employees/{id}")
    @ResponseBody
    public ResponseEntity<String> deleteEmployee(@PathVariable Long id) {
        try {
            bankEmployeeService.deleteEmployee(id);
            return ResponseEntity.ok("Employee deleted successfully.");
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @GetMapping("/clients")
    @ResponseBody
    public ResponseEntity<String> getAllClients() {
        List<Client> clients = bankEmployeeService.getAllClients();
        if (clients.isEmpty()) {
            return ResponseEntity.ok("No clients found.");
        }
        StringBuilder response = new StringBuilder();
        for (Client client : clients) {
            response.append("ID: ")
                    .append(client.getId())
                    .append(" | Name: ")
                    .append(client.getDisplayName())
                    .append(" | Username: ")
                    .append(client.getAccount().getUsername())
                    .append(System.lineSeparator());
        }
        return ResponseEntity.ok(response.toString());
    }

    @GetMapping("/bank-accounts")
    @ResponseBody
    public ResponseEntity<String> getAllBankAccounts() {
        List<BankAccount> accounts = bankEmployeeService.getAllBankAccounts();
        if (accounts.isEmpty()) {
            return ResponseEntity.ok("No bank accounts found.");
        }
        StringBuilder response = new StringBuilder();
        for (BankAccount account : accounts) {
            response.append("ID: ")
                    .append(account.getId())
                    .append(" | Account #: ")
                    .append(account.getAccountNumber())
                    .append(" | Name: ")
                    .append(account.getName())
                    .append(" | Balance: ")
                    .append(account.getBalance())
                    .append(" | Client: ")
                    .append(account.getClient().getDisplayName())
                    .append(System.lineSeparator());
        }
        return ResponseEntity.ok(response.toString());
    }

    @GetMapping("/clients/{clientId}/bank-accounts")
    @ResponseBody
    public ResponseEntity<String> getBankAccountsByClient(@PathVariable Long clientId) {
        try {
            List<BankAccount> accounts = bankEmployeeService.getBankAccountsByClient(clientId);
            if (accounts.isEmpty()) {
                return ResponseEntity.ok("No bank accounts found for this client.");
            }
            StringBuilder response = new StringBuilder();
            for (BankAccount account : accounts) {
                response.append("ID: ")
                        .append(account.getId())
                        .append(" | Account #: ")
                        .append(account.getAccountNumber())
                        .append(" | Name: ")
                        .append(account.getName())
                        .append(" | Balance: ")
                        .append(account.getBalance())
                        .append(System.lineSeparator());
            }
            return ResponseEntity.ok(response.toString());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }
}
