package com.retailmanagement.modules.erp.finance.repository;

import com.retailmanagement.modules.erp.finance.entity.Voucher;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    List<Voucher> findTop100ByOrganizationIdOrderByVoucherDateDescIdDesc(Long organizationId);
    List<Voucher> findByOrganizationIdAndVoucherDateBetweenOrderByVoucherDateAscIdAsc(Long organizationId, LocalDate fromDate, LocalDate toDate);
    Optional<Voucher> findByIdAndOrganizationId(Long id, Long organizationId);
    Optional<Voucher> findByOrganizationIdAndReferenceTypeAndReferenceId(Long organizationId, String referenceType, Long referenceId);
}
