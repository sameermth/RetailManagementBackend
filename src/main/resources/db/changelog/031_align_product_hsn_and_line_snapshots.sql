ALTER TABLE product
    ADD COLUMN IF NOT EXISTS hsn_code varchar(50);

ALTER TABLE purchase_order_line
    ADD COLUMN IF NOT EXISTS hsn_snapshot varchar(50);

ALTER TABLE purchase_receipt_line
    ADD COLUMN IF NOT EXISTS hsn_snapshot varchar(50);

ALTER TABLE sales_invoice_line
    ADD COLUMN IF NOT EXISTS hsn_snapshot varchar(50);

CREATE INDEX IF NOT EXISTS idx_product_hsn_code
    ON product(hsn_code);
