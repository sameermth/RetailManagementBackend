package com.retailmanagement.modules.erp.party.service;

import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.erp.common.security.ErpAccessGuard;
import com.retailmanagement.modules.erp.foundation.entity.Organization;
import com.retailmanagement.modules.erp.foundation.repository.OrganizationRepository;
import com.retailmanagement.modules.erp.party.dto.CustomerDtos;
import com.retailmanagement.modules.erp.party.entity.Customer;
import com.retailmanagement.modules.erp.party.entity.StoreCustomerTerms;
import com.retailmanagement.modules.erp.party.repository.CustomerRepository;
import com.retailmanagement.modules.erp.party.repository.StoreCustomerTermsRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerManagementService {

    private final CustomerRepository customerRepository;
    private final StoreCustomerTermsRepository storeCustomerTermsRepository;
    private final ErpAccessGuard accessGuard;
    private final OrganizationRepository organizationRepository;

    @Transactional(readOnly = true)
    public List<CustomerDtos.CustomerResponse> listCustomers(Long organizationId) {
        accessGuard.assertOrganizationAccess(organizationId);
        return customerRepository.findByOrganizationId(organizationId).stream()
                .sorted(Comparator.comparing(Customer::getCustomerCode))
                .map(this::toCustomerResponse)
                .toList();
    }

    public CustomerDtos.CustomerResponse createCustomer(Long organizationId, Long branchId, CustomerDtos.UpsertCustomerRequest request) {
        accessGuard.assertBranchAccess(organizationId, branchId);
        Customer customer = new Customer();
        customer.setOrganizationId(organizationId);
        customer.setBranchId(branchId);
        applyCustomerRequest(customer, request);
        customer = customerRepository.save(customer);
        upsertDefaultTerms(customer);
        return toCustomerResponse(customer);
    }

    public CustomerDtos.CustomerResponse updateCustomer(Long organizationId, Long customerId, CustomerDtos.UpsertCustomerRequest request) {
        accessGuard.assertOrganizationAccess(organizationId);
        Customer customer = customerRepository.findByIdAndOrganizationId(customerId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));
        applyCustomerRequest(customer, request);
        customer = customerRepository.save(customer);
        syncTermsCreditLimit(customer);
        return toCustomerResponse(customer);
    }

    @Transactional(readOnly = true)
    public CustomerDtos.StoreCustomerTermsResponse getStoreCustomerTerms(Long organizationId, Long customerId) {
        accessGuard.assertOrganizationAccess(organizationId);
        return storeCustomerTermsRepository.findByOrganizationIdAndCustomerId(organizationId, customerId)
                .map(this::toTermsResponse)
                .orElse(null);
    }

    public CustomerDtos.StoreCustomerTermsResponse upsertStoreCustomerTerms(
            Long organizationId,
            Long customerId,
            CustomerDtos.UpsertStoreCustomerTermsRequest request
    ) {
        accessGuard.assertOrganizationAccess(organizationId);
        Customer customer = customerRepository.findByIdAndOrganizationId(customerId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));

        StoreCustomerTerms terms = storeCustomerTermsRepository.findByOrganizationIdAndCustomerId(organizationId, customerId)
                .orElseGet(StoreCustomerTerms::new);
        terms.setOrganizationId(organizationId);
        terms.setCustomerId(customerId);
        terms.setCustomerSegment(normalizeSegment(request.customerSegment(), customer.getCustomerType()));
        terms.setCreditLimit(request.creditLimit() == null ? customer.getCreditLimit() : request.creditLimit());
        terms.setCreditDays(request.creditDays());
        terms.setLoyaltyEnabled(Boolean.TRUE.equals(request.loyaltyEnabled()));
        terms.setLoyaltyPointsBalance(request.loyaltyPointsBalance() == null ? BigDecimal.ZERO : request.loyaltyPointsBalance());
        terms.setPriceTier(trimToNull(request.priceTier()));
        terms.setDiscountPolicy(trimToNull(request.discountPolicy()));
        terms.setIsPreferred(Boolean.TRUE.equals(request.isPreferred()));
        terms.setIsActive(request.isActive() == null || Boolean.TRUE.equals(request.isActive()));
        terms.setContractStart(request.contractStart());
        terms.setContractEnd(request.contractEnd());
        terms.setRemarks(trimToNull(request.remarks()));
        terms = storeCustomerTermsRepository.save(terms);

        customer.setCreditLimit(terms.getCreditLimit());
        customerRepository.save(customer);
        return toTermsResponse(terms);
    }

    private void upsertDefaultTerms(Customer customer) {
        StoreCustomerTerms terms = storeCustomerTermsRepository.findByOrganizationIdAndCustomerId(customer.getOrganizationId(), customer.getId())
                .orElseGet(StoreCustomerTerms::new);
        terms.setOrganizationId(customer.getOrganizationId());
        terms.setCustomerId(customer.getId());
        terms.setCustomerSegment(defaultSegment(customer.getCustomerType()));
        terms.setCreditLimit(customer.getCreditLimit() == null ? BigDecimal.ZERO : customer.getCreditLimit());
        terms.setCreditDays(null);
        terms.setLoyaltyEnabled(false);
        terms.setLoyaltyPointsBalance(BigDecimal.ZERO);
        terms.setIsPreferred(false);
        terms.setIsActive("ACTIVE".equals(customer.getStatus()));
        storeCustomerTermsRepository.save(terms);
    }

    private void syncTermsCreditLimit(Customer customer) {
        storeCustomerTermsRepository.findByOrganizationIdAndCustomerId(customer.getOrganizationId(), customer.getId())
                .ifPresent(terms -> {
                    terms.setCreditLimit(customer.getCreditLimit() == null ? BigDecimal.ZERO : customer.getCreditLimit());
                    terms.setCustomerSegment(normalizeSegment(terms.getCustomerSegment(), customer.getCustomerType()));
                    storeCustomerTermsRepository.save(terms);
                });
    }

    private void applyCustomerRequest(Customer customer, CustomerDtos.UpsertCustomerRequest request) {
        customer.setCustomerCode(resolveCustomerCode(customer, request.customerCode()));
        customer.setFullName(request.fullName().trim());
        customer.setCustomerType(normalizeCustomerType(request.customerType(), request.gstin()));
        customer.setLegalName(trimToNull(request.legalName()) == null ? request.fullName().trim() : request.legalName().trim());
        customer.setTradeName(trimToNull(request.tradeName()) == null ? customer.getFullName() : request.tradeName().trim());
        customer.setPhone(trimToNull(request.phone()));
        customer.setEmail(trimToNull(request.email()));
        customer.setGstin(trimToNull(request.gstin()));
        customer.setLinkedOrganizationId(request.linkedOrganizationId());
        customer.setBillingAddress(trimToNull(request.billingAddress()));
        customer.setShippingAddress(trimToNull(request.shippingAddress()));
        customer.setState(trimToNull(request.state()));
        customer.setStateCode(trimToNull(request.stateCode()));
        customer.setContactPersonName(trimToNull(request.contactPersonName()));
        customer.setContactPersonPhone(trimToNull(request.contactPersonPhone()));
        customer.setContactPersonEmail(trimToNull(request.contactPersonEmail()));
        customer.setCreditLimit(request.creditLimit() == null ? BigDecimal.ZERO : request.creditLimit());
        customer.setIsPlatformLinked(Boolean.TRUE.equals(request.isPlatformLinked()) || request.linkedOrganizationId() != null);
        customer.setNotes(trimToNull(request.notes()));
        customer.setStatus(trimToNull(request.status()) == null ? "ACTIVE" : request.status().trim().toUpperCase());
    }

    private String resolveCustomerCode(Customer customer, String requestedCode) {
        String normalized = trimToNull(requestedCode);
        if (normalized != null) {
            String code = normalized.toUpperCase();
            boolean exists = customer.getId() == null
                    ? customerRepository.existsByOrganizationIdAndCustomerCode(customer.getOrganizationId(), code)
                    : customerRepository.existsByOrganizationIdAndCustomerCodeAndIdNot(customer.getOrganizationId(), code, customer.getId());
            if (exists) {
                throw new com.retailmanagement.common.exceptions.BusinessException("Customer code already exists: " + code);
            }
            return code;
        }
        if (trimToNull(customer.getCustomerCode()) != null) {
            return customer.getCustomerCode().trim().toUpperCase();
        }
        return generateCustomerCode(customer.getOrganizationId());
    }

    private String generateCustomerCode(Long organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));
        String orgCode = organization.getCode().trim().toUpperCase();
        for (int sequence = 1; sequence < 100000; sequence++) {
            String generated = "CUST-" + orgCode + "-" + String.format("%04d", sequence);
            if (!customerRepository.existsByOrganizationIdAndCustomerCode(organizationId, generated)) {
                return generated;
            }
        }
        throw new com.retailmanagement.common.exceptions.BusinessException("Unable to generate customer code for organization " + organizationId);
    }

    private CustomerDtos.CustomerResponse toCustomerResponse(Customer customer) {
        return new CustomerDtos.CustomerResponse(
                customer.getId(),
                customer.getOrganizationId(),
                customer.getBranchId(),
                customer.getLinkedOrganizationId(),
                customer.getCustomerCode(),
                customer.getFullName(),
                customer.getCustomerType(),
                customer.getLegalName(),
                customer.getTradeName(),
                customer.getPhone(),
                customer.getEmail(),
                customer.getGstin(),
                customer.getBillingAddress(),
                customer.getShippingAddress(),
                customer.getState(),
                customer.getStateCode(),
                customer.getContactPersonName(),
                customer.getContactPersonPhone(),
                customer.getContactPersonEmail(),
                customer.getIsPlatformLinked(),
                customer.getCreditLimit(),
                customer.getNotes(),
                customer.getStatus(),
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }

    private CustomerDtos.StoreCustomerTermsResponse toTermsResponse(StoreCustomerTerms terms) {
        return new CustomerDtos.StoreCustomerTermsResponse(
                terms.getId(),
                terms.getOrganizationId(),
                terms.getCustomerId(),
                terms.getCustomerSegment(),
                terms.getCreditLimit(),
                terms.getCreditDays(),
                terms.getLoyaltyEnabled(),
                terms.getLoyaltyPointsBalance(),
                terms.getPriceTier(),
                terms.getDiscountPolicy(),
                terms.getIsPreferred(),
                terms.getIsActive(),
                terms.getContractStart(),
                terms.getContractEnd(),
                terms.getRemarks(),
                terms.getCreatedAt(),
                terms.getUpdatedAt()
        );
    }

    private String normalizeCustomerType(String type, String gstin) {
        if (trimToNull(type) != null) {
            return type.trim().toUpperCase();
        }
        return trimToNull(gstin) == null ? "INDIVIDUAL" : "BUSINESS";
    }

    private String normalizeSegment(String segment, String customerType) {
        if (trimToNull(segment) != null) {
            return segment.trim().toUpperCase();
        }
        return defaultSegment(customerType);
    }

    private String defaultSegment(String customerType) {
        return "BUSINESS".equals(customerType) ? "B2B" : "RETAIL";
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
