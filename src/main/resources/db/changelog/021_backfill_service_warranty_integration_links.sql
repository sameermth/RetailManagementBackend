UPDATE service_ticket st
SET sales_return_id = sr.id
FROM sales_return sr
WHERE st.sales_return_id IS NULL
  AND st.customer_id = sr.customer_id
  AND st.sales_invoice_id = sr.original_sales_invoice_id
  AND st.source_type = 'REPLACEMENT';

UPDATE warranty_claim wc
SET product_ownership_id = po.id,
    sales_invoice_id = po.sales_invoice_id,
    warranty_start_date = po.warranty_start_date,
    warranty_end_date = po.warranty_end_date
FROM product_ownership po
WHERE wc.product_ownership_id IS NULL
  AND wc.customer_id = po.customer_id
  AND wc.product_id = po.product_id
  AND (
    (wc.serial_number_id IS NOT NULL AND wc.serial_number_id = po.serial_number_id)
    OR (wc.serial_number_id IS NULL AND po.serial_number_id IS NULL)
  );

UPDATE warranty_claim wc
SET sales_return_id = st.sales_return_id
FROM service_ticket st
WHERE wc.sales_return_id IS NULL
  AND wc.service_ticket_id = st.id
  AND st.sales_return_id IS NOT NULL;
