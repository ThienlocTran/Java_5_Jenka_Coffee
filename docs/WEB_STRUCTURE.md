# Cấu trúc trang web Jenka Coffee

## 🌐 PHẦN KHÁCH HÀNG (SITE)

### • Trang chủ
- Hiển thị banner, sản phẩm nổi bật
- Giới thiệu về quán cafe
- Tin tức mới nhất

### • Tìm kiếm
- Tìm kiếm sản phẩm theo tên
- Lọc theo danh mục
- Lọc theo khoảng giá
- Sắp xếp (giá, tên, mới nhất)

### • Danh sách sản phẩm
- Hiển thị tất cả sản phẩm
- Phân trang
- Xem nhanh sản phẩm (Quick View)
- Thêm vào giỏ hàng

### • Chi tiết sản phẩm
- Thông tin chi tiết sản phẩm
- Hình ảnh sản phẩm
- Mô tả, giá cả
- Số lượng tồn kho
- Thêm vào giỏ hàng

### • Giỏ hàng
- Xem danh sách sản phẩm trong giỏ
- Cập nhật số lượng
- Xóa sản phẩm
- Tính tổng tiền
- Áp dụng mã giảm giá

### • Thanh toán
- Nhập thông tin giao hàng
- Chọn phương thức thanh toán (COD, VNPay, Momo)
- Sử dụng điểm tích lũy
- Xác nhận đơn hàng

### • Đăng nhập
- Đăng nhập bằng username/password
- Đăng nhập bằng email
- Đăng nhập bằng số điện thoại
- Ghi nhớ đăng nhập

### • Đăng ký
- Đăng ký tài khoản mới
- Xác thực qua email
- Xác thực qua OTP điện thoại
- Kiểm tra username/email trùng lặp

### • Quên mật khẩu
- Gửi yêu cầu reset mật khẩu
- Nhận link/OTP qua email/phone
- Đặt lại mật khẩu mới

### • Thông tin người dùng
- Xem thông tin cá nhân
- Cập nhật thông tin (tên, email, phone, ảnh)
- Đổi mật khẩu
- Xem điểm tích lũy và hạng thành viên

### • Lịch sử đơn hàng
- Xem danh sách đơn hàng đã đặt
- Chi tiết từng đơn hàng
- Trạng thái đơn hàng
- Hủy đơn hàng (nếu chưa xác nhận)

### • Lịch sử điểm thưởng
- Xem lịch sử tích điểm
- Xem lịch sử sử dụng điểm
- Hạng thành viên hiện tại

### • Đặt lịch dịch vụ
- Đặt lịch sửa chữa máy pha cafe
- Đặt lịch tư vấn
- Chọn thời gian ưu tiên
- Mô tả yêu cầu

### • Tin tức
- Danh sách tin tức
- Chi tiết tin tức
- Tin tức khuyến mãi
- Hướng dẫn pha chế

### • Liên hệ
- Form liên hệ
- Thông tin quán (địa chỉ, phone, email)
- Bản đồ
- Gửi câu hỏi/góp ý

---

## 👨‍💼 PHẦN QUẢN TRỊ (ADMIN)

### • Trang chủ Admin
- Tổng quan hệ thống
- Thống kê nhanh
- Biểu đồ doanh thu
- Đơn hàng mới

### • Dashboard
- Thống kê tổng quan
  - Tổng doanh thu
  - Số đơn hàng
  - Số khách hàng
  - Số sản phẩm
- Biểu đồ doanh thu theo thời gian
- Sản phẩm bán chạy
- Đơn hàng gần đây
- Top khách hàng

### • Quản lý sản phẩm
- Danh sách sản phẩm (phân trang, tìm kiếm)
- Thêm sản phẩm mới
- Sửa thông tin sản phẩm
- Xóa sản phẩm
- Bật/tắt trạng thái sản phẩm
- Upload ảnh sản phẩm
- Quản lý tồn kho
- Cảnh báo hết hàng

### • Quản lý danh mục
- Danh sách danh mục
- Thêm danh mục mới
- Sửa danh mục
- Xóa danh mục (nếu không có sản phẩm)
- Upload icon danh mục
- Kiểm tra số lượng sản phẩm theo danh mục

### • Quản lý đơn hàng
- Danh sách đơn hàng
- Lọc theo trạng thái (Mới, Đã xác nhận, Đang giao, Hoàn thành, Đã hủy)
- Chi tiết đơn hàng
- Cập nhật trạng thái đơn hàng
- Hủy đơn hàng
- In hóa đơn
- Xuất báo cáo đơn hàng

### • Quản lý người dùng
- Danh sách tài khoản
- Thêm tài khoản mới
- Sửa thông tin tài khoản
- Xóa tài khoản
- Khóa/Mở khóa tài khoản
- Bật/tắt trạng thái kích hoạt
- Reset mật khẩu cho user
- Xem lịch sử mua hàng của user
- Xem điểm tích lũy của user

