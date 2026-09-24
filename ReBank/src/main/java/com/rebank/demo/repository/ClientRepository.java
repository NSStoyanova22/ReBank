package com.rebank.demo.repository;

import com.rebank.demo.model.Client;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, Long> {

    Optional<Client> findByAccountId(Long accountId);
}
