package com.retailmanagement.modules.auth.repository;

import com.retailmanagement.modules.auth.model.Account;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository("authAccountRepository")
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByLoginIdentifierIgnoreCase(String loginIdentifier);

    @Query("""
            SELECT DISTINCT a
            FROM User u
            JOIN u.account a
            JOIN u.role r
            WHERE r.code = 'OWNER'
            """)
    List<Account> findOwnerAccountsForPlatformAdmin();
}
