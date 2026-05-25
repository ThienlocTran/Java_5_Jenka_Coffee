-- Migration: Add home addon fields to products table
-- Non-destructive, additive only

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS is_home_addon boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS home_addon_position integer;

-- Optional index for fast homepage query
CREATE INDEX IF NOT EXISTS idx_products_home_addon ON products (is_home_addon, available, home_addon_position);
