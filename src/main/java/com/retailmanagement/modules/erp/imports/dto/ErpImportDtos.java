package com.retailmanagement.modules.erp.imports.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class ErpImportDtos {
    private ErpImportDtos() {}

    public enum ImportEntityType {
        CUSTOMERS,
        SUPPLIERS,
        PRODUCTS
    }

    public enum ImportAction {
        CREATE,
        UPDATE
    }

    public enum ImportRowStatus {
        VALID,
        ERROR,
        IMPORTED,
        FAILED
    }

    public record ImportRowResult(
            int rowNumber,
            ImportAction action,
            ImportRowStatus status,
            String reference,
            List<String> messages,
            Map<String, String> values
    ) {}

    public record ImportPreviewResponse(
            ImportEntityType entityType,
            int totalRows,
            int validRows,
            int invalidRows,
            List<ImportRowResult> rows
    ) {}

    public record ImportExecutionResponse(
            Long importJobId,
            ImportEntityType entityType,
            int totalRows,
            int importedRows,
            int failedRows,
            List<ImportRowResult> rows
    ) {}

    public record ImportJobSummaryResponse(
            Long id,
            Long organizationId,
            Long branchId,
            ImportEntityType entityType,
            String sourceFileName,
            String contentType,
            boolean updateExisting,
            int totalRows,
            int validRows,
            int importedRows,
            int failedRows,
            String status,
            LocalDateTime startedAt,
            LocalDateTime completedAt
    ) {}

    public record ImportJobDetailsResponse(
            Long id,
            Long organizationId,
            Long branchId,
            ImportEntityType entityType,
            String sourceFileName,
            String contentType,
            boolean updateExisting,
            int totalRows,
            int validRows,
            int importedRows,
            int failedRows,
            String status,
            LocalDateTime startedAt,
            LocalDateTime completedAt,
            List<ImportRowResult> rows
    ) {}
}
