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
            SELECT a
            FROM AuthAccount a
            JOIN a.person p
            WHERE lower(a.loginIdentifier) = lower(:login)
               OR lower(coalesce(p.primaryEmail, '')) = lower(:login)
            ORDER BY a.id ASC
            """)
    Optional<Account> findByLoginOrEmailIgnoreCase(String login);

    @Query("""
            SELECT DISTINCT a
            FROM User u
            JOIN u.account a
            JOIN u.role r
            WHERE r.code = 'OWNER'
            """)
    List<Account> findOwnerAccountsForPlatformAdmin();
}
