package com.retailmanagement.modules.erp.catalog.repository;

import com.retailmanagement.modules.erp.catalog.entity.Uom;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UomRepository extends JpaRepository<Uom, Long> {

    List<Uom> findByUomGroupId(Long uomGroupId);

    Optional<Uom> findById(Long id);

    Optional<Uom> findByCodeIgnoreCase(String code);

    Optional<Uom> findByNameIgnoreCase(String name);

    List<Uom> findTop30ByIsActiveTrueOrderByCodeAsc();

    List<Uom> findTop30ByIsActiveTrueAndCodeContainingIgnoreCaseOrIsActiveTrueAndNameContainingIgnoreCaseOrderByCodeAsc(
            String code,
            String name
    );
}
