CREATE TABLE recurring_sales_invoice (
  id                     BIGSERIAL PRIMARY KEY,
  organization_id        BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  branch_id              BIGINT NOT NULL REFERENCES branch(id) ON DELETE CASCADE,
  warehouse_id           BIGINT NOT NULL REFERENCES warehouse(id) ON DELETE RESTRICT,
  customer_id            BIGINT NOT NULL REFERENCES customer(id) ON DELETE RESTRICT,
  price_list_id          BIGINT,
  template_number        VARCHAR(80) NOT NULL,
  frequency              VARCHAR(20) NOT NULL,
  start_date             DATE NOT NULL,
  next_run_date          DATE NOT NULL,
  end_date               DATE,
  due_days               INTEGER,
  place_of_supply_state_code VARCHAR(8),
  remarks                TEXT,
  is_active              BOOLEAN NOT NULL DEFAULT TRUE,
  last_run_at            TIMESTAMP,
  last_sales_invoice_id  BIGINT REFERENCES sales_invoice(id) ON DELETE SET NULL,
  created_at             TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at             TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by             BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  updated_by             BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  CONSTRAINT uq_recurring_sales_invoice_org_number UNIQUE (organization_id, template_number),
  CONSTRAINT chk_recurring_sales_invoice_frequency CHECK (frequency IN ('DAILY','WEEKLY','MONTHLY','QUARTERLY','YEARLY')),
  CONSTRAINT chk_recurring_sales_invoice_due_days CHECK (due_days IS NULL OR due_days >= 0)
);

CREATE INDEX idx_recurring_sales_invoice_org_next_run
  ON recurring_sales_invoice(organization_id, is_active, next_run_date);

CREATE TABLE recurring_sales_invoice_line (
  id                          BIGSERIAL PRIMARY KEY,
  recurring_sales_invoice_id  BIGINT NOT NULL REFERENCES recurring_sales_invoice(id) ON DELETE CASCADE,
  product_id                  BIGINT NOT NULL REFERENCES store_product(id) ON DELETE RESTRICT,
  uom_id                      BIGINT NOT NULL REFERENCES uom(id) ON DELETE RESTRICT,
  quantity                    NUMERIC(18,3) NOT NULL,
  base_quantity               NUMERIC(18,3) NOT NULL,
  unit_price                  NUMERIC(18,2),
  discount_amount             NUMERIC(18,2) NOT NULL DEFAULT 0,
  warranty_months             INTEGER,
  remarks                     TEXT,
  created_at                  TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at                  TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by                  BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  updated_by                  BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  CONSTRAINT chk_recurring_sales_invoice_line_qty CHECK (quantity > 0 AND base_quantity > 0),
  CONSTRAINT chk_recurring_sales_invoice_line_discount CHECK (discount_amount >= 0)
);

CREATE TABLE recurring_journal (
  id                     BIGSERIAL PRIMARY KEY,
  organization_id        BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  branch_id              BIGINT NOT NULL REFERENCES branch(id) ON DELETE CASCADE,
  template_number        VARCHAR(80) NOT NULL,
  voucher_type           VARCHAR(30) NOT NULL,
  frequency              VARCHAR(20) NOT NULL,
  start_date             DATE NOT NULL,
  next_run_date          DATE NOT NULL,
  end_date               DATE,
  remarks                TEXT,
  is_active              BOOLEAN NOT NULL DEFAULT TRUE,
  last_run_at            TIMESTAMP,
  last_voucher_id        BIGINT REFERENCES voucher(id) ON DELETE SET NULL,
  created_at             TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at             TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by             BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  updated_by             BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  CONSTRAINT uq_recurring_journal_org_number UNIQUE (organization_id, template_number),
  CONSTRAINT chk_recurring_journal_frequency CHECK (frequency IN ('DAILY','WEEKLY','MONTHLY','QUARTERLY','YEARLY'))
);

CREATE INDEX idx_recurring_journal_org_next_run
  ON recurring_journal(organization_id, is_active, next_run_date);

CREATE TABLE recurring_journal_line (
  id                    BIGSERIAL PRIMARY KEY,
  recurring_journal_id  BIGINT NOT NULL REFERENCES recurring_journal(id) ON DELETE CASCADE,
  account_id            BIGINT NOT NULL REFERENCES account(id) ON DELETE RESTRICT,
  debit_amount          NUMERIC(18,2) NOT NULL DEFAULT 0,
  credit_amount         NUMERIC(18,2) NOT NULL DEFAULT 0,
  narrative             TEXT,
  customer_id           BIGINT REFERENCES customer(id) ON DELETE SET NULL,
  supplier_id           BIGINT REFERENCES supplier(id) ON DELETE SET NULL,
  created_at            TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at            TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by            BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  updated_by            BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  CONSTRAINT chk_recurring_journal_line_debit_credit CHECK (
    (debit_amount > 0 AND credit_amount = 0) OR
    (credit_amount > 0 AND debit_amount = 0)
  )
);
