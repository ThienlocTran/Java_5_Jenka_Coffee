-- VULN-SESSION-REVOCATION FIX: Add lastPasswordResetDate to Account table
-- Used to invalidate old JWT tokens after password reset
ALTER TABLE Accounts ADD COLUMN IF NOT EXISTS lastPasswordResetDate TIMESTAMP;

-- VULN-UNCAPPED-VOUCHER FIX: Add maxDiscountAmount to Voucher table
-- Caps percentage discounts to prevent unlimited discounts on large orders
ALTER TABLE Vouchers ADD COLUMN IF NOT EXISTS maxDiscountAmount DECIMAL(18,2);

-- Add comment for documentation
COMMENT ON COLUMN Accounts.lastPasswordResetDate IS 'VULN-SESSION-REVOCATION FIX: Timestamp of last password change, used to invalidate old JWT tokens';
COMMENT ON COLUMN Vouchers.maxDiscountAmount IS 'VULN-UNCAPPED-VOUCHER FIX: Maximum discount amount for percentage vouchers (e.g., 20% off but max 50k)';
