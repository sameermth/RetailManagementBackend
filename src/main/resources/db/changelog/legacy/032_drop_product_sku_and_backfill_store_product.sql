UPDATE store_product sp
SET sku = p.sku
FROM product p
WHERE sp.product_id = p.id
  AND p.sku IS NOT NULL
  AND btrim(p.sku) <> ''
  AND (sp.sku IS NULL OR btrim(sp.sku) = '');

DROP INDEX IF EXISTS idx_product_sku;

ALTER TABLE product
    DROP COLUMN IF EXISTS sku;
