package com.retailmanagement.modules.erp.catalog.repository;

import com.retailmanagement.modules.erp.catalog.entity.HsnMaster;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HsnMasterRepository extends JpaRepository<HsnMaster, Long> {

    Optional<HsnMaster> findByHsnCode(String hsnCode);

    boolean existsByHsnCode(String hsnCode);

    List<HsnMaster> findTop30ByIsActiveTrueAndHsnCodeContainingIgnoreCaseOrIsActiveTrueAndDescriptionContainingIgnoreCaseOrderByHsnCodeAsc(
            String hsnCode,
            String description
    );

    List<HsnMaster> findTop30ByIsActiveTrueOrderByHsnCodeAsc();
}
