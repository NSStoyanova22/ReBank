package com.rebank.demo.repository;

import com.rebank.demo.model.BankTransaction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {

    List<BankTransaction> findByBankAccountIdOrderByCreatedAtDescIdDesc(Long bankAccountId);
}
