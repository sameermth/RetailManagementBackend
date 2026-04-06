package com.retailmanagement.modules.erp.catalog.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class HsnDtos {
    private HsnDtos() {}

    public record HsnMasterResponse(
            Long id,
            String hsnCode,
            String description,
            String chapterCode,
            BigDecimal cgstRate,
            BigDecimal sgstRate,
            BigDecimal igstRate,
            BigDecimal cessRate,
            Boolean isActive,
            String sourceName,
            LocalDate effectiveFrom,
            Boolean derivedFromDatedTaxRule
    ) {}
}
