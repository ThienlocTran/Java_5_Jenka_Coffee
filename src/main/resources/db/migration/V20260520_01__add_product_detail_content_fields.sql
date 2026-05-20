ALTER TABLE IF EXISTS products
    ADD COLUMN IF NOT EXISTS short_description TEXT,
    ADD COLUMN IF NOT EXISTS detail_description TEXT,
    ADD COLUMN IF NOT EXISTS specifications_json TEXT,
    ADD COLUMN IF NOT EXISTS features_json TEXT,
    ADD COLUMN IF NOT EXISTS warranty_info TEXT,
    ADD COLUMN IF NOT EXISTS shipping_info TEXT,
    ADD COLUMN IF NOT EXISTS suitable_for TEXT,
    ADD COLUMN IF NOT EXISTS faq_json TEXT,
    ADD COLUMN IF NOT EXISTS meta_title VARCHAR(255),
    ADD COLUMN IF NOT EXISTS meta_description VARCHAR(320);
