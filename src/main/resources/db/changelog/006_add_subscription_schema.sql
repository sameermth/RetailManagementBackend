-- =========================================================
-- Subscription schema and organization subscription versioning
-- Used for feature gating and JWT invalidation when a plan changes.
-- =========================================================

ALTER TABLE organization
  ADD COLUMN subscription_version BIGINT NOT NULL DEFAULT 1;

CREATE TABLE subscription_plan (
  id                    BIGSERIAL PRIMARY KEY,
  code                  VARCHAR(50) NOT NULL UNIQUE,
  name                  VARCHAR(100) NOT NULL,
  description           TEXT,
  billing_period        VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
  is_active             BOOLEAN NOT NULL DEFAULT TRUE,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT chk_subscription_plan_billing_period CHECK (billing_period IN ('TRIAL','MONTHLY','QUARTERLY','YEARLY','CUSTOM'))
);

CREATE TABLE subscription_feature (
  id                    BIGSERIAL PRIMARY KEY,
  code                  VARCHAR(100) NOT NULL UNIQUE,
  name                  VARCHAR(150) NOT NULL,
  module_code           VARCHAR(50) NOT NULL,
  description           TEXT,
  is_active             BOOLEAN NOT NULL DEFAULT TRUE,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT
);

CREATE TABLE subscription_plan_feature (
  id                    BIGSERIAL PRIMARY KEY,
  subscription_plan_id  BIGINT NOT NULL REFERENCES subscription_plan(id) ON DELETE CASCADE,
  subscription_feature_id BIGINT NOT NULL REFERENCES subscription_feature(id) ON DELETE CASCADE,
  is_enabled            BOOLEAN NOT NULL DEFAULT TRUE,
  feature_limit         INTEGER,
  config_json           JSONB NOT NULL DEFAULT '{}'::jsonb,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT uq_subscription_plan_feature UNIQUE (subscription_plan_id, subscription_feature_id)
);

CREATE TABLE organization_subscription (
  id                    BIGSERIAL PRIMARY KEY,
  organization_id       BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  subscription_plan_id  BIGINT NOT NULL REFERENCES subscription_plan(id) ON DELETE RESTRICT,
  status                VARCHAR(20) NOT NULL,
  starts_on             DATE NOT NULL,
  ends_on               DATE,
  auto_renew            BOOLEAN NOT NULL DEFAULT FALSE,
  purchased_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  grace_until           DATE,
  notes                 TEXT,
  metadata_json         JSONB NOT NULL DEFAULT '{}'::jsonb,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT chk_organization_subscription_status CHECK (status IN ('TRIALING','ACTIVE','PAST_DUE','CANCELLED','EXPIRED'))
);

CREATE INDEX idx_organization_subscription_org_status ON organization_subscription(organization_id, status, starts_on DESC);
CREATE INDEX idx_subscription_plan_feature_plan ON subscription_plan_feature(subscription_plan_id);

ALTER TABLE subscription_plan ADD CONSTRAINT fk_subscription_plan_created_by FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE subscription_plan ADD CONSTRAINT fk_subscription_plan_updated_by FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE subscription_feature ADD CONSTRAINT fk_subscription_feature_created_by FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE subscription_feature ADD CONSTRAINT fk_subscription_feature_updated_by FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE subscription_plan_feature ADD CONSTRAINT fk_subscription_plan_feature_created_by FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE subscription_plan_feature ADD CONSTRAINT fk_subscription_plan_feature_updated_by FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE organization_subscription ADD CONSTRAINT fk_organization_subscription_created_by FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE organization_subscription ADD CONSTRAINT fk_organization_subscription_updated_by FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;

INSERT INTO subscription_plan (code, name, description, billing_period, is_active)
VALUES
  ('STARTER', 'Starter', 'Core operations for a single-location business', 'MONTHLY', TRUE),
  ('GROWTH', 'Growth', 'Multi-branch operations with service and expense modules', 'MONTHLY', TRUE),
  ('ENTERPRISE', 'Enterprise', 'Full access including approval workflows', 'CUSTOM', TRUE)
ON CONFLICT (code) DO NOTHING;

