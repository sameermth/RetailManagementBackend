INSERT INTO account (organization_id, code, name, account_type, is_system, is_active)
SELECT o.id, src.code, src.name, src.account_type, TRUE, TRUE
FROM organization o
JOIN (
  VALUES
    ('CASH', 'Cash In Hand', 'ASSET'),
    ('BANK', 'Bank Account', 'ASSET'),
    ('AR', 'Accounts Receivable', 'ASSET'),
    ('INVENTORY', 'Inventory', 'ASSET'),
    ('AP', 'Accounts Payable', 'LIABILITY'),
    ('OUTPUT_GST', 'Output GST', 'LIABILITY'),
    ('INPUT_GST', 'Input GST', 'ASSET'),
    ('SALES', 'Sales Revenue', 'INCOME'),
    ('COGS', 'Cost of Goods Sold', 'EXPENSE'),
    ('DISCOUNT_ALLOWED', 'Discount Allowed', 'EXPENSE'),
    ('PURCHASES', 'Purchases', 'EXPENSE'),
    ('EXPENSE_CONTROL', 'Operating Expenses', 'EXPENSE'),
    ('STOCK_LOSS', 'Stock Adjustment Loss', 'EXPENSE'),
    ('STOCK_GAIN', 'Stock Adjustment Gain', 'INCOME')
) AS src(code, name, account_type) ON TRUE
ON CONFLICT (organization_id, code) DO NOTHING;
