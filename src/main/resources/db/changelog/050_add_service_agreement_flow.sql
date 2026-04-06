CREATE TABLE IF NOT EXISTS service_agreement (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organization(id),
    branch_id BIGINT NOT NULL REFERENCES branch(id),
    customer_id BIGINT NOT NULL REFERENCES customer(id),
    sales_invoice_id BIGINT NOT NULL REFERENCES sales_invoice(id),
    agreement_number VARCHAR(64) NOT NULL,
    agreement_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    service_start_date DATE NOT NULL,
    service_end_date DATE NOT NULL,
    labor_included BOOLEAN NOT NULL DEFAULT FALSE,
    parts_included BOOLEAN NOT NULL DEFAULT FALSE,
    preventive_visits_included INTEGER,
    visit_limit INTEGER,
    sla_hours INTEGER,
    agreement_amount NUMERIC(18, 2),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by BIGINT REFERENCES app_user(id),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT REFERENCES app_user(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_service_agreement_org_number
    ON service_agreement (organization_id, agreement_number);

CREATE INDEX IF NOT EXISTS idx_service_agreement_org_status
    ON service_agreement (organization_id, status);

CREATE INDEX IF NOT EXISTS idx_service_agreement_invoice
    ON service_agreement (sales_invoice_id);

ALTER TABLE service_agreement
    DROP CONSTRAINT IF EXISTS chk_service_agreement_status;

ALTER TABLE service_agreement
    ADD CONSTRAINT chk_service_agreement_status
    CHECK (status IN ('DRAFT', 'ACTIVE', 'EXPIRED', 'CANCELLED'));

ALTER TABLE service_agreement
    DROP CONSTRAINT IF EXISTS chk_service_agreement_type;

ALTER TABLE service_agreement
    ADD CONSTRAINT chk_service_agreement_type
    CHECK (agreement_type IN ('AMC', 'INSTALLATION_SUPPORT', 'SERVICE_CONTRACT', 'PREVENTIVE_MAINTENANCE'));

CREATE TABLE IF NOT EXISTS service_agreement_item (
    id BIGSERIAL PRIMARY KEY,
    service_agreement_id BIGINT NOT NULL REFERENCES service_agreement(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES store_product(id),
    product_ownership_id BIGINT REFERENCES product_ownership(id),
    sales_invoice_line_id BIGINT REFERENCES sales_invoice_line(id),
    serial_number_id BIGINT REFERENCES serial_number(id),
    coverage_scope VARCHAR(32) NOT NULL DEFAULT 'FULL',
    included_service_notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by BIGINT REFERENCES app_user(id),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT REFERENCES app_user(id)
);

CREATE INDEX IF NOT EXISTS idx_service_agreement_item_agreement
    ON service_agreement_item (service_agreement_id);

CREATE INDEX IF NOT EXISTS idx_service_agreement_item_ownership
    ON service_agreement_item (product_ownership_id);

CREATE INDEX IF NOT EXISTS idx_service_agreement_item_invoice_line
    ON service_agreement_item (sales_invoice_line_id);

ALTER TABLE service_agreement_item
    DROP CONSTRAINT IF EXISTS chk_service_agreement_item_coverage_scope;

ALTER TABLE service_agreement_item
    ADD CONSTRAINT chk_service_agreement_item_coverage_scope
    CHECK (coverage_scope IN ('FULL', 'LABOR_ONLY', 'PARTS_ONLY', 'VISIT_ONLY'));
