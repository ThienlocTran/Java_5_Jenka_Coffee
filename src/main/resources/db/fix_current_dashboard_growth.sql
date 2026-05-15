-- ============================================================
-- JENKA COFFEE - Dashboard growth metrics support
--
-- Adds account createdate so admin dashboard can count new customers
-- in the current period. Existing rows are backfilled with NOW() because
-- older accounts did not store their creation timestamp.
-- ============================================================

SELECT current_database() AS database_name, current_schema() AS schema_name;

ALTER TABLE public."accounts"
    ADD COLUMN IF NOT EXISTS createdate TIMESTAMP;

UPDATE public."accounts"
SET createdate = NOW()
WHERE createdate IS NULL;

ALTER TABLE public."accounts"
    ALTER COLUMN createdate SET DEFAULT NOW(),
    ALTER COLUMN createdate SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_accounts_createdate
    ON public."accounts" (createdate DESC);
