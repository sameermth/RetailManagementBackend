ALTER TABLE store_product
  ADD COLUMN IF NOT EXISTS default_sale_price NUMERIC(18,2);

CREATE TABLE IF NOT EXISTS store_product_price (
  id BIGSERIAL PRIMARY KEY,
  organization_id BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  store_product_id BIGINT NOT NULL REFERENCES store_product(id) ON DELETE CASCADE,
  price_type VARCHAR(40) NOT NULL DEFAULT 'SELLING',
  customer_segment VARCHAR(40),
  price NUMERIC(18,2) NOT NULL,
  min_quantity NUMERIC(18,6),
  effective_from DATE NOT NULL,
  effective_to DATE,
  is_default BOOLEAN NOT NULL DEFAULT FALSE,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by BIGINT,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by BIGINT
);

CREATE INDEX IF NOT EXISTS idx_store_product_price_lookup
  ON store_product_price(organization_id, store_product_id, price_type, is_active, effective_from);

INSERT INTO store_product_price (
  organization_id,
  store_product_id,
  price_type,
  customer_segment,
  price,
  effective_from,
  is_default,
  is_active,
  created_at,
  updated_at
)
SELECT
  sp.organization_id,
  sp.id,
  'SELLING',
  'RETAIL',
  sp.default_sale_price,
  CURRENT_DATE,
  TRUE,
  TRUE,
  NOW(),
  NOW()
FROM store_product sp
WHERE sp.default_sale_price IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM store_product_price spp
    WHERE spp.store_product_id = sp.id
      AND spp.price_type = 'SELLING'
      AND COALESCE(spp.customer_segment, 'RETAIL') = 'RETAIL'
      AND spp.is_active = TRUE
  );
