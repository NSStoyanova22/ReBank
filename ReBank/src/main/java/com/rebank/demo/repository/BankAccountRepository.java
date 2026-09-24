package com.rebank.demo.repository;

import com.rebank.demo.model.BankAccount;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    boolean existsByAccountNumber(String accountNumber);

    List<BankAccount> findByClientAccountIdOrderByCreatedAtAscIdAsc(Long accountId);

    Optional<BankAccount> findFirstByClientAccountIdOrderByCreatedAtAscIdAsc(Long accountId);

    Optional<BankAccount> findByIdAndClientAccountId(Long id, Long accountId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from BankAccount a where a.id = :id")
    Optional<BankAccount> findByIdForUpdate(@Param("id") Long id);
}
