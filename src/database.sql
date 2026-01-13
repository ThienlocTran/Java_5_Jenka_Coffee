Use master
/* ==================================================
   SCRIPT KHỞI TẠO DATABASE JENKA COFFEE
   ASSIGNMENT JAVA 5 - TEAM LEADER: THIÊN LỘC
   ================================================== */

USE master;
GO

-- 1. Xóa Database cũ nếu tồn tại để làm sạch
IF EXISTS (SELECT * FROM sys.databases WHERE name = 'JenkaCoffeeDB')
BEGIN
        DROP DATABASE JenkaCoffeeDB;
END
GO

-- 2. Tạo Database mới
CREATE DATABASE JenkaCoffeeDB;
GO

USE JenkaCoffeeDB;
GO

/* ==================================================
   PHẦN 1: TẠO BẢNG (TABLES)
   ================================================== */

-- Bảng 1: ACCOUNTS (Khách hàng & Admin)
CREATE TABLE Accounts (
                          username VARCHAR(50) NOT NULL,
                          password VARCHAR(100) NOT NULL,
                          fullname NVARCHAR(100) NOT NULL,
                          email VARCHAR(100) NOT NULL,
                          photo NVARCHAR(255) NULL,
                          activated BIT DEFAULT 1, -- 1: Hoạt động, 0: Khóa
                          admin BIT DEFAULT 0,     -- 1: Là Admin, 0: Là Khách
                          PRIMARY KEY (username)
);
GO

-- Bảng 2: CATEGORIES (Loại hàng)
CREATE TABLE Categories (
                            id VARCHAR(50) NOT NULL,
                            name NVARCHAR(100) NOT NULL,
                            PRIMARY KEY (id)
);
GO

-- Bảng 3: PRODUCTS (Sản phẩm)
CREATE TABLE Products (
                          id INT IDENTITY(1,1) NOT NULL,
                          name NVARCHAR(200) NOT NULL,
                          image NVARCHAR(255) NULL,
                          price FLOAT NOT NULL CHECK (price >= 0),
                          createDate DATE DEFAULT GETDATE(),
                          available BIT DEFAULT 1,
                          CategoryId VARCHAR(50) NOT NULL,
                          PRIMARY KEY (id),
                          FOREIGN KEY (CategoryId) REFERENCES Categories(id)
);
GO

-- Bảng 4: ORDERS (Đơn hàng tổng)
CREATE TABLE Orders    (
                           id BIGINT IDENTITY(1,1) NOT NULL,
                           username VARCHAR(50) NOT NULL,
                           createDate DATETIME DEFAULT GETDATE(),
                           address NVARCHAR(255) NOT NULL,
                           phone VARCHAR(15) NOT NULL, -- SĐT người nhận
                           status INT DEFAULT 0, -- 0: Mới, 1: Duyệt, 2: Giao, 3: Huỷ
                           PRIMARY KEY (id),
                           FOREIGN KEY (username) REFERENCES Accounts(username)
);
GO

-- Bảng 5: ORDER_DETAILS (Chi tiết đơn hàng)
CREATE TABLE OrderDetails (
                              id BIGINT IDENTITY(1,1) NOT NULL,
                              OrderId BIGINT NOT NULL,
                              ProductId INT NOT NULL,
                              price FLOAT NOT NULL, -- Giá tại thời điểm mua
                              quantity INT NOT NULL CHECK (quantity > 0),
                              PRIMARY KEY (id),
                              FOREIGN KEY (OrderId) REFERENCES Orders(id),
                              FOREIGN KEY (ProductId) REFERENCES Products(id)
);
GO

/* ==================================================
   PHẦN 2: CHÈN DỮ LIỆU MẪU (SEED DATA)
   ================================================== */

-- 1. Thêm Accounts (Mật khẩu đang để '123' chưa mã hóa, sau này dùng Java mã hóa sau)
INSERT INTO Accounts (username, password, fullname, email, admin) VALUES
                                                                      ('admin', '123', N'Trần Thiên Lộc (Leader)', 'loctt@fpt.edu.vn', 1),
                                                                      ('user', '123', N'Nguyễn Văn Khách', 'khachhang@gmail.com', 0),
                                                                      ('tuvan', '123', N'Tứ Văn (Staff)', 'van@jenka.com', 1);

-- 2. Thêm Categories
INSERT INTO Categories (id, name) VALUES
                                      ('MAY_PHA', N'Máy Pha Cà Phê'),
                                      ('HAT_CF', N'Hạt Cà Phê Cao Cấp'),
                                      ('PHU_KIEN', N'Phụ Kiện Pha Chế');

-- 3. Thêm Products (Máy xịn sò)
-- Chèn lại với tên file ảnh thật có trong máy bro
INSERT INTO Products (name, price, image, CategoryId, createDate) VALUES
                                                                      (N'Máy Jura Giga W10 (Thụy Sĩ)', 95000000, 'may_pha_01.webp', 'MAY_PHA', '2025-10-20'),
                                                                      (N'Máy Breville 870 XL', 18500000, 'may_pha_02.png', 'MAY_PHA', '2025-11-01'),
                                                                      (N'Máy Nuova Simonelli', 65000000, 'may_pha_03.png', 'MAY_PHA', '2025-12-05'),
                                                                      (N'Máy Casadio Undici', 42000000, 'may_pha_04.png', 'MAY_PHA', '2025-12-10'),
                                                                      (N'Cà phê Hạt Arabica Cầu Đất', 250000, 'may_pha_05.png', 'HAT_CF', '2025-12-15'), -- Giả bộ dùng ảnh này đỡ
                                                                      (N'Máy Xay Cà Phê Eureka', 8900000, 'may_pha_06.png', 'PHU_KIEN', '2026-01-05');
-- 4. Thêm Orders (Giả lập đơn hàng đã mua)
INSERT INTO Orders (username, createDate, address, phone, status) VALUES
                                                                      ('user', '2026-01-10 08:30:00', N'123 CMT8, Q.3, TP.HCM', '0909123456', 1), -- Đã duyệt
                                                                      ('user', '2026-01-12 14:20:00', N'456 Lê Lợi, Q.1, TP.HCM', '0909123456', 0); -- Mới đặt

-- 5. Thêm OrderDetails
INSERT INTO OrderDetails (OrderId, ProductId, price, quantity) VALUES
                                                                   (1, 3, 250000, 2), -- Đơn 1 mua 2 gói Arabica
                                                                   (2, 2, 18500000, 1); -- Đơn 2 mua 1 máy Breville

GO
SELECT * FROM Accounts;
SELECT * FROM Products;