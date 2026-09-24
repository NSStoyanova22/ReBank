package com.rebank.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rebank.demo.model.Account;
import com.rebank.demo.model.Bank;
import com.rebank.demo.model.BankEmployee;
import com.rebank.demo.model.BankTransaction;
import com.rebank.demo.repository.AccountRepository;
import com.rebank.demo.repository.BankAccountRepository;
import com.rebank.demo.repository.BankEmployeeRepository;
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
    private BankEmployeeRepository employees;

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
        employees.deleteAll();
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
        BankEmployee employee = employeeService.createEmployee(bank.getId(), "Jordan Staff", "Operations", "staffpass");
        Account customer = accounts.save(new Account("customer", "customer@example.com", passwordEncoder.encode("secret123")));
        MockHttpSession customerSession = new MockHttpSession();
        customerSession.setAttribute("accountId", customer.getId());

        mockMvc.perform(post("/deposit").session(customerSession).param("amount", "25.00"))
                .andExpect(status().isOk());
        BankTransaction transaction = transactions.findAll().get(0);

        MvcResult login = mockMvc.perform(post("/employee-login")
                        .param("employeeId", employee.getId().toString())
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
}
