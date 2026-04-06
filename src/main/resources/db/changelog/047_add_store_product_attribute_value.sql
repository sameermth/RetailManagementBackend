CREATE TABLE store_product_attribute_value (
  id                      BIGSERIAL PRIMARY KEY,
  organization_id         BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  store_product_id        BIGINT NOT NULL REFERENCES store_product(id) ON DELETE CASCADE,
  attribute_definition_id BIGINT NOT NULL REFERENCES product_attribute_definition(id) ON DELETE CASCADE,
  value_text              VARCHAR(500),
  value_number            NUMERIC(18,6),
  value_boolean           BOOLEAN,
  value_date              DATE,
  value_option_id         BIGINT REFERENCES product_attribute_option(id) ON DELETE SET NULL,
  value_json              JSONB,
  created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by              BIGINT,
  updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by              BIGINT,
  CONSTRAINT uq_store_product_attribute_value UNIQUE (store_product_id, attribute_definition_id)
);

CREATE INDEX idx_store_product_attribute_value_lookup
  ON store_product_attribute_value (organization_id, store_product_id, attribute_definition_id);
