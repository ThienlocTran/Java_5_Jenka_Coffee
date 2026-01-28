-- Migration Script: Hash existing plain-text passwords with BCrypt
-- Execute this BEFORE deploying BCrypt changes

-- IMPORTANT: This script is for DEMO/DEVELOPMENT only
-- In production, you should:
-- 1. Force users to reset passwords
-- 2. OR migrate gradually with a flag field
-- 3. NEVER store plain text passwords

-- For demo accounts, update to BCrypt hashed versions
-- Password: "123" → BCrypt hash (strength 12)

-- Admin account (username: admin, password: 123)
UPDATE Accounts 
SET password_hash = '$2a$12$LQv3c1yqBWVHxkd0LHAkCOdkTsPwXIrQwZL5wFKJ/M1MvFGlVJ3z2'
WHERE username = 'admin';

-- User account (username: user, password: 123)
UPDATE Accounts 
SET password_hash = '$2a$12$LQv3c1yqBWVHxkd0LHAkCOdkTsPwXIrQwZL5wFKJ/M1MvFGlVJ3z2'
WHERE username = 'user';

-- NOTES:
-- The hash above is for password "123"
-- Generated with BCryptPasswordEncoder(12)
-- All users with password "123" will use the same hash (this is normal for BCrypt)
--
-- If you have other accounts with different passwords, you need to:
-- 1. Generate BCrypt hash for each password
-- 2. Update each account individually
-- OR better: implement a password reset flow

PRINT 'Migration completed. All demo accounts now use BCrypt hashed passwords.';
