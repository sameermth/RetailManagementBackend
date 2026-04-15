package com.retailmanagement.modules.erp.imports.controller;

import com.retailmanagement.modules.erp.common.api.ErpApiResponse;
import com.retailmanagement.modules.erp.imports.dto.ErpImportDtos;
import com.retailmanagement.modules.erp.imports.service.ErpImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/erp/imports")
@RequiredArgsConstructor
@Tag(name = "ERP Imports", description = "Bulk preview and import endpoints for migration and onboarding")
public class ErpImportController {

    private final ErpImportService erpImportService;

    @GetMapping("/customers/template")
    @Operation(summary = "Download customer import CSV template")
    @PreAuthorize("hasAuthority('sales.view')")
    public ResponseEntity<ByteArrayResource> customerTemplate() {
        return template("customers-import-template.csv", erpImportService.customerTemplate());
    }

    @PostMapping(value = "/customers/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Preview customer import file")
    @PreAuthorize("hasAuthority('sales.create')")
    public ErpApiResponse<ErpImportDtos.ImportPreviewResponse> previewCustomers(
            @RequestParam Long organizationId,
            @RequestParam Long branchId,
            @RequestParam(defaultValue = "false") boolean updateExisting,
            @RequestPart("file") MultipartFile file
    ) {
        return ErpApiResponse.ok(erpImportService.previewCustomers(organizationId, branchId, updateExisting, file));
    }

    @PostMapping(value = "/customers", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Execute customer import file")
    @PreAuthorize("hasAuthority('sales.create')")
    public ErpApiResponse<ErpImportDtos.ImportExecutionResponse> importCustomers(
            @RequestParam Long organizationId,
            @RequestParam Long branchId,
            @RequestParam(defaultValue = "false") boolean updateExisting,
            @RequestPart("file") MultipartFile file
    ) {
        return ErpApiResponse.ok(erpImportService.importCustomers(organizationId, branchId, updateExisting, file), "Customers imported");
    }

    @GetMapping("/suppliers/template")
    @Operation(summary = "Download supplier import CSV template")
    @PreAuthorize("hasAuthority('purchase.view')")
    public ResponseEntity<ByteArrayResource> supplierTemplate() {
        return template("suppliers-import-template.csv", erpImportService.supplierTemplate());
    }

    @PostMapping(value = "/suppliers/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Preview supplier import file")
    @PreAuthorize("hasAuthority('purchase.create')")
    public ErpApiResponse<ErpImportDtos.ImportPreviewResponse> previewSuppliers(
            @RequestParam Long organizationId,
            @RequestParam Long branchId,
            @RequestParam(defaultValue = "false") boolean updateExisting,
            @RequestPart("file") MultipartFile file
    ) {
        return ErpApiResponse.ok(erpImportService.previewSuppliers(organizationId, branchId, updateExisting, file));
    }

    @PostMapping(value = "/suppliers", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Execute supplier import file")
    @PreAuthorize("hasAuthority('purchase.create')")
    public ErpApiResponse<ErpImportDtos.ImportExecutionResponse> importSuppliers(
            @RequestParam Long organizationId,
            @RequestParam Long branchId,
            @RequestParam(defaultValue = "false") boolean updateExisting,
            @RequestPart("file") MultipartFile file
    ) {
        return ErpApiResponse.ok(erpImportService.importSuppliers(organizationId, branchId, updateExisting, file), "Suppliers imported");
    }

    @GetMapping("/products/template")
    @Operation(summary = "Download product import CSV template")
    @PreAuthorize("hasAuthority('catalog.view')")
    public ResponseEntity<ByteArrayResource> productTemplate() {
        return template("products-import-template.csv", erpImportService.productTemplate());
    }

    @PostMapping(value = "/products/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Preview product import file")
    @PreAuthorize("hasAuthority('catalog.manage')")
    public ErpApiResponse<ErpImportDtos.ImportPreviewResponse> previewProducts(
            @RequestParam Long organizationId,
            @RequestParam(defaultValue = "false") boolean updateExisting,
            @RequestPart("file") MultipartFile file
    ) {
        return ErpApiResponse.ok(erpImportService.previewProducts(organizationId, updateExisting, file));
    }

    @PostMapping(value = "/products", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Execute product import file")
    @PreAuthorize("hasAuthority('catalog.manage')")
    public ErpApiResponse<ErpImportDtos.ImportExecutionResponse> importProducts(
            @RequestParam Long organizationId,
            @RequestParam(defaultValue = "false") boolean updateExisting,
            @RequestPart("file") MultipartFile file
    ) {
        return ErpApiResponse.ok(erpImportService.importProducts(organizationId, updateExisting, file), "Products imported");
    }

    @GetMapping("/history")
    @Operation(summary = "List import job history")
    @PreAuthorize("hasAnyAuthority('sales.view','purchase.view','catalog.view')")
    public ErpApiResponse<java.util.List<ErpImportDtos.ImportJobSummaryResponse>> history(
            @RequestParam Long organizationId,
            @RequestParam(required = false) ErpImportDtos.ImportEntityType entityType
    ) {
        return ErpApiResponse.ok(erpImportService.listImportJobs(organizationId, entityType));
    }

    @GetMapping("/history/{id}")
    @Operation(summary = "Get import job detail")
    @PreAuthorize("hasAnyAuthority('sales.view','purchase.view','catalog.view')")
    public ErpApiResponse<ErpImportDtos.ImportJobDetailsResponse> historyDetail(
            @PathVariable Long id,
            @RequestParam Long organizationId
    ) {
        return ErpApiResponse.ok(erpImportService.getImportJob(organizationId, id));
    }

    @GetMapping("/history/{id}/failed-rows")
    @Operation(summary = "Download failed import rows as CSV")
    @PreAuthorize("hasAnyAuthority('sales.view','purchase.view','catalog.view')")
    public ResponseEntity<ByteArrayResource> failedRows(
            @PathVariable Long id,
            @RequestParam Long organizationId
    ) {
        return template("import-" + id + "-failed-rows.csv", erpImportService.failedRowsCsv(organizationId, id));
    }

    private ResponseEntity<ByteArrayResource> template(String filename, String body) {
        byte[] content = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(content.length)
                .body(new ByteArrayResource(content));
    }
}
