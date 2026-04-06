-- Consolidated bootstrap schema for fresh databases only.

-- Generated from legacy Liquibase history. Do not use on an already-migrated database.


-- ===== SOURCE: 001_create_erp_v4_schema.sql =====

BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =========================================================
-- ERP V4 - PostgreSQL schema
-- =========================================================
-- Notes:
-- 1) All business tables carry created_at/created_by/updated_at/updated_by.
-- 2) created_by and updated_by FKs are added at the end to avoid circular DDL issues.
-- 3) Quantities are stored as numeric(18,6); money as numeric(18,2).
-- 4) stock_movement is the inventory source of truth; inventory_balance is a derived/read model.
-- 5) audit_event is immutable business evidence; normalized tables remain operational truth.
-- =========================================================

-- -----------------------------
-- Utility functions / triggers
-- -----------------------------
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS trigger AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- =========================================================
-- Foundation / Access
-- =========================================================
CREATE TABLE organization (
  id                    BIGSERIAL PRIMARY KEY,
  code                  VARCHAR(50) NOT NULL UNIQUE,
  name                  VARCHAR(200) NOT NULL,
  legal_name            VARCHAR(250),
  phone                 VARCHAR(30),
  email                 VARCHAR(150),
  gstin                 VARCHAR(30),
  is_active             BOOLEAN NOT NULL DEFAULT TRUE,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT
);

CREATE TABLE branch (
  id                    BIGSERIAL PRIMARY KEY,
  organization_id       BIGINT NOT NULL REFERENCES organization(id) ON DELETE RESTRICT,
  code                  VARCHAR(50) NOT NULL,
  name                  VARCHAR(200) NOT NULL,
  phone                 VARCHAR(30),
  email                 VARCHAR(150),
  address_line1         VARCHAR(250),
  address_line2         VARCHAR(250),
  city                  VARCHAR(100),
  state                 VARCHAR(100),
  postal_code           VARCHAR(20),
  country               VARCHAR(100) DEFAULT 'India',
  is_active             BOOLEAN NOT NULL DEFAULT TRUE,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT uq_branch_org_code UNIQUE (organization_id, code)
);

CREATE TABLE warehouse (
  id                    BIGSERIAL PRIMARY KEY,
  organization_id       BIGINT NOT NULL REFERENCES organization(id) ON DELETE RESTRICT,
  branch_id             BIGINT NOT NULL REFERENCES branch(id) ON DELETE RESTRICT,
  code                  VARCHAR(50) NOT NULL,
  name                  VARCHAR(200) NOT NULL,
  warehouse_type        VARCHAR(40) NOT NULL DEFAULT 'STANDARD',
  is_primary            BOOLEAN NOT NULL DEFAULT FALSE,
  is_active             BOOLEAN NOT NULL DEFAULT TRUE,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT uq_warehouse_org_code UNIQUE (organization_id, code),
  CONSTRAINT chk_warehouse_type CHECK (warehouse_type IN ('STANDARD','STORE','GODOWN','SERVICE','TRANSIT','DAMAGED'))
);

CREATE TABLE role (
  id                    BIGSERIAL PRIMARY KEY,
  code                  VARCHAR(50) NOT NULL UNIQUE,
  name                  VARCHAR(100) NOT NULL,
  is_system             BOOLEAN NOT NULL DEFAULT FALSE,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT
);

CREATE TABLE permission (
  id                    BIGSERIAL PRIMARY KEY,
  code                  VARCHAR(100) NOT NULL UNIQUE,
  name                  VARCHAR(150) NOT NULL,
  module_code           VARCHAR(50) NOT NULL,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT
);

CREATE TABLE role_permission (
  id                    BIGSERIAL PRIMARY KEY,
  role_id               BIGINT NOT NULL REFERENCES role(id) ON DELETE CASCADE,
  permission_id         BIGINT NOT NULL REFERENCES permission(id) ON DELETE CASCADE,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT uq_role_permission UNIQUE (role_id, permission_id)
);

CREATE TABLE app_user (
  id                    BIGSERIAL PRIMARY KEY,
  organization_id       BIGINT NOT NULL REFERENCES organization(id) ON DELETE RESTRICT,
  default_branch_id     BIGINT REFERENCES branch(id) ON DELETE RESTRICT,
  role_id               BIGINT REFERENCES role(id) ON DELETE RESTRICT,
  employee_code         VARCHAR(50),
  full_name             VARCHAR(200) NOT NULL,
  email                 VARCHAR(150),
  phone                 VARCHAR(30),
  password_hash         TEXT,
  is_active             BOOLEAN NOT NULL DEFAULT TRUE,
  joined_on             DATE,
  exited_on             DATE,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT uq_user_org_employee_code UNIQUE (organization_id, employee_code),
  CONSTRAINT uq_user_org_email UNIQUE NULLS NOT DISTINCT (organization_id, email)
);

CREATE TABLE user_branch_access (
  id                    BIGSERIAL PRIMARY KEY,
  user_id               BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  branch_id             BIGINT NOT NULL REFERENCES branch(id) ON DELETE CASCADE,
  is_default            BOOLEAN NOT NULL DEFAULT FALSE,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT uq_user_branch UNIQUE (user_id, branch_id)
);

CREATE TABLE app_setting (
  id                    BIGSERIAL PRIMARY KEY,
  organization_id       BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  branch_id             BIGINT REFERENCES branch(id) ON DELETE CASCADE,
  setting_key           VARCHAR(120) NOT NULL,
  setting_value         JSONB NOT NULL,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT uq_setting_scope UNIQUE (organization_id, branch_id, setting_key)
);

CREATE TABLE document_sequence (
  id                    BIGSERIAL PRIMARY KEY,
  organization_id       BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  branch_id             BIGINT REFERENCES branch(id) ON DELETE CASCADE,
  document_type         VARCHAR(50) NOT NULL,
  prefix                VARCHAR(30),
  suffix                VARCHAR(30),
  next_number           BIGINT NOT NULL DEFAULT 1,
  padding_length        INTEGER NOT NULL DEFAULT 5,
  reset_policy          VARCHAR(20) NOT NULL DEFAULT 'NEVER',
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT uq_doc_seq UNIQUE (organization_id, branch_id, document_type),
  CONSTRAINT chk_reset_policy CHECK (reset_policy IN ('NEVER','YEARLY','MONTHLY'))
);

CREATE TABLE approval_rule (
  id                    BIGSERIAL PRIMARY KEY,
  organization_id       BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  branch_id             BIGINT REFERENCES branch(id) ON DELETE CASCADE,
  entity_type           VARCHAR(50) NOT NULL,
  approval_type         VARCHAR(50) NOT NULL,
  min_amount            NUMERIC(18,2),
  max_amount            NUMERIC(18,2),
  approver_role_id      BIGINT REFERENCES role(id) ON DELETE RESTRICT,
  priority_order        INTEGER NOT NULL DEFAULT 1,
  is_active             BOOLEAN NOT NULL DEFAULT TRUE,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT
);

CREATE TABLE approval_request (
  id                    BIGSERIAL PRIMARY KEY,
  organization_id       BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  branch_id             BIGINT REFERENCES branch(id) ON DELETE CASCADE,
  entity_type           VARCHAR(50) NOT NULL,
  entity_id             BIGINT NOT NULL,
  entity_number         VARCHAR(80),
  approval_type         VARCHAR(50) NOT NULL,
  status                VARCHAR(30) NOT NULL,
  requested_by          BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  requested_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  current_approver_user_id BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  current_approver_role_snapshot VARCHAR(100),
  request_reason        TEXT,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT chk_approval_status CHECK (status IN ('PENDING','APPROVED','REJECTED','CANCELLED'))
);

CREATE TABLE approval_history (
  id                    BIGSERIAL PRIMARY KEY,
  approval_request_id   BIGINT NOT NULL REFERENCES approval_request(id) ON DELETE CASCADE,
  approver_user_id      BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  action                VARCHAR(30) NOT NULL,
  approver_role_snapshot VARCHAR(100),
  remarks               TEXT,
  action_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT chk_approval_action CHECK (action IN ('REQUESTED','APPROVED','REJECTED','REASSIGNED','CANCELLED'))
);

-- =========================================================
-- Party masters
-- =========================================================
CREATE TABLE customer (
  id                    BIGSERIAL PRIMARY KEY,
  organization_id       BIGINT NOT NULL REFERENCES organization(id) ON DELETE RESTRICT,
  branch_id             BIGINT REFERENCES branch(id) ON DELETE RESTRICT,
  customer_code         VARCHAR(50) NOT NULL,
  full_name             VARCHAR(200) NOT NULL,
  phone                 VARCHAR(30),
  email                 VARCHAR(150),
  gstin                 VARCHAR(30),
  credit_limit          NUMERIC(18,2) NOT NULL DEFAULT 0,
  status                VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  notes                 TEXT,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT uq_customer_org_code UNIQUE (organization_id, customer_code),
  CONSTRAINT chk_customer_status CHECK (status IN ('ACTIVE','INACTIVE','BLOCKED'))
);

CREATE TABLE customer_address (
  id                    BIGSERIAL PRIMARY KEY,
  customer_id           BIGINT NOT NULL REFERENCES customer(id) ON DELETE CASCADE,
  address_type          VARCHAR(20) NOT NULL,
  line1                 VARCHAR(250) NOT NULL,
  line2                 VARCHAR(250),
  city                  VARCHAR(100),
  state                 VARCHAR(100),
  postal_code           VARCHAR(20),
  country               VARCHAR(100) DEFAULT 'India',
  is_default            BOOLEAN NOT NULL DEFAULT FALSE,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT chk_customer_address_type CHECK (address_type IN ('BILLING','SHIPPING','BOTH','OTHER'))
);

CREATE TABLE supplier (
  id                    BIGSERIAL PRIMARY KEY,
  organization_id       BIGINT NOT NULL REFERENCES organization(id) ON DELETE RESTRICT,
  branch_id             BIGINT REFERENCES branch(id) ON DELETE RESTRICT,
  supplier_code         VARCHAR(50) NOT NULL,
  name                  VARCHAR(200) NOT NULL,
  phone                 VARCHAR(30),
  email                 VARCHAR(150),
  gstin                 VARCHAR(30),
  status                VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  notes                 TEXT,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT uq_supplier_org_code UNIQUE (organization_id, supplier_code),
  CONSTRAINT chk_supplier_status CHECK (status IN ('ACTIVE','INACTIVE','BLOCKED'))
);

CREATE TABLE supplier_address (
  id                    BIGSERIAL PRIMARY KEY,
  supplier_id           BIGINT NOT NULL REFERENCES supplier(id) ON DELETE CASCADE,
  address_type          VARCHAR(20) NOT NULL,
  line1                 VARCHAR(250) NOT NULL,
  line2                 VARCHAR(250),
  city                  VARCHAR(100),
  state                 VARCHAR(100),
  postal_code           VARCHAR(20),
  country               VARCHAR(100) DEFAULT 'India',
  is_default            BOOLEAN NOT NULL DEFAULT FALSE,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT chk_supplier_address_type CHECK (address_type IN ('BILLING','SHIPPING','BOTH','OTHER'))
);

CREATE TABLE distributor (
  id                    BIGSERIAL PRIMARY KEY,
  organization_id       BIGINT NOT NULL REFERENCES organization(id) ON DELETE RESTRICT,
  branch_id             BIGINT REFERENCES branch(id) ON DELETE RESTRICT,
  distributor_code      VARCHAR(50) NOT NULL,
  name                  VARCHAR(200) NOT NULL,
  phone                 VARCHAR(30),
  email                 VARCHAR(150),
  gstin                 VARCHAR(30),
  status                VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  notes                 TEXT,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT uq_distributor_org_code UNIQUE (organization_id, distributor_code),
  CONSTRAINT chk_distributor_status CHECK (status IN ('ACTIVE','INACTIVE','BLOCKED'))
);

CREATE TABLE distributor_address (
  id                    BIGSERIAL PRIMARY KEY,
  distributor_id        BIGINT NOT NULL REFERENCES distributor(id) ON DELETE CASCADE,
  address_type          VARCHAR(20) NOT NULL,
  line1                 VARCHAR(250) NOT NULL,
  line2                 VARCHAR(250),
  city                  VARCHAR(100),
  state                 VARCHAR(100),
  postal_code           VARCHAR(20),
  country               VARCHAR(100) DEFAULT 'India',
  is_default            BOOLEAN NOT NULL DEFAULT FALSE,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT chk_distributor_address_type CHECK (address_type IN ('BILLING','SHIPPING','BOTH','OTHER'))
);

-- =========================================================
-- Product masters / UOM / Tax / Pricing
-- =========================================================
CREATE TABLE category (
  id                    BIGSERIAL PRIMARY KEY,
  organization_id       BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  parent_category_id    BIGINT REFERENCES category(id) ON DELETE SET NULL,
  name                  VARCHAR(150) NOT NULL,
  code                  VARCHAR(50),
  is_active             BOOLEAN NOT NULL DEFAULT TRUE,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT uq_category_org_name_parent UNIQUE (organization_id, parent_category_id, name)
);

CREATE TABLE brand (
  id                    BIGSERIAL PRIMARY KEY,
  organization_id       BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  name                  VARCHAR(150) NOT NULL,
  code                  VARCHAR(50),
  is_active             BOOLEAN NOT NULL DEFAULT TRUE,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT uq_brand_org_name UNIQUE (organization_id, name)
);

CREATE TABLE uom_group (
  id                    BIGSERIAL PRIMARY KEY,
  code                  VARCHAR(50) NOT NULL UNIQUE,
  name                  VARCHAR(100) NOT NULL,
  allows_fraction       BOOLEAN NOT NULL DEFAULT FALSE,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT
);

CREATE TABLE uom (
  id                    BIGSERIAL PRIMARY KEY,
  uom_group_id          BIGINT NOT NULL REFERENCES uom_group(id) ON DELETE RESTRICT,
  code                  VARCHAR(30) NOT NULL UNIQUE,
  name                  VARCHAR(60) NOT NULL,
  decimal_scale         INTEGER NOT NULL DEFAULT 0,
  is_active             BOOLEAN NOT NULL DEFAULT TRUE,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT
);

CREATE TABLE tax_group (
  id                    BIGSERIAL PRIMARY KEY,
  organization_id       BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  code                  VARCHAR(50) NOT NULL,
  name                  VARCHAR(100) NOT NULL,
  cgst_rate             NUMERIC(9,4) NOT NULL DEFAULT 0,
  sgst_rate             NUMERIC(9,4) NOT NULL DEFAULT 0,
  igst_rate             NUMERIC(9,4) NOT NULL DEFAULT 0,
  cess_rate             NUMERIC(9,4) NOT NULL DEFAULT 0,
  is_active             BOOLEAN NOT NULL DEFAULT TRUE,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT uq_tax_group_org_code UNIQUE (organization_id, code)
);

CREATE TABLE price_list (
  id                    BIGSERIAL PRIMARY KEY,
  organization_id       BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  code                  VARCHAR(50) NOT NULL,
  name                  VARCHAR(100) NOT NULL,
  price_list_type       VARCHAR(30) NOT NULL,
  valid_from            TIMESTAMPTZ,
  valid_to              TIMESTAMPTZ,
  is_active             BOOLEAN NOT NULL DEFAULT TRUE,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT uq_price_list_org_code UNIQUE (organization_id, code),
  CONSTRAINT chk_price_list_type CHECK (price_list_type IN ('MRP','RETAIL','WHOLESALE','DEALER','CUSTOM'))
);

CREATE TABLE product (
  id                    BIGSERIAL PRIMARY KEY,
  organization_id       BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  category_id           BIGINT REFERENCES category(id) ON DELETE SET NULL,
  brand_id              BIGINT REFERENCES brand(id) ON DELETE SET NULL,
  base_uom_id           BIGINT NOT NULL REFERENCES uom(id) ON DELETE RESTRICT,
  tax_group_id          BIGINT REFERENCES tax_group(id) ON DELETE SET NULL,
  sku                   VARCHAR(80) NOT NULL,
  name                  VARCHAR(200) NOT NULL,
  description           TEXT,
  inventory_tracking_mode VARCHAR(30) NOT NULL,
  serial_tracking_enabled BOOLEAN NOT NULL DEFAULT FALSE,
  batch_tracking_enabled  BOOLEAN NOT NULL DEFAULT FALSE,
  expiry_tracking_enabled BOOLEAN NOT NULL DEFAULT FALSE,
  fractional_quantity_allowed BOOLEAN NOT NULL DEFAULT FALSE,
  min_stock_base_qty    NUMERIC(18,6) NOT NULL DEFAULT 0,
  reorder_level_base_qty NUMERIC(18,6) NOT NULL DEFAULT 0,
  is_service_item       BOOLEAN NOT NULL DEFAULT FALSE,
  is_active             BOOLEAN NOT NULL DEFAULT TRUE,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT uq_product_org_sku UNIQUE (organization_id, sku),
  CONSTRAINT chk_product_tracking_mode CHECK (inventory_tracking_mode IN ('SIMPLE','SERIALIZED','BATCHED','BATCHED_EXPIRY','FRACTIONAL','MIXED_UOM'))
);

