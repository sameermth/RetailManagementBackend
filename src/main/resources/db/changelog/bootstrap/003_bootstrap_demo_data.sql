-- Consolidated bootstrap demo/sample data for fresh databases only.

-- Generated from legacy Liquibase history. Do not use on an already-migrated database.


-- ===== SOURCE: 003_seed_erp_v4_sample_data.sql =====


-- =========================================================
-- ERP V4 demo/sample data
-- Run after:
--   001_create_erp_v4_schema.sql
--   002_seed_erp_v4_master_data.sql
-- Purpose:
--   realistic multi-tenant sample data for faster backend testing.
-- =========================================================
-- Important note:
--   In the current schema, a warehouse belongs to exactly one branch
--   (warehouse.branch_id is mandatory). So this sample models central
--   godowns per branch/org, but not a single warehouse shared by multiple
--   branches. If you want true shared-warehouse access across branches,
--   add a branch_warehouse_access mapping table in a later migration.
-- =========================================================

-- ---------------------------------------------------------
-- 1) Organizations
-- ---------------------------------------------------------
INSERT INTO organization (code, name, legal_name, phone, email, gstin, is_active)
VALUES
  ('SPC', 'Shakti Power Centre', 'Shakti Power Centre Private Limited', '+91-9876500001', 'admin@shaktipower.test', '24AAVCS1234Q1Z5', TRUE),
  ('UHL', 'Urban Home Lights', 'Urban Home Lights LLP', '+91-9876500002', 'admin@urbanlights.test', '24AAAFU5678R1Z2', TRUE)
ON CONFLICT (code) DO NOTHING;

-- ---------------------------------------------------------
-- 2) Branches
-- ---------------------------------------------------------
INSERT INTO branch (organization_id, code, name, phone, email, address_line1, city, state, postal_code, country, is_active)
SELECT o.id, x.code, x.name, x.phone, x.email, x.address, x.city, x.state, x.postal_code, 'India', TRUE
FROM organization o
JOIN (
  VALUES
    ('SPC', 'SPC-HQ',   'Mithapur Head Office', '+91-9876501001', 'hq@shaktipower.test', 'Station Road', 'Mithapur', 'Gujarat', '361345'),
    ('SPC', 'SPC-JAM',  'Jamnagar Retail',      '+91-9876501002', 'jam@shaktipower.test', 'Ranjit Road', 'Jamnagar', 'Gujarat', '361001'),
    ('SPC', 'SPC-RJK',  'Rajkot Retail',        '+91-9876501003', 'rajkot@shaktipower.test', 'Kalawad Road', 'Rajkot', 'Gujarat', '360005'),
    ('UHL', 'UHL-AHD',  'Ahmedabad Showroom',   '+91-9876502001', 'ahd@urbanlights.test', 'CG Road', 'Ahmedabad', 'Gujarat', '380009'),
    ('UHL', 'UHL-SUR',  'Surat Lighting Hub',   '+91-9876502002', 'surat@urbanlights.test', 'Ring Road', 'Surat', 'Gujarat', '395002')
) AS x(org_code, code, name, phone, email, address, city, state, postal_code)
  ON o.code = x.org_code
ON CONFLICT (organization_id, code) DO NOTHING;

-- ---------------------------------------------------------
-- 3) Warehouses
-- ---------------------------------------------------------
INSERT INTO warehouse (organization_id, branch_id, code, name, warehouse_type, is_primary, is_active)
SELECT o.id, b.id, x.code, x.name, x.warehouse_type, x.is_primary, TRUE
FROM organization o
JOIN (
  VALUES
    ('SPC', 'SPC-HQ',  'SPC-HQ-STORE',   'HQ Front Store',       'STORE',   TRUE),
    ('SPC', 'SPC-HQ',  'SPC-HQ-GODOWN',  'HQ Central Godown',    'GODOWN',  FALSE),
    ('SPC', 'SPC-HQ',  'SPC-HQ-SVC',     'HQ Service Bay',       'SERVICE', FALSE),
    ('SPC', 'SPC-JAM', 'SPC-JAM-STORE',  'Jamnagar Store',       'STORE',   TRUE),
    ('SPC', 'SPC-JAM', 'SPC-JAM-GODOWN', 'Jamnagar Back Godown', 'GODOWN',  FALSE),
    ('SPC', 'SPC-RJK', 'SPC-RJK-STORE',  'Rajkot Store',         'STORE',   TRUE),
    ('UHL', 'UHL-AHD', 'UHL-AHD-STORE',  'Ahmedabad Showroom',   'STORE',   TRUE),
    ('UHL', 'UHL-AHD', 'UHL-AHD-GODOWN', 'Ahmedabad Stock Hub',  'GODOWN',  FALSE),
    ('UHL', 'UHL-SUR', 'UHL-SUR-STORE',  'Surat Store',          'STORE',   TRUE),
    ('UHL', 'UHL-SUR', 'UHL-SUR-DMG',    'Surat Damaged Area',   'DAMAGED', FALSE)
) AS x(org_code, branch_code, code, name, warehouse_type, is_primary)
  ON o.code = x.org_code
JOIN branch b ON b.organization_id = o.id AND b.code = x.branch_code
ON CONFLICT (organization_id, code) DO NOTHING;

-- ---------------------------------------------------------
-- 4) Users (multiple owners/admin/staff/technicians per org)
-- ---------------------------------------------------------
INSERT INTO app_user (organization_id, default_branch_id, role_id, employee_code, full_name, email, phone, password_hash, is_active, joined_on)
SELECT o.id, b.id, r.id, x.employee_code, x.full_name, x.email, x.phone, '{noop}secret123', TRUE, x.joined_on
FROM organization o
JOIN (
  VALUES
    ('SPC','SPC-HQ','OWNER','SPC-OWN-01','Sameer Khan','sameer@shaktipower.test','+91-9990000001', DATE '2023-01-01'),
    ('SPC','SPC-JAM','OWNER','SPC-OWN-02','Aisha Khan','aisha@shaktipower.test','+91-9990000002', DATE '2023-02-01'),
    ('SPC','SPC-HQ','ADMIN','SPC-ADM-01','Ritesh Patel','ritesh@shaktipower.test','+91-9990000003', DATE '2023-03-15'),
    ('SPC','SPC-HQ','ACCOUNTANT','SPC-ACC-01','Kiran Shah','kiran@shaktipower.test','+91-9990000004', DATE '2023-04-01'),
    ('SPC','SPC-JAM','STORE_MANAGER','SPC-MGR-01','Bhavesh Jadeja','bhavesh@shaktipower.test','+91-9990000005', DATE '2023-04-10'),
    ('SPC','SPC-JAM','CASHIER','SPC-CAS-01','Neha Parmar','neha@shaktipower.test','+91-9990000006', DATE '2023-05-01'),
    ('SPC','SPC-HQ','PURCHASE_OPERATOR','SPC-PUR-01','Manoj Solanki','manoj@shaktipower.test','+91-9990000007', DATE '2023-05-12'),
    ('SPC','SPC-HQ','TECHNICIAN','SPC-TEC-01','Ravi Makwana','ravi@shaktipower.test','+91-9990000008', DATE '2023-06-01'),
    ('UHL','UHL-AHD','OWNER','UHL-OWN-01','Priya Mehta','priya@urbanlights.test','+91-9990000011', DATE '2023-01-10'),
    ('UHL','UHL-SUR','OWNER','UHL-OWN-02','Nirav Mehta','nirav@urbanlights.test','+91-9990000012', DATE '2023-02-10'),
    ('UHL','UHL-AHD','ADMIN','UHL-ADM-01','Jinesh Vora','jinesh@urbanlights.test','+91-9990000013', DATE '2023-03-05'),
    ('UHL','UHL-AHD','ACCOUNTANT','UHL-ACC-01','Pooja Shah','pooja@urbanlights.test','+91-9990000014', DATE '2023-03-20'),
    ('UHL','UHL-SUR','STORE_MANAGER','UHL-MGR-01','Hetal Desai','hetal@urbanlights.test','+91-9990000015', DATE '2023-04-18'),
    ('UHL','UHL-SUR','CASHIER','UHL-CAS-01','Arjun Rana','arjun@urbanlights.test','+91-9990000016', DATE '2023-05-09'),
    ('UHL','UHL-AHD','TECHNICIAN','UHL-TEC-01','Mitesh Soni','mitesh@urbanlights.test','+91-9990000017', DATE '2023-06-09')
) AS x(org_code, default_branch_code, role_code, employee_code, full_name, email, phone, joined_on)
  ON o.code = x.org_code
JOIN role r ON r.code = x.role_code
JOIN branch b ON b.organization_id = o.id AND b.code = x.default_branch_code
ON CONFLICT (organization_id, employee_code) DO NOTHING;

-- branch access (owners and admins across branches; others scoped or semi-shared)
INSERT INTO user_branch_access (user_id, branch_id, is_default)
SELECT u.id, b.id, (u.default_branch_id = b.id)
FROM app_user u
JOIN organization o ON o.id = u.organization_id
JOIN branch b ON b.organization_id = o.id
WHERE (o.code = 'SPC' AND u.employee_code IN ('SPC-OWN-01','SPC-OWN-02','SPC-ADM-01','SPC-ACC-01'))
   OR (o.code = 'UHL' AND u.employee_code IN ('UHL-OWN-01','UHL-OWN-02','UHL-ADM-01','UHL-ACC-01'))
ON CONFLICT (user_id, branch_id) DO NOTHING;

INSERT INTO user_branch_access (user_id, branch_id, is_default)
SELECT u.id, b.id, (u.default_branch_id = b.id)
FROM app_user u
JOIN branch b ON b.id IN (u.default_branch_id,
  (SELECT id FROM branch x WHERE x.organization_id = u.organization_id AND x.code = CASE WHEN u.organization_id = (SELECT id FROM organization WHERE code='SPC') THEN 'SPC-HQ' ELSE 'UHL-AHD' END))
WHERE u.employee_code IN ('SPC-MGR-01','SPC-CAS-01','SPC-PUR-01','SPC-TEC-01','UHL-MGR-01','UHL-CAS-01','UHL-TEC-01')
ON CONFLICT (user_id, branch_id) DO NOTHING;

-- ---------------------------------------------------------
-- 5) Backfill created_by / updated_by on org/branch/warehouse
-- ---------------------------------------------------------
UPDATE organization o
SET created_by = u.id, updated_by = u.id
FROM app_user u
WHERE ((o.code = 'SPC' AND u.employee_code = 'SPC-OWN-01')
    OR (o.code = 'UHL' AND u.employee_code = 'UHL-OWN-01'))
  AND (o.created_by IS NULL OR o.updated_by IS NULL);

UPDATE branch b
SET created_by = u.id, updated_by = u.id
FROM organization o, app_user u
WHERE b.organization_id = o.id
  AND ((o.code = 'SPC' AND u.employee_code = 'SPC-OWN-01')
    OR (o.code = 'UHL' AND u.employee_code = 'UHL-OWN-01'))
  AND u.organization_id = o.id
  AND (b.created_by IS NULL OR b.updated_by IS NULL);

UPDATE warehouse w
SET created_by = u.id, updated_by = u.id
FROM organization o, app_user u
WHERE w.organization_id = o.id
  AND ((o.code = 'SPC' AND u.employee_code = 'SPC-OWN-01')
    OR (o.code = 'UHL' AND u.employee_code = 'UHL-OWN-01'))
  AND u.organization_id = o.id
  AND (w.created_by IS NULL OR w.updated_by IS NULL);

-- ---------------------------------------------------------
-- 6) Org-scoped masters: tax, price list, expense category, accounts
-- ---------------------------------------------------------
INSERT INTO tax_group (organization_id, code, name, cgst_rate, sgst_rate, igst_rate, cess_rate, is_active, created_by, updated_by)
SELECT o.id, src.code, src.name, src.cgst, src.sgst, src.igst, src.cess, TRUE, owner_u.id, owner_u.id
FROM organization o
JOIN app_user owner_u ON owner_u.organization_id = o.id AND owner_u.role_id = (SELECT id FROM role WHERE code='OWNER')
JOIN (
  VALUES
    ('GST_0',  'GST 0%',  0.0, 0.0, 0.0, 0.0),
    ('GST_5',  'GST 5%',  2.5, 2.5, 5.0, 0.0),
    ('GST_12', 'GST 12%', 6.0, 6.0, 12.0, 0.0),
    ('GST_18', 'GST 18%', 9.0, 9.0, 18.0, 0.0)
) AS src(code, name, cgst, sgst, igst, cess) ON TRUE
WHERE owner_u.employee_code IN ('SPC-OWN-01','UHL-OWN-01')
ON CONFLICT (organization_id, code) DO NOTHING;

INSERT INTO price_list (organization_id, code, name, price_list_type, is_active, created_by, updated_by)
SELECT o.id, src.code, src.name, src.price_list_type, TRUE, owner_u.id, owner_u.id
FROM organization o
JOIN app_user owner_u ON owner_u.organization_id = o.id AND owner_u.role_id = (SELECT id FROM role WHERE code='OWNER')
JOIN (
  VALUES
    ('MRP', 'MRP', 'MRP'),
    ('RETAIL', 'Retail', 'RETAIL'),
    ('WHOLESALE', 'Wholesale', 'WHOLESALE'),
    ('DEALER', 'Dealer', 'DEALER')
) AS src(code, name, price_list_type) ON TRUE
WHERE owner_u.employee_code IN ('SPC-OWN-01','UHL-OWN-01')
ON CONFLICT (organization_id, code) DO NOTHING;

INSERT INTO expense_category (organization_id, code, name, is_active, created_by, updated_by)
SELECT o.id, src.code, src.name, TRUE, owner_u.id, owner_u.id
FROM organization o
JOIN app_user owner_u ON owner_u.organization_id = o.id AND owner_u.role_id = (SELECT id FROM role WHERE code='OWNER')
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
WHERE owner_u.employee_code IN ('SPC-OWN-01','UHL-OWN-01')
ON CONFLICT (organization_id, code) DO NOTHING;

INSERT INTO account (organization_id, code, name, account_type, is_system, is_active, created_by, updated_by)
SELECT o.id, src.code, src.name, src.account_type, TRUE, TRUE, owner_u.id, owner_u.id
FROM organization o
JOIN app_user owner_u ON owner_u.organization_id = o.id AND owner_u.role_id = (SELECT id FROM role WHERE code='OWNER')
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
    ('PURCHASES', 'Purchases', 'EXPENSE'),
    ('EXPENSE_CONTROL', 'Operating Expenses', 'EXPENSE'),
    ('STOCK_LOSS', 'Stock Adjustment Loss', 'EXPENSE'),
    ('STOCK_GAIN', 'Stock Adjustment Gain', 'INCOME')
) AS src(code, name, account_type) ON TRUE
WHERE owner_u.employee_code IN ('SPC-OWN-01','UHL-OWN-01')
ON CONFLICT (organization_id, code) DO NOTHING;

-- app settings + document sequences
INSERT INTO app_setting (organization_id, branch_id, setting_key, setting_value, created_by, updated_by)
SELECT o.id, NULL, 'inventory.negative_stock_policy', '{"mode":"WARN"}'::jsonb, u.id, u.id
FROM organization o JOIN app_user u ON u.organization_id=o.id AND u.employee_code IN ('SPC-OWN-01','UHL-OWN-01')
ON CONFLICT (organization_id, branch_id, setting_key) DO NOTHING;

INSERT INTO app_setting (organization_id, branch_id, setting_key, setting_value, created_by, updated_by)
SELECT o.id, NULL, 'service.default_warranty_months', '{"months":24}'::jsonb, u.id, u.id
FROM organization o JOIN app_user u ON u.organization_id=o.id AND u.employee_code IN ('SPC-OWN-01','UHL-OWN-01')
ON CONFLICT (organization_id, branch_id, setting_key) DO NOTHING;

INSERT INTO document_sequence (organization_id, branch_id, document_type, prefix, next_number, padding_length, reset_policy, created_by, updated_by)
SELECT o.id, NULL, src.document_type, src.prefix, src.next_num, 5, 'YEARLY', u.id, u.id
FROM organization o
JOIN app_user u ON u.organization_id = o.id AND u.employee_code IN ('SPC-OWN-01','UHL-OWN-01')
JOIN (
  VALUES
    ('PURCHASE_ORDER','PO', 1003),
    ('PURCHASE_RECEIPT','GRN', 1003),
    ('SALES_INVOICE','INV', 1004),
    ('CUSTOMER_RECEIPT','RCPT', 1003),
    ('SUPPLIER_PAYMENT','PAY', 1002),
    ('SERVICE_TICKET','SVC', 1002),
    ('WARRANTY_CLAIM','WCL', 1002),
    ('EXPENSE','EXP', 1003),
    ('RECURRING_EXPENSE','REXP', 1002),
    ('VOUCHER','VCH', 1010)
) AS src(document_type, prefix, next_num) ON TRUE
ON CONFLICT (organization_id, branch_id, document_type) DO NOTHING;

-- ---------------------------------------------------------
-- 7) Party masters
-- ---------------------------------------------------------
INSERT INTO customer (organization_id, branch_id, customer_code, full_name, phone, email, gstin, credit_limit, status, notes, created_by, updated_by)
SELECT o.id, b.id, x.customer_code, x.full_name, x.phone, x.email, x.gstin, x.credit_limit, 'ACTIVE', x.notes, u.id, u.id
FROM organization o
JOIN (
  VALUES
    ('SPC','SPC-JAM','CUST-SPC-001','Maa Electronics','+91-9811100001','procurement@maaelectronics.test','24AABCM1111D1Z0',200000,'Dealer customer','SPC-MGR-01'),
    ('SPC','SPC-JAM','CUST-SPC-002','Jay Ambe Cold Drinks','+91-9811100002','owner@jayambe.test',NULL,50000,'Retail repeat customer','SPC-CAS-01'),
    ('SPC','SPC-RJK','CUST-SPC-003','Shiv Enterprise','+91-9811100003','accounts@shiventerprise.test','24AACCS2222L1Z8',150000,'Credit customer','SPC-ADM-01'),
    ('SPC','SPC-HQ','CUST-SPC-004','Ocean Residency Society','+91-9811100004','admin@oceanresidency.test',NULL,300000,'AMC / service customer','SPC-OWN-01'),
    ('UHL','UHL-AHD','CUST-UHL-001','Bright Homes','+91-9822200001','hello@brighthomes.test','24AATFB3333P1Z3',100000,'Interior partner','UHL-ADM-01'),
    ('UHL','UHL-SUR','CUST-UHL-002','Lumen Mart','+91-9822200002','purchase@lumenmart.test',NULL,75000,'Retail chain counter','UHL-MGR-01'),
    ('UHL','UHL-SUR','CUST-UHL-003','Vijay Traders','+91-9822200003','vijay@traders.test','24AACCV4444N1ZA',120000,'Wholesale lighting buyer','UHL-CAS-01')
) AS x(org_code, branch_code, customer_code, full_name, phone, email, gstin, credit_limit, notes, created_by_emp)
  ON o.code = x.org_code
