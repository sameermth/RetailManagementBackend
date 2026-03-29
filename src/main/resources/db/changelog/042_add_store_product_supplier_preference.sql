CREATE TABLE IF NOT EXISTS store_product_supplier_preference (
  id BIGSERIAL PRIMARY KEY,
  organization_id BIGINT NOT NULL REFERENCES organization(id) ON DELETE RESTRICT,
  store_product_id BIGINT NOT NULL REFERENCES store_product(id) ON DELETE RESTRICT,
  supplier_id BIGINT NOT NULL REFERENCES supplier(id) ON DELETE RESTRICT,
  supplier_product_id BIGINT NOT NULL REFERENCES supplier_product(id) ON DELETE RESTRICT,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  remarks TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  CONSTRAINT uq_store_product_supplier_preference UNIQUE (organization_id, store_product_id)
);

CREATE INDEX IF NOT EXISTS idx_erp_store_product_supplier_pref_product
  ON store_product_supplier_preference (organization_id, store_product_id, is_active);

CREATE INDEX IF NOT EXISTS idx_erp_store_product_supplier_pref_supplier
  ON store_product_supplier_preference (organization_id, supplier_id, is_active);
