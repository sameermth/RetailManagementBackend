ALTER TABLE subscription_plan
  ADD COLUMN IF NOT EXISTS max_organizations INTEGER,
  ADD COLUMN IF NOT EXISTS is_unlimited_organizations BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE subscription_plan
SET max_organizations = CASE code
      WHEN 'STARTER' THEN 1
      WHEN 'GROWTH' THEN 3
      WHEN 'ENTERPRISE' THEN NULL
      ELSE max_organizations
    END,
    is_unlimited_organizations = CASE
      WHEN code = 'ENTERPRISE' THEN TRUE
      ELSE FALSE
    END
WHERE code IN ('STARTER', 'GROWTH', 'ENTERPRISE');

ALTER TABLE organization
  ADD COLUMN IF NOT EXISTS owner_account_id BIGINT;

ALTER TABLE organization
  ADD CONSTRAINT fk_organization_owner_account
  FOREIGN KEY (owner_account_id) REFERENCES auth_account(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_organization_owner_account
  ON organization(owner_account_id);

UPDATE organization o
SET owner_account_id = resolved.account_id
FROM (
    SELECT DISTINCT ON (u.organization_id)
           u.organization_id,
           u.account_id
    FROM app_user u
    JOIN role r ON r.id = u.role_id
    WHERE r.code = 'OWNER'
      AND u.account_id IS NOT NULL
    ORDER BY u.organization_id, u.id
) resolved
WHERE o.id = resolved.organization_id
  AND o.owner_account_id IS NULL;

UPDATE organization o
SET owner_account_id = fallback_owner.account_id
FROM (
    SELECT DISTINCT ON (u.organization_id)
           u.organization_id,
           u.account_id
    FROM app_user u
    WHERE u.account_id IS NOT NULL
    ORDER BY u.organization_id, u.id
) fallback_owner
WHERE o.id = fallback_owner.organization_id
  AND o.owner_account_id IS NULL;

ALTER TABLE organization
  ALTER COLUMN owner_account_id SET NOT NULL;

CREATE TABLE IF NOT EXISTS account_subscription (
  id                             BIGSERIAL PRIMARY KEY,
  account_id                     BIGINT NOT NULL REFERENCES auth_account(id) ON DELETE CASCADE,
  subscription_plan_id           BIGINT NOT NULL REFERENCES subscription_plan(id) ON DELETE RESTRICT,
  status                         VARCHAR(20) NOT NULL,
  starts_on                      DATE NOT NULL,
  ends_on                        DATE,
  auto_renew                     BOOLEAN NOT NULL DEFAULT FALSE,
  purchased_at                   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  grace_until                    DATE,
  notes                          TEXT,
  metadata_json                  JSONB NOT NULL DEFAULT '{}'::jsonb,
  created_at                     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by                     BIGINT,
  updated_at                     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by                     BIGINT,
  CONSTRAINT chk_account_subscription_status CHECK (status IN ('TRIALING','ACTIVE','PAST_DUE','CANCELLED','EXPIRED'))
);

CREATE INDEX IF NOT EXISTS idx_account_subscription_account_status
  ON account_subscription(account_id, status, starts_on DESC);

ALTER TABLE account_subscription
  ADD CONSTRAINT fk_account_subscription_created_by FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;

ALTER TABLE account_subscription
  ADD CONSTRAINT fk_account_subscription_updated_by FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;

INSERT INTO account_subscription (
  account_id,
  subscription_plan_id,
  status,
  starts_on,
  ends_on,
  auto_renew,
  purchased_at,
  grace_until,
  notes,
  metadata_json,
  created_at,
  created_by,
  updated_at,
  updated_by
)
SELECT owner_map.owner_account_id,
       owner_map.subscription_plan_id,
       owner_map.status,
       owner_map.starts_on,
       owner_map.ends_on,
       owner_map.auto_renew,
       owner_map.purchased_at,
       owner_map.grace_until,
       COALESCE(owner_map.notes, 'Backfilled from organization subscription'),
       (
         CASE
           WHEN owner_map.metadata_json IS NULL OR btrim(owner_map.metadata_json::text) IN ('', 'null') THEN '{}'::jsonb
           ELSE owner_map.metadata_json
         END
       ) || jsonb_build_object('source', 'migration-043'),
       NOW(),
       owner_map.created_by,
       NOW(),
       owner_map.updated_by
FROM (
    SELECT DISTINCT ON (o.owner_account_id)
           o.owner_account_id,
           os.subscription_plan_id,
           os.status,
           os.starts_on,
           os.ends_on,
           os.auto_renew,
           os.purchased_at,
           os.grace_until,
           os.notes,
           os.metadata_json,
           os.created_by,
           os.updated_by
    FROM organization o
    JOIN organization_subscription os ON os.organization_id = o.id
    WHERE o.owner_account_id IS NOT NULL
    ORDER BY o.owner_account_id,
             CASE os.status
               WHEN 'ACTIVE' THEN 0
               WHEN 'TRIALING' THEN 1
               WHEN 'PAST_DUE' THEN 2
               WHEN 'CANCELLED' THEN 3
               WHEN 'EXPIRED' THEN 4
               ELSE 9
             END,
             COALESCE(os.ends_on, DATE '2999-12-31') DESC,
             os.starts_on DESC,
             os.id DESC
) owner_map
LEFT JOIN account_subscription existing
  ON existing.account_id = owner_map.owner_account_id
WHERE existing.id IS NULL;

UPDATE organization o
SET subscription_version = COALESCE(o.subscription_version, 0) + 1
WHERE o.owner_account_id IS NOT NULL;
