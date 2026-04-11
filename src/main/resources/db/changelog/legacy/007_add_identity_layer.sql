-- =========================================================
-- Identity layer for global person/account and org-scoped profiles
-- Keeps app_user as the organization membership record for now.
-- =========================================================

CREATE TABLE person (
  id                    BIGSERIAL PRIMARY KEY,
  legal_name            VARCHAR(200) NOT NULL,
  primary_phone         VARCHAR(30),
  primary_email         VARCHAR(150),
  phone_verified        BOOLEAN NOT NULL DEFAULT FALSE,
  email_verified        BOOLEAN NOT NULL DEFAULT FALSE,
  status                VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  legacy_source_type    VARCHAR(30),
  legacy_source_id      BIGINT,
  CONSTRAINT chk_person_status CHECK (status IN ('ACTIVE','INACTIVE','MERGED','BLOCKED'))
);

CREATE TABLE auth_account (
  id                    BIGSERIAL PRIMARY KEY,
  person_id             BIGINT NOT NULL REFERENCES person(id) ON DELETE CASCADE,
  login_identifier      VARCHAR(100) NOT NULL UNIQUE,
  password_hash         TEXT,
  is_active             BOOLEAN NOT NULL DEFAULT TRUE,
  is_locked             BOOLEAN NOT NULL DEFAULT FALSE,
  last_login_at         TIMESTAMPTZ,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  legacy_source_type    VARCHAR(30),
  legacy_source_id      BIGINT,
  CONSTRAINT uq_account_person UNIQUE (person_id)
);

CREATE TABLE organization_person_profile (
  id                    BIGSERIAL PRIMARY KEY,
  organization_id       BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  person_id             BIGINT NOT NULL REFERENCES person(id) ON DELETE CASCADE,
  display_name          VARCHAR(200) NOT NULL,
  phone_for_org         VARCHAR(30),
  email_for_org         VARCHAR(150),
  notes                 TEXT,
  tags                  JSONB NOT NULL DEFAULT '[]'::jsonb,
  preferred_language    VARCHAR(30),
  is_active             BOOLEAN NOT NULL DEFAULT TRUE,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  legacy_source_type    VARCHAR(30),
  legacy_source_id      BIGINT,
  CONSTRAINT uq_org_person_profile UNIQUE (organization_id, person_id)
);

ALTER TABLE app_user ADD COLUMN person_id BIGINT;
ALTER TABLE app_user ADD COLUMN account_id BIGINT;
ALTER TABLE customer ADD COLUMN organization_person_profile_id BIGINT;
ALTER TABLE supplier ADD COLUMN organization_person_profile_id BIGINT;
ALTER TABLE distributor ADD COLUMN organization_person_profile_id BIGINT;

INSERT INTO person (
  legal_name,
  primary_phone,
  primary_email,
  status,
  created_at,
  created_by,
  updated_at,
  updated_by,
  legacy_source_type,
  legacy_source_id
)
SELECT
  u.full_name,
  u.phone,
  u.email,
  CASE WHEN u.is_active THEN 'ACTIVE' ELSE 'INACTIVE' END,
  u.created_at,
  u.created_by,
  u.updated_at,
  u.updated_by,
  'APP_USER',
  u.id
FROM app_user u;

INSERT INTO auth_account (
  person_id,
  login_identifier,
  password_hash,
  is_active,
  is_locked,
  created_at,
  created_by,
  updated_at,
  updated_by,
  legacy_source_type,
  legacy_source_id
)
SELECT
  p.id,
  COALESCE(NULLIF(u.employee_code, ''), NULLIF(u.email, ''), 'user-' || u.id),
  u.password_hash,
  u.is_active,
  FALSE,
  u.created_at,
  u.created_by,
  u.updated_at,
  u.updated_by,
  'APP_USER',
  u.id
FROM app_user u
JOIN person p
  ON p.legacy_source_type = 'APP_USER'
 AND p.legacy_source_id = u.id;

INSERT INTO organization_person_profile (
  organization_id,
  person_id,
  display_name,
  phone_for_org,
  email_for_org,
  is_active,
  created_at,
  created_by,
  updated_at,
  updated_by,
  legacy_source_type,
  legacy_source_id
)
SELECT
  u.organization_id,
  p.id,
  u.full_name,
  u.phone,
  u.email,
  u.is_active,
  u.created_at,
  u.created_by,
  u.updated_at,
  u.updated_by,
  'APP_USER',
  u.id
FROM app_user u
JOIN person p
  ON p.legacy_source_type = 'APP_USER'
 AND p.legacy_source_id = u.id;

UPDATE app_user u
SET person_id = p.id,
    account_id = a.id
FROM person p
JOIN auth_account a
  ON a.legacy_source_type = 'APP_USER'
 AND a.legacy_source_id = p.legacy_source_id
WHERE p.legacy_source_type = 'APP_USER'
  AND p.legacy_source_id = u.id;

INSERT INTO person (
  legal_name,
  primary_phone,
  primary_email,
  status,
  created_at,
  created_by,
  updated_at,
  updated_by,
  legacy_source_type,
  legacy_source_id
)
SELECT
  c.full_name,
  c.phone,
  c.email,
  CASE WHEN c.status = 'ACTIVE' THEN 'ACTIVE' ELSE 'INACTIVE' END,
  c.created_at,
  c.created_by,
  c.updated_at,
  c.updated_by,
  'CUSTOMER',
  c.id
FROM customer c;

INSERT INTO organization_person_profile (
  organization_id,
  person_id,
  display_name,
  phone_for_org,
  email_for_org,
  notes,
  is_active,
  created_at,
  created_by,
  updated_at,
  updated_by,
  legacy_source_type,
  legacy_source_id
)
SELECT
  c.organization_id,
  p.id,
  c.full_name,
  c.phone,
  c.email,
  c.notes,
  c.status = 'ACTIVE',
  c.created_at,
  c.created_by,
  c.updated_at,
  c.updated_by,
  'CUSTOMER',
  c.id