### • Quản lý voucher
- Danh sách mã giảm giá
- Thêm voucher mới
- Sửa voucher
- Xóa voucher
- Bật/tắt voucher
- Thiết lập điều kiện:
  - Loại giảm giá (cố định/phần trăm)
  - Số tiền giảm
  - Đơn hàng tối thiểu
  - Số lượng voucher
  - Ngày hết hạn

### • Quản lý đặt lịch
- Danh sách yêu cầu đặt lịch
- Xem chi tiết yêu cầu
- Cập nhật trạng thái (Chờ xác nhận, Đã xác nhận, Hoàn thành, Đã hủy)
- Lọc theo trạng thái
- Ghi chú cho yêu cầu

### • Quản lý tin tức
- Danh sách tin tức
- Thêm tin tức mới
- Sửa tin tức
- Xóa tin tức
- Bật/tắt hiển thị
- Upload ảnh tin tức
- Soạn thảo nội dung (Rich text editor)

### • Báo cáo doanh thu
- Doanh thu theo ngày/tuần/tháng/năm
- Biểu đồ doanh thu
- So sánh doanh thu các kỳ
- Xuất báo cáo Excel/PDF
- Lọc theo khoảng thời gian

### • Báo cáo sản phẩm
- Sản phẩm bán chạy nhất
- Sản phẩm tồn kho nhiều
- Sản phẩm sắp hết hàng
- Sản phẩm chưa bán được
- Thống kê theo danh mục

### • Báo cáo khách hàng
- Top khách hàng mua nhiều nhất
- Khách hàng mới
- Khách hàng theo hạng (Member, Silver, Gold, Diamond)
- Phân tích hành vi mua hàng

### • Báo cáo thanh toán
- Thống kê theo phương thức thanh toán
- Doanh thu COD
- Doanh thu VNPay
- Doanh thu Momo
- Tỷ lệ thanh toán thành công

### • Thống kê
- Tổng quan hệ thống
- Biểu đồ tăng trưởng
- Phân tích xu hướng
- Dự báo doanh thu

---

## 🔐 TÍNH NĂNG BẢO MẬT

### • Xác thực
- Đăng nhập session-based
- Mã hóa mật khẩu (BCrypt)
- Token activation (email)
- OTP verification (phone)
- Remember me
- Auto logout sau thời gian không hoạt động

### • Phân quyền
- Role USER: Truy cập phần khách hàng
- Role ADMIN: Truy cập phần quản trị
- Kiểm tra quyền truy cập mỗi request
- Trang unauthorized khi không có quyền

### • Bảo vệ
- CSRF protection
- XSS prevention
- SQL injection prevention
- Session fixation protection
- Secure password reset

---

## 🎨 GIAO DIỆN

### • Responsive Design
- Desktop
- Tablet
- Mobile

### • Theme
- Light mode (mặc định)
- Màu sắc chủ đạo: Nâu cafe
- Font chữ dễ đọc
- Icons Material Design

### • Components
- Navigation bar
- Footer
- Breadcrumb
- Pagination
- Modal/Dialog
- Toast notification
- Loading spinner
- Image gallery
- Product card
- Cart badge

---

## 🌍 ĐA NGÔN NGỮ

### • Tiếng Việt (mặc định)
- Giao diện tiếng Việt
- Thông báo tiếng Việt
- Email tiếng Việt

### • English
- English interface
- English notifications
- English emails

---

## 📱 TÍCH HỢP

### • Thanh toán
- COD (Thanh toán khi nhận hàng)
- VNPay (Cổng thanh toán điện tử)
- Momo (Ví điện tử)

### • Upload ảnh
- Cloudinary (Cloud storage)
- Local storage (Fallback)

### • Email
- Gửi email kích hoạt tài khoản
- Gửi email reset mật khẩu
- Gửi email xác nhận đơn hàng
- Gửi email thông báo

### • SMS
- Gửi OTP xác thực
- Gửi thông báo đơn hàng

---

## 📊 HỆ THỐNG ĐIỂM THƯỞNG

### • Tích điểm
- Mua hàng được tích điểm (1% giá trị đơn)
- Điểm sinh nhật
- Điểm sự kiện đặc biệt

### • Sử dụng điểm
- Quy đổi điểm thành tiền
- Giảm giá khi thanh toán
- 100 điểm = 1,000 VNĐ

### • Hạng thành viên
- MEMBER: 0 - 999 điểm
- SILVER: 1,000 - 4,999 điểm
- GOLD: 5,000 - 9,999 điểm
- DIAMOND: 10,000+ điểm

### • Ưu đãi theo hạng
- MEMBER: Tích điểm cơ bản
- SILVER: Tích điểm x1.2, giảm 5%
- GOLD: Tích điểm x1.5, giảm 10%
- DIAMOND: Tích điểm x2, giảm 15%
