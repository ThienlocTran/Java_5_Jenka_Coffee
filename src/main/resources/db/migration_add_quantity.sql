-- ==========================================
-- SQL MIGRATION: Add Quantity Column to Products
-- UPDATED THRESHOLD: 10 products for LOW_STOCK
-- ==========================================

-- STEP 1: Add Quantity column (if not exists)
IF NOT EXISTS (
    SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_NAME = 'Products' AND COLUMN_NAME = 'Quantity'
)
BEGIN
    ALTER TABLE Products 
    ADD Quantity INT NOT NULL DEFAULT 0;
    
    PRINT 'Column Quantity added successfully!';
END
ELSE
BEGIN
    PRINT 'Column Quantity already exists, skipping...';
END
GO

-- ==========================================
-- STEP 2: Populate Initial Stock Values
-- ==========================================

-- Option 1: Set same quantity for all products
UPDATE Products 
SET Quantity = 50 
WHERE Quantity = 0;

-- Option 2: Set quantity by category
-- UPDATE Products 
-- SET Quantity = 100 
-- WHERE Categoryid = 'MAY_PHA' AND Quantity = 0;

-- UPDATE Products 
-- SET Quantity = 200 
-- WHERE Categoryid = 'PHU_KIEN' AND Quantity = 0;

GO

-- ==========================================
-- STEP 3: Verify Changes (Updated Threshold)
-- ==========================================

SELECT 
    p.Id,
    p.Name,
    p.Quantity,
    c.Name as CategoryName,
    CASE 
        WHEN p.Quantity = 0 THEN 'OUT_OF_STOCK'
        WHEN p.Quantity < 10 THEN 'LOW_STOCK'
        ELSE 'IN_STOCK'
    END as StockStatus
FROM Products p
LEFT JOIN Categories c ON p.Categoryid = c.Id
ORDER BY p.Quantity ASC;

GO

-- ==========================================
-- STEP 4: Create View for Admin Dashboard
-- ==========================================

IF OBJECT_ID('vw_LowStockProducts', 'V') IS NOT NULL
    DROP VIEW vw_LowStockProducts;
GO

CREATE VIEW vw_LowStockProducts AS
SELECT 
    p.Id,
    p.Name,
    p.Quantity,
    p.price,
    c.Name as CategoryName,
    CASE 
        WHEN p.Quantity = 0 THEN 'Hết hàng'
        WHEN p.Quantity < 10 THEN 'Sắp hết (' + CAST(p.Quantity AS VARCHAR) + ' sản phẩm)'
        ELSE 'Còn đủ'
    END as StockStatusText
FROM Products p
LEFT JOIN Categories c ON p.Categoryid = c.Id
WHERE p.Quantity < 10  -- Changed from 5 to 10
AND p.Available = 1;
GO

-- Query to check low stock alerts for admin
SELECT * FROM vw_LowStockProducts ORDER BY Quantity ASC;

GO

-- ==========================================
-- OPTIONAL: Add constraint to prevent negative quantity
-- ==========================================

ALTER TABLE Products 
ADD CONSTRAINT CK_Products_Quantity_NonNegative 
CHECK (Quantity >= 0);

PRINT 'Migration completed successfully!';
PRINT 'LOW_STOCK threshold set to: < 10 products';
