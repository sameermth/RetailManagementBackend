INSERT INTO account (organization_id, code, name, account_type, is_system, is_active)
SELECT o.id, src.code, src.name, src.account_type, TRUE, TRUE
FROM organization o
JOIN (
  VALUES
    ('WARRANTY_EXPENSE', 'Warranty Replacement Expense', 'EXPENSE'),
    ('GOODWILL_EXPENSE', 'Goodwill Replacement Expense', 'EXPENSE')
) AS src(code, name, account_type) ON TRUE
ON CONFLICT (organization_id, code) DO NOTHING;
