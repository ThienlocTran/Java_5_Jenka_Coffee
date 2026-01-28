-- ==================================================
-- MIGRATION SCRIPT: Thêm các bảng mới cho Jenka Coffee
-- Chỉ chạy script này NẾU các bảng chưa tồn tại
-- ==================================================

USE db_ac3c24_jenkacoffee;
GO

-- 1. VOUCHERS (nếu chưa tồn tại)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Vouchers')
BEGIN
    CREATE TABLE Vouchers (
        code VARCHAR(20) NOT NULL,
        discountAmount DECIMAL(18,2) NOT NULL,
        discountType VARCHAR(10) DEFAULT 'FIXED',
        minOrderAmount DECIMAL(18,2) NULL,
        expirationDate DATETIME NOT NULL,
        quantity INT DEFAULT 0,
        active BIT DEFAULT 1,
        PRIMARY KEY (code)
    );
    PRINT 'Table Vouchers created successfully';
END
ELSE
BEGIN
    PRINT 'Table Vouchers already exists';
END
GO

-- 2. PAYMENTS (nếu chưa tồn tại)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Payments')
BEGIN
    CREATE TABLE Payments (
        id BIGINT IDENTITY(1,1) NOT NULL,
        OrderId BIGINT NOT NULL,
        amount DECIMAL(18,2) NOT NULL,
        paymentMethod VARCHAR(20) NOT NULL,
        transactionCode VARCHAR(50) NULL,
        paymentDate DATETIME DEFAULT GETDATE(),
        status VARCHAR(20) DEFAULT 'PENDING',
        PRIMARY KEY (id),
        FOREIGN KEY (OrderId) REFERENCES Orders(id)
    );
    PRINT 'Table Payments created successfully';
END
ELSE
BEGIN
    PRINT 'Table Payments already exists';
END
GO

-- 3. SERVICE_BOOKINGS (nếu chưa tồn tại)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'ServiceBookings')
BEGIN
    CREATE TABLE ServiceBookings (
        id BIGINT IDENTITY(1,1) NOT NULL,
        username VARCHAR(50) NULL,
        customerName NVARCHAR(100) NOT NULL,
        phone VARCHAR(15) NOT NULL,
        description NVARCHAR(MAX) NOT NULL,
        bookingDate DATETIME NOT NULL,
        preferredTime NVARCHAR(50) NULL,
        status VARCHAR(20) DEFAULT 'PENDING',
        PRIMARY KEY (id),
        FOREIGN KEY (username) REFERENCES Accounts(username)
    );
    PRINT 'Table ServiceBookings created successfully';
END
ELSE
BEGIN
    PRINT 'Table ServiceBookings already exists';
END
GO

-- 4. POINT_HISTORY (nếu chưa tồn tại)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'PointHistory')
BEGIN
    CREATE TABLE PointHistory (
        id BIGINT IDENTITY(1,1) NOT NULL,
        username VARCHAR(50) NOT NULL,
        amount INT NOT NULL,
        OrderId BIGINT NULL,
        reason NVARCHAR(255) NOT NULL,
        createDate DATETIME DEFAULT GETDATE(),
        PRIMARY KEY (id),
        FOREIGN KEY (username) REFERENCES Accounts(username),
        FOREIGN KEY (OrderId) REFERENCES Orders(id)
    );
    PRINT 'Table PointHistory created successfully';
END
ELSE
BEGIN
    PRINT 'Table PointHistory already exists';
END
GO

-- 5. CẬP NHẬT BẢNG ACCOUNTS (thêm các cột mới nếu chưa có)
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Accounts') AND name = 'phone')
BEGIN
    ALTER TABLE Accounts ADD phone VARCHAR(15) NULL;
    PRINT 'Added column phone to Accounts';
END

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Accounts') AND name = 'phone_verified')
BEGIN
    ALTER TABLE Accounts ADD phone_verified BIT DEFAULT 0;
    PRINT 'Added column phone_verified to Accounts';
END

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Accounts') AND name = 'points')
BEGIN
    ALTER TABLE Accounts ADD points INT DEFAULT 0;
    PRINT 'Added column points to Accounts';
END

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Accounts') AND name = 'customer_rank')
BEGIN
    ALTER TABLE Accounts ADD customer_rank VARCHAR(20) DEFAULT 'MEMBER';
    PRINT 'Added column customer_rank to Accounts';
END
GO

-- 6. RENAME COLUMN password -> password_hash (nếu cần)
IF EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Accounts') AND name = 'Password')
BEGIN
    EXEC sp_rename 'Accounts.Password', 'password_hash', 'COLUMN';
    PRINT 'Renamed Password to password_hash in Accounts';
END
GO

-- 7. CẬP NHẬT BẢNG ORDERS (thêm các cột mới nếu chưa có)
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Orders') AND name = 'VoucherCode')
BEGIN
    ALTER TABLE Orders ADD VoucherCode VARCHAR(20) NULL;
    PRINT 'Added column VoucherCode to Orders';
END

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Orders') AND name = 'totalAmount')
BEGIN
    ALTER TABLE Orders ADD totalAmount DECIMAL(18,2) NULL;
    PRINT 'Added column totalAmount to Orders';
END
GO

-- 8. THÊM FOREIGN KEY cho Orders.VoucherCode (nếu chưa có)
IF NOT EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FK_Orders_Vouchers')
BEGIN
    ALTER TABLE Orders 
    ADD CONSTRAINT FK_Orders_Vouchers 
    FOREIGN KEY (VoucherCode) REFERENCES Vouchers(code);
    PRINT 'Added foreign key FK_Orders_Vouchers';
END
GO

-- 9. THÊM UNIQUE CONSTRAINT cho Accounts.phone (nếu chưa có)
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE object_id = OBJECT_ID('Accounts') AND name = 'UQ_Accounts_phone')
BEGIN
    ALTER TABLE Accounts ADD CONSTRAINT UQ_Accounts_phone UNIQUE (phone);
    PRINT 'Added unique constraint on Accounts.phone';
END
GO

PRINT '=== MIGRATION COMPLETED SUCCESSFULLY ===';
