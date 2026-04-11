ALTER TABLE service_replacement
  ADD COLUMN IF NOT EXISTS stock_source_bucket VARCHAR(40) NOT NULL DEFAULT 'SALEABLE';

ALTER TABLE warranty_claim
  ADD COLUMN IF NOT EXISTS upstream_route_type VARCHAR(40),
  ADD COLUMN IF NOT EXISTS upstream_company_name VARCHAR(255),
  ADD COLUMN IF NOT EXISTS upstream_reference_number VARCHAR(120),
  ADD COLUMN IF NOT EXISTS upstream_status VARCHAR(40),
  ADD COLUMN IF NOT EXISTS routed_on DATE;