CREATE TABLE product_uom_conversion (
  id                    BIGSERIAL PRIMARY KEY,
  product_id            BIGINT NOT NULL REFERENCES product(id) ON DELETE CASCADE,
  from_uom_id           BIGINT NOT NULL REFERENCES uom(id) ON DELETE RESTRICT,
  to_uom_id             BIGINT NOT NULL REFERENCES uom(id) ON DELETE RESTRICT,
  multiplier            NUMERIC(18,6) NOT NULL,
  is_purchase_uom       BOOLEAN NOT NULL DEFAULT FALSE,
  is_sales_uom          BOOLEAN NOT NULL DEFAULT FALSE,
  is_default            BOOLEAN NOT NULL DEFAULT FALSE,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT uq_product_uom_conv UNIQUE (product_id, from_uom_id, to_uom_id),
  CONSTRAINT chk_product_uom_multiplier CHECK (multiplier > 0)
);

CREATE TABLE price_list_item (
  id                    BIGSERIAL PRIMARY KEY,
  price_list_id         BIGINT NOT NULL REFERENCES price_list(id) ON DELETE CASCADE,
  product_id            BIGINT NOT NULL REFERENCES product(id) ON DELETE CASCADE,
  uom_id                BIGINT NOT NULL REFERENCES uom(id) ON DELETE RESTRICT,
  price                 NUMERIC(18,2) NOT NULL,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT uq_price_list_item UNIQUE (price_list_id, product_id, uom_id),
  CONSTRAINT chk_price_list_item_price CHECK (price >= 0)
);

-- =========================================================
-- Inventory traceability
-- =========================================================
CREATE TABLE inventory_batch (
  id                    BIGSERIAL PRIMARY KEY,
  organization_id       BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  product_id            BIGINT NOT NULL REFERENCES product(id) ON DELETE RESTRICT,
  batch_number          VARCHAR(100) NOT NULL,
  manufacturer_batch_number VARCHAR(100),
  manufactured_on       DATE,
  expiry_on             DATE,
  status                VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT uq_batch_org_product_batch UNIQUE (organization_id, product_id, batch_number),
  CONSTRAINT chk_batch_status CHECK (status IN ('ACTIVE','EXPIRED','BLOCKED','CLOSED'))
);

CREATE TABLE serial_number (
  id                    BIGSERIAL PRIMARY KEY,
  organization_id       BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  product_id            BIGINT NOT NULL REFERENCES product(id) ON DELETE RESTRICT,
  batch_id              BIGINT REFERENCES inventory_batch(id) ON DELETE SET NULL,
  serial_number         VARCHAR(150) NOT NULL,
  manufacturer_serial_number VARCHAR(150),
  status                VARCHAR(20) NOT NULL,
  current_warehouse_id  BIGINT REFERENCES warehouse(id) ON DELETE SET NULL,
  current_customer_id   BIGINT REFERENCES customer(id) ON DELETE SET NULL,
  warranty_start_date   DATE,
  warranty_end_date     DATE,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT uq_serial_org_product_serial UNIQUE (organization_id, product_id, serial_number),
  CONSTRAINT chk_serial_status CHECK (status IN ('IN_STOCK','RESERVED','SOLD','RETURNED','REPLACED','SCRAPPED','SERVICE_IN','SERVICE_OUT'))
);

CREATE TABLE inventory_balance (
  id                    BIGSERIAL PRIMARY KEY,
  organization_id       BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  branch_id             BIGINT NOT NULL REFERENCES branch(id) ON DELETE RESTRICT,
  warehouse_id          BIGINT NOT NULL REFERENCES warehouse(id) ON DELETE RESTRICT,
  product_id            BIGINT NOT NULL REFERENCES product(id) ON DELETE RESTRICT,
  batch_id              BIGINT REFERENCES inventory_batch(id) ON DELETE SET NULL,
  on_hand_base_quantity NUMERIC(18,6) NOT NULL DEFAULT 0,
  reserved_base_quantity NUMERIC(18,6) NOT NULL DEFAULT 0,
  available_base_quantity NUMERIC(18,6) NOT NULL DEFAULT 0,
  avg_cost              NUMERIC(18,6) NOT NULL DEFAULT 0,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT uq_inventory_balance_scope UNIQUE (organization_id, branch_id, warehouse_id, product_id, batch_id)
);

CREATE TABLE stock_movement (
  id                    BIGSERIAL PRIMARY KEY,
  organization_id       BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  branch_id             BIGINT NOT NULL REFERENCES branch(id) ON DELETE RESTRICT,
  warehouse_id          BIGINT NOT NULL REFERENCES warehouse(id) ON DELETE RESTRICT,
  product_id            BIGINT NOT NULL REFERENCES product(id) ON DELETE RESTRICT,
  movement_type         VARCHAR(30) NOT NULL,
  reference_type        VARCHAR(50) NOT NULL,
  reference_id          BIGINT NOT NULL,
  reference_number      VARCHAR(100),
  direction             VARCHAR(3) NOT NULL,
  uom_id                BIGINT NOT NULL REFERENCES uom(id) ON DELETE RESTRICT,
  quantity              NUMERIC(18,6) NOT NULL,
  base_quantity         NUMERIC(18,6) NOT NULL,
  unit_cost             NUMERIC(18,6) NOT NULL DEFAULT 0,
  total_cost            NUMERIC(18,6) NOT NULL DEFAULT 0,
  movement_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT chk_stock_direction CHECK (direction IN ('IN','OUT')),
  CONSTRAINT chk_stock_quantity_nonzero CHECK (quantity <> 0),
  CONSTRAINT chk_stock_base_quantity_nonzero CHECK (base_quantity <> 0),
  CONSTRAINT chk_stock_movement_type CHECK (movement_type IN ('PURCHASE_RECEIPT','SALES_INVOICE','SALES_RETURN','PURCHASE_RETURN','TRANSFER_OUT','TRANSFER_IN','ADJUSTMENT_IN','ADJUSTMENT_OUT','RESERVATION','UNRESERVATION','SERVICE_OUT','SERVICE_IN','OPENING_STOCK'))
);

CREATE TABLE stock_movement_serial (
  id                    BIGSERIAL PRIMARY KEY,
  stock_movement_id     BIGINT NOT NULL REFERENCES stock_movement(id) ON DELETE CASCADE,
  serial_number_id      BIGINT NOT NULL REFERENCES serial_number(id) ON DELETE RESTRICT,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT uq_stock_movement_serial UNIQUE (stock_movement_id, serial_number_id)
);

CREATE TABLE stock_movement_batch (
  id                    BIGSERIAL PRIMARY KEY,
  stock_movement_id     BIGINT NOT NULL REFERENCES stock_movement(id) ON DELETE CASCADE,
  batch_id              BIGINT NOT NULL REFERENCES inventory_batch(id) ON DELETE RESTRICT,
  quantity              NUMERIC(18,6) NOT NULL,
  base_quantity         NUMERIC(18,6) NOT NULL,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT chk_stock_movement_batch_qty CHECK (quantity <> 0 AND base_quantity <> 0)
);

CREATE TABLE inventory_reservation (
  id                    BIGSERIAL PRIMARY KEY,
  organization_id       BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  branch_id             BIGINT NOT NULL REFERENCES branch(id) ON DELETE RESTRICT,
  warehouse_id          BIGINT NOT NULL REFERENCES warehouse(id) ON DELETE RESTRICT,
  product_id            BIGINT NOT NULL REFERENCES product(id) ON DELETE RESTRICT,
  source_document_type  VARCHAR(50) NOT NULL,
  source_document_id    BIGINT NOT NULL,
  source_document_line_id BIGINT,
  reserved_base_quantity NUMERIC(18,6) NOT NULL,
  status                VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT chk_inventory_reservation_qty CHECK (reserved_base_quantity > 0),
  CONSTRAINT chk_inventory_reservation_status CHECK (status IN ('ACTIVE','RELEASED','CONSUMED','CANCELLED'))
);

CREATE TABLE stock_transfer (
  id                    BIGSERIAL PRIMARY KEY,
  organization_id       BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  branch_id             BIGINT NOT NULL REFERENCES branch(id) ON DELETE RESTRICT,
  from_warehouse_id     BIGINT NOT NULL REFERENCES warehouse(id) ON DELETE RESTRICT,
  to_warehouse_id       BIGINT NOT NULL REFERENCES warehouse(id) ON DELETE RESTRICT,
  transfer_number       VARCHAR(80) NOT NULL,
  transfer_date         DATE NOT NULL,
  status                VARCHAR(20) NOT NULL,
  remarks               TEXT,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT uq_stock_transfer_org_number UNIQUE (organization_id, transfer_number),
  CONSTRAINT chk_stock_transfer_status CHECK (status IN ('DRAFT','POSTED','CANCELLED')),
  CONSTRAINT chk_stock_transfer_wh CHECK (from_warehouse_id <> to_warehouse_id)
);

CREATE TABLE stock_transfer_line (
  id                    BIGSERIAL PRIMARY KEY,
  stock_transfer_id     BIGINT NOT NULL REFERENCES stock_transfer(id) ON DELETE CASCADE,
  product_id            BIGINT NOT NULL REFERENCES product(id) ON DELETE RESTRICT,
  uom_id                BIGINT NOT NULL REFERENCES uom(id) ON DELETE RESTRICT,
  quantity              NUMERIC(18,6) NOT NULL,
  base_quantity         NUMERIC(18,6) NOT NULL,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT chk_stock_transfer_line_qty CHECK (quantity > 0 AND base_quantity > 0)
);

CREATE TABLE stock_adjustment (
  id                    BIGSERIAL PRIMARY KEY,
  organization_id       BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  branch_id             BIGINT NOT NULL REFERENCES branch(id) ON DELETE RESTRICT,
  warehouse_id          BIGINT NOT NULL REFERENCES warehouse(id) ON DELETE RESTRICT,
  adjustment_number     VARCHAR(80) NOT NULL,
  adjustment_date       DATE NOT NULL,
  reason                VARCHAR(120) NOT NULL,
  status                VARCHAR(20) NOT NULL,
  remarks               TEXT,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT uq_stock_adj_org_number UNIQUE (organization_id, adjustment_number),
  CONSTRAINT chk_stock_adjustment_status CHECK (status IN ('DRAFT','APPROVED','POSTED','CANCELLED'))
);

CREATE TABLE stock_adjustment_line (
  id                    BIGSERIAL PRIMARY KEY,
  stock_adjustment_id   BIGINT NOT NULL REFERENCES stock_adjustment(id) ON DELETE CASCADE,
  product_id            BIGINT NOT NULL REFERENCES product(id) ON DELETE RESTRICT,
  uom_id                BIGINT NOT NULL REFERENCES uom(id) ON DELETE RESTRICT,
  quantity_delta        NUMERIC(18,6) NOT NULL,
  base_quantity_delta   NUMERIC(18,6) NOT NULL,
  unit_cost             NUMERIC(18,6) NOT NULL DEFAULT 0,
  line_reason           VARCHAR(120),
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT chk_stock_adjustment_delta_nonzero CHECK (quantity_delta <> 0 AND base_quantity_delta <> 0)
);

-- =========================================================
-- Commercial - Purchase / Sales / Payments
-- =========================================================
CREATE TABLE purchase_order (
  id                    BIGSERIAL PRIMARY KEY,
  organization_id       BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  branch_id             BIGINT NOT NULL REFERENCES branch(id) ON DELETE RESTRICT,
  supplier_id           BIGINT NOT NULL REFERENCES supplier(id) ON DELETE RESTRICT,
  po_number             VARCHAR(80) NOT NULL,
  po_date               DATE NOT NULL,
  status                VARCHAR(20) NOT NULL,
  subtotal              NUMERIC(18,2) NOT NULL DEFAULT 0,
  tax_amount            NUMERIC(18,2) NOT NULL DEFAULT 0,
  total_amount          NUMERIC(18,2) NOT NULL DEFAULT 0,
  remarks               TEXT,
  submitted_at          TIMESTAMPTZ,
  submitted_by          BIGINT,
  approved_at           TIMESTAMPTZ,
  approved_by           BIGINT,
  cancelled_at          TIMESTAMPTZ,
  cancelled_by          BIGINT,
  cancel_reason         TEXT,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT uq_po_org_number UNIQUE (organization_id, po_number),
  CONSTRAINT chk_po_status CHECK (status IN ('DRAFT','SUBMITTED','APPROVED','PARTIALLY_RECEIVED','RECEIVED','CLOSED','CANCELLED'))
);

CREATE TABLE purchase_order_line (
  id                    BIGSERIAL PRIMARY KEY,
  purchase_order_id     BIGINT NOT NULL REFERENCES purchase_order(id) ON DELETE CASCADE,
  product_id            BIGINT NOT NULL REFERENCES product(id) ON DELETE RESTRICT,
  uom_id                BIGINT NOT NULL REFERENCES uom(id) ON DELETE RESTRICT,
  quantity              NUMERIC(18,6) NOT NULL,
  base_quantity         NUMERIC(18,6) NOT NULL,
  unit_price            NUMERIC(18,2) NOT NULL,
  tax_rate              NUMERIC(9,4) NOT NULL DEFAULT 0,
  line_amount           NUMERIC(18,2) NOT NULL,
  received_base_quantity NUMERIC(18,6) NOT NULL DEFAULT 0,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT chk_po_line_qty CHECK (quantity > 0 AND base_quantity > 0),
  CONSTRAINT chk_po_line_price CHECK (unit_price >= 0)
);

CREATE TABLE purchase_receipt (
  id                    BIGSERIAL PRIMARY KEY,
  organization_id       BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  branch_id             BIGINT NOT NULL REFERENCES branch(id) ON DELETE RESTRICT,
  warehouse_id          BIGINT NOT NULL REFERENCES warehouse(id) ON DELETE RESTRICT,
  purchase_order_id     BIGINT REFERENCES purchase_order(id) ON DELETE SET NULL,
  supplier_id           BIGINT NOT NULL REFERENCES supplier(id) ON DELETE RESTRICT,
  receipt_number        VARCHAR(80) NOT NULL,
  receipt_date          DATE NOT NULL,
  status                VARCHAR(20) NOT NULL,
  subtotal              NUMERIC(18,2) NOT NULL DEFAULT 0,
  tax_amount            NUMERIC(18,2) NOT NULL DEFAULT 0,
  total_amount          NUMERIC(18,2) NOT NULL DEFAULT 0,
  remarks               TEXT,
  posted_at             TIMESTAMPTZ,
  cancelled_at          TIMESTAMPTZ,
  cancelled_by          BIGINT,
  cancel_reason         TEXT,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT uq_purchase_receipt_org_number UNIQUE (organization_id, receipt_number),
  CONSTRAINT chk_purchase_receipt_status CHECK (status IN ('DRAFT','POSTED','PARTIALLY_BILLED','BILLED','CANCELLED'))
);

CREATE TABLE purchase_receipt_line (
  id                    BIGSERIAL PRIMARY KEY,
  purchase_receipt_id   BIGINT NOT NULL REFERENCES purchase_receipt(id) ON DELETE CASCADE,
  purchase_order_line_id BIGINT REFERENCES purchase_order_line(id) ON DELETE SET NULL,
  product_id            BIGINT NOT NULL REFERENCES product(id) ON DELETE RESTRICT,
  uom_id                BIGINT NOT NULL REFERENCES uom(id) ON DELETE RESTRICT,
  quantity              NUMERIC(18,6) NOT NULL,
  base_quantity         NUMERIC(18,6) NOT NULL,
  unit_cost             NUMERIC(18,2) NOT NULL,
  tax_rate              NUMERIC(9,4) NOT NULL DEFAULT 0,
  line_amount           NUMERIC(18,2) NOT NULL,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT chk_purchase_receipt_line_qty CHECK (quantity > 0 AND base_quantity > 0)
);

CREATE TABLE purchase_receipt_line_serial (
  id                    BIGSERIAL PRIMARY KEY,
  purchase_receipt_line_id BIGINT NOT NULL REFERENCES purchase_receipt_line(id) ON DELETE CASCADE,
  serial_number_id      BIGINT NOT NULL REFERENCES serial_number(id) ON DELETE RESTRICT,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT uq_purchase_receipt_line_serial UNIQUE (purchase_receipt_line_id, serial_number_id)
);

