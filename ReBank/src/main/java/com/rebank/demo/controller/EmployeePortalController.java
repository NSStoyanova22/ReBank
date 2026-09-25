package com.rebank.demo.controller;

import com.rebank.demo.service.BankEmployeeService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class EmployeePortalController {

    private static final String EMPLOYEE_ACCOUNT_ID = "employeeAccountId";

    private final BankEmployeeService employees;

    public EmployeePortalController(BankEmployeeService employees) {
        this.employees = employees;
    }

    @GetMapping("/employee-login")
    public String loginPage(HttpSession session, Model model) {
        if (session.getAttribute(EMPLOYEE_ACCOUNT_ID) != null) {
            return "redirect:/employee";
        }
        return "employee-login";
    }

    @PostMapping("/employee-login")
    public String login(
            @RequestParam String usernameOrEmail,
            @RequestParam String password,
            HttpSession session,
            Model model) {
        model.addAttribute("usernameOrEmail", usernameOrEmail);
        try {
            session.setAttribute(EMPLOYEE_ACCOUNT_ID, employees.authenticate(usernameOrEmail, password).getId());
            return "redirect:/employee";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return "employee-login";
        }
    }

    @GetMapping("/employee")
    public String dashboard(HttpSession session, Model model) {
        Long employeeAccountId = employeeAccountId(session);
        if (employeeAccountId == null) {
            return "redirect:/employee-login";
        }

        try {
            model.addAttribute("employee", employees.getEmployeeById(employeeAccountId));
            model.addAttribute("transactions", employees.getAllTransactions());
            return "employee";
        } catch (IllegalArgumentException ex) {
            session.removeAttribute(EMPLOYEE_ACCOUNT_ID);
            return "redirect:/employee-login";
        }
    }

    @PostMapping("/employee/transactions/{transactionId}/delete")
    public String deleteTransaction(
            @PathVariable Long transactionId,
            HttpSession session,
            Model model) {
        Long employeeAccountId = employeeAccountId(session);
        if (employeeAccountId == null) {
            return "redirect:/employee-login";
        }

        try {
            employees.deleteTransaction(transactionId);
            return "redirect:/employee";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("employee", employees.getEmployeeById(employeeAccountId));
            model.addAttribute("transactions", employees.getAllTransactions());
            return "employee";
        }
    }

    @PostMapping("/employee/logout")
    public String logout(HttpSession session) {
        session.removeAttribute(EMPLOYEE_ACCOUNT_ID);
        return "redirect:/employee-login";
    }

    private Long employeeAccountId(HttpSession session) {
        Object employeeAccountId = session.getAttribute(EMPLOYEE_ACCOUNT_ID);
        return employeeAccountId instanceof Long ? (Long) employeeAccountId : null;
    }
}
