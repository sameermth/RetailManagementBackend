UPDATE sales_invoice si
SET seller_tax_registration_id = tr.id,
    seller_gstin = tr.gstin
FROM tax_registration tr
WHERE si.organization_id = tr.organization_id
  AND tr.branch_id IS NULL
  AND tr.is_active = TRUE
  AND tr.is_default = TRUE
  AND si.seller_tax_registration_id IS NULL;

UPDATE sales_invoice si
SET customer_gstin = c.gstin
FROM customer c
WHERE si.customer_id = c.id
  AND si.customer_gstin IS NULL
  AND c.gstin IS NOT NULL
  AND btrim(c.gstin) <> '';

UPDATE sales_invoice
SET place_of_supply_state_code = COALESCE(
      NULLIF(SUBSTRING(customer_gstin FROM 1 FOR 2), ''),
      NULLIF(SUBSTRING(seller_gstin FROM 1 FOR 2), '')
    )
WHERE place_of_supply_state_code IS NULL;

UPDATE purchase_order po
SET seller_tax_registration_id = tr.id,
    seller_gstin = tr.gstin
FROM tax_registration tr
WHERE po.organization_id = tr.organization_id
  AND tr.branch_id IS NULL
  AND tr.is_active = TRUE
  AND tr.is_default = TRUE
  AND po.seller_tax_registration_id IS NULL;

UPDATE purchase_order po
SET supplier_gstin = s.gstin
FROM supplier s
WHERE po.supplier_id = s.id
  AND po.supplier_gstin IS NULL
  AND s.gstin IS NOT NULL
  AND btrim(s.gstin) <> '';

UPDATE purchase_order
SET place_of_supply_state_code = COALESCE(
      NULLIF(SUBSTRING(supplier_gstin FROM 1 FOR 2), ''),
      NULLIF(SUBSTRING(seller_gstin FROM 1 FOR 2), '')
    )
WHERE place_of_supply_state_code IS NULL;

UPDATE purchase_receipt pr
SET seller_tax_registration_id = tr.id,
    seller_gstin = tr.gstin
FROM tax_registration tr
WHERE pr.organization_id = tr.organization_id
  AND tr.branch_id IS NULL
  AND tr.is_active = TRUE
  AND tr.is_default = TRUE
  AND pr.seller_tax_registration_id IS NULL;

UPDATE purchase_receipt pr
SET supplier_gstin = s.gstin
FROM supplier s
WHERE pr.supplier_id = s.id
  AND pr.supplier_gstin IS NULL
  AND s.gstin IS NOT NULL
  AND btrim(s.gstin) <> '';

UPDATE purchase_receipt
SET place_of_supply_state_code = COALESCE(
      NULLIF(SUBSTRING(supplier_gstin FROM 1 FOR 2), ''),
      NULLIF(SUBSTRING(seller_gstin FROM 1 FOR 2), '')
    )
WHERE place_of_supply_state_code IS NULL;

UPDATE sales_invoice_line
SET taxable_amount = line_amount
WHERE taxable_amount = 0
  AND line_amount IS NOT NULL
  AND line_amount > 0;

UPDATE purchase_order_line
SET taxable_amount = line_amount
WHERE taxable_amount = 0
  AND line_amount IS NOT NULL
  AND line_amount > 0;

UPDATE purchase_receipt_line
SET taxable_amount = line_amount
WHERE taxable_amount = 0
  AND line_amount IS NOT NULL
  AND line_amount > 0;

WITH sales_line_tax AS (
  SELECT
    sil.id,
    sil.taxable_amount,
    sil.tax_rate,
    COALESCE(si.place_of_supply_state_code, SUBSTRING(si.customer_gstin FROM 1 FOR 2), SUBSTRING(si.seller_gstin FROM 1 FOR 2)) AS place_state_code,
    SUBSTRING(si.seller_gstin FROM 1 FOR 2) AS seller_state_code
  FROM sales_invoice_line sil
  JOIN sales_invoice si ON si.id = sil.sales_invoice_id
  WHERE sil.tax_rate > 0
    AND sil.taxable_amount > 0
    AND sil.cgst_amount = 0
    AND sil.sgst_amount = 0
    AND sil.igst_amount = 0
)
UPDATE sales_invoice_line sil
SET cgst_rate = CASE
      WHEN slt.place_state_code = slt.seller_state_code THEN ROUND(slt.tax_rate / 2.0, 4)
      ELSE 0
    END,
    cgst_amount = CASE
      WHEN slt.place_state_code = slt.seller_state_code THEN ROUND(slt.taxable_amount * (slt.tax_rate / 2.0) / 100.0, 2)
      ELSE 0
    END,
    sgst_rate = CASE
      WHEN slt.place_state_code = slt.seller_state_code THEN ROUND(slt.tax_rate / 2.0, 4)
      ELSE 0
    END,
    sgst_amount = CASE
      WHEN slt.place_state_code = slt.seller_state_code
        THEN ROUND((slt.taxable_amount * slt.tax_rate / 100.0) - ROUND(slt.taxable_amount * (slt.tax_rate / 2.0) / 100.0, 2), 2)
      ELSE 0
    END,
    igst_rate = CASE
      WHEN slt.place_state_code IS DISTINCT FROM slt.seller_state_code THEN slt.tax_rate
      ELSE 0
    END,
    igst_amount = CASE
      WHEN slt.place_state_code IS DISTINCT FROM slt.seller_state_code THEN ROUND(slt.taxable_amount * slt.tax_rate / 100.0, 2)
      ELSE 0
    END
