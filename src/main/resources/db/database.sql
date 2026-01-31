/* ==================================================
   SCRIPT KHỞI TẠO LẠI DỮ LIỆU JENKA COFFEE
   HOSTING: db_ac3c24_jenkacoffee
   UPDATED: 2026-01-14
   ================================================== */

USE db_ac3c24_jenkacoffee; -- Đảm bảo đang chọn đúng DB trên hosting
GO

/* ==================================================
   PHẦN 0: DỌN DẸP DỮ LIỆU CŨ (DROP TABLES)
   (Phải xóa theo thứ tự: Bảng con trước -> Bảng cha sau)
   ================================================== */

-- Tắt kiểm tra khóa ngoại để xóa cho dễ (nếu cần), nhưng xóa chuẩn thì không cần
IF OBJECT_ID('dbo.OrderDetails', 'U') IS NOT NULL DROP TABLE dbo.OrderDetails;
IF OBJECT_ID('dbo.Orders', 'U') IS NOT NULL DROP TABLE dbo.Orders;
IF OBJECT_ID('dbo.Products', 'U') IS NOT NULL DROP TABLE dbo.Products;
IF OBJECT_ID('dbo.Categories', 'U') IS NOT NULL DROP TABLE dbo.Categories;
IF OBJECT_ID('dbo.Accounts', 'U') IS NOT NULL DROP TABLE dbo.Accounts;
GO

/* ==================================================
   PHẦN 1: TẠO BẢNG MỚI (CREATE TABLES)
   ================================================== */

-- 1. Bảng ACCOUNTS
CREATE TABLE Accounts (
                          username VARCHAR(50) NOT NULL,
                          password VARCHAR(100) NOT NULL,
                          fullname NVARCHAR(100) NOT NULL,
                          email VARCHAR(100) NOT NULL,
                          photo NVARCHAR(255) NULL,
                          activated BIT DEFAULT 1,
                          admin BIT DEFAULT 0,
                          PRIMARY KEY (username)
);
GO

-- 2. Bảng CATEGORIES (Đã cập nhật theo yêu cầu mới)
CREATE TABLE Categories (
                            id VARCHAR(50) NOT NULL,
                            name NVARCHAR(100) NOT NULL,
                            PRIMARY KEY (id)
);
GO

-- 3. Bảng PRODUCTS
CREATE TABLE Products (
                          id INT IDENTITY(1,1) NOT NULL,
                          name NVARCHAR(200) NOT NULL,
                          image NVARCHAR(255) NULL,
                          price FLOAT NOT NULL CHECK (price >= 0),
                          createDate DATE DEFAULT GETDATE(),
                          available BIT DEFAULT 1,
                          CategoryId VARCHAR(50) NOT NULL,
                          description NVARCHAR(MAX) NULL, -- Thêm cột mô tả cho xịn
                          PRIMARY KEY (id),
                          FOREIGN KEY (CategoryId) REFERENCES Categories(id)
);
GO

-- 4. Bảng ORDERS
CREATE TABLE Orders (
                        id BIGINT IDENTITY(1,1) NOT NULL,
                        username VARCHAR(50) NOT NULL,
                        createDate DATETIME DEFAULT GETDATE(),
                        address NVARCHAR(255) NOT NULL,
                        phone VARCHAR(15) NOT NULL,
                        status INT DEFAULT 0, -- 0: Mới, 1: Duyệt, 2: Giao, 3: Huỷ
                        PRIMARY KEY (id),
                        FOREIGN KEY (username) REFERENCES Accounts(username)
);
GO

-- 5. Bảng ORDER_DETAILS
CREATE TABLE OrderDetails (
                              id BIGINT IDENTITY(1,1) NOT NULL,
                              OrderId BIGINT NOT NULL,
                              ProductId INT NOT NULL,
                              price FLOAT NOT NULL,
                              quantity INT NOT NULL CHECK (quantity > 0),
                              PRIMARY KEY (id),
                              FOREIGN KEY (OrderId) REFERENCES Orders(id),
                              FOREIGN KEY (ProductId) REFERENCES Products(id)
);
GO

/* ==================================================
   PHẦN 2: CHÈN DỮ LIỆU MẪU (SEED DATA)
   ================================================== */

-- 1. Insert Accounts
INSERT INTO Accounts (username, password, fullname, email, admin, photo) VALUES
                                                                             ('admin', '123', N'Trần Thiên Lộc (Leader)', 'loctt@fpt.edu.vn', 1, 'admin.jpg'),
                                                                             ('user', '123', N'Nguyễn Văn Khách', 'khachhang@gmail.com', 0, 'user.jpg'),
                                                                             ('staff', '123', N'Nhân Viên Tư Vấn', 'staff@jenka.com', 1, 'staff.jpg');

