-- Safe additive production migration for Jenka Coffee
-- Purpose: align live VPS schema with current backend expectations without data loss.
-- Safety:
--   - no DROP TABLE
--   - no TRUNCATE
--   - no DELETE
--   - no rename
--   - no destructive reset
--   - backfill only NULL/blank values

BEGIN;

-- ============================================================
-- PRODUCTS
-- ============================================================

ALTER TABLE IF EXISTS products
    ADD COLUMN IF NOT EXISTS short_description TEXT,
    ADD COLUMN IF NOT EXISTS detail_description TEXT,
    ADD COLUMN IF NOT EXISTS specifications_json TEXT,
    ADD COLUMN IF NOT EXISTS features_json TEXT,
    ADD COLUMN IF NOT EXISTS faq_json TEXT,
    ADD COLUMN IF NOT EXISTS suitable_for TEXT,
    ADD COLUMN IF NOT EXISTS warranty_info TEXT,
    ADD COLUMN IF NOT EXISTS shipping_info TEXT,
    ADD COLUMN IF NOT EXISTS meta_title VARCHAR(255),
    ADD COLUMN IF NOT EXISTS meta_description VARCHAR(320),
    ADD COLUMN IF NOT EXISTS is_home_addon BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS home_addon_position INTEGER;

UPDATE products
SET is_home_addon = FALSE
WHERE is_home_addon IS NULL;

CREATE INDEX IF NOT EXISTS idx_products_home_addon
    ON products (is_home_addon, available, home_addon_position);

-- ============================================================
-- CATEGORIES
-- ============================================================

