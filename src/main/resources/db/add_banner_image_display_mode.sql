ALTER TABLE banner_image
ADD COLUMN IF NOT EXISTS display_mode VARCHAR(30) DEFAULT 'IMAGE_ONLY';


SELECT id, name, slug, description, meta_title, meta_description, seo_content
FROM categories
ORDER BY id;

SELECT id, name, category_id, category_id
FROM products
ORDER BY id;