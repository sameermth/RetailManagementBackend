-- Consolidated bootstrap master data for fresh databases only.

-- Generated from legacy Liquibase history. Do not use on an already-migrated database.


-- ===== SOURCE: 002_seed_erp_v4_master_data.sql =====

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


-- ===== SOURCE: 009_seed_permission_matrix_v1.sql =====

BEGIN;

INSERT INTO permission (code, name, module_code)
VALUES
  ('org.view', 'View organization', 'FOUNDATION'),
  ('org.manage', 'Manage organization', 'FOUNDATION'),
  ('branch.view', 'View branches', 'FOUNDATION'),
  ('branch.manage', 'Manage branches', 'FOUNDATION'),
  ('warehouse.view', 'View warehouses', 'FOUNDATION'),
  ('warehouse.manage', 'Manage warehouses', 'FOUNDATION'),
  ('user.view', 'View users', 'USERS'),
  ('user.manage', 'Manage users', 'USERS'),
  ('subscription.view', 'View subscriptions', 'SUBSCRIPTION'),
  ('subscription.manage', 'Manage subscriptions', 'SUBSCRIPTION'),
  ('settings.view', 'View settings', 'SETTINGS'),
  ('settings.manage', 'Manage settings', 'SETTINGS'),
  ('catalog.view', 'View catalog', 'CATALOG'),
  ('catalog.manage', 'Manage catalog', 'CATALOG'),
  ('inventory.view', 'View inventory', 'INVENTORY'),
  ('inventory.receive', 'Receive inventory', 'INVENTORY'),
  ('inventory.adjust', 'Adjust inventory', 'INVENTORY'),
  ('inventory.transfer', 'Transfer inventory', 'INVENTORY'),
  ('purchase.view', 'View purchases', 'PURCHASE'),
  ('purchase.create', 'Create purchases', 'PURCHASE'),
  ('purchase.approve', 'Approve purchases', 'PURCHASE'),
  ('purchase.post', 'Post purchase documents', 'PURCHASE'),
  ('sales.view', 'View sales', 'SALES'),
  ('sales.create', 'Create sales', 'SALES'),
  ('sales.post', 'Post sales documents', 'SALES'),
  ('sales.return', 'Process sales returns', 'SALES'),
  ('payment.receive', 'Receive payments', 'PAYMENTS'),
  ('payment.pay', 'Create outgoing payments', 'PAYMENTS'),
  ('report.branch.view', 'View branch reports', 'REPORTS'),
  ('report.org.view', 'View organization reports', 'REPORTS'),
  ('tax.view', 'View tax settings', 'TAX'),
  ('tax.manage', 'Manage tax settings', 'TAX'),
  ('accounting.view', 'View accounting', 'ACCOUNTING'),
  ('accounting.post', 'Post accounting entries', 'ACCOUNTING'),
  ('service.view', 'View service', 'SERVICE'),
  ('service.manage', 'Manage service', 'SERVICE'),
  ('approval.manage', 'Manage approvals', 'APPROVALS')
ON CONFLICT (code) DO NOTHING;

