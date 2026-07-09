ALTER TABLE categories
ADD COLUMN IF NOT EXISTS slug VARCHAR(160),
ADD COLUMN IF NOT EXISTS description TEXT,
ADD COLUMN IF NOT EXISTS meta_title VARCHAR(255),
ADD COLUMN IF NOT EXISTS meta_description VARCHAR(320),
ADD COLUMN IF NOT EXISTS seo_content TEXT;

UPDATE categories
SET slug = CASE id
    WHEN 'MAY_PHA' THEN 'may-pha-ca-phe'
    WHEN 'MAY_XAY' THEN 'may-xay-ca-phe'
    WHEN 'PHU_KIEN' THEN 'phu-kien-ca-phe'
    WHEN 'CA_PHE' THEN 'ca-phe'
    WHEN 'DO_AN_VAT' THEN 'do-an-vat'
    WHEN 'MAY_EP' THEN 'may-ep'
    WHEN 'CF_AN_VAT' THEN 'ca-phe-do-an-vat'
    WHEN 'DUNG_CU' THEN 'dung-cu-pha-che'
    WHEN 'HANG_CU' THEN 'hang-cu-ca-phe'
    WHEN 'XAY_EP' THEN 'may-xay-may-ep'
    ELSE lower(replace(trim(name), ' ', '-'))
END
WHERE slug IS NULL OR trim(slug) = '';

CREATE UNIQUE INDEX IF NOT EXISTS ux_categories_slug
ON categories(slug);


SELECT id, name, slug, description, meta_title, meta_description, seo_content
FROM categories
ORDER BY id;