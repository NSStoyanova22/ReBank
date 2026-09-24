package com.rebank.demo.repository;

import com.rebank.demo.model.Bank;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankRepository extends JpaRepository<Bank, Long> {

    Optional<Bank> findByCode(String code);
}
