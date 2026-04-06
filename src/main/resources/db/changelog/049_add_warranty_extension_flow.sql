CREATE TABLE warranty_extension (
  id                    BIGSERIAL PRIMARY KEY,
  organization_id       BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  product_ownership_id  BIGINT NOT NULL REFERENCES product_ownership(id) ON DELETE CASCADE,
  serial_number_id      BIGINT REFERENCES serial_number(id) ON DELETE SET NULL,
  sales_invoice_id      BIGINT REFERENCES sales_invoice(id) ON DELETE SET NULL,
  sales_invoice_line_id BIGINT REFERENCES sales_invoice_line(id) ON DELETE SET NULL,
  extension_type        VARCHAR(40) NOT NULL,
  months_added          INTEGER NOT NULL,
  start_date            DATE,
  end_date              DATE,
  status                VARCHAR(20) NOT NULL,
  reason                VARCHAR(300),
  reference_number      VARCHAR(120),
  amount                NUMERIC(18,2),
  remarks               TEXT,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT chk_warranty_extension_type CHECK (extension_type IN ('MANUFACTURER_PROMO','PAID_EXTENDED','GOODWILL','MANUAL_CORRECTION')),
  CONSTRAINT chk_warranty_extension_status CHECK (status IN ('ACTIVE','CANCELLED','EXPIRED')),
  CONSTRAINT chk_warranty_extension_months CHECK (months_added > 0)
);

CREATE INDEX idx_warranty_extension_ownership ON warranty_extension(organization_id, product_ownership_id, status, id);
