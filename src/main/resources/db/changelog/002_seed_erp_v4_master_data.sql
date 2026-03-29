BEGIN;

-- =========================================================
-- ERP V4 - Reference/master seed data
-- Run after 001_create_erp_v4_schema.sql
-- Adjust organization-scoped inserts after creating your first organization.
-- =========================================================

-- Global roles
INSERT INTO role (code, name, is_system)
VALUES
  ('OWNER', 'Owner', TRUE),
  ('ADMIN', 'Administrator', TRUE),
  ('ACCOUNTANT', 'Accountant', TRUE),
  ('STORE_MANAGER', 'Store Manager', TRUE),
  ('CASHIER', 'Cashier', TRUE),
  ('PURCHASE_OPERATOR', 'Purchase Operator', TRUE),
  ('TECHNICIAN', 'Technician', TRUE),
  ('VIEWER', 'Viewer', TRUE)
ON CONFLICT (code) DO NOTHING;

-- UOM groups
INSERT INTO uom_group (code, name, allows_fraction)
VALUES
  ('COUNT', 'Count', FALSE),
  ('WEIGHT', 'Weight', TRUE),
  ('VOLUME', 'Volume', TRUE),
  ('LENGTH', 'Length', TRUE),
  ('AREA', 'Area', TRUE)
ON CONFLICT (code) DO NOTHING;

-- UOMs
INSERT INTO uom (uom_group_id, code, name, decimal_scale, is_active)
SELECT ug.id, src.code, src.name, src.decimal_scale, TRUE
FROM (
  VALUES
    ('COUNT','PCS','Pieces',0),
    ('COUNT','DOZEN','Dozen',0),
    ('COUNT','BOX','Box',0),
    ('COUNT','CARTON','Carton',0),
    ('COUNT','PAIR','Pair',0),
    ('WEIGHT','G','Gram',3),
    ('WEIGHT','KG','Kilogram',3),
    ('WEIGHT','TON','Ton',3),
    ('VOLUME','ML','Millilitre',3),
    ('VOLUME','LTR','Litre',3),
    ('LENGTH','MM','Millimetre',3),
    ('LENGTH','CM','Centimetre',3),
    ('LENGTH','MTR','Metre',3),
    ('AREA','SQFT','Square Foot',3),
    ('AREA','SQM','Square Metre',3)
) AS src(group_code, code, name, decimal_scale)
JOIN uom_group ug ON ug.code = src.group_code
ON CONFLICT (code) DO NOTHING;

-- Permissions
INSERT INTO permission (code, name, module_code)
VALUES
  ('dashboard.view', 'View dashboard', 'DASHBOARD'),
  ('masters.view', 'View masters', 'MASTERS'),
  ('masters.manage', 'Manage masters', 'MASTERS'),
  ('sales.view', 'View sales', 'SALES'),
  ('sales.create', 'Create sales invoice', 'SALES'),
  ('sales.cancel', 'Cancel sales invoice', 'SALES'),
  ('sales.return', 'Process sales return', 'SALES'),
  ('purchases.view', 'View purchases', 'PURCHASE'),
  ('purchases.create', 'Create purchase order', 'PURCHASE'),
  ('purchases.approve', 'Approve purchase order', 'PURCHASE'),
  ('purchases.receive', 'Receive purchase order', 'PURCHASE'),
  ('inventory.view', 'View inventory', 'INVENTORY'),
  ('inventory.adjust', 'Adjust stock', 'INVENTORY'),
  ('inventory.transfer', 'Transfer stock', 'INVENTORY'),
  ('payments.customer', 'Manage customer receipts', 'FINANCE'),
  ('payments.supplier', 'Manage supplier payments', 'FINANCE'),
  ('expenses.view', 'View expenses', 'EXPENSE'),
  ('expenses.create', 'Create expense', 'EXPENSE'),
  ('expenses.approve', 'Approve expense', 'EXPENSE'),
  ('reports.view', 'View reports', 'REPORTS'),
  ('service.view', 'View service', 'SERVICE'),
  ('service.manage', 'Manage service tickets', 'SERVICE'),
  ('service.claims', 'Manage warranty claims', 'SERVICE'),
  ('settings.manage', 'Manage settings', 'SETTINGS'),
  ('users.manage', 'Manage users', 'USERS'),
  ('approvals.manage', 'Manage approvals', 'APPROVALS')
