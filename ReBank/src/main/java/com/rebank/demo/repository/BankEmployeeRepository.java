package com.rebank.demo.repository;

import com.rebank.demo.model.BankEmployee;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankEmployeeRepository extends JpaRepository<BankEmployee, Long> {

	Optional<BankEmployee> findById(Long id);
}
