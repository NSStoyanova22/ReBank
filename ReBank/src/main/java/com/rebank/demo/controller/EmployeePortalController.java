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

    private static final String EMPLOYEE_ID = "employeeId";

    private final BankEmployeeService employees;

    public EmployeePortalController(BankEmployeeService employees) {
        this.employees = employees;
    }

    @GetMapping("/employee-login")
    public String loginPage(HttpSession session, Model model) {
        if (session.getAttribute(EMPLOYEE_ID) != null) {
            return "redirect:/employee";
        }
        return "employee-login";
    }

    @PostMapping("/employee-login")
    public String login(
            @RequestParam Long employeeId,
            @RequestParam String password,
            HttpSession session,
            Model model) {
        try {
            employees.authenticate(employeeId, password);
            session.setAttribute(EMPLOYEE_ID, employeeId);
            return "redirect:/employee";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return "employee-login";
        }
    }

    @GetMapping("/employee")
    public String dashboard(HttpSession session, Model model) {
        Long employeeId = employeeId(session);
        if (employeeId == null) {
            return "redirect:/employee-login";
        }

        try {
            model.addAttribute("employee", employees.getEmployeeById(employeeId));
            model.addAttribute("transactions", employees.getAllTransactions());
            return "employee";
        } catch (IllegalArgumentException ex) {
            session.removeAttribute(EMPLOYEE_ID);
            return "redirect:/employee-login";
        }
    }

    @PostMapping("/employee/transactions/{transactionId}/delete")
    public String deleteTransaction(
            @PathVariable Long transactionId,
            HttpSession session,
            Model model) {
        if (employeeId(session) == null) {
            return "redirect:/employee-login";
        }

        try {
            employees.deleteTransaction(transactionId);
            return "redirect:/employee";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("employee", employees.getEmployeeById(employeeId(session)));
            model.addAttribute("transactions", employees.getAllTransactions());
            return "employee";
        }
    }

    @PostMapping("/employee/logout")
    public String logout(HttpSession session) {
        session.removeAttribute(EMPLOYEE_ID);
        return "redirect:/employee-login";
    }

    private Long employeeId(HttpSession session) {
        Object employeeId = session.getAttribute(EMPLOYEE_ID);
        return employeeId instanceof Long ? (Long) employeeId : null;
    }
}
