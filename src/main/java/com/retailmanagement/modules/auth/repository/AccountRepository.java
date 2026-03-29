package com.retailmanagement.modules.auth.repository;

import com.retailmanagement.modules.auth.model.Account;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("authAccountRepository")
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByLoginIdentifierIgnoreCase(String loginIdentifier);
}
