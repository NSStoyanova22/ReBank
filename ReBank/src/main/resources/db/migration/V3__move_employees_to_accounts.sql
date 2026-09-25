ALTER TABLE accounts
    ADD COLUMN IF NOT EXISTS account_type VARCHAR(20) NOT NULL DEFAULT 'CLIENT',
    ADD COLUMN IF NOT EXISTS employee_bank_id BIGINT REFERENCES banks(id),
    ADD COLUMN IF NOT EXISTS employee_role VARCHAR(80);

INSERT INTO accounts (username, email, password_hash, created_at, account_type, employee_bank_id, employee_role)
SELECT
    'employee_' || id,
    'employee_' || id || '@rebank.local',
    COALESCE(password_hash, '$2a$10$7EqJtq98hPqEX7fNZaFWoO.cPi5b7eJfZpq1fYV8e1IrT1C7fR6jBS'),
    created_at,
    'EMPLOYEE',
    bank_id,
    role
FROM bank_employees
ON CONFLICT (username) DO NOTHING;

DROP TABLE IF EXISTS bank_employees;
