BEGIN;

ALTER TABLE "categories"
    ADD COLUMN IF NOT EXISTS image_crop_x NUMERIC(5,2) NOT NULL DEFAULT 0;

ALTER TABLE "categories"
    ADD COLUMN IF NOT EXISTS image_crop_y NUMERIC(5,2) NOT NULL DEFAULT 0;

ALTER TABLE "categories"
    ADD COLUMN IF NOT EXISTS image_crop_width NUMERIC(5,2) NOT NULL DEFAULT 100;

ALTER TABLE "categories"
    ADD COLUMN IF NOT EXISTS image_crop_height NUMERIC(5,2) NOT NULL DEFAULT 100;

ALTER TABLE "categories"
    ADD COLUMN IF NOT EXISTS image_zoom NUMERIC(4,2) NOT NULL DEFAULT 1.00;

COMMIT;



ROLLBACK;

SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
ORDER BY table_name;