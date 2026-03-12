package com.retailmanagement.modules.supplier.repository;

import com.retailmanagement.modules.supplier.model.SupplierContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierContactRepository extends JpaRepository<SupplierContact, Long> {

    List<SupplierContact> findBySupplierId(Long supplierId);

    List<SupplierContact> findBySupplierIdAndIsPrimaryTrue(Long supplierId);
}