CREATE TABLE purchase_receipt_line_batch (
  id                    BIGSERIAL PRIMARY KEY,
  purchase_receipt_line_id BIGINT NOT NULL REFERENCES purchase_receipt_line(id) ON DELETE CASCADE,
  batch_id              BIGINT NOT NULL REFERENCES inventory_batch(id) ON DELETE RESTRICT,
  quantity              NUMERIC(18,6) NOT NULL,
  base_quantity         NUMERIC(18,6) NOT NULL,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT
);

CREATE TABLE sales_invoice (
  id                    BIGSERIAL PRIMARY KEY,
  organization_id       BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  branch_id             BIGINT NOT NULL REFERENCES branch(id) ON DELETE RESTRICT,
  warehouse_id          BIGINT NOT NULL REFERENCES warehouse(id) ON DELETE RESTRICT,
  customer_id           BIGINT NOT NULL REFERENCES customer(id) ON DELETE RESTRICT,
  price_list_id         BIGINT REFERENCES price_list(id) ON DELETE SET NULL,
  invoice_number        VARCHAR(80) NOT NULL,
  invoice_date          DATE NOT NULL,
  status                VARCHAR(20) NOT NULL,
  subtotal              NUMERIC(18,2) NOT NULL DEFAULT 0,
  discount_amount       NUMERIC(18,2) NOT NULL DEFAULT 0,
  tax_amount            NUMERIC(18,2) NOT NULL DEFAULT 0,
  total_amount          NUMERIC(18,2) NOT NULL DEFAULT 0,
  remarks               TEXT,
  printed_at            TIMESTAMPTZ,
  emailed_at            TIMESTAMPTZ,
  posted_at             TIMESTAMPTZ,
  cancelled_at          TIMESTAMPTZ,
  cancelled_by          BIGINT,
  cancel_reason         TEXT,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT uq_sales_invoice_org_number UNIQUE (organization_id, invoice_number),
  CONSTRAINT chk_sales_invoice_status CHECK (status IN ('DRAFT','CONFIRMED','POSTED','PARTIALLY_PAID','PAID','RETURNED','CANCELLED'))
);

CREATE TABLE sales_invoice_line (
  id                    BIGSERIAL PRIMARY KEY,
  sales_invoice_id      BIGINT NOT NULL REFERENCES sales_invoice(id) ON DELETE CASCADE,
  product_id            BIGINT NOT NULL REFERENCES product(id) ON DELETE RESTRICT,
  uom_id                BIGINT NOT NULL REFERENCES uom(id) ON DELETE RESTRICT,
  quantity              NUMERIC(18,6) NOT NULL,
  base_quantity         NUMERIC(18,6) NOT NULL,
  unit_price            NUMERIC(18,2) NOT NULL,
  discount_amount       NUMERIC(18,2) NOT NULL DEFAULT 0,
  tax_rate              NUMERIC(9,4) NOT NULL DEFAULT 0,
  line_amount           NUMERIC(18,2) NOT NULL,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT chk_sales_invoice_line_qty CHECK (quantity > 0 AND base_quantity > 0)
);

CREATE TABLE sales_line_serial (
  id                    BIGSERIAL PRIMARY KEY,
  sales_invoice_line_id BIGINT NOT NULL REFERENCES sales_invoice_line(id) ON DELETE CASCADE,
  serial_number_id      BIGINT NOT NULL REFERENCES serial_number(id) ON DELETE RESTRICT,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT uq_sales_line_serial UNIQUE (sales_invoice_line_id, serial_number_id)
);

CREATE TABLE sales_line_batch (
  id                    BIGSERIAL PRIMARY KEY,
  sales_invoice_line_id BIGINT NOT NULL REFERENCES sales_invoice_line(id) ON DELETE CASCADE,
  batch_id              BIGINT NOT NULL REFERENCES inventory_batch(id) ON DELETE RESTRICT,
  quantity              NUMERIC(18,6) NOT NULL,
  base_quantity         NUMERIC(18,6) NOT NULL,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT
);

CREATE TABLE customer_receipt (
  id                    BIGSERIAL PRIMARY KEY,
  organization_id       BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  branch_id             BIGINT NOT NULL REFERENCES branch(id) ON DELETE RESTRICT,
  customer_id           BIGINT NOT NULL REFERENCES customer(id) ON DELETE RESTRICT,
  receipt_number        VARCHAR(80) NOT NULL,
  receipt_date          DATE NOT NULL,
  payment_method        VARCHAR(20) NOT NULL,
  reference_number      VARCHAR(100),
  amount                NUMERIC(18,2) NOT NULL,
  status                VARCHAR(20) NOT NULL,
  remarks               TEXT,
  cancelled_at          TIMESTAMPTZ,
  cancelled_by          BIGINT,
  cancel_reason         TEXT,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT uq_customer_receipt_org_number UNIQUE (organization_id, receipt_number),
  CONSTRAINT chk_customer_receipt_status CHECK (status IN ('POSTED','ALLOCATED','CANCELLED')),
  CONSTRAINT chk_customer_receipt_amount CHECK (amount > 0),
  CONSTRAINT chk_customer_receipt_method CHECK (payment_method IN ('CASH','BANK','UPI','CARD','CHEQUE','OTHER'))
);

CREATE TABLE customer_receipt_allocation (
  id                    BIGSERIAL PRIMARY KEY,
  customer_receipt_id   BIGINT NOT NULL REFERENCES customer_receipt(id) ON DELETE CASCADE,
  sales_invoice_id      BIGINT NOT NULL REFERENCES sales_invoice(id) ON DELETE RESTRICT,
  allocated_amount      NUMERIC(18,2) NOT NULL,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT chk_customer_receipt_alloc_amount CHECK (allocated_amount > 0)
);

CREATE TABLE supplier_payment (
  id                    BIGSERIAL PRIMARY KEY,
  organization_id       BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  branch_id             BIGINT NOT NULL REFERENCES branch(id) ON DELETE RESTRICT,
  supplier_id           BIGINT NOT NULL REFERENCES supplier(id) ON DELETE RESTRICT,
  payment_number        VARCHAR(80) NOT NULL,
  payment_date          DATE NOT NULL,
  payment_method        VARCHAR(20) NOT NULL,
  reference_number      VARCHAR(100),
  amount                NUMERIC(18,2) NOT NULL,
  status                VARCHAR(20) NOT NULL,
  remarks               TEXT,
  cancelled_at          TIMESTAMPTZ,
  cancelled_by          BIGINT,
  cancel_reason         TEXT,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT uq_supplier_payment_org_number UNIQUE (organization_id, payment_number),
  CONSTRAINT chk_supplier_payment_status CHECK (status IN ('POSTED','ALLOCATED','CANCELLED')),
  CONSTRAINT chk_supplier_payment_amount CHECK (amount > 0),
  CONSTRAINT chk_supplier_payment_method CHECK (payment_method IN ('CASH','BANK','UPI','CARD','CHEQUE','OTHER'))
);

CREATE TABLE supplier_payment_allocation (
  id                    BIGSERIAL PRIMARY KEY,
  supplier_payment_id   BIGINT NOT NULL REFERENCES supplier_payment(id) ON DELETE CASCADE,
  purchase_receipt_id   BIGINT NOT NULL REFERENCES purchase_receipt(id) ON DELETE RESTRICT,
  allocated_amount      NUMERIC(18,2) NOT NULL,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT chk_supplier_payment_alloc_amount CHECK (allocated_amount > 0)
);

-- =========================================================
-- Finance lite
-- =========================================================
CREATE TABLE account (
  id                    BIGSERIAL PRIMARY KEY,
  organization_id       BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  code                  VARCHAR(50) NOT NULL,
  name                  VARCHAR(150) NOT NULL,
  account_type          VARCHAR(30) NOT NULL,
  parent_account_id     BIGINT REFERENCES account(id) ON DELETE SET NULL,
  is_system             BOOLEAN NOT NULL DEFAULT FALSE,
  is_active             BOOLEAN NOT NULL DEFAULT TRUE,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT uq_account_org_code UNIQUE (organization_id, code),
  CONSTRAINT chk_account_type CHECK (account_type IN ('ASSET','LIABILITY','EQUITY','INCOME','EXPENSE'))
);

CREATE TABLE voucher (
  id                    BIGSERIAL PRIMARY KEY,
  organization_id       BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  branch_id             BIGINT REFERENCES branch(id) ON DELETE RESTRICT,
  voucher_number        VARCHAR(80) NOT NULL,
  voucher_date          DATE NOT NULL,
  voucher_type          VARCHAR(30) NOT NULL,
  reference_type        VARCHAR(50),
  reference_id          BIGINT,
  remarks               TEXT,
  status                VARCHAR(20) NOT NULL DEFAULT 'POSTED',
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT uq_voucher_org_number UNIQUE (organization_id, voucher_number),
  CONSTRAINT chk_voucher_type CHECK (voucher_type IN ('SALES','PURCHASE','RECEIPT','PAYMENT','EXPENSE','ADJUSTMENT','JOURNAL','OPENING'))
);

CREATE TABLE ledger_entry (
  id                    BIGSERIAL PRIMARY KEY,
  organization_id       BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  branch_id             BIGINT REFERENCES branch(id) ON DELETE RESTRICT,
  voucher_id            BIGINT NOT NULL REFERENCES voucher(id) ON DELETE CASCADE,
  account_id            BIGINT NOT NULL REFERENCES account(id) ON DELETE RESTRICT,
  entry_date            DATE NOT NULL,
  debit_amount          NUMERIC(18,2) NOT NULL DEFAULT 0,
  credit_amount         NUMERIC(18,2) NOT NULL DEFAULT 0,
  narrative             TEXT,
  customer_id           BIGINT REFERENCES customer(id) ON DELETE SET NULL,
  supplier_id           BIGINT REFERENCES supplier(id) ON DELETE SET NULL,
  sales_invoice_id      BIGINT REFERENCES sales_invoice(id) ON DELETE SET NULL,
  purchase_receipt_id   BIGINT REFERENCES purchase_receipt(id) ON DELETE SET NULL,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT chk_ledger_debit_credit CHECK (
    (debit_amount > 0 AND credit_amount = 0) OR
    (credit_amount > 0 AND debit_amount = 0)
  )
);

-- =========================================================
-- Service / Warranty
-- =========================================================
CREATE TABLE product_ownership (
  id                    BIGSERIAL PRIMARY KEY,
  organization_id       BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  customer_id           BIGINT NOT NULL REFERENCES customer(id) ON DELETE RESTRICT,
  product_id            BIGINT NOT NULL REFERENCES product(id) ON DELETE RESTRICT,
  serial_number_id      BIGINT REFERENCES serial_number(id) ON DELETE SET NULL,
  sales_invoice_id      BIGINT NOT NULL REFERENCES sales_invoice(id) ON DELETE RESTRICT,
  sales_invoice_line_id BIGINT NOT NULL REFERENCES sales_invoice_line(id) ON DELETE RESTRICT,
  ownership_start_date  DATE NOT NULL,
  warranty_start_date   DATE,
  warranty_end_date     DATE,
  status                VARCHAR(20) NOT NULL,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT chk_product_ownership_status CHECK (status IN ('ACTIVE','EXPIRED','RETURNED','REPLACED','VOID'))
);

CREATE TABLE service_ticket (
  id                    BIGSERIAL PRIMARY KEY,
  organization_id       BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  branch_id             BIGINT NOT NULL REFERENCES branch(id) ON DELETE RESTRICT,
  customer_id           BIGINT NOT NULL REFERENCES customer(id) ON DELETE RESTRICT,
  sales_invoice_id      BIGINT REFERENCES sales_invoice(id) ON DELETE SET NULL,
  ticket_number         VARCHAR(80) NOT NULL,
  source_type           VARCHAR(30) NOT NULL,
  priority              VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
  status                VARCHAR(20) NOT NULL,
  complaint_summary     VARCHAR(250) NOT NULL,
  issue_description     TEXT,
  reported_on           DATE NOT NULL,
  assigned_to_user_id   BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT uq_service_ticket_org_number UNIQUE (organization_id, ticket_number),
  CONSTRAINT chk_service_source_type CHECK (source_type IN ('INVOICE','WALK_IN','AMC','REPLACEMENT','OTHER')),
  CONSTRAINT chk_service_priority CHECK (priority IN ('LOW','MEDIUM','HIGH','CRITICAL')),
  CONSTRAINT chk_service_status CHECK (status IN ('OPEN','ASSIGNED','IN_PROGRESS','WAITING_PARTS','RESOLVED','CLOSED','CANCELLED'))
);

CREATE TABLE service_ticket_item (
  id                    BIGSERIAL PRIMARY KEY,
  service_ticket_id     BIGINT NOT NULL REFERENCES service_ticket(id) ON DELETE CASCADE,
  product_id            BIGINT NOT NULL REFERENCES product(id) ON DELETE RESTRICT,
  serial_number_id      BIGINT REFERENCES serial_number(id) ON DELETE SET NULL,
  product_ownership_id  BIGINT REFERENCES product_ownership(id) ON DELETE SET NULL,
  symptom_notes         TEXT,
  diagnosis_notes       TEXT,
  resolution_status     VARCHAR(20),
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT chk_service_item_resolution_status CHECK (resolution_status IS NULL OR resolution_status IN ('PENDING','REPAIRABLE','REPLACED','REJECTED','CLOSED'))
);

CREATE TABLE service_visit (
  id                    BIGSERIAL PRIMARY KEY,
  organization_id       BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  branch_id             BIGINT NOT NULL REFERENCES branch(id) ON DELETE RESTRICT,
  service_ticket_id     BIGINT NOT NULL REFERENCES service_ticket(id) ON DELETE CASCADE,
  technician_user_id    BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  scheduled_at          TIMESTAMPTZ,
  started_at            TIMESTAMPTZ,
  completed_at          TIMESTAMPTZ,
  visit_status          VARCHAR(20) NOT NULL,
  visit_notes           TEXT,
  parts_used_json       JSONB,
  customer_feedback     TEXT,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT chk_service_visit_status CHECK (visit_status IN ('SCHEDULED','STARTED','COMPLETED','CANCELLED','FAILED'))
);

CREATE TABLE warranty_claim (
  id                    BIGSERIAL PRIMARY KEY,
  organization_id       BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  branch_id             BIGINT NOT NULL REFERENCES branch(id) ON DELETE RESTRICT,
  service_ticket_id     BIGINT REFERENCES service_ticket(id) ON DELETE SET NULL,
  customer_id           BIGINT NOT NULL REFERENCES customer(id) ON DELETE RESTRICT,
  product_id            BIGINT NOT NULL REFERENCES product(id) ON DELETE RESTRICT,
  serial_number_id      BIGINT REFERENCES serial_number(id) ON DELETE SET NULL,
  supplier_id           BIGINT REFERENCES supplier(id) ON DELETE SET NULL,
  distributor_id        BIGINT REFERENCES distributor(id) ON DELETE SET NULL,
  claim_number          VARCHAR(80) NOT NULL,
  claim_type            VARCHAR(30) NOT NULL,
  status                VARCHAR(20) NOT NULL,
  claim_date            DATE NOT NULL,
  approved_on           DATE,
  claim_notes           TEXT,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT uq_warranty_claim_org_number UNIQUE (organization_id, claim_number),
  CONSTRAINT chk_warranty_claim_type CHECK (claim_type IN ('REPAIR','REPLACEMENT','CREDIT_NOTE','REJECTED')),
  CONSTRAINT chk_warranty_claim_status CHECK (status IN ('OPEN','SUBMITTED','APPROVED','REJECTED','SETTLED','CLOSED'))
);

-- =========================================================
-- Expense / notifications / reporting
-- =========================================================
CREATE TABLE expense_category (
  id                    BIGSERIAL PRIMARY KEY,
  organization_id       BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  code                  VARCHAR(50) NOT NULL,
  name                  VARCHAR(100) NOT NULL,
  is_active             BOOLEAN NOT NULL DEFAULT TRUE,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT uq_expense_category_org_code UNIQUE (organization_id, code)
);

