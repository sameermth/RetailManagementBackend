ALTER TABLE ledger_entry
  ADD COLUMN IF NOT EXISTS service_replacement_id BIGINT REFERENCES service_replacement(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_ledger_service_replacement ON ledger_entry(service_replacement_id);
