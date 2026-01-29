/* =====================================================
   MIGRATION SCRIPT - FIX PASSWORD_HASH COLUMN LENGTH
   ISSUE: BCrypt hash needs 60 chars, current column too short
   ===================================================== */

USE db_ac3c24_jenkacoffee;
GO

-- Bước 1: Kiểm tra độ dài hiện tại
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    CHARACTER_MAXIMUM_LENGTH,
    IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'Accounts' 
  AND COLUMN_NAME = 'password_hash';
GO

-- Bước 2: Alter column để mở rộng
-- Nếu column hiện tại là VARCHAR(32) hoặc nhỏ hơn 255, cần alter
ALTER TABLE Accounts
ALTER COLUMN password_hash VARCHAR(255) NOT NULL;
GO

-- Bước 3: Verify thay đổi
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    CHARACTER_MAXIMUM_LENGTH,
    IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'Accounts' 
  AND COLUMN_NAME = 'password_hash';
GO

-- Bước 4: Update lại admin password với BCrypt hash đầy đủ (60 chars)
-- Hash của password "123" với BCrypt strength 12
UPDATE Accounts
SET password_hash = '$2a$12$1Dc2pPqXS8CWzpBSnNxKq.0/ybyidjzVt705o7K.pUK..SQrSOx9y'
WHERE username = 'admin';
GO

-- Verify admin password đã đúng
SELECT 
    username,
    fullname,
    LEN(password_hash) as hash_length,
    LEFT(password_hash, 10) as hash_prefix,
    activated,
    admin
FROM Accounts
WHERE username = 'admin';
GO

PRINT 'Migration completed! Password hash column now supports BCrypt (255 chars)';
