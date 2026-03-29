package com.retailmanagement.modules.erp.party.repository;

import com.retailmanagement.modules.erp.party.entity.Distributor;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("erpDistributorRepository")
public interface DistributorRepository extends JpaRepository<Distributor, Long> {
    Optional<Distributor> findByOrganizationIdAndId(Long organizationId, Long id);
    boolean existsByEmail(String email);
}
