package com.rebank.demo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rebank.demo.model.Account;
import com.rebank.demo.repository.AccountRepository;
import com.rebank.demo.repository.BankAccountRepository;
import com.rebank.demo.repository.BankRepository;
import com.rebank.demo.repository.BankTransactionRepository;
import com.rebank.demo.repository.ClientRepository;
import jakarta.servlet.http.HttpSession;
import java.util.Objects;
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
class AuthFlowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accounts;

    @Autowired
    private BankAccountRepository bankAccounts;

    @Autowired
    private ClientRepository clients;

    @Autowired
    private BankRepository banks;

    @Autowired
    private BankTransactionRepository transactions;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registrationFormFieldsCreateAccountAndSession() throws Exception {
        clearDatabase();

        MvcResult result = mockMvc.perform(post("/register")
                        .param("username", "vini")
                        .param("email", "VINI@example.com")
                        .param("password", "secret123")
                        .param("confirmPassword", "secret123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andReturn();

        Account account = accounts.findByUsernameIgnoreCase("vini").orElseThrow();
        HttpSession session = result.getRequest().getSession(false);

        org.junit.jupiter.api.Assertions.assertEquals("vini@example.com", account.getEmail());
        org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches("secret123", account.getPasswordHash()));
        org.junit.jupiter.api.Assertions.assertEquals(account.getId(), Objects.requireNonNull(session).getAttribute("accountId"));
        org.junit.jupiter.api.Assertions.assertEquals(1, bankAccounts.findByClientAccountIdOrderByCreatedAtAscIdAsc(account.getId()).size());
    }

    @Test
    void registrationErrorsPreserveMappedFields() throws Exception {
        clearDatabase();

        mockMvc.perform(post("/register")
                        .param("username", "vini")
                        .param("email", "vini@example.com")
                        .param("password", "secret123")
                        .param("confirmPassword", "different"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("vini")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("vini@example.com")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Passwords do not match.")));
    }

    @Test
    void accountCanLogInAndSeeAccountInfo() throws Exception {
        clearDatabase();
        accounts.save(new Account("vini", "vini@example.com", passwordEncoder.encode("secret123")));

        MvcResult result = mockMvc.perform(post("/login")
                        .param("usernameOrEmail", "vini")
                        .param("password", "secret123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andReturn();

        HttpSession session = result.getRequest().getSession(false);

        mockMvc.perform(get("/").session((MockHttpSession) Objects.requireNonNull(session)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Welcome back")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("vini")));
    }

    @Test
    void loginErrorsPreserveMappedField() throws Exception {
        clearDatabase();

        mockMvc.perform(post("/login")
                        .param("usernameOrEmail", "missing@example.com")
                        .param("password", "bad-password"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("missing@example.com")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Invalid username/email or password.")));
    }

    private void clearDatabase() {
        transactions.deleteAll();
        bankAccounts.deleteAll();
        clients.deleteAll();
        accounts.deleteAll();
        banks.deleteAll();
    }
}
