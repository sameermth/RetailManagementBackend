package com.retailmanagement.modules.auth.repository;

import com.retailmanagement.modules.auth.model.OrganizationPersonProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationPersonProfileRepository extends JpaRepository<OrganizationPersonProfile, Long> {
    Optional<OrganizationPersonProfile> findByOrganizationIdAndPersonId(Long organizationId, Long personId);
}
