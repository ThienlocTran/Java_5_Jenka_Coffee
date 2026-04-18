-- ============================================================================
-- V20: Improve Admin Dashboard UX
-- ============================================================================
-- 
-- Changes:
-- 1. Add phone field to Contacts (replace email as primary contact method)
-- 2. Simplify Order status (remove unnecessary statuses)
-- 
-- Date: 2026-04-18
-- ============================================================================

-- 1. Add phone field to Contacts table
ALTER TABLE "contacts" ADD COLUMN IF NOT EXISTS "phone" VARCHAR(15);

-- 2. Make email nullable (phone is now primary contact method)
ALTER TABLE "contacts" ALTER COLUMN "email" DROP NOT NULL;

-- 3. Add index on isRead for faster notification queries
CREATE INDEX IF NOT EXISTS idx_contacts_isread ON "contacts"("isread");

-- 4. Add index on createdAt for sorting
CREATE INDEX IF NOT EXISTS idx_contacts_createdat ON "contacts"("createdat" DESC);

-- Note: Order status simplification will be handled in application logic
-- Status mapping:
-- 0 = NEW (Mới) - waiting for confirmation
-- 1 = CONFIRMED (Đã xác nhận) - confirmed by admin, ready to ship
-- 3 = CANCELLED (Đã hủy) - cancelled by admin or customer
-- 
-- Removed statuses (will be mapped to CONFIRMED in application):
-- 2 = SHIPPING (mapped to CONFIRMED)
-- 4 = COMPLETED (mapped to CONFIRMED)
