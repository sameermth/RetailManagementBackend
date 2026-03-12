package com.retailmanagement.modules.supplier.service;

import com.retailmanagement.modules.supplier.dto.request.SupplierRequest;
import com.retailmanagement.modules.supplier.dto.request.SupplierRatingRequest;
import com.retailmanagement.modules.supplier.dto.response.SupplierResponse;
import com.retailmanagement.modules.supplier.dto.response.SupplierSummaryResponse;
import com.retailmanagement.modules.supplier.enums.SupplierStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface SupplierService {

    SupplierResponse createSupplier(SupplierRequest request);

    SupplierResponse updateSupplier(Long id, SupplierRequest request);

    SupplierResponse getSupplierById(Long id);

    SupplierResponse getSupplierByCode(String supplierCode);

    SupplierResponse getSupplierByEmail(String email);

    SupplierResponse getSupplierByPhone(String phone);

    Page<SupplierResponse> getAllSuppliers(Pageable pageable);

    Page<SupplierResponse> searchSuppliers(String searchTerm, Pageable pageable);

    Page<SupplierResponse> getSuppliersByStatus(SupplierStatus status, Pageable pageable);

    void deleteSupplier(Long id);

    void activateSupplier(Long id);

    void deactivateSupplier(Long id);

    void blacklistSupplier(Long id, String reason);

    SupplierResponse updateOutstandingAmount(Long id, BigDecimal amount);

    List<SupplierSummaryResponse> getSuppliersWithOutstanding();

    List<SupplierSummaryResponse> getAllSupplierSummaries();

    void addSupplierRating(Long supplierId, SupplierRatingRequest ratingRequest);

    Double getSupplierAverageRating(Long supplierId);

    long getSupplierCount();

    boolean isEmailUnique(String email);

    boolean isPhoneUnique(String phone);

    void updateLastPurchaseDate(Long supplierId);
}