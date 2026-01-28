/* ==================================================
   SCRIPT DATABASE JENKA COFFEE - V2 (FINAL)
   UPDATED: 2026-01-27
   NOTE: Đã chuyển FLOAT -> DECIMAL, Thêm ràng buộc
   ================================================== */

USE db_ac3c24_jenkacoffee;
GO

/* --- PHẦN 0: DROP TABLES --- */
IF OBJECT_ID('dbo.PointHistory', 'U') IS NOT NULL DROP TABLE dbo.PointHistory;
IF OBJECT_ID('dbo.ServiceBookings', 'U') IS NOT NULL DROP TABLE dbo.ServiceBookings;
IF OBJECT_ID('dbo.Payments', 'U') IS NOT NULL DROP TABLE dbo.Payments;
IF OBJECT_ID('dbo.OrderDetails', 'U') IS NOT NULL DROP TABLE dbo.OrderDetails;
IF OBJECT_ID('dbo.Orders', 'U') IS NOT NULL DROP TABLE dbo.Orders;
IF OBJECT_ID('dbo.Vouchers', 'U') IS NOT NULL DROP TABLE dbo.Vouchers;
IF OBJECT_ID('dbo.Products', 'U') IS NOT NULL DROP TABLE dbo.Products;
IF OBJECT_ID('dbo.Categories', 'U') IS NOT NULL DROP TABLE dbo.Categories;
IF OBJECT_ID('dbo.Accounts', 'U') IS NOT NULL DROP TABLE dbo.Accounts;
GO

/* --- PHẦN 1: TẠO BẢNG --- */

-- 1. ACCOUNTS
CREATE TABLE Accounts (
                          username VARCHAR(50) NOT NULL,
                          password_hash VARCHAR(255) NOT NULL, -- Đổi tên cột, lưu chuỗi BCrypt
                          fullname NVARCHAR(100) NOT NULL,
                          email VARCHAR(100) NOT NULL UNIQUE -- Thêm ràng buộc duy nhất
                            ,phone VARCHAR(15) NULL UNIQUE ,
                          phone_verified BIT DEFAULT 0,-- sđt đã xác thực chưa?
                          photo NVARCHAR(255) NULL,
                          activated BIT DEFAULT 1,
                          admin BIT DEFAULT 0,
                          points INT DEFAULT 0,
    -- Rank: MEMBER, SILVER, GOLD, DIAMOND
                          customer_rank VARCHAR(20) DEFAULT 'MEMBER',
                          PRIMARY KEY (username)
);
GO

-- 2. CATEGORIES
CREATE TABLE Categories (
                            id VARCHAR(50) NOT NULL,
                            name NVARCHAR(100) NOT NULL,
                            PRIMARY KEY (id)
);
GO

-- 3. PRODUCTS
CREATE TABLE Products (
                          id INT IDENTITY(1,1) NOT NULL,
                          name NVARCHAR(200) NOT NULL,
                          image NVARCHAR(255) NULL,
                          price DECIMAL(18,2) NOT NULL CHECK (price >= 0), -- Dùng DECIMAL chuẩn tiền tệ
                          createDate DATETIME DEFAULT GETDATE(), -- Lấy cả giờ
                          available BIT DEFAULT 1,
                          CategoryId VARCHAR(50) NOT NULL,
                          description NVARCHAR(MAX) NULL,
                          PRIMARY KEY (id),
                          FOREIGN KEY (CategoryId) REFERENCES Categories(id)
);
GO

-- 4. VOUCHERS
CREATE TABLE Vouchers (
                          code VARCHAR(20) NOT NULL,
                          discountAmount DECIMAL(18,2) NOT NULL, -- Giảm bao nhiêu tiền/phần trăm
                          discountType VARCHAR(10) DEFAULT 'FIXED', -- 'FIXED' (Trừ tiền) hoặc 'PERCENT' (%)
                          minOrderAmount DECIMAL(18,2) NULL, -- Đơn tối thiểu để áp dụng
                          expirationDate DATETIME NOT NULL,
                          quantity INT DEFAULT 0,
                          active BIT DEFAULT 1,
                          PRIMARY KEY (code)
);
GO

