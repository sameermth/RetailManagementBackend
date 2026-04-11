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
