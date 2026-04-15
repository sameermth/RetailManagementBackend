package com.retailmanagement.modules.platformadmin.repository;

import com.retailmanagement.modules.platformadmin.entity.PlatformIncident;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformIncidentRepository extends JpaRepository<PlatformIncident, Long> {
    List<PlatformIncident> findAllByOrderByOpenedAtDescIdDesc();
    long countByProductId(Long productId);
}
