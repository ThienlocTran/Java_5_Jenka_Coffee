# Báo Cáo Phân Tích Bảo Mật & Hệ Thống Tối Ưu - Jenka Coffee (Phiên bản Enterprise)
**Đối tượng:** Ứng dụng quản trị và bán hàng Jenka Coffee (Spring Boot + Vue.js)
**Mức độ đánh giá:** White-box Penetration Testing & System Architecture Review
**Tổng số điểm nóng đã phát hiện:** 80 Vulnerabilities & Architecture Limitations

---

## TÓM TẮT GIAI ĐOẠN 1: BUGS 1 - 36 (Vulnerabilities Nền Tảng)
*(Đã được fix hoặc phát hiện từ các phiên phân tích trước)*

- **Bảo mật API & Rate Limit:** Bỏ qua Rate Limit IP Spoofing (Sử dụng Header ảo), Lỗi tiêu thụ Body Request ở RateLimitFilter khiến Frontend gãy data.
- **Xác thực & Phân quyền:** IDOR ở Endpoint API Admin, Frontend Validation không đồng bộ với Backend (Ép OAuth user điền mật khẩu cũ). Lỗi cấu trúc Blacklist Token qua In-memory.
- **Gian lận Thương Mại:** Cart Poisoning (Dùng chung anonymous key), Báo cáo doanh số Analytics bị thổi phồng, Số lượng mua bị âm hoặc đánh lừa Max=99. 
- **SEO & File System:** (Bug 34 & 35) Sitemap API bất đồng bộ với Vue Router gây liệt SEO. Volatile Analytics làm mất toàn bộ số liệu Tracking khi sập Server. Lỗ hổng Path Traversal & Orphaned Cloudinary Storage khi xóa dữ liệu ảnh.
- **Bảo mật XSS & CSP:** Quên cấu hình Content Security Policy an toàn, cho phép Inline Scripts. Lỗ hổng lưu Inject Script qua Note đặt hàng.

---

## GIAI ĐOẠN 2: BUGS 37 - 44 (Business Logic & Luồng Thanh Toán)

* **Bug 37: OAuth Takeover:** Tài khoản chưa kích hoạt có thể bị chiếm đoạt thông qua tính năng Google OAuth.
* **Bug 38: Payment Fraud:** Thiếu cơ chế Server-side Verification với Cổng Thanh Toán.
* **Bug 39: Tràn Socket/Resource Leak:** `GoogleOAuthServiceImpl` liên tục tạo `NetHttpTransport` mới mà không `.close()`.
* **Bug 40: Bỏ Qua Consent:** Thiếu Validation bắt buộc ở tầng Backend cho "Điều khoản và Quy định".
* **Bug 41: Email XSS:** Thư viện `EmailServiceImpl` KHÔNG thoát HTML nội dung `fullname`.
* **Bug 42: Voucher Overwrite:** Lỗi thực thi `upsert` trên bảng Voucher làm đè campaign cũ đang chạy.
* **Bug 43: Pagination DoS:** `ApiOrderController` cho phép truyền số trang vô hạn.
* **Bug 44: Business Logic Voucher:** Data Schema của Voucher thiếu cấu hình `maxUsesPerUser`.

---

## GIAI ĐOẠN 3: BUGS 45 - 53 (State Management & Race Conditions)