JOIN branch b ON b.organization_id = o.id AND b.code = x.branch_code
JOIN app_user u ON u.organization_id = o.id AND u.employee_code = x.created_by_emp
ON CONFLICT (organization_id, customer_code) DO NOTHING;

INSERT INTO customer_address (customer_id, address_type, line1, line2, city, state, postal_code, country, is_default, created_by, updated_by)
SELECT c.id, 'BILLING', 'Main Market', NULL, COALESCE(b.city,'Jamnagar'), COALESCE(b.state,'Gujarat'), COALESCE(b.postal_code,'361001'), 'India', TRUE, c.created_by, c.updated_by
FROM customer c
LEFT JOIN branch b ON b.id = c.branch_id
ON CONFLICT DO NOTHING;

INSERT INTO supplier (organization_id, branch_id, supplier_code, name, phone, email, gstin, status, notes, created_by, updated_by)
SELECT o.id, b.id, x.supplier_code, x.name, x.phone, x.email, x.gstin, 'ACTIVE', x.notes, u.id, u.id
FROM organization o
JOIN (
  VALUES
    ('SPC','SPC-HQ','SUP-SPC-001','Exide Industrial Supplies','+91-9833300001','sales@exide.test','24AACES5555B1ZZ','Battery supplier','SPC-PUR-01'),
    ('SPC','SPC-HQ','SUP-SPC-002','Luminous Power Tech','+91-9833300002','partner@luminous.test','24AACCL6666M1ZX','Inverter supplier','SPC-PUR-01'),
    ('SPC','SPC-HQ','SUP-SPC-003','Polycab Distribution','+91-9833300003','wire@polycab.test','24AACCP7777Q1Z7','Wire and cable supplier','SPC-PUR-01'),
    ('UHL','UHL-AHD','SUP-UHL-001','Philips Lighting India','+91-9844400001','b2b@philips.test','24AACCP8888W1Z5','Lighting supplier','UHL-ADM-01'),
    ('UHL','UHL-AHD','SUP-UHL-002','Havells Trade Channel','+91-9844400002','trade@havells.test','24AABCH9999T1Z4','Electrical goods supplier','UHL-ADM-01')
) AS x(org_code, branch_code, supplier_code, name, phone, email, gstin, notes, created_by_emp)
  ON o.code = x.org_code
JOIN branch b ON b.organization_id = o.id AND b.code = x.branch_code
JOIN app_user u ON u.organization_id = o.id AND u.employee_code = x.created_by_emp
ON CONFLICT (organization_id, supplier_code) DO NOTHING;

INSERT INTO supplier_address (supplier_id, address_type, line1, line2, city, state, postal_code, country, is_default, created_by, updated_by)
SELECT s.id, 'BILLING', 'Industrial Estate', NULL, 'Ahmedabad', 'Gujarat', '380001', 'India', TRUE, s.created_by, s.updated_by
FROM supplier s
ON CONFLICT DO NOTHING;

INSERT INTO distributor (organization_id, branch_id, distributor_code, name, phone, email, gstin, status, notes, created_by, updated_by)
SELECT o.id, b.id, x.distributor_code, x.name, x.phone, x.email, x.gstin, 'ACTIVE', x.notes, u.id, u.id
FROM organization o
JOIN (
  VALUES
    ('SPC','SPC-HQ','DST-SPC-001','Gujarat Energy Distribution','+91-9855500001','warranty@ged.test','24AACCG1111F1Z2','Warranty routing partner','SPC-ADM-01'),
    ('UHL','UHL-AHD','DST-UHL-001','West India Electrical Distributors','+91-9855500002','claims@wied.test','24AACCW2222H1Z9','Regional lighting distributor','UHL-ADM-01')
) AS x(org_code, branch_code, distributor_code, name, phone, email, gstin, notes, created_by_emp)
  ON o.code = x.org_code
JOIN branch b ON b.organization_id = o.id AND b.code = x.branch_code
JOIN app_user u ON u.organization_id = o.id AND u.employee_code = x.created_by_emp
ON CONFLICT (organization_id, distributor_code) DO NOTHING;

INSERT INTO distributor_address (distributor_id, address_type, line1, line2, city, state, postal_code, country, is_default, created_by, updated_by)
SELECT d.id, 'BILLING', 'Corporate Park', NULL, 'Vadodara', 'Gujarat', '390001', 'India', TRUE, d.created_by, d.updated_by
FROM distributor d
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------
-- 8) Categories, brands, products, UOM conversions, pricing
-- ---------------------------------------------------------
INSERT INTO category (organization_id, parent_category_id, name, code, is_active, created_by, updated_by)
SELECT o.id, NULL, x.name, x.code, TRUE, u.id, u.id
FROM organization o
JOIN app_user u ON u.organization_id = o.id AND u.employee_code IN ('SPC-OWN-01','UHL-OWN-01')
JOIN (
  VALUES ('POWER','Power Solutions'), ('BATTERY','Battery'), ('WIRE','Wires & Cables'), ('LUBE','Lubricants'), ('LIGHT','Lighting')
) AS x(code, name) ON TRUE
ON CONFLICT (organization_id, parent_category_id, name) DO NOTHING;

INSERT INTO brand (organization_id, name, code, is_active, created_by, updated_by)
SELECT o.id, x.name, x.code, TRUE, u.id, u.id
FROM organization o
JOIN app_user u ON u.organization_id = o.id AND u.employee_code IN ('SPC-OWN-01','UHL-OWN-01')
JOIN (
  VALUES ('LUMINOUS','LUMI'), ('EXIDE','EXID'), ('POLYCAB','POLY'), ('SERVO','SERV'), ('PHILIPS','PHIL'), ('HAVELLS','HAVL')
) AS x(name, code) ON TRUE
ON CONFLICT (organization_id, name) DO NOTHING;

-- SPC products
INSERT INTO product (
  organization_id, category_id, brand_id, base_uom_id, tax_group_id, sku, name, description,
  inventory_tracking_mode, serial_tracking_enabled, batch_tracking_enabled, expiry_tracking_enabled,
  fractional_quantity_allowed, min_stock_base_qty, reorder_level_base_qty, is_service_item, is_active,
  created_by, updated_by
)
SELECT o.id, c.id, br.id, uom.id, tg.id, x.sku, x.name, x.description,
       x.tracking_mode, x.serial_enabled, x.batch_enabled, x.expiry_enabled,
       x.fractional_allowed, x.min_stock, x.reorder_level, x.is_service_item, TRUE,
       usr.id, usr.id
FROM organization o
JOIN app_user usr ON usr.organization_id = o.id AND usr.employee_code = 'SPC-OWN-01'
JOIN (
  VALUES
    ('INV-900VA', 'EcoVolt 900VA Inverter', 'Home inverter with serial tracking', 'POWER', 'LUMI', 'PCS', 'GST_18', 'SERIALIZED', TRUE, FALSE, FALSE, FALSE, 2, 5, FALSE),
    ('BAT-150AH', 'Tubular Battery 150AH', 'Battery with serial tracking and warranty', 'BATTERY', 'EXID', 'PCS', 'GST_18', 'SERIALIZED', TRUE, FALSE, FALSE, FALSE, 3, 8, FALSE),
    ('WIRE-6SQ', 'Copper Wire 6 Sqmm', 'Wire sold by metre', 'WIRE', 'POLY', 'MTR', 'GST_18', 'FRACTIONAL', FALSE, FALSE, FALSE, TRUE, 100, 300, FALSE),
    ('OIL-1LTR', 'Servo Battery Distilled Water 1L', 'Consumable sold by bottle / carton', 'LUBE', 'SERV', 'LTR', 'GST_18', 'BATCHED', FALSE, TRUE, FALSE, TRUE, 20, 50, FALSE),
    ('BULB-9W', 'LED Bulb 9W', 'Bulb sold in piece/dozen/carton', 'LIGHT', 'PHIL', 'PCS', 'GST_12', 'MIXED_UOM', FALSE, TRUE, FALSE, FALSE, 24, 120, FALSE)
 ) AS x(sku, name, description, category_code, brand_code, base_uom_code, tax_code, tracking_mode, serial_enabled, batch_enabled, expiry_enabled, fractional_allowed, min_stock, reorder_level, is_service_item) ON TRUE
JOIN category c ON c.organization_id = o.id AND c.code = x.category_code
JOIN brand br ON br.organization_id = o.id AND br.code = x.brand_code
JOIN uom ON uom.code = x.base_uom_code
JOIN tax_group tg ON tg.organization_id = o.id AND tg.code = x.tax_code
WHERE o.code = 'SPC'
ON CONFLICT (organization_id, sku) DO NOTHING;

-- UHL products
INSERT INTO product (
  organization_id, category_id, brand_id, base_uom_id, tax_group_id, sku, name, description,
  inventory_tracking_mode, serial_tracking_enabled, batch_tracking_enabled, expiry_tracking_enabled,
  fractional_quantity_allowed, min_stock_base_qty, reorder_level_base_qty, is_service_item, is_active,
  created_by, updated_by
)
SELECT o.id, c.id, br.id, uom.id, tg.id, x.sku, x.name, x.description,
       x.tracking_mode, x.serial_enabled, x.batch_enabled, x.expiry_enabled,
       x.fractional_allowed, x.min_stock, x.reorder_level, x.is_service_item, TRUE,
       usr.id, usr.id
FROM organization o
JOIN app_user usr ON usr.organization_id = o.id AND usr.employee_code = 'UHL-OWN-01'
JOIN (
  VALUES
    ('LIGHT-PANEL18', 'Slim Panel Light 18W', 'Ceiling panel light', 'LIGHT', 'PHIL', 'PCS', 'GST_18', 'SIMPLE', FALSE, FALSE, FALSE, FALSE, 20, 60, FALSE),
    ('FAN-CLG-1200', 'Ceiling Fan 1200mm', 'Fan with serial tracking', 'LIGHT', 'HAVL', 'PCS', 'GST_18', 'SERIALIZED', TRUE, FALSE, FALSE, FALSE, 4, 10, FALSE),
    ('WIRE-1.5SQ', 'House Wire 1.5 Sqmm', 'Electrical wire sold by metre', 'WIRE', 'HAVL', 'MTR', 'GST_18', 'FRACTIONAL', FALSE, FALSE, FALSE, TRUE, 200, 500, FALSE),
    ('STRIP-LED5M', 'LED Strip 5M', 'Strip lighting with batch control', 'LIGHT', 'PHIL', 'PCS', 'GST_18', 'BATCHED', FALSE, TRUE, FALSE, FALSE, 10, 40, FALSE),
    ('SWITCH-6A', 'Modular Switch 6A', 'Switch sold in box/carton', 'LIGHT', 'HAVL', 'PCS', 'GST_18', 'MIXED_UOM', FALSE, TRUE, FALSE, FALSE, 100, 500, FALSE)
 ) AS x(sku, name, description, category_code, brand_code, base_uom_code, tax_code, tracking_mode, serial_enabled, batch_enabled, expiry_enabled, fractional_allowed, min_stock, reorder_level, is_service_item) ON TRUE
JOIN category c ON c.organization_id = o.id AND c.code = x.category_code
JOIN brand br ON br.organization_id = o.id AND br.code = x.brand_code
JOIN uom ON uom.code = x.base_uom_code
JOIN tax_group tg ON tg.organization_id = o.id AND tg.code = x.tax_code
WHERE o.code = 'UHL'
ON CONFLICT (organization_id, sku) DO NOTHING;

-- UOM conversions for mixed/fractional goods
INSERT INTO product_uom_conversion (product_id, from_uom_id, to_uom_id, multiplier, is_purchase_uom, is_sales_uom, is_default, created_by, updated_by)
SELECT p.id, fu.id, tu.id, x.multiplier, x.is_purchase_uom, x.is_sales_uom, x.is_default, p.created_by, p.updated_by
FROM product p
JOIN organization o ON o.id = p.organization_id
JOIN (
  VALUES
    ('SPC','BULB-9W','DOZEN','PCS',12, TRUE, TRUE, FALSE),
    ('SPC','BULB-9W','CARTON','PCS',120, TRUE, FALSE, FALSE),
    ('SPC','OIL-1LTR','ML','LTR',0.001, FALSE, TRUE, FALSE),
    ('UHL','SWITCH-6A','BOX','PCS',20, TRUE, TRUE, FALSE),
    ('UHL','SWITCH-6A','CARTON','PCS',200, TRUE, FALSE, FALSE),
    ('UHL','STRIP-LED5M','BOX','PCS',10, TRUE, FALSE, FALSE)
) AS x(org_code, sku, from_uom, to_uom, multiplier, is_purchase_uom, is_sales_uom, is_default)
  ON o.code = x.org_code AND p.sku = x.sku
JOIN uom fu ON fu.code = x.from_uom
JOIN uom tu ON tu.code = x.to_uom
ON CONFLICT DO NOTHING;

-- price list items
INSERT INTO price_list_item (price_list_id, product_id, uom_id, price, created_by, updated_by)
SELECT pl.id, p.id, u.id, x.price, p.created_by, p.updated_by
FROM organization o
JOIN (
  VALUES
    ('SPC','RETAIL','INV-900VA','PCS',6500),
    ('SPC','RETAIL','BAT-150AH','PCS',12800),
    ('SPC','RETAIL','WIRE-6SQ','MTR',92),
    ('SPC','RETAIL','OIL-1LTR','LTR',55),
    ('SPC','RETAIL','BULB-9W','PCS',95),
    ('SPC','WHOLESALE','BULB-9W','DOZEN',1020),
    ('UHL','RETAIL','LIGHT-PANEL18','PCS',650),
    ('UHL','RETAIL','FAN-CLG-1200','PCS',2200),
    ('UHL','RETAIL','WIRE-1.5SQ','MTR',24),
    ('UHL','RETAIL','STRIP-LED5M','PCS',410),
    ('UHL','WHOLESALE','SWITCH-6A','BOX',780)
) AS x(org_code, price_list_code, sku, uom_code, price)
  ON o.code = x.org_code
JOIN price_list pl ON pl.organization_id = o.id AND pl.code = x.price_list_code
JOIN product p ON p.organization_id = o.id AND p.sku = x.sku
JOIN uom u ON u.code = x.uom_code
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------
-- 9) Opening inventory entities: batches, serials, balances, movements
-- ---------------------------------------------------------
-- batches
INSERT INTO inventory_batch (organization_id, product_id, batch_number, manufacturer_batch_number, manufactured_on, expiry_on, status, created_by, updated_by)
SELECT o.id, p.id, x.batch_number, x.manufacturer_batch_number, x.manufactured_on, x.expiry_on, 'ACTIVE', u.id, u.id
FROM organization o
JOIN (
  VALUES
    ('SPC','OIL-1LTR','OIL-B-2025-01','SERVO-2025-01', DATE '2025-01-01', DATE '2027-01-01','SPC-PUR-01'),
    ('SPC','BULB-9W','BULB-B-2025-02','PHIL-2025-02', DATE '2025-02-10', NULL,'SPC-PUR-01'),
    ('UHL','STRIP-LED5M','STRIP-B-2025-03','PHIL-2025-03', DATE '2025-03-01', NULL,'UHL-ADM-01'),
    ('UHL','SWITCH-6A','SWITCH-B-2025-02','HAVL-2025-02', DATE '2025-02-15', NULL,'UHL-ADM-01')
) AS x(org_code, sku, batch_number, manufacturer_batch_number, manufactured_on, expiry_on, created_by_emp)
  ON o.code = x.org_code
JOIN product p ON p.organization_id = o.id AND p.sku = x.sku
JOIN app_user u ON u.organization_id = o.id AND u.employee_code = x.created_by_emp
ON CONFLICT (organization_id, product_id, batch_number) DO NOTHING;

-- serials from opening stock / initial inward
INSERT INTO serial_number (organization_id, product_id, batch_id, serial_number, manufacturer_serial_number, status, current_warehouse_id, current_customer_id, warranty_start_date, warranty_end_date, created_by, updated_by)
SELECT o.id, p.id, ib.id, x.serial_number, x.manufacturer_serial, x.status, w.id, NULL, NULL, NULL, u.id, u.id
FROM organization o
JOIN (
  VALUES
    ('SPC','INV-900VA',NULL,'INV900-0001','LUM-INV900-0001','IN_STOCK','SPC-HQ-GODOWN','SPC-PUR-01'),
    ('SPC','INV-900VA',NULL,'INV900-0002','LUM-INV900-0002','IN_STOCK','SPC-HQ-GODOWN','SPC-PUR-01'),
    ('SPC','INV-900VA',NULL,'INV900-0003','LUM-INV900-0003','IN_STOCK','SPC-JAM-STORE','SPC-PUR-01'),
    ('SPC','BAT-150AH',NULL,'BAT150-0101','EXD-BAT150-0101','IN_STOCK','SPC-HQ-GODOWN','SPC-PUR-01'),
    ('SPC','BAT-150AH',NULL,'BAT150-0102','EXD-BAT150-0102','IN_STOCK','SPC-HQ-GODOWN','SPC-PUR-01'),
    ('SPC','BAT-150AH',NULL,'BAT150-0103','EXD-BAT150-0103','IN_STOCK','SPC-JAM-STORE','SPC-PUR-01'),
    ('UHL','FAN-CLG-1200',NULL,'FAN1200-0201','HAV-FAN1200-0201','IN_STOCK','UHL-AHD-GODOWN','UHL-ADM-01'),
    ('UHL','FAN-CLG-1200',NULL,'FAN1200-0202','HAV-FAN1200-0202','IN_STOCK','UHL-SUR-STORE','UHL-ADM-01'),
    ('UHL','FAN-CLG-1200',NULL,'FAN1200-0203','HAV-FAN1200-0203','IN_STOCK','UHL-SUR-STORE','UHL-ADM-01')
) AS x(org_code, sku, batch_number, serial_number, manufacturer_serial, status, warehouse_code, created_by_emp)
  ON o.code = x.org_code
