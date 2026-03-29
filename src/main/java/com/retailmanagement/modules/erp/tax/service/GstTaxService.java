package com.retailmanagement.modules.erp.tax.service;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.erp.catalog.entity.TaxGroup;
import com.retailmanagement.modules.erp.catalog.repository.TaxGroupRepository;
import com.retailmanagement.modules.erp.tax.entity.TaxRegistration;
import com.retailmanagement.modules.erp.tax.repository.TaxRegistrationRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GstTaxService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final TaxGroupRepository taxGroupRepository;
    private final TaxRegistrationRepository taxRegistrationRepository;

    public TaxContext resolveSalesTax(
            Long organizationId,
            Long branchId,
            LocalDate documentDate,
            Long taxGroupId,
            String counterpartyGstin,
            String explicitPlaceOfSupplyStateCode,
            BigDecimal taxableAmount
    ) {
        return resolveTaxContext(
                organizationId,
                branchId,
                documentDate,
                taxGroupId,
                counterpartyGstin,
                explicitPlaceOfSupplyStateCode,
                taxableAmount
        );
    }

    public TaxContext resolvePurchaseTax(
            Long organizationId,
            Long branchId,
            LocalDate documentDate,
            Long taxGroupId,
            String counterpartyGstin,
            String explicitPlaceOfSupplyStateCode,
            BigDecimal taxableAmount
    ) {
        return resolveTaxContext(
                organizationId,
                branchId,
                documentDate,
                taxGroupId,
                counterpartyGstin,
                explicitPlaceOfSupplyStateCode,
                taxableAmount
        );
    }

    private TaxContext resolveTaxContext(
            Long organizationId,
            Long branchId,
            LocalDate documentDate,
            Long taxGroupId,
            String counterpartyGstin,
            String explicitPlaceOfSupplyStateCode,
            BigDecimal taxableAmount
    ) {
        LocalDate effectiveDate = documentDate == null ? LocalDate.now() : documentDate;
        TaxRegistration sellerRegistration = taxRegistrationRepository
                .findApplicableRegistration(organizationId, branchId, effectiveDate)
                .orElse(null);

        if (sellerRegistration == null) {
            return TaxContext.noRegistration(normalizeAmount(taxableAmount));
        }

        TaxGroup taxGroup = taxGroupRepository.findByIdAndOrganizationId(taxGroupId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Tax group not found: " + taxGroupId));

        String placeOfSupplyStateCode = normalizeStateCode(explicitPlaceOfSupplyStateCode);
        if (placeOfSupplyStateCode == null) {
            placeOfSupplyStateCode = extractStateCode(counterpartyGstin);
        }
        if (placeOfSupplyStateCode == null) {
            placeOfSupplyStateCode = sellerRegistration.getRegistrationStateCode();
        }

        boolean intraState = normalizeStateCode(sellerRegistration.getRegistrationStateCode()).equals(placeOfSupplyStateCode);
        BigDecimal normalizedTaxableAmount = normalizeAmount(taxableAmount);

        BigDecimal cgstRate = intraState ? zeroIfNull(taxGroup.getCgstRate()) : BigDecimal.ZERO;
        BigDecimal sgstRate = intraState ? zeroIfNull(taxGroup.getSgstRate()) : BigDecimal.ZERO;
        BigDecimal igstRate = intraState ? BigDecimal.ZERO : zeroIfNull(taxGroup.getIgstRate());
        BigDecimal cessRate = zeroIfNull(taxGroup.getCessRate());

        BigDecimal cgstAmount = percentageOf(normalizedTaxableAmount, cgstRate);
        BigDecimal sgstAmount = percentageOf(normalizedTaxableAmount, sgstRate);
        BigDecimal igstAmount = percentageOf(normalizedTaxableAmount, igstRate);
        BigDecimal cessAmount = percentageOf(normalizedTaxableAmount, cessRate);
        BigDecimal totalTaxAmount = cgstAmount.add(sgstAmount).add(igstAmount).add(cessAmount);

        return new TaxContext(
                sellerRegistration.getId(),
                sellerRegistration.getGstin(),
                placeOfSupplyStateCode,
                normalizedTaxableAmount,
                cgstRate,
                cgstAmount,
                sgstRate,
                sgstAmount,
                igstRate,
                igstAmount,
                cessRate,
                cessAmount,
                totalTaxAmount,
                intraState
        );
    }

    public static String extractStateCode(String gstin) {
        if (gstin == null) {
            return null;
        }
        String value = gstin.trim();
        if (value.length() < 2) {
            return null;
        }
        String stateCode = value.substring(0, 2);
        return stateCode.chars().allMatch(Character::isDigit) ? stateCode : null;
    }

    private static BigDecimal percentageOf(BigDecimal amount, BigDecimal rate) {
        return normalizeAmount(amount)
                .multiply(zeroIfNull(rate))
                .divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static BigDecimal normalizeAmount(BigDecimal value) {
        return zeroIfNull(value).setScale(2, RoundingMode.HALF_UP);
    }

    private static String normalizeStateCode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        if (normalized.length() > 10) {
            throw new BusinessException("Invalid state code: " + value);
        }
        return normalized;
    }

    public record TaxContext(
            Long sellerTaxRegistrationId,
            String sellerGstin,
            String placeOfSupplyStateCode,
            BigDecimal taxableAmount,
            BigDecimal cgstRate,
            BigDecimal cgstAmount,
            BigDecimal sgstRate,
            BigDecimal sgstAmount,
            BigDecimal igstRate,
            BigDecimal igstAmount,
            BigDecimal cessRate,
            BigDecimal cessAmount,
            BigDecimal totalTaxAmount,
            boolean intraState
    ) {
        public BigDecimal effectiveTaxRate() {
            return cgstRate.add(sgstRate).add(igstRate).add(cessRate);
        }

        public BigDecimal lineTotal() {
            return taxableAmount.add(totalTaxAmount);
        }

        public static TaxContext noRegistration(BigDecimal taxableAmount) {
            return new TaxContext(
                    null,
                    null,
                    null,
                    taxableAmount,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    true
            );
        }
    }
}
