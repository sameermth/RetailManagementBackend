BEGIN;

INSERT INTO role (code, name, is_system)
VALUES ('PLATFORM_ADMIN', 'Platform Administrator', TRUE)
ON CONFLICT (code) DO NOTHING;

INSERT INTO permission (code, name, module_code)
VALUES ('platform.manage', 'Manage platform admin console', 'PLATFORM_ADMIN')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.code = 'platform.manage'
WHERE r.code = 'PLATFORM_ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO person (
  legal_name,
  primary_phone,
  primary_email,
  phone_verified,
  email_verified,
  status
)
SELECT
  'Retail Platform Admin',
  '+91-9990000099',
  'platform-admin@retailmanagement.test',
  TRUE,
  TRUE,
  'ACTIVE'
WHERE NOT EXISTS (
  SELECT 1
  FROM person
  WHERE primary_email = 'platform-admin@retailmanagement.test'
);

INSERT INTO auth_account (
  person_id,
  login_identifier,
  password_hash,
  is_active,
  is_locked
)
SELECT
  p.id,
  'PLAT-ADM-01',
  '{noop}secret123',
  TRUE,
  FALSE
FROM person p
WHERE p.primary_email = 'platform-admin@retailmanagement.test'
  AND NOT EXISTS (
    SELECT 1
    FROM auth_account a
    WHERE lower(a.login_identifier) = lower('PLAT-ADM-01')
  );

INSERT INTO organization (
  code,
  name,
  legal_name,
  phone,
  email,
  owner_account_id,
  is_active
)
SELECT
  'PLATFORM',
  'Retail Management Platform',
  'Retail Management Platform',
  '+91-9990000099',
  'platform-admin@retailmanagement.test',
  a.id,
  TRUE
FROM auth_account a
WHERE lower(a.login_identifier) = lower('PLAT-ADM-01')
  AND NOT EXISTS (
    SELECT 1
    FROM organization o
    WHERE o.code = 'PLATFORM'
  );

INSERT INTO organization_person_profile (
  organization_id,
  person_id,
  display_name,
  phone_for_org,
  email_for_org,
  is_active
)
SELECT
  o.id,
  p.id,
  'Retail Platform Admin',
  '+91-9990000099',
  'platform-admin@retailmanagement.test',
  TRUE
FROM organization o
JOIN person p ON p.primary_email = 'platform-admin@retailmanagement.test'
WHERE o.code = 'PLATFORM'
  AND NOT EXISTS (
    SELECT 1
    FROM organization_person_profile opp
    WHERE opp.organization_id = o.id
      AND opp.person_id = p.id
  );

INSERT INTO app_user (
  organization_id,
  person_id,
  account_id,
  default_branch_id,
  role_id,
  employee_code,
  is_active,
  joined_on
)
SELECT
  o.id,
  p.id,
  a.id,
  NULL,
  r.id,
  'PLAT-ADM-01',
  TRUE,
  CURRENT_DATE
FROM organization o
JOIN person p ON p.primary_email = 'platform-admin@retailmanagement.test'
JOIN auth_account a ON a.person_id = p.id AND lower(a.login_identifier) = lower('PLAT-ADM-01')
JOIN role r ON r.code = 'PLATFORM_ADMIN'
WHERE o.code = 'PLATFORM'
  AND NOT EXISTS (
    SELECT 1
    FROM app_user u
    WHERE u.organization_id = o.id
      AND u.employee_code = 'PLAT-ADM-01'
  );

COMMIT;
