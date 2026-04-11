ALTER TABLE sales_invoice
  DROP CONSTRAINT IF EXISTS chk_sales_invoice_status;

ALTER TABLE sales_invoice
  ADD CONSTRAINT chk_sales_invoice_status
  CHECK (status IN (
    'DRAFT',
    'SUBMITTED',
    'PENDING_APPROVAL',
    'CONFIRMED',
    'POSTED',
    'PARTIALLY_PAID',
    'PAID',
    'RETURNED',
    'CANCELLED'
  ));
