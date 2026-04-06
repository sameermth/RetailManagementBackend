package com.retailmanagement.modules.erp.catalog.controller;

import com.retailmanagement.modules.erp.catalog.dto.HsnDtos;
import com.retailmanagement.modules.erp.catalog.entity.HsnMaster;
import com.retailmanagement.modules.erp.catalog.service.HsnMasterService;
import com.retailmanagement.modules.erp.common.api.ErpApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/erp/hsn")
@RequiredArgsConstructor
@Tag(name = "ERP HSN", description = "HSN master reference endpoints")
public class HsnMasterController {

    private final HsnMasterService hsnMasterService;

    @GetMapping
    @Operation(summary = "Search HSN master")
    @PreAuthorize("hasAuthority('catalog.view')")
    public ErpApiResponse<List<HsnDtos.HsnMasterResponse>> search(@RequestParam(required = false) String query,
                                                                  @RequestParam(required = false) LocalDate effectiveDate) {
        return ErpApiResponse.ok(hsnMasterService.searchResolved(query, effectiveDate).stream().map(this::toResponse).toList());
    }

    @GetMapping("/{hsnCode}")
    @Operation(summary = "Get HSN master by code")
    @PreAuthorize("hasAuthority('catalog.view')")
    public ErpApiResponse<HsnDtos.HsnMasterResponse> get(@PathVariable String hsnCode,
                                                         @RequestParam(required = false) LocalDate effectiveDate) {
        return ErpApiResponse.ok(toResponse(hsnMasterService.getResolvedByCode(hsnCode, effectiveDate)));
    }

    private HsnDtos.HsnMasterResponse toResponse(HsnMasterService.HsnReference reference) {
        HsnMaster hsn = reference.master();
        var taxRate = reference.taxRate();
        return new HsnDtos.HsnMasterResponse(
                hsn.getId(),
                hsn.getHsnCode(),
                hsn.getDescription(),
                hsn.getChapterCode(),
                taxRate != null ? taxRate.getCgstRate() : hsn.getCgstRate(),
                taxRate != null ? taxRate.getSgstRate() : hsn.getSgstRate(),
                taxRate != null ? taxRate.getIgstRate() : hsn.getIgstRate(),
                taxRate != null ? taxRate.getCessRate() : hsn.getCessRate(),
                hsn.getIsActive(),
                taxRate != null ? taxRate.getSourceName() : hsn.getSourceName(),
                taxRate != null ? taxRate.getEffectiveFrom() : hsn.getEffectiveFrom(),
                taxRate != null
        );
    }
}
