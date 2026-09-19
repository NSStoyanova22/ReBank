package com.rebank.demo.web;

import com.rebank.demo.account.Account;
import com.rebank.demo.account.AccountRepository;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final AccountRepository accounts;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AccountRepository accounts, PasswordEncoder passwordEncoder) {
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @PostMapping("/register")
    public String register(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            HttpSession session,
            Model model) {
        Map<String, String> values = Map.of(
                "username", username,
                "email", email);
        model.addAllAttributes(values);

        String error = registrationError(username, email, password, confirmPassword);
        if (error != null) {
            model.addAttribute("error", error);
            return "register";
        }

        Account account = accounts.save(new Account(
                username.trim(),
                email.trim().toLowerCase(),
                passwordEncoder.encode(password)));
        session.setAttribute("accountId", account.getId());
        return "redirect:/";
    }

    private String registrationError(String username, String email, String password, String confirmPassword) {
        if (isBlank(username) || isBlank(email) || isBlank(password) || isBlank(confirmPassword)) {
            return "All fields are required.";
        }
        if (password.length() < 6) {
            return "Password must be at least 6 characters.";
        }
        if (!password.equals(confirmPassword)) {
            return "Passwords do not match.";
        }
        if (accounts.existsByUsernameIgnoreCase(username.trim())) {
            return "Username is already taken.";
        }
        if (accounts.existsByEmailIgnoreCase(email.trim())) {
            return "Email is already registered.";
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
