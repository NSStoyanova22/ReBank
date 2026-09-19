package com.rebank.demo.web;

import com.rebank.demo.account.AccountRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final AccountRepository accounts;

    public HomeController(AccountRepository accounts) {
        this.accounts = accounts;
    }

    @GetMapping("/")
    public String index(HttpSession session, Model model) {
        Long accountId = (Long) session.getAttribute("accountId");
        if (accountId != null) {
            accounts.findById(accountId).ifPresent(account -> model.addAttribute("account", account));
        }
        return "index";
    }
}
