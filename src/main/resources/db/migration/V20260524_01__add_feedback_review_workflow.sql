ALTER TABLE IF EXISTS store_feedbacks
    ADD COLUMN IF NOT EXISTS branch VARCHAR(100),
    ADD COLUMN IF NOT EXISTS rating INTEGER,
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP;

UPDATE store_feedbacks
SET rating = COALESCE(rating, store_rating, staff_rating)
WHERE rating IS NULL;

CREATE INDEX IF NOT EXISTS idx_store_feedbacks_status
    ON store_feedbacks (status);

CREATE INDEX IF NOT EXISTS idx_store_feedbacks_approved_at
    ON store_feedbacks (approved_at DESC);

SELECT column_name, data_type
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name = 'store_feedbacks'
  AND column_name IN ('branch', 'rating', 'status', 'approved_at')
ORDER BY column_name;