WITH rp(role_code, permission_code) AS (
  VALUES
    ('OWNER','org.view'),
    ('OWNER','org.manage'),
    ('OWNER','branch.view'),
    ('OWNER','branch.manage'),
    ('OWNER','warehouse.view'),
    ('OWNER','warehouse.manage'),
    ('OWNER','user.view'),
    ('OWNER','user.manage'),
    ('OWNER','subscription.view'),
    ('OWNER','subscription.manage'),
    ('OWNER','settings.view'),
    ('OWNER','settings.manage'),
    ('OWNER','catalog.view'),
    ('OWNER','catalog.manage'),
    ('OWNER','inventory.view'),
    ('OWNER','inventory.receive'),
    ('OWNER','inventory.adjust'),
    ('OWNER','inventory.transfer'),
    ('OWNER','purchase.view'),
    ('OWNER','purchase.create'),
    ('OWNER','purchase.approve'),
    ('OWNER','purchase.post'),
    ('OWNER','sales.view'),
    ('OWNER','sales.create'),
    ('OWNER','sales.post'),
    ('OWNER','sales.return'),
    ('OWNER','payment.receive'),
    ('OWNER','payment.pay'),
    ('OWNER','report.branch.view'),
    ('OWNER','report.org.view'),
    ('OWNER','tax.view'),
    ('OWNER','tax.manage'),
    ('OWNER','accounting.view'),
    ('OWNER','accounting.post'),
    ('OWNER','service.view'),
    ('OWNER','service.manage'),
    ('OWNER','approval.manage'),

    ('ADMIN','org.view'),
    ('ADMIN','branch.view'),
    ('ADMIN','branch.manage'),
    ('ADMIN','warehouse.view'),
    ('ADMIN','warehouse.manage'),
    ('ADMIN','user.view'),
    ('ADMIN','user.manage'),
    ('ADMIN','subscription.view'),
    ('ADMIN','settings.view'),
    ('ADMIN','settings.manage'),
    ('ADMIN','catalog.view'),
    ('ADMIN','catalog.manage'),
    ('ADMIN','inventory.view'),
    ('ADMIN','inventory.receive'),
    ('ADMIN','inventory.adjust'),
    ('ADMIN','inventory.transfer'),
    ('ADMIN','purchase.view'),
    ('ADMIN','purchase.create'),
    ('ADMIN','purchase.approve'),
    ('ADMIN','purchase.post'),
    ('ADMIN','sales.view'),
    ('ADMIN','sales.create'),
    ('ADMIN','sales.post'),
    ('ADMIN','sales.return'),
    ('ADMIN','payment.receive'),
    ('ADMIN','payment.pay'),
    ('ADMIN','report.branch.view'),
    ('ADMIN','report.org.view'),
    ('ADMIN','tax.view'),
    ('ADMIN','accounting.view'),
    ('ADMIN','service.view'),
    ('ADMIN','service.manage'),
    ('ADMIN','approval.manage'),

    ('ACCOUNTANT','org.view'),
    ('ACCOUNTANT','branch.view'),
    ('ACCOUNTANT','warehouse.view'),
    ('ACCOUNTANT','catalog.view'),
    ('ACCOUNTANT','inventory.view'),
    ('ACCOUNTANT','purchase.view'),
    ('ACCOUNTANT','sales.view'),
    ('ACCOUNTANT','payment.receive'),
    ('ACCOUNTANT','payment.pay'),
    ('ACCOUNTANT','report.branch.view'),
    ('ACCOUNTANT','report.org.view'),
    ('ACCOUNTANT','tax.view'),
    ('ACCOUNTANT','tax.manage'),
    ('ACCOUNTANT','accounting.view'),
    ('ACCOUNTANT','accounting.post'),
    ('ACCOUNTANT','service.view'),

    ('STORE_MANAGER','org.view'),
    ('STORE_MANAGER','branch.view'),
    ('STORE_MANAGER','warehouse.view'),
    ('STORE_MANAGER','catalog.view'),
    ('STORE_MANAGER','catalog.manage'),
    ('STORE_MANAGER','inventory.view'),
    ('STORE_MANAGER','inventory.receive'),
    ('STORE_MANAGER','inventory.adjust'),
    ('STORE_MANAGER','inventory.transfer'),
    ('STORE_MANAGER','purchase.view'),
    ('STORE_MANAGER','purchase.create'),
    ('STORE_MANAGER','purchase.post'),
    ('STORE_MANAGER','sales.view'),
    ('STORE_MANAGER','sales.create'),
    ('STORE_MANAGER','sales.post'),
    ('STORE_MANAGER','sales.return'),
    ('STORE_MANAGER','payment.receive'),
    ('STORE_MANAGER','report.branch.view'),
    ('STORE_MANAGER','service.view'),
    ('STORE_MANAGER','service.manage'),

    ('CASHIER','branch.view'),
    ('CASHIER','warehouse.view'),
    ('CASHIER','catalog.view'),
    ('CASHIER','inventory.view'),
    ('CASHIER','sales.view'),
    ('CASHIER','sales.create'),
    ('CASHIER','sales.post'),
    ('CASHIER','sales.return'),
    ('CASHIER','payment.receive'),

    ('PURCHASE_OPERATOR','branch.view'),
    ('PURCHASE_OPERATOR','warehouse.view'),
    ('PURCHASE_OPERATOR','catalog.view'),
    ('PURCHASE_OPERATOR','inventory.view'),
    ('PURCHASE_OPERATOR','inventory.receive'),
    ('PURCHASE_OPERATOR','purchase.view'),
    ('PURCHASE_OPERATOR','purchase.create'),
    ('PURCHASE_OPERATOR','purchase.post'),

    ('TECHNICIAN','branch.view'),
    ('TECHNICIAN','catalog.view'),
    ('TECHNICIAN','inventory.view'),
    ('TECHNICIAN','service.view'),
    ('TECHNICIAN','service.manage'),

    ('VIEWER','org.view'),
    ('VIEWER','branch.view'),
    ('VIEWER','warehouse.view'),
    ('VIEWER','catalog.view'),
    ('VIEWER','inventory.view'),
    ('VIEWER','purchase.view'),
    ('VIEWER','sales.view'),
    ('VIEWER','report.branch.view'),
    ('VIEWER','service.view')
)
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM rp
JOIN role r ON r.code = rp.role_code
JOIN permission p ON p.code = rp.permission_code
ON CONFLICT (role_id, permission_id) DO NOTHING;

