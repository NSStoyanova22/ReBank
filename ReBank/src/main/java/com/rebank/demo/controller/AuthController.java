package com.rebank.demo.controller;

import com.rebank.demo.model.Account;
import com.rebank.demo.repository.AccountRepository;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import java.util.Optional;
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

    @GetMapping("/login")
    public String login() {
        return "login";
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

    @PostMapping("/login")
    public String login(
            @RequestParam String usernameOrEmail,
            @RequestParam String password,
            HttpSession session,
            Model model) {
        model.addAttribute("usernameOrEmail", usernameOrEmail);

        if (isBlank(usernameOrEmail) || isBlank(password)) {
            model.addAttribute("error", "Username/email and password are required.");
            return "login";
        }

        Optional<Account> account = findAccount(usernameOrEmail.trim());
        if (account.isEmpty() || !passwordEncoder.matches(password, account.get().getPasswordHash())) {
            model.addAttribute("error", "Invalid username/email or password.");
            return "login";
        }

        session.setAttribute("accountId", account.get().getId());
        return "redirect:/";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    private Optional<Account> findAccount(String usernameOrEmail) {
        if (usernameOrEmail.contains("@")) {
            return accounts.findByEmailIgnoreCase(usernameOrEmail);
        }
        return accounts.findByUsernameIgnoreCase(usernameOrEmail);
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