JOIN product p ON p.organization_id = o.id AND p.sku = x.sku
LEFT JOIN inventory_batch ib ON ib.organization_id = o.id AND ib.product_id = p.id AND ib.batch_number = x.batch_number
JOIN warehouse w ON w.organization_id = o.id AND w.code = x.warehouse_code
JOIN app_user u ON u.organization_id = o.id AND u.employee_code = x.created_by_emp
ON CONFLICT (organization_id, product_id, serial_number) DO NOTHING;

-- inventory balance rows
INSERT INTO inventory_balance (organization_id, branch_id, warehouse_id, product_id, batch_id, on_hand_base_quantity, reserved_base_quantity, available_base_quantity, avg_cost, created_by, updated_by)
SELECT o.id, b.id, w.id, p.id, ib.id, x.on_hand, x.reserved, x.available, x.avg_cost, u.id, u.id
FROM organization o
JOIN (
  VALUES
    ('SPC','SPC-HQ-GODOWN','INV-900VA',NULL,2.0,0.0,2.0,5000.000000,'SPC-PUR-01'),
    ('SPC','SPC-JAM-STORE','INV-900VA',NULL,1.0,0.0,1.0,5050.000000,'SPC-PUR-01'),
    ('SPC','SPC-HQ-GODOWN','BAT-150AH',NULL,2.0,0.0,2.0,9800.000000,'SPC-PUR-01'),
    ('SPC','SPC-JAM-STORE','BAT-150AH',NULL,1.0,0.0,1.0,9900.000000,'SPC-PUR-01'),
    ('SPC','SPC-HQ-GODOWN','WIRE-6SQ',NULL,500.0,0.0,500.0,70.000000,'SPC-PUR-01'),
    ('SPC','SPC-HQ-GODOWN','OIL-1LTR','OIL-B-2025-01',120.0,0.0,120.0,35.000000,'SPC-PUR-01'),
    ('SPC','SPC-JAM-STORE','BULB-9W','BULB-B-2025-02',240.0,0.0,240.0,60.000000,'SPC-PUR-01'),
    ('UHL','UHL-AHD-GODOWN','FAN-CLG-1200',NULL,1.0,0.0,1.0,1650.000000,'UHL-ADM-01'),
    ('UHL','UHL-SUR-STORE','FAN-CLG-1200',NULL,2.0,0.0,2.0,1680.000000,'UHL-ADM-01'),
    ('UHL','UHL-AHD-GODOWN','LIGHT-PANEL18',NULL,80.0,0.0,80.0,420.000000,'UHL-ADM-01'),
    ('UHL','UHL-SUR-STORE','WIRE-1.5SQ',NULL,900.0,0.0,900.0,15.500000,'UHL-ADM-01'),
    ('UHL','UHL-AHD-GODOWN','STRIP-LED5M','STRIP-B-2025-03',70.0,0.0,70.0,250.000000,'UHL-ADM-01'),
    ('UHL','UHL-SUR-STORE','SWITCH-6A','SWITCH-B-2025-02',600.0,0.0,600.0,22.000000,'UHL-ADM-01')
) AS x(org_code, warehouse_code, sku, batch_number, on_hand, reserved, available, avg_cost, created_by_emp)
  ON o.code = x.org_code
JOIN warehouse w ON w.organization_id = o.id AND w.code = x.warehouse_code
JOIN branch b ON b.id = w.branch_id
JOIN product p ON p.organization_id = o.id AND p.sku = x.sku
LEFT JOIN inventory_batch ib ON ib.organization_id = o.id AND ib.product_id = p.id AND ib.batch_number IS NOT DISTINCT FROM x.batch_number
JOIN app_user u ON u.organization_id = o.id AND u.employee_code = x.created_by_emp
ON CONFLICT (organization_id, branch_id, warehouse_id, product_id, batch_id) DO NOTHING;

-- opening stock movements
INSERT INTO stock_movement (organization_id, branch_id, warehouse_id, product_id, movement_type, reference_type, reference_id, reference_number, direction, uom_id, quantity, base_quantity, unit_cost, total_cost, movement_at, created_by, updated_by)
SELECT o.id, b.id, w.id, p.id, 'OPENING_STOCK', 'OPENING', 1, 'OPEN-2025', 'IN', u.id, x.quantity, x.base_quantity, x.unit_cost, x.total_cost, x.movement_at, usr.id, usr.id
FROM organization o
JOIN (
  VALUES
    ('SPC','SPC-HQ-GODOWN','INV-900VA','PCS',2,2,5000,10000,TIMESTAMPTZ '2025-03-01 09:00:00+05:30','SPC-PUR-01'),
    ('SPC','SPC-JAM-STORE','INV-900VA','PCS',1,1,5050,5050,TIMESTAMPTZ '2025-03-01 09:10:00+05:30','SPC-PUR-01'),
    ('SPC','SPC-HQ-GODOWN','BAT-150AH','PCS',2,2,9800,19600,TIMESTAMPTZ '2025-03-01 09:15:00+05:30','SPC-PUR-01'),
    ('SPC','SPC-JAM-STORE','BAT-150AH','PCS',1,1,9900,9900,TIMESTAMPTZ '2025-03-01 09:20:00+05:30','SPC-PUR-01'),
    ('SPC','SPC-HQ-GODOWN','WIRE-6SQ','MTR',500,500,70,35000,TIMESTAMPTZ '2025-03-01 09:30:00+05:30','SPC-PUR-01'),
    ('SPC','SPC-HQ-GODOWN','OIL-1LTR','LTR',120,120,35,4200,TIMESTAMPTZ '2025-03-01 09:40:00+05:30','SPC-PUR-01'),
    ('SPC','SPC-JAM-STORE','BULB-9W','PCS',240,240,60,14400,TIMESTAMPTZ '2025-03-01 09:50:00+05:30','SPC-PUR-01'),
    ('UHL','UHL-AHD-GODOWN','FAN-CLG-1200','PCS',1,1,1650,1650,TIMESTAMPTZ '2025-03-01 10:00:00+05:30','UHL-ADM-01'),
    ('UHL','UHL-SUR-STORE','FAN-CLG-1200','PCS',2,2,1680,3360,TIMESTAMPTZ '2025-03-01 10:10:00+05:30','UHL-ADM-01'),
    ('UHL','UHL-AHD-GODOWN','LIGHT-PANEL18','PCS',80,80,420,33600,TIMESTAMPTZ '2025-03-01 10:20:00+05:30','UHL-ADM-01'),
    ('UHL','UHL-SUR-STORE','WIRE-1.5SQ','MTR',900,900,15.5,13950,TIMESTAMPTZ '2025-03-01 10:30:00+05:30','UHL-ADM-01'),
    ('UHL','UHL-AHD-GODOWN','STRIP-LED5M','PCS',70,70,250,17500,TIMESTAMPTZ '2025-03-01 10:40:00+05:30','UHL-ADM-01'),
    ('UHL','UHL-SUR-STORE','SWITCH-6A','PCS',600,600,22,13200,TIMESTAMPTZ '2025-03-01 10:50:00+05:30','UHL-ADM-01')
) AS x(org_code, warehouse_code, sku, uom_code, quantity, base_quantity, unit_cost, total_cost, movement_at, created_by_emp)
  ON o.code = x.org_code
JOIN warehouse w ON w.organization_id = o.id AND w.code = x.warehouse_code
JOIN branch b ON b.id = w.branch_id
JOIN product p ON p.organization_id = o.id AND p.sku = x.sku
JOIN uom u ON u.code = x.uom_code
JOIN app_user usr ON usr.organization_id = o.id AND usr.employee_code = x.created_by_emp
ON CONFLICT DO NOTHING;

-- attach opening serials to opening stock movements
INSERT INTO stock_movement_serial (stock_movement_id, serial_number_id, created_by, updated_by)
SELECT sm.id, sn.id, sm.created_by, sm.updated_by
FROM stock_movement sm
JOIN product p ON p.id = sm.product_id
JOIN serial_number sn ON sn.product_id = p.id AND sn.current_warehouse_id = sm.warehouse_id
WHERE sm.movement_type = 'OPENING_STOCK'
  AND sm.reference_number = 'OPEN-2025'
  AND p.sku IN ('INV-900VA','BAT-150AH','FAN-CLG-1200')
ON CONFLICT DO NOTHING;

INSERT INTO stock_movement_batch (stock_movement_id, batch_id, quantity, base_quantity, created_by, updated_by)
SELECT sm.id, ib.id, sm.quantity, sm.base_quantity, sm.created_by, sm.updated_by
FROM stock_movement sm
JOIN inventory_batch ib ON ib.product_id = sm.product_id
WHERE sm.movement_type = 'OPENING_STOCK'
  AND sm.reference_number = 'OPEN-2025'
  AND ((ib.batch_number = 'OIL-B-2025-01' AND sm.product_id = ib.product_id)
    OR (ib.batch_number = 'BULB-B-2025-02' AND sm.product_id = ib.product_id)
    OR (ib.batch_number = 'STRIP-B-2025-03' AND sm.product_id = ib.product_id)
    OR (ib.batch_number = 'SWITCH-B-2025-02' AND sm.product_id = ib.product_id))
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------
-- 10) Purchase flow samples
-- ---------------------------------------------------------
INSERT INTO purchase_order (organization_id, branch_id, supplier_id, po_number, po_date, status, subtotal, tax_amount, total_amount, remarks, submitted_at, submitted_by, approved_at, approved_by, created_by, updated_by)
SELECT o.id, b.id, s.id, x.po_number, x.po_date, x.status, x.subtotal, x.tax_amount, x.total_amount, x.remarks,
       x.submitted_at, submit_u.id, x.approved_at, approve_u.id, create_u.id, create_u.id
FROM organization o
JOIN (
  VALUES
    ('SPC','SPC-HQ','SUP-SPC-002','PO-SPC-00001',DATE '2026-02-25','APPROVED',26000,4680,30680,'Monthly inverter and battery replenishment',TIMESTAMPTZ '2026-02-25 11:00:00+05:30','SPC-PUR-01',TIMESTAMPTZ '2026-02-25 15:00:00+05:30','SPC-OWN-01','SPC-PUR-01'),
    ('UHL','UHL-AHD','SUP-UHL-001','PO-UHL-00001',DATE '2026-02-27','APPROVED',47000,8460,55460,'Panel and strip procurement',TIMESTAMPTZ '2026-02-27 10:30:00+05:30','UHL-ADM-01',TIMESTAMPTZ '2026-02-27 13:00:00+05:30','UHL-OWN-01','UHL-ADM-01')
) AS x(org_code, branch_code, supplier_code, po_number, po_date, status, subtotal, tax_amount, total_amount, remarks, submitted_at, submitted_by_emp, approved_at, approved_by_emp, created_by_emp)
  ON o.code = x.org_code
JOIN branch b ON b.organization_id = o.id AND b.code = x.branch_code
JOIN supplier s ON s.organization_id = o.id AND s.supplier_code = x.supplier_code
JOIN app_user create_u ON create_u.organization_id = o.id AND create_u.employee_code = x.created_by_emp
LEFT JOIN app_user submit_u ON submit_u.organization_id = o.id AND submit_u.employee_code = x.submitted_by_emp
LEFT JOIN app_user approve_u ON approve_u.organization_id = o.id AND approve_u.employee_code = x.approved_by_emp
ON CONFLICT (organization_id, po_number) DO NOTHING;

INSERT INTO purchase_order_line (purchase_order_id, product_id, uom_id, quantity, base_quantity, unit_price, tax_rate, line_amount, received_base_quantity, created_by, updated_by)
SELECT po.id, p.id, u.id, x.quantity, x.base_quantity, x.unit_price, x.tax_rate, x.line_amount, x.received_base_quantity, po.created_by, po.updated_by
FROM purchase_order po
JOIN organization o ON o.id = po.organization_id
JOIN (
  VALUES
    ('PO-SPC-00001','INV-900VA','PCS',2,2,4900,18,9800,2),
    ('PO-SPC-00001','BAT-150AH','PCS',1,1,9800,18,9800,1),
    ('PO-SPC-00001','WIRE-6SQ','MTR',100,100,64,18,6400,100),
    ('PO-UHL-00001','LIGHT-PANEL18','PCS',50,50,410,18,20500,50),
    ('PO-UHL-00001','STRIP-LED5M','PCS',40,40,240,18,9600,40),
    ('PO-UHL-00001','SWITCH-6A','BOX',10,200,380,18,3800,200),
    ('PO-UHL-00001','FAN-CLG-1200','PCS',3,3,1650,18,4950,3)
 ) AS x(po_number, sku, uom_code, quantity, base_quantity, unit_price, tax_rate, line_amount, received_base_quantity)
  ON po.po_number = x.po_number
JOIN product p ON p.organization_id = o.id AND p.sku = x.sku
JOIN uom u ON u.code = x.uom_code
ON CONFLICT DO NOTHING;

INSERT INTO purchase_receipt (organization_id, branch_id, warehouse_id, purchase_order_id, supplier_id, receipt_number, receipt_date, status, subtotal, tax_amount, total_amount, remarks, posted_at, created_by, updated_by)
SELECT o.id, b.id, w.id, po.id, s.id, x.receipt_number, x.receipt_date, x.status, x.subtotal, x.tax_amount, x.total_amount, x.remarks, x.posted_at, u.id, u.id
FROM organization o
JOIN (
  VALUES
    ('SPC','SPC-HQ','SPC-HQ-GODOWN','PO-SPC-00001','SUP-SPC-002','GRN-SPC-00001',DATE '2026-02-28','POSTED',26000,4680,30680,'Goods received complete',TIMESTAMPTZ '2026-02-28 17:45:00+05:30','SPC-PUR-01'),
    ('UHL','UHL-AHD','UHL-AHD-GODOWN','PO-UHL-00001','SUP-UHL-001','GRN-UHL-00001',DATE '2026-03-01','POSTED',38850,6993,45843,'First inward completed',TIMESTAMPTZ '2026-03-01 18:10:00+05:30','UHL-ADM-01')
) AS x(org_code, branch_code, warehouse_code, po_number, supplier_code, receipt_number, receipt_date, status, subtotal, tax_amount, total_amount, remarks, posted_at, created_by_emp)
  ON o.code = x.org_code
JOIN branch b ON b.organization_id = o.id AND b.code = x.branch_code
JOIN warehouse w ON w.organization_id = o.id AND w.code = x.warehouse_code
JOIN purchase_order po ON po.organization_id = o.id AND po.po_number = x.po_number
JOIN supplier s ON s.organization_id = o.id AND s.supplier_code = x.supplier_code
JOIN app_user u ON u.organization_id = o.id AND u.employee_code = x.created_by_emp
ON CONFLICT (organization_id, receipt_number) DO NOTHING;

INSERT INTO purchase_receipt_line (purchase_receipt_id, purchase_order_line_id, product_id, uom_id, quantity, base_quantity, unit_cost, tax_rate, line_amount, created_by, updated_by)
SELECT pr.id, pol.id, p.id, u.id, x.quantity, x.base_quantity, x.unit_cost, x.tax_rate, x.line_amount, pr.created_by, pr.updated_by
FROM purchase_receipt pr
JOIN purchase_order po ON po.id = pr.purchase_order_id
JOIN organization o ON o.id = pr.organization_id
JOIN (
  VALUES
    ('GRN-SPC-00001','INV-900VA','PCS',2,2,4900,18,9800),
    ('GRN-SPC-00001','BAT-150AH','PCS',1,1,9800,18,9800),
    ('GRN-SPC-00001','WIRE-6SQ','MTR',100,100,64,18,6400),
    ('GRN-UHL-00001','LIGHT-PANEL18','PCS',50,50,410,18,20500),
    ('GRN-UHL-00001','STRIP-LED5M','PCS',20,20,240,18,4800),
    ('GRN-UHL-00001','SWITCH-6A','BOX',8,160,380,18,3040),
    ('GRN-UHL-00001','FAN-CLG-1200','PCS',3,3,1650,18,4950)
 ) AS x(receipt_number, sku, uom_code, quantity, base_quantity, unit_cost, tax_rate, line_amount)
  ON pr.receipt_number = x.receipt_number
JOIN purchase_order_line pol ON pol.purchase_order_id = po.id AND pol.product_id = (SELECT id FROM product p2 WHERE p2.organization_id = po.organization_id AND p2.sku = x.sku)
JOIN product p ON p.organization_id = o.id AND p.sku = x.sku
JOIN uom u ON u.code = x.uom_code
ON CONFLICT DO NOTHING;

-- batches and serials received on GRN
INSERT INTO inventory_batch (organization_id, product_id, batch_number, manufacturer_batch_number, manufactured_on, expiry_on, status, created_by, updated_by)
SELECT o.id, p.id, x.batch_number, x.manufacturer_batch_number, x.manufactured_on, x.expiry_on, 'ACTIVE', u.id, u.id
FROM organization o
JOIN (
  VALUES
    ('UHL','STRIP-LED5M','STRIP-B-2026-01','PHIL-2026-01',DATE '2026-01-15',NULL::date,'UHL-ADM-01')
) AS x(org_code, sku, batch_number, manufacturer_batch_number, manufactured_on, expiry_on, created_by_emp)
  ON o.code = x.org_code
JOIN product p ON p.organization_id = o.id AND p.sku = x.sku
JOIN app_user u ON u.organization_id = o.id AND u.employee_code = x.created_by_emp
ON CONFLICT (organization_id, product_id, batch_number) DO NOTHING;

INSERT INTO serial_number (organization_id, product_id, batch_id, serial_number, manufacturer_serial_number, status, current_warehouse_id, created_by, updated_by)
SELECT o.id, p.id, NULL, x.serial_number, x.manufacturer_serial_number, 'IN_STOCK', w.id, u.id, u.id
FROM organization o
JOIN (
  VALUES
    ('SPC','INV-900VA','SPC-HQ-GODOWN','INV900-1001','LUM-INV900-1001','SPC-PUR-01'),
    ('SPC','INV-900VA','SPC-HQ-GODOWN','INV900-1002','LUM-INV900-1002','SPC-PUR-01'),
    ('SPC','BAT-150AH','SPC-HQ-GODOWN','BAT150-1101','EXD-BAT150-1101','SPC-PUR-01'),
    ('UHL','FAN-CLG-1200','UHL-AHD-GODOWN','FAN1200-1201','HAV-FAN1200-1201','UHL-ADM-01'),
    ('UHL','FAN-CLG-1200','UHL-AHD-GODOWN','FAN1200-1202','HAV-FAN1200-1202','UHL-ADM-01'),
    ('UHL','FAN-CLG-1200','UHL-AHD-GODOWN','FAN1200-1203','HAV-FAN1200-1203','UHL-ADM-01')
) AS x(org_code, sku, warehouse_code, serial_number, manufacturer_serial_number, created_by_emp)
  ON o.code = x.org_code