FROM customer c
JOIN person p
  ON p.legacy_source_type = 'CUSTOMER'
 AND p.legacy_source_id = c.id;

UPDATE customer c
SET organization_person_profile_id = opp.id
FROM organization_person_profile opp
WHERE opp.legacy_source_type = 'CUSTOMER'
  AND opp.legacy_source_id = c.id;

INSERT INTO person (
  legal_name,
  primary_phone,
  primary_email,
  status,
  created_at,
  created_by,
  updated_at,
  updated_by,
  legacy_source_type,
  legacy_source_id
)
SELECT
  s.name,
  s.phone,
  s.email,
  CASE WHEN s.status = 'ACTIVE' THEN 'ACTIVE' ELSE 'INACTIVE' END,
  s.created_at,
  s.created_by,
  s.updated_at,
  s.updated_by,
  'SUPPLIER',
  s.id
FROM supplier s;

INSERT INTO organization_person_profile (
  organization_id,
  person_id,
  display_name,
  phone_for_org,
  email_for_org,
  is_active,
  created_at,
  created_by,
  updated_at,
  updated_by,
  legacy_source_type,
  legacy_source_id
)
SELECT
  s.organization_id,
  p.id,
  s.name,
  s.phone,
  s.email,
  s.status = 'ACTIVE',
  s.created_at,
  s.created_by,
  s.updated_at,
  s.updated_by,
  'SUPPLIER',
  s.id
FROM supplier s
JOIN person p
  ON p.legacy_source_type = 'SUPPLIER'
 AND p.legacy_source_id = s.id;

UPDATE supplier s
SET organization_person_profile_id = opp.id
FROM organization_person_profile opp
WHERE opp.legacy_source_type = 'SUPPLIER'
  AND opp.legacy_source_id = s.id;

INSERT INTO person (
  legal_name,
  primary_phone,
  primary_email,
  status,
  created_at,
  created_by,
  updated_at,
  updated_by,
  legacy_source_type,
  legacy_source_id
)
SELECT
  d.name,
  d.phone,
  d.email,
  CASE WHEN d.status = 'ACTIVE' THEN 'ACTIVE' ELSE 'INACTIVE' END,
  d.created_at,
  d.created_by,
  d.updated_at,
  d.updated_by,
  'DISTRIBUTOR',
  d.id
FROM distributor d;

INSERT INTO organization_person_profile (
  organization_id,
  person_id,
  display_name,
  phone_for_org,
  email_for_org,
  is_active,
  created_at,
  created_by,
  updated_at,
  updated_by,
  legacy_source_type,
  legacy_source_id
)
SELECT
  d.organization_id,
  p.id,
  d.name,
  d.phone,
  d.email,
  d.status = 'ACTIVE',
  d.created_at,
  d.created_by,
  d.updated_at,
  d.updated_by,
  'DISTRIBUTOR',
  d.id
FROM distributor d
JOIN person p
  ON p.legacy_source_type = 'DISTRIBUTOR'
 AND p.legacy_source_id = d.id;

UPDATE distributor d
SET organization_person_profile_id = opp.id
FROM organization_person_profile opp
WHERE opp.legacy_source_type = 'DISTRIBUTOR'
  AND opp.legacy_source_id = d.id;

ALTER TABLE app_user
  ALTER COLUMN person_id SET NOT NULL,
  ALTER COLUMN account_id SET NOT NULL;

ALTER TABLE app_user
  ADD CONSTRAINT fk_app_user_person FOREIGN KEY (person_id) REFERENCES person(id) ON DELETE RESTRICT,
  ADD CONSTRAINT fk_app_user_account FOREIGN KEY (account_id) REFERENCES auth_account(id) ON DELETE RESTRICT;

ALTER TABLE customer
  ADD CONSTRAINT fk_customer_org_person_profile FOREIGN KEY (organization_person_profile_id) REFERENCES organization_person_profile(id) ON DELETE SET NULL;
ALTER TABLE supplier
  ADD CONSTRAINT fk_supplier_org_person_profile FOREIGN KEY (organization_person_profile_id) REFERENCES organization_person_profile(id) ON DELETE SET NULL;
ALTER TABLE distributor
  ADD CONSTRAINT fk_distributor_org_person_profile FOREIGN KEY (organization_person_profile_id) REFERENCES organization_person_profile(id) ON DELETE SET NULL;

ALTER TABLE person ADD CONSTRAINT fk_person_created_by FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE person ADD CONSTRAINT fk_person_updated_by FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE auth_account ADD CONSTRAINT fk_auth_account_created_by FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE auth_account ADD CONSTRAINT fk_auth_account_updated_by FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE organization_person_profile ADD CONSTRAINT fk_org_person_profile_created_by FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE organization_person_profile ADD CONSTRAINT fk_org_person_profile_updated_by FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;

CREATE INDEX idx_person_primary_phone ON person(primary_phone);
CREATE INDEX idx_person_primary_email ON person(primary_email);
CREATE INDEX idx_org_person_profile_org_display_name ON organization_person_profile(organization_id, display_name);

ALTER TABLE person DROP COLUMN legacy_source_type;
ALTER TABLE person DROP COLUMN legacy_source_id;
ALTER TABLE auth_account DROP COLUMN legacy_source_type;
ALTER TABLE auth_account DROP COLUMN legacy_source_id;
ALTER TABLE organization_person_profile DROP COLUMN legacy_source_type;
ALTER TABLE organization_person_profile DROP COLUMN legacy_source_id;
