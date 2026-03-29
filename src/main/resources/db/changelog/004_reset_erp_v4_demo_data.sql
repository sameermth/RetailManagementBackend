-- 004_reset_erp_v4_demo_data.sql
-- Development-only reset script for ERP V4 demo/sample data
-- PostgreSQL
--
-- Purpose:
--   - Clear seeded transactional and master data quickly during backend development
--   - Keep schema, functions, triggers, and indexes intact
--   - Reset identity sequences
--
-- Recommended usage order after reset:
--   1) 002_seed_erp_v4_master_data.sql
--   2) 003_seed_erp_v4_sample_data.sql
--
-- IMPORTANT:
--   - This script deletes data from the ERP tables.
--   - Run only in local/dev/test environments.

BEGIN;

TRUNCATE TABLE
    audit_event,
    ledger_entry,
    voucher_line,
    voucher,

    warranty_claim,
    service_visit,
    service_ticket_item,
    service_ticket,

    approval_history,
    approval_request,
    approval_rule,

    recurring_expense,
    expense,

    stock_adjustment_line,
    stock_adjustment,
    stock_transfer_line,
    stock_transfer,
    inventory_reservation,
    inventory_balance,
    stock_movement_batch,
    stock_movement_serial,
    stock_movement,

    supplier_payment_allocation,
    supplier_payment,
    customer_receipt_allocation,
    customer_receipt,

    product_ownership,
    sales_line_batch,
    sales_line_serial,
    sales_invoice_line,
    sales_invoice,

    purchase_receipt_line_batch,
    purchase_receipt_line_serial,
    purchase_receipt_line,
    purchase_receipt,
    purchase_order_line,
    purchase_order,

    serial_number,
    inventory_batch,

    loyalty_transaction,
    report_schedule,
    notification,
    notification_template,

    price_list_item,
    price_list,

    product_uom_conversion,
    product,

    brand,
    category,

    customer_address,
    customer,
    supplier_address,
    supplier,
    distributor_address,
    distributor,

    expense_category,
    account,

    user_branch_access,
    "user",
    role_permission,
    permission,
    role,

    warehouse,
    branch,
    organization
RESTART IDENTITY CASCADE;

COMMIT;
