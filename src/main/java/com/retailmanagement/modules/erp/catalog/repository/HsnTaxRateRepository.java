package com.retailmanagement.modules.erp.catalog.repository;

import com.retailmanagement.modules.erp.catalog.entity.HsnTaxRate;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HsnTaxRateRepository extends JpaRepository<HsnTaxRate, Long> {

    @Query("""
            select rate
            from HsnTaxRate rate
            where rate.hsnCode = :hsnCode
              and rate.isActive = true
              and rate.effectiveFrom <= :effectiveDate
              and (rate.effectiveTo is null or rate.effectiveTo >= :effectiveDate)
            order by rate.effectiveFrom desc, rate.id desc
            """)
    Optional<HsnTaxRate> findApplicableRate(@Param("hsnCode") String hsnCode, @Param("effectiveDate") LocalDate effectiveDate);
}
