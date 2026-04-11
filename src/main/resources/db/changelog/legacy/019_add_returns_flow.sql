INSERT INTO permission (code, name, module_code)
SELECT 'purchase.return', 'Process purchase returns', 'PURCHASE'
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = 'purchase.return');

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.code = 'purchase.return'
WHERE r.code IN ('OWNER','ADMIN','STORE_MANAGER','PURCHASE_OPERATOR')
  AND NOT EXISTS (
    SELECT 1
    FROM role_permission rp
    WHERE rp.role_id = r.id
      AND rp.permission_id = p.id
  );

ALTER TABLE sales_invoice_line
  ADD COLUMN IF NOT EXISTS unit_cost_at_sale NUMERIC(18,2) NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS total_cost_at_sale NUMERIC(18,2) NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS sales_return (
  id BIGSERIAL PRIMARY KEY,
  organization_id BIGINT NOT NULL REFERENCES organization(id) ON DELETE RESTRICT,
  branch_id BIGINT NOT NULL REFERENCES branch(id) ON DELETE RESTRICT,
  warehouse_id BIGINT NOT NULL REFERENCES warehouse(id) ON DELETE RESTRICT,
  customer_id BIGINT NOT NULL REFERENCES customer(id) ON DELETE RESTRICT,
  original_sales_invoice_id BIGINT NOT NULL REFERENCES sales_invoice(id) ON DELETE RESTRICT,
  return_number VARCHAR(60) NOT NULL,
  return_date DATE NOT NULL,
  seller_gstin VARCHAR(30),
  customer_gstin VARCHAR(30),
  place_of_supply_state_code VARCHAR(10),
  reason VARCHAR(120),
  status VARCHAR(20) NOT NULL DEFAULT 'POSTED',
  subtotal NUMERIC(18,2) NOT NULL DEFAULT 0,
  tax_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  total_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  remarks TEXT,
  posted_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  CONSTRAINT uq_sales_return_org_number UNIQUE (organization_id, return_number),
  CONSTRAINT chk_sales_return_status CHECK (status IN ('DRAFT','POSTED','CANCELLED'))
);

CREATE TABLE IF NOT EXISTS sales_return_line (
  id BIGSERIAL PRIMARY KEY,
  sales_return_id BIGINT NOT NULL REFERENCES sales_return(id) ON DELETE CASCADE,
  original_sales_invoice_line_id BIGINT NOT NULL REFERENCES sales_invoice_line(id) ON DELETE RESTRICT,
  product_id BIGINT NOT NULL REFERENCES store_product(id) ON DELETE RESTRICT,
  uom_id BIGINT NOT NULL REFERENCES uom(id) ON DELETE RESTRICT,
  quantity NUMERIC(18,6) NOT NULL,
  base_quantity NUMERIC(18,6) NOT NULL,
  unit_price NUMERIC(18,2) NOT NULL,
  discount_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  taxable_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  tax_rate NUMERIC(9,4) NOT NULL DEFAULT 0,
  cgst_rate NUMERIC(9,4) NOT NULL DEFAULT 0,
  cgst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  sgst_rate NUMERIC(9,4) NOT NULL DEFAULT 0,
  sgst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  igst_rate NUMERIC(9,4) NOT NULL DEFAULT 0,
  igst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  cess_rate NUMERIC(9,4) NOT NULL DEFAULT 0,
  cess_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  line_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  unit_cost_at_return NUMERIC(18,2) NOT NULL DEFAULT 0,
  total_cost_at_return NUMERIC(18,2) NOT NULL DEFAULT 0,
  disposition VARCHAR(30) NOT NULL DEFAULT 'RESTOCK',
  reason VARCHAR(120),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  CONSTRAINT chk_sales_return_disposition CHECK (disposition IN ('RESTOCK','DAMAGED','SERVICE_PENDING','SCRAP'))
);

CREATE TABLE IF NOT EXISTS purchase_return (
  id BIGSERIAL PRIMARY KEY,
  organization_id BIGINT NOT NULL REFERENCES organization(id) ON DELETE RESTRICT,
  branch_id BIGINT NOT NULL REFERENCES branch(id) ON DELETE RESTRICT,
  warehouse_id BIGINT NOT NULL REFERENCES warehouse(id) ON DELETE RESTRICT,
  supplier_id BIGINT NOT NULL REFERENCES supplier(id) ON DELETE RESTRICT,
  original_purchase_receipt_id BIGINT NOT NULL REFERENCES purchase_receipt(id) ON DELETE RESTRICT,
  return_number VARCHAR(60) NOT NULL,
  return_date DATE NOT NULL,
  seller_gstin VARCHAR(30),
  supplier_gstin VARCHAR(30),
  place_of_supply_state_code VARCHAR(10),
  reason VARCHAR(120),
  status VARCHAR(20) NOT NULL DEFAULT 'POSTED',
  subtotal NUMERIC(18,2) NOT NULL DEFAULT 0,
  tax_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  total_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  remarks TEXT,
  posted_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  CONSTRAINT uq_purchase_return_org_number UNIQUE (organization_id, return_number),
  CONSTRAINT chk_purchase_return_status CHECK (status IN ('DRAFT','POSTED','CANCELLED'))
);

CREATE TABLE IF NOT EXISTS purchase_return_line (
  id BIGSERIAL PRIMARY KEY,
  purchase_return_id BIGINT NOT NULL REFERENCES purchase_return(id) ON DELETE CASCADE,
  original_purchase_receipt_line_id BIGINT NOT NULL REFERENCES purchase_receipt_line(id) ON DELETE RESTRICT,
  product_id BIGINT NOT NULL REFERENCES store_product(id) ON DELETE RESTRICT,
  uom_id BIGINT NOT NULL REFERENCES uom(id) ON DELETE RESTRICT,
  quantity NUMERIC(18,6) NOT NULL,
  base_quantity NUMERIC(18,6) NOT NULL,
  unit_cost NUMERIC(18,2) NOT NULL,
  taxable_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  tax_rate NUMERIC(9,4) NOT NULL DEFAULT 0,
  cgst_rate NUMERIC(9,4) NOT NULL DEFAULT 0,
  cgst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  sgst_rate NUMERIC(9,4) NOT NULL DEFAULT 0,
  sgst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  igst_rate NUMERIC(9,4) NOT NULL DEFAULT 0,
  igst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  cess_rate NUMERIC(9,4) NOT NULL DEFAULT 0,
  cess_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  line_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  reason VARCHAR(120),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_sales_return_org_original_invoice ON sales_return (organization_id, original_sales_invoice_id, return_date);
CREATE INDEX IF NOT EXISTS idx_purchase_return_org_original_receipt ON purchase_return (organization_id, original_purchase_receipt_id, return_date);
CREATE INDEX IF NOT EXISTS idx_sales_return_line_original_line ON sales_return_line (original_sales_invoice_line_id);
CREATE INDEX IF NOT EXISTS idx_purchase_return_line_original_line ON purchase_return_line (original_purchase_receipt_line_id);
