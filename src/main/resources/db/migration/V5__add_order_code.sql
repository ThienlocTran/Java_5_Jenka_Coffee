-- =============================================================================
-- V5 : Add public order code column to Orders table
-- Replaces raw numeric PK in public URLs  →  /orders/ORD-20260511-AB12CD
-- =============================================================================

-- Step 1: Add column as nullable first so existing rows are not rejected
ALTER TABLE "Orders"
    ADD COLUMN IF NOT EXISTS "orderCode" VARCHAR(30);

-- Step 2: Back-fill existing orders with a deterministic placeholder code.
--         Format keeps the ORD-YYYYMMDD-XXXXXX shape so the regex validator passes.
--         The LPAD ensures the numeric suffix is always 6 chars (padded with zeros).
UPDATE "Orders"
SET "orderCode" = 'ORD-' ||
                  TO_CHAR("CreateDate", 'YYYYMMDD') || '-' ||
                  LPAD(CAST("Id" AS VARCHAR), 6, '0')
WHERE "orderCode" IS NULL;

-- Step 3: Enforce NOT NULL and UNIQUE now that every row has a value
ALTER TABLE "Orders"
    ALTER COLUMN "orderCode" SET NOT NULL;

ALTER TABLE "Orders"
    ADD CONSTRAINT uq_order_code UNIQUE ("orderCode");

-- Step 4: Add index for fast lookups by orderCode (API calls use this)
CREATE INDEX IF NOT EXISTS idx_order_code ON "Orders" ("orderCode");
