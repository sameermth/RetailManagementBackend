ALTER TABLE service_ticket
  ADD COLUMN IF NOT EXISTS sales_return_id BIGINT REFERENCES sales_return(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_service_ticket_sales_return ON service_ticket(sales_return_id);

ALTER TABLE warranty_claim
  ADD COLUMN IF NOT EXISTS product_ownership_id BIGINT REFERENCES product_ownership(id) ON DELETE SET NULL,
  ADD COLUMN IF NOT EXISTS sales_invoice_id BIGINT REFERENCES sales_invoice(id) ON DELETE SET NULL,
  ADD COLUMN IF NOT EXISTS sales_return_id BIGINT REFERENCES sales_return(id) ON DELETE SET NULL,
  ADD COLUMN IF NOT EXISTS warranty_start_date DATE,
  ADD COLUMN IF NOT EXISTS warranty_end_date DATE;

CREATE INDEX IF NOT EXISTS idx_warranty_claim_ownership ON warranty_claim(product_ownership_id);
CREATE INDEX IF NOT EXISTS idx_warranty_claim_sales_return ON warranty_claim(sales_return_id);
