package com.retailmanagement.modules.distributor.service;

import com.retailmanagement.modules.distributor.dto.request.DistributorRequest;
import com.retailmanagement.modules.distributor.dto.response.DistributorResponse;
import com.retailmanagement.modules.distributor.dto.response.DistributorSummaryResponse;
import com.retailmanagement.modules.distributor.enums.DistributorStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface DistributorService {

    DistributorResponse createDistributor(DistributorRequest request);

    DistributorResponse updateDistributor(Long id, DistributorRequest request);

    DistributorResponse getDistributorById(Long id);

    DistributorResponse getDistributorByCode(String distributorCode);

    DistributorResponse getDistributorByEmail(String email);

    DistributorResponse getDistributorByPhone(String phone);

    Page<DistributorResponse> getAllDistributors(Pageable pageable);

    Page<DistributorResponse> searchDistributors(String searchTerm, Pageable pageable);

    Page<DistributorResponse> getDistributorsByStatus(DistributorStatus status, Pageable pageable);

    List<DistributorResponse> getDistributorsByRegion(String region);

    List<DistributorResponse> getDistributorsByTerritory(String territory);

    void deleteDistributor(Long id);

    void activateDistributor(Long id);

    void deactivateDistributor(Long id);

    void blacklistDistributor(Long id, String reason);

    DistributorResponse updateOutstandingAmount(Long id, BigDecimal amount);

    List<DistributorSummaryResponse> getDistributorsWithOutstanding();

    List<DistributorSummaryResponse> getAllDistributorSummaries();

    long getDistributorCount();

    boolean isEmailUnique(String email);

    boolean isPhoneUnique(String phone);

    void updateLastOrderDate(Long distributorId);
}