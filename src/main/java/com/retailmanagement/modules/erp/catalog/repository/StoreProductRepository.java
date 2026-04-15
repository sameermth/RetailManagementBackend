package com.retailmanagement.modules.erp.catalog.repository;

import com.retailmanagement.modules.erp.catalog.entity.StoreProduct;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository("erpStoreProductRepository")
public interface StoreProductRepository extends JpaRepository<StoreProduct, Long> {
    List<StoreProduct> findByOrganizationId(Long organizationId);
    List<StoreProduct> findByProductId(Long productId);
    Optional<StoreProduct> findByOrganizationIdAndSku(Long organizationId, String sku);
    Optional<StoreProduct> findFirstByOrganizationIdAndSkuIgnoreCase(Long organizationId, String sku);
    Optional<StoreProduct> findByOrganizationIdAndProductId(Long organizationId, Long productId);

    @Query("""
            select sp
            from ErpStoreProduct sp
            where sp.organizationId = :organizationId
              and sp.isActive = true
              and (
                    lower(sp.sku) like lower(concat('%', :query, '%'))
                 or lower(sp.name) like lower(concat('%', :query, '%'))
              )
            order by sp.name asc, sp.id asc
            """)
    List<StoreProduct> searchActiveForPos(@Param("organizationId") Long organizationId, @Param("query") String query);
}
