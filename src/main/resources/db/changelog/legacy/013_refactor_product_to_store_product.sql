ALTER TABLE product RENAME TO store_product;
ALTER TABLE catalog_product RENAME TO product;

ALTER TABLE store_product RENAME COLUMN catalog_product_id TO product_id;

ALTER INDEX IF EXISTS uq_product_org_catalog_product RENAME TO uq_store_product_org_product;
ALTER INDEX IF EXISTS idx_catalog_product_name_brand RENAME TO idx_product_name_brand;
ALTER INDEX IF EXISTS idx_catalog_product_sku RENAME TO idx_product_sku;
ALTER INDEX IF EXISTS idx_erp_product_org_sku RENAME TO idx_store_product_org_sku;

ALTER TABLE store_product RENAME CONSTRAINT fk_product_catalog_product TO fk_store_product_product;

ALTER TABLE product RENAME CONSTRAINT fk_catalog_product_created_by TO fk_product_created_by;
ALTER TABLE product RENAME CONSTRAINT fk_catalog_product_updated_by TO fk_product_updated_by;
