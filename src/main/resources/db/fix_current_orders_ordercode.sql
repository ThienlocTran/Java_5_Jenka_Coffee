-- ============================================================
-- JENKA COFFEE - Fix current PostgreSQL orders.ordercode column
-- Use this on an existing database that was created before the
-- public order code refactor.
--
-- Why this exists:
--   PostgreSQL folds unquoted orderCode to ordercode.
--   A quoted column named "orderCode" is a different column.
--   Hibernate inserts into ordercode, so the physical column must
--   be lowercase ordercode.
--
-- Run on the SAME database/schema that Spring Boot connects to:
--   psql -U <db_user> -d <db_name> -f src/main/resources/db/fix_current_orders_ordercode.sql
-- ============================================================

SELECT
    current_database() AS database_name,
    current_schema() AS schema_name;

DO $$
BEGIN
    IF to_regclass('public.orders') IS NULL THEN
        RAISE EXCEPTION 'Table public.orders does not exist. Check whether Spring Boot is using another schema/database.';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'orders'
          AND column_name = 'orderCode'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'orders'
          AND column_name = 'ordercode'
    ) THEN
        ALTER TABLE public.orders RENAME COLUMN "orderCode" TO ordercode;
        RAISE NOTICE 'Renamed public.orders."orderCode" to public.orders.ordercode';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'orders'
          AND column_name = 'ordercode'
    ) THEN
        ALTER TABLE public.orders ADD COLUMN ordercode VARCHAR(30);
        RAISE NOTICE 'Added public.orders.ordercode';
    END IF;

    UPDATE public.orders
    SET ordercode = 'ORD-' || to_char(COALESCE(createdate, CURRENT_TIMESTAMP), 'YYYYMMDD') || '-' || upper(substr(md5(id::text || clock_timestamp()::text), 1, 6))
    WHERE ordercode IS NULL OR trim(ordercode) = '';

    ALTER TABLE public.orders ALTER COLUMN ordercode SET NOT NULL;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'public.orders'::regclass
          AND conname = 'uq_order_code'
    ) THEN
        ALTER TABLE public.orders ADD CONSTRAINT uq_order_code UNIQUE (ordercode);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_order_code ON public.orders (ordercode);

-- Old voucher columns are not used anymore. Drop both possible spellings safely.
ALTER TABLE public.orders DROP COLUMN IF EXISTS vouchercode;
ALTER TABLE public.orders DROP COLUMN IF EXISTS "VoucherCode";

SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name = 'orders'
ORDER BY ordinal_position;
