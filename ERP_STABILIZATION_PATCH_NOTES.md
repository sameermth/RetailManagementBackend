# ERP stabilization patch

## Focus
- Fix malformed JSON payload string literals in ERP bridge services
- Add shared ERP constants for statuses and inventory movement types
- Add shared JSON payload builder utility
- Normalize inventory movement type usage to database-supported values

## Why this patch matters
Several ERP bridge services were building JSON payloads with manually concatenated Java string literals. Some of those payload strings were malformed and could break compilation or runtime behavior. This patch replaces them with a shared utility and extracts repeated status/movement literals into constants.

## Included files
- `modules/erp/common/constants/ErpDocumentStatuses.java`
- `modules/erp/common/constants/ErpInventoryMovementTypes.java`
- `modules/erp/common/util/ErpJsonPayloads.java`
- fixes in ERP sales, purchase, inventory, and service bridge services
