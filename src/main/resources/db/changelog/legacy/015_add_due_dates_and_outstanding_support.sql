ALTER TABLE sales_invoice
    ADD COLUMN IF NOT EXISTS due_date DATE;

ALTER TABLE purchase_receipt
    ADD COLUMN IF NOT EXISTS due_date DATE;

UPDATE sales_invoice
SET due_date = invoice_date
WHERE due_date IS NULL;

UPDATE purchase_receipt
SET due_date = receipt_date
WHERE due_date IS NULL;

ALTER TABLE sales_invoice
    ALTER COLUMN due_date SET NOT NULL;

ALTER TABLE purchase_receipt
    ALTER COLUMN due_date SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_sales_invoice_org_due_date
    ON sales_invoice(organization_id, due_date, status);

CREATE INDEX IF NOT EXISTS idx_purchase_receipt_org_due_date
    ON purchase_receipt(organization_id, due_date, status);
