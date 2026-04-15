package com.retailmanagement.modules.erp.party.repository;

import com.retailmanagement.modules.erp.party.entity.Customer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("erpCustomerRepository")
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByOrganizationId(Long organizationId);

    Optional<Customer> findByIdAndOrganizationId(Long id, Long organizationId);

    Optional<Customer> findByOrganizationIdAndCustomerCodeIgnoreCase(Long organizationId, String customerCode);

    boolean existsByEmail(String email);

    boolean existsByOrganizationIdAndCustomerCode(Long organizationId, String customerCode);

    boolean existsByOrganizationIdAndCustomerCodeAndIdNot(Long organizationId, String customerCode, Long id);
}
