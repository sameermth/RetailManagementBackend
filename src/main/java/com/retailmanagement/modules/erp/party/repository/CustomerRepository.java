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

    boolean existsByEmail(String email);

    boolean existsByOrganizationIdAndCustomerCode(Long organizationId, String customerCode);
}