-- 2. Insert Categories (Theo đúng 6 loại bro yêu cầu)
-- ID mình viết tắt cho gọn, Name viết đầy đủ tiếng Việt
INSERT INTO Categories (id, name) VALUES
                                      ('MAY_PHA', N'Máy Pha Cà Phê'),
                                      ('MAY_XAY', N'Máy Xay Cà Phê'),
                                      ('XAY_EP', N'Máy Xay & Máy Ép'),
                                      ('CF_AN_VAT', N'Cà Phê & Đồ Ăn Vặt'),
                                      ('DUNG_CU', N'Dụng Cụ Pha Chế'),
                                      ('HANG_CU', N'Máy Pha & Xay Cũ Lướt');

-- 3. Insert Products (Mỗi loại 2-3 sản phẩm demo)
INSERT INTO Products (name, price, image, CategoryId, createDate, description) VALUES
-- 1. Máy Pha Cà Phê
(N'Máy Pha Cà Phê Breville 870 XL', 18500000, 'may_pha_01.jpg', 'MAY_PHA', '2025-11-01', N'Nhập khẩu Úc, tích hợp máy xay.'),
(N'Máy Pha Nuova Simonelli Appia II', 65000000, 'may_pha_02.jpg', 'MAY_PHA', '2025-11-02', N'Chuyên nghiệp dành cho quán lớn.'),
(N'Máy Pha Cà Phê Casadio Undici', 42000000, 'may_pha_03.jpg', 'MAY_PHA', '2025-11-03', N'Nồi hơi lớn, công suất mạnh mẽ.'),

-- 2. Máy Xay Cà Phê
(N'Máy Xay Cà Phê Eureka Mignon', 8900000, 'may_xay_01.jpg', 'MAY_XAY', '2025-12-01', N'Thiết kế nhỏ gọn, độ ồn thấp.'),
(N'Máy Xay Fiorenzato F64E', 18000000, 'may_xay_02.jpg', 'MAY_XAY', '2025-12-02', N'Màn hình cảm ứng, tốc độ xay cực nhanh.'),

-- 3. Máy Xay & Máy Ép
(N'Máy Ép Trái Cây Chậm Hurom', 9500000, 'xay_ep_01.jpg', 'XAY_EP', '2026-01-05', N'Giữ nguyên vitamin, ép kiệt bã.'),
(N'Máy Xay Sinh Tố Công Nghiệp Vitamix', 12500000, 'xay_ep_02.jpg', 'XAY_EP', '2026-01-06', N'Xay đá bi nhuyễn mịn trong 3 giây.'),

-- 4. Cà Phê & Đồ Ăn Vặt
(N'Cà Phê Hạt Arabica Cầu Đất (500g)', 250000, 'cf_01.jpg', 'CF_AN_VAT', '2026-01-10', N'Hương thơm quyến rũ, vị chua thanh.'),
(N'Cà Phê Robusta Honey (1kg)', 320000, 'cf_02.jpg', 'CF_AN_VAT', '2026-01-11', N'Đậm đà, hậu vị ngọt sâu.'),
(N'Bánh Biscotti Nguyên Cám', 85000, 'an_vat_01.jpg', 'CF_AN_VAT', '2026-01-12', N'Ăn kiêng, healthy, phù hợp uống kèm cafe.'),

-- 5. Dụng Cụ Pha Chế
(N'Bộ Shaker Inox Cao Cấp', 150000, 'dung_cu_01.jpg', 'DUNG_CU', '2026-01-13', N'Chuyên dùng cho Bartender.'),
(N'Ca Đánh Sữa Latte Art 600ml', 250000, 'dung_cu_02.jpg', 'DUNG_CU', '2026-01-13', N'Mũi nhọn dễ tạo hình.'),

-- 6. Máy Cũ Lướt (Second hand)
(N'Máy Pha Cũ Expobar (95%)', 25000000, 'cu_luot_01.jpg', 'HANG_CU', '2026-01-01', N'Máy thanh lý quán, còn bảo hành 3 tháng.'),
(N'Máy Xay Cũ Mazzer Super Jolly (90%)', 9000000, 'cu_luot_02.jpg', 'HANG_CU', '2026-01-02', N'Lưỡi dao còn bén, motor chạy êm.');

-- 4. Insert Orders
INSERT INTO Orders (username, createDate, address, phone, status) VALUES
                                                                      ('user', '2026-01-10 08:30:00', N'123 CMT8, Q.3, TP.HCM', '0909123456', 1), -- Đã giao
                                                                      ('user', '2026-01-14 09:00:00', N'KTX FPT Polytechnic', '0123456789', 0); -- Mới đặt

-- 5. Insert OrderDetails
INSERT INTO OrderDetails (OrderId, ProductId, price, quantity) VALUES
                                                                   (1, 8, 250000, 2), -- Đơn 1 mua 2 gói Arabica
                                                                   (2, 10, 85000, 5); -- Đơn 2 mua 5 gói Biscotti

GO
-- Check lại dữ liệu xem lên chưa
SELECT * FROM Categories;
SELECT * FROM Products;