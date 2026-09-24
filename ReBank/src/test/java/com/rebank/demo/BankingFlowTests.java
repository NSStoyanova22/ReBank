package com.rebank.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rebank.demo.model.Account;
import com.rebank.demo.model.BankAccount;
import com.rebank.demo.repository.BankTransactionRepository;
import com.rebank.demo.repository.AccountRepository;
import com.rebank.demo.repository.BankAccountRepository;
import com.rebank.demo.repository.BankRepository;
import com.rebank.demo.repository.ClientRepository;
import java.math.BigDecimal;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class BankingFlowTests {

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

    @BeforeEach
    void resetDatabase() {
        transactions.deleteAll();
        bankAccounts.deleteAll();
        clients.deleteAll();
        accounts.deleteAll();
        banks.deleteAll();
    }

    @Test
    void accountCanDepositWithdrawTransferAndSeeHistory() throws Exception {
        Account sender = accounts.save(new Account("vini", "vini@example.com", passwordEncoder.encode("secret123")));
        Account recipient = accounts.save(new Account("alex", "alex@example.com", passwordEncoder.encode("secret123")));
        MockHttpSession session = sessionFor(sender);

        mockMvc.perform(post("/deposit").session(session).param("amount", "100.00"))
                .andExpect(status().isOk())
                .andExpect(content().string("Deposit complete."));
        mockMvc.perform(post("/withdraw").session(session).param("amount", "40.00"))
                .andExpect(status().isOk())
                .andExpect(content().string("Withdrawal complete."));
        mockMvc.perform(post("/transfer").session(session)
                        .param("recipientUsername", "alex")
                        .param("amount", "25.50"))
                .andExpect(status().isOk())
                .andExpect(content().string("Transfer complete."));

        BankAccount senderBankAccount = primaryBankAccount(sender);
        BankAccount recipientBankAccount = primaryBankAccount(recipient);
        assertEquals(new BigDecimal("34.50"), senderBankAccount.getBalance());
        assertEquals(new BigDecimal("25.50"), recipientBankAccount.getBalance());
        assertEquals(4, transactions.count());

        mockMvc.perform(get("/transactions").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Sent to alex")))
                .andExpect(content().string(Matchers.containsString("-25.50")))
                .andExpect(content().string(Matchers.containsString("balance 34.50")));
    }

    @Test
    void clientCanOpenMultipleBankAccountsAndDepositIntoSelectedAccount() throws Exception {
        Account account = accounts.save(new Account("vini", "vini@example.com", passwordEncoder.encode("secret123")));
        MockHttpSession session = sessionFor(account);

        mockMvc.perform(post("/bank-accounts").session(session).param("name", "Everyday"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Bank account opened: RB")));
        mockMvc.perform(post("/bank-accounts").session(session).param("name", "Savings"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Bank account opened: RB")));

        List<BankAccount> openedAccounts = bankAccounts.findByClientAccountIdOrderByCreatedAtAscIdAsc(account.getId());
        assertEquals(2, openedAccounts.size());

        mockMvc.perform(post("/deposit")
                        .session(session)
                        .param("bankAccountId", openedAccounts.get(1).getId().toString())
                        .param("amount", "75.00"))
                .andExpect(status().isOk())
                .andExpect(content().string("Deposit complete."));

        assertEquals(new BigDecimal("0.00"), bankAccounts.findById(openedAccounts.get(0).getId()).orElseThrow().getBalance());
        assertEquals(new BigDecimal("75.00"), bankAccounts.findById(openedAccounts.get(1).getId()).orElseThrow().getBalance());
    }

    @Test
    void withdrawalRejectsInsufficientFundsWithoutLedgerEntry() throws Exception {
        Account account = accounts.save(new Account("vini", "vini@example.com", passwordEncoder.encode("secret123")));

        mockMvc.perform(post("/withdraw").session(sessionFor(account)).param("amount", "1.00"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Insufficient funds."));

        assertTrue(bankAccounts.findByClientAccountIdOrderByCreatedAtAscIdAsc(account.getId()).isEmpty());
        assertEquals(0, transactions.count());
    }

    @Test
    void depositRejectsAmountsWithMoreThanTwoDecimals() throws Exception {
        Account account = accounts.save(new Account("vini", "vini@example.com", passwordEncoder.encode("secret123")));

        mockMvc.perform(post("/deposit").session(sessionFor(account)).param("amount", "10.999"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Amount must use dollars and cents."));

        assertTrue(bankAccounts.findByClientAccountIdOrderByCreatedAtAscIdAsc(account.getId()).isEmpty());
        assertEquals(0, transactions.count());
    }

    private MockHttpSession sessionFor(Account account) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("accountId", account.getId());
        return session;
    }

    private BankAccount primaryBankAccount(Account account) {
        return bankAccounts.findFirstByClientAccountIdOrderByCreatedAtAscIdAsc(account.getId()).orElseThrow();
    }
}