JOIN product p ON p.organization_id = o.id AND p.sku = x.sku
JOIN warehouse w ON w.organization_id = o.id AND w.code = x.warehouse_code
JOIN app_user u ON u.organization_id = o.id AND u.employee_code = x.created_by_emp
ON CONFLICT (organization_id, product_id, serial_number) DO NOTHING;

INSERT INTO purchase_receipt_line_serial (purchase_receipt_line_id, serial_number_id, created_by, updated_by)
SELECT prl.id, sn.id, prl.created_by, prl.updated_by
FROM purchase_receipt_line prl
JOIN purchase_receipt pr ON pr.id = prl.purchase_receipt_id
JOIN organization o ON o.id = pr.organization_id
JOIN product p ON p.id = prl.product_id
JOIN serial_number sn ON sn.organization_id = o.id AND sn.product_id = p.id
WHERE (pr.receipt_number = 'GRN-SPC-00001' AND sn.serial_number IN ('INV900-1001','INV900-1002','BAT150-1101'))
   OR (pr.receipt_number = 'GRN-UHL-00001' AND sn.serial_number IN ('FAN1200-1201','FAN1200-1202','FAN1200-1203'))
ON CONFLICT DO NOTHING;

INSERT INTO purchase_receipt_line_batch (purchase_receipt_line_id, batch_id, quantity, base_quantity, created_by, updated_by)
SELECT prl.id, ib.id, CASE WHEN u.code = 'BOX' THEN 8 ELSE 20 END, CASE WHEN u.code = 'BOX' THEN 160 ELSE 20 END, prl.created_by, prl.updated_by
FROM purchase_receipt_line prl
JOIN purchase_receipt pr ON pr.id = prl.purchase_receipt_id
JOIN product p ON p.id = prl.product_id
JOIN uom u ON u.id = prl.uom_id
JOIN inventory_batch ib ON ib.product_id = p.id
WHERE (pr.receipt_number = 'GRN-UHL-00001' AND p.sku = 'STRIP-LED5M' AND ib.batch_number = 'STRIP-B-2026-01')
   OR (pr.receipt_number = 'GRN-UHL-00001' AND p.sku = 'SWITCH-6A' AND ib.batch_number = 'SWITCH-B-2025-02')
ON CONFLICT DO NOTHING;

-- purchase receipt stock movements
INSERT INTO stock_movement (organization_id, branch_id, warehouse_id, product_id, movement_type, reference_type, reference_id, reference_number, direction, uom_id, quantity, base_quantity, unit_cost, total_cost, movement_at, created_by, updated_by)
SELECT pr.organization_id, pr.branch_id, pr.warehouse_id, prl.product_id, 'PURCHASE_RECEIPT', 'PURCHASE_RECEIPT', pr.id, pr.receipt_number, 'IN', prl.uom_id,
       prl.quantity, prl.base_quantity, prl.unit_cost, prl.base_quantity * prl.unit_cost, pr.posted_at, pr.created_by, pr.updated_by
FROM purchase_receipt pr
JOIN purchase_receipt_line prl ON prl.purchase_receipt_id = pr.id
WHERE pr.status = 'POSTED'
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------
-- 11) Sales flow samples (paid and credit sales)
-- ---------------------------------------------------------
INSERT INTO sales_invoice (organization_id, branch_id, warehouse_id, customer_id, price_list_id, invoice_number, invoice_date, status, subtotal, discount_amount, tax_amount, total_amount, remarks, printed_at, emailed_at, posted_at, created_by, updated_by)
SELECT o.id, b.id, w.id, c.id, pl.id, x.invoice_number, x.invoice_date, x.status, x.subtotal, x.discount_amount, x.tax_amount, x.total_amount, x.remarks,
       x.printed_at, x.emailed_at, x.posted_at, u.id, u.id
FROM organization o
JOIN (
  VALUES
    ('SPC','SPC-JAM','SPC-JAM-STORE','CUST-SPC-002','RETAIL','INV-SPC-00001',DATE '2026-03-03','PAID',19295,0,3473.10,22768.10,'Counter sale with serial tracked items',TIMESTAMPTZ '2026-03-03 13:00:00+05:30',NULL,TIMESTAMPTZ '2026-03-03 12:55:00+05:30','SPC-CAS-01'),
    ('SPC','SPC-RJK','SPC-HQ-GODOWN','CUST-SPC-003','RETAIL','INV-SPC-00002',DATE '2026-03-05','PARTIALLY_PAID',14740,500,2563.20,16803.20,'Credit sale to trade customer',TIMESTAMPTZ '2026-03-05 18:30:00+05:30',TIMESTAMPTZ '2026-03-05 18:45:00+05:30',TIMESTAMPTZ '2026-03-05 18:20:00+05:30','SPC-ADM-01'),
    ('UHL','UHL-SUR','UHL-SUR-STORE','CUST-UHL-002','RETAIL','INV-UHL-00001',DATE '2026-03-07','PAID',5348,0,962.64,6310.64,'Mixed retail basket',TIMESTAMPTZ '2026-03-07 20:00:00+05:30',NULL,TIMESTAMPTZ '2026-03-07 19:55:00+05:30','UHL-CAS-01')
) AS x(org_code, branch_code, warehouse_code, customer_code, price_list_code, invoice_number, invoice_date, status, subtotal, discount_amount, tax_amount, total_amount, remarks, printed_at, emailed_at, posted_at, created_by_emp)
  ON o.code = x.org_code
JOIN branch b ON b.organization_id = o.id AND b.code = x.branch_code
JOIN warehouse w ON w.organization_id = o.id AND w.code = x.warehouse_code
JOIN customer c ON c.organization_id = o.id AND c.customer_code = x.customer_code
LEFT JOIN price_list pl ON pl.organization_id = o.id AND pl.code = x.price_list_code
JOIN app_user u ON u.organization_id = o.id AND u.employee_code = x.created_by_emp
ON CONFLICT (organization_id, invoice_number) DO NOTHING;

INSERT INTO sales_invoice_line (sales_invoice_id, product_id, uom_id, quantity, base_quantity, unit_price, discount_amount, tax_rate, line_amount, created_by, updated_by)
SELECT si.id, p.id, u.id, x.quantity, x.base_quantity, x.unit_price, x.discount_amount, x.tax_rate, x.line_amount, si.created_by, si.updated_by
FROM sales_invoice si
JOIN organization o ON o.id = si.organization_id
JOIN (
  VALUES
    ('INV-SPC-00001','INV-900VA','PCS',1,1,6500,0,18,6500),
    ('INV-SPC-00001','BAT-150AH','PCS',1,1,12800,0,18,12800),
    ('INV-SPC-00002','WIRE-6SQ','MTR',80,80,92,500,18,6860),
    ('INV-SPC-00002','OIL-1LTR','LTR',20,20,55,0,18,1100),
    ('INV-SPC-00002','BULB-9W','DOZEN',6,72,1020,0,18,6120),
    ('INV-UHL-00001','LIGHT-PANEL18','PCS',4,4,650,0,18,2600),
    ('INV-UHL-00001','WIRE-1.5SQ','MTR',60,60,24,0,18,1440),
    ('INV-UHL-00001','SWITCH-6A','BOX',1,20,780,0,18,780),
    ('INV-UHL-00001','FAN-CLG-1200','PCS',1,1,2200,0,18,2200)
 ) AS x(invoice_number, sku, uom_code, quantity, base_quantity, unit_price, discount_amount, tax_rate, line_amount)
  ON si.invoice_number = x.invoice_number
JOIN product p ON p.organization_id = o.id AND p.sku = x.sku
JOIN uom u ON u.code = x.uom_code
ON CONFLICT DO NOTHING;

INSERT INTO sales_line_serial (sales_invoice_line_id, serial_number_id, created_by, updated_by)
SELECT sil.id, sn.id, sil.created_by, sil.updated_by
FROM sales_invoice_line sil
JOIN sales_invoice si ON si.id = sil.sales_invoice_id
JOIN product p ON p.id = sil.product_id
JOIN serial_number sn ON sn.product_id = p.id
WHERE (si.invoice_number = 'INV-SPC-00001' AND sn.serial_number IN ('INV900-0003','BAT150-0103'))
   OR (si.invoice_number = 'INV-UHL-00001' AND sn.serial_number IN ('FAN1200-0202'))
ON CONFLICT DO NOTHING;

INSERT INTO sales_line_batch (sales_invoice_line_id, batch_id, quantity, base_quantity, created_by, updated_by)
SELECT sil.id, ib.id, CASE WHEN u.code='DOZEN' THEN 6 ELSE CASE WHEN p.sku='SWITCH-6A' THEN 1 ELSE 20 END END,
       CASE WHEN u.code='DOZEN' THEN 72 ELSE CASE WHEN p.sku='SWITCH-6A' THEN 20 ELSE 20 END END,
       sil.created_by, sil.updated_by
FROM sales_invoice_line sil
JOIN product p ON p.id = sil.product_id
JOIN uom u ON u.id = sil.uom_id
JOIN inventory_batch ib ON ib.product_id = p.id
WHERE (p.sku='BULB-9W' AND ib.batch_number='BULB-B-2025-02' AND sil.sales_invoice_id = (SELECT id FROM sales_invoice WHERE invoice_number='INV-SPC-00002'))
   OR (p.sku='OIL-1LTR' AND ib.batch_number='OIL-B-2025-01' AND sil.sales_invoice_id = (SELECT id FROM sales_invoice WHERE invoice_number='INV-SPC-00002'))
   OR (p.sku='SWITCH-6A' AND ib.batch_number='SWITCH-B-2025-02' AND sil.sales_invoice_id = (SELECT id FROM sales_invoice WHERE invoice_number='INV-UHL-00001'))
ON CONFLICT DO NOTHING;

-- customer receipts
INSERT INTO customer_receipt (organization_id, branch_id, customer_id, receipt_number, receipt_date, payment_method, reference_number, amount, status, remarks, created_by, updated_by)
SELECT o.id, b.id, c.id, x.receipt_number, x.receipt_date, x.payment_method, x.reference_number, x.amount, x.status, x.remarks, u.id, u.id
FROM organization o
JOIN (
  VALUES
    ('SPC','SPC-JAM','CUST-SPC-002','RCPT-SPC-00001',DATE '2026-03-03','UPI','UPI-7788',22768.10,'ALLOCATED','Full counter payment','SPC-CAS-01'),
    ('SPC','SPC-RJK','CUST-SPC-003','RCPT-SPC-00002',DATE '2026-03-06','BANK','NEFT-9901',8000.00,'ALLOCATED','Part payment against trade invoice','SPC-ACC-01'),
    ('UHL','UHL-SUR','CUST-UHL-002','RCPT-UHL-00001',DATE '2026-03-07','CARD','POS-5511',6310.64,'ALLOCATED','Retail card payment','UHL-CAS-01')
) AS x(org_code, branch_code, customer_code, receipt_number, receipt_date, payment_method, reference_number, amount, status, remarks, created_by_emp)
  ON o.code = x.org_code
JOIN branch b ON b.organization_id = o.id AND b.code = x.branch_code
JOIN customer c ON c.organization_id = o.id AND c.customer_code = x.customer_code
JOIN app_user u ON u.organization_id = o.id AND u.employee_code = x.created_by_emp
ON CONFLICT (organization_id, receipt_number) DO NOTHING;

INSERT INTO customer_receipt_allocation (customer_receipt_id, sales_invoice_id, allocated_amount, created_by, updated_by)
SELECT cr.id, si.id, x.allocated_amount, cr.created_by, cr.updated_by
FROM customer_receipt cr
JOIN (
  VALUES
    ('RCPT-SPC-00001','INV-SPC-00001',22768.10),
    ('RCPT-SPC-00002','INV-SPC-00002',8000.00),
    ('RCPT-UHL-00001','INV-UHL-00001',6310.64)
 ) AS x(receipt_number, invoice_number, allocated_amount)
  ON cr.receipt_number = x.receipt_number
JOIN sales_invoice si ON si.organization_id = cr.organization_id AND si.invoice_number = x.invoice_number
ON CONFLICT DO NOTHING;

-- supplier payments
INSERT INTO supplier_payment (organization_id, branch_id, supplier_id, payment_number, payment_date, payment_method, reference_number, amount, status, remarks, created_by, updated_by)
SELECT o.id, b.id, s.id, x.payment_number, x.payment_date, x.payment_method, x.reference_number, x.amount, x.status, x.remarks, u.id, u.id
FROM organization o
JOIN (
  VALUES
    ('SPC','SPC-HQ','SUP-SPC-002','PAY-SPC-00001',DATE '2026-03-02','BANK','UTR-SPC-001',15000.00,'ALLOCATED','Part payment against GRN-SPC-00001','SPC-ACC-01'),
    ('UHL','UHL-AHD','SUP-UHL-001','PAY-UHL-00001',DATE '2026-03-03','BANK','UTR-UHL-001',25000.00,'ALLOCATED','Partial supplier settlement','UHL-ACC-01')
) AS x(org_code, branch_code, supplier_code, payment_number, payment_date, payment_method, reference_number, amount, status, remarks, created_by_emp)
  ON o.code = x.org_code
JOIN branch b ON b.organization_id = o.id AND b.code = x.branch_code
JOIN supplier s ON s.organization_id = o.id AND s.supplier_code = x.supplier_code
JOIN app_user u ON u.organization_id = o.id AND u.employee_code = x.created_by_emp
ON CONFLICT (organization_id, payment_number) DO NOTHING;

INSERT INTO supplier_payment_allocation (supplier_payment_id, purchase_receipt_id, allocated_amount, created_by, updated_by)
SELECT sp.id, pr.id, x.allocated_amount, sp.created_by, sp.updated_by
FROM supplier_payment sp
JOIN (
  VALUES
    ('PAY-SPC-00001','GRN-SPC-00001',15000.00),
    ('PAY-UHL-00001','GRN-UHL-00001',25000.00)
 ) AS x(payment_number, receipt_number, allocated_amount)
  ON sp.payment_number = x.payment_number
JOIN purchase_receipt pr ON pr.organization_id = sp.organization_id AND pr.receipt_number = x.receipt_number
ON CONFLICT DO NOTHING;

-- sales stock movements
INSERT INTO stock_movement (organization_id, branch_id, warehouse_id, product_id, movement_type, reference_type, reference_id, reference_number, direction, uom_id, quantity, base_quantity, unit_cost, total_cost, movement_at, created_by, updated_by)
SELECT si.organization_id, si.branch_id, si.warehouse_id, sil.product_id, 'SALES_INVOICE', 'SALES_INVOICE', si.id, si.invoice_number, 'OUT', sil.uom_id,
       sil.quantity, sil.base_quantity,
       CASE
         WHEN p.sku='INV-900VA' THEN 5050
         WHEN p.sku='BAT-150AH' THEN 9900
         WHEN p.sku='WIRE-6SQ' THEN 70
         WHEN p.sku='OIL-1LTR' THEN 35
         WHEN p.sku='BULB-9W' THEN 60
         WHEN p.sku='LIGHT-PANEL18' THEN 420
         WHEN p.sku='WIRE-1.5SQ' THEN 15.5
         WHEN p.sku='SWITCH-6A' THEN 22
         WHEN p.sku='FAN-CLG-1200' THEN 1680
         ELSE 0 END,
       sil.base_quantity * CASE
         WHEN p.sku='INV-900VA' THEN 5050
         WHEN p.sku='BAT-150AH' THEN 9900
         WHEN p.sku='WIRE-6SQ' THEN 70
         WHEN p.sku='OIL-1LTR' THEN 35
         WHEN p.sku='BULB-9W' THEN 60
         WHEN p.sku='LIGHT-PANEL18' THEN 420
         WHEN p.sku='WIRE-1.5SQ' THEN 15.5
         WHEN p.sku='SWITCH-6A' THEN 22
         WHEN p.sku='FAN-CLG-1200' THEN 1680
         ELSE 0 END,
       si.posted_at, si.created_by, si.updated_by
FROM sales_invoice si
JOIN sales_invoice_line sil ON sil.sales_invoice_id = si.id
JOIN product p ON p.id = sil.product_id
WHERE si.status IN ('POSTED','PARTIALLY_PAID','PAID')
ON CONFLICT DO NOTHING;

-- inventory reservations for a future UHL invoice
INSERT INTO sales_invoice (organization_id, branch_id, warehouse_id, customer_id, price_list_id, invoice_number, invoice_date, status, subtotal, discount_amount, tax_amount, total_amount, remarks, created_by, updated_by)
SELECT o.id, b.id, w.id, c.id, pl.id, 'INV-UHL-00002', DATE '2026-03-10', 'CONFIRMED', 2200, 0, 396, 2596, 'Confirmed sale awaiting dispatch', u.id, u.id
FROM organization o
JOIN branch b ON b.organization_id=o.id AND b.code='UHL-SUR'
JOIN warehouse w ON w.organization_id=o.id AND w.code='UHL-SUR-STORE'
JOIN customer c ON c.organization_id=o.id AND c.customer_code='CUST-UHL-003'
JOIN price_list pl ON pl.organization_id=o.id AND pl.code='RETAIL'
JOIN app_user u ON u.organization_id=o.id AND u.employee_code='UHL-CAS-01'
WHERE o.code='UHL'
ON CONFLICT (organization_id, invoice_number) DO NOTHING;

INSERT INTO sales_invoice_line (sales_invoice_id, product_id, uom_id, quantity, base_quantity, unit_price, discount_amount, tax_rate, line_amount, created_by, updated_by)
SELECT si.id, p.id, u.id, 1, 1, 2200, 0, 18, 2200, si.created_by, si.updated_by
FROM sales_invoice si
JOIN product p ON p.organization_id=si.organization_id AND p.sku='FAN-CLG-1200'
JOIN uom u ON u.code='PCS'
WHERE si.invoice_number='INV-UHL-00002'
ON CONFLICT DO NOTHING;

