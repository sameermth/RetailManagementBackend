-- =========================================================
-- Shared product catalog with organization/store association
-- Existing `product` rows remain the org/store-facing records used by
-- purchase, sales, invoice, and inventory tables. This migration adds
-- a central catalog record behind each org product and backfills links.
-- =========================================================

CREATE TABLE catalog_product (
  id                              BIGSERIAL PRIMARY KEY,
  sku                             VARCHAR(80),
  name                            VARCHAR(200) NOT NULL,
  description                     TEXT,
  category_name                   VARCHAR(150),
  brand_name                      VARCHAR(150),
  base_uom_id                     BIGINT NOT NULL REFERENCES uom(id) ON DELETE RESTRICT,
  inventory_tracking_mode         VARCHAR(30) NOT NULL,
  serial_tracking_enabled         BOOLEAN NOT NULL DEFAULT FALSE,
  batch_tracking_enabled          BOOLEAN NOT NULL DEFAULT FALSE,
  expiry_tracking_enabled         BOOLEAN NOT NULL DEFAULT FALSE,
  fractional_quantity_allowed     BOOLEAN NOT NULL DEFAULT FALSE,
  is_service_item                 BOOLEAN NOT NULL DEFAULT FALSE,
  is_active                       BOOLEAN NOT NULL DEFAULT TRUE,
  created_at                      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by                      BIGINT,
  updated_at                      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by                      BIGINT
);

ALTER TABLE catalog_product ADD CONSTRAINT fk_catalog_product_created_by FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE catalog_product ADD CONSTRAINT fk_catalog_product_updated_by FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;

ALTER TABLE product ADD COLUMN catalog_product_id BIGINT;

INSERT INTO catalog_product (
  sku,
  name,
  description,
  category_name,
  brand_name,
  base_uom_id,
  inventory_tracking_mode,
  serial_tracking_enabled,
  batch_tracking_enabled,
  expiry_tracking_enabled,
  fractional_quantity_allowed,
  is_service_item,
  is_active,
  created_at,
  created_by,
  updated_at,
  updated_by
)
SELECT DISTINCT ON (
  lower(trim(p.name)),
  coalesce(lower(trim(b.name)), ''),
  p.base_uom_id,
  p.inventory_tracking_mode,
  p.serial_tracking_enabled,
  p.batch_tracking_enabled,
  p.expiry_tracking_enabled,
  p.fractional_quantity_allowed,
  p.is_service_item
)
  p.sku,
  p.name,
  p.description,
  c.name,
  b.name,
  p.base_uom_id,
  p.inventory_tracking_mode,
  p.serial_tracking_enabled,
  p.batch_tracking_enabled,
  p.expiry_tracking_enabled,
  p.fractional_quantity_allowed,
  p.is_service_item,
  p.is_active,
  p.created_at,
  p.created_by,
  p.updated_at,
  p.updated_by
FROM product p
LEFT JOIN category c ON c.id = p.category_id
LEFT JOIN brand b ON b.id = p.brand_id
ORDER BY
  lower(trim(p.name)),
  coalesce(lower(trim(b.name)), ''),
  p.base_uom_id,
  p.inventory_tracking_mode,
  p.serial_tracking_enabled,
  p.batch_tracking_enabled,
  p.expiry_tracking_enabled,
  p.fractional_quantity_allowed,
  p.is_service_item,
  p.id;

UPDATE product p
SET catalog_product_id = cp.id
FROM catalog_product cp
WHERE cp.name = p.name
  AND cp.base_uom_id = p.base_uom_id
  AND cp.inventory_tracking_mode = p.inventory_tracking_mode
  AND cp.serial_tracking_enabled = p.serial_tracking_enabled
  AND cp.batch_tracking_enabled = p.batch_tracking_enabled
  AND cp.expiry_tracking_enabled = p.expiry_tracking_enabled
  AND cp.fractional_quantity_allowed = p.fractional_quantity_allowed
  AND cp.is_service_item = p.is_service_item
  AND coalesce(cp.brand_name, '') = coalesce((SELECT b.name FROM brand b WHERE b.id = p.brand_id), '')
  AND coalesce(cp.category_name, '') = coalesce((SELECT c.name FROM category c WHERE c.id = p.category_id), '');

ALTER TABLE product
  ALTER COLUMN catalog_product_id SET NOT NULL;

ALTER TABLE product
  ADD CONSTRAINT fk_product_catalog_product
  FOREIGN KEY (catalog_product_id) REFERENCES catalog_product(id) ON DELETE RESTRICT;

CREATE UNIQUE INDEX uq_product_org_catalog_product ON product(organization_id, catalog_product_id);
CREATE INDEX idx_catalog_product_name_brand ON catalog_product(name, brand_name);
CREATE INDEX idx_catalog_product_sku ON catalog_product(sku);
