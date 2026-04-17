-- Create store_feedbacks table for customer feedback system
CREATE TABLE IF NOT EXISTS store_feedbacks (
    id BIGSERIAL PRIMARY KEY,
    branch VARCHAR(10) NOT NULL,
    fullname VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    comment TEXT,
    store_rating INTEGER NOT NULL CHECK (store_rating >= 1 AND store_rating <= 5),
    staff_rating INTEGER NOT NULL CHECK (staff_rating >= 1 AND staff_rating <= 5),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for faster queries
CREATE INDEX idx_store_feedbacks_branch ON store_feedbacks(branch);
CREATE INDEX idx_store_feedbacks_created_at ON store_feedbacks(created_at DESC);
