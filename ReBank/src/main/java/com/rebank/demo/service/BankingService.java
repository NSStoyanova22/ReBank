package com.rebank.demo.service;

import com.rebank.demo.model.Account;
import com.rebank.demo.model.Bank;
import com.rebank.demo.model.BankAccount;
import com.rebank.demo.model.BankTransaction;
import com.rebank.demo.model.Client;
import com.rebank.demo.repository.AccountRepository;
import com.rebank.demo.repository.BankAccountRepository;
import com.rebank.demo.repository.BankRepository;
import com.rebank.demo.repository.BankTransactionRepository;
import com.rebank.demo.repository.ClientRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BankingService {

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000000.00");
    private static final String DEFAULT_BANK_CODE = "REBANK";

    private final AccountRepository accounts;
    private final ClientRepository clients;
    private final BankRepository banks;
    private final BankAccountRepository bankAccounts;
    private final BankTransactionRepository transactions;

    public BankingService(
            AccountRepository accounts,
            ClientRepository clients,
            BankRepository banks,
            BankAccountRepository bankAccounts,
            BankTransactionRepository transactions) {
        this.accounts = accounts;
        this.clients = clients;
        this.banks = banks;
        this.bankAccounts = bankAccounts;
        this.transactions = transactions;
    }

    @Transactional
    public BankAccount openBankAccount(Long accountId, String name) {
        Client client = clientFor(accountId);
        Bank bank = defaultBank();
        String accountName = isBlank(name) ? "Main account" : name.trim();
        if (accountName.length() > 80) {
            throw new IllegalArgumentException("Account name is too long.");
        }
        return bankAccounts.save(new BankAccount(client, bank, uniqueAccountNumber(), accountName));
    }

    @Transactional(readOnly = true)
    public List<BankAccount> bankAccountsFor(Long accountId) {
        requireAccount(accountId);
        return bankAccounts.findByClientAccountIdOrderByCreatedAtAscIdAsc(accountId);
    }

    @Transactional
    public void deposit(Long accountId, Long bankAccountId, String rawAmount) {
        BigDecimal amount = parseAmount(rawAmount);
        BankAccount bankAccount = lockedOwnedBankAccount(accountId, bankAccountId);

        bankAccount.credit(amount);
        transactions.save(new BankTransaction(
                bankAccount,
                null,
                BankTransaction.Type.DEPOSIT,
                BankTransaction.Direction.CREDIT,
                amount,
                bankAccount.getBalance(),
                "Deposit"));
    }

    @Transactional
    public void withdraw(Long accountId, Long bankAccountId, String rawAmount) {
        BigDecimal amount = parseAmount(rawAmount);
        BankAccount bankAccount = lockedOwnedBankAccount(accountId, bankAccountId);

        requireFunds(bankAccount, amount);
        bankAccount.debit(amount);
        transactions.save(new BankTransaction(
                bankAccount,
                null,
                BankTransaction.Type.WITHDRAWAL,
                BankTransaction.Direction.DEBIT,
                amount,
                bankAccount.getBalance(),
                "Withdrawal"));
    }

    @Transactional
    public void send(
            Long senderId,
            Long sourceBankAccountId,
            String recipientUsername,
            Long recipientBankAccountId,
            String rawAmount) {
        if (isBlank(recipientUsername)) {
            throw new IllegalArgumentException("Recipient username is required.");
        }

        BigDecimal amount = parseAmount(rawAmount);
        Account recipient = accounts.findByUsernameIgnoreCase(recipientUsername.trim())
                .orElseThrow(() -> new IllegalArgumentException("Recipient account was not found."));
        if (recipient.getId().equals(senderId)) {
            throw new IllegalArgumentException("You cannot send money to yourself.");
        }

        Long senderAccountId = ownedBankAccountId(senderId, sourceBankAccountId);
        Long recipientAccountId = ownedBankAccountId(recipient.getId(), recipientBankAccountId);
        List<BankAccount> locked = List.of(senderAccountId, recipientAccountId).stream()
                .distinct()
                .sorted()
                .map(this::lockedAccount)
                .sorted(Comparator.comparing(BankAccount::getId))
                .toList();
        BankAccount sender = locked.stream()
                .filter(account -> account.getId().equals(senderAccountId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Bank account was not found."));
        BankAccount lockedRecipient = locked.stream()
                .filter(account -> account.getId().equals(recipientAccountId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Recipient bank account was not found."));

        requireFunds(sender, amount);
        sender.debit(amount);
        lockedRecipient.credit(amount);

        transactions.save(new BankTransaction(
                sender,
                lockedRecipient,
                BankTransaction.Type.TRANSFER,
                BankTransaction.Direction.DEBIT,
                amount,
                sender.getBalance(),
                "Sent to " + recipient.getUsername()));
        transactions.save(new BankTransaction(
                lockedRecipient,
                sender,
                BankTransaction.Type.TRANSFER,
                BankTransaction.Direction.CREDIT,
                amount,
                lockedRecipient.getBalance(),
                "Received from " + sender.getClient().getAccount().getUsername()));
    }

    @Transactional(readOnly = true)
    public List<BankTransaction> transactionHistory(Long accountId, Long bankAccountId) {
        Optional<Long> ownedBankAccountId = existingOwnedBankAccountId(accountId, bankAccountId);
        return ownedBankAccountId
                .map(transactions::findByBankAccountIdOrderByCreatedAtDescIdDesc)
                .orElseGet(List::of);
    }

    private Client clientFor(Long accountId) {
        return clients.findByAccountId(accountId)
                .orElseGet(() -> {
                    Account account = requireAccount(accountId);
                    return clients.save(new Client(account, account.getUsername()));
                });
    }

    private Bank defaultBank() {
        return banks.findByCode(DEFAULT_BANK_CODE)
                .orElseGet(() -> banks.save(new Bank(DEFAULT_BANK_CODE, "ReBank")));
    }

    private Account requireAccount(Long accountId) {
        return accounts.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account was not found."));
    }

    private Long ownedBankAccountId(Long accountId, Long bankAccountId) {
        if (bankAccountId == null) {
            return bankAccounts.findFirstByClientAccountIdOrderByCreatedAtAscIdAsc(accountId)
                    .orElseGet(() -> openBankAccount(accountId, "Main account"))
                    .getId();
        }
        return bankAccounts.findByIdAndClientAccountId(bankAccountId, accountId)
                .orElseThrow(() -> new IllegalArgumentException("Bank account was not found."))
                .getId();
    }

    private Optional<Long> existingOwnedBankAccountId(Long accountId, Long bankAccountId) {
        requireAccount(accountId);
        if (bankAccountId == null) {
            return bankAccounts.findFirstByClientAccountIdOrderByCreatedAtAscIdAsc(accountId)
                    .map(BankAccount::getId);
        }
        return Optional.of(bankAccounts.findByIdAndClientAccountId(bankAccountId, accountId)
                .orElseThrow(() -> new IllegalArgumentException("Bank account was not found."))
                .getId());
    }

    private BankAccount lockedOwnedBankAccount(Long accountId, Long bankAccountId) {
        return lockedAccount(ownedBankAccountId(accountId, bankAccountId));
    }

    private BankAccount lockedAccount(Long bankAccountId) {
        return bankAccounts.findByIdForUpdate(bankAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Bank account was not found."));
    }

    private String uniqueAccountNumber() {
        String accountNumber;
        do {
            accountNumber = "RB" + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase();
        } while (bankAccounts.existsByAccountNumber(accountNumber));
        return accountNumber;
    }

    private void requireFunds(BankAccount account, BigDecimal amount) {
        if (account.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds.");
        }
    }

    private BigDecimal parseAmount(String rawAmount) {
        if (isBlank(rawAmount)) {
            throw new IllegalArgumentException("Amount is required.");
        }
        try {
            BigDecimal amount = new BigDecimal(rawAmount.trim()).setScale(2, RoundingMode.UNNECESSARY);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Amount must be greater than zero.");
            }
            if (amount.compareTo(MAX_AMOUNT) > 0) {
                throw new IllegalArgumentException("Amount is too large.");
            }
            return amount;
        } catch (ArithmeticException | NumberFormatException ex) {
            throw new IllegalArgumentException("Amount must use dollars and cents.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