INSERT INTO inventory_reservation (organization_id, branch_id, warehouse_id, product_id, source_document_type, source_document_id, source_document_line_id, reserved_base_quantity, status, created_by, updated_by)
SELECT si.organization_id, si.branch_id, si.warehouse_id, sil.product_id, 'SALES_INVOICE', si.id, sil.id, 1, 'ACTIVE', si.created_by, si.updated_by
FROM sales_invoice si
JOIN sales_invoice_line sil ON sil.sales_invoice_id = si.id
WHERE si.invoice_number='INV-UHL-00002'
ON CONFLICT DO NOTHING;

INSERT INTO stock_movement (organization_id, branch_id, warehouse_id, product_id, movement_type, reference_type, reference_id, reference_number, direction, uom_id, quantity, base_quantity, unit_cost, total_cost, movement_at, created_by, updated_by)
SELECT si.organization_id, si.branch_id, si.warehouse_id, sil.product_id, 'RESERVATION', 'SALES_INVOICE', si.id, si.invoice_number, 'OUT', sil.uom_id, 1, 1, 1680, 1680, NOW(), si.created_by, si.updated_by
FROM sales_invoice si
JOIN sales_invoice_line sil ON sil.sales_invoice_id=si.id
WHERE si.invoice_number='INV-UHL-00002'
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------
-- 12) Transfer, adjustment, expense, recurring expense
-- ---------------------------------------------------------
INSERT INTO stock_transfer (organization_id, branch_id, from_warehouse_id, to_warehouse_id, transfer_number, transfer_date, status, remarks, created_by, updated_by)
SELECT o.id, fb.id, fw.id, tw.id, 'TRN-SPC-00001', DATE '2026-03-04', 'POSTED', 'Move inverter from HQ to Jamnagar store', u.id, u.id
FROM organization o
JOIN warehouse fw ON fw.organization_id=o.id AND fw.code='SPC-HQ-GODOWN'
JOIN warehouse tw ON tw.organization_id=o.id AND tw.code='SPC-JAM-STORE'
JOIN branch fb ON fb.id=fw.branch_id
JOIN app_user u ON u.organization_id=o.id AND u.employee_code='SPC-ADM-01'
WHERE o.code='SPC'
ON CONFLICT (organization_id, transfer_number) DO NOTHING;

INSERT INTO stock_transfer_line (stock_transfer_id, product_id, uom_id, quantity, base_quantity, created_by, updated_by)
SELECT st.id, p.id, u.id, 1, 1, st.created_by, st.updated_by
FROM stock_transfer st
JOIN product p ON p.organization_id=st.organization_id AND p.sku='INV-900VA'
JOIN uom u ON u.code='PCS'
WHERE st.transfer_number='TRN-SPC-00001'
ON CONFLICT DO NOTHING;

INSERT INTO stock_adjustment (organization_id, branch_id, warehouse_id, adjustment_number, adjustment_date, reason, status, created_by, updated_by)
SELECT o.id, b.id, w.id, 'ADJ-UHL-00001', DATE '2026-03-08', 'Damaged one LED strip during handling', 'POSTED', u.id, u.id
FROM organization o
JOIN warehouse w ON w.organization_id=o.id AND w.code='UHL-AHD-GODOWN'
JOIN branch b ON b.id=w.branch_id
JOIN app_user u ON u.organization_id=o.id AND u.employee_code='UHL-ADM-01'
WHERE o.code='UHL'
ON CONFLICT (organization_id, adjustment_number) DO NOTHING;

INSERT INTO stock_adjustment_line (stock_adjustment_id, product_id, uom_id, quantity_delta, base_quantity_delta, unit_cost, line_reason, created_by, updated_by)
SELECT sa.id, p.id, u.id, -1, -1, 250, 'Physical damage', sa.created_by, sa.updated_by
FROM stock_adjustment sa
JOIN product p ON p.organization_id=sa.organization_id AND p.sku='STRIP-LED5M'
JOIN uom u ON u.code='PCS'
WHERE sa.adjustment_number='ADJ-UHL-00001'
ON CONFLICT DO NOTHING;

INSERT INTO expense (organization_id, branch_id, expense_category_id, expense_number, expense_date, amount, status, receipt_url, remarks, submitted_at, submitted_by, approved_at, approved_by, paid_at, created_by, updated_by)
SELECT o.id, b.id, ec.id, x.expense_number, x.expense_date, x.amount, x.status, x.receipt_url, x.remarks,
       x.submitted_at, submit_u.id, x.approved_at, approve_u.id, x.paid_at, create_u.id, create_u.id
FROM organization o
JOIN (
  VALUES
    ('SPC','SPC-JAM','TRANSPORT','EXP-SPC-00001',DATE '2026-03-03',1800,'PAID','https://example.test/exp/spc-1.jpg','Local transport charges',TIMESTAMPTZ '2026-03-03 20:00:00+05:30','SPC-MGR-01',TIMESTAMPTZ '2026-03-03 20:30:00+05:30','SPC-OWN-02',TIMESTAMPTZ '2026-03-03 20:45:00+05:30','SPC-MGR-01'),
    ('UHL','UHL-SUR','MAINTENANCE','EXP-UHL-00001',DATE '2026-03-08',2400,'SUBMITTED','https://example.test/exp/uhl-1.jpg','Showroom electrical repair pending approval',TIMESTAMPTZ '2026-03-08 19:00:00+05:30','UHL-MGR-01',NULL,NULL,NULL,'UHL-MGR-01')
) AS x(org_code, branch_code, expense_code, expense_number, expense_date, amount, status, receipt_url, remarks, submitted_at, submitted_by_emp, approved_at, approved_by_emp, paid_at, created_by_emp)
  ON o.code=x.org_code
JOIN branch b ON b.organization_id=o.id AND b.code=x.branch_code
JOIN expense_category ec ON ec.organization_id=o.id AND ec.code=x.expense_code
JOIN app_user create_u ON create_u.organization_id=o.id AND create_u.employee_code=x.created_by_emp
LEFT JOIN app_user submit_u ON submit_u.organization_id=o.id AND submit_u.employee_code=x.submitted_by_emp
LEFT JOIN app_user approve_u ON approve_u.organization_id=o.id AND approve_u.employee_code=x.approved_by_emp
ON CONFLICT (organization_id, expense_number) DO NOTHING;

INSERT INTO recurring_expense (organization_id, branch_id, expense_category_id, recurring_number, frequency, amount, start_date, next_run_date, is_active, remarks, created_by, updated_by)
SELECT o.id, b.id, ec.id, x.recurring_number, x.frequency, x.amount, x.start_date, x.next_run_date, TRUE, x.remarks, u.id, u.id
FROM organization o
JOIN (
  VALUES
    ('SPC','SPC-HQ','RENT','REXP-SPC-00001','MONTHLY',35000,DATE '2026-01-01',DATE '2026-04-01','HQ rent','SPC-ACC-01'),
    ('UHL','UHL-AHD','ELECTRICITY','REXP-UHL-00001','MONTHLY',12000,DATE '2026-01-01',DATE '2026-04-01','Ahmedabad showroom utility','UHL-ACC-01')
) AS x(org_code, branch_code, expense_code, recurring_number, frequency, amount, start_date, next_run_date, remarks, created_by_emp)
  ON o.code=x.org_code
JOIN branch b ON b.organization_id=o.id AND b.code=x.branch_code
JOIN expense_category ec ON ec.organization_id=o.id AND ec.code=x.expense_code
JOIN app_user u ON u.organization_id=o.id AND u.employee_code=x.created_by_emp
ON CONFLICT (organization_id, recurring_number) DO NOTHING;

-- ---------------------------------------------------------
-- 13) Approvals, service, warranty, ownership
-- ---------------------------------------------------------
INSERT INTO approval_rule (organization_id, branch_id, entity_type, approval_type, min_amount, max_amount, approver_role_id, priority_order, is_active, created_by, updated_by)
SELECT o.id, NULL, x.entity_type, x.approval_type, x.min_amount, x.max_amount, r.id, 1, TRUE, u.id, u.id
FROM organization o
JOIN app_user u ON u.organization_id=o.id AND u.employee_code IN ('SPC-OWN-01','UHL-OWN-01')
JOIN (
  VALUES
    ('SPC','EXPENSE','EXPENSE_APPROVAL',1000,NULL::numeric,'OWNER'),
    ('UHL','EXPENSE','EXPENSE_APPROVAL',1000,NULL::numeric,'OWNER'),
    ('UHL','STOCK_ADJUSTMENT','STOCK_ADJUSTMENT_APPROVAL',500,NULL::numeric,'OWNER')
) AS x(org_code, entity_type, approval_type, min_amount, max_amount, role_code)
  ON o.code = x.org_code
JOIN role r ON r.code = x.role_code
ON CONFLICT DO NOTHING;

INSERT INTO approval_request (organization_id, branch_id, entity_type, entity_id, entity_number, approval_type, status, requested_by, requested_at, current_approver_user_id, current_approver_role_snapshot, request_reason, created_by, updated_by)
SELECT e.organization_id, e.branch_id, 'EXPENSE', e.id, e.expense_number, 'EXPENSE_APPROVAL', 'PENDING', req_u.id, e.submitted_at, appr_u.id, 'OWNER', 'Maintenance spend above threshold', req_u.id, req_u.id
FROM expense e
JOIN app_user req_u ON req_u.organization_id=e.organization_id AND req_u.employee_code='UHL-MGR-01'
JOIN app_user appr_u ON appr_u.organization_id=e.organization_id AND appr_u.employee_code='UHL-OWN-02'
WHERE e.expense_number='EXP-UHL-00001'
ON CONFLICT DO NOTHING;

INSERT INTO approval_history (approval_request_id, approver_user_id, action, approver_role_snapshot, remarks, action_at, created_by, updated_by)
SELECT ar.id, ar.requested_by, 'REQUESTED', 'STORE_MANAGER', 'Submitted for owner approval', ar.requested_at, ar.requested_by, ar.requested_by
FROM approval_request ar
WHERE ar.entity_number='EXP-UHL-00001'
ON CONFLICT DO NOTHING;

-- Product ownership from serialized sales
INSERT INTO product_ownership (organization_id, customer_id, product_id, serial_number_id, sales_invoice_id, sales_invoice_line_id, ownership_start_date, warranty_start_date, warranty_end_date, status, created_by, updated_by)
SELECT si.organization_id, si.customer_id, sil.product_id, sls.serial_number_id, si.id, sil.id,
       si.invoice_date, si.invoice_date, si.invoice_date + INTERVAL '24 months', 'ACTIVE', si.created_by, si.updated_by
FROM sales_invoice si
JOIN sales_invoice_line sil ON sil.sales_invoice_id = si.id
JOIN sales_line_serial sls ON sls.sales_invoice_line_id = sil.id
WHERE si.invoice_number IN ('INV-SPC-00001','INV-UHL-00001')
ON CONFLICT DO NOTHING;

-- update sold serial ownership state
UPDATE serial_number sn
SET status = 'SOLD',
    current_customer_id = si.customer_id,
    warranty_start_date = si.invoice_date,
    warranty_end_date = si.invoice_date + INTERVAL '24 months',
    updated_by = si.created_by
FROM sales_line_serial sls
JOIN sales_invoice_line sil ON sil.id = sls.sales_invoice_line_id
JOIN sales_invoice si ON si.id = sil.sales_invoice_id
WHERE sn.id = sls.serial_number_id;

INSERT INTO service_ticket (organization_id, branch_id, customer_id, sales_invoice_id, ticket_number, source_type, priority, status, complaint_summary, issue_description, reported_on, assigned_to_user_id, created_by, updated_by)
SELECT si.organization_id, si.branch_id, si.customer_id, si.id, 'SVC-SPC-00001', 'INVOICE', 'HIGH', 'IN_PROGRESS',
       'Battery backup issue', 'Customer reported low backup and repeated cut-off after installation.', DATE '2026-03-20', tech_u.id, owner_u.id, owner_u.id
FROM sales_invoice si
JOIN app_user tech_u ON tech_u.organization_id=si.organization_id AND tech_u.employee_code='SPC-TEC-01'
JOIN app_user owner_u ON owner_u.organization_id=si.organization_id AND owner_u.employee_code='SPC-OWN-01'
WHERE si.invoice_number='INV-SPC-00001'
ON CONFLICT (organization_id, ticket_number) DO NOTHING;

INSERT INTO service_ticket_item (service_ticket_id, product_id, serial_number_id, product_ownership_id, symptom_notes, diagnosis_notes, resolution_status, created_by, updated_by)
SELECT st.id, po.product_id, po.serial_number_id, po.id, 'Battery discharges quickly', 'Likely cell issue, claim with supplier', 'PENDING', st.created_by, st.updated_by
FROM service_ticket st
JOIN product_ownership po ON po.sales_invoice_id = st.sales_invoice_id
JOIN product p ON p.id = po.product_id AND p.sku='BAT-150AH'
WHERE st.ticket_number='SVC-SPC-00001'
ON CONFLICT DO NOTHING;

INSERT INTO service_visit (organization_id, branch_id, service_ticket_id, technician_user_id, scheduled_at, started_at, completed_at, visit_status, visit_notes, parts_used_json, customer_feedback, created_by, updated_by)
SELECT st.organization_id, st.branch_id, st.id, tech_u.id,
       TIMESTAMPTZ '2026-03-21 10:00:00+05:30', TIMESTAMPTZ '2026-03-21 10:20:00+05:30', TIMESTAMPTZ '2026-03-21 11:10:00+05:30', 'COMPLETED',
       'Voltage measured below threshold. Logged warranty case.', '[{"part":"Terminal Kit","qty":1}]'::jsonb, 'Please resolve under warranty', st.created_by, st.updated_by
FROM service_ticket st
JOIN app_user tech_u ON tech_u.id = st.assigned_to_user_id
WHERE st.ticket_number='SVC-SPC-00001'
ON CONFLICT DO NOTHING;

INSERT INTO warranty_claim (organization_id, branch_id, service_ticket_id, customer_id, product_id, serial_number_id, supplier_id, distributor_id, claim_number, claim_type, status, claim_date, approved_on, claim_notes, created_by, updated_by)
SELECT st.organization_id, st.branch_id, st.id, st.customer_id, sti.product_id, sti.serial_number_id,
       s.id, d.id, 'WCL-SPC-00001', 'REPLACEMENT', 'SUBMITTED', DATE '2026-03-21', NULL,
       'Battery serial submitted for replacement under warranty.', st.created_by, st.updated_by
FROM service_ticket st
JOIN service_ticket_item sti ON sti.service_ticket_id = st.id
LEFT JOIN supplier s ON s.organization_id = st.organization_id AND s.supplier_code='SUP-SPC-001'
LEFT JOIN distributor d ON d.organization_id = st.organization_id AND d.distributor_code='DST-SPC-001'
WHERE st.ticket_number='SVC-SPC-00001'
ON CONFLICT (organization_id, claim_number) DO NOTHING;

-- ---------------------------------------------------------
-- 14) Notifications, schedules, vouchers, ledger, audit evidence
-- ---------------------------------------------------------
INSERT INTO notification_template (organization_id, template_code, channel, subject, body, is_active, created_by, updated_by)
SELECT o.id, x.template_code, x.channel, x.subject, x.body, TRUE, u.id, u.id
FROM organization o
JOIN app_user u ON u.organization_id=o.id AND u.employee_code IN ('SPC-OWN-01','UHL-OWN-01')
JOIN (
  VALUES
    ('SPC','PAYMENT_REMINDER','WHATSAPP','Payment Reminder','Dear {{customer_name}}, your payment of {{amount}} is due for invoice {{invoice_number}}.'),
    ('SPC','SERVICE_UPDATE','SMS','Service Update','Your service ticket {{ticket_number}} is now {{status}}.'),
    ('UHL','PAYMENT_REMINDER','WHATSAPP','Payment Reminder','Dear {{customer_name}}, kindly clear due invoice {{invoice_number}}.'),
    ('UHL','LOW_STOCK_ALERT','EMAIL','Low Stock Alert','The item {{sku}} is below reorder level at {{branch_name}}.')
) AS x(org_code, template_code, channel, subject, body)
  ON o.code=x.org_code
ON CONFLICT (organization_id, template_code) DO NOTHING;

INSERT INTO notification (organization_id, user_id, customer_id, supplier_id, template_id, channel, status, reference_type, reference_id, scheduled_at, sent_at, read_at, payload_json, created_by, updated_by)
SELECT o.id, u.id, c.id, NULL, nt.id, nt.channel, 'SENT', 'SALES_INVOICE', si.id,
       si.posted_at, si.posted_at + INTERVAL '5 minutes', NULL,
       jsonb_build_object('invoice_number', si.invoice_number, 'customer_name', c.full_name, 'amount', si.total_amount),
       si.created_by, si.updated_by
FROM sales_invoice si
JOIN organization o ON o.id=si.organization_id
JOIN customer c ON c.id=si.customer_id
JOIN notification_template nt ON nt.organization_id=o.id AND nt.template_code='PAYMENT_REMINDER'
JOIN app_user u ON u.organization_id=o.id AND u.employee_code IN ('SPC-CAS-01','UHL-CAS-01')
WHERE si.invoice_number IN ('INV-SPC-00002','INV-UHL-00002')
ON CONFLICT DO NOTHING;

INSERT INTO report_schedule (organization_id, user_id, schedule_code, report_type, frequency, delivery_channel, is_active, next_run_at, config_json, created_by, updated_by)
SELECT o.id, u.id, x.schedule_code, x.report_type, x.frequency, x.delivery_channel, TRUE, x.next_run_at, x.config_json, u.id, u.id
FROM organization o
JOIN (
  VALUES
    ('SPC','SPC-OWN-01','RPT-SPC-DAILY','DAILY_SALES_SUMMARY','DAILY','EMAIL',TIMESTAMPTZ '2026-03-26 07:00:00+05:30','{"scope":"organization"}'::jsonb),
    ('UHL','UHL-OWN-01','RPT-UHL-LOWSTOCK','LOW_STOCK_SUMMARY','WEEKLY','APP',TIMESTAMPTZ '2026-03-27 09:00:00+05:30','{"scope":"organization"}'::jsonb)
) AS x(org_code, user_emp, schedule_code, report_type, frequency, delivery_channel, next_run_at, config_json)
  ON o.code=x.org_code
