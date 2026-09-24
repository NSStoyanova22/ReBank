package com.rebank.demo.repository;

import com.rebank.demo.model.BankEmployee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankEmployeeRepository extends JpaRepository<BankEmployee, Long> {
}
