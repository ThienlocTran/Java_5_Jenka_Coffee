-- ======================================================================
-- SCRIPT CHÈN DỮ LIỆU MẪU CHO JENKA COFFEE (POSTGRESQL)
-- Chạy sau khi đã thực thi schema.sql
-- ======================================================================

-- 1. INSERT ACCOUNTS
-- Mật khẩu '123' đã được hash: $2a$10$cw.T.w.e.r.t.y.u.i.o.p.1.2.3.4.5.6.7.8.9.0.a.b.c.d (Ví dụ tượng trưng)
INSERT INTO "accounts" ("username", "password_hash", "fullname", "email", "phone", "phone_verified", "admin", "points", "customer_rank", "photo", "activated") VALUES
('admin', '$2a$10$cw.T.w.e.r.t.y.u.i.o.p.1.2.3.4.5.6.7.8.9.0.a.b.c.d', 'Trần Thiên Lộc (Leader)', 'loctt@fpt.edu.vn', '0909123456', TRUE, TRUE, 1500, 'DIAMOND', 'admin.jpg', TRUE),
('staff', '$2a$10$cw.T.w.e.r.t.y.u.i.o.p.1.2.3.4.5.6.7.8.9.0.a.b.c.d', 'Nhân Viên Tư Vấn', 'staff@jenka.com', '0909888999', TRUE, TRUE, 0, 'MEMBER', 'staff.jpg', TRUE),
('user',  '$2a$10$cw.T.w.e.r.t.y.u.i.o.p.1.2.3.4.5.6.7.8.9.0.a.b.c.d', 'Nguyễn Văn Khách', 'khachhang@gmail.com', '0123456789', FALSE, FALSE, 200, 'GOLD', 'user.jpg', TRUE);

-- 2. INSERT CATEGORIES
INSERT INTO "categories" ("id", "name", "icon") VALUES
('MAY_PHA', 'Máy Pha Cà Phê', 'May_Pha_Ca_Phe.webp'),
('MAY_XAY', 'Máy Xay Cà Phê', 'May_Xay_Ca_Phe.webp'),
('XAY_EP', 'Máy Xay & Máy Ép', 'may_xay_sinh_to_may_ep.webp'),
('CF_AN_VAT', 'Cà Phê & Đồ Ăn Vặt', 'ca_phe_do_an.webp'),
('DUNG_CU', 'Dụng Cụ Pha Chế', 'dung_cu_pha_che.webp'),
('HANG_CU', 'Máy Cũ Lướt (Second Hand)', 'may_pha_may_xay_cu.webp');

-- 3. INSERT PRODUCTS
INSERT INTO "products" ("name", "price", "image", "categoryid", "createdate", "description", "available", "quantity") VALUES
-- Máy Pha
('Máy Pha Cà Phê Breville 870 XL', 18500000, 'may_pha_01.jpg', 'MAY_PHA', '2025-11-01', 'Nhập khẩu Úc, tích hợp máy xay, áp suất 15 bar.', TRUE, 10),
('Máy Pha Nuova Simonelli Appia II', 65000000, 'may_pha_02.jpg', 'MAY_PHA', '2025-11-02', 'Chuyên nghiệp 2 group dành cho quán lớn.', TRUE, 5),
('Máy Pha Casadio Undici A1', 42000000, 'may_pha_03.jpg', 'MAY_PHA', '2025-11-03', 'Nồi hơi lớn, công suất mạnh mẽ, bền bỉ.', TRUE, 8),

-- Máy Xay
('Máy Xay Eureka Mignon', 8900000, 'may_xay_01.jpg', 'MAY_XAY', '2025-12-01', 'Thiết kế nhỏ gọn, độ ồn thấp, xay Espresso chuẩn.', TRUE, 15),
('Máy Xay Fiorenzato F64E', 18000000, 'may_xay_02.jpg', 'MAY_XAY', '2025-12-02', 'Màn hình cảm ứng, tốc độ xay cực nhanh.', TRUE, 7),

-- Xay & Ép
('Máy Ép Chậm Hurom H200', 9500000, 'xay_ep_01.jpg', 'XAY_EP', '2026-01-05', 'Giữ nguyên vitamin, ép kiệt bã, dễ vệ sinh.', TRUE, 12),
('Máy Xay Vitamix The Quiet One', 12500000, 'xay_ep_02.jpg', 'XAY_EP', '2026-01-06', 'Chuyên xay đá bi, sinh tố, độ ồn cực thấp.', TRUE, 4),

-- Cafe & Ăn Vặt
('Cà Phê Hạt Arabica Cầu Đất (500g)', 250000, 'cf_01.jpg', 'CF_AN_VAT', '2026-01-10', 'Hương thơm quyến rũ, vị chua thanh nhẹ.', TRUE, 100),
('Cà Phê Robusta Honey (1kg)', 320000, 'cf_02.jpg', 'CF_AN_VAT', '2026-01-11', 'Đậm đà, hậu vị ngọt sâu, hàm lượng cafein cao.', TRUE, 150),
('Bánh Biscotti Nguyên Cám (Hũ)', 85000, 'an_vat_01.jpg', 'CF_AN_VAT', '2026-01-12', 'Ăn kiêng, healthy, phù hợp uống kèm cafe.', TRUE, 50),