-- 5. ORDERS
CREATE TABLE Orders (
                        id BIGINT IDENTITY(1,1) NOT NULL,
                        username VARCHAR(50) NOT NULL,
                        createDate DATETIME DEFAULT GETDATE(),
                        address NVARCHAR(255) NOT NULL,
                        phone VARCHAR(15) NOT NULL,
    -- Status: 0=NEW, 1=CONFIRMED, 2=SHIPPING, 3=CANCELLED, 4=COMPLETED
                        status INT DEFAULT 0,
                        VoucherCode VARCHAR(20) NULL,
                        totalAmount DECIMAL(18,2) NULL, -- Tổng tiền sau khi giảm
                        PRIMARY KEY (id),
                        FOREIGN KEY (username) REFERENCES Accounts(username),
                        FOREIGN KEY (VoucherCode) REFERENCES Vouchers(code)
);
GO

-- 6. ORDER_DETAILS
CREATE TABLE OrderDetails (
                              id BIGINT IDENTITY(1,1) NOT NULL,
                              OrderId BIGINT NOT NULL,
                              ProductId INT NOT NULL,
                              price DECIMAL(18,2) NOT NULL, -- Giá tại thời điểm mua
                              quantity INT NOT NULL,
                              PRIMARY KEY (id),
                              FOREIGN KEY (OrderId) REFERENCES Orders(id),
                              FOREIGN KEY (ProductId) REFERENCES Products(id)
);
GO

-- 7. PAYMENTS
CREATE TABLE Payments (
                          id BIGINT IDENTITY(1,1) NOT NULL,
                          OrderId BIGINT NOT NULL,
                          amount DECIMAL(18,2) NOT NULL,
                          paymentMethod VARCHAR(20) NOT NULL, -- COD, VNPAY, MOMO
                          transactionCode VARCHAR(50) NULL,
                          paymentDate DATETIME DEFAULT GETDATE(),
                          status VARCHAR(20) DEFAULT 'PENDING',
                          PRIMARY KEY (id),
                          FOREIGN KEY (OrderId) REFERENCES Orders(id)
);
GO

-- 8. SERVICE_BOOKINGS
CREATE TABLE ServiceBookings (
                                 id BIGINT IDENTITY(1,1) NOT NULL,
                                 username VARCHAR(50) NULL,
                                 customerName NVARCHAR(100) NOT NULL,
                                 phone VARCHAR(15) NOT NULL,
                                 description NVARCHAR(MAX) NOT NULL,
                                 bookingDate DATETIME NOT NULL,
                                 preferredTime NVARCHAR(50) NULL, -- Khung giờ mong muốn (Sáng/Chiều)
                                 status VARCHAR(20) DEFAULT 'PENDING',
                                 PRIMARY KEY (id),
                                 FOREIGN KEY (username) REFERENCES Accounts(username)
);
GO

-- 9. POINT_HISTORY
CREATE TABLE PointHistory (
                              id BIGINT IDENTITY(1,1) NOT NULL,
                              username VARCHAR(50) NOT NULL,
                              amount INT NOT NULL,
                              OrderId BIGINT NULL, -- Trace từ đơn hàng nào (Có thể Null nếu là điểm thưởng sự kiện)
                              reason NVARCHAR(255) NOT NULL,
                              createDate DATETIME DEFAULT GETDATE(),
                              PRIMARY KEY (id),
                              FOREIGN KEY (username) REFERENCES Accounts(username),
                              FOREIGN KEY (OrderId) REFERENCES Orders(id)
);
GO


-- 1. INSERT ACCOUNTS
-- Mật khẩu '123' đã được hash: $2a$12$s.7q.7q.7q.7q.7q.7q.7q.7q.7q.7q.7q.7q.7q.7q.7q (Ví dụ tượng trưng)
-- Lưu ý: Bạn nên dùng code Java để tạo hash mới nếu cần
INSERT INTO Accounts (username, password_hash, fullname, email, phone, phone_verified, admin, points, customer_rank, photo, activated) VALUES
                                                                                                                                           ('admin', '$2a$10$cw.T.w.e.r.t.y.u.i.o.p.1.2.3.4.5.6.7.8.9.0.a.b.c.d', N'Trần Thiên Lộc (Leader)', 'loctt@fpt.edu.vn', '0909123456', 1, 1, 1500, 'DIAMOND', 'admin.jpg', 1),
                                                                                                                                           ('staff', '$2a$10$cw.T.w.e.r.t.y.u.i.o.p.1.2.3.4.5.6.7.8.9.0.a.b.c.d', N'Nhân Viên Tư Vấn', 'staff@jenka.com', '0909888999', 1, 1, 0, 'MEMBER', 'staff.jpg', 1),
                                                                                                                                           ('user',  '$2a$10$cw.T.w.e.r.t.y.u.i.o.p.1.2.3.4.5.6.7.8.9.0.a.b.c.d', N'Nguyễn Văn Khách', 'khachhang@gmail.com', '0123456789', 0, 0, 200, 'GOLD', 'user.jpg', 1);