INSERT INTO subscription_feature (code, name, module_code, description, is_active)
VALUES
  ('dashboard', 'Dashboard', 'DASHBOARD', 'Access business dashboards', TRUE),
  ('masters', 'Masters', 'MASTERS', 'Manage core masters and settings data', TRUE),
  ('catalog', 'Catalog', 'CATALOG', 'Manage shared and store product catalog', TRUE),
  ('inventory', 'Inventory', 'INVENTORY', 'Track stock, serials, and batches', TRUE),
  ('purchases', 'Purchases', 'PURCHASES', 'Create purchase orders and receipts', TRUE),
  ('sales', 'Sales', 'SALES', 'Create sales invoices and receipts', TRUE),
  ('payments', 'Payments', 'PAYMENTS', 'Manage customer and supplier payments', TRUE),
  ('reports', 'Reports', 'REPORTS', 'View operational and financial reports', TRUE),
  ('expenses', 'Expenses', 'EXPENSES', 'Record and manage expenses', TRUE),
  ('service', 'Service', 'SERVICE', 'Manage service tickets and warranty claims', TRUE),
  ('settings', 'Settings', 'SETTINGS', 'Manage organization and application settings', TRUE),
  ('users', 'Users', 'USERS', 'Manage users and branch access', TRUE),
  ('multi_branch', 'Multi Branch', 'SETTINGS', 'Operate multiple counters/branches', TRUE),
  ('approvals', 'Approvals', 'APPROVALS', 'Use configurable approval workflows', TRUE)
ON CONFLICT (code) DO NOTHING;

WITH matrix(plan_code, feature_code, is_enabled) AS (
  VALUES
    ('STARTER','dashboard',TRUE),('STARTER','masters',TRUE),('STARTER','catalog',TRUE),('STARTER','inventory',TRUE),
    ('STARTER','purchases',TRUE),('STARTER','sales',TRUE),('STARTER','payments',TRUE),('STARTER','reports',TRUE),
    ('STARTER','settings',TRUE),('STARTER','users',TRUE),
    ('GROWTH','dashboard',TRUE),('GROWTH','masters',TRUE),('GROWTH','catalog',TRUE),('GROWTH','inventory',TRUE),
    ('GROWTH','purchases',TRUE),('GROWTH','sales',TRUE),('GROWTH','payments',TRUE),('GROWTH','reports',TRUE),
    ('GROWTH','expenses',TRUE),('GROWTH','service',TRUE),('GROWTH','settings',TRUE),('GROWTH','users',TRUE),
    ('GROWTH','multi_branch',TRUE),
    ('ENTERPRISE','dashboard',TRUE),('ENTERPRISE','masters',TRUE),('ENTERPRISE','catalog',TRUE),('ENTERPRISE','inventory',TRUE),
    ('ENTERPRISE','purchases',TRUE),('ENTERPRISE','sales',TRUE),('ENTERPRISE','payments',TRUE),('ENTERPRISE','reports',TRUE),
    ('ENTERPRISE','expenses',TRUE),('ENTERPRISE','service',TRUE),('ENTERPRISE','settings',TRUE),('ENTERPRISE','users',TRUE),
    ('ENTERPRISE','multi_branch',TRUE),('ENTERPRISE','approvals',TRUE)
)
INSERT INTO subscription_plan_feature (subscription_plan_id, subscription_feature_id, is_enabled)
SELECT sp.id, sf.id, matrix.is_enabled
FROM matrix
JOIN subscription_plan sp ON sp.code = matrix.plan_code
JOIN subscription_feature sf ON sf.code = matrix.feature_code
ON CONFLICT (subscription_plan_id, subscription_feature_id) DO NOTHING;

INSERT INTO organization_subscription (
  organization_id,
  subscription_plan_id,
  status,
  starts_on,
  auto_renew,
  notes,
  metadata_json
)
SELECT o.id,
       sp.id,
       'ACTIVE',
       CURRENT_DATE,
       FALSE,
       'Backfilled default subscription during subscription schema migration',
       jsonb_build_object('source', 'migration-006')
FROM organization o
JOIN subscription_plan sp ON sp.code = 'GROWTH'
LEFT JOIN organization_subscription os ON os.organization_id = o.id
WHERE os.id IS NULL;
