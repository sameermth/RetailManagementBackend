package com.retailmanagement.modules.erp.catalog.repository;

import com.retailmanagement.modules.erp.catalog.entity.Brand;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandRepository extends JpaRepository<Brand, Long> {

    List<Brand> findTop30ByOrganizationIdAndIsActiveTrueOrderByNameAsc(Long organizationId);

    List<Brand> findTop30ByOrganizationIdAndIsActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(Long organizationId, String name);
}