COMMIT;


-- ===== SOURCE: 014_seed_system_accounts_for_all_orgs.sql =====

INSERT INTO account (organization_id, code, name, account_type, is_system, is_active)
SELECT o.id, src.code, src.name, src.account_type, TRUE, TRUE
FROM organization o
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
    ('COGS', 'Cost of Goods Sold', 'EXPENSE'),
    ('DISCOUNT_ALLOWED', 'Discount Allowed', 'EXPENSE'),
    ('PURCHASES', 'Purchases', 'EXPENSE'),
    ('EXPENSE_CONTROL', 'Operating Expenses', 'EXPENSE'),
    ('STOCK_LOSS', 'Stock Adjustment Loss', 'EXPENSE'),
    ('STOCK_GAIN', 'Stock Adjustment Gain', 'INCOME')
) AS src(code, name, account_type) ON TRUE
ON CONFLICT (organization_id, code) DO NOTHING;


-- ===== SOURCE: 023_seed_service_replacement_accounts.sql =====

INSERT INTO account (organization_id, code, name, account_type, is_system, is_active)
SELECT o.id, src.code, src.name, src.account_type, TRUE, TRUE
FROM organization o
JOIN (
  VALUES
    ('WARRANTY_EXPENSE', 'Warranty Replacement Expense', 'EXPENSE'),
    ('GOODWILL_EXPENSE', 'Goodwill Replacement Expense', 'EXPENSE')
) AS src(code, name, account_type) ON TRUE
ON CONFLICT (organization_id, code) DO NOTHING;


-- ===== SOURCE: 044_add_hsn_master.sql (seed section) =====

