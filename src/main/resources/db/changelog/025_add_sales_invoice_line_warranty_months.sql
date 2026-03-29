ALTER TABLE sales_invoice_line
    ADD COLUMN IF NOT EXISTS warranty_months INTEGER;

UPDATE sales_invoice_line
SET warranty_months = 0
WHERE warranty_months IS NULL;
