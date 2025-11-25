-- Add allow_overspend flag to expense_category_budgets
ALTER TABLE expense_category_budgets
ADD COLUMN IF NOT EXISTS allow_overspend BOOLEAN DEFAULT TRUE NOT NULL;

-- Backfill: set true where null (defensive)
UPDATE expense_category_budgets SET allow_overspend = TRUE WHERE allow_overspend IS NULL;