INSERT INTO hsn_master (
  hsn_code, description, chapter_code, cgst_rate, sgst_rate, igst_rate, cess_rate, is_active, source_name, effective_from
) VALUES
  ('39173100', 'Flexible tubes, pipes and hoses, of plastics', '39', 9.00, 9.00, 18.00, 0.00, TRUE, 'Curated phase-1 reference seed', DATE '2025-01-01'),
  ('73269099', 'Other articles of iron or steel', '73', 9.00, 9.00, 18.00, 0.00, TRUE, 'Curated phase-1 reference seed', DATE '2025-01-01'),
  ('84145190', 'Fans: table, floor, wall, window, ceiling or roof fans, other', '84', 6.00, 6.00, 12.00, 0.00, TRUE, 'Curated phase-1 reference seed', DATE '2025-01-01'),
  ('85043190', 'Electrical transformers, other', '85', 9.00, 9.00, 18.00, 0.00, TRUE, 'Curated phase-1 reference seed', DATE '2025-01-01'),
  ('85362000', 'Automatic circuit breakers', '85', 9.00, 9.00, 18.00, 0.00, TRUE, 'Curated phase-1 reference seed', DATE '2025-01-01'),
  ('85365090', 'Electrical switches, other', '85', 9.00, 9.00, 18.00, 0.00, TRUE, 'Curated phase-1 reference seed', DATE '2025-01-01'),
  ('85366990', 'Plugs and sockets, other', '85', 9.00, 9.00, 18.00, 0.00, TRUE, 'Curated phase-1 reference seed', DATE '2025-01-01'),
  ('85371000', 'Boards, panels, consoles, cabinets and other bases for electric control or distribution', '85', 9.00, 9.00, 18.00, 0.00, TRUE, 'Curated phase-1 reference seed', DATE '2025-01-01'),
  ('85444299', 'Insulated electric conductors fitted with connectors, other', '85', 9.00, 9.00, 18.00, 0.00, TRUE, 'Curated phase-1 reference seed', DATE '2025-01-01'),
  ('85444999', 'Other insulated electric conductors, other', '85', 9.00, 9.00, 18.00, 0.00, TRUE, 'Curated phase-1 reference seed', DATE '2025-01-01'),
  ('94051090', 'Chandeliers and other electric ceiling or wall lighting fittings, other', '94', 6.00, 6.00, 12.00, 0.00, TRUE, 'Curated phase-1 reference seed', DATE '2025-01-01'),
  ('94054090', 'Other electric lamps and lighting fittings', '94', 6.00, 6.00, 12.00, 0.00, TRUE, 'Curated phase-1 reference seed', DATE '2025-01-01');

UPDATE product
SET hsn_code = '94054090'
WHERE lower(name) LIKE '%flood light%'
  AND (hsn_code IS NULL OR trim(hsn_code) = '');


-- ===== SOURCE: 045_add_hsn_tax_rate.sql (seed section) =====

INSERT INTO hsn_tax_rate (
  hsn_code, cgst_rate, sgst_rate, igst_rate, cess_rate, effective_from, effective_to, is_active, source_name
)
SELECT
  hsn_code,
  cgst_rate,
  sgst_rate,
  igst_rate,
  cess_rate,
  COALESCE(effective_from, CURRENT_DATE),
  NULL,
  is_active,
  source_name
FROM hsn_master
WHERE NOT EXISTS (
  SELECT 1
  FROM hsn_tax_rate rate
  WHERE rate.hsn_code = hsn_master.hsn_code
    AND rate.effective_from = COALESCE(hsn_master.effective_from, CURRENT_DATE)
);


-- ===== SOURCE: 046_add_product_attribute_metadata.sql (seed section) =====

