# ERP service + warranty bridge patch

This patch adds service and warranty APIs on top of the ERP bridge state.

## What it adds
- service ticket entity, items, visits
- warranty claim entity
- distributor ERP repository for claim routing
- service/warranty repositories
- service/warranty service layer
- service/warranty controller

## New endpoints
- `GET /api/erp/service/tickets?organizationId=...`
- `GET /api/erp/service/tickets/{id}?organizationId=...`
- `POST /api/erp/service/tickets`
- `POST /api/erp/service/tickets/{id}/assign`
- `POST /api/erp/service/tickets/{id}/visits`
- `POST /api/erp/service/tickets/{id}/close`
- `GET /api/erp/service/warranty-claims?organizationId=...`
- `GET /api/erp/service/warranty-claims/{id}?organizationId=...`
- `POST /api/erp/service/warranty-claims`
- `POST /api/erp/service/warranty-claims/{id}/status`

## Behavior
- service tickets can originate from invoice or walk-in style sources
- service items can be linked to product ownership and serial numbers
- service visits update ticket workflow state
- warranty claims can route to supplier or distributor
- audit events are written for ticket creation/assignment/visit/close and claim creation/status updates
