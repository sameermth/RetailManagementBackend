package com.retailmanagement.modules.supplier.service.impl;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.supplier.dto.request.SupplierContactRequest;
import com.retailmanagement.modules.supplier.dto.request.SupplierRatingRequest;
import com.retailmanagement.modules.supplier.dto.request.SupplierRequest;
import com.retailmanagement.modules.supplier.dto.response.SupplierResponse;
import com.retailmanagement.modules.supplier.dto.response.SupplierSummaryResponse;
import com.retailmanagement.modules.supplier.enums.SupplierStatus;
import com.retailmanagement.modules.supplier.mapper.SupplierContactMapper;
import com.retailmanagement.modules.supplier.mapper.SupplierMapper;
import com.retailmanagement.modules.supplier.model.Supplier;
import com.retailmanagement.modules.supplier.model.SupplierContact;
import com.retailmanagement.modules.supplier.model.SupplierRating;
import com.retailmanagement.modules.supplier.repository.SupplierContactRepository;
import com.retailmanagement.modules.supplier.repository.SupplierRatingRepository;
import com.retailmanagement.modules.supplier.repository.SupplierRepository;
import com.retailmanagement.modules.supplier.service.SupplierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;
    private final SupplierContactRepository contactRepository;
    private final SupplierRatingRepository ratingRepository;
    private final SupplierMapper supplierMapper;
    private final SupplierContactMapper contactMapper;

    @Override
    public SupplierResponse createSupplier(SupplierRequest request) {
        log.info("Creating new supplier with name: {}", request.getName());

        // Check if email already exists
        if (request.getEmail() != null && supplierRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Supplier with email " + request.getEmail() + " already exists");
        }

        // Check if phone already exists
        if (request.getPhone() != null && supplierRepository.existsByPhone(request.getPhone())) {
            throw new BusinessException("Supplier with phone " + request.getPhone() + " already exists");
        }

        Supplier supplier = supplierMapper.toEntity(request);

        // Generate supplier code
        supplier.setSupplierCode(generateSupplierCode());
        supplier.setStatus(SupplierStatus.ACTIVE);
        supplier.setOutstandingAmount(BigDecimal.ZERO);
        supplier.setCreatedBy("SYSTEM");
        supplier.setUpdatedBy("SYSTEM");

        Supplier savedSupplier = supplierRepository.save(supplier);

        // Save contacts if any
        if (request.getContacts() != null && !request.getContacts().isEmpty()) {
            saveSupplierContacts(savedSupplier, request.getContacts());
        }

        log.info("Supplier created successfully with code: {}", savedSupplier.getSupplierCode());

        return supplierMapper.toResponse(savedSupplier);
    }

    private String generateSupplierCode() {
        String year = String.valueOf(LocalDate.now().getYear()).substring(2);
        String randomPart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String supplierCode = "SUP-" + year + "-" + randomPart;

        while (supplierRepository.existsBySupplierCode(supplierCode)) {
            randomPart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            supplierCode = "SUP-" + year + "-" + randomPart;
        }

        return supplierCode;
    }

    private void saveSupplierContacts(Supplier supplier, List<SupplierContactRequest> contactRequests) {
        for (SupplierContactRequest contactRequest : contactRequests) {
            SupplierContact contact = contactMapper.toEntity(contactRequest);
            contact.setSupplier(supplier);
            contactRepository.save(contact);
        }
    }

    @Override
    public SupplierResponse updateSupplier(Long id, SupplierRequest request) {
        log.info("Updating supplier with ID: {}", id);

        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + id));

        // Check email uniqueness if changed
        if (request.getEmail() != null && !request.getEmail().equals(supplier.getEmail()) &&
                supplierRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Supplier with email " + request.getEmail() + " already exists");
        }

        // Check phone uniqueness if changed
        if (request.getPhone() != null && !request.getPhone().equals(supplier.getPhone()) &&
                supplierRepository.existsByPhone(request.getPhone())) {
            throw new BusinessException("Supplier with phone " + request.getPhone() + " already exists");
        }

        // Update fields
        supplier.setName(request.getName());
        supplier.setEmail(request.getEmail());
        supplier.setPhone(request.getPhone());
        supplier.setAlternatePhone(request.getAlternatePhone());
        supplier.setAddress(request.getAddress());
        supplier.setCity(request.getCity());
        supplier.setState(request.getState());
        supplier.setCountry(request.getCountry());
        supplier.setPincode(request.getPincode());
        supplier.setGstNumber(request.getGstNumber());
        supplier.setPanNumber(request.getPanNumber());
        supplier.setWebsite(request.getWebsite());
        supplier.setContactPerson(request.getContactPerson());
        supplier.setContactPersonPhone(request.getContactPersonPhone());
        supplier.setContactPersonEmail(request.getContactPersonEmail());
        supplier.setCreditLimit(request.getCreditLimit());
        supplier.setPaymentTerms(request.getPaymentTerms());
        supplier.setPaymentMethod(request.getPaymentMethod());
        supplier.setBankName(request.getBankName());
        supplier.setBankAccountNumber(request.getBankAccountNumber());
        supplier.setBankIfscCode(request.getBankIfscCode());
        supplier.setBankBranch(request.getBankBranch());
        supplier.setUpiId(request.getUpiId());
        supplier.setTaxType(request.getTaxType());
        supplier.setTaxRegistrationNumber(request.getTaxRegistrationNumber());
        supplier.setBusinessType(request.getBusinessType());
        supplier.setLeadTimeDays(request.getLeadTimeDays());
        supplier.setMinimumOrderValue(request.getMinimumOrderValue());
        supplier.setMaximumOrderValue(request.getMaximumOrderValue());
        supplier.setNotes(request.getNotes());
        supplier.setIsActive(request.getIsActive());
        supplier.setUpdatedBy("SYSTEM");

        Supplier updatedSupplier = supplierRepository.save(supplier);

        // Update contacts if provided
        if (request.getContacts() != null) {
            // Delete existing contacts
            contactRepository.deleteAll(contactRepository.findBySupplierId(supplier.getId()));
            // Save new contacts
            saveSupplierContacts(updatedSupplier, request.getContacts());
        }

        log.info("Supplier updated successfully with ID: {}", updatedSupplier.getId());

        return supplierMapper.toResponse(updatedSupplier);
    }

    @Override
    public SupplierResponse getSupplierById(Long id) {
        log.debug("Fetching supplier with ID: {}", id);

        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + id));

        SupplierResponse response = supplierMapper.toResponse(supplier);
        response.setAverageRating(getSupplierAverageRating(id));

        return response;
    }

    @Override
    public SupplierResponse getSupplierByCode(String supplierCode) {
        log.debug("Fetching supplier with code: {}", supplierCode);

        Supplier supplier = supplierRepository.findBySupplierCode(supplierCode)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with code: " + supplierCode));

        SupplierResponse response = supplierMapper.toResponse(supplier);
        response.setAverageRating(getSupplierAverageRating(supplier.getId()));

        return response;
    }

    @Override
    public SupplierResponse getSupplierByEmail(String email) {
        log.debug("Fetching supplier with email: {}", email);

        Supplier supplier = supplierRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with email: " + email));

        SupplierResponse response = supplierMapper.toResponse(supplier);
        response.setAverageRating(getSupplierAverageRating(supplier.getId()));

        return response;
    }

    @Override
    public SupplierResponse getSupplierByPhone(String phone) {
        log.debug("Fetching supplier with phone: {}", phone);

        Supplier supplier = supplierRepository.findByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with phone: " + phone));

        SupplierResponse response = supplierMapper.toResponse(supplier);
        response.setAverageRating(getSupplierAverageRating(supplier.getId()));

        return response;
    }

    @Override
    public Page<SupplierResponse> getAllSuppliers(Pageable pageable) {
        log.debug("Fetching all suppliers with pagination");

        return supplierRepository.findAll(pageable)
                .map(supplier -> {
                    SupplierResponse response = supplierMapper.toResponse(supplier);
                    response.setAverageRating(getSupplierAverageRating(supplier.getId()));
                    return response;
                });
    }

    @Override
    public Page<SupplierResponse> searchSuppliers(String searchTerm, Pageable pageable) {
        log.debug("Searching suppliers with term: {}", searchTerm);

        return supplierRepository.searchSuppliers(searchTerm, pageable)
                .map(supplier -> {
                    SupplierResponse response = supplierMapper.toResponse(supplier);
                    response.setAverageRating(getSupplierAverageRating(supplier.getId()));
                    return response;
                });
    }

    @Override
    public Page<SupplierResponse> getSuppliersByStatus(SupplierStatus status, Pageable pageable) {
        log.debug("Fetching suppliers with status: {}", status);

        return supplierRepository.findByStatus(status, pageable)
                .map(supplier -> {
                    SupplierResponse response = supplierMapper.toResponse(supplier);
                    response.setAverageRating(getSupplierAverageRating(supplier.getId()));
                    return response;
                });
    }

    @Override
    public void deleteSupplier(Long id) {
        log.info("Deleting supplier with ID: {}", id);

        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + id));

        // Check if supplier has any purchases
        if (supplier.getPurchases() != null && !supplier.getPurchases().isEmpty()) {
            throw new BusinessException("Cannot delete supplier with existing purchase records");
        }

        supplierRepository.delete(supplier);
        log.info("Supplier deleted successfully with ID: {}", id);
    }

    @Override
    public void activateSupplier(Long id) {
        log.info("Activating supplier with ID: {}", id);

        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + id));

        supplier.setStatus(SupplierStatus.ACTIVE);
        supplier.setUpdatedBy("SYSTEM");
        supplierRepository.save(supplier);
    }

    @Override
    public void deactivateSupplier(Long id) {
        log.info("Deactivating supplier with ID: {}", id);

        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + id));

        supplier.setStatus(SupplierStatus.INACTIVE);
        supplier.setUpdatedBy("SYSTEM");
        supplierRepository.save(supplier);
    }

    @Override
    public void blacklistSupplier(Long id, String reason) {
        log.info("Blacklisting supplier with ID: {}", id);

        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + id));

        supplier.setStatus(SupplierStatus.BLACKLISTED);
        supplier.setNotes(supplier.getNotes() + " [BLACKLISTED: " + reason + "]");
        supplier.setUpdatedBy("SYSTEM");
        supplierRepository.save(supplier);
    }

    @Override
    public SupplierResponse updateOutstandingAmount(Long id, BigDecimal amount) {
        log.info("Updating outstanding amount for supplier ID: {} by {}", id, amount);

        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + id));

        BigDecimal newOutstanding = supplier.getOutstandingAmount().add(amount);
        supplier.setOutstandingAmount(newOutstanding);

        Supplier updatedSupplier = supplierRepository.save(supplier);
        return supplierMapper.toResponse(updatedSupplier);
    }

    @Override
    public List<SupplierSummaryResponse> getSuppliersWithOutstanding() {
        log.debug("Fetching suppliers with outstanding amount");

        return supplierRepository.findSuppliersWithOutstanding().stream()
                .map(supplier -> {
                    SupplierSummaryResponse summary = supplierMapper.toSummaryResponse(supplier);
                    summary.setAverageRating(getSupplierAverageRating(supplier.getId()));
                    return summary;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<SupplierSummaryResponse> getAllSupplierSummaries() {
        log.debug("Fetching all supplier summaries");

        return supplierRepository.findAllOrderedByName().stream()
                .map(supplier -> {
                    SupplierSummaryResponse summary = supplierMapper.toSummaryResponse(supplier);
                    summary.setAverageRating(getSupplierAverageRating(supplier.getId()));
                    return summary;
                })
                .collect(Collectors.toList());
    }

    @Override
    public void addSupplierRating(Long supplierId, SupplierRatingRequest ratingRequest) {
        log.info("Adding rating for supplier ID: {}", supplierId);

        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + supplierId));

        // Calculate average rating
        double averageRating = (ratingRequest.getQualityRating() +
                ratingRequest.getDeliveryRating() +
                ratingRequest.getPriceRating() +
                ratingRequest.getCommunicationRating()) / 4.0;

        SupplierRating rating = SupplierRating.builder()
                .supplier(supplier)
                .qualityRating(ratingRequest.getQualityRating())
                .deliveryRating(ratingRequest.getDeliveryRating())
                .priceRating(ratingRequest.getPriceRating())
                .communicationRating(ratingRequest.getCommunicationRating())
                .averageRating(BigDecimal.valueOf(averageRating)
                        .setScale(2, RoundingMode.HALF_UP).doubleValue())
                .comments(ratingRequest.getComments())
                .purchaseId(ratingRequest.getPurchaseId())
                .ratedBy("SYSTEM")
                .ratedAt(LocalDateTime.now())
                .build();

        ratingRepository.save(rating);
        log.info("Rating added successfully for supplier ID: {}", supplierId);
    }

    @Override
    public Double getSupplierAverageRating(Long supplierId) {
        Double averageRating = ratingRepository.getAverageRatingForSupplier(supplierId);
        return averageRating != null ?
                BigDecimal.valueOf(averageRating).setScale(2, RoundingMode.HALF_UP).doubleValue() : 0.0;
    }

    @Override
    public long getSupplierCount() {
        return supplierRepository.count();
    }

    @Override
    public boolean isEmailUnique(String email) {
        return !supplierRepository.existsByEmail(email);
    }

    @Override
    public boolean isPhoneUnique(String phone) {
        return !supplierRepository.existsByPhone(phone);
    }

    @Override
    public void updateLastPurchaseDate(Long supplierId) {
        log.debug("Updating last purchase date for supplier ID: {}", supplierId);

        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + supplierId));

        supplier.setLastPurchaseDate(LocalDateTime.now());
        supplierRepository.save(supplier);
    }
}