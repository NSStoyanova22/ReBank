package com.rebank.demo.controller;

import com.rebank.demo.model.BankAccount;
import com.rebank.demo.repository.AccountRepository;
import com.rebank.demo.repository.BankAccountRepository;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final AccountRepository accounts;
    private final BankAccountRepository bankAccounts;

    public HomeController(AccountRepository accounts, BankAccountRepository bankAccounts) {
        this.accounts = accounts;
        this.bankAccounts = bankAccounts;
    }

    @GetMapping("/")
    public String index(HttpSession session, Model model) {
        Long accountId = (Long) session.getAttribute("accountId");
        if (accountId != null) {
            accounts.findById(accountId).ifPresent(account -> {
                model.addAttribute("account", account);
                BankAccount bankAccount = bankAccounts
                        .findFirstByClientAccountIdOrderByCreatedAtAscIdAsc(accountId)
                        .orElse(null);
                model.addAttribute("bankAccount", bankAccount);
                model.addAttribute("balance", bankAccount == null ? BigDecimal.ZERO : bankAccount.getBalance());
            });
        }
        return "index";
    }
}
