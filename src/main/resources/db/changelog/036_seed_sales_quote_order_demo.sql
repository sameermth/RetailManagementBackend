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

