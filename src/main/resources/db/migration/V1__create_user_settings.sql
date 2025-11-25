-- Create user_settings table (portable SQL, no DB-specific procedural blocks)
CREATE TABLE IF NOT EXISTS user_settings (
    id BIGSERIAL PRIMARY KEY,
    id_user BIGINT NOT NULL UNIQUE,
    currency VARCHAR(10) NOT NULL,
    decimal_digits INT NOT NULL,
    week_start VARCHAR(20) NOT NULL
);

-- Add FK constraint referencing users (works for H2 and Postgres)
ALTER TABLE user_settings
  ADD CONSTRAINT IF NOT EXISTS fk_user_settings_user FOREIGN KEY (id_user) REFERENCES users (id) ON DELETE CASCADE;
