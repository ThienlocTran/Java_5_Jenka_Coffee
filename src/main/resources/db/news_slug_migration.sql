CREATE EXTENSION IF NOT EXISTS unaccent;

ALTER TABLE news
ADD COLUMN IF NOT EXISTS slug VARCHAR(255);

DO $$
DECLARE
    rec RECORD;
    base_slug TEXT;
    candidate_slug TEXT;
    suffix INT;
BEGIN
    FOR rec IN
        SELECT id, title
        FROM news
        WHERE slug IS NULL OR btrim(slug) = ''
        ORDER BY id
    LOOP
        base_slug := lower(
            regexp_replace(
                unaccent(replace(replace(COALESCE(rec.title, ''), 'Đ', 'D'), 'đ', 'd')),
                '[^a-zA-Z0-9]+',
                '-',
                'g'
            )
        );
        base_slug := regexp_replace(base_slug, '-+', '-', 'g');
        base_slug := regexp_replace(base_slug, '(^-|-$)', '', 'g');

        IF base_slug IS NULL OR base_slug = '' THEN
            base_slug := 'news';
        END IF;

        candidate_slug := base_slug;
        suffix := 2;

        WHILE EXISTS (
            SELECT 1
            FROM news
            WHERE lower(slug) = lower(candidate_slug)
              AND id <> rec.id
        ) LOOP
            candidate_slug := base_slug || '-' || suffix;
            suffix := suffix + 1;
        END LOOP;

        UPDATE news
        SET slug = candidate_slug
        WHERE id = rec.id;
    END LOOP;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS ux_news_slug
ON news (lower(slug))
WHERE slug IS NOT NULL;
