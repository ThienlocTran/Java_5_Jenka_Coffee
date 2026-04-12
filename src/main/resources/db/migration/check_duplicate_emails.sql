-- Check for duplicate emails before migration
-- This helps identify data issues

-- 1. Check empty string emails
SELECT 'Empty string emails' as issue_type, COUNT(*) as count
FROM Accounts 
WHERE Email = '' OR TRIM(Email) = '';

-- 2. Check NULL emails
SELECT 'NULL emails' as issue_type, COUNT(*) as count
FROM Accounts 
WHERE Email IS NULL;

-- 3. Check duplicate non-empty emails
SELECT 'Duplicate emails' as issue_type, Email, COUNT(*) as count
FROM Accounts 
WHERE Email IS NOT NULL AND Email != ''
GROUP BY Email
HAVING COUNT(*) > 1;

-- 4. Show all accounts with empty or NULL email
SELECT Username, Fullname, Email, phone
FROM Accounts 
WHERE Email IS NULL OR Email = '' OR TRIM(Email) = ''
ORDER BY Username;
