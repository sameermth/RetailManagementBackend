package com.retailmanagement.modules.erp.catalog.service;

import com.retailmanagement.modules.erp.catalog.entity.HsnTaxRate;
import com.retailmanagement.modules.erp.catalog.repository.HsnTaxRateRepository;
import java.time.LocalDate;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HsnTaxRateService {

    private final HsnTaxRateRepository hsnTaxRateRepository;

    public Optional<HsnTaxRate> findApplicableRate(String hsnCode, LocalDate effectiveDate) {
        String normalizedHsnCode = normalize(hsnCode);
        if (normalizedHsnCode == null) {
            return Optional.empty();
        }
        LocalDate date = effectiveDate == null ? LocalDate.now() : effectiveDate;
        return hsnTaxRateRepository.findApplicableRate(normalizedHsnCode, date);
    }

    private String normalize(String hsnCode) {
        if (hsnCode == null) {
            return null;
        }
        String normalized = hsnCode.trim();
        return normalized.isBlank() ? null : normalized;
    }
}