ON CONFLICT (code) DO NOTHING;

-- Role -> permission mapping
WITH rp(role_code, permission_code) AS (
  VALUES
    ('OWNER','dashboard.view'),('OWNER','masters.view'),('OWNER','masters.manage'),('OWNER','sales.view'),('OWNER','sales.create'),('OWNER','sales.cancel'),('OWNER','sales.return'),
    ('OWNER','purchases.view'),('OWNER','purchases.create'),('OWNER','purchases.approve'),('OWNER','purchases.receive'),('OWNER','inventory.view'),('OWNER','inventory.adjust'),('OWNER','inventory.transfer'),
    ('OWNER','payments.customer'),('OWNER','payments.supplier'),('OWNER','expenses.view'),('OWNER','expenses.create'),('OWNER','expenses.approve'),('OWNER','reports.view'),('OWNER','service.view'),('OWNER','service.manage'),('OWNER','service.claims'),('OWNER','settings.manage'),('OWNER','users.manage'),('OWNER','approvals.manage'),
    ('ADMIN','dashboard.view'),('ADMIN','masters.view'),('ADMIN','masters.manage'),('ADMIN','sales.view'),('ADMIN','sales.create'),('ADMIN','sales.cancel'),('ADMIN','sales.return'),
    ('ADMIN','purchases.view'),('ADMIN','purchases.create'),('ADMIN','purchases.approve'),('ADMIN','purchases.receive'),('ADMIN','inventory.view'),('ADMIN','inventory.adjust'),('ADMIN','inventory.transfer'),
    ('ADMIN','payments.customer'),('ADMIN','payments.supplier'),('ADMIN','expenses.view'),('ADMIN','expenses.create'),('ADMIN','expenses.approve'),('ADMIN','reports.view'),('ADMIN','service.view'),('ADMIN','service.manage'),('ADMIN','service.claims'),('ADMIN','users.manage'),('ADMIN','approvals.manage'),
    ('ACCOUNTANT','dashboard.view'),('ACCOUNTANT','masters.view'),('ACCOUNTANT','sales.view'),('ACCOUNTANT','sales.create'),('ACCOUNTANT','purchases.view'),('ACCOUNTANT','purchases.create'),('ACCOUNTANT','purchases.receive'),('ACCOUNTANT','inventory.view'),('ACCOUNTANT','payments.customer'),('ACCOUNTANT','payments.supplier'),('ACCOUNTANT','expenses.view'),('ACCOUNTANT','expenses.create'),('ACCOUNTANT','reports.view'),
    ('STORE_MANAGER','dashboard.view'),('STORE_MANAGER','masters.view'),('STORE_MANAGER','sales.view'),('STORE_MANAGER','sales.create'),('STORE_MANAGER','purchases.view'),('STORE_MANAGER','purchases.receive'),('STORE_MANAGER','inventory.view'),('STORE_MANAGER','inventory.adjust'),('STORE_MANAGER','inventory.transfer'),('STORE_MANAGER','reports.view'),('STORE_MANAGER','service.view'),('STORE_MANAGER','service.manage'),
    ('CASHIER','dashboard.view'),('CASHIER','sales.view'),('CASHIER','sales.create'),('CASHIER','payments.customer'),('CASHIER','inventory.view'),
    ('PURCHASE_OPERATOR','dashboard.view'),('PURCHASE_OPERATOR','purchases.view'),('PURCHASE_OPERATOR','purchases.create'),('PURCHASE_OPERATOR','purchases.receive'),('PURCHASE_OPERATOR','inventory.view'),
    ('TECHNICIAN','dashboard.view'),('TECHNICIAN','service.view'),('TECHNICIAN','service.manage'),('TECHNICIAN','inventory.view'),
    ('VIEWER','dashboard.view'),('VIEWER','masters.view'),('VIEWER','sales.view'),('VIEWER','purchases.view'),('VIEWER','inventory.view'),('VIEWER','expenses.view'),('VIEWER','reports.view'),('VIEWER','service.view')
)
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM rp
JOIN role r ON r.code = rp.role_code
JOIN permission p ON p.code = rp.permission_code
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Optional starter organization-scoped seed data.
-- Replace/adjust organization code before running if you want this block.
WITH org AS (
  SELECT id FROM organization WHERE code = 'DEFAULT' LIMIT 1
)
INSERT INTO tax_group (organization_id, code, name, cgst_rate, sgst_rate, igst_rate, cess_rate, is_active)
SELECT org.id, src.code, src.name, src.cgst, src.sgst, src.igst, src.cess, TRUE
FROM org
JOIN (
  VALUES
    ('GST_0',  'GST 0%',  0.0, 0.0, 0.0, 0.0),
    ('GST_5',  'GST 5%',  2.5, 2.5, 5.0, 0.0),
    ('GST_12', 'GST 12%', 6.0, 6.0, 12.0, 0.0),
    ('GST_18', 'GST 18%', 9.0, 9.0, 18.0, 0.0),
    ('GST_28', 'GST 28%', 14.0, 14.0, 28.0, 0.0)
) AS src(code, name, cgst, sgst, igst, cess) ON TRUE
ON CONFLICT (organization_id, code) DO NOTHING;

