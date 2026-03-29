ALTER TABLE inventory_reservation
  ADD COLUMN IF NOT EXISTS batch_id BIGINT REFERENCES inventory_batch(id) ON DELETE RESTRICT,
  ADD COLUMN IF NOT EXISTS serial_number_id BIGINT REFERENCES serial_number(id) ON DELETE RESTRICT;

CREATE INDEX IF NOT EXISTS idx_inventory_reservation_source
  ON inventory_reservation(source_document_type, source_document_id, status);

CREATE INDEX IF NOT EXISTS idx_inventory_reservation_line
  ON inventory_reservation(source_document_line_id, status);

CREATE INDEX IF NOT EXISTS idx_inventory_reservation_serial
  ON inventory_reservation(serial_number_id, status);
