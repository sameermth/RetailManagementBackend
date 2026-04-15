ALTER TABLE sales_order
    ADD COLUMN IF NOT EXISTS expected_fulfillment_by DATE;

UPDATE sales_quote
SET valid_until = quote_date + INTERVAL '30 day'
WHERE valid_until IS NULL;

UPDATE sales_order
SET expected_fulfillment_by = order_date + INTERVAL '15 day'
WHERE expected_fulfillment_by IS NULL;