CREATE TABLE expense (
  id                    BIGSERIAL PRIMARY KEY,
  organization_id       BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  branch_id             BIGINT NOT NULL REFERENCES branch(id) ON DELETE RESTRICT,
  expense_category_id   BIGINT NOT NULL REFERENCES expense_category(id) ON DELETE RESTRICT,
  expense_number        VARCHAR(80) NOT NULL,
  expense_date          DATE NOT NULL,
  amount                NUMERIC(18,2) NOT NULL,
  status                VARCHAR(20) NOT NULL,
  receipt_url           TEXT,
  remarks               TEXT,
  submitted_at          TIMESTAMPTZ,
  submitted_by          BIGINT,
  approved_at           TIMESTAMPTZ,
  approved_by           BIGINT,
  paid_at               TIMESTAMPTZ,
  cancelled_at          TIMESTAMPTZ,
  cancelled_by          BIGINT,
  cancel_reason         TEXT,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT uq_expense_org_number UNIQUE (organization_id, expense_number),
  CONSTRAINT chk_expense_status CHECK (status IN ('DRAFT','SUBMITTED','APPROVED','REJECTED','PAID','CANCELLED')),
  CONSTRAINT chk_expense_amount CHECK (amount > 0)
);

CREATE TABLE recurring_expense (
  id                    BIGSERIAL PRIMARY KEY,
  organization_id       BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  branch_id             BIGINT NOT NULL REFERENCES branch(id) ON DELETE RESTRICT,
  expense_category_id   BIGINT NOT NULL REFERENCES expense_category(id) ON DELETE RESTRICT,
  recurring_number      VARCHAR(80) NOT NULL,
  frequency             VARCHAR(20) NOT NULL,
  amount                NUMERIC(18,2) NOT NULL,
  start_date            DATE NOT NULL,
  next_run_date         DATE NOT NULL,
  is_active             BOOLEAN NOT NULL DEFAULT TRUE,
  remarks               TEXT,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT uq_recurring_expense_org_number UNIQUE (organization_id, recurring_number),
  CONSTRAINT chk_recurring_frequency CHECK (frequency IN ('DAILY','WEEKLY','MONTHLY','QUARTERLY','YEARLY')),
  CONSTRAINT chk_recurring_amount CHECK (amount > 0)
);

CREATE TABLE notification_template (
  id                    BIGSERIAL PRIMARY KEY,
  organization_id       BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  template_code         VARCHAR(80) NOT NULL,
  channel               VARCHAR(20) NOT NULL,
  subject               VARCHAR(200),
  body                  TEXT NOT NULL,
  is_active             BOOLEAN NOT NULL DEFAULT TRUE,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT uq_notification_template_org_code UNIQUE (organization_id, template_code),
  CONSTRAINT chk_notification_template_channel CHECK (channel IN ('APP','SMS','EMAIL','WHATSAPP'))
);

CREATE TABLE notification (
  id                    BIGSERIAL PRIMARY KEY,
  organization_id       BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  user_id               BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  customer_id           BIGINT REFERENCES customer(id) ON DELETE SET NULL,
  supplier_id           BIGINT REFERENCES supplier(id) ON DELETE SET NULL,
  template_id           BIGINT REFERENCES notification_template(id) ON DELETE SET NULL,
  channel               VARCHAR(20) NOT NULL,
  status                VARCHAR(20) NOT NULL,
  reference_type        VARCHAR(50),
  reference_id          BIGINT,
  scheduled_at          TIMESTAMPTZ,
  sent_at               TIMESTAMPTZ,
  read_at               TIMESTAMPTZ,
  payload_json          JSONB,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT chk_notification_channel CHECK (channel IN ('APP','SMS','EMAIL','WHATSAPP')),
  CONSTRAINT chk_notification_status CHECK (status IN ('PENDING','SENT','FAILED','READ','CANCELLED'))
);

CREATE TABLE report_schedule (
  id                    BIGSERIAL PRIMARY KEY,
  organization_id       BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  user_id               BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  schedule_code         VARCHAR(80) NOT NULL,
  report_type           VARCHAR(50) NOT NULL,
  frequency             VARCHAR(20) NOT NULL,
  delivery_channel      VARCHAR(20) NOT NULL,
  is_active             BOOLEAN NOT NULL DEFAULT TRUE,
  next_run_at           TIMESTAMPTZ,
  config_json           JSONB,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT uq_report_schedule_org_code UNIQUE (organization_id, schedule_code),
  CONSTRAINT chk_report_schedule_frequency CHECK (frequency IN ('DAILY','WEEKLY','MONTHLY','QUARTERLY','YEARLY')),
  CONSTRAINT chk_report_schedule_channel CHECK (delivery_channel IN ('APP','EMAIL','WHATSAPP'))
);

-- =========================================================
-- Immutable business event audit
-- =========================================================
CREATE TABLE audit_event (
  id                    BIGSERIAL PRIMARY KEY,
  organization_id       BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  branch_id             BIGINT REFERENCES branch(id) ON DELETE SET NULL,
  event_type            VARCHAR(60) NOT NULL,
  entity_type           VARCHAR(60) NOT NULL,
  entity_id             BIGINT NOT NULL,
  entity_number         VARCHAR(100),
  action                VARCHAR(40) NOT NULL,
  actor_user_id         BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  actor_name_snapshot   VARCHAR(200),
  actor_role_snapshot   VARCHAR(100),
  warehouse_id          BIGINT REFERENCES warehouse(id) ON DELETE SET NULL,
  customer_id           BIGINT REFERENCES customer(id) ON DELETE SET NULL,
  supplier_id           BIGINT REFERENCES supplier(id) ON DELETE SET NULL,
  occurred_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  summary               VARCHAR(500),
  payload_json          JSONB NOT NULL,
  device_id             VARCHAR(120),
  app_version           VARCHAR(50),
  ip_address            VARCHAR(64),
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT
);

-- =========================================================
-- Indexes
-- =========================================================
CREATE INDEX idx_branch_org ON branch(organization_id);
CREATE INDEX idx_warehouse_org_branch ON warehouse(organization_id, branch_id);
CREATE INDEX idx_user_org_role ON app_user(organization_id, role_id);
CREATE INDEX idx_user_default_branch ON app_user(default_branch_id);
CREATE INDEX idx_user_branch_access_branch ON user_branch_access(branch_id);
CREATE INDEX idx_customer_org_branch ON customer(organization_id, branch_id, status);
CREATE INDEX idx_supplier_org_branch ON supplier(organization_id, branch_id, status);
CREATE INDEX idx_distributor_org_branch ON distributor(organization_id, branch_id, status);
CREATE INDEX idx_category_org_parent ON category(organization_id, parent_category_id);
CREATE INDEX idx_product_org_category_brand ON product(organization_id, category_id, brand_id);
CREATE INDEX idx_product_tracking_mode ON product(inventory_tracking_mode, serial_tracking_enabled, batch_tracking_enabled);
CREATE INDEX idx_price_list_item_product ON price_list_item(product_id, uom_id);
CREATE INDEX idx_batch_product_status ON inventory_batch(product_id, status, expiry_on);
CREATE INDEX idx_serial_product_status ON serial_number(product_id, status);
CREATE INDEX idx_serial_current_wh ON serial_number(current_warehouse_id);
CREATE INDEX idx_inventory_balance_lookup ON inventory_balance(organization_id, branch_id, warehouse_id, product_id, batch_id);
CREATE INDEX idx_stock_movement_ref ON stock_movement(reference_type, reference_id);
CREATE INDEX idx_stock_movement_lookup ON stock_movement(organization_id, warehouse_id, product_id, movement_at DESC);
CREATE INDEX idx_stock_movement_branch ON stock_movement(branch_id, movement_type, movement_at DESC);
CREATE INDEX idx_stock_movement_serial_serial ON stock_movement_serial(serial_number_id);
CREATE INDEX idx_stock_movement_batch_batch ON stock_movement_batch(batch_id);
CREATE INDEX idx_inventory_reservation_lookup ON inventory_reservation(warehouse_id, product_id, status);
CREATE INDEX idx_po_supplier_status ON purchase_order(supplier_id, status, po_date DESC);
CREATE INDEX idx_po_branch_status ON purchase_order(branch_id, status, po_date DESC);
CREATE INDEX idx_purchase_receipt_supplier_status ON purchase_receipt(supplier_id, status, receipt_date DESC);
CREATE INDEX idx_purchase_receipt_wh_date ON purchase_receipt(warehouse_id, receipt_date DESC);
CREATE INDEX idx_purchase_receipt_line_product ON purchase_receipt_line(product_id);
CREATE INDEX idx_sales_invoice_customer_status ON sales_invoice(customer_id, status, invoice_date DESC);
CREATE INDEX idx_sales_invoice_branch_status ON sales_invoice(branch_id, status, invoice_date DESC);
CREATE INDEX idx_sales_invoice_wh_date ON sales_invoice(warehouse_id, invoice_date DESC);
CREATE INDEX idx_sales_invoice_line_product ON sales_invoice_line(product_id);
CREATE INDEX idx_customer_receipt_customer_date ON customer_receipt(customer_id, receipt_date DESC);
CREATE INDEX idx_customer_receipt_alloc_invoice ON customer_receipt_allocation(sales_invoice_id);
CREATE INDEX idx_supplier_payment_supplier_date ON supplier_payment(supplier_id, payment_date DESC);
CREATE INDEX idx_supplier_payment_alloc_receipt ON supplier_payment_allocation(purchase_receipt_id);
CREATE INDEX idx_account_org_type ON account(organization_id, account_type, is_active);
CREATE INDEX idx_voucher_org_date ON voucher(organization_id, voucher_date DESC);
CREATE INDEX idx_ledger_entry_voucher ON ledger_entry(voucher_id);
CREATE INDEX idx_ledger_entry_account_date ON ledger_entry(account_id, entry_date DESC);
CREATE INDEX idx_ledger_entry_customer ON ledger_entry(customer_id, entry_date DESC);
CREATE INDEX idx_ledger_entry_supplier ON ledger_entry(supplier_id, entry_date DESC);
CREATE INDEX idx_product_ownership_customer ON product_ownership(customer_id, status);
CREATE INDEX idx_product_ownership_serial ON product_ownership(serial_number_id);
CREATE INDEX idx_service_ticket_customer_status ON service_ticket(customer_id, status, reported_on DESC);
CREATE INDEX idx_service_ticket_assignee_status ON service_ticket(assigned_to_user_id, status);
CREATE INDEX idx_service_ticket_item_serial ON service_ticket_item(serial_number_id);
CREATE INDEX idx_service_visit_ticket ON service_visit(service_ticket_id, scheduled_at DESC);
CREATE INDEX idx_warranty_claim_status_date ON warranty_claim(status, claim_date DESC);
CREATE INDEX idx_expense_branch_status_date ON expense(branch_id, status, expense_date DESC);
CREATE INDEX idx_notification_status_scheduled ON notification(status, scheduled_at);
CREATE INDEX idx_report_schedule_next_run ON report_schedule(is_active, next_run_at);
CREATE INDEX idx_approval_request_entity ON approval_request(entity_type, entity_id);
CREATE INDEX idx_approval_request_status ON approval_request(status, requested_at DESC);
CREATE INDEX idx_audit_event_entity ON audit_event(entity_type, entity_id, occurred_at DESC);
CREATE INDEX idx_audit_event_actor ON audit_event(actor_user_id, occurred_at DESC);
CREATE INDEX idx_audit_event_customer ON audit_event(customer_id, occurred_at DESC);
CREATE INDEX idx_audit_event_payload_gin ON audit_event USING GIN (payload_json);
CREATE INDEX idx_notification_payload_gin ON notification USING GIN (payload_json);
CREATE INDEX idx_report_schedule_config_gin ON report_schedule USING GIN (config_json);

-- =========================================================
-- Updated_at triggers
-- =========================================================
DO $$
DECLARE
  t TEXT;
BEGIN
  FOR t IN
    SELECT unnest(ARRAY[
      'organization','branch','warehouse','role','permission','role_permission','app_user','user_branch_access','app_setting','document_sequence',
      'approval_rule','approval_request','approval_history','customer','customer_address','supplier','supplier_address','distributor','distributor_address',
      'category','brand','uom_group','uom','tax_group','price_list','product','product_uom_conversion','price_list_item',
      'inventory_batch','serial_number','inventory_balance','stock_movement','stock_movement_serial','stock_movement_batch','inventory_reservation','stock_transfer','stock_transfer_line','stock_adjustment','stock_adjustment_line',
      'purchase_order','purchase_order_line','purchase_receipt','purchase_receipt_line','purchase_receipt_line_serial','purchase_receipt_line_batch',
      'sales_invoice','sales_invoice_line','sales_line_serial','sales_line_batch','customer_receipt','customer_receipt_allocation','supplier_payment','supplier_payment_allocation',
      'account','voucher','ledger_entry','product_ownership','service_ticket','service_ticket_item','service_visit','warranty_claim',
      'expense_category','expense','recurring_expense','notification_template','notification','report_schedule','audit_event'
    ])
  LOOP
    EXECUTE format('CREATE TRIGGER trg_%I_set_updated_at BEFORE UPDATE ON %I FOR EACH ROW EXECUTE FUNCTION set_updated_at();', t, t);
  END LOOP;
END$$;