* **Bug 45: Lỗ Hổng Ảo Giác Đa Luồng:** Khóa `synchronized` ở `ProductServiceImpl` vô nghĩa khi Scale-out Load Balancing.
* **Bug 46: Thùng Rác Vĩnh Cửu:** `JwtBlacklistServiceImpl` cấu hình `Caffeine` lưu cố định mọi token trong 7 ngày bỏ qua TTL.
* **Bug 47: Nấm mồ Marketing (SPA Blank SEO):** Vue.js chạy thuần CSR thiếu Server-side Rendering cho Meta Tag.
* **Bug 48: Ném Bom Thư Điện Tử (Email Spam DoS):** API Quên mật khẩu không Rate Limit (chỉ chặn IP), bị spam làm Blacklist Server SMTP.
* **Bug 49: Bóng Ma CSRF Tái Sinh (Subdomain CSRF):** Tắt CSRF Protection và chỉ tin vào `SameSite=LAX` làm hở sườn Subdomain.
* **Bug 50: Giả Mạo Vận Mệnh (Analytics Manipulation):** Endpoint đếm Visitor không thiết lập Authenticity Token.
* **Bug 51: Lời Nguyền Không Thể Mở Rộng:** Giỏ hàng nằm cứng trong bộ nhớ RAM (`ConcurrentHashMap`), phá rãnh khả năng scale.
* **Bug 52: Cổ Chai Flash Sale:** Dùng Lock PESSIMISTIC SQL cho cái Voucher làm chết cứng đường truyền kết nối đợt Sale.
* **Bug 53: Thiếu hụt Inventory Management:** Hoàn toàn thiếu field `stockQuantity`, xuất Bill bán âm số lượng thực tế gây đền bù Hợp đồng.

---

## GIAI ĐOẠN 4: BUGS 54 - 64 (System Architecture & Security Audit)

* **Bug 54: Nút Bấm Tự Sát (Admin Suicide):** Hệ thống không có cờ "Last Admin" để chặn việc Admin tự xóa chính tài khoản mình.
* **Bug 55: Loạn Luân Quyền Lực (Admin Takeover via Insider):** Admin có quyền Đổi mật khẩu của Admin khác mà không qua Audit.
* **Bug 56: Bẻ Cong Không Thời Gian (Timezone Blindness):** Export DTO chuyển `LocalDateTime.now()` thẳng ra chuỗi chạy sai lệch Database.
* **Bug 57: Lòng Trung Thành Chết Yểu:** Order flow có hàm trừ Điểm Thưởng, nhưng KHÔNG HỀ cấu hình việc Cộng thêm Điểm khi hoàn tất mua hàng.
* **Bug 58: Hiệu Ứng Bầy Đàn (Cache Stampede on Categories):** `@Cacheable` không có cờ `sync=true`. Database lòi chóp vì truy vấn song song.
* **Bug 59: Lỗ Sâu Vận Chuyển:** Phí Giao hàng bị Hardcode `0đ` ở Frontend, Database không có cột `shippingFee`. Quán tự gánh phí Ship thực tế.
* **Bug 60: Nhật Ký Câm Lặng (Missing Action Audits):** Quản trị viên sửa đổi dữ liệu VÔ HÌNH. Không có bảng `AuditLog` lưu lại hành vi.
* **Bug 61: Dư Thừa Dữ Liệu SQL:** Dùng `findAll()` nạp nguyên Entity khổng lồ đè lên RAM thay vì các DTO mỏng để render List. 
* **Bug 62: Tắc Nghẽn Động Mạch Mạng [CỰC ĐỘC]:** Gọi API Cloudinary (Upload) NẰM TRONG phạm vi `@Transactional` khóa kẹt Database.
* **Bug 63: Hệ Miễn Dịch Chậm Chạp (JWT Lockout Delay):** Khóa Account trên DB nhưng `JwtAuthFilter` vẫn cho JWT ngoại lai tiếp tục truy cập 24 tiếng.
* **Bug 64: Kẻ Nuốt Chửng Phân Quyền:** `GlobalExceptionHandler` xực luôn `AccessDeniedException` trả `500 Server Error` thay vì `403 Forbidden`.

---

## GIAI ĐOẠN 5: BUGS 65 - 80 (DevOps, Caching & Vùng Tối Edge Cases)

