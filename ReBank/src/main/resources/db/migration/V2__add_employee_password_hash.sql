ALTER TABLE bank_employees
    ADD COLUMN IF NOT EXISTS password_hash VARCHAR(100);
