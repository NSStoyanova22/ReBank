package com.rebank.demo.controller;

import com.rebank.demo.model.BankAccount;
import com.rebank.demo.model.BankTransaction;
import com.rebank.demo.service.BankingService;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class BankController {

    private final BankingService banking;

    public BankController(BankingService banking) {
        this.banking = banking;
    }

    @PostMapping("/bank-accounts")
    @ResponseBody
    public ResponseEntity<String> openBankAccount(
            @RequestParam(required = false) String name,
            HttpSession session) {
        Long accountId = accountId(session);
        if (accountId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Login required.");
        }
        try {
            BankAccount bankAccount = banking.openBankAccount(accountId, name);
            return ResponseEntity.ok("Bank account opened: " + bankAccount.getAccountNumber());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @GetMapping("/bank-accounts")
    @ResponseBody
    public ResponseEntity<String> bankAccounts(HttpSession session) {
        Long accountId = accountId(session);
        if (accountId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Login required.");
        }
        List<BankAccount> bankAccounts = banking.bankAccountsFor(accountId);
        if (bankAccounts.isEmpty()) {
            return ResponseEntity.ok("No bank accounts yet.");
        }
        StringBuilder response = new StringBuilder();
        for (BankAccount bankAccount : bankAccounts) {
            response.append(bankAccount.getId())
                    .append(" | ")
                    .append(bankAccount.getAccountNumber())
                    .append(" | ")
                    .append(bankAccount.getName())
                    .append(" | balance ")
                    .append(bankAccount.getBalance())
                    .append(System.lineSeparator());
        }
        return ResponseEntity.ok(response.toString());
    }

    @PostMapping("/deposit")
    @ResponseBody
    public ResponseEntity<String> deposit(
            @RequestParam(required = false) Long bankAccountId,
            @RequestParam String amount,
            HttpSession session) {
        return runMoneyAction(session, () -> banking.deposit(accountId(session), bankAccountId, amount), "Deposit complete.");
    }

    @PostMapping("/withdraw")
    @ResponseBody
    public ResponseEntity<String> withdraw(
            @RequestParam(required = false) Long bankAccountId,
            @RequestParam String amount,
            HttpSession session) {
        return runMoneyAction(session, () -> banking.withdraw(accountId(session), bankAccountId, amount), "Withdrawal complete.");
    }

    @PostMapping("/transfer")
    @ResponseBody
    public ResponseEntity<String> transfer(
            @RequestParam(required = false) Long sourceBankAccountId,
            @RequestParam String recipientUsername,
            @RequestParam(required = false) Long recipientBankAccountId,
            @RequestParam String amount,
            HttpSession session) {
        return runMoneyAction(
                session,
                () -> banking.send(accountId(session), sourceBankAccountId, recipientUsername, recipientBankAccountId, amount),
                "Transfer complete.");
    }

    @GetMapping("/transactions")
    @ResponseBody
    public ResponseEntity<String> history(
            @RequestParam(required = false) Long bankAccountId,
            HttpSession session) {
        Long accountId = accountId(session);
        if (accountId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Login required.");
        }
        try {
            StringBuilder history = new StringBuilder();
            for (BankTransaction transaction : banking.transactionHistory(accountId, bankAccountId)) {
                history.append(transaction.getCreatedAt())
                        .append(" | ")
                        .append(transaction.getType())
                        .append(" | ")
                        .append(transaction.getDescription())
                        .append(" | ")
                        .append(transaction.getDirection() == BankTransaction.Direction.CREDIT ? "+" : "-")
                        .append(transaction.getAmount())
                        .append(" | balance ")
                        .append(transaction.getBalanceAfter())
                        .append(System.lineSeparator());
            }
            return ResponseEntity.ok(history.isEmpty() ? "No transactions yet." : history.toString());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    private ResponseEntity<String> runMoneyAction(
            HttpSession session,
            Runnable action,
            String successMessage) {
        if (accountId(session) == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Login required.");
        }
        try {
            action.run();
            return ResponseEntity.ok(successMessage);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    private Long accountId(HttpSession session) {
        return (Long) session.getAttribute("accountId");
    }
}
