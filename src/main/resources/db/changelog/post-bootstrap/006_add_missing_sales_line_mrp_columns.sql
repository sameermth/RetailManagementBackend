ALTER TABLE sales_quote_line
    ADD COLUMN IF NOT EXISTS mrp numeric(18, 2);

ALTER TABLE sales_order_line
    ADD COLUMN IF NOT EXISTS mrp numeric(18, 2);