JOIN app_user u ON u.organization_id=o.id AND u.employee_code=x.user_emp
ON CONFLICT (organization_id, schedule_code) DO NOTHING;

-- vouchers and ledger entries
INSERT INTO voucher (organization_id, branch_id, voucher_number, voucher_date, voucher_type, reference_type, reference_id, remarks, status, created_by, updated_by)
SELECT si.organization_id, si.branch_id, 'VCH-' || si.invoice_number, si.invoice_date, 'SALES', 'SALES_INVOICE', si.id, 'Auto sales voucher', 'POSTED', si.created_by, si.updated_by
FROM sales_invoice si
WHERE si.invoice_number IN ('INV-SPC-00001','INV-SPC-00002','INV-UHL-00001')
ON CONFLICT (organization_id, voucher_number) DO NOTHING;

INSERT INTO ledger_entry (organization_id, branch_id, voucher_id, account_id, entry_date, debit_amount, credit_amount, narrative, customer_id, supplier_id, sales_invoice_id, purchase_receipt_id, created_by, updated_by)
SELECT v.organization_id, v.branch_id, v.id, a.id, v.voucher_date,
       CASE WHEN a.code = 'AR' THEN si.total_amount ELSE 0 END,
       CASE WHEN a.code = 'SALES' THEN si.subtotal - si.discount_amount ELSE 0 END,
       'Sales posting', si.customer_id, NULL, si.id, NULL, v.created_by, v.updated_by
FROM voucher v
JOIN sales_invoice si ON si.id = v.reference_id AND v.reference_type='SALES_INVOICE'
JOIN account a ON a.organization_id = v.organization_id AND a.code IN ('AR','SALES')
WHERE v.voucher_number LIKE 'VCH-INV-%'
  AND ((a.code='AR' AND si.total_amount>0) OR (a.code='SALES' AND si.subtotal>0))
ON CONFLICT DO NOTHING;

INSERT INTO voucher (organization_id, branch_id, voucher_number, voucher_date, voucher_type, reference_type, reference_id, remarks, status, created_by, updated_by)
SELECT cr.organization_id, cr.branch_id, 'VCH-' || cr.receipt_number, cr.receipt_date, 'RECEIPT', 'CUSTOMER_RECEIPT', cr.id, 'Customer receipt voucher', 'POSTED', cr.created_by, cr.updated_by
FROM customer_receipt cr
ON CONFLICT (organization_id, voucher_number) DO NOTHING;

INSERT INTO ledger_entry (organization_id, branch_id, voucher_id, account_id, entry_date, debit_amount, credit_amount, narrative, customer_id, sales_invoice_id, created_by, updated_by)
SELECT v.organization_id, v.branch_id, v.id, a.id, v.voucher_date,
       CASE WHEN a.code='CASH' THEN cr.amount ELSE 0 END,
       CASE WHEN a.code='AR' THEN cr.amount ELSE 0 END,
       'Customer receipt posting', cr.customer_id,
       (SELECT cra.sales_invoice_id FROM customer_receipt_allocation cra WHERE cra.customer_receipt_id=cr.id LIMIT 1),
       v.created_by, v.updated_by
FROM voucher v
JOIN customer_receipt cr ON cr.id = v.reference_id AND v.reference_type='CUSTOMER_RECEIPT'
JOIN account a ON a.organization_id = v.organization_id AND a.code IN ('CASH','AR')
ON CONFLICT DO NOTHING;

-- audit events as immutable business evidence
INSERT INTO audit_event (organization_id, branch_id, event_type, entity_type, entity_id, entity_number, action, actor_user_id, actor_name_snapshot, actor_role_snapshot, warehouse_id, customer_id, supplier_id, occurred_at, summary, payload_json, device_id, app_version, ip_address, created_by, updated_by)
SELECT si.organization_id, si.branch_id, 'SALE_POSTED', 'SALES_INVOICE', si.id, si.invoice_number, 'POSTED', u.id, u.full_name, r.name, si.warehouse_id, si.customer_id, NULL,
       si.posted_at, 'Sales invoice posted with line snapshot',
       jsonb_build_object(
         'invoice', row_to_json(si),
         'customer', (SELECT row_to_json(c) FROM customer c WHERE c.id = si.customer_id),
         'lines', (SELECT jsonb_agg(row_to_json(sil)) FROM sales_invoice_line sil WHERE sil.sales_invoice_id = si.id),
         'serials', (SELECT jsonb_agg(jsonb_build_object('line_id', sls.sales_invoice_line_id, 'serial_number', sn.serial_number))
                     FROM sales_line_serial sls JOIN serial_number sn ON sn.id = sls.serial_number_id
                     JOIN sales_invoice_line sil2 ON sil2.id=sls.sales_invoice_line_id WHERE sil2.sales_invoice_id = si.id),
         'batches', (SELECT jsonb_agg(jsonb_build_object('line_id', slb.sales_invoice_line_id, 'batch_id', slb.batch_id, 'base_quantity', slb.base_quantity))
                     FROM sales_line_batch slb JOIN sales_invoice_line sil3 ON sil3.id=slb.sales_invoice_line_id WHERE sil3.sales_invoice_id = si.id)
       ),
       'POS-' || si.branch_id, '1.0.0-demo', '10.10.10.10', si.created_by, si.updated_by
FROM sales_invoice si
JOIN app_user u ON u.id = si.created_by
LEFT JOIN role r ON r.id = u.role_id
WHERE si.invoice_number IN ('INV-SPC-00001','INV-SPC-00002','INV-UHL-00001')
ON CONFLICT DO NOTHING;

INSERT INTO audit_event (organization_id, branch_id, event_type, entity_type, entity_id, entity_number, action, actor_user_id, actor_name_snapshot, actor_role_snapshot, warehouse_id, customer_id, supplier_id, occurred_at, summary, payload_json, device_id, app_version, ip_address, created_by, updated_by)
SELECT st.organization_id, st.branch_id, 'SERVICE_PROGRESS', 'SERVICE_TICKET', st.id, st.ticket_number, 'UPDATED', u.id, u.full_name, r.name, NULL, st.customer_id, NULL,
       NOW(), 'Service ticket updated and warranty path created',
       jsonb_build_object(
         'ticket', row_to_json(st),
         'items', (SELECT jsonb_agg(row_to_json(sti)) FROM service_ticket_item sti WHERE sti.service_ticket_id = st.id),
         'visits', (SELECT jsonb_agg(row_to_json(sv)) FROM service_visit sv WHERE sv.service_ticket_id = st.id),
         'warranty_claim', (SELECT row_to_json(wc) FROM warranty_claim wc WHERE wc.service_ticket_id = st.id LIMIT 1)
       ),
       'SERVICE-APP-01', '1.0.0-demo', '10.10.20.10', st.updated_by, st.updated_by
FROM service_ticket st
JOIN app_user u ON u.id = st.updated_by
LEFT JOIN role r ON r.id = u.role_id
WHERE st.ticket_number = 'SVC-SPC-00001'
ON CONFLICT DO NOTHING;


-- ===== SOURCE: 012_backfill_tax_snapshot_fields.sql =====

UPDATE sales_invoice si
SET seller_tax_registration_id = tr.id,
    seller_gstin = tr.gstin
FROM tax_registration tr
WHERE si.organization_id = tr.organization_id
  AND tr.branch_id IS NULL
  AND tr.is_active = TRUE
  AND tr.is_default = TRUE
  AND si.seller_tax_registration_id IS NULL;

UPDATE sales_invoice si
SET customer_gstin = c.gstin
FROM customer c
WHERE si.customer_id = c.id
  AND si.customer_gstin IS NULL
  AND c.gstin IS NOT NULL
  AND btrim(c.gstin) <> '';

UPDATE sales_invoice
SET place_of_supply_state_code = COALESCE(
      NULLIF(SUBSTRING(customer_gstin FROM 1 FOR 2), ''),
      NULLIF(SUBSTRING(seller_gstin FROM 1 FOR 2), '')
    )
WHERE place_of_supply_state_code IS NULL;

UPDATE purchase_order po
SET seller_tax_registration_id = tr.id,
    seller_gstin = tr.gstin
FROM tax_registration tr
WHERE po.organization_id = tr.organization_id
  AND tr.branch_id IS NULL
  AND tr.is_active = TRUE
  AND tr.is_default = TRUE
  AND po.seller_tax_registration_id IS NULL;

UPDATE purchase_order po
SET supplier_gstin = s.gstin
FROM supplier s
WHERE po.supplier_id = s.id
  AND po.supplier_gstin IS NULL
  AND s.gstin IS NOT NULL
  AND btrim(s.gstin) <> '';

UPDATE purchase_order
SET place_of_supply_state_code = COALESCE(
      NULLIF(SUBSTRING(supplier_gstin FROM 1 FOR 2), ''),
      NULLIF(SUBSTRING(seller_gstin FROM 1 FOR 2), '')
    )
WHERE place_of_supply_state_code IS NULL;

UPDATE purchase_receipt pr
SET seller_tax_registration_id = tr.id,
    seller_gstin = tr.gstin
FROM tax_registration tr
WHERE pr.organization_id = tr.organization_id
  AND tr.branch_id IS NULL
  AND tr.is_active = TRUE
  AND tr.is_default = TRUE
  AND pr.seller_tax_registration_id IS NULL;

UPDATE purchase_receipt pr
SET supplier_gstin = s.gstin
FROM supplier s
WHERE pr.supplier_id = s.id
  AND pr.supplier_gstin IS NULL
  AND s.gstin IS NOT NULL
  AND btrim(s.gstin) <> '';

UPDATE purchase_receipt
SET place_of_supply_state_code = COALESCE(
      NULLIF(SUBSTRING(supplier_gstin FROM 1 FOR 2), ''),
      NULLIF(SUBSTRING(seller_gstin FROM 1 FOR 2), '')
    )
WHERE place_of_supply_state_code IS NULL;

UPDATE sales_invoice_line
SET taxable_amount = line_amount
WHERE taxable_amount = 0
  AND line_amount IS NOT NULL
  AND line_amount > 0;

UPDATE purchase_order_line
SET taxable_amount = line_amount
WHERE taxable_amount = 0
  AND line_amount IS NOT NULL
  AND line_amount > 0;

UPDATE purchase_receipt_line
SET taxable_amount = line_amount
WHERE taxable_amount = 0
  AND line_amount IS NOT NULL
  AND line_amount > 0;

WITH sales_line_tax AS (
  SELECT
    sil.id,
    sil.taxable_amount,
    sil.tax_rate,
    COALESCE(si.place_of_supply_state_code, SUBSTRING(si.customer_gstin FROM 1 FOR 2), SUBSTRING(si.seller_gstin FROM 1 FOR 2)) AS place_state_code,
    SUBSTRING(si.seller_gstin FROM 1 FOR 2) AS seller_state_code
  FROM sales_invoice_line sil
  JOIN sales_invoice si ON si.id = sil.sales_invoice_id
  WHERE sil.tax_rate > 0
    AND sil.taxable_amount > 0
    AND sil.cgst_amount = 0
    AND sil.sgst_amount = 0
    AND sil.igst_amount = 0
)
UPDATE sales_invoice_line sil
SET cgst_rate = CASE
      WHEN slt.place_state_code = slt.seller_state_code THEN ROUND(slt.tax_rate / 2.0, 4)
      ELSE 0
    END,
    cgst_amount = CASE
      WHEN slt.place_state_code = slt.seller_state_code THEN ROUND(slt.taxable_amount * (slt.tax_rate / 2.0) / 100.0, 2)
      ELSE 0
    END,
    sgst_rate = CASE
      WHEN slt.place_state_code = slt.seller_state_code THEN ROUND(slt.tax_rate / 2.0, 4)
      ELSE 0
    END,
    sgst_amount = CASE
      WHEN slt.place_state_code = slt.seller_state_code
        THEN ROUND((slt.taxable_amount * slt.tax_rate / 100.0) - ROUND(slt.taxable_amount * (slt.tax_rate / 2.0) / 100.0, 2), 2)
      ELSE 0
    END,
    igst_rate = CASE
      WHEN slt.place_state_code IS DISTINCT FROM slt.seller_state_code THEN slt.tax_rate
      ELSE 0
    END,
    igst_amount = CASE
      WHEN slt.place_state_code IS DISTINCT FROM slt.seller_state_code THEN ROUND(slt.taxable_amount * slt.tax_rate / 100.0, 2)
      ELSE 0
    END
FROM sales_line_tax slt
WHERE sil.id = slt.id;

WITH purchase_order_line_tax AS (
  SELECT
    pol.id,
    pol.taxable_amount,
    pol.tax_rate,
    COALESCE(po.place_of_supply_state_code, SUBSTRING(po.supplier_gstin FROM 1 FOR 2), SUBSTRING(po.seller_gstin FROM 1 FOR 2)) AS place_state_code,
    SUBSTRING(po.seller_gstin FROM 1 FOR 2) AS seller_state_code
  FROM purchase_order_line pol
  JOIN purchase_order po ON po.id = pol.purchase_order_id
  WHERE pol.tax_rate > 0
    AND pol.taxable_amount > 0
    AND pol.cgst_amount = 0
    AND pol.sgst_amount = 0
    AND pol.igst_amount = 0
)
UPDATE purchase_order_line pol
SET cgst_rate = CASE
      WHEN polt.place_state_code = polt.seller_state_code THEN ROUND(polt.tax_rate / 2.0, 4)
      ELSE 0
    END,
    cgst_amount = CASE
      WHEN polt.place_state_code = polt.seller_state_code THEN ROUND(polt.taxable_amount * (polt.tax_rate / 2.0) / 100.0, 2)
      ELSE 0
    END,
    sgst_rate = CASE
      WHEN polt.place_state_code = polt.seller_state_code THEN ROUND(polt.tax_rate / 2.0, 4)
      ELSE 0
    END,
    sgst_amount = CASE
      WHEN polt.place_state_code = polt.seller_state_code
        THEN ROUND((polt.taxable_amount * polt.tax_rate / 100.0) - ROUND(polt.taxable_amount * (polt.tax_rate / 2.0) / 100.0, 2), 2)
      ELSE 0
    END,
    igst_rate = CASE
      WHEN polt.place_state_code IS DISTINCT FROM polt.seller_state_code THEN polt.tax_rate
      ELSE 0
    END,
    igst_amount = CASE
      WHEN polt.place_state_code IS DISTINCT FROM polt.seller_state_code THEN ROUND(polt.taxable_amount * polt.tax_rate / 100.0, 2)
      ELSE 0
    END
FROM purchase_order_line_tax polt
WHERE pol.id = polt.id;

WITH purchase_receipt_line_tax AS (
  SELECT
    prl.id,
    prl.taxable_amount,
    prl.tax_rate,
    COALESCE(pr.place_of_supply_state_code, SUBSTRING(pr.supplier_gstin FROM 1 FOR 2), SUBSTRING(pr.seller_gstin FROM 1 FOR 2)) AS place_state_code,
    SUBSTRING(pr.seller_gstin FROM 1 FOR 2) AS seller_state_code
  FROM purchase_receipt_line prl
  JOIN purchase_receipt pr ON pr.id = prl.purchase_receipt_id
  WHERE prl.tax_rate > 0
    AND prl.taxable_amount > 0
    AND prl.cgst_amount = 0
    AND prl.sgst_amount = 0
    AND prl.igst_amount = 0
)
UPDATE purchase_receipt_line prl
SET cgst_rate = CASE
      WHEN prlt.place_state_code = prlt.seller_state_code THEN ROUND(prlt.tax_rate / 2.0, 4)
      ELSE 0
    END,
    cgst_amount = CASE
      WHEN prlt.place_state_code = prlt.seller_state_code THEN ROUND(prlt.taxable_amount * (prlt.tax_rate / 2.0) / 100.0, 2)
      ELSE 0
    END,
    sgst_rate = CASE
      WHEN prlt.place_state_code = prlt.seller_state_code THEN ROUND(prlt.tax_rate / 2.0, 4)
      ELSE 0
    END,
    sgst_amount = CASE
      WHEN prlt.place_state_code = prlt.seller_state_code
        THEN ROUND((prlt.taxable_amount * prlt.tax_rate / 100.0) - ROUND(prlt.taxable_amount * (prlt.tax_rate / 2.0) / 100.0, 2), 2)
      ELSE 0
    END,
    igst_rate = CASE
      WHEN prlt.place_state_code IS DISTINCT FROM prlt.seller_state_code THEN prlt.tax_rate
      ELSE 0
    END,
    igst_amount = CASE
      WHEN prlt.place_state_code IS DISTINCT FROM prlt.seller_state_code THEN ROUND(prlt.taxable_amount * prlt.tax_rate / 100.0, 2)
      ELSE 0
    END
FROM purchase_receipt_line_tax prlt
WHERE prl.id = prlt.id;


-- ===== SOURCE: 033_seed_phase1_demo_enrichment.sql =====

-- =========================================================
-- Phase 1 demo enrichment for the current ERP model
-- Adds:
--   * HSN codes on shared product master
--   * richer supplier/customer relationship terms
--   * segmented selling prices on store products
--   * one clearly shared product linked into multiple orgs
--   * supplier-product mappings for better sourcing demos
-- =========================================================

-- ---------------------------------------------------------
-- 1) HSN codes for existing shared master products
-- ---------------------------------------------------------
UPDATE product
SET hsn_code = CASE name
    WHEN 'EcoVolt 900VA Inverter' THEN '85044090'
    WHEN 'Tubular Battery 150AH' THEN '85072000'
    WHEN 'Copper Wire 6 Sqmm' THEN '85444999'
    WHEN 'House Wire 1.5 Sqmm' THEN '85444999'
    WHEN 'Servo Battery Distilled Water 1L' THEN '38200000'
    WHEN 'LED Bulb 9W' THEN '85395000'
    WHEN 'LED Strip 5M' THEN '94054090'
    WHEN 'Slim Panel Light 18W' THEN '94054090'
    WHEN 'Ceiling Fan 1200mm' THEN '84145120'
    WHEN 'Modular Switch 6A' THEN '85365090'
    ELSE hsn_code
END
WHERE hsn_code IS NULL
  AND name IN (
    'EcoVolt 900VA Inverter',
    'Tubular Battery 150AH',
    'Copper Wire 6 Sqmm',
    'House Wire 1.5 Sqmm',
    'Servo Battery Distilled Water 1L',
    'LED Bulb 9W',
    'LED Strip 5M',
    'Slim Panel Light 18W',
    'Ceiling Fan 1200mm',
    'Modular Switch 6A'
  );

