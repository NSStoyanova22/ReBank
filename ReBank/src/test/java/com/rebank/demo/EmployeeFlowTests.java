package com.rebank.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rebank.demo.model.Account;
import com.rebank.demo.model.Account.AccountType;
import com.rebank.demo.model.Bank;
import com.rebank.demo.model.BankTransaction;
import com.rebank.demo.repository.AccountRepository;
import com.rebank.demo.repository.BankAccountRepository;
import com.rebank.demo.repository.BankRepository;
import com.rebank.demo.repository.BankTransactionRepository;
import com.rebank.demo.repository.ClientRepository;
import com.rebank.demo.service.BankEmployeeService;
import jakarta.servlet.http.HttpSession;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class EmployeeFlowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accounts;

    @Autowired
    private BankAccountRepository bankAccounts;

    @Autowired
    private BankRepository banks;

    @Autowired
    private BankTransactionRepository transactions;

    @Autowired
    private ClientRepository clients;

    @Autowired
    private BankEmployeeService employeeService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void resetDatabase() {
        transactions.deleteAll();
        bankAccounts.deleteAll();
        clients.deleteAll();
        accounts.deleteAll();
        banks.deleteAll();
    }

    @Test
    void customerSessionCannotOpenEmployeeConsoleOrDeleteTransaction() throws Exception {
        Account customer = accounts.save(new Account("customer", "customer@example.com", passwordEncoder.encode("secret123")));
        MockHttpSession customerSession = new MockHttpSession();
        customerSession.setAttribute("accountId", customer.getId());

        mockMvc.perform(get("/employee").session(customerSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee-login"));
    }

    @Test
    void employeeCanLogInViewConsoleAndRemoveTransaction() throws Exception {
        Bank bank = banks.save(new Bank("STAFF", "Staff Bank"));
        Account employee = employeeService.createEmployee(
                bank.getId(),
                "jordan",
                "jordan@example.com",
                "Operations",
                "staffpass");
        Account customer = accounts.save(new Account("customer", "customer@example.com", passwordEncoder.encode("secret123")));
        MockHttpSession customerSession = new MockHttpSession();
        customerSession.setAttribute("accountId", customer.getId());

        mockMvc.perform(post("/deposit").session(customerSession).param("amount", "25.00"))
                .andExpect(status().isOk());
        BankTransaction transaction = transactions.findAll().get(0);

        MvcResult login = mockMvc.perform(post("/employee-login")
                        .param("usernameOrEmail", "jordan")
                        .param("password", "staffpass"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee"))
                .andReturn();
        HttpSession employeeSession = login.getRequest().getSession(false);

        mockMvc.perform(get("/employee").session((MockHttpSession) Objects.requireNonNull(employeeSession)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Account activity")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Deposit")));

        mockMvc.perform(post("/employee/transactions/" + transaction.getId() + "/delete")
                        .session((MockHttpSession) employeeSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee"));

        assertEquals(0, transactions.count());
    }

    @Test
    void employeeAccountCannotUseCustomerBanking() throws Exception {
        Bank bank = banks.save(new Bank("STAFF", "Staff Bank"));
        Account employee = employeeService.createEmployee(
                bank.getId(),
                "jordan",
                "jordan@example.com",
                "Operations",
                "staffpass");
        MockHttpSession employeeSession = new MockHttpSession();
        employeeSession.setAttribute("accountId", employee.getId());

        mockMvc.perform(post("/deposit").session(employeeSession).param("amount", "25.00"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Employee accounts cannot use customer banking."));

        assertEquals(0, clients.count());
        assertEquals(AccountType.EMPLOYEE, accounts.findById(employee.getId()).orElseThrow().getAccountType());
    }

    @Test
    void adminEmployeeListOnlyReturnsEmployeeAccounts() throws Exception {
        Bank bank = banks.save(new Bank("STAFF", "Staff Bank"));
        accounts.save(new Account("customer", "customer@example.com", passwordEncoder.encode("secret123")));

        mockMvc.perform(post("/admin/employees")
                        .param("bankId", bank.getId().toString())
                        .param("username", "jordan")
                        .param("email", "jordan@example.com")
                        .param("role", "Operations")
                        .param("password", "staffpass"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Employee created with ID: ")));

        mockMvc.perform(get("/admin/employees"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Username: jordan")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("customer"))));
    }
}