GO

-- 2. INSERT CATEGORIES
INSERT INTO Categories (id, name) VALUES
                                      ('MAY_PHA', N'Máy Pha Cà Phê'),
                                      ('MAY_XAY', N'Máy Xay Cà Phê'),
                                      ('XAY_EP', N'Máy Xay & Máy Ép'),
                                      ('CF_AN_VAT', N'Cà Phê & Đồ Ăn Vặt'),
                                      ('DUNG_CU', N'Dụng Cụ Pha Chế'),
                                      ('HANG_CU', N'Máy Cũ Lướt (Second Hand)');
GO

-- 3. INSERT PRODUCTS
INSERT INTO Products (name, price, image, CategoryId, createDate, description, available) VALUES
-- Máy Pha
(N'Máy Pha Cà Phê Breville 870 XL', 18500000, 'may_pha_01.jpg', 'MAY_PHA', '2025-11-01', N'Nhập khẩu Úc, tích hợp máy xay, áp suất 15 bar.', 1),
(N'Máy Pha Nuova Simonelli Appia II', 65000000, 'may_pha_02.jpg', 'MAY_PHA', '2025-11-02', N'Chuyên nghiệp 2 group dành cho quán lớn.', 1),
(N'Máy Pha Casadio Undici A1', 42000000, 'may_pha_03.jpg', 'MAY_PHA', '2025-11-03', N'Nồi hơi lớn, công suất mạnh mẽ, bền bỉ.', 1),

-- Máy Xay
(N'Máy Xay Eureka Mignon', 8900000, 'may_xay_01.jpg', 'MAY_XAY', '2025-12-01', N'Thiết kế nhỏ gọn, độ ồn thấp, xay Espresso chuẩn.', 1),
(N'Máy Xay Fiorenzato F64E', 18000000, 'may_xay_02.jpg', 'MAY_XAY', '2025-12-02', N'Màn hình cảm ứng, tốc độ xay cực nhanh.', 1),

-- Xay & Ép
(N'Máy Ép Chậm Hurom H200', 9500000, 'xay_ep_01.jpg', 'XAY_EP', '2026-01-05', N'Giữ nguyên vitamin, ép kiệt bã, dễ vệ sinh.', 1),
(N'Máy Xay Vitamix The Quiet One', 12500000, 'xay_ep_02.jpg', 'XAY_EP', '2026-01-06', N'Chuyên xay đá bi, sinh tố, độ ồn cực thấp.', 1),

-- Cafe & Ăn Vặt
(N'Cà Phê Hạt Arabica Cầu Đất (500g)', 250000, 'cf_01.jpg', 'CF_AN_VAT', '2026-01-10', N'Hương thơm quyến rũ, vị chua thanh nhẹ.', 1),
(N'Cà Phê Robusta Honey (1kg)', 320000, 'cf_02.jpg', 'CF_AN_VAT', '2026-01-11', N'Đậm đà, hậu vị ngọt sâu, hàm lượng cafein cao.', 1),
(N'Bánh Biscotti Nguyên Cám (Hũ)', 85000, 'an_vat_01.jpg', 'CF_AN_VAT', '2026-01-12', N'Ăn kiêng, healthy, phù hợp uống kèm cafe.', 1),

-- Dụng Cụ
(N'Bộ Shaker Inox 500ml', 150000, 'dung_cu_01.jpg', 'DUNG_CU', '2026-01-13', N'Thép không gỉ 304, chuyên dùng cho Bartender.', 1),
(N'Ca Đánh Sữa Latte Art 600ml', 250000, 'dung_cu_02.jpg', 'DUNG_CU', '2026-01-13', N'Mũi nhọn dễ tạo hình, vạch chia dung tích.', 1),

-- Hàng Cũ
(N'Máy Pha Cũ Expobar (95%)', 25000000, 'cu_luot_01.jpg', 'HANG_CU', '2026-01-01', N'Thanh lý quán nghỉ bán, bảo hành 3 tháng.', 1),
(N'Máy Xay Cũ Mazzer Super Jolly', 9000000, 'cu_luot_02.jpg', 'HANG_CU', '2026-01-02', N'Lưỡi dao còn bén, motor chạy êm ru.', 1);
GO