-- ---------------------------------------------------------
-- 2) Shared product master present in multiple organizations
-- ---------------------------------------------------------
INSERT INTO product (
    name,
    description,
    category_name,
    brand_name,
    hsn_code,
    base_uom_id,
    inventory_tracking_mode,
    serial_tracking_enabled,
    batch_tracking_enabled,
    expiry_tracking_enabled,
    fractional_quantity_allowed,
    is_service_item,
    is_active,
    created_at,
    updated_at,
    created_by,
    updated_by
)
SELECT
    'LED Flood Light 50W',
    'Shared flood light demo product sold by both organizations',
    'Lighting',
    'PHILIPS',
    '94054090',
    u.id,
    'SIMPLE',
    FALSE,
    FALSE,
    FALSE,
    FALSE,
    FALSE,
    TRUE,
    NOW(),
    NOW(),
    NULL,
    NULL
FROM uom u
WHERE u.code = 'PCS'
  AND NOT EXISTS (
    SELECT 1
    FROM product p
    WHERE lower(p.name) = lower('LED Flood Light 50W')
      AND coalesce(lower(p.brand_name), '') = lower('PHILIPS')
  );

-- ---------------------------------------------------------
-- 3) Supplier enrichment and a Philips channel for SPC
-- ---------------------------------------------------------
INSERT INTO supplier (
    organization_id,
    branch_id,
    supplier_code,
    name,
    legal_name,
    trade_name,
    phone,
    email,
    gstin,
    billing_address,
    shipping_address,
    state,
    state_code,
    contact_person_name,
    contact_person_phone,
    contact_person_email,
    payment_terms,
    is_platform_linked,
    status,
    notes,
    created_by,
    updated_by
)
SELECT
    o.id,
    b.id,
    'SUP-SPC-004',
    'Philips Trade Gujarat',
    'Philips Trade Gujarat Private Limited',
    'Philips Trade Gujarat',
    '+91-9833300004',
    'lighting@philips-gujarat.test',
    '24AACCP8888W1Z5',
    'Naroda GIDC, Ahmedabad',
    'Naroda GIDC, Ahmedabad',
    'Gujarat',
    '24',
    'Amit Shah',
    '+91-9833300104',
    'amit@philips-gujarat.test',
    '30 DAYS',
    FALSE,
    'ACTIVE',
    'Shared lighting supplier for demo walkthroughs',
    u.id,
    u.id
FROM organization o
JOIN branch b ON b.organization_id = o.id AND b.code = 'SPC-HQ'
JOIN app_user u ON u.organization_id = o.id AND u.employee_code = 'SPC-PUR-01'
WHERE o.code = 'SPC'
  AND NOT EXISTS (
    SELECT 1
    FROM supplier s
    WHERE s.organization_id = o.id
      AND s.supplier_code = 'SUP-SPC-004'
  );

UPDATE supplier
SET legal_name = COALESCE(legal_name, name),
    trade_name = COALESCE(trade_name, name),
    billing_address = COALESCE(billing_address, 'Industrial Estate, Ahmedabad'),
    shipping_address = COALESCE(shipping_address, billing_address, 'Industrial Estate, Ahmedabad'),
    state = COALESCE(state, 'Gujarat'),
    state_code = COALESCE(state_code, '24'),
    payment_terms = COALESCE(payment_terms, '30 DAYS'),
    contact_person_name = COALESCE(contact_person_name, 'Sales Desk'),
    contact_person_phone = COALESCE(contact_person_phone, phone),
    contact_person_email = COALESCE(contact_person_email, email)
WHERE supplier_code IN (
    'SUP-SPC-001',
    'SUP-SPC-002',
    'SUP-SPC-003',
    'SUP-SPC-004',
    'SUP-UHL-001',
    'SUP-UHL-002'
  );

-- ---------------------------------------------------------
-- 4) Shared product linked into both orgs as store products
-- ---------------------------------------------------------
INSERT INTO store_product (
    organization_id,
    product_id,
    category_id,
    brand_id,
    base_uom_id,
    tax_group_id,
    sku,
    name,
    description,
    inventory_tracking_mode,
    serial_tracking_enabled,
    batch_tracking_enabled,
    expiry_tracking_enabled,
    fractional_quantity_allowed,
    min_stock_base_qty,
    reorder_level_base_qty,
    default_sale_price,
    is_service_item,
    is_active,
    created_at,
    updated_at,
    created_by,
    updated_by
)
SELECT
    o.id,
    p.id,
    c.id,
    br.id,
    p.base_uom_id,
    tg.id,
    x.sku,
    p.name,
    p.description,
    p.inventory_tracking_mode,
    p.serial_tracking_enabled,
    p.batch_tracking_enabled,
    p.expiry_tracking_enabled,
    p.fractional_quantity_allowed,
    x.min_stock,
    x.reorder_level,
    x.default_sale_price,
    p.is_service_item,
    TRUE,
    NOW(),
    NOW(),
    u.id,
    u.id
FROM (
    VALUES
        ('SPC', 'SPC-FLOOD-50W', 12::numeric, 30::numeric, 890::numeric, 'SPC-OWN-01'),
        ('UHL', 'UHL-FLOOD-50W', 20::numeric, 45::numeric, 950::numeric, 'UHL-OWN-01')
) AS x(org_code, sku, min_stock, reorder_level, default_sale_price, owner_emp)
JOIN organization o ON o.code = x.org_code
JOIN product p
  ON lower(p.name) = lower('LED Flood Light 50W')
 AND coalesce(lower(p.brand_name), '') = lower('PHILIPS')
JOIN category c ON c.organization_id = o.id AND c.code = 'LIGHT'
JOIN brand br ON br.organization_id = o.id AND br.code = 'PHIL'
JOIN tax_group tg ON tg.organization_id = o.id AND tg.code = 'GST_18'
JOIN app_user u ON u.organization_id = o.id AND u.employee_code = x.owner_emp
WHERE NOT EXISTS (
    SELECT 1
    FROM store_product sp
    WHERE sp.organization_id = o.id
      AND sp.product_id = p.id
  );

-- ---------------------------------------------------------
-- 5) Supplier-product mappings and store-supplier terms
-- ---------------------------------------------------------
INSERT INTO supplier_product (
    organization_id,
    supplier_id,
    product_id,
    supplier_product_code,
    supplier_product_name,
    priority,
    is_preferred,
    is_active,
    created_at,
    updated_at,
    created_by,
    updated_by
)
SELECT
    o.id,
    s.id,
    p.id,
    x.supplier_product_code,
    p.name,
    x.priority,
    x.is_preferred,
    TRUE,
    NOW(),
    NOW(),
    u.id,
    u.id
FROM (
    VALUES
        ('SPC', 'SUP-SPC-001', 'Tubular Battery 150AH', 'EXD-BAT-150AH', 1, TRUE, 'SPC-PUR-01'),
        ('SPC', 'SUP-SPC-002', 'EcoVolt 900VA Inverter', 'LUM-INV-900VA', 1, TRUE, 'SPC-PUR-01'),
        ('SPC', 'SUP-SPC-003', 'Copper Wire 6 Sqmm', 'POLY-6SQ', 1, TRUE, 'SPC-PUR-01'),
        ('SPC', 'SUP-SPC-004', 'LED Bulb 9W', 'PHIL-BULB-9W', 1, TRUE, 'SPC-PUR-01'),
        ('SPC', 'SUP-SPC-004', 'LED Flood Light 50W', 'PHIL-FLOOD-50W', 1, TRUE, 'SPC-PUR-01'),
        ('UHL', 'SUP-UHL-001', 'Slim Panel Light 18W', 'PHIL-PANEL-18W', 1, TRUE, 'UHL-ADM-01'),
        ('UHL', 'SUP-UHL-001', 'LED Strip 5M', 'PHIL-STRIP-5M', 1, TRUE, 'UHL-ADM-01'),
        ('UHL', 'SUP-UHL-001', 'LED Flood Light 50W', 'PHIL-FLOOD-50W', 1, TRUE, 'UHL-ADM-01'),
        ('UHL', 'SUP-UHL-002', 'Ceiling Fan 1200mm', 'HAV-FAN-1200', 1, TRUE, 'UHL-ADM-01'),
        ('UHL', 'SUP-UHL-002', 'House Wire 1.5 Sqmm', 'HAV-WIRE-1.5SQ', 1, TRUE, 'UHL-ADM-01'),
        ('UHL', 'SUP-UHL-002', 'Modular Switch 6A', 'HAV-SWITCH-6A', 1, TRUE, 'UHL-ADM-01')
) AS x(org_code, supplier_code, product_name, supplier_product_code, priority, is_preferred, user_emp)
JOIN organization o ON o.code = x.org_code
JOIN supplier s ON s.organization_id = o.id AND s.supplier_code = x.supplier_code
JOIN product p ON lower(p.name) = lower(x.product_name)
JOIN app_user u ON u.organization_id = o.id AND u.employee_code = x.user_emp
WHERE NOT EXISTS (
    SELECT 1
    FROM supplier_product sp
    WHERE sp.organization_id = o.id
      AND sp.supplier_id = s.id
      AND sp.product_id = p.id
  );

UPDATE store_supplier_terms sst
SET payment_terms = COALESCE(sst.payment_terms, CASE
        WHEN s.supplier_code IN ('SUP-SPC-001', 'SUP-SPC-002', 'SUP-UHL-001') THEN '30 DAYS'
        ELSE '15 DAYS'
    END),
    credit_limit = CASE
        WHEN s.supplier_code IN ('SUP-SPC-001', 'SUP-SPC-002') THEN 250000
        WHEN s.supplier_code = 'SUP-SPC-004' THEN 150000
        WHEN s.supplier_code = 'SUP-UHL-001' THEN 300000
        WHEN s.supplier_code = 'SUP-UHL-002' THEN 180000
        ELSE COALESCE(sst.credit_limit, 0)
    END,
    credit_days = COALESCE(sst.credit_days, CASE
        WHEN s.supplier_code IN ('SUP-SPC-001', 'SUP-SPC-002', 'SUP-UHL-001') THEN 30
        ELSE 15
    END),
    is_preferred = CASE
        WHEN s.supplier_code IN ('SUP-SPC-001', 'SUP-SPC-002', 'SUP-SPC-003', 'SUP-SPC-004', 'SUP-UHL-001', 'SUP-UHL-002')
            THEN TRUE
        ELSE sst.is_preferred
    END,
    order_via_email = TRUE,
    order_via_whatsapp = TRUE,
    remarks = COALESCE(sst.remarks, 'Demo commercial terms for sourcing walkthrough')
FROM supplier s
WHERE s.id = sst.supplier_id
  AND s.supplier_code IN (
      'SUP-SPC-001',
      'SUP-SPC-002',
      'SUP-SPC-003',
      'SUP-SPC-004',
      'SUP-UHL-001',
      'SUP-UHL-002'
  );

-- ---------------------------------------------------------
-- 6) Customer relationship terms for demo pricing/credit/loyalty
-- ---------------------------------------------------------
UPDATE store_customer_terms sct
SET customer_segment = CASE c.customer_code
        WHEN 'CUST-SPC-001' THEN 'DEALER'
        WHEN 'CUST-SPC-003' THEN 'B2B'
        WHEN 'CUST-UHL-001' THEN 'B2B'
        WHEN 'CUST-UHL-003' THEN 'WHOLESALE'
        ELSE 'RETAIL'
    END,
    credit_limit = CASE c.customer_code
        WHEN 'CUST-SPC-001' THEN 250000
        WHEN 'CUST-SPC-003' THEN 175000
        WHEN 'CUST-UHL-001' THEN 125000
        WHEN 'CUST-UHL-003' THEN 150000
        ELSE COALESCE(sct.credit_limit, 0)
    END,
    credit_days = CASE c.customer_code
        WHEN 'CUST-SPC-001' THEN 30
        WHEN 'CUST-SPC-003' THEN 21
        WHEN 'CUST-UHL-001' THEN 21
        WHEN 'CUST-UHL-003' THEN 14
        ELSE sct.credit_days
    END,
    loyalty_enabled = CASE
        WHEN c.customer_code IN ('CUST-SPC-002', 'CUST-UHL-002') THEN TRUE
        ELSE FALSE
    END,
    loyalty_points_balance = CASE c.customer_code
        WHEN 'CUST-SPC-002' THEN 120
        WHEN 'CUST-UHL-002' THEN 85
        ELSE COALESCE(sct.loyalty_points_balance, 0)
    END,
    price_tier = CASE c.customer_code
        WHEN 'CUST-SPC-001' THEN 'DEALER'
        WHEN 'CUST-SPC-003' THEN 'B2B'
        WHEN 'CUST-UHL-001' THEN 'B2B'
        WHEN 'CUST-UHL-003' THEN 'WHOLESALE'
        ELSE 'RETAIL'
    END,
    discount_policy = CASE c.customer_code
        WHEN 'CUST-SPC-001' THEN 'Dealer slab discount'
        WHEN 'CUST-UHL-003' THEN 'Wholesale counter discount'
        ELSE sct.discount_policy
    END,
    is_preferred = CASE
        WHEN c.customer_code IN ('CUST-SPC-001', 'CUST-SPC-003', 'CUST-UHL-001', 'CUST-UHL-003') THEN TRUE
        ELSE sct.is_preferred
    END,
    remarks = COALESCE(sct.remarks, 'Demo customer relationship terms')
FROM customer c
WHERE c.id = sct.customer_id;

UPDATE customer
SET billing_address = COALESCE(billing_address, 'Main Market, Gujarat'),
    shipping_address = COALESCE(shipping_address, billing_address, 'Main Market, Gujarat'),
    state = COALESCE(state, 'Gujarat'),
    state_code = COALESCE(state_code, '24'),
    contact_person_name = COALESCE(contact_person_name, full_name),
    contact_person_phone = COALESCE(contact_person_phone, phone),
    contact_person_email = COALESCE(contact_person_email, email)
WHERE customer_code IN (
    'CUST-SPC-001',
    'CUST-SPC-002',
    'CUST-SPC-003',
    'CUST-SPC-004',
    'CUST-UHL-001',
    'CUST-UHL-002',
    'CUST-UHL-003'
  );

-- ---------------------------------------------------------
-- 7) Store-product selling defaults and segment prices
-- ---------------------------------------------------------
UPDATE store_product
SET default_sale_price = CASE sku
    WHEN 'INV-900VA' THEN 6500
    WHEN 'BAT-150AH' THEN 12800
    WHEN 'WIRE-6SQ' THEN 92
    WHEN 'OIL-1LTR' THEN 55
    WHEN 'BULB-9W' THEN 95
    WHEN 'LIGHT-PANEL18' THEN 650
    WHEN 'FAN-CLG-1200' THEN 2200
    WHEN 'WIRE-1.5SQ' THEN 24
    WHEN 'STRIP-LED5M' THEN 410
    WHEN 'SWITCH-6A' THEN 45
    WHEN 'SPC-FLOOD-50W' THEN 890
    WHEN 'UHL-FLOOD-50W' THEN 950
    ELSE default_sale_price
END
WHERE sku IN (
    'INV-900VA',
    'BAT-150AH',
    'WIRE-6SQ',
    'OIL-1LTR',
    'BULB-9W',
    'LIGHT-PANEL18',
    'FAN-CLG-1200',
    'WIRE-1.5SQ',
    'STRIP-LED5M',
    'SWITCH-6A',
    'SPC-FLOOD-50W',
    'UHL-FLOOD-50W'
  );

INSERT INTO store_product_price (
    organization_id,
    store_product_id,
    price_type,
    customer_segment,
    price,
    min_quantity,
    effective_from,
    effective_to,
    is_default,
    is_active,
    created_at,
    updated_at,
    created_by,
    updated_by
)
SELECT
    sp.organization_id,
    sp.id,
    'SELLING',
    x.customer_segment,
    x.price,
    x.min_quantity,
    DATE '2026-01-01',
    NULL,
    x.is_default,
    TRUE,
    NOW(),
    NOW(),
    u.id,
    u.id
FROM (
    VALUES
        ('SPC', 'INV-900VA', 'RETAIL', 6500::numeric, NULL::numeric, TRUE, 'SPC-OWN-01'),
        ('SPC', 'INV-900VA', 'DEALER', 6200::numeric, NULL::numeric, FALSE, 'SPC-OWN-01'),
        ('SPC', 'BAT-150AH', 'RETAIL', 12800::numeric, NULL::numeric, TRUE, 'SPC-OWN-01'),
        ('SPC', 'BAT-150AH', 'B2B', 12350::numeric, NULL::numeric, FALSE, 'SPC-OWN-01'),
        ('SPC', 'WIRE-6SQ', 'RETAIL', 92::numeric, NULL::numeric, TRUE, 'SPC-OWN-01'),
        ('SPC', 'WIRE-6SQ', 'B2B', 88::numeric, 50::numeric, FALSE, 'SPC-OWN-01'),
        ('SPC', 'BULB-9W', 'RETAIL', 95::numeric, NULL::numeric, TRUE, 'SPC-OWN-01'),
        ('SPC', 'BULB-9W', 'WHOLESALE', 85::numeric, 24::numeric, FALSE, 'SPC-OWN-01'),
        ('SPC', 'SPC-FLOOD-50W', 'RETAIL', 890::numeric, NULL::numeric, TRUE, 'SPC-OWN-01'),
        ('SPC', 'SPC-FLOOD-50W', 'DEALER', 835::numeric, 5::numeric, FALSE, 'SPC-OWN-01'),
        ('UHL', 'LIGHT-PANEL18', 'RETAIL', 650::numeric, NULL::numeric, TRUE, 'UHL-OWN-01'),
        ('UHL', 'LIGHT-PANEL18', 'B2B', 610::numeric, 10::numeric, FALSE, 'UHL-OWN-01'),
        ('UHL', 'FAN-CLG-1200', 'RETAIL', 2200::numeric, NULL::numeric, TRUE, 'UHL-OWN-01'),
        ('UHL', 'FAN-CLG-1200', 'WHOLESALE', 2100::numeric, 2::numeric, FALSE, 'UHL-OWN-01'),
        ('UHL', 'SWITCH-6A', 'RETAIL', 45::numeric, NULL::numeric, TRUE, 'UHL-OWN-01'),
        ('UHL', 'SWITCH-6A', 'WHOLESALE', 39::numeric, 20::numeric, FALSE, 'UHL-OWN-01'),
        ('UHL', 'UHL-FLOOD-50W', 'RETAIL', 950::numeric, NULL::numeric, TRUE, 'UHL-OWN-01'),
        ('UHL', 'UHL-FLOOD-50W', 'B2B', 905::numeric, 6::numeric, FALSE, 'UHL-OWN-01')
) AS x(org_code, sku, customer_segment, price, min_quantity, is_default, owner_emp)
JOIN organization o ON o.code = x.org_code
JOIN store_product sp ON sp.organization_id = o.id AND sp.sku = x.sku
JOIN app_user u ON u.organization_id = o.id AND u.employee_code = x.owner_emp
WHERE NOT EXISTS (
    SELECT 1
    FROM store_product_price spp
    WHERE spp.organization_id = sp.organization_id
      AND spp.store_product_id = sp.id
      AND spp.price_type = 'SELLING'
      AND coalesce(spp.customer_segment, 'RETAIL') = x.customer_segment
      AND spp.effective_from = DATE '2026-01-01'
  );