WITH owner_ctx AS (
  SELECT o.id AS organization_id, u.id AS owner_user_id
  FROM organization o
  JOIN app_user u ON u.organization_id = o.id
  WHERE u.employee_code IN ('SPC-OWN-01','UHL-OWN-01')
),
defs AS (
  INSERT INTO product_attribute_definition (
    organization_id, code, label, description, data_type, input_type, is_required, is_active,
    unit_label, placeholder, help_text, sort_order, created_by, updated_by
  )
  SELECT owner_ctx.organization_id, src.code, src.label, src.description, src.data_type, src.input_type, src.is_required, TRUE,
         src.unit_label, src.placeholder, src.help_text, src.sort_order, owner_ctx.owner_user_id, owner_ctx.owner_user_id
  FROM owner_ctx
  JOIN (
    VALUES
      ('battery_type', 'Battery Type', 'Battery fitment / maintenance type', 'OPTION', 'SELECT', TRUE, NULL, 'Select battery type', 'Used to distinguish RMF / LMF / tubular types', 10),
      ('capacity_ah', 'Capacity (Ah)', 'Battery capacity in ampere-hour', 'NUMBER', 'NUMBER', FALSE, 'Ah', 'Enter capacity', 'Useful for battery sizing and search', 20),
      ('voltage', 'Voltage', 'Nominal voltage of the battery', 'NUMBER', 'NUMBER', FALSE, 'V', 'Enter voltage', 'Common battery voltage rating', 30),
      ('viscosity_grade', 'Viscosity Grade', 'Lubricant viscosity grade', 'OPTION', 'SELECT', TRUE, NULL, 'Select viscosity', 'Examples: 20W40, 10W30', 10),
      ('pack_size', 'Pack Size', 'Retail pack size', 'OPTION', 'SELECT', TRUE, NULL, 'Select pack size', 'Examples: 500ML, 1L, 4L', 20),
      ('oil_type', 'Oil Type', 'Type of lubricant', 'OPTION', 'SELECT', FALSE, NULL, 'Select oil type', 'Engine oil, gear oil, coolant, etc.', 30)
  ) AS src(code, label, description, data_type, input_type, is_required, unit_label, placeholder, help_text, sort_order) ON TRUE
  ON CONFLICT (organization_id, code) DO NOTHING
  RETURNING id, organization_id, code, created_by
),
all_defs AS (
  SELECT d.id, d.organization_id, d.code, d.created_by
  FROM defs d
  UNION
  SELECT pad.id, pad.organization_id, pad.code, pad.created_by
  FROM product_attribute_definition pad
  WHERE pad.code IN ('battery_type','capacity_ah','voltage','viscosity_grade','pack_size','oil_type')
),
scopes AS (
  INSERT INTO product_attribute_scope (
    organization_id, attribute_definition_id, category_id, brand_id, created_by, updated_by
  )
  SELECT ad.organization_id, ad.id, c.id, NULL, ad.created_by, ad.created_by
  FROM all_defs ad
  JOIN category c ON c.organization_id = ad.organization_id
  WHERE (
      ad.code IN ('battery_type','capacity_ah','voltage') AND c.code = 'BATTERY'
    ) OR (
      ad.code IN ('viscosity_grade','pack_size','oil_type') AND c.code = 'LUBE'
    )
  AND NOT EXISTS (
    SELECT 1
    FROM product_attribute_scope existing
    WHERE existing.organization_id = ad.organization_id
      AND existing.attribute_definition_id = ad.id
      AND existing.category_id = c.id
      AND existing.brand_id IS NULL
  )
  RETURNING id
)
INSERT INTO product_attribute_option (
  attribute_definition_id, code, label, sort_order, is_active, created_by, updated_by
)
SELECT ad.id, opt.code, opt.label, opt.sort_order, TRUE, ad.created_by, ad.created_by
FROM all_defs ad
JOIN (
  VALUES
    ('battery_type', 'RMF', 'RMF', 10),
    ('battery_type', 'LMF', 'LMF', 20),
    ('battery_type', 'TUBULAR', 'Tubular', 30),
    ('battery_type', 'SMF', 'SMF', 40),
    ('viscosity_grade', '10W30', '10W30', 10),
    ('viscosity_grade', '15W40', '15W40', 20),
    ('viscosity_grade', '20W40', '20W40', 30),
    ('viscosity_grade', '5W30', '5W30', 40),
    ('pack_size', '500ML', '500 ML', 10),
    ('pack_size', '900ML', '900 ML', 20),
    ('pack_size', '1L', '1 L', 30),
    ('pack_size', '4L', '4 L', 40),
    ('oil_type', 'ENGINE_OIL', 'Engine Oil', 10),
    ('oil_type', 'GEAR_OIL', 'Gear Oil', 20),
    ('oil_type', 'COOLANT', 'Coolant', 30),
    ('oil_type', 'BRAKE_FLUID', 'Brake Fluid', 40)
) AS opt(attribute_code, code, label, sort_order)
  ON opt.attribute_code = ad.code
WHERE NOT EXISTS (
  SELECT 1
  FROM product_attribute_option existing
  WHERE existing.attribute_definition_id = ad.id
    AND existing.code = opt.code
);
