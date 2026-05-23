-- Migration: Add SEO text fields to categories table
-- Safe to run multiple times (IF NOT EXISTS).
-- Run on VPS before deploying updated backend.

ALTER TABLE categories
    ADD COLUMN IF NOT EXISTS description TEXT;

ALTER TABLE categories
    ADD COLUMN IF NOT EXISTS meta_title VARCHAR(255);

ALTER TABLE categories
    ADD COLUMN IF NOT EXISTS meta_description VARCHAR(500);

ALTER TABLE categories
    ADD COLUMN IF NOT EXISTS seo_content TEXT;

-- Verify
SELECT id, name, slug, description, meta_title, meta_description, seo_content
FROM categories
WHERE id = 'MAY_XAY_CA_PHE';
