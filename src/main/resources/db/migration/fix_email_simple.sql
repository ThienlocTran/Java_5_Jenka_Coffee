-- Simple one-liner fix for production
-- Run this if the detailed script has issues

-- Remove NOT NULL constraint
ALTER TABLE Accounts ALTER COLUMN Email DROP NOT NULL;

-- Convert empty emails to NULL
UPDATE Accounts SET Email = NULL WHERE Email = '' OR TRIM(Email) = '';