* **Bug 65: Bí Mật Mở Toang (Hardcoded Production Secrets):** `application.properties` nạp thẳng mật khẩu thực của NeonDB, Cloudinary, Gmail, JWT Secret ở tham số fallback mặc định.
* **Bug 66: Quả Bom Phá Mỡ (ddl-auto=update):** Bật cấu hình `ddl-auto=update` ở Production gây nguy cơ corrupt cấu trúc cột tự động khi Maintain.
* **Bug 67: Tự Tay Bóp Cổ (Low Tomcat Threads):** Tomcat bị giới hạn cứng `max-threads=20`, dễ dàng bị tê liệt hoàn toàn (DoS tự nhiên) nếu hệ thống có thao tác tắc nghẽn IO.
* **Bug 68: Kẻ Hủy Diệt Index (SQL EXTRACT):** Dùng hàm `EXTRACT(YEAR FROM o.createDate)` ở mệnh đề WHERE khiến DB bị mù, phải Full Table Scan loại bỏ Index B-Tree.
* **Bug 69: Truy Vấn Không Đáy (Unbounded Analytics):** Hàm tính Doanh thu/Sản phẩm Top hoàn toàn bỏ quên mệnh đề WHERE Timeframe, tính toán lại toàn bộ lịch sử 100% DB mỗi lần Load Dashboard.
* **Bug 70: Lỗ Đen Tìm Kiếm (SQL LIKE Bắn Vỡ Index):** `LIKE LOWER('%keyword%')` (Dấu % nằm trước) triệt tiêu hoàn toàn tốc độ tìm kiếm B-Tree. 
* **Bug 71: Cửa Hậu Khóa Account (OAuth Lockout Bypass):** Khách bị Admin KHÓA (`activated=false`) nhưng khi bấm Login = Google thì lại vượt rào vào như chốn không người.
* **Bug 72: Cỗ Máy Hút Máu Đám Mây (Cloud Billing Array DoS):** API Up Ảnh nhận mảng files vô hạn. Đẩy 5,000 files rác 1 lúc tiêu diệt sạch Quota miễn phí bên Cloudinary.
* **Bug 73: Giấy Phép Bất Tử (Immortal Reset Tokens):** Token quên mật khẩu KHÔNG có Cột thời hạn (`Expiry`). Link quên pass từ 2 năm trước vẫn có giá trị thu hồi tài khoản.
* **Bug 74: Dashboard Xuyên Không (SQL Timezone Conflict):** Hàm lấy Tháng từ Server Mỹ (UTC), Khách ở VN (UTC+7). Doanh thu bị lệch sai tháng một cách ngớ ngẩn.
* **Bug 75: Vượt Rào Phân Trang (Deep Pagination DoS):** Rải request `?page=1_Tỷ` bắt SQL phải lội đĩa cứng đếm pointer ảo (OFFSET khổng lồ) khiến CPU Database rục rã ngay lập tức.
* **Bug 76: Cơn Ác Mộng Máy Chấm Công (Orphaned Category Drops):** Xóa 1 danh mục sẽ kích hoạt Cascade xóa toàn bộ sản phẩm bên trong âm thầm, khách hàng bay luôn cửa hàng không lời báo trước.
* **Bug 77: Bức Tranh Tẩy Xóa (File Uploads CORS Missing):** API `/uploads/**` quên khai báo CORS. Frontend dùng Canvas xuất PDF thì trình duyệt Chrome cấm cửa chặn Security Origin Tainted.
* **Bug 78: Bom Lũ Băng Hủy (OOM Risk File GetBytes):** Hàm lưu file gọi `file.getBytes()` thay vì dùng cơ chế luồng `Stream/transferTo`. Nếu file 50MB, nó nhét nguyên khối cộm 50MB lên RAM máy chủ, dễ đánh sập bộ nhớ Node.
* **Bug 79: Rác Header Vô Nghĩa (Pointless JSON CSP):** Lệnh Content Security Policy (CSP) được gán gượng ép vào toàn bộ đường dẫn REST API JSON. Browser chỉ đọc CSP ở mã HTML, nên hành vi gửi Kèm CSP cho JSON là thừa thãi gây phí Bandwidth.
* **Bug 80: Sitemap Máy Chém Mạng (Unbounded XML Gen):** Điểm Endpoint `/sitemap.xml` bốc trọn 100% dữ liệu Repository trong 1 câu gọi làm tràn RAM Out-of-memory. Chuẩn phải sinh sitemap bằng luồng Stream từ DB lên file nén gzip.

***(Tài liệu này được tạo vào: 2026-04-12 để chốt sổ Chiến dịch Hardening)***
