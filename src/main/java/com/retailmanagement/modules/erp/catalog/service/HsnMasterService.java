package com.retailmanagement.modules.erp.catalog.service;

import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.erp.catalog.entity.HsnMaster;
import com.retailmanagement.modules.erp.catalog.entity.HsnTaxRate;
import com.retailmanagement.modules.erp.catalog.repository.HsnMasterRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HsnMasterService {

    private final HsnMasterRepository hsnMasterRepository;
    private final HsnTaxRateService hsnTaxRateService;

    public List<HsnMaster> search(String query) {
        String q = query == null ? "" : query.trim();
        if (q.isBlank()) {
            return hsnMasterRepository.findTop30ByIsActiveTrueOrderByHsnCodeAsc();
        }
        return hsnMasterRepository
                .findTop30ByIsActiveTrueAndHsnCodeContainingIgnoreCaseOrIsActiveTrueAndDescriptionContainingIgnoreCaseOrderByHsnCodeAsc(q, q);
    }

    public HsnMaster getByCode(String hsnCode) {
        return hsnMasterRepository.findByHsnCode(hsnCode == null ? null : hsnCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("HSN not found: " + hsnCode));
    }

    public HsnReference getResolvedByCode(String hsnCode, LocalDate effectiveDate) {
        HsnMaster master = getByCode(hsnCode);
        HsnTaxRate rate = hsnTaxRateService.findApplicableRate(master.getHsnCode(), effectiveDate).orElse(null);
        return new HsnReference(master, rate);
    }

    public List<HsnReference> searchResolved(String query, LocalDate effectiveDate) {
        return search(query).stream()
                .map(master -> new HsnReference(
                        master,
                        hsnTaxRateService.findApplicableRate(master.getHsnCode(), effectiveDate).orElse(null)
                ))
                .toList();
    }

    public boolean exists(String hsnCode) {
        String normalized = normalize(hsnCode);
        return normalized != null && hsnMasterRepository.existsByHsnCode(normalized);
    }

    public String normalize(String hsnCode) {
        if (hsnCode == null) {
            return null;
        }
        String normalized = hsnCode.trim();
        return normalized.isBlank() ? null : normalized;
    }

    public record HsnReference(HsnMaster master, HsnTaxRate taxRate) {}
}
