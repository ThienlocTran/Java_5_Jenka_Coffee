-- Migration Script: Update admin password to BCrypt hash
-- Password: 123
-- BCrypt hash: $2a$12$LQv3c1yqBWVHxkd0LHAkCOdkTsPwXIrQwZL5wFKJ/M1MvFGlVJ3z2
Password: 123 BCrypt Hash: $2a$12$aohCYb53pJStseMIYSU0B.6Iu02p35SjZrEgFt9.9UoFZgG2c2Xwy Length: 60 SQL Update: UPDATE Accounts SET password_hash = N'$2a$12$aohCYb53pJStseMIYSU0B.6Iu02p35SjZrEgFt9.9UoFZgG2c2Xwy' WHERE username = 'admin';
-- IMPORTANT: Use NVARCHAR and escape $ properly
UPDATE Accounts 
SET password_hash = N'$2a$12$LQv3c1yqBWVHxkd0LHAkCOdkTsPwXIrQwZL5wFKJ/M1MvFGlVJ3z2'
WHERE username = 'admin';

-- Verify the update
SELECT 
    username, 
    password_hash,
    LEN(password_hash) as hash_length,
    activated,
    admin
FROM Accounts 
WHERE username = 'admin';

PRINT 'Admin password updated to BCrypt hash (password: 123)';
