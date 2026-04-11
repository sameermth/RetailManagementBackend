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
