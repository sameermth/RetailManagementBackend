ALTER TABLE supplier
    ADD COLUMN IF NOT EXISTS linked_organization_id BIGINT REFERENCES organization(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS legal_name VARCHAR(200),
    ADD COLUMN IF NOT EXISTS trade_name VARCHAR(200),
    ADD COLUMN IF NOT EXISTS billing_address TEXT,
    ADD COLUMN IF NOT EXISTS shipping_address TEXT,
    ADD COLUMN IF NOT EXISTS state VARCHAR(100),
    ADD COLUMN IF NOT EXISTS state_code VARCHAR(10),
    ADD COLUMN IF NOT EXISTS contact_person_name VARCHAR(200),
    ADD COLUMN IF NOT EXISTS contact_person_phone VARCHAR(30),
    ADD COLUMN IF NOT EXISTS contact_person_email VARCHAR(150),
    ADD COLUMN IF NOT EXISTS payment_terms VARCHAR(120),
    ADD COLUMN IF NOT EXISTS is_platform_linked BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE supplier
SET legal_name = COALESCE(legal_name, name),
    trade_name = COALESCE(trade_name, name)
WHERE legal_name IS NULL
   OR trade_name IS NULL;

CREATE TABLE IF NOT EXISTS supplier_product (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
    supplier_id BIGINT NOT NULL REFERENCES supplier(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    supplier_product_code VARCHAR(100),
    supplier_product_name VARCHAR(200),
    priority INTEGER NOT NULL DEFAULT 1,
    is_preferred BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by BIGINT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by BIGINT,
    CONSTRAINT uq_supplier_product UNIQUE (organization_id, supplier_id, product_id)
);

CREATE TABLE IF NOT EXISTS store_supplier_terms (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
    supplier_id BIGINT NOT NULL REFERENCES supplier(id) ON DELETE CASCADE,
    payment_terms VARCHAR(120),
    credit_limit NUMERIC(18,2) NOT NULL DEFAULT 0,
    credit_days INTEGER,
    is_preferred BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    contract_start DATE,
    contract_end DATE,
    order_via_email BOOLEAN NOT NULL DEFAULT FALSE,
    order_via_whatsapp BOOLEAN NOT NULL DEFAULT FALSE,
    remarks TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by BIGINT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by BIGINT,
    CONSTRAINT uq_store_supplier_terms UNIQUE (organization_id, supplier_id)
);

ALTER TABLE purchase_order_line
    ADD COLUMN IF NOT EXISTS supplier_product_id BIGINT REFERENCES supplier_product(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS product_master_id BIGINT REFERENCES product(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS sku_snapshot VARCHAR(100),
    ADD COLUMN IF NOT EXISTS product_name_snapshot VARCHAR(200),
    ADD COLUMN IF NOT EXISTS supplier_product_code_snapshot VARCHAR(100);

ALTER TABLE purchase_receipt_line
    ADD COLUMN IF NOT EXISTS supplier_product_id BIGINT REFERENCES supplier_product(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS product_master_id BIGINT REFERENCES product(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS sku_snapshot VARCHAR(100),
    ADD COLUMN IF NOT EXISTS product_name_snapshot VARCHAR(200),
    ADD COLUMN IF NOT EXISTS supplier_product_code_snapshot VARCHAR(100);

INSERT INTO store_supplier_terms (
    organization_id, supplier_id, payment_terms, is_preferred, is_active, remarks, created_at, updated_at
)
SELECT DISTINCT
    s.organization_id,
    s.id,
    s.payment_terms,
    FALSE,
    CASE WHEN s.status = 'ACTIVE' THEN TRUE ELSE FALSE END,
    s.notes,
    NOW(),
    NOW()
FROM supplier s
ON CONFLICT (organization_id, supplier_id) DO NOTHING;

INSERT INTO supplier_product (
    organization_id, supplier_id, product_id, supplier_product_name, is_active, created_at, updated_at
)
SELECT DISTINCT
    po.organization_id,
    po.supplier_id,
    sp.product_id,
    COALESCE(pol.product_name_snapshot, sp.name),
    TRUE,
    NOW(),
    NOW()
FROM purchase_order po
JOIN purchase_order_line pol ON pol.purchase_order_id = po.id
JOIN store_product sp ON sp.id = pol.product_id
ON CONFLICT (organization_id, supplier_id, product_id) DO NOTHING;

INSERT INTO supplier_product (
    organization_id, supplier_id, product_id, supplier_product_name, is_active, created_at, updated_at
)
SELECT DISTINCT
    pr.organization_id,
    pr.supplier_id,
    sp.product_id,
    COALESCE(prl.product_name_snapshot, sp.name),
    TRUE,
    NOW(),
    NOW()
FROM purchase_receipt pr
JOIN purchase_receipt_line prl ON prl.purchase_receipt_id = pr.id
JOIN store_product sp ON sp.id = prl.product_id
ON CONFLICT (organization_id, supplier_id, product_id) DO NOTHING;

UPDATE purchase_order_line pol
SET product_master_id = sp.product_id,
    sku_snapshot = COALESCE(pol.sku_snapshot, sp.sku),
    product_name_snapshot = COALESCE(pol.product_name_snapshot, sp.name),
    supplier_product_id = COALESCE(
        pol.supplier_product_id,
        (
            SELECT sup.id
            FROM purchase_order po
            JOIN supplier_product sup
              ON sup.organization_id = po.organization_id
             AND sup.supplier_id = po.supplier_id
             AND sup.product_id = sp.product_id
            WHERE po.id = pol.purchase_order_id
            ORDER BY sup.is_preferred DESC, sup.priority ASC, sup.id ASC
            LIMIT 1
        )
    ),
    supplier_product_code_snapshot = COALESCE(
        pol.supplier_product_code_snapshot,
        (
            SELECT sup.supplier_product_code
            FROM purchase_order po
            JOIN supplier_product sup
              ON sup.organization_id = po.organization_id
             AND sup.supplier_id = po.supplier_id
             AND sup.product_id = sp.product_id
            WHERE po.id = pol.purchase_order_id
            ORDER BY sup.is_preferred DESC, sup.priority ASC, sup.id ASC
            LIMIT 1
        )
    )
FROM store_product sp
WHERE sp.id = pol.product_id;

UPDATE purchase_receipt_line prl
SET product_master_id = sp.product_id,
    sku_snapshot = COALESCE(prl.sku_snapshot, sp.sku),
    product_name_snapshot = COALESCE(prl.product_name_snapshot, sp.name),
    supplier_product_id = COALESCE(
        prl.supplier_product_id,
        (
            SELECT sup.id
            FROM purchase_receipt pr
            JOIN supplier_product sup
              ON sup.organization_id = pr.organization_id
             AND sup.supplier_id = pr.supplier_id
             AND sup.product_id = sp.product_id
            WHERE pr.id = prl.purchase_receipt_id
            ORDER BY sup.is_preferred DESC, sup.priority ASC, sup.id ASC
            LIMIT 1
        )
    ),
    supplier_product_code_snapshot = COALESCE(
        prl.supplier_product_code_snapshot,
        (
            SELECT sup.supplier_product_code
            FROM purchase_receipt pr
            JOIN supplier_product sup
              ON sup.organization_id = pr.organization_id
             AND sup.supplier_id = pr.supplier_id
             AND sup.product_id = sp.product_id
            WHERE pr.id = prl.purchase_receipt_id
            ORDER BY sup.is_preferred DESC, sup.priority ASC, sup.id ASC
            LIMIT 1
        )
    )
FROM store_product sp
WHERE sp.id = prl.product_id;

CREATE INDEX IF NOT EXISTS idx_supplier_product_supplier ON supplier_product(organization_id, supplier_id, is_active);
CREATE INDEX IF NOT EXISTS idx_supplier_product_product ON supplier_product(organization_id, product_id, is_active);
CREATE INDEX IF NOT EXISTS idx_store_supplier_terms_supplier ON store_supplier_terms(organization_id, supplier_id, is_active);