-- ---------------------------------------------------------
-- 8) Opening inventory for the shared product in both orgs
-- ---------------------------------------------------------
INSERT INTO inventory_balance (
    organization_id,
    branch_id,
    warehouse_id,
    product_id,
    batch_id,
    on_hand_base_quantity,
    reserved_base_quantity,
    available_base_quantity,
    avg_cost,
    created_by,
    updated_by
)
SELECT
    o.id,
    w.branch_id,
    w.id,
    sp.id,
    NULL,
    x.on_hand,
    0,
    x.on_hand,
    x.avg_cost,
    u.id,
    u.id
FROM (
    VALUES
        ('SPC', 'SPC-HQ-GODOWN', 'SPC-FLOOD-50W', 18::numeric, 610::numeric, 'SPC-PUR-01'),
        ('UHL', 'UHL-AHD-GODOWN', 'UHL-FLOOD-50W', 25::numeric, 665::numeric, 'UHL-ADM-01')
) AS x(org_code, warehouse_code, sku, on_hand, avg_cost, user_emp)
JOIN organization o ON o.code = x.org_code
JOIN warehouse w ON w.organization_id = o.id AND w.code = x.warehouse_code
JOIN store_product sp ON sp.organization_id = o.id AND sp.sku = x.sku
JOIN app_user u ON u.organization_id = o.id AND u.employee_code = x.user_emp
WHERE NOT EXISTS (
    SELECT 1
    FROM inventory_balance ib
    WHERE ib.organization_id = o.id
      AND ib.warehouse_id = w.id
      AND ib.product_id = sp.id
      AND ib.batch_id IS NULL
  );

INSERT INTO stock_movement (
    organization_id,
    branch_id,
    warehouse_id,
    product_id,
    movement_type,
    reference_type,
    reference_id,
    reference_number,
    direction,
    uom_id,
    quantity,
    base_quantity,
    unit_cost,
    total_cost,
    movement_at,
    created_by,
    updated_by
)
SELECT
    o.id,
    w.branch_id,
    w.id,
    sp.id,
    'OPENING_STOCK',
    'OPENING',
    1,
    'OPEN-DEMO-SHARED',
    'IN',
    sp.base_uom_id,
    x.on_hand,
    x.on_hand,
    x.avg_cost,
    x.on_hand * x.avg_cost,
    TIMESTAMPTZ '2026-03-01 11:30:00+05:30',
    u.id,
    u.id
FROM (
    VALUES
        ('SPC', 'SPC-HQ-GODOWN', 'SPC-FLOOD-50W', 18::numeric, 610::numeric, 'SPC-PUR-01'),
        ('UHL', 'UHL-AHD-GODOWN', 'UHL-FLOOD-50W', 25::numeric, 665::numeric, 'UHL-ADM-01')
) AS x(org_code, warehouse_code, sku, on_hand, avg_cost, user_emp)
JOIN organization o ON o.code = x.org_code
JOIN warehouse w ON w.organization_id = o.id AND w.code = x.warehouse_code
JOIN store_product sp ON sp.organization_id = o.id AND sp.sku = x.sku
JOIN app_user u ON u.organization_id = o.id AND u.employee_code = x.user_emp
WHERE NOT EXISTS (
    SELECT 1
    FROM stock_movement sm
    WHERE sm.organization_id = o.id
      AND sm.warehouse_id = w.id
      AND sm.product_id = sp.id
      AND sm.reference_number = 'OPEN-DEMO-SHARED'
  );


-- ===== SOURCE: 036_seed_sales_quote_order_demo.sql =====

-- Reproducible quotation -> sales order -> sales invoice demo trail for phase 1

INSERT INTO sales_quote (
  organization_id,
  branch_id,
  warehouse_id,
  customer_id,
  quote_type,
  quote_number,
  quote_date,
  valid_until,
  seller_gstin,
  customer_gstin,
  place_of_supply_state_code,
  status,
  subtotal,
  discount_amount,
  tax_amount,
  total_amount,
  remarks,
  created_by,
  updated_by
)
SELECT o.id, b.id, w.id, c.id, 'QUOTATION', 'QTN-SPC-DEMO-00001', DATE '2026-03-12', DATE '2026-03-20',
       o.gstin, c.gstin, '24', 'INVOICED', 2884.00, 0.00, 519.12, 3403.12,
       'Seeded quotation demo converted to order and invoice', u.id, u.id
FROM organization o
JOIN branch b ON b.organization_id = o.id AND b.code = 'SPC-RJK'
JOIN warehouse w ON w.organization_id = o.id AND w.code = 'SPC-HQ-GODOWN'
JOIN customer c ON c.organization_id = o.id AND c.customer_code = 'CUST-SPC-004'
JOIN app_user u ON u.organization_id = o.id AND u.employee_code = 'SPC-OWN-01'
WHERE o.code = 'SPC'
  AND NOT EXISTS (
    SELECT 1 FROM sales_quote sq
    WHERE sq.organization_id = o.id
      AND sq.quote_number = 'QTN-SPC-DEMO-00001'
  );

INSERT INTO sales_quote_line (
  sales_quote_id,
  product_id,
  uom_id,
  hsn_snapshot,
  quantity,
  base_quantity,
  unit_price,
  discount_amount,
  taxable_amount,
  tax_rate,
  cgst_rate,
  cgst_amount,
  sgst_rate,
  sgst_amount,
  igst_rate,
  igst_amount,
  cess_rate,
  cess_amount,
  line_amount,
  remarks,
  created_by,
  updated_by
)
SELECT sq.id, sp.id, u.id, p.hsn_code, x.quantity, x.base_quantity, x.unit_price, 0.00, x.taxable_amount, 18.0000,
       9.0000, x.cgst_amount, 9.0000, x.sgst_amount, 0.0000, 0.00, 0.0000, 0.00, x.line_amount, NULL, sq.created_by, sq.updated_by
FROM sales_quote sq
JOIN organization o ON o.id = sq.organization_id
JOIN (
  VALUES
    ('SPC-FLOOD-50W','PCS',2.000000,2.000000,890.00,1780.00,160.20,160.20,2100.40),
    ('WIRE-6SQ','MTR',12.000000,12.000000,92.00,1104.00,99.36,99.36,1302.72)
) AS x(sku, uom_code, quantity, base_quantity, unit_price, taxable_amount, cgst_amount, sgst_amount, line_amount)
  ON TRUE
JOIN store_product sp ON sp.organization_id = o.id AND sp.sku = x.sku
JOIN product p ON p.id = sp.product_id
JOIN uom u ON u.code = x.uom_code
WHERE sq.quote_number = 'QTN-SPC-DEMO-00001'
  AND NOT EXISTS (
    SELECT 1 FROM sales_quote_line sql
    WHERE sql.sales_quote_id = sq.id
  );

INSERT INTO sales_order (
  organization_id,
  branch_id,
  warehouse_id,
  customer_id,
  source_quote_id,
  order_number,
  order_date,
  seller_gstin,
  customer_gstin,
  place_of_supply_state_code,
  status,
  subtotal,
  discount_amount,
  tax_amount,
  total_amount,
  remarks,
  created_by,
  updated_by
)
SELECT sq.organization_id, sq.branch_id, sq.warehouse_id, sq.customer_id, sq.id, 'SO-SPC-DEMO-00001', DATE '2026-03-13',
       sq.seller_gstin, sq.customer_gstin, sq.place_of_supply_state_code, 'INVOICED',
       sq.subtotal, sq.discount_amount, sq.tax_amount, sq.total_amount,
       'Seeded sales order converted from demo quotation', sq.created_by, sq.updated_by
FROM sales_quote sq
WHERE sq.quote_number = 'QTN-SPC-DEMO-00001'
  AND NOT EXISTS (
    SELECT 1 FROM sales_order so
    WHERE so.organization_id = sq.organization_id
      AND so.order_number = 'SO-SPC-DEMO-00001'
  );

INSERT INTO sales_order_line (
  sales_order_id,
  source_quote_line_id,
  product_id,
  uom_id,
  hsn_snapshot,
  quantity,
  base_quantity,
  unit_price,
  discount_amount,
  taxable_amount,
  tax_rate,
  cgst_rate,
  cgst_amount,
  sgst_rate,
  sgst_amount,
  igst_rate,
  igst_amount,
  cess_rate,
  cess_amount,
  line_amount,
  remarks,
  created_by,
  updated_by
)
SELECT so.id, sql.id, sql.product_id, sql.uom_id, sql.hsn_snapshot, sql.quantity, sql.base_quantity,
       sql.unit_price, sql.discount_amount, sql.taxable_amount, sql.tax_rate,
       sql.cgst_rate, sql.cgst_amount, sql.sgst_rate, sql.sgst_amount,
       sql.igst_rate, sql.igst_amount, sql.cess_rate, sql.cess_amount,
       sql.line_amount, sql.remarks, so.created_by, so.updated_by
FROM sales_order so
JOIN sales_quote sq ON sq.id = so.source_quote_id
JOIN sales_quote_line sql ON sql.sales_quote_id = sq.id
WHERE so.order_number = 'SO-SPC-DEMO-00001'
  AND NOT EXISTS (
    SELECT 1 FROM sales_order_line sol
    WHERE sol.sales_order_id = so.id
  );

INSERT INTO sales_invoice (
  organization_id,
  branch_id,
  warehouse_id,
  customer_id,
  price_list_id,
  invoice_number,
  invoice_date,
  status,
  subtotal,
  discount_amount,
  tax_amount,
  total_amount,
  remarks,
  seller_gstin,
  customer_gstin,
  place_of_supply_state_code,
  due_date,
  posted_at,
  source_quote_id,
  source_order_id,
  created_by,
  updated_by
)
SELECT so.organization_id, so.branch_id, so.warehouse_id, so.customer_id,
       pl.id, 'INV-SPC-00003', DATE '2026-03-14', 'POSTED',
       so.subtotal, so.discount_amount, so.tax_amount, so.total_amount,
       'Seeded posted invoice converted from demo sales order',
       so.seller_gstin, so.customer_gstin, so.place_of_supply_state_code,
       DATE '2026-03-14', TIMESTAMPTZ '2026-03-14 14:35:00+05:30',
       so.source_quote_id, so.id,
       so.created_by, so.updated_by
FROM sales_order so
LEFT JOIN price_list pl ON pl.organization_id = so.organization_id AND pl.code = 'RETAIL'
WHERE so.order_number = 'SO-SPC-DEMO-00001'
  AND NOT EXISTS (
    SELECT 1 FROM sales_invoice si
    WHERE si.organization_id = so.organization_id
      AND si.invoice_number = 'INV-SPC-00003'
  );

INSERT INTO sales_invoice_line (
  sales_invoice_id,
  product_id,
  uom_id,
  hsn_snapshot,
  quantity,
  base_quantity,
  unit_price,
  discount_amount,
  tax_rate,
  taxable_amount,
  cgst_rate,
  cgst_amount,
  sgst_rate,
  sgst_amount,
  igst_rate,
  igst_amount,
  cess_rate,
  cess_amount,
  line_amount,
  unit_cost_at_sale,
  total_cost_at_sale,
  warranty_months,
  created_by,
  updated_by
)
SELECT si.id, sol.product_id, sol.uom_id, sol.hsn_snapshot, sol.quantity, sol.base_quantity,
       sol.unit_price, sol.discount_amount, sol.tax_rate, sol.taxable_amount,
       sol.cgst_rate, sol.cgst_amount, sol.sgst_rate, sol.sgst_amount,
       sol.igst_rate, sol.igst_amount, sol.cess_rate, sol.cess_amount,
       sol.line_amount,
       CASE WHEN sp.sku = 'SPC-FLOOD-50W' THEN 610.00 ELSE 70.00 END,
       CASE WHEN sp.sku = 'SPC-FLOOD-50W' THEN 1220.00 ELSE 840.00 END,
       NULL,
       si.created_by, si.updated_by
FROM sales_invoice si
JOIN sales_order so ON so.id = si.source_order_id
JOIN sales_order_line sol ON sol.sales_order_id = so.id
JOIN store_product sp ON sp.id = sol.product_id
WHERE si.invoice_number = 'INV-SPC-00003'
  AND NOT EXISTS (
    SELECT 1 FROM sales_invoice_line sil
    WHERE sil.sales_invoice_id = si.id
  );

UPDATE sales_quote sq
SET converted_sales_order_id = so.id,
    converted_sales_invoice_id = si.id,
    updated_at = NOW(),
    updated_by = sq.created_by
FROM sales_order so
JOIN sales_invoice si ON si.source_order_id = so.id
WHERE sq.id = so.source_quote_id
  AND sq.quote_number = 'QTN-SPC-DEMO-00001';

UPDATE sales_order so
SET converted_sales_invoice_id = si.id,
    updated_at = NOW(),
    updated_by = so.created_by
FROM sales_invoice si
WHERE si.source_order_id = so.id
  AND so.order_number = 'SO-SPC-DEMO-00001';

INSERT INTO stock_movement (
  organization_id,
  branch_id,
  warehouse_id,
  product_id,
  movement_type,
  reference_type,
  reference_id,
  reference_number,
  direction,
  uom_id,
  quantity,
  base_quantity,
  unit_cost,
  total_cost,
  movement_at,
  created_by,
  updated_by
)
SELECT si.organization_id, si.branch_id, si.warehouse_id, sil.product_id,
       'SALES_INVOICE', 'SALES_INVOICE', si.id, si.invoice_number, 'OUT',
       sil.uom_id, sil.quantity, sil.base_quantity,
       sil.unit_cost_at_sale,
       sil.total_cost_at_sale,
       si.posted_at, si.created_by, si.updated_by
FROM sales_invoice si
JOIN sales_invoice_line sil ON sil.sales_invoice_id = si.id
WHERE si.invoice_number = 'INV-SPC-00003'
  AND NOT EXISTS (
    SELECT 1 FROM stock_movement sm
    WHERE sm.reference_type = 'SALES_INVOICE'
      AND sm.reference_id = si.id
      AND sm.product_id = sil.product_id
      AND sm.movement_type = 'SALES_INVOICE'
  );

UPDATE inventory_balance ib
SET on_hand_base_quantity = CASE
      WHEN sp.sku = 'SPC-FLOOD-50W' THEN 16.000000
      WHEN sp.sku = 'WIRE-6SQ' THEN 488.000000
      ELSE ib.on_hand_base_quantity
    END,
    reserved_base_quantity = 0.000000,
    available_base_quantity = CASE
      WHEN sp.sku = 'SPC-FLOOD-50W' THEN 16.000000
      WHEN sp.sku = 'WIRE-6SQ' THEN 488.000000
      ELSE ib.available_base_quantity
    END,
    updated_at = NOW(),
    updated_by = (
      SELECT u.id
      FROM app_user u
      JOIN organization o ON o.id = u.organization_id
      WHERE o.code = 'SPC' AND u.employee_code = 'SPC-OWN-01'
      LIMIT 1
    )
FROM store_product sp
JOIN organization o ON o.id = sp.organization_id
WHERE ib.organization_id = sp.organization_id
  AND ib.product_id = sp.id
  AND ib.warehouse_id = (
    SELECT w.id FROM warehouse w
    WHERE w.organization_id = o.id AND w.code = 'SPC-HQ-GODOWN'
  )
  AND o.code = 'SPC'
  AND sp.sku IN ('SPC-FLOOD-50W', 'WIRE-6SQ');

INSERT INTO voucher (
  organization_id,
  branch_id,
  voucher_number,
  voucher_date,
  voucher_type,
  reference_type,
  reference_id,
  remarks,
  status,
  created_by,
  updated_by
)
SELECT si.organization_id, si.branch_id, 'VCH-' || si.invoice_number, si.invoice_date,
       'SALES', 'SALES_INVOICE', si.id, 'Seeded sales voucher for quote/order demo', 'POSTED',
       si.created_by, si.updated_by
FROM sales_invoice si
WHERE si.invoice_number = 'INV-SPC-00003'
  AND NOT EXISTS (
    SELECT 1 FROM voucher v
    WHERE v.organization_id = si.organization_id
      AND v.voucher_number = 'VCH-' || si.invoice_number
  );

INSERT INTO ledger_entry (
  organization_id,
  branch_id,
  voucher_id,
  account_id,
  entry_date,
  debit_amount,
  credit_amount,
  narrative,
  customer_id,
  sales_invoice_id,
  created_by,
  updated_by
)
SELECT v.organization_id, v.branch_id, v.id, a.id, v.voucher_date,
       CASE WHEN a.code = 'AR' THEN si.total_amount ELSE 0 END,
       CASE WHEN a.code = 'SALES' THEN si.subtotal - si.discount_amount ELSE 0 END,
       'Seeded quote-order-invoice demo posting', si.customer_id, si.id, v.created_by, v.updated_by
FROM voucher v
JOIN sales_invoice si ON si.id = v.reference_id AND v.reference_type = 'SALES_INVOICE'
JOIN account a ON a.organization_id = v.organization_id AND a.code IN ('AR','SALES')
WHERE v.voucher_number = 'VCH-INV-SPC-00003'
  AND NOT EXISTS (
    SELECT 1 FROM ledger_entry le
    WHERE le.voucher_id = v.id
      AND le.account_id = a.id
  );


