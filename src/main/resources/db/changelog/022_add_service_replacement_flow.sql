CREATE TABLE IF NOT EXISTS service_replacement (
  id BIGSERIAL PRIMARY KEY,
  organization_id BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  branch_id BIGINT NOT NULL REFERENCES branch(id) ON DELETE RESTRICT,
  warehouse_id BIGINT NOT NULL REFERENCES warehouse(id) ON DELETE RESTRICT,
  service_ticket_id BIGINT REFERENCES service_ticket(id) ON DELETE SET NULL,
  warranty_claim_id BIGINT REFERENCES warranty_claim(id) ON DELETE SET NULL,
  sales_return_id BIGINT REFERENCES sales_return(id) ON DELETE SET NULL,
  customer_id BIGINT NOT NULL REFERENCES customer(id) ON DELETE RESTRICT,
  original_product_id BIGINT NOT NULL REFERENCES store_product(id) ON DELETE RESTRICT,
  original_serial_number_id BIGINT REFERENCES serial_number(id) ON DELETE SET NULL,
  original_product_ownership_id BIGINT REFERENCES product_ownership(id) ON DELETE SET NULL,
  replacement_product_id BIGINT NOT NULL REFERENCES store_product(id) ON DELETE RESTRICT,
  replacement_serial_number_id BIGINT REFERENCES serial_number(id) ON DELETE SET NULL,
  replacement_uom_id BIGINT NOT NULL REFERENCES uom(id) ON DELETE RESTRICT,
  replacement_quantity NUMERIC(18,6) NOT NULL,
  replacement_base_quantity NUMERIC(18,6) NOT NULL,
  replacement_number VARCHAR(80) NOT NULL,
  replacement_type VARCHAR(40) NOT NULL,
  status VARCHAR(20) NOT NULL,
  issued_on DATE NOT NULL,
  warranty_start_date DATE,
  warranty_end_date DATE,
  notes TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  CONSTRAINT uq_service_replacement_org_number UNIQUE (organization_id, replacement_number),
  CONSTRAINT chk_service_replacement_status CHECK (status IN ('ISSUED','CANCELLED')),
  CONSTRAINT chk_service_replacement_type CHECK (replacement_type IN ('WARRANTY_REPLACEMENT','SALES_RETURN_REPLACEMENT','GOODWILL_REPLACEMENT')),
  CONSTRAINT chk_service_replacement_qty CHECK (replacement_quantity > 0 AND replacement_base_quantity > 0)
);

CREATE INDEX IF NOT EXISTS idx_service_replacement_ticket ON service_replacement(service_ticket_id);
CREATE INDEX IF NOT EXISTS idx_service_replacement_claim ON service_replacement(warranty_claim_id);
CREATE INDEX IF NOT EXISTS idx_service_replacement_customer ON service_replacement(customer_id, issued_on DESC);
