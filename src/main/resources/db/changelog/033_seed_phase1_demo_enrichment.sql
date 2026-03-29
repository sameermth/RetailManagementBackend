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
