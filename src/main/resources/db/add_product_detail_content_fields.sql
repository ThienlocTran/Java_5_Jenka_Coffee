-- ============================================================
-- JENKA COFFEE - Add advisory/ecommerce content fields to products
--
-- Run on the SAME database/schema Spring Boot uses:
--   psql -U <db_user> -d <db_name> -f src/main/resources/db/add_product_detail_content_fields.sql
-- ============================================================

SELECT current_database() AS database_name, current_schema() AS schema_name;

ALTER TABLE public.products
    ADD COLUMN IF NOT EXISTS short_description TEXT,
    ADD COLUMN IF NOT EXISTS detail_description TEXT,
    ADD COLUMN IF NOT EXISTS specifications_json TEXT,
    ADD COLUMN IF NOT EXISTS features_json TEXT,
    ADD COLUMN IF NOT EXISTS warranty_info TEXT,
    ADD COLUMN IF NOT EXISTS shipping_info TEXT,
    ADD COLUMN IF NOT EXISTS suitable_for TEXT,
    ADD COLUMN IF NOT EXISTS faq_json TEXT,
    ADD COLUMN IF NOT EXISTS meta_title VARCHAR(255),
    ADD COLUMN IF NOT EXISTS meta_description TEXT;

SELECT
    column_name,
    data_type
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name = 'products'
  AND column_name IN (
      'short_description',
      'detail_description',
      'specifications_json',
      'features_json',
      'warranty_info',
      'shipping_info',
      'suitable_for',
      'faq_json',
      'meta_title',
      'meta_description'
  )
ORDER BY column_name;
