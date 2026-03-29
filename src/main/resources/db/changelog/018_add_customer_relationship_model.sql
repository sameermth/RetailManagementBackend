ALTER TABLE customer
  ADD COLUMN IF NOT EXISTS linked_organization_id BIGINT REFERENCES organization(id) ON DELETE SET NULL,
  ADD COLUMN IF NOT EXISTS customer_type VARCHAR(20),
  ADD COLUMN IF NOT EXISTS legal_name VARCHAR(200),
  ADD COLUMN IF NOT EXISTS trade_name VARCHAR(200),
  ADD COLUMN IF NOT EXISTS billing_address TEXT,
  ADD COLUMN IF NOT EXISTS shipping_address TEXT,
  ADD COLUMN IF NOT EXISTS state VARCHAR(100),
  ADD COLUMN IF NOT EXISTS state_code VARCHAR(10),
  ADD COLUMN IF NOT EXISTS contact_person_name VARCHAR(200),
  ADD COLUMN IF NOT EXISTS contact_person_phone VARCHAR(30),
  ADD COLUMN IF NOT EXISTS contact_person_email VARCHAR(150),
  ADD COLUMN IF NOT EXISTS is_platform_linked BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE customer
SET legal_name = COALESCE(NULLIF(TRIM(full_name), ''), legal_name),
    trade_name = COALESCE(NULLIF(TRIM(full_name), ''), trade_name),
    customer_type = COALESCE(customer_type, CASE WHEN gstin IS NULL OR BTRIM(gstin) = '' THEN 'INDIVIDUAL' ELSE 'BUSINESS' END),
    billing_address = COALESCE(billing_address, NULLIF(BTRIM(notes), '')),
    shipping_address = COALESCE(shipping_address, NULLIF(BTRIM(notes), ''))
WHERE legal_name IS NULL
   OR trade_name IS NULL
   OR customer_type IS NULL
   OR billing_address IS NULL
   OR shipping_address IS NULL;

ALTER TABLE customer
  ALTER COLUMN legal_name SET NOT NULL,
  ALTER COLUMN trade_name SET NOT NULL,
  ALTER COLUMN customer_type SET NOT NULL;

ALTER TABLE customer
  ADD CONSTRAINT chk_customer_type CHECK (customer_type IN ('INDIVIDUAL','BUSINESS'));

CREATE TABLE IF NOT EXISTS store_customer_terms (
  id                    BIGSERIAL PRIMARY KEY,
  organization_id       BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  customer_id           BIGINT NOT NULL REFERENCES customer(id) ON DELETE CASCADE,
  customer_segment      VARCHAR(30) NOT NULL DEFAULT 'RETAIL',
  credit_limit          NUMERIC(18,2) NOT NULL DEFAULT 0,
  credit_days           INTEGER,
  loyalty_enabled       BOOLEAN NOT NULL DEFAULT FALSE,
  loyalty_points_balance NUMERIC(18,2) NOT NULL DEFAULT 0,
  price_tier            VARCHAR(50),
  discount_policy       VARCHAR(100),
  is_preferred          BOOLEAN NOT NULL DEFAULT FALSE,
  is_active             BOOLEAN NOT NULL DEFAULT TRUE,
  contract_start        DATE,
  contract_end          DATE,
  remarks               TEXT,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  CONSTRAINT uq_store_customer_terms UNIQUE (organization_id, customer_id),
  CONSTRAINT chk_store_customer_segment CHECK (customer_segment IN ('RETAIL','WHOLESALE','B2B','DEALER','DISTRIBUTOR')),
  CONSTRAINT chk_store_customer_loyalty_points_non_negative CHECK (loyalty_points_balance >= 0)
);

INSERT INTO store_customer_terms (
  organization_id,
  customer_id,
  customer_segment,
  credit_limit,
  credit_days,
  loyalty_enabled,
  loyalty_points_balance,
  is_preferred,
  is_active,
  remarks,
  created_at,
  updated_at,
  created_by,
  updated_by
)
SELECT c.organization_id,
       c.id,
       CASE WHEN c.customer_type = 'BUSINESS' THEN 'B2B' ELSE 'RETAIL' END,
       COALESCE(c.credit_limit, 0),
       NULL,
       FALSE,
       0,
       FALSE,
       c.status = 'ACTIVE',
       c.notes,
       c.created_at,
       c.updated_at,
       c.created_by,
       c.updated_by
FROM customer c
WHERE NOT EXISTS (
  SELECT 1
  FROM store_customer_terms sct
  WHERE sct.organization_id = c.organization_id
    AND sct.customer_id = c.id
);

CREATE INDEX IF NOT EXISTS idx_customer_org_linked_org ON customer (organization_id, linked_organization_id);
CREATE INDEX IF NOT EXISTS idx_store_customer_terms_org_customer ON store_customer_terms (organization_id, customer_id);
