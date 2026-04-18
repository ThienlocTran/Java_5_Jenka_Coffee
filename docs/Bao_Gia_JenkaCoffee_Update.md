| STT | Tính năng | Mô tả chi tiết kỹ thuật | Giá trị (VNĐ) | Demo URL |
| :--- | :--- | :--- | :--- | :--- |
| **A** | **GIAO DIỆN KHÁCH HÀNG** | | | |
| A1 | Trang chủ động | Banner slider 4 hiệu ứng (fade/slide/zoom/ken burns), danh mục icon, sản phẩm nổi bật, tin tức. | 1.200.000 | `/` |
| A2 | Catalog sản phẩm | Lọc theo danh mục & giá, sort 4 kiểu (mới nhất/giá tăng/giá giảm/A-Z), phân trang. | 1.000.000 | `/product/list` |
| A3 | Chi tiết sản phẩm | Ảnh, mô tả, badge còn/hết hàng, thêm giỏ hàng, sản phẩm tương tự dạng carousel cuộn. | 600.000 | `/san-pham/:slug` |
| A4 | Giỏ hàng thông minh | Thêm/sửa/xóa không reload trang, animation bay vào giỏ, mini cart dropdown trên navbar. | 800.000 | `/cart` |
| A5 | Thanh toán | Form địa chỉ Tỉnh/Huyện/Xã chuẩn hóa 63 tỉnh, validate UX scroll-to-field, 3 phương thức thanh toán. | 1.200.000 | `/checkout` |
| A6 | Tài khoản người dùng | Đăng ký/đăng nhập bằng username/email/SĐT, xác thực OTP qua email, quên/reset mật khẩu. | 1.000.000 | `/auth/login` |
| A7 | Hồ sơ cá nhân | Cập nhật thông tin, đổi mật khẩu, upload/cắt ảnh đại diện đồng bộ lên Cloudinary. | 600.000 | `/profile` |
| A8 | Lịch sử đơn hàng | Xem danh sách đơn, trạng thái realtime, phân trang. | 400.000 | `/orders` |
| A9 | Tin tức & Blog | Danh sách bài viết, trang chi tiết, phân trang. | 400.000 | `/news` |
| A10 | Đặt lịch sửa chữa | Form đặt lịch kỹ thuật viên (tên, SĐT, ngày giờ, mô tả lỗi máy), hệ thống ghi nhận tức thì. | 400.000 | `/booking` |
| A11 | Liên hệ | Form gửi tin nhắn trực tiếp đến ban quản trị shop. | 200.000 | `/contact` |
| A12 | Responsive Mobile | Mobile menu drawer đầy đủ, layout co giãn toàn bộ mượt mà trên mọi thiết bị di động/tablet. | 600.000 | |
| | | **Tổng Nhóm A:** | **8.400.000** | |
| **B** | **QUẢN TRỊ ADMIN (ERP MINI)** | | | |
| B1 | Dashboard tổng quan | 4 thẻ KPI (doanh thu/đơn/sản phẩm/khách), biểu đồ doanh thu theo tháng, top 5 sản phẩm bán chạy. | 1.000.000 | `/admin/dashboard` |
| B2 | Quản lý sản phẩm | Thêm/sửa/xóa (CRUD), upload ảnh tự động nén lên Cloudinary, bật/tắt hiển thị, phân trang. | 1.000.000 | `/admin/products` |
| B3 | Quản lý danh mục | Thêm/sửa/xóa danh mục, hệ thống tự động kiểm tra ID trùng lặp. | 400.000 | `/admin/categories` |
| B4 | Quản lý đơn hàng | Theo dõi danh sách đơn hàng và cập nhật trạng thái cơ bản (Đang giao / Đã giao), xem chi tiết. | 900.000 | `/admin/orders` |
| B5 | Quản lý tài khoản | CRUD, khóa/mở khóa tài khoản, reset mật khẩu, kiểm soát avatar. | 600.000 | `/admin/accounts` |
| B6 | Quản lý tin tức | Thêm/sửa/xóa bài viết, upload ảnh, bật/tắt hiển thị. | 400.000 | `/admin/news` |
| B7 | Quản lý banner | Tạo bộ banner, upload nhiều ảnh cùng lúc, 4 hiệu ứng chuyển cảnh, kích hoạt lên trang chủ. | 1.200.000 | `/admin/banners` |
| B8 | Quản lý lịch hẹn | Danh sách yêu cầu sửa chữa, cập nhật trạng thái (Chờ/Xác nhận/Hoàn tất/Hủy), phân trang. | 400.000 | `/admin/booking` |
| B9 | Báo cáo doanh thu | Filter tuần/tháng/quý/năm, 3 dạng biểu đồ, dual-axis doanh thu + số đơn, bảng chi tiết. | 1.000.000 | `/admin/reports/revenue` |
| B10 | Báo cáo khách VIP | Top 10 khách chi tiêu nhiều nhất. | 400.000 | `/admin/reports/vip` |
| | | **Tổng Nhóm B:** | **7.300.000** | |
| **C** | **NỀN TẢNG, BẢO MẬT & HẠ TẦNG CỐT LÕI** | | | |
| C1 | Kiến trúc REST API | Spring Boot 3, layered architecture, DTO pattern, exception handling tập trung chuẩn doanh nghiệp. | 800.000 | *(Hệ thống ngầm)* |
| C2 | Bảo mật hệ thống | Session-based auth, route guard, phân quyền admin/user, chống injection (XSS/SQLi), validation. | 600.000 | *(Hệ thống ngầm)* |
| C3 | Hệ thống Email | Kích hoạt tài khoản, reset mật khẩu, thông báo hẹn ngày cho admin qua Gmail SMTP tự động. | 600.000 | *(Hệ thống ngầm)* |
| C4 | Lưu trữ & Xử lý Ảnh | Tích hợp Cloudinary, tự động nén ảnh, thiết lập preset riêng cho avatar và sản phẩm. | 400.000 | *(Hệ thống ngầm)* |
| C5 | Deploy Production | Cấu hình đưa hệ thống lên mạng: Railway (backend) + Vercel (frontend), xử lý bảo mật kết nối. | 400.000 | *(Hệ thống ngầm)* |
| C6 | Tối ưu hóa Tìm kiếm | Thiết lập Web tĩnh (Prerender) và tạo tự động Sitemap chạy ngầm cho bot Google thu thập. | 1.000.000 | *(Hệ thống ngầm)* |
| | | **Tổng Nhóm C:** | **3.800.000** | |
| | | **TỔNG A + B + C =** | **19.500.000** | |

---

### BẢNG COMBO TỔNG KẾT
| Tên Gói | Các nhóm tính năng bao gồm | Giá Gói (VNĐ) |
| :--- | :--- | :--- |
| **1. GÓI KINH DOANH** | Đầy đủ Nhóm **A + B + C** (Trừ 500k khuyến mại triển khai) | **19.000.000** |
| **2. GÓI TỰ ĐỘNG HÓA** | Đầy đủ Gói Kinh Doanh + Tích hợp bot tự động thanh toán (SePay/VNPay) 2.000.000đ | **21.000.000** |
