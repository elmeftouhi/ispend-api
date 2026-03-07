-- Flyway migration: add tenant_id columns and indexes to tenant-scoped tables
-- This migration makes tenant_id nullable so it can be rolled out safely.
-- Run this migration against your Postgres DB (enable Flyway in application.yml or run manually).

ALTER TABLE IF EXISTS users ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(64);
CREATE INDEX IF NOT EXISTS idx_users_tenant_id ON users(tenant_id);

ALTER TABLE IF EXISTS expenses ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(64);
CREATE INDEX IF NOT EXISTS idx_expenses_tenant_id ON expenses(tenant_id);

ALTER TABLE IF EXISTS expense_categories ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(64);
CREATE INDEX IF NOT EXISTS idx_expense_categories_tenant_id ON expense_categories(tenant_id);

ALTER TABLE IF EXISTS expense_statuses ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(64);
CREATE INDEX IF NOT EXISTS idx_expense_statuses_tenant_id ON expense_statuses(tenant_id);

ALTER TABLE IF EXISTS user_settings ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(64);
CREATE INDEX IF NOT EXISTS idx_user_settings_tenant_id ON user_settings(tenant_id);

ALTER TABLE IF EXISTS expense_category_budgets ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(64);
CREATE INDEX IF NOT EXISTS idx_expense_category_budgets_tenant_id ON expense_category_budgets(tenant_id);

-- Optional backfill: uncomment and set the DEFAULT_TENANT value if you want to backfill existing rows
-- UPDATE users SET tenant_id = 'DEFAULT_TENANT' WHERE tenant_id IS NULL;
-- UPDATE expenses SET tenant_id = 'DEFAULT_TENANT' WHERE tenant_id IS NULL;
-- UPDATE expense_categories SET tenant_id = 'DEFAULT_TENANT' WHERE tenant_id IS NULL;
-- UPDATE expense_statuses SET tenant_id = 'DEFAULT_TENANT' WHERE tenant_id IS NULL;
-- UPDATE user_settings SET tenant_id = 'DEFAULT_TENANT' WHERE tenant_id IS NULL;
-- UPDATE expense_category_budgets SET tenant_id = 'DEFAULT_TENANT' WHERE tenant_id IS NULL;

