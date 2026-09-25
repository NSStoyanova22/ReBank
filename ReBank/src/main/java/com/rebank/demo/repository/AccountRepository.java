package com.rebank.demo.repository;

import com.rebank.demo.model.Account;
import com.rebank.demo.model.Account.AccountType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByUsernameIgnoreCase(String username);

    Optional<Account> findByEmailIgnoreCase(String email);

    Optional<Account> findByIdAndAccountType(Long id, AccountType accountType);

    List<Account> findByAccountTypeOrderByCreatedAtAscIdAsc(AccountType accountType);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);
}
