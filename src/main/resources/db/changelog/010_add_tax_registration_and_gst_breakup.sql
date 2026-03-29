CREATE TABLE tax_registration (
  id                    BIGSERIAL PRIMARY KEY,
  organization_id       BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  branch_id             BIGINT REFERENCES branch(id) ON DELETE CASCADE,
  registration_type     VARCHAR(20) NOT NULL DEFAULT 'GST',
  registration_name     VARCHAR(200) NOT NULL,
  legal_name            VARCHAR(250),
  gstin                 VARCHAR(30) NOT NULL,
  registration_state_code VARCHAR(10) NOT NULL,
  registration_state_name VARCHAR(100),
  effective_from        DATE NOT NULL,
  effective_to          DATE,
  is_default            BOOLEAN NOT NULL DEFAULT FALSE,
  is_active             BOOLEAN NOT NULL DEFAULT TRUE,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT chk_tax_registration_type CHECK (registration_type IN ('GST')),
  CONSTRAINT uq_tax_registration_gstin UNIQUE (gstin)
);

CREATE INDEX idx_tax_registration_org_branch_effective
  ON tax_registration(organization_id, branch_id, effective_from DESC, is_active);

ALTER TABLE tax_registration ADD CONSTRAINT fk_tax_registration_created_by
  FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE tax_registration ADD CONSTRAINT fk_tax_registration_updated_by
  FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;

ALTER TABLE purchase_order
  ADD COLUMN seller_tax_registration_id BIGINT REFERENCES tax_registration(id) ON DELETE SET NULL,
  ADD COLUMN seller_gstin VARCHAR(30),
  ADD COLUMN supplier_gstin VARCHAR(30),
  ADD COLUMN place_of_supply_state_code VARCHAR(10);

ALTER TABLE purchase_receipt
  ADD COLUMN seller_tax_registration_id BIGINT REFERENCES tax_registration(id) ON DELETE SET NULL,
  ADD COLUMN seller_gstin VARCHAR(30),
  ADD COLUMN supplier_gstin VARCHAR(30),
  ADD COLUMN place_of_supply_state_code VARCHAR(10);

ALTER TABLE sales_invoice
  ADD COLUMN seller_tax_registration_id BIGINT REFERENCES tax_registration(id) ON DELETE SET NULL,
  ADD COLUMN seller_gstin VARCHAR(30),
  ADD COLUMN customer_gstin VARCHAR(30),
  ADD COLUMN place_of_supply_state_code VARCHAR(10);

ALTER TABLE purchase_order_line
  ADD COLUMN taxable_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  ADD COLUMN cgst_rate NUMERIC(9,4) NOT NULL DEFAULT 0,
  ADD COLUMN cgst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  ADD COLUMN sgst_rate NUMERIC(9,4) NOT NULL DEFAULT 0,
  ADD COLUMN sgst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  ADD COLUMN igst_rate NUMERIC(9,4) NOT NULL DEFAULT 0,
  ADD COLUMN igst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  ADD COLUMN cess_rate NUMERIC(9,4) NOT NULL DEFAULT 0,
  ADD COLUMN cess_amount NUMERIC(18,2) NOT NULL DEFAULT 0;

ALTER TABLE purchase_receipt_line
  ADD COLUMN taxable_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  ADD COLUMN cgst_rate NUMERIC(9,4) NOT NULL DEFAULT 0,
  ADD COLUMN cgst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  ADD COLUMN sgst_rate NUMERIC(9,4) NOT NULL DEFAULT 0,
  ADD COLUMN sgst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  ADD COLUMN igst_rate NUMERIC(9,4) NOT NULL DEFAULT 0,
  ADD COLUMN igst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  ADD COLUMN cess_rate NUMERIC(9,4) NOT NULL DEFAULT 0,
  ADD COLUMN cess_amount NUMERIC(18,2) NOT NULL DEFAULT 0;

ALTER TABLE sales_invoice_line
  ADD COLUMN taxable_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  ADD COLUMN cgst_rate NUMERIC(9,4) NOT NULL DEFAULT 0,
  ADD COLUMN cgst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  ADD COLUMN sgst_rate NUMERIC(9,4) NOT NULL DEFAULT 0,
  ADD COLUMN sgst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  ADD COLUMN igst_rate NUMERIC(9,4) NOT NULL DEFAULT 0,
  ADD COLUMN igst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  ADD COLUMN cess_rate NUMERIC(9,4) NOT NULL DEFAULT 0,
  ADD COLUMN cess_amount NUMERIC(18,2) NOT NULL DEFAULT 0;

INSERT INTO tax_registration (
  organization_id,
  registration_type,
  registration_name,
  legal_name,
  gstin,
  registration_state_code,
  registration_state_name,
  effective_from,
  is_default,
  is_active
)
SELECT
  o.id,
  'GST',
  COALESCE(o.name, o.code),
  o.legal_name,
  o.gstin,
  SUBSTRING(o.gstin FROM 1 FOR 2),
  NULL,
  CURRENT_DATE,
  TRUE,
  TRUE
FROM organization o
WHERE o.gstin IS NOT NULL
  AND btrim(o.gstin) <> ''
  AND NOT EXISTS (
    SELECT 1
    FROM tax_registration tr
    WHERE tr.organization_id = o.id
      AND tr.branch_id IS NULL
      AND tr.gstin = o.gstin
  );
