-- =============================================================================
-- V21 : Add public order code column to Orders table
-- Replaces raw numeric PK in public URLs -> /orders/ORD-20260511-AB12CD
-- =============================================================================

-- Step 1: Add column as nullable first so existing rows are not rejected.
ALTER TABLE "orders"
    ADD COLUMN IF NOT EXISTS "orderCode" VARCHAR(30);

-- Step 2: Back-fill existing orders with a deterministic public code.
-- Format keeps the ORD-YYYYMMDD-XXXXXX shape so the API validator passes.
UPDATE "orders"
SET "orderCode" = 'ORD-' ||
                  TO_CHAR(COALESCE("createdate", NOW()), 'YYYYMMDD') || '-' ||
                  LPAD(CAST("id" AS VARCHAR), 6, '0')
WHERE "orderCode" IS NULL;

-- Step 3: Enforce NOT NULL now that every row has a value.
ALTER TABLE "orders"
    ALTER COLUMN "orderCode" SET NOT NULL;

-- Step 4: Add uniqueness and lookup index. Keep this idempotent for dev
-- databases where the column/constraint may have been applied manually.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uq_order_code'
    ) THEN
        ALTER TABLE "orders"
            ADD CONSTRAINT uq_order_code UNIQUE ("orderCode");
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_order_code ON "orders" ("orderCode");
