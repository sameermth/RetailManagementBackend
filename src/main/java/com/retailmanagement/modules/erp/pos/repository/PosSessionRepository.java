package com.retailmanagement.modules.erp.pos.repository;

import com.retailmanagement.modules.erp.pos.entity.PosSession;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PosSessionRepository extends JpaRepository<PosSession, Long> {

    List<PosSession> findByOrganizationIdOrderByOpenedAtDescIdDesc(Long organizationId);

    Optional<PosSession> findFirstByOrganizationIdAndBranchIdAndWarehouseIdAndOpenedByUserIdAndStatusOrderByOpenedAtDescIdDesc(
            Long organizationId,
            Long branchId,
            Long warehouseId,
            Long openedByUserId,
            String status
    );

    boolean existsByOrganizationIdAndBranchIdAndWarehouseIdAndOpenedByUserIdAndStatus(
            Long organizationId,
            Long branchId,
            Long warehouseId,
            Long openedByUserId,
            String status
    );

    boolean existsByOrganizationIdAndBranchIdAndWarehouseIdAndTerminalNameIgnoreCaseAndStatus(
            Long organizationId,
            Long branchId,
            Long warehouseId,
            String terminalName,
            String status
    );
}