WITH org AS (
  SELECT id FROM organization WHERE code = 'DEFAULT' LIMIT 1
)
INSERT INTO price_list (organization_id, code, name, price_list_type, is_active)
SELECT org.id, src.code, src.name, src.price_list_type, TRUE
FROM org
JOIN (
  VALUES
    ('MRP', 'MRP', 'MRP'),
    ('RETAIL', 'Retail', 'RETAIL'),
    ('WHOLESALE', 'Wholesale', 'WHOLESALE'),
    ('DEALER', 'Dealer', 'DEALER')
) AS src(code, name, price_list_type) ON TRUE
ON CONFLICT (organization_id, code) DO NOTHING;

WITH org AS (
  SELECT id FROM organization WHERE code = 'DEFAULT' LIMIT 1
)
INSERT INTO expense_category (organization_id, code, name, is_active)
SELECT org.id, src.code, src.name, TRUE
FROM org
JOIN (
  VALUES
    ('RENT', 'Rent'),
    ('SALARY', 'Salary'),
    ('ELECTRICITY', 'Electricity'),
    ('TRANSPORT', 'Transport'),
    ('MAINTENANCE', 'Maintenance'),
    ('OFFICE', 'Office Expense'),
    ('MISC', 'Miscellaneous')
) AS src(code, name) ON TRUE
ON CONFLICT (organization_id, code) DO NOTHING;

WITH org AS (
  SELECT id FROM organization WHERE code = 'DEFAULT' LIMIT 1
)
INSERT INTO account (organization_id, code, name, account_type, is_system, is_active)
SELECT org.id, src.code, src.name, src.account_type, TRUE, TRUE
FROM org
JOIN (
  VALUES
    ('CASH', 'Cash In Hand', 'ASSET'),
    ('BANK', 'Bank Account', 'ASSET'),
    ('AR', 'Accounts Receivable', 'ASSET'),
    ('INVENTORY', 'Inventory', 'ASSET'),
    ('AP', 'Accounts Payable', 'LIABILITY'),
    ('OUTPUT_GST', 'Output GST', 'LIABILITY'),
    ('INPUT_GST', 'Input GST', 'ASSET'),
    ('SALES', 'Sales Revenue', 'INCOME'),
    ('DISCOUNT_ALLOWED', 'Discount Allowed', 'EXPENSE'),
    ('PURCHASES', 'Purchases', 'EXPENSE'),
    ('EXPENSE_CONTROL', 'Operating Expenses', 'EXPENSE'),
    ('STOCK_LOSS', 'Stock Adjustment Loss', 'EXPENSE'),
    ('STOCK_GAIN', 'Stock Adjustment Gain', 'INCOME')
) AS src(code, name, account_type) ON TRUE
ON CONFLICT (organization_id, code) DO NOTHING;

COMMIT;
