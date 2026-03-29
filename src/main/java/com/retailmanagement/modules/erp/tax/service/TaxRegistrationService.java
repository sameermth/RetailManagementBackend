package com.retailmanagement.modules.erp.tax.service;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.erp.common.security.ErpAccessGuard;
import com.retailmanagement.modules.erp.foundation.entity.Organization;
import com.retailmanagement.modules.erp.foundation.repository.BranchRepository;
import com.retailmanagement.modules.erp.foundation.repository.OrganizationRepository;
import com.retailmanagement.modules.erp.sales.entity.SalesInvoice;
import com.retailmanagement.modules.erp.sales.repository.SalesInvoiceRepository;
import com.retailmanagement.modules.erp.tax.dto.TaxDtos;
import com.retailmanagement.modules.erp.tax.entity.TaxRegistration;
import com.retailmanagement.modules.erp.tax.repository.TaxRegistrationRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TaxRegistrationService {

    private final TaxRegistrationRepository taxRegistrationRepository;
    private final OrganizationRepository organizationRepository;
    private final BranchRepository branchRepository;
    private final SalesInvoiceRepository salesInvoiceRepository;
    private final ErpAccessGuard accessGuard;

    @Transactional(readOnly = true)
    public TaxDtos.TaxRegistrationListResponse listRegistrations(Long organizationId, Long branchId, LocalDate documentDate) {
        accessGuard.assertOrganizationAccess(organizationId);
        if (branchId != null) {
            accessGuard.assertBranchAccess(organizationId, branchId);
        }
        LocalDate effectiveDate = documentDate == null ? LocalDate.now() : documentDate;
        List<TaxDtos.TaxRegistrationResponse> registrations = taxRegistrationRepository.findAll().stream()
                .filter(registration -> organizationId.equals(registration.getOrganizationId()))
                .sorted(Comparator.comparing(TaxRegistration::getBranchId, Comparator.nullsFirst(Long::compareTo))
                        .thenComparing(TaxRegistration::getEffectiveFrom, Comparator.reverseOrder()))
                .map(this::toResponse)
                .toList();
        TaxDtos.TaxRegistrationResponse applicable = taxRegistrationRepository
                .findApplicableRegistration(organizationId, null, effectiveDate)
                .map(this::toResponse)
                .orElse(null);
        TaxDtos.TaxRegistrationResponse applicableBranch = branchId == null
                ? null
                : taxRegistrationRepository.findApplicableRegistration(organizationId, branchId, effectiveDate)
                        .map(this::toResponse)
                        .orElse(null);
        TaxDtos.TaxRegistrationResponse effectiveRegistration = applicableBranch != null ? applicableBranch : applicable;
        String effectiveScope = effectiveRegistration == null
                ? "NONE"
                : (applicableBranch != null && applicableBranch.branchId() != null ? "BRANCH" : "ORGANIZATION");
        return new TaxDtos.TaxRegistrationListResponse(
                branchId,
                effectiveDate,
                registrations,
                applicable,
                applicableBranch,
                effectiveRegistration,
                effectiveScope
        );
    }

    public TaxDtos.TaxRegistrationResponse createRegistration(Long organizationId, TaxDtos.UpsertTaxRegistrationRequest request) {
        accessGuard.assertOrganizationAccess(organizationId);
        validateBranchScope(organizationId, request.branchId());
        if (taxRegistrationRepository.findAll().stream().anyMatch(reg -> reg.getGstin().equalsIgnoreCase(request.gstin().trim()))) {
            throw new BusinessException("GSTIN already exists: " + request.gstin());
        }
        TaxRegistration registration = new TaxRegistration();
        registration.setOrganizationId(organizationId);
        applyRequest(registration, request);
        if (Boolean.TRUE.equals(request.isDefault())) {
            clearExistingDefaults(organizationId, request.branchId());
        }
        return toResponse(taxRegistrationRepository.save(registration));
    }

    public TaxDtos.TaxRegistrationResponse updateRegistration(Long organizationId, Long registrationId, TaxDtos.UpsertTaxRegistrationRequest request) {
        accessGuard.assertOrganizationAccess(organizationId);
        TaxRegistration registration = taxRegistrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Tax registration not found: " + registrationId));
        if (!organizationId.equals(registration.getOrganizationId())) {
            throw new BusinessException("Tax registration does not belong to organization " + organizationId);
        }
        validateBranchScope(organizationId, request.branchId());
        if (taxRegistrationRepository.findAll().stream()
                .anyMatch(reg -> !reg.getId().equals(registrationId) && reg.getGstin().equalsIgnoreCase(request.gstin().trim()))) {
            throw new BusinessException("GSTIN already exists: " + request.gstin());
        }
        applyRequest(registration, request);
        if (Boolean.TRUE.equals(request.isDefault())) {
            clearExistingDefaults(organizationId, request.branchId(), registrationId);
        }
        return toResponse(taxRegistrationRepository.save(registration));
    }

    public TaxDtos.GstThresholdSettingsResponse updateThresholdSettings(Long organizationId, TaxDtos.GstThresholdSettingsRequest request) {
        accessGuard.assertOrganizationAccess(organizationId);
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));
        organization.setGstThresholdAmount(request.gstThresholdAmount());
        organization.setGstThresholdAlertEnabled(Boolean.TRUE.equals(request.gstThresholdAlertEnabled()));
        organizationRepository.save(organization);
        return new TaxDtos.GstThresholdSettingsResponse(organization.getId(), organization.getGstThresholdAmount(), organization.getGstThresholdAlertEnabled());
    }

    @Transactional(readOnly = true)
    public TaxDtos.GstThresholdStatusResponse thresholdStatus(Long organizationId, LocalDate asOfDate) {
        accessGuard.assertOrganizationAccess(organizationId);
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));
        LocalDate targetDate = asOfDate == null ? LocalDate.now() : asOfDate;
        LocalDate fyStart = financialYearStart(targetDate);

        BigDecimal turnover = salesInvoiceRepository.findByOrganizationIdOrderByIdDesc(organizationId).stream()
                .filter(invoice -> !("CANCELLED".equals(invoice.getStatus()) || "DRAFT".equals(invoice.getStatus())))
                .filter(invoice -> !invoice.getInvoiceDate().isBefore(fyStart) && !invoice.getInvoiceDate().isAfter(targetDate))
                .map(SalesInvoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean gstRegistered = taxRegistrationRepository.findApplicableRegistration(organizationId, null, targetDate).isPresent();
        BigDecimal threshold = organization.getGstThresholdAmount();
        BigDecimal ratio = threshold.signum() == 0
                ? BigDecimal.ZERO
                : turnover.divide(threshold, 4, RoundingMode.HALF_UP);
        boolean reached = threshold.signum() > 0 && turnover.compareTo(threshold) >= 0;
        String level = alertLevel(ratio, gstRegistered, reached);

        String message = switch (level) {
            case "REGISTERED" -> "GST registration is active for this organization.";
            case "CRITICAL" -> "GST threshold is reached or about to be reached. Register GST immediately.";
            case "HIGH" -> "GST threshold usage is high. Review registration readiness.";
            case "MEDIUM" -> "GST threshold usage is rising. Keep registration details ready.";
            default -> "GST threshold is currently within a safe range.";
        };

        return new TaxDtos.GstThresholdStatusResponse(
                organizationId,
                turnover.setScale(2, RoundingMode.HALF_UP),
                threshold.setScale(2, RoundingMode.HALF_UP),
                ratio.setScale(4, RoundingMode.HALF_UP),
                level,
                gstRegistered,
                reached,
                organization.getGstThresholdAlertEnabled(),
                message
        );
    }

    private void applyRequest(TaxRegistration registration, TaxDtos.UpsertTaxRegistrationRequest request) {
        registration.setBranchId(request.branchId());
        registration.setRegistrationType("GST");
        registration.setRegistrationName(request.registrationName().trim());
        registration.setLegalName(trimToNull(request.legalName()));
        registration.setGstin(request.gstin().trim().toUpperCase());
        registration.setRegistrationStateCode(request.registrationStateCode().trim().toUpperCase());
        registration.setRegistrationStateName(trimToNull(request.registrationStateName()));
        registration.setEffectiveFrom(request.effectiveFrom());
        registration.setEffectiveTo(request.effectiveTo());
        registration.setIsDefault(Boolean.TRUE.equals(request.isDefault()));
        registration.setIsActive(request.isActive() == null || Boolean.TRUE.equals(request.isActive()));
    }

    private void validateBranchScope(Long organizationId, Long branchId) {
        if (branchId != null) {
            accessGuard.assertBranchAccess(organizationId, branchId);
            branchRepository.findByIdAndOrganizationId(branchId, organizationId)
                    .orElseThrow(() -> new BusinessException("Branch does not belong to organization " + organizationId));
        }
    }

    private void clearExistingDefaults(Long organizationId, Long branchId) {
        clearExistingDefaults(organizationId, branchId, null);
    }

    private void clearExistingDefaults(Long organizationId, Long branchId, Long keepId) {
        taxRegistrationRepository.findAll().stream()
                .filter(reg -> organizationId.equals(reg.getOrganizationId()))
                .filter(reg -> java.util.Objects.equals(branchId, reg.getBranchId()))
                .filter(reg -> keepId == null || !keepId.equals(reg.getId()))
                .filter(reg -> Boolean.TRUE.equals(reg.getIsDefault()))
                .forEach(reg -> {
                    reg.setIsDefault(false);
                    taxRegistrationRepository.save(reg);
                });
    }

    private TaxDtos.TaxRegistrationResponse toResponse(TaxRegistration registration) {
        return new TaxDtos.TaxRegistrationResponse(
                registration.getId(),
                registration.getOrganizationId(),
                registration.getBranchId(),
                registration.getRegistrationType(),
                registration.getRegistrationName(),
                registration.getLegalName(),
                registration.getGstin(),
                registration.getRegistrationStateCode(),
                registration.getRegistrationStateName(),
                registration.getEffectiveFrom(),
                registration.getEffectiveTo(),
                registration.getIsDefault(),
                registration.getIsActive()
        );
    }

    private LocalDate financialYearStart(LocalDate asOfDate) {
        int year = asOfDate.getMonthValue() >= Month.APRIL.getValue() ? asOfDate.getYear() : asOfDate.getYear() - 1;
        return LocalDate.of(year, Month.APRIL, 1);
    }

    private String alertLevel(BigDecimal ratio, boolean registered, boolean reached) {
        if (registered) {
            return "REGISTERED";
        }
        if (reached || ratio.compareTo(new BigDecimal("0.90")) >= 0) {
            return "CRITICAL";
        }
        if (ratio.compareTo(new BigDecimal("0.75")) >= 0) {
            return "HIGH";
        }
        if (ratio.compareTo(new BigDecimal("0.50")) >= 0) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
