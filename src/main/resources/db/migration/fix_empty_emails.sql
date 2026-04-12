-- ============================================
-- FIX: Duplicate Empty Email Constraint Violation
-- ============================================
-- Problem: PostgreSQL unique constraint allows multiple NULLs but NOT multiple empty strings
-- Solution: Remove NOT NULL constraint + Convert empty strings to NULL
-- ============================================

-- STEP 1: Check current state (optional, for logging)
DO $$
DECLARE
    empty_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO empty_count
    FROM Accounts 
    WHERE Email = '' OR TRIM(Email) = '';
    
    RAISE NOTICE 'Found % accounts with empty email', empty_count;
END $$;

-- STEP 2: Remove NOT NULL constraint from Email column
-- This allows email to be NULL (optional field)
ALTER TABLE Accounts 
ALTER COLUMN Email DROP NOT NULL;

-- STEP 3: Convert all empty string emails to NULL
-- This fixes the unique constraint violation
UPDATE Accounts 
SET Email = NULL 
WHERE Email = '' OR TRIM(Email) = '';

-- STEP 4: Verify the fix
DO $$
DECLARE
    null_count INTEGER;
    duplicate_count INTEGER;
BEGIN
    -- Count NULL emails
    SELECT COUNT(*) INTO null_count
    FROM Accounts 
    WHERE Email IS NULL;
    
    RAISE NOTICE 'Accounts with NULL email: %', null_count;
    
    -- Check for duplicate non-NULL emails
    SELECT COUNT(*) INTO duplicate_count
    FROM (
        SELECT Email, COUNT(*) as cnt
        FROM Accounts 
        WHERE Email IS NOT NULL
        GROUP BY Email
        HAVING COUNT(*) > 1
    ) duplicates;
    
    IF duplicate_count > 0 THEN
        RAISE WARNING 'Found % duplicate non-NULL emails!', duplicate_count;
    ELSE
        RAISE NOTICE 'No duplicate emails found. Migration successful!';
    END IF;
END $$;
