/* ==================================================
   SCRIPT DATABASE JENKA COFFEE - V3 (NEWS MODULE)
   UPDATED: 2026-02-09
   NOTE: Thêm bảng News (Tin tức) và Quantity cho Products
   ================================================== */

USE db_ac3c24_jenkacoffee;
GO

/* --- PHẦN 0: DROP TABLE NEWS (Nếu đã tồn tại) --- */
IF OBJECT_ID('dbo.News', 'U') IS NOT NULL DROP TABLE dbo.News;
GO

/* --- PHẦN 1: TẠO BẢNG NEWS --- */
CREATE TABLE News (
    Id INT IDENTITY(1,1) NOT NULL,
    Title NVARCHAR(255) NOT NULL,
    Content NVARCHAR(MAX) NULL,
    Image NVARCHAR(500) NULL,
    CreateDate DATETIME DEFAULT GETDATE(),
    Available BIT DEFAULT 1,
    PRIMARY KEY (Id)
);
GO

/* --- PHẦN 2: THÊM CỘT QUANTITY CHO PRODUCTS (Nếu chưa có) --- */
-- Kiểm tra xem cột Quantity đã tồn tại chưa
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('dbo.Products') AND name = 'Quantity')
BEGIN
    ALTER TABLE Products ADD Quantity INT DEFAULT 0;
END
GO

/* --- PHẦN 3: INSERT DỮ LIỆU MẪU CHO NEWS --- */
INSERT INTO News (Title, Content, Image, CreateDate, Available) VALUES
(N'Khai trương chi nhánh mới tại Hà Nội', 
 N'Chúng tôi vui mừng thông báo khai trương chi nhánh mới tại Hà Nội với không gian hiện đại, đầy đủ tiện nghi và đội ngũ nhân viên chuyên nghiệp. Địa chỉ: 123 Trần Duy Hưng, Cầu Giấy, Hà Nội.

Đến với chi nhánh mới, quý khách sẽ được trải nghiệm:
- Không gian rộng rãi, thoáng mát với thiết kế hiện đại
- Đầy đủ các dòng máy pha cà phê cao cấp từ các thương hiệu nổi tiếng
- Đội ngũ nhân viên tư vấn chuyên nghiệp, nhiệt tình
- Chương trình khuyến mãi đặc biệt dành cho khách hàng mới

Hãy đến và trải nghiệm dịch vụ tốt nhất từ chúng tôi!', 
 'https://images.unsplash.com/photo-1554118811-1e0d58224f24?w=800',
 '2026-02-01 08:00:00',
 1),

(N'Chương trình khuyến mãi tháng 2/2026', 
 N'Nhân dịp đầu năm mới, Jenka Coffee triển khai chương trình khuyến mãi đặc biệt với nhiều ưu đãi hấp dẫn:

🎁 GIẢM GIÁ LÊN ĐẾN 30%
- Tất cả các dòng máy pha cà phê
- Máy xay cà phê chuyên nghiệp
- Phụ kiện và dụng cụ pha chế

🎁 QUÀ TẶNG HẤP DẪN
- Tặng 1kg cà phê hạt cao cấp cho đơn hàng từ 10 triệu
- Tặng bộ dụng cụ pha chế cho đơn hàng từ 20 triệu

🎁 MIỄN PHÍ VẬN CHUYỂN
- Áp dụng cho tất cả đơn hàng trong nội thành

Thời gian: Từ 01/02/2026 đến 29/02/2026
Đừng bỏ lỡ cơ hội này!', 
 'https://images.unsplash.com/photo-1607082348824-0a96f2a4b9da?w=800',
 '2026-02-05 09:00:00',
 1),

