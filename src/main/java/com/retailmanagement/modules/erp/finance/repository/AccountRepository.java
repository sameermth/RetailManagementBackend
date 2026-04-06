package com.retailmanagement.modules.erp.finance.repository;

import com.retailmanagement.modules.erp.finance.entity.Account;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByOrganizationIdOrderByCodeAsc(Long organizationId);
    List<Account> findByOrganizationIdAndIsActiveTrueOrderByCodeAsc(Long organizationId);
    List<Account> findByOrganizationIdAndAccountTypeOrderByCodeAsc(Long organizationId, String accountType);
    List<Account> findByOrganizationIdAndAccountTypeAndIsActiveTrueOrderByCodeAsc(Long organizationId, String accountType);
    Optional<Account> findByIdAndOrganizationId(Long id, Long organizationId);
    Optional<Account> findByOrganizationIdAndCode(Long organizationId, String code);
    boolean existsByOrganizationIdAndCodeAndIdNot(Long organizationId, String code, Long id);
    boolean existsByOrganizationIdAndParentAccountId(Long organizationId, Long parentAccountId);
}
