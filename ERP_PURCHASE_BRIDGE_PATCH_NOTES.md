# ERP purchase bridge patch

This patch adds the purchase/payables bridge on top of the ERP sales bridge.

## What it adds
- purchase order and purchase order line APIs
- purchase receipt and receipt line APIs
- supplier payment and allocation APIs
- stock-in posting for purchase receipts
- serial creation on inward
- batch creation on inward
- purchase order receipt progress tracking
- immutable audit events for purchase order, purchase receipt, and supplier payment flows

## New endpoints
- `GET /api/erp/purchases/orders?organizationId=...`
- `GET /api/erp/purchases/orders/{id}`
- `POST /api/erp/purchases/orders`
- `GET /api/erp/purchases/receipts?organizationId=...`
- `GET /api/erp/purchases/receipts/{id}`
- `POST /api/erp/purchases/receipts`
- `GET /api/erp/purchases/supplier-payments?organizationId=...`
- `POST /api/erp/purchases/supplier-payments`
- `POST /api/erp/purchases/supplier-payments/{id}/allocate`

## Notes
- receipt allocation currently drives receipt status to `PARTIALLY_BILLED` / `BILLED` because the schema does not yet expose a separate supplier bill bridge
- this is still a bridge migration, so legacy purchase modules remain untouched