(N'Hướng dẫn pha cà phê espresso hoàn hảo', 
 N'Bài viết chi tiết hướng dẫn cách pha một ly cà phê espresso hoàn hảo tại nhà với máy pha cà phê chuyên nghiệp.

☕ BƯỚC 1: CHỌN HẠT CÀ PHÊ
- Chọn hạt cà phê Arabica hoặc Robusta chất lượng cao
- Hạt cà phê nên được rang trong vòng 2-4 tuần
- Bảo quản hạt trong hộp kín, tránh ánh sáng

☕ BƯỚC 2: XAY CÀ PHÊ
- Xay mịn vừa phải (độ mịn như muối ăn)
- Xay ngay trước khi pha để giữ hương vị
- Liều lượng: 18-20g cho 1 shot espresso đôi

☕ BƯỚC 3: KỸ THUẬT PHA CHẾ
- Nhiệt độ nước: 90-96°C
- Áp suất: 9 bar
- Thời gian chiết xuất: 25-30 giây
- Lượng nước: 30-40ml cho 1 shot đôi

☕ BƯỚC 4: ĐÁNH GIÁ
- Màu sắc: Nâu đỏ đậm
- Crema: Dày 3-4mm, màu hạt dẻ
- Hương vị: Cân bằng giữa đắng, chua, ngọt

Chúc các bạn thành công!', 
 'https://images.unsplash.com/photo-1511920170033-f8396924c348?w=800',
 '2026-02-07 10:30:00',
 1),

(N'Bí quyết chọn máy pha cà phê phù hợp', 
 N'Hướng dẫn chi tiết giúp bạn chọn được máy pha cà phê phù hợp với nhu cầu và ngân sách.

🔍 PHÂN LOẠI MÁY PHA CÀ PHÊ

1. MÁY PHA BÁN TỰ ĐỘNG
- Phù hợp: Gia đình, quán nhỏ
- Giá: 5-20 triệu
- Ưu điểm: Dễ sử dụng, giá cả phải chăng
- Nhược điểm: Cần kỹ năng pha chế cơ bản

2. MÁY PHA TỰ ĐỘNG
- Phù hợp: Văn phòng, quán vừa
- Giá: 20-50 triệu
- Ưu điểm: Tự động hoàn toàn, ổn định
- Nhược điểm: Giá cao, bảo trì phức tạp

3. MÁY PHA CHUYÊN NGHIỆP
- Phù hợp: Quán lớn, chuỗi cửa hàng
- Giá: 50-200 triệu
- Ưu điểm: Công suất lớn, độ bền cao
- Nhược điểm: Giá rất cao, cần thợ chuyên

💡 LỜI KHUYÊN
- Xác định nhu cầu sử dụng hàng ngày
- Cân nhắc ngân sách đầu tư
- Chọn thương hiệu uy tín
- Quan tâm đến dịch vụ bảo hành

Liên hệ Jenka Coffee để được tư vấn miễn phí!', 
 'https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=800',
 '2026-02-08 14:00:00',
 1),

(N'Xu hướng cà phê specialty tại Việt Nam', 
 N'Cà phê specialty đang trở thành xu hướng được yêu thích tại Việt Nam với chất lượng cao và hương vị độc đáo.

📈 THỰC TRẠNG THỊ TRƯỜNG

Trong những năm gần đây, thị trường cà phê specialty tại Việt Nam đang phát triển mạnh mẽ với tốc độ tăng trưởng 20-30%/năm. Người tiêu dùng Việt Nam ngày càng quan tâm đến chất lượng và nguồn gốc của cà phê.

☕ ĐẶC ĐIỂM CÀ PHÊ SPECIALTY

- Điểm chất lượng: Từ 80 điểm trở lên (theo SCA)
- Nguồn gốc: Rõ ràng, minh bạch
- Quy trình: Chăm sóc tỉ mỉ từ trồng đến rang
- Hương vị: Phong phú, đa dạng

🌟 CÁC VÙNG TRỒNG NỔI TIẾNG

1. Cầu Đất, Lâm Đồng
2. Sơn La
3. Kon Tum
4. Đắk Lắk

💰 GIÁ CẢ

Cà phê specialty có giá cao hơn cà phê thương mại 2-5 lần, nhưng mang lại trải nghiệm hoàn toàn khác biệt về hương vị và chất lượng.

Jenka Coffee tự hào cung cấp các dòng cà phê specialty chất lượng cao từ các vùng trồng nổi tiếng!', 
 'https://images.unsplash.com/photo-1447933601403-0c6688de566e?w=800',
 '2026-02-09 11:00:00',
 1);
GO

/* --- PHẦN 5: KIỂM TRA DỮ LIỆU --- */
SELECT * FROM News ORDER BY CreateDate DESC;
GO
