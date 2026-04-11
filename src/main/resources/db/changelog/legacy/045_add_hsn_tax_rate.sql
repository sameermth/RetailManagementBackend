CREATE TABLE hsn_tax_rate (
  id                  BIGSERIAL PRIMARY KEY,
  hsn_code            VARCHAR(20) NOT NULL,
  cgst_rate           NUMERIC(10,2) NOT NULL DEFAULT 0,
  sgst_rate           NUMERIC(10,2) NOT NULL DEFAULT 0,
  igst_rate           NUMERIC(10,2) NOT NULL DEFAULT 0,
  cess_rate           NUMERIC(10,2) NOT NULL DEFAULT 0,
  effective_from      DATE NOT NULL,
  effective_to        DATE,
  is_active           BOOLEAN NOT NULL DEFAULT TRUE,
  source_name         VARCHAR(120),
  created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by          BIGINT,
  updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by          BIGINT,
  CONSTRAINT fk_hsn_tax_rate_hsn_code FOREIGN KEY (hsn_code) REFERENCES hsn_master(hsn_code),
  CONSTRAINT uq_hsn_tax_rate_start UNIQUE (hsn_code, effective_from)
);

CREATE INDEX idx_hsn_tax_rate_lookup ON hsn_tax_rate(hsn_code, effective_from, effective_to);

INSERT INTO hsn_tax_rate (
  hsn_code, cgst_rate, sgst_rate, igst_rate, cess_rate, effective_from, effective_to, is_active, source_name
)
SELECT
  hsn_code,
  cgst_rate,
  sgst_rate,
  igst_rate,
  cess_rate,
  COALESCE(effective_from, CURRENT_DATE),
  NULL,
  is_active,
  source_name
FROM hsn_master
WHERE NOT EXISTS (
  SELECT 1
  FROM hsn_tax_rate rate
  WHERE rate.hsn_code = hsn_master.hsn_code
    AND rate.effective_from = COALESCE(hsn_master.effective_from, CURRENT_DATE)
);
