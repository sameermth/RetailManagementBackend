package com.retailmanagement.modules.erp.service.repository;

import com.retailmanagement.modules.erp.service.entity.ServiceAgreementItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServiceAgreementItemRepository extends JpaRepository<ServiceAgreementItem, Long> {
    List<ServiceAgreementItem> findByServiceAgreementIdOrderByIdAsc(Long serviceAgreementId);

    @Query("""
            select item
            from ServiceAgreementItem item
            join ServiceAgreement agreement on agreement.id = item.serviceAgreementId
            where agreement.organizationId = :organizationId
              and item.productOwnershipId = :ownershipId
            order by agreement.serviceEndDate desc, agreement.id desc, item.id desc
            """)
    List<ServiceAgreementItem> findByOrganizationIdAndProductOwnershipIdOrderByAgreementPriority(
            @Param("organizationId") Long organizationId,
            @Param("ownershipId") Long ownershipId);

    @Query("""
            select item
            from ServiceAgreementItem item
            join ServiceAgreement agreement on agreement.id = item.serviceAgreementId
            where agreement.organizationId = :organizationId
              and agreement.salesInvoiceId = :salesInvoiceId
              and (:salesInvoiceLineId is null or item.salesInvoiceLineId = :salesInvoiceLineId)
            order by agreement.serviceEndDate desc, agreement.id desc, item.id desc
            """)
    List<ServiceAgreementItem> findByOrganizationIdAndInvoiceScopeOrderByAgreementPriority(
            @Param("organizationId") Long organizationId,
            @Param("salesInvoiceId") Long salesInvoiceId,
            @Param("salesInvoiceLineId") Long salesInvoiceLineId);
}
