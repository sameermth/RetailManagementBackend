ALTER TABLE store_product
    ADD COLUMN IF NOT EXISTS default_warranty_months INTEGER,
    ADD COLUMN IF NOT EXISTS warranty_terms TEXT;

UPDATE store_product
SET default_warranty_months = 0
WHERE default_warranty_months IS NULL;
