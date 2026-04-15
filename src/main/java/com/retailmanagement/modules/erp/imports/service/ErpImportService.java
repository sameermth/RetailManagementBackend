package com.retailmanagement.modules.erp.imports.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.erp.catalog.dto.ProductDtos;
import com.retailmanagement.modules.erp.catalog.entity.Brand;
import com.retailmanagement.modules.erp.catalog.entity.Category;
import com.retailmanagement.modules.erp.catalog.entity.StoreProduct;
import com.retailmanagement.modules.erp.catalog.entity.TaxGroup;
import com.retailmanagement.modules.erp.catalog.entity.Uom;
import com.retailmanagement.modules.erp.catalog.repository.BrandRepository;
import com.retailmanagement.modules.erp.catalog.repository.CategoryRepository;
import com.retailmanagement.modules.erp.catalog.repository.StoreProductRepository;
import com.retailmanagement.modules.erp.catalog.repository.TaxGroupRepository;
import com.retailmanagement.modules.erp.catalog.repository.UomRepository;
import com.retailmanagement.modules.erp.catalog.service.ProductService;
import com.retailmanagement.modules.erp.common.security.ErpAccessGuard;
import com.retailmanagement.modules.erp.imports.dto.ErpImportDtos;
import com.retailmanagement.modules.erp.imports.entity.ImportJob;
import com.retailmanagement.modules.erp.imports.entity.ImportJobRow;
import com.retailmanagement.modules.erp.imports.repository.ImportJobRepository;
import com.retailmanagement.modules.erp.imports.repository.ImportJobRowRepository;
import com.retailmanagement.modules.erp.party.dto.CustomerDtos;
import com.retailmanagement.modules.erp.party.dto.SupplierDtos;
import com.retailmanagement.modules.erp.party.entity.Customer;
import com.retailmanagement.modules.erp.party.entity.Supplier;
import com.retailmanagement.modules.erp.party.repository.CustomerRepository;
import com.retailmanagement.modules.erp.party.repository.SupplierRepository;
import com.retailmanagement.modules.erp.party.service.CustomerManagementService;
import com.retailmanagement.modules.erp.party.service.SupplierManagementService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class ErpImportService {

    private final ErpImportFileParser importFileParser;
    private final ErpAccessGuard accessGuard;
    private final CustomerManagementService customerManagementService;
    private final SupplierManagementService supplierManagementService;
    private final ProductService productService;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final StoreProductRepository storeProductRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final UomRepository uomRepository;
    private final TaxGroupRepository taxGroupRepository;
    private final ImportJobRepository importJobRepository;
    private final ImportJobRowRepository importJobRowRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public ErpImportDtos.ImportPreviewResponse previewCustomers(
            Long organizationId,
            Long branchId,
            boolean updateExisting,
            MultipartFile file
    ) {
        accessGuard.assertBranchAccess(organizationId, branchId);
        List<RowPlan<CustomerDtos.UpsertCustomerRequest>> plans = importFileParser.parse(file).stream()
                .map(row -> planCustomerRow(organizationId, row, updateExisting))
                .toList();
        return toPreviewResponse(ErpImportDtos.ImportEntityType.CUSTOMERS, plans);
    }

    public ErpImportDtos.ImportExecutionResponse importCustomers(
            Long organizationId,
            Long branchId,
            boolean updateExisting,
            MultipartFile file
    ) {
        accessGuard.assertBranchAccess(organizationId, branchId);
        List<RowPlan<CustomerDtos.UpsertCustomerRequest>> plans = importFileParser.parse(file).stream()
                .map(row -> planCustomerRow(organizationId, row, updateExisting))
                .toList();
        ImportJob job = createImportJob(organizationId, branchId, ErpImportDtos.ImportEntityType.CUSTOMERS, updateExisting, file, plans);
        List<ErpImportDtos.ImportRowResult> results = new ArrayList<>();
        int imported = 0;
        for (RowPlan<CustomerDtos.UpsertCustomerRequest> plan : plans) {
            if (!plan.messages().isEmpty()) {
                results.add(plan.toResult(ErpImportDtos.ImportRowStatus.FAILED));
                continue;
            }
            try {
                if (plan.action() == ErpImportDtos.ImportAction.UPDATE && plan.existingId() != null) {
                    customerManagementService.updateCustomer(organizationId, plan.existingId(), plan.payload());
                } else {
                    customerManagementService.createCustomer(organizationId, branchId, plan.payload());
                }
                imported++;
                results.add(plan.toResult(ErpImportDtos.ImportRowStatus.IMPORTED));
            } catch (Exception exception) {
                results.add(plan.withMessage(exception.getMessage()).toResult(ErpImportDtos.ImportRowStatus.FAILED));
            }
        }
        return finalizeExecution(job, ErpImportDtos.ImportEntityType.CUSTOMERS, results, imported);
    }

    @Transactional(readOnly = true)
    public ErpImportDtos.ImportPreviewResponse previewSuppliers(
            Long organizationId,
            Long branchId,
            boolean updateExisting,
            MultipartFile file
    ) {
        accessGuard.assertBranchAccess(organizationId, branchId);
        List<RowPlan<SupplierDtos.UpsertSupplierRequest>> plans = importFileParser.parse(file).stream()
                .map(row -> planSupplierRow(organizationId, row, updateExisting))
                .toList();
        return toPreviewResponse(ErpImportDtos.ImportEntityType.SUPPLIERS, plans);
    }

    public ErpImportDtos.ImportExecutionResponse importSuppliers(
            Long organizationId,
            Long branchId,
            boolean updateExisting,
            MultipartFile file
    ) {
        accessGuard.assertBranchAccess(organizationId, branchId);
        List<RowPlan<SupplierDtos.UpsertSupplierRequest>> plans = importFileParser.parse(file).stream()
                .map(row -> planSupplierRow(organizationId, row, updateExisting))
                .toList();
        ImportJob job = createImportJob(organizationId, branchId, ErpImportDtos.ImportEntityType.SUPPLIERS, updateExisting, file, plans);
        List<ErpImportDtos.ImportRowResult> results = new ArrayList<>();
        int imported = 0;
        for (RowPlan<SupplierDtos.UpsertSupplierRequest> plan : plans) {
            if (!plan.messages().isEmpty()) {
                results.add(plan.toResult(ErpImportDtos.ImportRowStatus.FAILED));
                continue;
            }
            try {
                if (plan.action() == ErpImportDtos.ImportAction.UPDATE && plan.existingId() != null) {
                    supplierManagementService.updateSupplier(organizationId, plan.existingId(), plan.payload());
                } else {
                    supplierManagementService.createSupplier(organizationId, branchId, plan.payload());
                }
                imported++;
                results.add(plan.toResult(ErpImportDtos.ImportRowStatus.IMPORTED));
            } catch (Exception exception) {
                results.add(plan.withMessage(exception.getMessage()).toResult(ErpImportDtos.ImportRowStatus.FAILED));
            }
        }
        return finalizeExecution(job, ErpImportDtos.ImportEntityType.SUPPLIERS, results, imported);
    }

    @Transactional(readOnly = true)
    public ErpImportDtos.ImportPreviewResponse previewProducts(
            Long organizationId,
            boolean updateExisting,
            MultipartFile file
    ) {
        accessGuard.assertOrganizationAccess(organizationId);
        List<RowPlan<ProductImportPayload>> plans = importFileParser.parse(file).stream()
                .map(row -> planProductRow(organizationId, row, updateExisting))
                .toList();
        return toPreviewResponse(ErpImportDtos.ImportEntityType.PRODUCTS, plans);
    }

    public ErpImportDtos.ImportExecutionResponse importProducts(
            Long organizationId,
            boolean updateExisting,
            MultipartFile file
    ) {
        accessGuard.assertOrganizationAccess(organizationId);
        List<RowPlan<ProductImportPayload>> plans = importFileParser.parse(file).stream()
                .map(row -> planProductRow(organizationId, row, updateExisting))
                .toList();
        ImportJob job = createImportJob(organizationId, null, ErpImportDtos.ImportEntityType.PRODUCTS, updateExisting, file, plans);
        List<ErpImportDtos.ImportRowResult> results = new ArrayList<>();
        int imported = 0;
        for (RowPlan<ProductImportPayload> plan : plans) {
            if (!plan.messages().isEmpty()) {
                results.add(plan.toResult(ErpImportDtos.ImportRowStatus.FAILED));
                continue;
            }
            try {
                ProductDtos.CreateStoreProductRequest request = toCreateStoreProductRequest(organizationId, plan.payload());
                if (plan.action() == ErpImportDtos.ImportAction.UPDATE && plan.existingId() != null) {
                    productService.update(plan.existingId(), toStoreProduct(request), request.attributes());
                } else {
                    productService.create(toStoreProduct(request), request.hsnCode(), request.attributes());
                }
                imported++;
                results.add(plan.toResult(ErpImportDtos.ImportRowStatus.IMPORTED));
            } catch (Exception exception) {
                results.add(plan.withMessage(exception.getMessage()).toResult(ErpImportDtos.ImportRowStatus.FAILED));
            }
        }
        return finalizeExecution(job, ErpImportDtos.ImportEntityType.PRODUCTS, results, imported);
    }

    @Transactional(readOnly = true)
    public List<ErpImportDtos.ImportJobSummaryResponse> listImportJobs(Long organizationId, ErpImportDtos.ImportEntityType entityType) {
        accessGuard.assertOrganizationAccess(organizationId);
        List<ImportJob> jobs = entityType == null
                ? importJobRepository.findTop100ByOrganizationIdOrderByStartedAtDescIdDesc(organizationId)
                : importJobRepository.findTop100ByOrganizationIdAndEntityTypeOrderByStartedAtDescIdDesc(organizationId, entityType.name());
        return jobs.stream().map(this::toJobSummaryResponse).toList();
    }

    @Transactional(readOnly = true)
    public ErpImportDtos.ImportJobDetailsResponse getImportJob(Long organizationId, Long importJobId) {
        accessGuard.assertOrganizationAccess(organizationId);
        ImportJob job = importJobRepository.findByIdAndOrganizationId(importJobId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Import job not found: " + importJobId));
        List<ErpImportDtos.ImportRowResult> rows = importJobRowRepository.findByImportJobIdOrderByRowNumberAscIdAsc(importJobId).stream()
                .map(this::toRowResult)
                .toList();
        return new ErpImportDtos.ImportJobDetailsResponse(
                job.getId(),
                job.getOrganizationId(),
                job.getBranchId(),
                ErpImportDtos.ImportEntityType.valueOf(job.getEntityType()),
                job.getSourceFileName(),
                job.getContentType(),
                Boolean.TRUE.equals(job.getUpdateExisting()),
                job.getTotalRows(),
                job.getValidRows(),
                job.getImportedRows(),
                job.getFailedRows(),
                job.getStatus(),
                job.getStartedAt(),
                job.getCompletedAt(),
                rows
        );
    }

    @Transactional(readOnly = true)
    public String failedRowsCsv(Long organizationId, Long importJobId) {
        accessGuard.assertOrganizationAccess(organizationId);
        ImportJob job = importJobRepository.findByIdAndOrganizationId(importJobId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Import job not found: " + importJobId));
        List<ImportJobRow> failedRows = importJobRowRepository.findByImportJobIdAndStatusOrderByRowNumberAscIdAsc(importJobId, ErpImportDtos.ImportRowStatus.FAILED.name());
        StringBuilder csv = new StringBuilder();
        csv.append("rowNumber,action,status,reference,messages,valuesJson\n");
        for (ImportJobRow row : failedRows) {
            csv.append(csvCell(String.valueOf(row.getRowNumber()))).append(',')
                    .append(csvCell(row.getAction())).append(',')
                    .append(csvCell(row.getStatus())).append(',')
                    .append(csvCell(row.getReference())).append(',')
                    .append(csvCell(joinMessages(readMessages(row.getMessagesJson())))).append(',')
                    .append(csvCell(row.getValuesJson()))
                    .append('\n');
        }
        if (failedRows.isEmpty()) {
            csv.append(csvCell("")).append(',').append(csvCell("")).append(',').append(csvCell("")).append(',')
                    .append(csvCell("")).append(',').append(csvCell("No failed rows")).append(',').append(csvCell("{ }"))
                    .append('\n');
        }
        return csv.toString();
    }

    public String customerTemplate() {
        return """
                customerCode,fullName,customerType,legalName,tradeName,phone,email,gstin,billingAddress,shippingAddress,state,stateCode,contactPersonName,contactPersonPhone,contactPersonEmail,creditLimit,notes,status
                ,Aarav Traders,BUSINESS,Aarav Traders Pvt Ltd,Aarav Traders,+91-9000000001,aarav@example.com,24ABCDE1234F1Z5,Shop 1 Main Road,Warehouse 2 Main Road,Gujarat,24,Rahul,+91-9000000010,rahul@example.com,50000,Priority dealer,ACTIVE
                """;
    }

    public String supplierTemplate() {
        return """
                supplierCode,name,legalName,tradeName,phone,email,gstin,billingAddress,shippingAddress,state,stateCode,contactPersonName,contactPersonPhone,contactPersonEmail,paymentTerms,notes,status
                ,Power Source Distributors,Power Source Distributors LLP,Power Source,+91-9000000101,powersource@example.com,24ABCDE1234F1Z5,Industrial Area Plot 8,Industrial Area Plot 8,Gujarat,24,Nitin,+91-9000000102,nitin@example.com,30 DAYS,Primary battery supplier,ACTIVE
                """;
    }

    public String productTemplate() {
        return """
                sku,name,description,categoryName,brandName,baseUomCode,taxGroupCode,hsnCode,inventoryTrackingMode,serialTrackingEnabled,batchTrackingEnabled,expiryTrackingEnabled,fractionalQuantityAllowed,minStockBaseQty,reorderLevelBaseQty,defaultSalePrice,defaultWarrantyMonths,warrantyTerms,isServiceItem,isActive
                BAT-AMARON-001,Amaron BL700LMF,12V automotive battery,Battery,Amaron,NOS,,85071000,SERIAL,true,false,false,false,2,4,6500,24,Standard warranty,false,true
                """;
    }

    private ErpImportDtos.ImportPreviewResponse toPreviewResponse(
            ErpImportDtos.ImportEntityType entityType,
            List<? extends RowPlan<?>> plans
    ) {
        List<ErpImportDtos.ImportRowResult> rows = new ArrayList<>();
        for (RowPlan<?> plan : plans) {
            rows.add(plan.toResult(plan.messages().isEmpty() ? ErpImportDtos.ImportRowStatus.VALID : ErpImportDtos.ImportRowStatus.ERROR));
        }
        int validRows = (int) rows.stream().filter(row -> row.status() == ErpImportDtos.ImportRowStatus.VALID).count();
        return new ErpImportDtos.ImportPreviewResponse(entityType, rows.size(), validRows, rows.size() - validRows, rows);
    }

    private ErpImportDtos.ImportExecutionResponse toExecutionResponse(
            Long importJobId,
            ErpImportDtos.ImportEntityType entityType,
            List<ErpImportDtos.ImportRowResult> rows,
            int importedRows
    ) {
        return new ErpImportDtos.ImportExecutionResponse(importJobId, entityType, rows.size(), importedRows, rows.size() - importedRows, rows);
    }

    private ImportJob createImportJob(
            Long organizationId,
            Long branchId,
            ErpImportDtos.ImportEntityType entityType,
            boolean updateExisting,
            MultipartFile file,
            List<? extends RowPlan<?>> plans
    ) {
        ImportJob job = new ImportJob();
        job.setOrganizationId(organizationId);
        job.setBranchId(branchId);
        job.setEntityType(entityType.name());
        job.setSourceFileName(file == null ? null : file.getOriginalFilename());
        job.setContentType(file == null ? null : file.getContentType());
        job.setUpdateExisting(updateExisting);
        job.setTotalRows(plans.size());
        job.setValidRows((int) plans.stream().filter(plan -> plan.messages().isEmpty()).count());
        job.setImportedRows(0);
        job.setFailedRows((int) plans.stream().filter(plan -> !plan.messages().isEmpty()).count());
        job.setStatus("RUNNING");
        job.setStartedAt(LocalDateTime.now());
        return importJobRepository.save(job);
    }

    private ErpImportDtos.ImportExecutionResponse finalizeExecution(
            ImportJob job,
            ErpImportDtos.ImportEntityType entityType,
            List<ErpImportDtos.ImportRowResult> results,
            int importedRows
    ) {
        persistImportRows(job, results);
        int failedRows = (int) results.stream().filter(row -> row.status() == ErpImportDtos.ImportRowStatus.FAILED).count();
        job.setImportedRows(importedRows);
        job.setFailedRows(failedRows);
        job.setValidRows((int) results.stream().filter(row ->
                row.status() == ErpImportDtos.ImportRowStatus.IMPORTED || row.status() == ErpImportDtos.ImportRowStatus.VALID).count());
        job.setCompletedAt(LocalDateTime.now());
        if (failedRows == 0) {
            job.setStatus("COMPLETED");
        } else if (importedRows == 0) {
            job.setStatus("FAILED");
        } else {
            job.setStatus("PARTIAL_FAILURE");
        }
        importJobRepository.save(job);
        return toExecutionResponse(job.getId(), entityType, results, importedRows);
    }

    private void persistImportRows(ImportJob job, List<ErpImportDtos.ImportRowResult> results) {
        List<ImportJobRow> rows = results.stream()
                .sorted(Comparator.comparingInt(ErpImportDtos.ImportRowResult::rowNumber))
                .map(result -> {
                    ImportJobRow row = new ImportJobRow();
                    row.setImportJobId(job.getId());
                    row.setRowNumber(result.rowNumber());
                    row.setAction(result.action().name());
                    row.setStatus(result.status().name());
                    row.setReference(result.reference());
                    row.setMessagesJson(writeJson(result.messages()));
                    row.setValuesJson(writeJson(result.values()));
                    return row;
                })
                .toList();
        importJobRowRepository.saveAll(rows);
    }

    private ErpImportDtos.ImportJobSummaryResponse toJobSummaryResponse(ImportJob job) {
        return new ErpImportDtos.ImportJobSummaryResponse(
                job.getId(),
                job.getOrganizationId(),
                job.getBranchId(),
                ErpImportDtos.ImportEntityType.valueOf(job.getEntityType()),
                job.getSourceFileName(),
                job.getContentType(),
                Boolean.TRUE.equals(job.getUpdateExisting()),
                job.getTotalRows(),
                job.getValidRows(),
                job.getImportedRows(),
                job.getFailedRows(),
                job.getStatus(),
                job.getStartedAt(),
                job.getCompletedAt()
        );
    }

    private ErpImportDtos.ImportRowResult toRowResult(ImportJobRow row) {
        return new ErpImportDtos.ImportRowResult(
                row.getRowNumber(),
                ErpImportDtos.ImportAction.valueOf(row.getAction()),
                ErpImportDtos.ImportRowStatus.valueOf(row.getStatus()),
                row.getReference(),
                readMessages(row.getMessagesJson()),
                readValues(row.getValuesJson())
        );
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("Unable to persist import history: " + exception.getMessage());
        }
    }

    private List<String> readMessages(String json) {
        try {
            return json == null ? List.of() : objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException exception) {
            throw new BusinessException("Unable to read import history messages: " + exception.getMessage());
        }
    }

    private Map<String, String> readValues(String json) {
        try {
            return json == null ? Map.of() : objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (JsonProcessingException exception) {
            throw new BusinessException("Unable to read import history values: " + exception.getMessage());
        }
    }

    private String joinMessages(List<String> messages) {
        return String.join(" | ", messages);
    }

    private String csvCell(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }

    private RowPlan<CustomerDtos.UpsertCustomerRequest> planCustomerRow(
            Long organizationId,
            ErpImportFileParser.ParsedRow row,
            boolean updateExisting
    ) {
        Map<String, String> values = row.values();
        List<String> messages = new ArrayList<>();
        String code = upper(trimmed(values, "customercode", "code"));
        String fullName = trimmed(values, "fullname", "name", "customername");
        if (fullName == null) {
            messages.add("fullName is required");
        }

        Optional<Customer> existing = code == null ? Optional.empty() : customerRepository.findByOrganizationIdAndCustomerCodeIgnoreCase(organizationId, code);
        ErpImportDtos.ImportAction action = updateExisting && existing.isPresent()
                ? ErpImportDtos.ImportAction.UPDATE
                : ErpImportDtos.ImportAction.CREATE;
        if (!updateExisting && existing.isPresent()) {
            messages.add("Customer code already exists: " + code);
        }

        CustomerDtos.UpsertCustomerRequest request = new CustomerDtos.UpsertCustomerRequest(
                code,
                fullName == null ? "" : fullName,
                upper(trimmed(values, "customertype", "type")),
                trimmed(values, "legalname"),
                trimmed(values, "tradename"),
                trimmed(values, "phone", "mobilenumber"),
                trimmed(values, "email"),
                upper(trimmed(values, "gstin")),
                null,
                trimmed(values, "billingaddress"),
                trimmed(values, "shippingaddress"),
                trimmed(values, "state"),
                trimmed(values, "statecode"),
                trimmed(values, "contactpersonname"),
                trimmed(values, "contactpersonphone"),
                trimmed(values, "contactpersonemail"),
                decimal(values, messages, "creditlimit"),
                booleanValue(values, "isplatformlinked"),
                trimmed(values, "notes", "remark", "remarks"),
                upper(trimmed(values, "status"))
        );
        return new RowPlan<>(row.rowNumber(), action, existing.map(Customer::getId).orElse(null), reference(code, fullName), values, messages, request);
    }

    private RowPlan<SupplierDtos.UpsertSupplierRequest> planSupplierRow(
            Long organizationId,
            ErpImportFileParser.ParsedRow row,
            boolean updateExisting
    ) {
        Map<String, String> values = row.values();
        List<String> messages = new ArrayList<>();
        String code = upper(trimmed(values, "suppliercode", "code"));
        String name = trimmed(values, "name", "suppliername");
        if (name == null) {
            messages.add("name is required");
        }

        Optional<Supplier> existing = code == null ? Optional.empty() : supplierRepository.findByOrganizationIdAndSupplierCodeIgnoreCase(organizationId, code);
        ErpImportDtos.ImportAction action = updateExisting && existing.isPresent()
                ? ErpImportDtos.ImportAction.UPDATE
                : ErpImportDtos.ImportAction.CREATE;
        if (!updateExisting && existing.isPresent()) {
            messages.add("Supplier code already exists: " + code);
        }

        SupplierDtos.UpsertSupplierRequest request = new SupplierDtos.UpsertSupplierRequest(
                code,
                name == null ? "" : name,
                trimmed(values, "legalname"),
                trimmed(values, "tradename"),
                trimmed(values, "phone", "mobilenumber"),
                trimmed(values, "email"),
                upper(trimmed(values, "gstin")),
                null,
                trimmed(values, "billingaddress"),
                trimmed(values, "shippingaddress"),
                trimmed(values, "state"),
                trimmed(values, "statecode"),
                trimmed(values, "contactpersonname"),
                trimmed(values, "contactpersonphone"),
                trimmed(values, "contactpersonemail"),
                trimmed(values, "paymentterms"),
                booleanValue(values, "isplatformlinked"),
                trimmed(values, "notes", "remark", "remarks"),
                upper(trimmed(values, "status"))
        );
        return new RowPlan<>(row.rowNumber(), action, existing.map(Supplier::getId).orElse(null), reference(code, name), values, messages, request);
    }

    private RowPlan<ProductImportPayload> planProductRow(
            Long organizationId,
            ErpImportFileParser.ParsedRow row,
            boolean updateExisting
    ) {
        Map<String, String> values = row.values();
        List<String> messages = new ArrayList<>();
        String sku = upper(trimmed(values, "sku", "productcode"));
        String name = trimmed(values, "name", "productname");
        String categoryName = trimmed(values, "categoryname", "category");
        String brandName = trimmed(values, "brandname", "brand");
        String baseUomCode = upper(trimmed(values, "baseuomcode", "uomcode", "uom"));
        if (sku == null) {
            messages.add("sku is required");
        }
        if (name == null) {
            messages.add("name is required");
        }
        if (categoryName == null) {
            messages.add("categoryName is required");
        }
        if (brandName == null) {
            messages.add("brandName is required");
        }
        if (baseUomCode == null) {
            messages.add("baseUomCode is required");
        }

        Optional<StoreProduct> existing = sku == null ? Optional.empty() : storeProductRepository.findFirstByOrganizationIdAndSkuIgnoreCase(organizationId, sku);
        ErpImportDtos.ImportAction action = updateExisting && existing.isPresent()
                ? ErpImportDtos.ImportAction.UPDATE
                : ErpImportDtos.ImportAction.CREATE;
        if (!updateExisting && existing.isPresent()) {
            messages.add("Store product SKU already exists: " + sku);
        }

        Category category = categoryName == null ? null : categoryRepository.findByOrganizationIdAndNameIgnoreCase(organizationId, categoryName).orElse(null);
        Brand brand = brandName == null ? null : brandRepository.findByOrganizationIdAndNameIgnoreCase(organizationId, brandName).orElse(null);

        Uom uom = null;
        if (baseUomCode != null) {
            uom = uomRepository.findByCodeIgnoreCase(baseUomCode)
                    .or(() -> uomRepository.findByNameIgnoreCase(baseUomCode))
                    .orElse(null);
            if (uom == null) {
                messages.add("Unknown UOM: " + baseUomCode);
            }
        }

        String taxGroupCode = upper(trimmed(values, "taxgroupcode", "taxgroup"));
        String hsnCode = upper(trimmed(values, "hsncode"));
        TaxGroup taxGroup = null;
        if (taxGroupCode != null) {
            taxGroup = taxGroupRepository.findByOrganizationIdAndCodeIgnoreCase(organizationId, taxGroupCode).orElse(null);
            if (taxGroup == null) {
                messages.add("Unknown tax group: " + taxGroupCode);
            }
        } else if (hsnCode == null) {
            messages.add("Either taxGroupCode or hsnCode is required");
        } else {
            try {
                ProductDtos.TaxGroupSuggestionResponse suggestion = productService.suggestTaxGroup(organizationId, hsnCode, LocalDate.now());
                if (!Boolean.TRUE.equals(suggestion.matched())) {
                    messages.add(suggestion.message() == null ? "No tax group matches HSN " + hsnCode : suggestion.message());
                }
            } catch (Exception exception) {
                messages.add(exception.getMessage());
            }
        }

        String inventoryTrackingMode = upper(trimmed(values, "inventorytrackingmode"));
        if (inventoryTrackingMode == null) {
            inventoryTrackingMode = "NONE";
        }

        ProductImportPayload payload = new ProductImportPayload(
                sku,
                name,
                trimmed(values, "description"),
                categoryName,
                category != null ? category.getId() : null,
                brandName,
                brand != null ? brand.getId() : null,
                uom == null ? null : uom.getId(),
                taxGroup == null ? null : taxGroup.getId(),
                hsnCode,
                inventoryTrackingMode,
                booleanValue(values, "serialtrackingenabled"),
                booleanValue(values, "batchtrackingenabled"),
                booleanValue(values, "expirytrackingenabled"),
                booleanValue(values, "fractionalquantityallowed"),
                decimal(values, messages, "minstockbaseqty"),
                decimal(values, messages, "reorderlevelbaseqty"),
                decimal(values, messages, "defaultsaleprice"),
                integer(values, messages, "defaultwarrantymonths"),
                trimmed(values, "warrantyterms"),
                booleanValue(values, "isserviceitem"),
                defaultTrue(values, "isactive")
        );
        Map<String, String> previewValues = new LinkedHashMap<>(values);
        if (category == null && categoryName != null) {
            previewValues.put("categoryaction", "CREATE");
        }
        if (brand == null && brandName != null) {
            previewValues.put("brandaction", "CREATE");
        }

        return new RowPlan<>(row.rowNumber(), action, existing.map(StoreProduct::getId).orElse(null), reference(sku, name), previewValues, messages, payload);
    }

    private ProductDtos.CreateStoreProductRequest toCreateStoreProductRequest(Long organizationId, ProductImportPayload payload) {
        Long categoryId = payload.categoryId() != null ? payload.categoryId() : ensureCategory(organizationId, payload.categoryName()).getId();
        Long brandId = payload.brandId() != null ? payload.brandId() : ensureBrand(organizationId, payload.brandName()).getId();
        return new ProductDtos.CreateStoreProductRequest(
                organizationId,
                null,
                categoryId,
                brandId,
                Objects.requireNonNull(payload.baseUomId(), "baseUomId"),
                payload.taxGroupId(),
                payload.sku(),
                payload.name(),
                payload.description(),
                payload.hsnCode(),
                payload.inventoryTrackingMode(),
                payload.serialTrackingEnabled(),
                payload.batchTrackingEnabled(),
                payload.expiryTrackingEnabled(),
                payload.fractionalQuantityAllowed(),
                null,
                payload.minStockBaseQty(),
                payload.reorderLevelBaseQty(),
                payload.defaultSalePrice(),
                null,
                payload.defaultWarrantyMonths(),
                payload.warrantyTerms(),
                Boolean.FALSE,
                null,
                payload.isServiceItem(),
                payload.isActive()
        );
    }

    private com.retailmanagement.modules.erp.catalog.entity.StoreProduct toStoreProduct(ProductDtos.CreateStoreProductRequest request) {
        com.retailmanagement.modules.erp.catalog.entity.StoreProduct storeProduct = new com.retailmanagement.modules.erp.catalog.entity.StoreProduct();
        storeProduct.setOrganizationId(request.organizationId());
        storeProduct.setProductId(request.productId());
        storeProduct.setCategoryId(request.categoryId());
        storeProduct.setBrandId(request.brandId());
        storeProduct.setBaseUomId(request.baseUomId());
        storeProduct.setTaxGroupId(request.taxGroupId());
        storeProduct.setSku(request.sku());
        storeProduct.setName(request.name());
        storeProduct.setDescription(request.description());
        storeProduct.setInventoryTrackingMode(request.inventoryTrackingMode());
        storeProduct.setSerialTrackingEnabled(Boolean.TRUE.equals(request.serialTrackingEnabled()));
        storeProduct.setBatchTrackingEnabled(Boolean.TRUE.equals(request.batchTrackingEnabled()));
        storeProduct.setExpiryTrackingEnabled(Boolean.TRUE.equals(request.expiryTrackingEnabled()));
        storeProduct.setFractionalQuantityAllowed(Boolean.TRUE.equals(request.fractionalQuantityAllowed()));
        if (request.minStockBaseQty() != null) {
            storeProduct.setMinStockBaseQty(request.minStockBaseQty());
        }
        if (request.reorderLevelBaseQty() != null) {
            storeProduct.setReorderLevelBaseQty(request.reorderLevelBaseQty());
        }
        if (request.defaultSalePrice() != null) {
            storeProduct.setDefaultSalePrice(request.defaultSalePrice());
        }
        if (request.defaultMrp() != null) {
            storeProduct.setDefaultMrp(request.defaultMrp());
        }
        storeProduct.setDefaultWarrantyMonths(request.defaultWarrantyMonths());
        storeProduct.setWarrantyTerms(request.warrantyTerms());
        storeProduct.setIsBundle(Boolean.TRUE.equals(request.isBundle()));
        storeProduct.setBundlePricingMode(request.bundlePricingMode());
        storeProduct.setIsServiceItem(Boolean.TRUE.equals(request.isServiceItem()));
        storeProduct.setIsActive(request.isActive() == null || request.isActive());
        return storeProduct;
    }

    private Category ensureCategory(Long organizationId, String categoryName) {
        return categoryRepository.findByOrganizationIdAndNameIgnoreCase(organizationId, categoryName)
                .orElseGet(() -> {
                    Category category = new Category();
                    category.setOrganizationId(organizationId);
                    category.setName(categoryName.trim());
                    category.setIsActive(true);
                    return categoryRepository.save(category);
                });
    }

    private Brand ensureBrand(Long organizationId, String brandName) {
        return brandRepository.findByOrganizationIdAndNameIgnoreCase(organizationId, brandName)
                .orElseGet(() -> {
                    Brand brand = new Brand();
                    brand.setOrganizationId(organizationId);
                    brand.setName(brandName.trim());
                    brand.setIsActive(true);
                    return brandRepository.save(brand);
                });
    }

    private String trimmed(Map<String, String> values, String... keys) {
        for (String key : keys) {
            String value = values.get(normalizeKey(key));
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String reference(String primary, String fallback) {
        return primary != null ? primary : fallback;
    }

    private String upper(String value) {
        return value == null ? null : value.toUpperCase(Locale.ROOT);
    }

    private String normalizeKey(String key) {
        return key == null ? "" : key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private BigDecimal decimal(Map<String, String> values, List<String> messages, String key) {
        String value = trimmed(values, key);
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            messages.add("Invalid decimal for " + key + ": " + value);
            return null;
        }
    }

    private Integer integer(Map<String, String> values, List<String> messages, String key) {
        String value = trimmed(values, key);
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            messages.add("Invalid integer for " + key + ": " + value);
            return null;
        }
    }

    private Boolean booleanValue(Map<String, String> values, String key) {
        String value = trimmed(values, key);
        if (value == null) {
            return null;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "true", "1", "yes", "y" -> true;
            case "false", "0", "no", "n" -> false;
            default -> null;
        };
    }

    private Boolean defaultTrue(Map<String, String> values, String key) {
        Boolean parsed = booleanValue(values, key);
        return parsed == null ? Boolean.TRUE : parsed;
    }

    private record ProductImportPayload(
            String sku,
            String name,
            String description,
            String categoryName,
            Long categoryId,
            String brandName,
            Long brandId,
            Long baseUomId,
            Long taxGroupId,
            String hsnCode,
            String inventoryTrackingMode,
            Boolean serialTrackingEnabled,
            Boolean batchTrackingEnabled,
            Boolean expiryTrackingEnabled,
            Boolean fractionalQuantityAllowed,
            BigDecimal minStockBaseQty,
            BigDecimal reorderLevelBaseQty,
            BigDecimal defaultSalePrice,
            Integer defaultWarrantyMonths,
            String warrantyTerms,
            Boolean isServiceItem,
            Boolean isActive
    ) {}

    private record RowPlan<T>(
            int rowNumber,
            ErpImportDtos.ImportAction action,
            Long existingId,
            String reference,
            Map<String, String> values,
            List<String> messages,
            T payload
    ) {
        private RowPlan<T> withMessage(String message) {
            List<String> updatedMessages = new ArrayList<>(messages);
            updatedMessages.add(message == null ? "Import failed" : message);
            return new RowPlan<>(rowNumber, action, existingId, reference, values, updatedMessages, payload);
        }

        private ErpImportDtos.ImportRowResult toResult(ErpImportDtos.ImportRowStatus status) {
            return new ErpImportDtos.ImportRowResult(
                    rowNumber,
                    action,
                    status,
                    reference,
                    List.copyOf(messages),
                    values == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(values))
            );
        }
    }
}
