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
