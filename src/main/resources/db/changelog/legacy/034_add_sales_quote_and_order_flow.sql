CREATE TABLE IF NOT EXISTS sales_quote (
  id BIGSERIAL PRIMARY KEY,
  organization_id BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  branch_id BIGINT NOT NULL REFERENCES branch(id) ON DELETE RESTRICT,
  warehouse_id BIGINT NOT NULL REFERENCES warehouse(id) ON DELETE RESTRICT,
  customer_id BIGINT NOT NULL REFERENCES customer(id) ON DELETE RESTRICT,
  quote_type VARCHAR(20) NOT NULL,
  quote_number VARCHAR(100) NOT NULL,
  quote_date DATE NOT NULL,
  valid_until DATE,
  seller_tax_registration_id BIGINT REFERENCES tax_registration(id) ON DELETE SET NULL,
  seller_gstin VARCHAR(30),
  customer_gstin VARCHAR(30),
  place_of_supply_state_code VARCHAR(10),
  status VARCHAR(40) NOT NULL,
  subtotal NUMERIC(18,2) NOT NULL DEFAULT 0,
  discount_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  tax_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  total_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  converted_sales_order_id BIGINT,
  converted_sales_invoice_id BIGINT REFERENCES sales_invoice(id) ON DELETE SET NULL,
  remarks TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  CONSTRAINT uq_sales_quote_number UNIQUE (organization_id, quote_number),
  CONSTRAINT chk_sales_quote_type CHECK (quote_type IN ('ESTIMATE','QUOTATION'))
);

CREATE TABLE IF NOT EXISTS sales_quote_line (
  id BIGSERIAL PRIMARY KEY,
  sales_quote_id BIGINT NOT NULL REFERENCES sales_quote(id) ON DELETE CASCADE,
  product_id BIGINT NOT NULL REFERENCES store_product(id) ON DELETE RESTRICT,
  uom_id BIGINT NOT NULL REFERENCES uom(id) ON DELETE RESTRICT,
  hsn_snapshot VARCHAR(50),
  quantity NUMERIC(18,6) NOT NULL,
  base_quantity NUMERIC(18,6) NOT NULL,
  unit_price NUMERIC(18,2) NOT NULL,
  discount_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  taxable_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  tax_rate NUMERIC(10,4) NOT NULL DEFAULT 0,
  cgst_rate NUMERIC(10,4),
  cgst_amount NUMERIC(18,2),
  sgst_rate NUMERIC(10,4),
  sgst_amount NUMERIC(18,2),
  igst_rate NUMERIC(10,4),
  igst_amount NUMERIC(18,2),
  cess_rate NUMERIC(10,4),
  cess_amount NUMERIC(18,2),
  line_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  remarks TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS sales_order (
  id BIGSERIAL PRIMARY KEY,
  organization_id BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  branch_id BIGINT NOT NULL REFERENCES branch(id) ON DELETE RESTRICT,
  warehouse_id BIGINT NOT NULL REFERENCES warehouse(id) ON DELETE RESTRICT,
  customer_id BIGINT NOT NULL REFERENCES customer(id) ON DELETE RESTRICT,
  source_quote_id BIGINT,
  order_number VARCHAR(100) NOT NULL,
  order_date DATE NOT NULL,
  seller_tax_registration_id BIGINT REFERENCES tax_registration(id) ON DELETE SET NULL,
  seller_gstin VARCHAR(30),
  customer_gstin VARCHAR(30),
  place_of_supply_state_code VARCHAR(10),
  status VARCHAR(40) NOT NULL,
  subtotal NUMERIC(18,2) NOT NULL DEFAULT 0,
  discount_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  tax_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  total_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  converted_sales_invoice_id BIGINT REFERENCES sales_invoice(id) ON DELETE SET NULL,
  remarks TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  CONSTRAINT uq_sales_order_number UNIQUE (organization_id, order_number)
);

CREATE TABLE IF NOT EXISTS sales_order_line (
  id BIGSERIAL PRIMARY KEY,
  sales_order_id BIGINT NOT NULL REFERENCES sales_order(id) ON DELETE CASCADE,
  source_quote_line_id BIGINT REFERENCES sales_quote_line(id) ON DELETE SET NULL,
  product_id BIGINT NOT NULL REFERENCES store_product(id) ON DELETE RESTRICT,
  uom_id BIGINT NOT NULL REFERENCES uom(id) ON DELETE RESTRICT,
  hsn_snapshot VARCHAR(50),
  quantity NUMERIC(18,6) NOT NULL,
  base_quantity NUMERIC(18,6) NOT NULL,
  unit_price NUMERIC(18,2) NOT NULL,
  discount_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  taxable_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  tax_rate NUMERIC(10,4) NOT NULL DEFAULT 0,
  cgst_rate NUMERIC(10,4),
  cgst_amount NUMERIC(18,2),
  sgst_rate NUMERIC(10,4),
  sgst_amount NUMERIC(18,2),
  igst_rate NUMERIC(10,4),
  igst_amount NUMERIC(18,2),
  cess_rate NUMERIC(10,4),
  cess_amount NUMERIC(18,2),
  line_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  remarks TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL
);

ALTER TABLE sales_quote
  ADD CONSTRAINT fk_sales_quote_converted_order
  FOREIGN KEY (converted_sales_order_id) REFERENCES sales_order(id) ON DELETE SET NULL;

ALTER TABLE sales_order
  ADD CONSTRAINT fk_sales_order_source_quote
  FOREIGN KEY (source_quote_id) REFERENCES sales_quote(id) ON DELETE SET NULL;

ALTER TABLE sales_invoice
  ADD COLUMN IF NOT EXISTS source_quote_id BIGINT REFERENCES sales_quote(id) ON DELETE SET NULL,
  ADD COLUMN IF NOT EXISTS source_order_id BIGINT REFERENCES sales_order(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_sales_quote_org_date ON sales_quote(organization_id, quote_date DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_sales_order_org_date ON sales_order(organization_id, order_date DESC, id DESC);
