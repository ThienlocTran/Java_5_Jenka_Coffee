-- V18: Add featured products support
-- Add isFeatured column to Products table for highlighting premium/high-margin products

ALTER TABLE Products ADD COLUMN IF NOT EXISTS isFeatured BOOLEAN DEFAULT FALSE;

-- Create index for better query performance when filtering featured products
CREATE INDEX IF NOT EXISTS idx_products_featured ON Products(isFeatured) WHERE isFeatured = TRUE;

-- Optional: Mark some existing products as featured (example)
-- UPDATE Products SET isFeatured = TRUE WHERE id IN (1, 2, 3);

COMMENT ON COLUMN Products.isFeatured IS 'Flag to mark products as featured/highlighted on homepage';
