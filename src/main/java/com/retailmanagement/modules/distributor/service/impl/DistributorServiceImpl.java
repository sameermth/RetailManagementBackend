package com.retailmanagement.modules.distributor.service.impl;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.distributor.dto.request.DistributorRequest;
import com.retailmanagement.modules.distributor.dto.response.DistributorResponse;
import com.retailmanagement.modules.distributor.dto.response.DistributorSummaryResponse;
import com.retailmanagement.modules.distributor.enums.DistributorStatus;
import com.retailmanagement.modules.distributor.mapper.DistributorMapper;
import com.retailmanagement.modules.distributor.model.Distributor;
import com.retailmanagement.modules.distributor.repository.DistributorRepository;
import com.retailmanagement.modules.distributor.service.DistributorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DistributorServiceImpl implements DistributorService {

    private final DistributorRepository distributorRepository;
    private final DistributorMapper distributorMapper;

    @Override
    public DistributorResponse createDistributor(DistributorRequest request) {
        log.info("Creating new distributor with name: {}", request.getName());

        // Check if email already exists
        if (request.getEmail() != null && distributorRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Distributor with email " + request.getEmail() + " already exists");
        }

        // Check if phone already exists
        if (request.getPhone() != null && distributorRepository.existsByPhone(request.getPhone())) {
            throw new BusinessException("Distributor with phone " + request.getPhone() + " already exists");
        }

        Distributor distributor = distributorMapper.toEntity(request);

        // Generate distributor code
        distributor.setDistributorCode(generateDistributorCode());
        distributor.setStatus(DistributorStatus.ACTIVE);
        distributor.setOutstandingAmount(BigDecimal.ZERO);
        distributor.setCreatedBy("SYSTEM");
        distributor.setUpdatedBy("SYSTEM");

        Distributor savedDistributor = distributorRepository.save(distributor);

        log.info("Distributor created successfully with code: {}", savedDistributor.getDistributorCode());

        return distributorMapper.toResponse(savedDistributor);
    }

    private String generateDistributorCode() {
        String year = String.valueOf(LocalDate.now().getYear()).substring(2);
        String randomPart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String distributorCode = "DIST-" + year + "-" + randomPart;

        while (distributorRepository.existsByDistributorCode(distributorCode)) {
            randomPart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            distributorCode = "DIST-" + year + "-" + randomPart;
        }

        return distributorCode;
    }

    @Override
    public DistributorResponse updateDistributor(Long id, DistributorRequest request) {
        log.info("Updating distributor with ID: {}", id);

        Distributor distributor = distributorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Distributor not found with id: " + id));

        // Check email uniqueness if changed
        if (request.getEmail() != null && !request.getEmail().equals(distributor.getEmail()) &&
                distributorRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Distributor with email " + request.getEmail() + " already exists");
        }

        // Check phone uniqueness if changed
        if (request.getPhone() != null && !request.getPhone().equals(distributor.getPhone()) &&
                distributorRepository.existsByPhone(request.getPhone())) {
            throw new BusinessException("Distributor with phone " + request.getPhone() + " already exists");
        }

        // Update fields
        distributor.setName(request.getName());
        distributor.setEmail(request.getEmail());
        distributor.setPhone(request.getPhone());
        distributor.setAlternatePhone(request.getAlternatePhone());
        distributor.setAddress(request.getAddress());
        distributor.setCity(request.getCity());
        distributor.setState(request.getState());
        distributor.setCountry(request.getCountry());
        distributor.setPincode(request.getPincode());
        distributor.setGstNumber(request.getGstNumber());
        distributor.setPanNumber(request.getPanNumber());
        distributor.setWebsite(request.getWebsite());
        distributor.setContactPerson(request.getContactPerson());
        distributor.setContactPersonPhone(request.getContactPersonPhone());
        distributor.setContactPersonEmail(request.getContactPersonEmail());
        distributor.setCreditLimit(request.getCreditLimit());
        distributor.setPaymentTerms(request.getPaymentTerms());
        distributor.setPaymentMethod(request.getPaymentMethod());
        distributor.setBankName(request.getBankName());
        distributor.setBankAccountNumber(request.getBankAccountNumber());
        distributor.setBankIfscCode(request.getBankIfscCode());
        distributor.setBankBranch(request.getBankBranch());
        distributor.setUpiId(request.getUpiId());
        distributor.setRegion(request.getRegion());
        distributor.setTerritory(request.getTerritory());
        distributor.setCommissionRate(request.getCommissionRate());
        distributor.setDeliveryTimeDays(request.getDeliveryTimeDays());
        distributor.setMinimumOrderValue(request.getMinimumOrderValue());
        distributor.setNotes(request.getNotes());
        distributor.setIsActive(request.getIsActive());
        distributor.setUpdatedBy("SYSTEM");

        Distributor updatedDistributor = distributorRepository.save(distributor);
        log.info("Distributor updated successfully with ID: {}", updatedDistributor.getId());

        return distributorMapper.toResponse(updatedDistributor);
    }

    @Override
    public DistributorResponse getDistributorById(Long id) {
        log.debug("Fetching distributor with ID: {}", id);

        Distributor distributor = distributorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Distributor not found with id: " + id));

        return distributorMapper.toResponse(distributor);
    }

    @Override
    public DistributorResponse getDistributorByCode(String distributorCode) {
        log.debug("Fetching distributor with code: {}", distributorCode);

        Distributor distributor = distributorRepository.findByDistributorCode(distributorCode)
                .orElseThrow(() -> new ResourceNotFoundException("Distributor not found with code: " + distributorCode));

        return distributorMapper.toResponse(distributor);
    }

    @Override
    public DistributorResponse getDistributorByEmail(String email) {
        log.debug("Fetching distributor with email: {}", email);

        Distributor distributor = distributorRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Distributor not found with email: " + email));

        return distributorMapper.toResponse(distributor);
    }

    @Override
    public DistributorResponse getDistributorByPhone(String phone) {
        log.debug("Fetching distributor with phone: {}", phone);

        Distributor distributor = distributorRepository.findByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException("Distributor not found with phone: " + phone));

        return distributorMapper.toResponse(distributor);
    }

    @Override
    public Page<DistributorResponse> getAllDistributors(Pageable pageable) {
        log.debug("Fetching all distributors with pagination");

        return distributorRepository.findAll(pageable)
                .map(distributorMapper::toResponse);
    }

    @Override
    public Page<DistributorResponse> searchDistributors(String searchTerm, Pageable pageable) {
        log.debug("Searching distributors with term: {}", searchTerm);

        return distributorRepository.searchDistributors(searchTerm, pageable)
                .map(distributorMapper::toResponse);
    }

    @Override
    public Page<DistributorResponse> getDistributorsByStatus(DistributorStatus status, Pageable pageable) {
        log.debug("Fetching distributors with status: {}", status);

        return distributorRepository.findByStatus(status, pageable)
                .map(distributorMapper::toResponse);
    }

    @Override
    public List<DistributorResponse> getDistributorsByRegion(String region) {
        log.debug("Fetching distributors in region: {}", region);

        return distributorRepository.findByRegion(region).stream()
                .map(distributorMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<DistributorResponse> getDistributorsByTerritory(String territory) {
        log.debug("Fetching distributors in territory: {}", territory);

        return distributorRepository.findByTerritory(territory).stream()
                .map(distributorMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteDistributor(Long id) {
        log.info("Deleting distributor with ID: {}", id);

        Distributor distributor = distributorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Distributor not found with id: " + id));

        // Check if distributor has any orders
        if (distributor.getOrders() != null && !distributor.getOrders().isEmpty()) {
            throw new BusinessException("Cannot delete distributor with existing orders");
        }

        distributorRepository.delete(distributor);
        log.info("Distributor deleted successfully with ID: {}", id);
    }

    @Override
    public void activateDistributor(Long id) {
        log.info("Activating distributor with ID: {}", id);

        Distributor distributor = distributorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Distributor not found with id: " + id));

        distributor.setStatus(DistributorStatus.ACTIVE);
        distributor.setUpdatedBy("SYSTEM");
        distributorRepository.save(distributor);
    }

    @Override
    public void deactivateDistributor(Long id) {
        log.info("Deactivating distributor with ID: {}", id);

        Distributor distributor = distributorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Distributor not found with id: " + id));

        distributor.setStatus(DistributorStatus.INACTIVE);
        distributor.setUpdatedBy("SYSTEM");
        distributorRepository.save(distributor);
    }

    @Override
    public void blacklistDistributor(Long id, String reason) {
        log.info("Blacklisting distributor with ID: {}", id);

        Distributor distributor = distributorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Distributor not found with id: " + id));

        distributor.setStatus(DistributorStatus.BLACKLISTED);
        distributor.setNotes(distributor.getNotes() + " [BLACKLISTED: " + reason + "]");
        distributor.setUpdatedBy("SYSTEM");
        distributorRepository.save(distributor);
    }

    @Override
    public DistributorResponse updateOutstandingAmount(Long id, BigDecimal amount) {
        log.info("Updating outstanding amount for distributor ID: {} by {}", id, amount);

        Distributor distributor = distributorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Distributor not found with id: " + id));

        BigDecimal newOutstanding = distributor.getOutstandingAmount().add(amount);
        distributor.setOutstandingAmount(newOutstanding);

        Distributor updatedDistributor = distributorRepository.save(distributor);
        return distributorMapper.toResponse(updatedDistributor);
    }

    @Override
    public List<DistributorSummaryResponse> getDistributorsWithOutstanding() {
        log.debug("Fetching distributors with outstanding amount");

        return distributorRepository.findDistributorsWithOutstanding().stream()
                .map(distributorMapper::toSummaryResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<DistributorSummaryResponse> getAllDistributorSummaries() {
        log.debug("Fetching all distributor summaries");

        return distributorRepository.findAll().stream()
                .map(distributorMapper::toSummaryResponse)
                .collect(Collectors.toList());
    }

    @Override
    public long getDistributorCount() {
        return distributorRepository.count();
    }

    @Override
    public boolean isEmailUnique(String email) {
        return !distributorRepository.existsByEmail(email);
    }

    @Override
    public boolean isPhoneUnique(String phone) {
        return !distributorRepository.existsByPhone(phone);
    }

    @Override
    public void updateLastOrderDate(Long distributorId) {
        log.debug("Updating last order date for distributor ID: {}", distributorId);

        Distributor distributor = distributorRepository.findById(distributorId)
                .orElseThrow(() -> new ResourceNotFoundException("Distributor not found with id: " + distributorId));

        distributor.setLastOrderDate(LocalDateTime.now());
        distributorRepository.save(distributor);
    }
}