-- =========================================================
-- Add audit user FKs at the end
-- =========================================================
ALTER TABLE organization        ADD CONSTRAINT fk_organization_created_by         FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE organization        ADD CONSTRAINT fk_organization_updated_by         FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE branch              ADD CONSTRAINT fk_branch_created_by               FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE branch              ADD CONSTRAINT fk_branch_updated_by               FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE warehouse           ADD CONSTRAINT fk_warehouse_created_by            FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE warehouse           ADD CONSTRAINT fk_warehouse_updated_by            FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE role                ADD CONSTRAINT fk_role_created_by                 FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE role                ADD CONSTRAINT fk_role_updated_by                 FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE permission          ADD CONSTRAINT fk_permission_created_by           FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE permission          ADD CONSTRAINT fk_permission_updated_by           FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE role_permission     ADD CONSTRAINT fk_role_permission_created_by      FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE role_permission     ADD CONSTRAINT fk_role_permission_updated_by      FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE app_user            ADD CONSTRAINT fk_app_user_created_by             FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE app_user            ADD CONSTRAINT fk_app_user_updated_by             FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE user_branch_access  ADD CONSTRAINT fk_user_branch_access_created_by   FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE user_branch_access  ADD CONSTRAINT fk_user_branch_access_updated_by   FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE app_setting         ADD CONSTRAINT fk_app_setting_created_by          FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE app_setting         ADD CONSTRAINT fk_app_setting_updated_by          FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE document_sequence   ADD CONSTRAINT fk_document_sequence_created_by    FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE document_sequence   ADD CONSTRAINT fk_document_sequence_updated_by    FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE approval_rule       ADD CONSTRAINT fk_approval_rule_created_by        FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE approval_rule       ADD CONSTRAINT fk_approval_rule_updated_by        FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE approval_request    ADD CONSTRAINT fk_approval_request_created_by     FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE approval_request    ADD CONSTRAINT fk_approval_request_updated_by     FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE approval_history    ADD CONSTRAINT fk_approval_history_created_by     FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE approval_history    ADD CONSTRAINT fk_approval_history_updated_by     FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE customer            ADD CONSTRAINT fk_customer_created_by             FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE customer            ADD CONSTRAINT fk_customer_updated_by             FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE customer_address    ADD CONSTRAINT fk_customer_address_created_by     FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE customer_address    ADD CONSTRAINT fk_customer_address_updated_by     FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE supplier            ADD CONSTRAINT fk_supplier_created_by             FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE supplier            ADD CONSTRAINT fk_supplier_updated_by             FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE supplier_address    ADD CONSTRAINT fk_supplier_address_created_by     FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE supplier_address    ADD CONSTRAINT fk_supplier_address_updated_by     FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE distributor         ADD CONSTRAINT fk_distributor_created_by          FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE distributor         ADD CONSTRAINT fk_distributor_updated_by          FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE distributor_address ADD CONSTRAINT fk_distributor_address_created_by  FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE distributor_address ADD CONSTRAINT fk_distributor_address_updated_by  FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE category            ADD CONSTRAINT fk_category_created_by             FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE category            ADD CONSTRAINT fk_category_updated_by             FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE brand               ADD CONSTRAINT fk_brand_created_by                FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE brand               ADD CONSTRAINT fk_brand_updated_by                FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE uom_group           ADD CONSTRAINT fk_uom_group_created_by           FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE uom_group           ADD CONSTRAINT fk_uom_group_updated_by           FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE uom                 ADD CONSTRAINT fk_uom_created_by                 FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE uom                 ADD CONSTRAINT fk_uom_updated_by                 FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE tax_group           ADD CONSTRAINT fk_tax_group_created_by           FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE tax_group           ADD CONSTRAINT fk_tax_group_updated_by           FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE price_list          ADD CONSTRAINT fk_price_list_created_by          FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE price_list          ADD CONSTRAINT fk_price_list_updated_by          FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE product             ADD CONSTRAINT fk_product_created_by             FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE product             ADD CONSTRAINT fk_product_updated_by             FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE product_uom_conversion ADD CONSTRAINT fk_product_uom_conv_created_by FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE product_uom_conversion ADD CONSTRAINT fk_product_uom_conv_updated_by FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE price_list_item     ADD CONSTRAINT fk_price_list_item_created_by     FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE price_list_item     ADD CONSTRAINT fk_price_list_item_updated_by     FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE inventory_batch     ADD CONSTRAINT fk_inventory_batch_created_by     FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE inventory_batch     ADD CONSTRAINT fk_inventory_batch_updated_by     FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE serial_number       ADD CONSTRAINT fk_serial_number_created_by       FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE serial_number       ADD CONSTRAINT fk_serial_number_updated_by       FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE inventory_balance   ADD CONSTRAINT fk_inventory_balance_created_by   FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE inventory_balance   ADD CONSTRAINT fk_inventory_balance_updated_by   FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE stock_movement      ADD CONSTRAINT fk_stock_movement_created_by      FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE stock_movement      ADD CONSTRAINT fk_stock_movement_updated_by      FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE stock_movement_serial ADD CONSTRAINT fk_stock_movement_serial_created_by FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE stock_movement_serial ADD CONSTRAINT fk_stock_movement_serial_updated_by FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE stock_movement_batch ADD CONSTRAINT fk_stock_movement_batch_created_by FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE stock_movement_batch ADD CONSTRAINT fk_stock_movement_batch_updated_by FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE inventory_reservation ADD CONSTRAINT fk_inventory_reservation_created_by FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE inventory_reservation ADD CONSTRAINT fk_inventory_reservation_updated_by FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE stock_transfer      ADD CONSTRAINT fk_stock_transfer_created_by      FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE stock_transfer      ADD CONSTRAINT fk_stock_transfer_updated_by      FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE stock_transfer_line ADD CONSTRAINT fk_stock_transfer_line_created_by FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE stock_transfer_line ADD CONSTRAINT fk_stock_transfer_line_updated_by FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE stock_adjustment    ADD CONSTRAINT fk_stock_adjustment_created_by    FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE stock_adjustment    ADD CONSTRAINT fk_stock_adjustment_updated_by    FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE stock_adjustment_line ADD CONSTRAINT fk_stock_adjustment_line_created_by FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE stock_adjustment_line ADD CONSTRAINT fk_stock_adjustment_line_updated_by FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE purchase_order      ADD CONSTRAINT fk_purchase_order_created_by      FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE purchase_order      ADD CONSTRAINT fk_purchase_order_updated_by      FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE purchase_order_line ADD CONSTRAINT fk_purchase_order_line_created_by FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE purchase_order_line ADD CONSTRAINT fk_purchase_order_line_updated_by FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE purchase_receipt    ADD CONSTRAINT fk_purchase_receipt_created_by    FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE purchase_receipt    ADD CONSTRAINT fk_purchase_receipt_updated_by    FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE purchase_receipt_line ADD CONSTRAINT fk_purchase_receipt_line_created_by FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE purchase_receipt_line ADD CONSTRAINT fk_purchase_receipt_line_updated_by FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE purchase_receipt_line_serial ADD CONSTRAINT fk_purchase_receipt_line_serial_created_by FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE purchase_receipt_line_serial ADD CONSTRAINT fk_purchase_receipt_line_serial_updated_by FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE purchase_receipt_line_batch ADD CONSTRAINT fk_purchase_receipt_line_batch_created_by FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE purchase_receipt_line_batch ADD CONSTRAINT fk_purchase_receipt_line_batch_updated_by FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE sales_invoice       ADD CONSTRAINT fk_sales_invoice_created_by       FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE sales_invoice       ADD CONSTRAINT fk_sales_invoice_updated_by       FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE sales_invoice_line  ADD CONSTRAINT fk_sales_invoice_line_created_by  FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE sales_invoice_line  ADD CONSTRAINT fk_sales_invoice_line_updated_by  FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE sales_line_serial   ADD CONSTRAINT fk_sales_line_serial_created_by   FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE sales_line_serial   ADD CONSTRAINT fk_sales_line_serial_updated_by   FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE sales_line_batch    ADD CONSTRAINT fk_sales_line_batch_created_by    FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE sales_line_batch    ADD CONSTRAINT fk_sales_line_batch_updated_by    FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE customer_receipt    ADD CONSTRAINT fk_customer_receipt_created_by    FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE customer_receipt    ADD CONSTRAINT fk_customer_receipt_updated_by    FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE customer_receipt_allocation ADD CONSTRAINT fk_customer_receipt_alloc_created_by FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE customer_receipt_allocation ADD CONSTRAINT fk_customer_receipt_alloc_updated_by FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE supplier_payment    ADD CONSTRAINT fk_supplier_payment_created_by    FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE supplier_payment    ADD CONSTRAINT fk_supplier_payment_updated_by    FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE supplier_payment_allocation ADD CONSTRAINT fk_supplier_payment_alloc_created_by FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE supplier_payment_allocation ADD CONSTRAINT fk_supplier_payment_alloc_updated_by FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE account             ADD CONSTRAINT fk_account_created_by             FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE account             ADD CONSTRAINT fk_account_updated_by             FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE voucher             ADD CONSTRAINT fk_voucher_created_by             FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE voucher             ADD CONSTRAINT fk_voucher_updated_by             FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE ledger_entry        ADD CONSTRAINT fk_ledger_entry_created_by        FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE ledger_entry        ADD CONSTRAINT fk_ledger_entry_updated_by        FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE product_ownership   ADD CONSTRAINT fk_product_ownership_created_by   FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE product_ownership   ADD CONSTRAINT fk_product_ownership_updated_by   FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE service_ticket      ADD CONSTRAINT fk_service_ticket_created_by      FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE service_ticket      ADD CONSTRAINT fk_service_ticket_updated_by      FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE service_ticket_item ADD CONSTRAINT fk_service_ticket_item_created_by FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE service_ticket_item ADD CONSTRAINT fk_service_ticket_item_updated_by FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE service_visit       ADD CONSTRAINT fk_service_visit_created_by       FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE service_visit       ADD CONSTRAINT fk_service_visit_updated_by       FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE warranty_claim      ADD CONSTRAINT fk_warranty_claim_created_by      FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE warranty_claim      ADD CONSTRAINT fk_warranty_claim_updated_by      FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE expense_category    ADD CONSTRAINT fk_expense_category_created_by    FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE expense_category    ADD CONSTRAINT fk_expense_category_updated_by    FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE expense             ADD CONSTRAINT fk_expense_created_by             FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE expense             ADD CONSTRAINT fk_expense_updated_by             FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE recurring_expense   ADD CONSTRAINT fk_recurring_expense_created_by   FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE recurring_expense   ADD CONSTRAINT fk_recurring_expense_updated_by   FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE notification_template ADD CONSTRAINT fk_notification_template_created_by FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE notification_template ADD CONSTRAINT fk_notification_template_updated_by FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE notification        ADD CONSTRAINT fk_notification_created_by        FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE notification        ADD CONSTRAINT fk_notification_updated_by        FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE report_schedule     ADD CONSTRAINT fk_report_schedule_created_by     FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE report_schedule     ADD CONSTRAINT fk_report_schedule_updated_by     FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE audit_event         ADD CONSTRAINT fk_audit_event_created_by         FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE audit_event         ADD CONSTRAINT fk_audit_event_updated_by         FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;

