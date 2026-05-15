-- ============================================================
-- JENKA COFFEE - Add homepage featured ordering to existing DB
--
-- Run on the SAME database/schema Spring Boot uses:
--   psql -U <db_user> -d <db_name> -f src/main/resources/db/fix_current_products_featured_position.sql
-- ============================================================

SELECT current_database() AS database_name, current_schema() AS schema_name;

ALTER TABLE public.products
    ADD COLUMN IF NOT EXISTS featured_position INTEGER;

WITH ranked AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY createdate DESC, id DESC) AS rn
    FROM public.products
    WHERE COALESCE(isfeatured, false) = true
      AND featured_position IS NULL
)
UPDATE public.products p
SET featured_position = ranked.rn
FROM ranked
WHERE p.id = ranked.id;

CREATE INDEX IF NOT EXISTS idx_products_featured_position
    ON public.products (featured_position);

SELECT id, name, isfeatured, featured_position
FROM public.products
WHERE COALESCE(isfeatured, false) = true
ORDER BY featured_position NULLS LAST, createdate DESC;
