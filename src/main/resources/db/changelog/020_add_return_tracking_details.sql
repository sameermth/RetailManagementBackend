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