-- Action actor columns
ALTER TABLE purchase_order      ADD CONSTRAINT fk_purchase_order_submitted_by    FOREIGN KEY (submitted_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE purchase_order      ADD CONSTRAINT fk_purchase_order_approved_by     FOREIGN KEY (approved_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE purchase_order      ADD CONSTRAINT fk_purchase_order_cancelled_by    FOREIGN KEY (cancelled_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE purchase_receipt    ADD CONSTRAINT fk_purchase_receipt_cancelled_by  FOREIGN KEY (cancelled_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE sales_invoice       ADD CONSTRAINT fk_sales_invoice_cancelled_by      FOREIGN KEY (cancelled_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE customer_receipt    ADD CONSTRAINT fk_customer_receipt_cancelled_by  FOREIGN KEY (cancelled_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE supplier_payment    ADD CONSTRAINT fk_supplier_payment_cancelled_by  FOREIGN KEY (cancelled_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE expense             ADD CONSTRAINT fk_expense_submitted_by           FOREIGN KEY (submitted_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE expense             ADD CONSTRAINT fk_expense_approved_by            FOREIGN KEY (approved_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE expense             ADD CONSTRAINT fk_expense_cancelled_by           FOREIGN KEY (cancelled_by) REFERENCES app_user(id) ON DELETE SET NULL;

COMMIT;


-- ===== SOURCE: 005_add_central_product_catalog.sql =====

-- =========================================================
-- Shared product catalog with organization/store association
-- Existing `product` rows remain the org/store-facing records used by
-- purchase, sales, invoice, and inventory tables. This migration adds
-- a central catalog record behind each org product and backfills links.
-- =========================================================

CREATE TABLE catalog_product (
  id                              BIGSERIAL PRIMARY KEY,
  sku                             VARCHAR(80),
  name                            VARCHAR(200) NOT NULL,
  description                     TEXT,
  category_name                   VARCHAR(150),
  brand_name                      VARCHAR(150),
  base_uom_id                     BIGINT NOT NULL REFERENCES uom(id) ON DELETE RESTRICT,
  inventory_tracking_mode         VARCHAR(30) NOT NULL,
  serial_tracking_enabled         BOOLEAN NOT NULL DEFAULT FALSE,
  batch_tracking_enabled          BOOLEAN NOT NULL DEFAULT FALSE,
  expiry_tracking_enabled         BOOLEAN NOT NULL DEFAULT FALSE,
  fractional_quantity_allowed     BOOLEAN NOT NULL DEFAULT FALSE,
  is_service_item                 BOOLEAN NOT NULL DEFAULT FALSE,
  is_active                       BOOLEAN NOT NULL DEFAULT TRUE,
  created_at                      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by                      BIGINT,
  updated_at                      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by                      BIGINT
);

ALTER TABLE catalog_product ADD CONSTRAINT fk_catalog_product_created_by FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE catalog_product ADD CONSTRAINT fk_catalog_product_updated_by FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;

ALTER TABLE product ADD COLUMN catalog_product_id BIGINT;

INSERT INTO catalog_product (
  sku,
  name,
  description,
  category_name,
  brand_name,
  base_uom_id,
  inventory_tracking_mode,
  serial_tracking_enabled,
  batch_tracking_enabled,
  expiry_tracking_enabled,
  fractional_quantity_allowed,
  is_service_item,
  is_active,
  created_at,
  created_by,
  updated_at,
  updated_by
)
SELECT DISTINCT ON (
  lower(trim(p.name)),
  coalesce(lower(trim(b.name)), ''),
  p.base_uom_id,
  p.inventory_tracking_mode,
  p.serial_tracking_enabled,
  p.batch_tracking_enabled,
  p.expiry_tracking_enabled,
  p.fractional_quantity_allowed,
  p.is_service_item
)
  p.sku,
  p.name,
  p.description,
  c.name,
  b.name,
  p.base_uom_id,
  p.inventory_tracking_mode,
  p.serial_tracking_enabled,
  p.batch_tracking_enabled,
  p.expiry_tracking_enabled,
  p.fractional_quantity_allowed,
  p.is_service_item,
  p.is_active,
  p.created_at,
  p.created_by,
  p.updated_at,
  p.updated_by
FROM product p
LEFT JOIN category c ON c.id = p.category_id
LEFT JOIN brand b ON b.id = p.brand_id
ORDER BY
  lower(trim(p.name)),
  coalesce(lower(trim(b.name)), ''),
  p.base_uom_id,
  p.inventory_tracking_mode,
  p.serial_tracking_enabled,
  p.batch_tracking_enabled,
  p.expiry_tracking_enabled,
  p.fractional_quantity_allowed,
  p.is_service_item,
  p.id;

UPDATE product p
SET catalog_product_id = cp.id
FROM catalog_product cp
WHERE cp.name = p.name
  AND cp.base_uom_id = p.base_uom_id
  AND cp.inventory_tracking_mode = p.inventory_tracking_mode
  AND cp.serial_tracking_enabled = p.serial_tracking_enabled
  AND cp.batch_tracking_enabled = p.batch_tracking_enabled
  AND cp.expiry_tracking_enabled = p.expiry_tracking_enabled
  AND cp.fractional_quantity_allowed = p.fractional_quantity_allowed
  AND cp.is_service_item = p.is_service_item
  AND coalesce(cp.brand_name, '') = coalesce((SELECT b.name FROM brand b WHERE b.id = p.brand_id), '')
  AND coalesce(cp.category_name, '') = coalesce((SELECT c.name FROM category c WHERE c.id = p.category_id), '');

ALTER TABLE product
  ALTER COLUMN catalog_product_id SET NOT NULL;

ALTER TABLE product
  ADD CONSTRAINT fk_product_catalog_product
  FOREIGN KEY (catalog_product_id) REFERENCES catalog_product(id) ON DELETE RESTRICT;

CREATE UNIQUE INDEX uq_product_org_catalog_product ON product(organization_id, catalog_product_id);
CREATE INDEX idx_catalog_product_name_brand ON catalog_product(name, brand_name);
CREATE INDEX idx_catalog_product_sku ON catalog_product(sku);


-- ===== SOURCE: 006_add_subscription_schema.sql =====

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


-- ===== SOURCE: 007_add_identity_layer.sql =====

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


-- ===== SOURCE: 008_drop_legacy_user_identity_columns.sql =====

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


-- ===== SOURCE: 010_add_tax_registration_and_gst_breakup.sql =====

CREATE TABLE tax_registration (
  id                    BIGSERIAL PRIMARY KEY,
  organization_id       BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  branch_id             BIGINT REFERENCES branch(id) ON DELETE CASCADE,
  registration_type     VARCHAR(20) NOT NULL DEFAULT 'GST',
  registration_name     VARCHAR(200) NOT NULL,
  legal_name            VARCHAR(250),
  gstin                 VARCHAR(30) NOT NULL,
  registration_state_code VARCHAR(10) NOT NULL,
  registration_state_name VARCHAR(100),
  effective_from        DATE NOT NULL,
  effective_to          DATE,
  is_default            BOOLEAN NOT NULL DEFAULT FALSE,
  is_active             BOOLEAN NOT NULL DEFAULT TRUE,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT chk_tax_registration_type CHECK (registration_type IN ('GST')),
  CONSTRAINT uq_tax_registration_gstin UNIQUE (gstin)
);

CREATE INDEX idx_tax_registration_org_branch_effective
  ON tax_registration(organization_id, branch_id, effective_from DESC, is_active);

ALTER TABLE tax_registration ADD CONSTRAINT fk_tax_registration_created_by
  FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE tax_registration ADD CONSTRAINT fk_tax_registration_updated_by
  FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;

ALTER TABLE purchase_order
  ADD COLUMN seller_tax_registration_id BIGINT REFERENCES tax_registration(id) ON DELETE SET NULL,
  ADD COLUMN seller_gstin VARCHAR(30),
  ADD COLUMN supplier_gstin VARCHAR(30),
  ADD COLUMN place_of_supply_state_code VARCHAR(10);

ALTER TABLE purchase_receipt
  ADD COLUMN seller_tax_registration_id BIGINT REFERENCES tax_registration(id) ON DELETE SET NULL,
  ADD COLUMN seller_gstin VARCHAR(30),
  ADD COLUMN supplier_gstin VARCHAR(30),
  ADD COLUMN place_of_supply_state_code VARCHAR(10);

ALTER TABLE sales_invoice
  ADD COLUMN seller_tax_registration_id BIGINT REFERENCES tax_registration(id) ON DELETE SET NULL,
  ADD COLUMN seller_gstin VARCHAR(30),
  ADD COLUMN customer_gstin VARCHAR(30),
  ADD COLUMN place_of_supply_state_code VARCHAR(10);

ALTER TABLE purchase_order_line
  ADD COLUMN taxable_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  ADD COLUMN cgst_rate NUMERIC(9,4) NOT NULL DEFAULT 0,
  ADD COLUMN cgst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  ADD COLUMN sgst_rate NUMERIC(9,4) NOT NULL DEFAULT 0,
  ADD COLUMN sgst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  ADD COLUMN igst_rate NUMERIC(9,4) NOT NULL DEFAULT 0,
  ADD COLUMN igst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  ADD COLUMN cess_rate NUMERIC(9,4) NOT NULL DEFAULT 0,
  ADD COLUMN cess_amount NUMERIC(18,2) NOT NULL DEFAULT 0;

ALTER TABLE purchase_receipt_line
  ADD COLUMN taxable_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  ADD COLUMN cgst_rate NUMERIC(9,4) NOT NULL DEFAULT 0,
  ADD COLUMN cgst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  ADD COLUMN sgst_rate NUMERIC(9,4) NOT NULL DEFAULT 0,
  ADD COLUMN sgst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  ADD COLUMN igst_rate NUMERIC(9,4) NOT NULL DEFAULT 0,
  ADD COLUMN igst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  ADD COLUMN cess_rate NUMERIC(9,4) NOT NULL DEFAULT 0,
  ADD COLUMN cess_amount NUMERIC(18,2) NOT NULL DEFAULT 0;

ALTER TABLE sales_invoice_line
  ADD COLUMN taxable_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  ADD COLUMN cgst_rate NUMERIC(9,4) NOT NULL DEFAULT 0,
  ADD COLUMN cgst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  ADD COLUMN sgst_rate NUMERIC(9,4) NOT NULL DEFAULT 0,
  ADD COLUMN sgst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  ADD COLUMN igst_rate NUMERIC(9,4) NOT NULL DEFAULT 0,
  ADD COLUMN igst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  ADD COLUMN cess_rate NUMERIC(9,4) NOT NULL DEFAULT 0,
  ADD COLUMN cess_amount NUMERIC(18,2) NOT NULL DEFAULT 0;

INSERT INTO tax_registration (
  organization_id,
  registration_type,
  registration_name,
  legal_name,
  gstin,
  registration_state_code,
  registration_state_name,
  effective_from,
  is_default,
  is_active
)
SELECT
  o.id,
  'GST',
  COALESCE(o.name, o.code),
  o.legal_name,
  o.gstin,
  SUBSTRING(o.gstin FROM 1 FOR 2),
  NULL,
  CURRENT_DATE,
  TRUE,
  TRUE
FROM organization o
WHERE o.gstin IS NOT NULL
  AND btrim(o.gstin) <> ''
  AND NOT EXISTS (
    SELECT 1
    FROM tax_registration tr
    WHERE tr.organization_id = o.id
      AND tr.branch_id IS NULL
      AND tr.gstin = o.gstin
  );


-- ===== SOURCE: 011_add_gst_threshold_settings.sql =====

ALTER TABLE organization
  ADD COLUMN gst_threshold_amount NUMERIC(18,2) NOT NULL DEFAULT 4000000,
  ADD COLUMN gst_threshold_alert_enabled BOOLEAN NOT NULL DEFAULT TRUE;


-- ===== SOURCE: 013_refactor_product_to_store_product.sql =====

ALTER TABLE product RENAME TO store_product;
ALTER TABLE catalog_product RENAME TO product;

ALTER TABLE store_product RENAME COLUMN catalog_product_id TO product_id;

ALTER INDEX IF EXISTS uq_product_org_catalog_product RENAME TO uq_store_product_org_product;
ALTER INDEX IF EXISTS idx_catalog_product_name_brand RENAME TO idx_product_name_brand;
ALTER INDEX IF EXISTS idx_catalog_product_sku RENAME TO idx_product_sku;
ALTER INDEX IF EXISTS idx_erp_product_org_sku RENAME TO idx_store_product_org_sku;

ALTER TABLE store_product RENAME CONSTRAINT fk_product_catalog_product TO fk_store_product_product;

ALTER TABLE product RENAME CONSTRAINT fk_catalog_product_created_by TO fk_product_created_by;
ALTER TABLE product RENAME CONSTRAINT fk_catalog_product_updated_by TO fk_product_updated_by;


-- ===== SOURCE: 015_add_due_dates_and_outstanding_support.sql =====

ALTER TABLE sales_invoice
    ADD COLUMN IF NOT EXISTS due_date DATE;

ALTER TABLE purchase_receipt
    ADD COLUMN IF NOT EXISTS due_date DATE;

UPDATE sales_invoice
SET due_date = invoice_date
WHERE due_date IS NULL;

UPDATE purchase_receipt
SET due_date = receipt_date
WHERE due_date IS NULL;

ALTER TABLE sales_invoice
    ALTER COLUMN due_date SET NOT NULL;

ALTER TABLE purchase_receipt
    ALTER COLUMN due_date SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_sales_invoice_org_due_date
    ON sales_invoice(organization_id, due_date, status);

CREATE INDEX IF NOT EXISTS idx_purchase_receipt_org_due_date
    ON purchase_receipt(organization_id, due_date, status);


-- ===== SOURCE: 016_add_erp_expense_accounting.sql =====

ALTER TABLE expense_category
    ADD COLUMN IF NOT EXISTS expense_account_id BIGINT;

ALTER TABLE expense
    ADD COLUMN IF NOT EXISTS due_date DATE,
    ADD COLUMN IF NOT EXISTS payment_method VARCHAR(30);

ALTER TABLE ledger_entry
    ADD COLUMN IF NOT EXISTS expense_id BIGINT;

UPDATE expense
SET due_date = expense_date
WHERE due_date IS NULL;

UPDATE expense_category ec
SET expense_account_id = a.id
FROM account a
WHERE ec.organization_id = a.organization_id
  AND a.code = 'EXPENSE_CONTROL'
  AND ec.expense_account_id IS NULL;

INSERT INTO account (organization_id, code, name, account_type, parent_account_id, is_system, is_active, created_at, updated_at)
SELECT o.id, 'EXPENSE_PAYABLE', 'Expense Payable', 'LIABILITY', NULL, TRUE, TRUE, NOW(), NOW()
FROM organization o
WHERE NOT EXISTS (
    SELECT 1 FROM account a WHERE a.organization_id = o.id AND a.code = 'EXPENSE_PAYABLE'
);

UPDATE expense
SET payment_method = 'CASH'
WHERE payment_method IS NULL AND status = 'PAID';

CREATE INDEX IF NOT EXISTS idx_expense_org_due_date
    ON expense(organization_id, due_date, status);

CREATE INDEX IF NOT EXISTS idx_expense_category_account
    ON expense_category(organization_id, expense_account_id);

CREATE INDEX IF NOT EXISTS idx_ledger_expense
    ON ledger_entry(expense_id, entry_date);


-- ===== SOURCE: 017_add_supplier_relationship_model.sql =====

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


-- ===== SOURCE: 018_add_customer_relationship_model.sql =====

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


-- ===== SOURCE: 019_add_returns_flow.sql =====

INSERT INTO permission (code, name, module_code)
SELECT 'purchase.return', 'Process purchase returns', 'PURCHASE'
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = 'purchase.return');

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.code = 'purchase.return'
WHERE r.code IN ('OWNER','ADMIN','STORE_MANAGER','PURCHASE_OPERATOR')
  AND NOT EXISTS (
    SELECT 1
    FROM role_permission rp
    WHERE rp.role_id = r.id
      AND rp.permission_id = p.id
  );

ALTER TABLE sales_invoice_line
  ADD COLUMN IF NOT EXISTS unit_cost_at_sale NUMERIC(18,2) NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS total_cost_at_sale NUMERIC(18,2) NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS sales_return (
  id BIGSERIAL PRIMARY KEY,
  organization_id BIGINT NOT NULL REFERENCES organization(id) ON DELETE RESTRICT,
  branch_id BIGINT NOT NULL REFERENCES branch(id) ON DELETE RESTRICT,
  warehouse_id BIGINT NOT NULL REFERENCES warehouse(id) ON DELETE RESTRICT,
  customer_id BIGINT NOT NULL REFERENCES customer(id) ON DELETE RESTRICT,
  original_sales_invoice_id BIGINT NOT NULL REFERENCES sales_invoice(id) ON DELETE RESTRICT,
  return_number VARCHAR(60) NOT NULL,
  return_date DATE NOT NULL,
  seller_gstin VARCHAR(30),
  customer_gstin VARCHAR(30),
  place_of_supply_state_code VARCHAR(10),
  reason VARCHAR(120),
  status VARCHAR(20) NOT NULL DEFAULT 'POSTED',
  subtotal NUMERIC(18,2) NOT NULL DEFAULT 0,
  tax_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  total_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  remarks TEXT,
  posted_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  CONSTRAINT uq_sales_return_org_number UNIQUE (organization_id, return_number),
  CONSTRAINT chk_sales_return_status CHECK (status IN ('DRAFT','POSTED','CANCELLED'))
);

CREATE TABLE IF NOT EXISTS sales_return_line (
  id BIGSERIAL PRIMARY KEY,
  sales_return_id BIGINT NOT NULL REFERENCES sales_return(id) ON DELETE CASCADE,
  original_sales_invoice_line_id BIGINT NOT NULL REFERENCES sales_invoice_line(id) ON DELETE RESTRICT,
  product_id BIGINT NOT NULL REFERENCES store_product(id) ON DELETE RESTRICT,
  uom_id BIGINT NOT NULL REFERENCES uom(id) ON DELETE RESTRICT,
  quantity NUMERIC(18,6) NOT NULL,
  base_quantity NUMERIC(18,6) NOT NULL,
  unit_price NUMERIC(18,2) NOT NULL,
  discount_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  taxable_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  tax_rate NUMERIC(9,4) NOT NULL DEFAULT 0,
  cgst_rate NUMERIC(9,4) NOT NULL DEFAULT 0,
  cgst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  sgst_rate NUMERIC(9,4) NOT NULL DEFAULT 0,
  sgst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  igst_rate NUMERIC(9,4) NOT NULL DEFAULT 0,
  igst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  cess_rate NUMERIC(9,4) NOT NULL DEFAULT 0,
  cess_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  line_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  unit_cost_at_return NUMERIC(18,2) NOT NULL DEFAULT 0,
  total_cost_at_return NUMERIC(18,2) NOT NULL DEFAULT 0,
  disposition VARCHAR(30) NOT NULL DEFAULT 'RESTOCK',
  reason VARCHAR(120),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  CONSTRAINT chk_sales_return_disposition CHECK (disposition IN ('RESTOCK','DAMAGED','SERVICE_PENDING','SCRAP'))
);

CREATE TABLE IF NOT EXISTS purchase_return (
  id BIGSERIAL PRIMARY KEY,
  organization_id BIGINT NOT NULL REFERENCES organization(id) ON DELETE RESTRICT,
  branch_id BIGINT NOT NULL REFERENCES branch(id) ON DELETE RESTRICT,
  warehouse_id BIGINT NOT NULL REFERENCES warehouse(id) ON DELETE RESTRICT,
  supplier_id BIGINT NOT NULL REFERENCES supplier(id) ON DELETE RESTRICT,
  original_purchase_receipt_id BIGINT NOT NULL REFERENCES purchase_receipt(id) ON DELETE RESTRICT,
  return_number VARCHAR(60) NOT NULL,
  return_date DATE NOT NULL,
  seller_gstin VARCHAR(30),
  supplier_gstin VARCHAR(30),
  place_of_supply_state_code VARCHAR(10),
  reason VARCHAR(120),
  status VARCHAR(20) NOT NULL DEFAULT 'POSTED',
  subtotal NUMERIC(18,2) NOT NULL DEFAULT 0,
  tax_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  total_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  remarks TEXT,
  posted_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  CONSTRAINT uq_purchase_return_org_number UNIQUE (organization_id, return_number),
  CONSTRAINT chk_purchase_return_status CHECK (status IN ('DRAFT','POSTED','CANCELLED'))
);

CREATE TABLE IF NOT EXISTS purchase_return_line (
  id BIGSERIAL PRIMARY KEY,
  purchase_return_id BIGINT NOT NULL REFERENCES purchase_return(id) ON DELETE CASCADE,
  original_purchase_receipt_line_id BIGINT NOT NULL REFERENCES purchase_receipt_line(id) ON DELETE RESTRICT,
  product_id BIGINT NOT NULL REFERENCES store_product(id) ON DELETE RESTRICT,
  uom_id BIGINT NOT NULL REFERENCES uom(id) ON DELETE RESTRICT,
  quantity NUMERIC(18,6) NOT NULL,
  base_quantity NUMERIC(18,6) NOT NULL,
  unit_cost NUMERIC(18,2) NOT NULL,
  taxable_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  tax_rate NUMERIC(9,4) NOT NULL DEFAULT 0,
  cgst_rate NUMERIC(9,4) NOT NULL DEFAULT 0,
  cgst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  sgst_rate NUMERIC(9,4) NOT NULL DEFAULT 0,
  sgst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  igst_rate NUMERIC(9,4) NOT NULL DEFAULT 0,
  igst_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  cess_rate NUMERIC(9,4) NOT NULL DEFAULT 0,
  cess_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  line_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  reason VARCHAR(120),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_sales_return_org_original_invoice ON sales_return (organization_id, original_sales_invoice_id, return_date);
CREATE INDEX IF NOT EXISTS idx_purchase_return_org_original_receipt ON purchase_return (organization_id, original_purchase_receipt_id, return_date);
CREATE INDEX IF NOT EXISTS idx_sales_return_line_original_line ON sales_return_line (original_sales_invoice_line_id);
CREATE INDEX IF NOT EXISTS idx_purchase_return_line_original_line ON purchase_return_line (original_purchase_receipt_line_id);


-- ===== SOURCE: 020_add_return_tracking_details.sql =====

CREATE TABLE IF NOT EXISTS sales_return_line_serial (
  id BIGSERIAL PRIMARY KEY,
  sales_return_line_id BIGINT NOT NULL REFERENCES sales_return_line(id) ON DELETE CASCADE,
  serial_number_id BIGINT NOT NULL REFERENCES serial_number(id) ON DELETE RESTRICT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  CONSTRAINT uq_sales_return_line_serial UNIQUE (sales_return_line_id, serial_number_id)
);

CREATE TABLE IF NOT EXISTS sales_return_line_batch (
  id BIGSERIAL PRIMARY KEY,
  sales_return_line_id BIGINT NOT NULL REFERENCES sales_return_line(id) ON DELETE CASCADE,
  batch_id BIGINT NOT NULL REFERENCES inventory_batch(id) ON DELETE RESTRICT,
  quantity NUMERIC(18,6) NOT NULL,
  base_quantity NUMERIC(18,6) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS purchase_return_line_serial (
  id BIGSERIAL PRIMARY KEY,
  purchase_return_line_id BIGINT NOT NULL REFERENCES purchase_return_line(id) ON DELETE CASCADE,
  serial_number_id BIGINT NOT NULL REFERENCES serial_number(id) ON DELETE RESTRICT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  CONSTRAINT uq_purchase_return_line_serial UNIQUE (purchase_return_line_id, serial_number_id)
);

CREATE TABLE IF NOT EXISTS purchase_return_line_batch (
  id BIGSERIAL PRIMARY KEY,
  purchase_return_line_id BIGINT NOT NULL REFERENCES purchase_return_line(id) ON DELETE CASCADE,
  batch_id BIGINT NOT NULL REFERENCES inventory_batch(id) ON DELETE RESTRICT,
  quantity NUMERIC(18,6) NOT NULL,
  base_quantity NUMERIC(18,6) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_sales_return_line_serial_serial ON sales_return_line_serial (serial_number_id);
CREATE INDEX IF NOT EXISTS idx_purchase_return_line_serial_serial ON purchase_return_line_serial (serial_number_id);


-- ===== SOURCE: 021_add_service_warranty_integration_links.sql =====

ALTER TABLE service_ticket
  ADD COLUMN IF NOT EXISTS sales_return_id BIGINT REFERENCES sales_return(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_service_ticket_sales_return ON service_ticket(sales_return_id);

ALTER TABLE warranty_claim
  ADD COLUMN IF NOT EXISTS product_ownership_id BIGINT REFERENCES product_ownership(id) ON DELETE SET NULL,
  ADD COLUMN IF NOT EXISTS sales_invoice_id BIGINT REFERENCES sales_invoice(id) ON DELETE SET NULL,
  ADD COLUMN IF NOT EXISTS sales_return_id BIGINT REFERENCES sales_return(id) ON DELETE SET NULL,
  ADD COLUMN IF NOT EXISTS warranty_start_date DATE,
  ADD COLUMN IF NOT EXISTS warranty_end_date DATE;

CREATE INDEX IF NOT EXISTS idx_warranty_claim_ownership ON warranty_claim(product_ownership_id);
CREATE INDEX IF NOT EXISTS idx_warranty_claim_sales_return ON warranty_claim(sales_return_id);


-- ===== SOURCE: 021_backfill_service_warranty_integration_links.sql =====

UPDATE service_ticket st
SET sales_return_id = sr.id
FROM sales_return sr
WHERE st.sales_return_id IS NULL
  AND st.customer_id = sr.customer_id
  AND st.sales_invoice_id = sr.original_sales_invoice_id
  AND st.source_type = 'REPLACEMENT';

UPDATE warranty_claim wc
SET product_ownership_id = po.id,
    sales_invoice_id = po.sales_invoice_id,
    warranty_start_date = po.warranty_start_date,
    warranty_end_date = po.warranty_end_date
FROM product_ownership po
WHERE wc.product_ownership_id IS NULL
  AND wc.customer_id = po.customer_id
  AND wc.product_id = po.product_id
  AND (
    (wc.serial_number_id IS NOT NULL AND wc.serial_number_id = po.serial_number_id)
    OR (wc.serial_number_id IS NULL AND po.serial_number_id IS NULL)
  );

UPDATE warranty_claim wc
SET sales_return_id = st.sales_return_id
FROM service_ticket st
WHERE wc.sales_return_id IS NULL
  AND wc.service_ticket_id = st.id
  AND st.sales_return_id IS NOT NULL;


-- ===== SOURCE: 022_add_service_replacement_flow.sql =====

CREATE TABLE IF NOT EXISTS service_replacement (
  id BIGSERIAL PRIMARY KEY,
  organization_id BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  branch_id BIGINT NOT NULL REFERENCES branch(id) ON DELETE RESTRICT,
  warehouse_id BIGINT NOT NULL REFERENCES warehouse(id) ON DELETE RESTRICT,
  service_ticket_id BIGINT REFERENCES service_ticket(id) ON DELETE SET NULL,
  warranty_claim_id BIGINT REFERENCES warranty_claim(id) ON DELETE SET NULL,
  sales_return_id BIGINT REFERENCES sales_return(id) ON DELETE SET NULL,
  customer_id BIGINT NOT NULL REFERENCES customer(id) ON DELETE RESTRICT,
  original_product_id BIGINT NOT NULL REFERENCES store_product(id) ON DELETE RESTRICT,
  original_serial_number_id BIGINT REFERENCES serial_number(id) ON DELETE SET NULL,
  original_product_ownership_id BIGINT REFERENCES product_ownership(id) ON DELETE SET NULL,
  replacement_product_id BIGINT NOT NULL REFERENCES store_product(id) ON DELETE RESTRICT,
  replacement_serial_number_id BIGINT REFERENCES serial_number(id) ON DELETE SET NULL,
  replacement_uom_id BIGINT NOT NULL REFERENCES uom(id) ON DELETE RESTRICT,
  replacement_quantity NUMERIC(18,6) NOT NULL,
  replacement_base_quantity NUMERIC(18,6) NOT NULL,
  replacement_number VARCHAR(80) NOT NULL,
  replacement_type VARCHAR(40) NOT NULL,
  status VARCHAR(20) NOT NULL,
  issued_on DATE NOT NULL,
  warranty_start_date DATE,
  warranty_end_date DATE,
  notes TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  CONSTRAINT uq_service_replacement_org_number UNIQUE (organization_id, replacement_number),
  CONSTRAINT chk_service_replacement_status CHECK (status IN ('ISSUED','CANCELLED')),
  CONSTRAINT chk_service_replacement_type CHECK (replacement_type IN ('WARRANTY_REPLACEMENT','SALES_RETURN_REPLACEMENT','GOODWILL_REPLACEMENT')),
  CONSTRAINT chk_service_replacement_qty CHECK (replacement_quantity > 0 AND replacement_base_quantity > 0)
);

CREATE INDEX IF NOT EXISTS idx_service_replacement_ticket ON service_replacement(service_ticket_id);
CREATE INDEX IF NOT EXISTS idx_service_replacement_claim ON service_replacement(warranty_claim_id);
CREATE INDEX IF NOT EXISTS idx_service_replacement_customer ON service_replacement(customer_id, issued_on DESC);


-- ===== SOURCE: 024_add_service_replacement_ledger_link.sql =====

ALTER TABLE ledger_entry
  ADD COLUMN IF NOT EXISTS service_replacement_id BIGINT REFERENCES service_replacement(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_ledger_service_replacement ON ledger_entry(service_replacement_id);


-- ===== SOURCE: 025_add_sales_invoice_line_warranty_months.sql =====

ALTER TABLE sales_invoice_line
    ADD COLUMN IF NOT EXISTS warranty_months INTEGER;

UPDATE sales_invoice_line
SET warranty_months = 0
WHERE warranty_months IS NULL;


-- ===== SOURCE: 026_add_inventory_reservation_tracking.sql =====

ALTER TABLE inventory_reservation
  ADD COLUMN IF NOT EXISTS batch_id BIGINT REFERENCES inventory_batch(id) ON DELETE RESTRICT,
  ADD COLUMN IF NOT EXISTS serial_number_id BIGINT REFERENCES serial_number(id) ON DELETE RESTRICT;

CREATE INDEX IF NOT EXISTS idx_inventory_reservation_source
  ON inventory_reservation(source_document_type, source_document_id, status);

CREATE INDEX IF NOT EXISTS idx_inventory_reservation_line
  ON inventory_reservation(source_document_line_id, status);

CREATE INDEX IF NOT EXISTS idx_inventory_reservation_serial
  ON inventory_reservation(serial_number_id, status);


-- ===== SOURCE: 027_add_store_product_pricing.sql =====

ALTER TABLE store_product
  ADD COLUMN IF NOT EXISTS default_sale_price NUMERIC(18,2);

CREATE TABLE IF NOT EXISTS store_product_price (
  id BIGSERIAL PRIMARY KEY,
  organization_id BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  store_product_id BIGINT NOT NULL REFERENCES store_product(id) ON DELETE CASCADE,
  price_type VARCHAR(40) NOT NULL DEFAULT 'SELLING',
  customer_segment VARCHAR(40),
  price NUMERIC(18,2) NOT NULL,
  min_quantity NUMERIC(18,6),
  effective_from DATE NOT NULL,
  effective_to DATE,
  is_default BOOLEAN NOT NULL DEFAULT FALSE,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by BIGINT,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by BIGINT
);

CREATE INDEX IF NOT EXISTS idx_store_product_price_lookup
  ON store_product_price(organization_id, store_product_id, price_type, is_active, effective_from);

INSERT INTO store_product_price (
  organization_id,
  store_product_id,
  price_type,
  customer_segment,
  price,
  effective_from,
  is_default,
  is_active,
  created_at,
  updated_at
)
SELECT
  sp.organization_id,
  sp.id,
  'SELLING',
  'RETAIL',
  sp.default_sale_price,
  CURRENT_DATE,
  TRUE,
  TRUE,
  NOW(),
  NOW()
FROM store_product sp
WHERE sp.default_sale_price IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM store_product_price spp
    WHERE spp.store_product_id = sp.id
      AND spp.price_type = 'SELLING'
      AND COALESCE(spp.customer_segment, 'RETAIL') = 'RETAIL'
      AND spp.is_active = TRUE
  );


-- ===== SOURCE: 028_add_sales_return_inspection.sql =====

ALTER TABLE sales_return
  ADD COLUMN IF NOT EXISTS inspected_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS inspected_by BIGINT,
  ADD COLUMN IF NOT EXISTS inspection_notes TEXT;

ALTER TABLE sales_return_line
  ADD COLUMN IF NOT EXISTS inspection_status VARCHAR(40) NOT NULL DEFAULT 'PENDING',
  ADD COLUMN IF NOT EXISTS inspection_notes TEXT;


-- ===== SOURCE: 029_add_warranty_claim_routing_and_replacement_bucket.sql =====

ALTER TABLE service_replacement
  ADD COLUMN IF NOT EXISTS stock_source_bucket VARCHAR(40) NOT NULL DEFAULT 'SALEABLE';

ALTER TABLE warranty_claim
  ADD COLUMN IF NOT EXISTS upstream_route_type VARCHAR(40),
  ADD COLUMN IF NOT EXISTS upstream_company_name VARCHAR(255),
  ADD COLUMN IF NOT EXISTS upstream_reference_number VARCHAR(120),
  ADD COLUMN IF NOT EXISTS upstream_status VARCHAR(40),
  ADD COLUMN IF NOT EXISTS routed_on DATE;


-- ===== SOURCE: 030_add_inventory_reservation_expiry.sql =====

ALTER TABLE inventory_reservation
    ADD COLUMN IF NOT EXISTS expires_at timestamp,
    ADD COLUMN IF NOT EXISTS released_at timestamp,
    ADD COLUMN IF NOT EXISTS release_reason varchar(100);

UPDATE inventory_reservation
SET expires_at = COALESCE(expires_at, created_at + interval '2 hours')
WHERE status = 'ACTIVE';

UPDATE inventory_reservation
SET released_at = COALESCE(released_at, updated_at, created_at),
    release_reason = COALESCE(release_reason, CASE
        WHEN status = 'CONSUMED' THEN 'CONSUMED'
        WHEN status = 'RELEASED' THEN 'LEGACY_RELEASE'
        ELSE release_reason
    END)
WHERE status IN ('CONSUMED', 'RELEASED');

CREATE INDEX IF NOT EXISTS idx_inventory_reservation_status_expires_at
    ON inventory_reservation(status, expires_at);


-- ===== SOURCE: 031_align_product_hsn_and_line_snapshots.sql =====

ALTER TABLE product
    ADD COLUMN IF NOT EXISTS hsn_code varchar(50);

ALTER TABLE purchase_order_line
    ADD COLUMN IF NOT EXISTS hsn_snapshot varchar(50);

ALTER TABLE purchase_receipt_line
    ADD COLUMN IF NOT EXISTS hsn_snapshot varchar(50);

ALTER TABLE sales_invoice_line
    ADD COLUMN IF NOT EXISTS hsn_snapshot varchar(50);

CREATE INDEX IF NOT EXISTS idx_product_hsn_code
    ON product(hsn_code);


-- ===== SOURCE: 032_drop_product_sku_and_backfill_store_product.sql =====

UPDATE store_product sp
SET sku = p.sku
FROM product p
WHERE sp.product_id = p.id
  AND p.sku IS NOT NULL
  AND btrim(p.sku) <> ''
  AND (sp.sku IS NULL OR btrim(sp.sku) = '');

DROP INDEX IF EXISTS idx_product_sku;

ALTER TABLE product
    DROP COLUMN IF EXISTS sku;


-- ===== SOURCE: 034_add_sales_quote_and_order_flow.sql =====

CREATE TABLE IF NOT EXISTS sales_quote (
  id BIGSERIAL PRIMARY KEY,
  organization_id BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  branch_id BIGINT NOT NULL REFERENCES branch(id) ON DELETE RESTRICT,
  warehouse_id BIGINT NOT NULL REFERENCES warehouse(id) ON DELETE RESTRICT,
  customer_id BIGINT NOT NULL REFERENCES customer(id) ON DELETE RESTRICT,
  quote_type VARCHAR(20) NOT NULL,
  quote_number VARCHAR(100) NOT NULL,
  quote_date DATE NOT NULL,
  valid_until DATE,
  seller_tax_registration_id BIGINT REFERENCES tax_registration(id) ON DELETE SET NULL,
  seller_gstin VARCHAR(30),
  customer_gstin VARCHAR(30),
  place_of_supply_state_code VARCHAR(10),
  status VARCHAR(40) NOT NULL,
  subtotal NUMERIC(18,2) NOT NULL DEFAULT 0,
  discount_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  tax_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  total_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  converted_sales_order_id BIGINT,
  converted_sales_invoice_id BIGINT REFERENCES sales_invoice(id) ON DELETE SET NULL,
  remarks TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  CONSTRAINT uq_sales_quote_number UNIQUE (organization_id, quote_number),
  CONSTRAINT chk_sales_quote_type CHECK (quote_type IN ('ESTIMATE','QUOTATION'))
);

CREATE TABLE IF NOT EXISTS sales_quote_line (
  id BIGSERIAL PRIMARY KEY,
  sales_quote_id BIGINT NOT NULL REFERENCES sales_quote(id) ON DELETE CASCADE,
  product_id BIGINT NOT NULL REFERENCES store_product(id) ON DELETE RESTRICT,
  uom_id BIGINT NOT NULL REFERENCES uom(id) ON DELETE RESTRICT,
  hsn_snapshot VARCHAR(50),
  quantity NUMERIC(18,6) NOT NULL,
  base_quantity NUMERIC(18,6) NOT NULL,
  unit_price NUMERIC(18,2) NOT NULL,
  discount_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  taxable_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  tax_rate NUMERIC(10,4) NOT NULL DEFAULT 0,
  cgst_rate NUMERIC(10,4),
  cgst_amount NUMERIC(18,2),
  sgst_rate NUMERIC(10,4),
  sgst_amount NUMERIC(18,2),
  igst_rate NUMERIC(10,4),
  igst_amount NUMERIC(18,2),
  cess_rate NUMERIC(10,4),
  cess_amount NUMERIC(18,2),
  line_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  remarks TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS sales_order (
  id BIGSERIAL PRIMARY KEY,
  organization_id BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  branch_id BIGINT NOT NULL REFERENCES branch(id) ON DELETE RESTRICT,
  warehouse_id BIGINT NOT NULL REFERENCES warehouse(id) ON DELETE RESTRICT,
  customer_id BIGINT NOT NULL REFERENCES customer(id) ON DELETE RESTRICT,
  source_quote_id BIGINT,
  order_number VARCHAR(100) NOT NULL,
  order_date DATE NOT NULL,
  seller_tax_registration_id BIGINT REFERENCES tax_registration(id) ON DELETE SET NULL,
  seller_gstin VARCHAR(30),
  customer_gstin VARCHAR(30),
  place_of_supply_state_code VARCHAR(10),
  status VARCHAR(40) NOT NULL,
  subtotal NUMERIC(18,2) NOT NULL DEFAULT 0,
  discount_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  tax_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  total_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  converted_sales_invoice_id BIGINT REFERENCES sales_invoice(id) ON DELETE SET NULL,
  remarks TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  CONSTRAINT uq_sales_order_number UNIQUE (organization_id, order_number)
);

CREATE TABLE IF NOT EXISTS sales_order_line (
  id BIGSERIAL PRIMARY KEY,
  sales_order_id BIGINT NOT NULL REFERENCES sales_order(id) ON DELETE CASCADE,
  source_quote_line_id BIGINT REFERENCES sales_quote_line(id) ON DELETE SET NULL,
  product_id BIGINT NOT NULL REFERENCES store_product(id) ON DELETE RESTRICT,
  uom_id BIGINT NOT NULL REFERENCES uom(id) ON DELETE RESTRICT,
  hsn_snapshot VARCHAR(50),
  quantity NUMERIC(18,6) NOT NULL,
  base_quantity NUMERIC(18,6) NOT NULL,
  unit_price NUMERIC(18,2) NOT NULL,
  discount_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  taxable_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  tax_rate NUMERIC(10,4) NOT NULL DEFAULT 0,
  cgst_rate NUMERIC(10,4),
  cgst_amount NUMERIC(18,2),
  sgst_rate NUMERIC(10,4),
  sgst_amount NUMERIC(18,2),
  igst_rate NUMERIC(10,4),
  igst_amount NUMERIC(18,2),
  cess_rate NUMERIC(10,4),
  cess_amount NUMERIC(18,2),
  line_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  remarks TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL
);

ALTER TABLE sales_quote
  ADD CONSTRAINT fk_sales_quote_converted_order
  FOREIGN KEY (converted_sales_order_id) REFERENCES sales_order(id) ON DELETE SET NULL;

ALTER TABLE sales_order
  ADD CONSTRAINT fk_sales_order_source_quote
  FOREIGN KEY (source_quote_id) REFERENCES sales_quote(id) ON DELETE SET NULL;

ALTER TABLE sales_invoice
  ADD COLUMN IF NOT EXISTS source_quote_id BIGINT REFERENCES sales_quote(id) ON DELETE SET NULL,
  ADD COLUMN IF NOT EXISTS source_order_id BIGINT REFERENCES sales_order(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_sales_quote_org_date ON sales_quote(organization_id, quote_date DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_sales_order_org_date ON sales_order(organization_id, order_date DESC, id DESC);


-- ===== SOURCE: 035_expand_sales_invoice_status_constraint.sql =====

ALTER TABLE sales_invoice
  DROP CONSTRAINT IF EXISTS chk_sales_invoice_status;

ALTER TABLE sales_invoice
  ADD CONSTRAINT chk_sales_invoice_status
  CHECK (status IN (
    'DRAFT',
    'SUBMITTED',
    'PENDING_APPROVAL',
    'CONFIRMED',
    'POSTED',
    'PARTIALLY_PAID',
    'PAID',
    'RETURNED',
    'CANCELLED'
  ));


-- ===== SOURCE: 037_add_recurring_invoice_and_journal_flow.sql =====

CREATE TABLE recurring_sales_invoice (
  id                     BIGSERIAL PRIMARY KEY,
  organization_id        BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  branch_id              BIGINT NOT NULL REFERENCES branch(id) ON DELETE CASCADE,
  warehouse_id           BIGINT NOT NULL REFERENCES warehouse(id) ON DELETE RESTRICT,
  customer_id            BIGINT NOT NULL REFERENCES customer(id) ON DELETE RESTRICT,
  price_list_id          BIGINT,
  template_number        VARCHAR(80) NOT NULL,
  frequency              VARCHAR(20) NOT NULL,
  start_date             DATE NOT NULL,
  next_run_date          DATE NOT NULL,
  end_date               DATE,
  due_days               INTEGER,
  place_of_supply_state_code VARCHAR(8),
  remarks                TEXT,
  is_active              BOOLEAN NOT NULL DEFAULT TRUE,
  last_run_at            TIMESTAMP,
  last_sales_invoice_id  BIGINT REFERENCES sales_invoice(id) ON DELETE SET NULL,
  created_at             TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at             TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by             BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  updated_by             BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  CONSTRAINT uq_recurring_sales_invoice_org_number UNIQUE (organization_id, template_number),
  CONSTRAINT chk_recurring_sales_invoice_frequency CHECK (frequency IN ('DAILY','WEEKLY','MONTHLY','QUARTERLY','YEARLY')),
  CONSTRAINT chk_recurring_sales_invoice_due_days CHECK (due_days IS NULL OR due_days >= 0)
);

CREATE INDEX idx_recurring_sales_invoice_org_next_run
  ON recurring_sales_invoice(organization_id, is_active, next_run_date);

CREATE TABLE recurring_sales_invoice_line (
  id                          BIGSERIAL PRIMARY KEY,
  recurring_sales_invoice_id  BIGINT NOT NULL REFERENCES recurring_sales_invoice(id) ON DELETE CASCADE,
  product_id                  BIGINT NOT NULL REFERENCES store_product(id) ON DELETE RESTRICT,
  uom_id                      BIGINT NOT NULL REFERENCES uom(id) ON DELETE RESTRICT,
  quantity                    NUMERIC(18,3) NOT NULL,
  base_quantity               NUMERIC(18,3) NOT NULL,
  unit_price                  NUMERIC(18,2),
  discount_amount             NUMERIC(18,2) NOT NULL DEFAULT 0,
  warranty_months             INTEGER,
  remarks                     TEXT,
  created_at                  TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at                  TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by                  BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  updated_by                  BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  CONSTRAINT chk_recurring_sales_invoice_line_qty CHECK (quantity > 0 AND base_quantity > 0),
  CONSTRAINT chk_recurring_sales_invoice_line_discount CHECK (discount_amount >= 0)
);

CREATE TABLE recurring_journal (
  id                     BIGSERIAL PRIMARY KEY,
  organization_id        BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  branch_id              BIGINT NOT NULL REFERENCES branch(id) ON DELETE CASCADE,
  template_number        VARCHAR(80) NOT NULL,
  voucher_type           VARCHAR(30) NOT NULL,
  frequency              VARCHAR(20) NOT NULL,
  start_date             DATE NOT NULL,
  next_run_date          DATE NOT NULL,
  end_date               DATE,
  remarks                TEXT,
  is_active              BOOLEAN NOT NULL DEFAULT TRUE,
  last_run_at            TIMESTAMP,
  last_voucher_id        BIGINT REFERENCES voucher(id) ON DELETE SET NULL,
  created_at             TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at             TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by             BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  updated_by             BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  CONSTRAINT uq_recurring_journal_org_number UNIQUE (organization_id, template_number),
  CONSTRAINT chk_recurring_journal_frequency CHECK (frequency IN ('DAILY','WEEKLY','MONTHLY','QUARTERLY','YEARLY'))
);

CREATE INDEX idx_recurring_journal_org_next_run
  ON recurring_journal(organization_id, is_active, next_run_date);

CREATE TABLE recurring_journal_line (
  id                    BIGSERIAL PRIMARY KEY,
  recurring_journal_id  BIGINT NOT NULL REFERENCES recurring_journal(id) ON DELETE CASCADE,
  account_id            BIGINT NOT NULL REFERENCES account(id) ON DELETE RESTRICT,
  debit_amount          NUMERIC(18,2) NOT NULL DEFAULT 0,
  credit_amount         NUMERIC(18,2) NOT NULL DEFAULT 0,
  narrative             TEXT,
  customer_id           BIGINT REFERENCES customer(id) ON DELETE SET NULL,
  supplier_id           BIGINT REFERENCES supplier(id) ON DELETE SET NULL,
  created_at            TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at            TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by            BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  updated_by            BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  CONSTRAINT chk_recurring_journal_line_debit_credit CHECK (
    (debit_amount > 0 AND credit_amount = 0) OR
    (credit_amount > 0 AND debit_amount = 0)
  )
);


-- ===== SOURCE: 038_expand_inventory_operation_status_constraints.sql =====

ALTER TABLE stock_adjustment
    DROP CONSTRAINT IF EXISTS chk_stock_adjustment_status;

ALTER TABLE stock_adjustment
    ADD CONSTRAINT chk_stock_adjustment_status
    CHECK (status IN ('DRAFT','SUBMITTED','PENDING_APPROVAL','APPROVED','POSTED','REJECTED','CANCELLED'));

ALTER TABLE stock_transfer
    DROP CONSTRAINT IF EXISTS chk_stock_transfer_status;

ALTER TABLE stock_transfer
    ADD CONSTRAINT chk_stock_transfer_status
    CHECK (status IN ('DRAFT','SUBMITTED','PENDING_APPROVAL','POSTED','REJECTED','CANCELLED'));


-- ===== SOURCE: 039_add_bank_reconciliation_flow.sql =====

CREATE TABLE bank_statement_entry (
  id                     BIGSERIAL PRIMARY KEY,
  organization_id        BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  branch_id              BIGINT REFERENCES branch(id) ON DELETE SET NULL,
  account_id             BIGINT NOT NULL REFERENCES account(id) ON DELETE RESTRICT,
  entry_date             DATE NOT NULL,
  value_date             DATE,
  reference_number       VARCHAR(120),
  description            TEXT,
  debit_amount           NUMERIC(18,2) NOT NULL DEFAULT 0,
  credit_amount          NUMERIC(18,2) NOT NULL DEFAULT 0,
  status                 VARCHAR(20) NOT NULL DEFAULT 'UNMATCHED',
  matched_ledger_entry_id BIGINT REFERENCES ledger_entry(id) ON DELETE SET NULL,
  matched_on             TIMESTAMP,
  matched_by             BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  remarks                TEXT,
  created_at             TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at             TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by             BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  updated_by             BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  CONSTRAINT chk_bank_statement_entry_amount CHECK (
    (debit_amount > 0 AND credit_amount = 0) OR
    (credit_amount > 0 AND debit_amount = 0)
  ),
  CONSTRAINT chk_bank_statement_entry_status CHECK (status IN ('UNMATCHED','MATCHED','RECONCILED','IGNORED'))
);

CREATE INDEX idx_bank_statement_entry_org_account_date
  ON bank_statement_entry(organization_id, account_id, entry_date DESC);

CREATE UNIQUE INDEX uq_bank_statement_entry_matched_ledger_entry
  ON bank_statement_entry(matched_ledger_entry_id)
  WHERE matched_ledger_entry_id IS NOT NULL;


-- ===== SOURCE: 040_expand_serial_status_constraint.sql =====

ALTER TABLE serial_number DROP CONSTRAINT IF EXISTS chk_serial_status;

ALTER TABLE serial_number
  ADD CONSTRAINT chk_serial_status CHECK (
    status IN (
      'IN_STOCK',
      'RESERVED',
      'ALLOCATED',
      'SOLD',
      'RETURNED',
      'REPLACED',
      'SCRAPPED',
      'SERVICE_IN',
      'SERVICE_OUT'
    )
  );


-- ===== SOURCE: 041_expand_return_status_constraints.sql =====

ALTER TABLE sales_return DROP CONSTRAINT IF EXISTS chk_sales_return_status;

ALTER TABLE sales_return
  ADD CONSTRAINT chk_sales_return_status
    CHECK (status IN ('DRAFT','PENDING_INSPECTION','PENDING_APPROVAL','POSTED','REJECTED','CANCELLED'));

ALTER TABLE purchase_return DROP CONSTRAINT IF EXISTS chk_purchase_return_status;

ALTER TABLE purchase_return
  ADD CONSTRAINT chk_purchase_return_status
    CHECK (status IN ('DRAFT','SUBMITTED','PENDING_APPROVAL','POSTED','REJECTED','CANCELLED'));


-- ===== SOURCE: 042_add_store_product_supplier_preference.sql =====

CREATE TABLE IF NOT EXISTS store_product_supplier_preference (
  id BIGSERIAL PRIMARY KEY,
  organization_id BIGINT NOT NULL REFERENCES organization(id) ON DELETE RESTRICT,
  store_product_id BIGINT NOT NULL REFERENCES store_product(id) ON DELETE RESTRICT,
  supplier_id BIGINT NOT NULL REFERENCES supplier(id) ON DELETE RESTRICT,
  supplier_product_id BIGINT NOT NULL REFERENCES supplier_product(id) ON DELETE RESTRICT,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  remarks TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
  CONSTRAINT uq_store_product_supplier_preference UNIQUE (organization_id, store_product_id)
);

CREATE INDEX IF NOT EXISTS idx_erp_store_product_supplier_pref_product
  ON store_product_supplier_preference (organization_id, store_product_id, is_active);

CREATE INDEX IF NOT EXISTS idx_erp_store_product_supplier_pref_supplier
  ON store_product_supplier_preference (organization_id, supplier_id, is_active);


-- ===== SOURCE: 043_owner_account_subscription_model.sql =====

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


-- ===== SOURCE: 047_add_store_product_attribute_value.sql =====

CREATE TABLE store_product_attribute_value (
  id                      BIGSERIAL PRIMARY KEY,
  organization_id         BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  store_product_id        BIGINT NOT NULL REFERENCES store_product(id) ON DELETE CASCADE,
  attribute_definition_id BIGINT NOT NULL REFERENCES product_attribute_definition(id) ON DELETE CASCADE,
  value_text              VARCHAR(500),
  value_number            NUMERIC(18,6),
  value_boolean           BOOLEAN,
  value_date              DATE,
  value_option_id         BIGINT REFERENCES product_attribute_option(id) ON DELETE SET NULL,
  value_json              JSONB,
  created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by              BIGINT,
  updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by              BIGINT,
  CONSTRAINT uq_store_product_attribute_value UNIQUE (store_product_id, attribute_definition_id)
);

CREATE INDEX idx_store_product_attribute_value_lookup
  ON store_product_attribute_value (organization_id, store_product_id, attribute_definition_id);


-- ===== SOURCE: 048_add_store_product_warranty_fields.sql =====

ALTER TABLE store_product
    ADD COLUMN IF NOT EXISTS default_warranty_months INTEGER,
    ADD COLUMN IF NOT EXISTS warranty_terms TEXT;

UPDATE store_product
SET default_warranty_months = 0
WHERE default_warranty_months IS NULL;


-- ===== SOURCE: 049_add_warranty_extension_flow.sql =====

CREATE TABLE warranty_extension (
  id                    BIGSERIAL PRIMARY KEY,
  organization_id       BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  product_ownership_id  BIGINT NOT NULL REFERENCES product_ownership(id) ON DELETE CASCADE,
  serial_number_id      BIGINT REFERENCES serial_number(id) ON DELETE SET NULL,
  sales_invoice_id      BIGINT REFERENCES sales_invoice(id) ON DELETE SET NULL,
  sales_invoice_line_id BIGINT REFERENCES sales_invoice_line(id) ON DELETE SET NULL,
  extension_type        VARCHAR(40) NOT NULL,
  months_added          INTEGER NOT NULL,
  start_date            DATE,
  end_date              DATE,
  status                VARCHAR(20) NOT NULL,
  reason                VARCHAR(300),
  reference_number      VARCHAR(120),
  amount                NUMERIC(18,2),
  remarks               TEXT,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by            BIGINT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by            BIGINT,
  CONSTRAINT chk_warranty_extension_type CHECK (extension_type IN ('MANUFACTURER_PROMO','PAID_EXTENDED','GOODWILL','MANUAL_CORRECTION')),
  CONSTRAINT chk_warranty_extension_status CHECK (status IN ('ACTIVE','CANCELLED','EXPIRED')),
  CONSTRAINT chk_warranty_extension_months CHECK (months_added > 0)
);

CREATE INDEX idx_warranty_extension_ownership ON warranty_extension(organization_id, product_ownership_id, status, id);


-- ===== SOURCE: 050_add_service_agreement_flow.sql =====

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


-- ===== SOURCE: 044_add_hsn_master.sql (DDL only) =====

CREATE TABLE hsn_master (
  id                  BIGSERIAL PRIMARY KEY,
  hsn_code            VARCHAR(20) NOT NULL,
  description         VARCHAR(500) NOT NULL,
  chapter_code        VARCHAR(10),
  cgst_rate           NUMERIC(10,2) NOT NULL DEFAULT 0,
  sgst_rate           NUMERIC(10,2) NOT NULL DEFAULT 0,
  igst_rate           NUMERIC(10,2) NOT NULL DEFAULT 0,
  cess_rate           NUMERIC(10,2) NOT NULL DEFAULT 0,
  is_active           BOOLEAN NOT NULL DEFAULT TRUE,
  source_name         VARCHAR(120),
  effective_from      DATE,
  created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by          BIGINT,
  updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by          BIGINT,
  CONSTRAINT uq_hsn_master_code UNIQUE (hsn_code)
);

CREATE INDEX idx_hsn_master_description ON hsn_master(description);



-- ===== SOURCE: 045_add_hsn_tax_rate.sql (DDL only) =====

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



-- ===== SOURCE: 046_add_product_attribute_metadata.sql (DDL only) =====

CREATE TABLE product_attribute_definition (
  id                  BIGSERIAL PRIMARY KEY,
  organization_id     BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  code                VARCHAR(80) NOT NULL,
  label               VARCHAR(150) NOT NULL,
  description         VARCHAR(500),
  data_type           VARCHAR(30) NOT NULL,
  input_type          VARCHAR(30) NOT NULL,
  is_required         BOOLEAN NOT NULL DEFAULT FALSE,
  is_active           BOOLEAN NOT NULL DEFAULT TRUE,
  unit_label          VARCHAR(50),
  placeholder         VARCHAR(150),
  help_text           VARCHAR(300),
  sort_order          INTEGER NOT NULL DEFAULT 1,
  created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by          BIGINT,
  updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by          BIGINT,
  CONSTRAINT uq_product_attribute_definition_org_code UNIQUE (organization_id, code)
);

CREATE TABLE product_attribute_option (
  id                  BIGSERIAL PRIMARY KEY,
  attribute_definition_id BIGINT NOT NULL REFERENCES product_attribute_definition(id) ON DELETE CASCADE,
  code                VARCHAR(80) NOT NULL,
  label               VARCHAR(150) NOT NULL,
  sort_order          INTEGER NOT NULL DEFAULT 1,
  is_active           BOOLEAN NOT NULL DEFAULT TRUE,
  created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by          BIGINT,
  updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by          BIGINT,
  CONSTRAINT uq_product_attribute_option UNIQUE (attribute_definition_id, code)
);

CREATE TABLE product_attribute_scope (
  id                  BIGSERIAL PRIMARY KEY,
  organization_id     BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  attribute_definition_id BIGINT NOT NULL REFERENCES product_attribute_definition(id) ON DELETE CASCADE,
  category_id         BIGINT REFERENCES category(id) ON DELETE CASCADE,
  brand_id            BIGINT REFERENCES brand(id) ON DELETE CASCADE,
  created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by          BIGINT,
  updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by          BIGINT
);

CREATE INDEX idx_product_attribute_definition_org_active ON product_attribute_definition(organization_id, is_active, sort_order);
CREATE INDEX idx_product_attribute_option_def_sort ON product_attribute_option(attribute_definition_id, sort_order);
CREATE INDEX idx_product_attribute_scope_lookup ON product_attribute_scope(organization_id, category_id, brand_id);

