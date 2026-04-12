-- Migration V17: Fix Bugs 41-44
-- Bug 41: Email XSS (no DB changes needed - fixed in code)
-- Bug 42: Voucher Silent Upsert (no DB changes needed - fixed in code)
-- Bug 43: Deep Pagination DoS (no DB changes needed - fixed in code)
-- Bug 44: Add maxUsesPerUser field to Vouchers table

-- Add maxUsesPerUser column to Vouchers table
-- Default to 1 (one-time use per user) for backward compatibility
ALTER TABLE Vouchers ADD COLUMN IF NOT EXISTS maxUsesPerUser INT DEFAULT 1;

-- Update existing vouchers to have maxUsesPerUser = 1 (maintain current behavior)
UPDATE Vouchers SET maxUsesPerUser = 1 WHERE maxUsesPerUser IS NULL;