FROM sales_line_tax slt
WHERE sil.id = slt.id;

WITH purchase_order_line_tax AS (
  SELECT
    pol.id,
    pol.taxable_amount,
    pol.tax_rate,
    COALESCE(po.place_of_supply_state_code, SUBSTRING(po.supplier_gstin FROM 1 FOR 2), SUBSTRING(po.seller_gstin FROM 1 FOR 2)) AS place_state_code,
    SUBSTRING(po.seller_gstin FROM 1 FOR 2) AS seller_state_code
  FROM purchase_order_line pol
  JOIN purchase_order po ON po.id = pol.purchase_order_id
  WHERE pol.tax_rate > 0
    AND pol.taxable_amount > 0
    AND pol.cgst_amount = 0
    AND pol.sgst_amount = 0
    AND pol.igst_amount = 0
)
UPDATE purchase_order_line pol
SET cgst_rate = CASE
      WHEN polt.place_state_code = polt.seller_state_code THEN ROUND(polt.tax_rate / 2.0, 4)
      ELSE 0
    END,
    cgst_amount = CASE
      WHEN polt.place_state_code = polt.seller_state_code THEN ROUND(polt.taxable_amount * (polt.tax_rate / 2.0) / 100.0, 2)
      ELSE 0
    END,
    sgst_rate = CASE
      WHEN polt.place_state_code = polt.seller_state_code THEN ROUND(polt.tax_rate / 2.0, 4)
      ELSE 0
    END,
    sgst_amount = CASE
      WHEN polt.place_state_code = polt.seller_state_code
        THEN ROUND((polt.taxable_amount * polt.tax_rate / 100.0) - ROUND(polt.taxable_amount * (polt.tax_rate / 2.0) / 100.0, 2), 2)
      ELSE 0
    END,
    igst_rate = CASE
      WHEN polt.place_state_code IS DISTINCT FROM polt.seller_state_code THEN polt.tax_rate
      ELSE 0
    END,
    igst_amount = CASE
      WHEN polt.place_state_code IS DISTINCT FROM polt.seller_state_code THEN ROUND(polt.taxable_amount * polt.tax_rate / 100.0, 2)
      ELSE 0
    END
FROM purchase_order_line_tax polt
WHERE pol.id = polt.id;

WITH purchase_receipt_line_tax AS (
  SELECT
    prl.id,
    prl.taxable_amount,
    prl.tax_rate,
    COALESCE(pr.place_of_supply_state_code, SUBSTRING(pr.supplier_gstin FROM 1 FOR 2), SUBSTRING(pr.seller_gstin FROM 1 FOR 2)) AS place_state_code,
    SUBSTRING(pr.seller_gstin FROM 1 FOR 2) AS seller_state_code
  FROM purchase_receipt_line prl
  JOIN purchase_receipt pr ON pr.id = prl.purchase_receipt_id
  WHERE prl.tax_rate > 0
    AND prl.taxable_amount > 0
    AND prl.cgst_amount = 0
    AND prl.sgst_amount = 0
    AND prl.igst_amount = 0
)
UPDATE purchase_receipt_line prl
SET cgst_rate = CASE
      WHEN prlt.place_state_code = prlt.seller_state_code THEN ROUND(prlt.tax_rate / 2.0, 4)
      ELSE 0
    END,
    cgst_amount = CASE
      WHEN prlt.place_state_code = prlt.seller_state_code THEN ROUND(prlt.taxable_amount * (prlt.tax_rate / 2.0) / 100.0, 2)
      ELSE 0
    END,
    sgst_rate = CASE
      WHEN prlt.place_state_code = prlt.seller_state_code THEN ROUND(prlt.tax_rate / 2.0, 4)
      ELSE 0
    END,
    sgst_amount = CASE
      WHEN prlt.place_state_code = prlt.seller_state_code
        THEN ROUND((prlt.taxable_amount * prlt.tax_rate / 100.0) - ROUND(prlt.taxable_amount * (prlt.tax_rate / 2.0) / 100.0, 2), 2)
      ELSE 0
    END,
    igst_rate = CASE
      WHEN prlt.place_state_code IS DISTINCT FROM prlt.seller_state_code THEN prlt.tax_rate
      ELSE 0
    END,
    igst_amount = CASE
      WHEN prlt.place_state_code IS DISTINCT FROM prlt.seller_state_code THEN ROUND(prlt.taxable_amount * prlt.tax_rate / 100.0, 2)
      ELSE 0
    END
FROM purchase_receipt_line_tax prlt
WHERE prl.id = prlt.id;
