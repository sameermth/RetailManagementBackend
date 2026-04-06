package com.retailmanagement.modules.erp.catalog.repository;

import com.retailmanagement.modules.erp.catalog.entity.Category;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findTop30ByOrganizationIdAndIsActiveTrueOrderByNameAsc(Long organizationId);

    List<Category> findTop30ByOrganizationIdAndIsActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(Long organizationId, String name);
}