ALTER TABLE IF EXISTS categories
    ADD COLUMN IF NOT EXISTS description TEXT,
    ADD COLUMN IF NOT EXISTS meta_title VARCHAR(255),
    ADD COLUMN IF NOT EXISTS meta_description VARCHAR(500),
    ADD COLUMN IF NOT EXISTS seo_content TEXT,
    ADD COLUMN IF NOT EXISTS slug VARCHAR(300),
    ADD COLUMN IF NOT EXISTS image_crop_x NUMERIC(5,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS image_crop_y NUMERIC(5,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS image_crop_width NUMERIC(5,2) DEFAULT 100,
    ADD COLUMN IF NOT EXISTS image_crop_height NUMERIC(5,2) DEFAULT 100,
    ADD COLUMN IF NOT EXISTS image_zoom NUMERIC(4,2) DEFAULT 1.00;

UPDATE categories
SET image_crop_x = 0
WHERE image_crop_x IS NULL;

UPDATE categories
SET image_crop_y = 0
WHERE image_crop_y IS NULL;

UPDATE categories
SET image_crop_width = 100
WHERE image_crop_width IS NULL;

UPDATE categories
SET image_crop_height = 100
WHERE image_crop_height IS NULL;

UPDATE categories
SET image_zoom = 1
WHERE image_zoom IS NULL;

DO $$
DECLARE
    rec RECORD;
    fallback_slug TEXT;
    suffix_num INTEGER;
BEGIN
    UPDATE categories
    SET slug = 'may-pha-ca-phe'
    WHERE id = 'MAY_PHA'
      AND (slug IS NULL OR btrim(slug) = '');

    UPDATE categories
    SET slug = 'may-xay-ca-phe'
    WHERE id = 'MAY_XAY'
      AND (slug IS NULL OR btrim(slug) = '');

    UPDATE categories
    SET slug = 'may-xay-may-ep'
    WHERE id = 'MAY_EP'
      AND (slug IS NULL OR btrim(slug) = '');

    UPDATE categories
    SET slug = 'ca-phe-do-an-vat'
    WHERE id = 'CA_PHE'
      AND (slug IS NULL OR btrim(slug) = '');

    UPDATE categories
    SET slug = 'dung-cu-pha-che'
    WHERE id = 'DUNG_CU'
      AND (slug IS NULL OR btrim(slug) = '');

    FOR rec IN
        SELECT id
        FROM categories
        WHERE slug IS NULL OR btrim(slug) = ''
        ORDER BY id
    LOOP
        fallback_slug := lower(replace(COALESCE(rec.id, 'category'), '_', '-'));
        IF fallback_slug IS NULL OR fallback_slug = '' THEN
            fallback_slug := 'category';
        END IF;
        suffix_num := 2;

        WHILE EXISTS (
            SELECT 1
            FROM categories c2
            WHERE c2.slug = fallback_slug
              AND c2.id <> rec.id
        ) LOOP
            fallback_slug := lower(replace(COALESCE(rec.id, 'category'), '_', '-')) || '-' || suffix_num;
            suffix_num := suffix_num + 1;
        END LOOP;

        UPDATE categories
        SET slug = fallback_slug
        WHERE id = rec.id
          AND (slug IS NULL OR btrim(slug) = '');
    END LOOP;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS ux_categories_slug
    ON categories (slug)
    WHERE slug IS NOT NULL;

-- ============================================================
-- NEWS
-- ============================================================

ALTER TABLE IF EXISTS news
    ADD COLUMN IF NOT EXISTS slug VARCHAR(255),
    ADD COLUMN IF NOT EXISTS meta_title VARCHAR(255),
    ADD COLUMN IF NOT EXISTS meta_description VARCHAR(320),
    ADD COLUMN IF NOT EXISTS summary TEXT;

UPDATE news
SET slug = 'tin-tuc-' || id
WHERE slug IS NULL OR btrim(slug) = '';

CREATE UNIQUE INDEX IF NOT EXISTS ux_news_slug
    ON news (slug)
    WHERE slug IS NOT NULL;

-- ============================================================
-- BANNER IMAGE
-- ============================================================

ALTER TABLE IF EXISTS banner_image
    ADD COLUMN IF NOT EXISTS headline VARCHAR(255),
    ADD COLUMN IF NOT EXISTS sub_headline VARCHAR(500),
    ADD COLUMN IF NOT EXISTS primary_cta_text VARCHAR(120),
    ADD COLUMN IF NOT EXISTS primary_cta_link VARCHAR(500),
    ADD COLUMN IF NOT EXISTS secondary_cta_text VARCHAR(120),
    ADD COLUMN IF NOT EXISTS secondary_cta_link VARCHAR(500),
    ADD COLUMN IF NOT EXISTS target_link VARCHAR(500),
    ADD COLUMN IF NOT EXISTS display_mode VARCHAR(30) DEFAULT 'IMAGE_ONLY',
    ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS image_crop_x NUMERIC(5,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS image_crop_y NUMERIC(5,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS image_crop_width NUMERIC(5,2) DEFAULT 100,
    ADD COLUMN IF NOT EXISTS image_crop_height NUMERIC(5,2) DEFAULT 100,
    ADD COLUMN IF NOT EXISTS image_zoom NUMERIC(4,2) DEFAULT 1.00;

UPDATE banner_image
SET display_mode = 'IMAGE_ONLY'
WHERE display_mode IS NULL OR btrim(display_mode) = '';

UPDATE banner_image
SET active = TRUE
WHERE active IS NULL;

UPDATE banner_image
SET image_crop_x = 0
WHERE image_crop_x IS NULL;

UPDATE banner_image
SET image_crop_y = 0
WHERE image_crop_y IS NULL;

UPDATE banner_image
SET image_crop_width = 100
WHERE image_crop_width IS NULL;

UPDATE banner_image
SET image_crop_height = 100
WHERE image_crop_height IS NULL;

UPDATE banner_image
SET image_zoom = 1
WHERE image_zoom IS NULL;

-- ============================================================
-- STORE FEEDBACKS
-- ============================================================

ALTER TABLE IF EXISTS store_feedbacks
    ADD COLUMN IF NOT EXISTS rating INTEGER,
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP;

ALTER TABLE store_feedbacks
    ALTER COLUMN fullname DROP NOT NULL;

UPDATE store_feedbacks
SET rating = COALESCE(rating, store_rating)
WHERE rating IS NULL;

-- Current public visibility truth for existing VPS feedback rows is unknown from repo-only inspection.
-- Safest additive backfill is PENDING; operator can manually promote intended public rows to APPROVED after review.
UPDATE store_feedbacks
SET status = 'PENDING'
WHERE status IS NULL OR btrim(status) = '';

CREATE INDEX IF NOT EXISTS idx_store_feedbacks_status
    ON store_feedbacks (status);

CREATE INDEX IF NOT EXISTS idx_store_feedbacks_approved_at
    ON store_feedbacks (approved_at DESC);

-- ============================================================
-- CONSULTATION REQUESTS
-- ============================================================

CREATE TABLE IF NOT EXISTS consultation_requests (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(100),
    contact_phone VARCHAR(30) NOT NULL,
    need_type VARCHAR(30) NOT NULL,
    interest VARCHAR(40) NOT NULL,
    budget VARCHAR(30) NOT NULL,
    note TEXT,
    source VARCHAR(50),
    product_name VARCHAR(255),
    product_url TEXT,
    page_title VARCHAR(255),
    page_url TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_consultation_requests_status
    ON consultation_requests (status);

CREATE INDEX IF NOT EXISTS idx_consultation_requests_created_at
    ON consultation_requests (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_consultation_requests_updated_at
    ON consultation_requests (updated_at DESC);

-- ============================================================
-- CART ITEMS
-- ============================================================

CREATE TABLE IF NOT EXISTS cart_items (
    id BIGSERIAL PRIMARY KEY,
    cart_key VARCHAR(255) NOT NULL,
    product_id INTEGER NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    price_snapshot NUMERIC(18,2) NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    product_image VARCHAR(500),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_cart_items_cart_key_product_id
    ON cart_items (cart_key, product_id);

CREATE INDEX IF NOT EXISTS idx_cart_items_cart_key
    ON cart_items (cart_key);

CREATE INDEX IF NOT EXISTS idx_cart_items_updated_at
    ON cart_items (updated_at DESC);

-- ============================================================
-- VISITOR STATS
-- ============================================================

CREATE TABLE IF NOT EXISTS visitor_stats (
    stat_date DATE PRIMARY KEY,
    unique_visitors INTEGER NOT NULL DEFAULT 0,
    total_visits BIGINT NOT NULL DEFAULT 0,
    online_peak INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_visitor_stats_stat_date
    ON visitor_stats (stat_date DESC);

INSERT INTO visitor_stats (stat_date, unique_visitors, total_visits, online_peak)
VALUES (CURRENT_DATE, 0, 0, 0)
ON CONFLICT (stat_date) DO NOTHING;

COMMIT;