-- 4. INSERT VOUCHERS
INSERT INTO Vouchers (code, discountAmount, discountType, minOrderAmount, expirationDate, quantity, active) VALUES
                                                                                                                ('JENKA50', 50000, 'FIXED', 500000, '2026-12-31', 100, 1), -- Giảm 50k cho đơn từ 500k
                                                                                                                ('VIP10', 10, 'PERCENT', 1000000, '2026-06-30', 50, 1), -- Giảm 10% cho đơn từ 1tr
                                                                                                                ('WELCOME', 20000, 'FIXED', 0, '2026-12-31', 500, 1), -- Giảm 20k cho mọi đơn
                                                                                                                ('EXPIRED', 100000, 'FIXED', 0, '2025-01-01', 10, 0); -- Mã hết hạn (để test lỗi)
GO

-- 5. INSERT ORDERS (Quy trình mua hàng)
-- Đơn 1: Đã hoàn thành (Có Voucher, có Thanh toán)
INSERT INTO Orders (username, createDate, address, phone, status, VoucherCode, totalAmount) VALUES
    ('user', '2026-01-15 08:30:00', N'123 CMT8, Q.3, TP.HCM', '0123456789', 4, 'JENKA50', 18700000);
-- (Tổng 18tr750 - 50k voucher = 18tr700)

-- Đơn 2: Mới đặt (Chưa thanh toán, Ship COD)
INSERT INTO Orders (username, createDate, address, phone, status, VoucherCode, totalAmount) VALUES
    ('user', '2026-01-27 09:00:00', N'KTX FPT Polytechnic', '0123456789', 0, NULL, 500000);

-- Đơn 3: Đã hủy
INSERT INTO Orders (username, createDate, address, phone, status, VoucherCode, totalAmount) VALUES
    ('user', '2026-01-20 10:00:00', N'Quận 12, TP.HCM', '0123456789', 3, NULL, 85000);
GO

-- 6. INSERT ORDER_DETAILS
INSERT INTO OrderDetails (OrderId, ProductId, price, quantity) VALUES
                                                                   (1, 1, 18500000, 1), -- Đơn 1 mua Máy Breville
                                                                   (1, 8, 250000, 1),   -- Đơn 1 mua thêm Cafe Hạt
                                                                   (2, 8, 250000, 2),   -- Đơn 2 mua 2 gói Cafe Hạt
                                                                   (3, 10, 85000, 1);   -- Đơn 3 mua Bánh Biscotti
GO

-- 7. INSERT PAYMENTS (Lịch sử thanh toán)
INSERT INTO Payments (OrderId, amount, paymentMethod, transactionCode, paymentDate, status) VALUES
    (1, 18700000, 'VNPAY', 'VNP12345678', '2026-01-15 08:35:00', 'SUCCESS');
-- Đơn 2 và 3 chưa thanh toán hoặc COD nên chưa có record ở đây (hoặc record PENDING)
GO

-- 8. INSERT SERVICE_BOOKINGS (Đặt lịch sửa chữa)
INSERT INTO ServiceBookings (username, customerName, phone, description, bookingDate, preferredTime, status) VALUES
                                                                                                                 ('user', N'Nguyễn Văn Khách', '0123456789', N'Máy Breville bị kêu to, áp suất không lên.', '2026-02-01', N'Buổi Sáng', 'PENDING'),
                                                                                                                 ('admin', N'Khách Vãng Lai', '0987654321', N'Thay lưỡi dao máy xay.', '2026-01-25', N'Buổi Chiều', 'COMPLETED');
GO

-- 9. INSERT POINT_HISTORY (Lịch sử điểm)
INSERT INTO PointHistory (username, amount, OrderId, reason, createDate) VALUES
                                                                             ('user', 100, 1, N'Tích điểm đơn hàng #1', '2026-01-15 08:30:00'), -- Cộng điểm mua hàng
                                                                             ('user', 50, NULL, N'Thưởng đăng ký thành viên mới', '2026-01-01 00:00:00'), -- Cộng điểm sự kiện
                                                                             ('user', -10, NULL, N'Đổi quà móc khóa', '2026-01-16 10:00:00'); -- Trừ điểm
GO