ALTER TABLE store_product
    ADD COLUMN IF NOT EXISTS default_mrp numeric(18, 2);

ALTER TABLE inventory_batch
    ADD COLUMN IF NOT EXISTS batch_type VARCHAR(30) DEFAULT 'EXTERNAL_BATCH',
    ADD COLUMN IF NOT EXISTS source_document_type VARCHAR(40),
    ADD COLUMN IF NOT EXISTS source_document_id BIGINT,
    ADD COLUMN IF NOT EXISTS source_document_line_id BIGINT,
    ADD COLUMN IF NOT EXISTS purchase_unit_cost numeric(18, 2),
    ADD COLUMN IF NOT EXISTS suggested_sale_price numeric(18, 2),
    ADD COLUMN IF NOT EXISTS mrp numeric(18, 2);

UPDATE inventory_batch
SET batch_type = COALESCE(batch_type, 'EXTERNAL_BATCH')
WHERE batch_type IS NULL;

ALTER TABLE inventory_batch
    ALTER COLUMN batch_type SET NOT NULL;

ALTER TABLE purchase_receipt_line
    ADD COLUMN IF NOT EXISTS suggested_sale_price numeric(18, 2),
    ADD COLUMN IF NOT EXISTS mrp numeric(18, 2);

ALTER TABLE purchase_receipt_line_batch
    ADD COLUMN IF NOT EXISTS suggested_sale_price numeric(18, 2),
    ADD COLUMN IF NOT EXISTS mrp numeric(18, 2);

ALTER TABLE sales_invoice_line
    ADD COLUMN IF NOT EXISTS mrp numeric(18, 2);

ALTER TABLE sales_quote_line
    ADD COLUMN IF NOT EXISTS mrp numeric(18, 2);

ALTER TABLE sales_order_line
    ADD COLUMN IF NOT EXISTS mrp numeric(18, 2);