-- Dụng Cụ
('Bộ Shaker Inox 500ml', 150000, 'dung_cu_01.jpg', 'DUNG_CU', '2026-01-13', 'Thép không gỉ 304, chuyên dùng cho Bartender.', TRUE, 30),
('Ca Đánh Sữa Latte Art 600ml', 250000, 'dung_cu_02.jpg', 'DUNG_CU', '2026-01-13', 'Mũi nhọn dễ tạo hình, vạch chia dung tích.', TRUE, 25),

-- Hàng Cũ
('Máy Pha Cũ Expobar (95%)', 25000000, 'cu_luot_01.jpg', 'HANG_CU', '2026-01-01', 'Thanh lý quán nghỉ bán, bảo hành 3 tháng.', TRUE, 2),
('Máy Xay Cũ Mazzer Super Jolly', 9000000, 'cu_luot_02.jpg', 'HANG_CU', '2026-01-02', 'Lưỡi dao còn bén, motor chạy êm ru.', TRUE, 3);

-- Reset sequence cho bảng Products (Vì đã tự chèn ID ngầm khi INSERT không có ID, SERIAL sẽ tự sinh)
-- Do đã insert mà PostgreSQL tự tăng ID, không cần SETVAL nếu không gán cứng ID.
-- Ở đây tôi chọn KHÔNG gán cứng ID trong câu lệnh trên, để PostgreSQL tự lo ID từ 1 -> N.

-- 4. INSERT VOUCHERS
INSERT INTO "vouchers" ("code", "discountamount", "discounttype", "minorderamount", "expirationdate", "quantity", "active") VALUES
('JENKA50', 50000, 'FIXED', 500000, '2026-12-31', 100, TRUE),
('VIP10', 10, 'PERCENT', 1000000, '2026-06-30', 50, TRUE),
('WELCOME', 20000, 'FIXED', 0, '2026-12-31', 500, TRUE),
('EXPIRED', 100000, 'FIXED', 0, '2025-01-01', 10, FALSE);

-- 5. INSERT ORDERS
-- Lưu đồ: ID của Accounts là 'user'
INSERT INTO "orders" ("username", "createdate", "address", "phone", "status", "vouchercode", "totalamount") VALUES
('user', '2026-01-15 08:30:00', '123 CMT8, Q.3, TP.HCM', '0123456789', 4, 'JENKA50', 18700000),
('user', '2026-01-27 09:00:00', 'KTX FPT Polytechnic', '0123456789', 0, NULL, 500000),
('user', '2026-01-20 10:00:00', 'Quận 12, TP.HCM', '0123456789', 3, NULL, 85000);

-- 6. INSERT ORDER_DETAILS
-- Assume Order IDs are 1, 2, 3 and Product IDs match the insert order above (1, 8, etc.)
INSERT INTO "orderdetails" ("orderid", "productid", "price", "quantity") VALUES
(1, 1, 18500000, 1),
(1, 8, 250000, 1),
(2, 8, 250000, 2),
(3, 10, 85000, 1);

-- 7. INSERT PAYMENTS
INSERT INTO "payments" ("orderid", "amount", "paymentmethod", "transactioncode", "paymentdate", "status") VALUES
(1, 18700000, 'VNPAY', 'VNP12345678', '2026-01-15 08:35:00', 'SUCCESS');

-- 8. INSERT SERVICE_BOOKINGS
INSERT INTO "servicebookings" ("username", "customername", "phone", "description", "bookingdate", "preferredtime", "status") VALUES
('user', 'Nguyễn Văn Khách', '0123456789', 'Máy Breville bị kêu to, áp suất không lên.', '2026-02-01 08:00:00', 'Buổi Sáng', 'PENDING'),
('admin', 'Khách Vãng Lai', '0987654321', 'Thay lưỡi dao máy xay.', '2026-01-25 14:00:00', 'Buổi Chiều', 'COMPLETED');

-- 9. INSERT POINT_HISTORY
INSERT INTO "pointhistory" ("username", "amount", "orderid", "reason", "createdate") VALUES
('user', 100, 1, 'Tích điểm đơn hàng #1', '2026-01-15 08:30:00'),
('user', 50, NULL, 'Thưởng đăng ký thành viên mới', '2026-01-01 00:00:00'),
('user', -10, NULL, 'Đổi quà móc khóa', '2026-01-16 10:00:00');

-- 10. INSERT NEWS
INSERT INTO "news" ("title", "content", "image", "createdate", "available") VALUES
('Khai trương chi nhánh mới tại Hà Nội', 'Chúng tôi vui mừng thông báo khai trương chi nhánh mới tại Hà Nội với không gian hiện đại...', 'https://images.unsplash.com/photo-1554118811-1e0d58224f24?w=800', '2026-02-01 08:00:00', TRUE),
('Chương trình khuyến mãi tháng 2/2026', 'Nhân dịp đầu năm mới, Jenka Coffee triển khai chương trình khuyến mãi đặc biệt...', 'https://images.unsplash.com/photo-1607082348824-0a96f2a4b9da?w=800', '2026-02-05 09:00:00', TRUE);
