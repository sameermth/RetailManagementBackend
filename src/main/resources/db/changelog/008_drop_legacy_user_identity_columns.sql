-- =========================================================
-- Remove legacy identity columns from app_user after person/account migration
-- employee_code stays because it remains an organization membership code.
-- =========================================================

ALTER TABLE app_user DROP CONSTRAINT IF EXISTS uq_user_org_email;

ALTER TABLE app_user
  DROP COLUMN IF EXISTS full_name,
  DROP COLUMN IF EXISTS email,
  DROP COLUMN IF EXISTS phone,
  DROP COLUMN IF EXISTS password_hash;
