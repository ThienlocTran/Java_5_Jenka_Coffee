# ☕ Jenka Coffee - Website Bán Máy Pha Cà Phê
> **Assignment môn Lập trình Java 5 (Spring Boot) - FPT Polytechnic**

![Java](https://img.shields.io/badge/Java-17+-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![Bootstrap](https://img.shields.io/badge/Bootstrap-5-563D7C?style=for-the-badge&logo=bootstrap&logoColor=white)
![Thymeleaf](https://img.shields.io/badge/Thymeleaf-Template-005F0F?style=for-the-badge&logo=thymeleaf&logoColor=white)

## 📖 Giới thiệu (Overview)
**Jenka Coffee** là dự án xây dựng website thương mại điện tử chuyên kinh doanh các dòng máy pha cà phê cao cấp. Dự án được phát triển dựa trên nền tảng **Spring Boot** kết hợp với **Thymeleaf** và **Bootstrap 5**, đáp ứng các tiêu chuẩn về giao diện hiện đại (Swiss Style) và trải nghiệm người dùng tối ưu.

## 👥 Thành viên nhóm (Team Members)

| STT | Thành viên                 | Vai trò (Role)        | Nhiệm vụ chính |
|:---:|:---------------------------|:----------------------|:---|
| 1 | **Trần Thiên Lộc**         | 👑 Leader / Fullstack | Setup dự án, Layout UI, Trang chủ, Core Admin |
| 2 | **Võ Xuân Phú**            | Backend / Frontend    | Giỏ hàng, Thanh toán, Quản lý Đơn hàng & SP |
| 3 | **Đặng Nguyễn Thiên Ngọc** | Backend / Frontend    | Quản lý Danh mục, Trang tĩnh, Xử lý lỗi |
| 4 | **Nguyễn Tứ Văn**          | Backend / Frontend    | Đăng ký, Đăng nhập, Quản lý Tài khoản (Auth) |

## 🛠️ Công nghệ sử dụng (Tech Stack)

* **Backend:** Java 17, Spring Boot (Spring MVC, Spring Data JPA, Spring Security).
* **Frontend:** HTML5, CSS3, Bootstrap 5, Thymeleaf Engine.
* **Database:** SQL Server 2012+.
* **Tools:** IntelliJ IDEA / STS, Maven, Git/GitHub.
* **External Services:** Cloudinary (Lưu trữ ảnh), JavaMailSender (Gửi email).

## ✨ Chức năng chính (Features)

### 🛒 Dành cho Khách hàng (User Site)
- [ ] **Trang chủ:** Hiển thị banner, sản phẩm nổi bật, xem nhanh (Quick View Modal).
- [ ] **Sản phẩm:** Xem danh sách, lọc theo loại/giá, xem chi tiết sản phẩm.
- [ ] **Mua hàng:** Thêm vào giỏ hàng, cập nhật số lượng, Mini-cart dropdown.
- [ ] **Thanh toán:** Đặt hàng (Checkout) và xem lịch sử đơn hàng.
- [ ] **Tài khoản:** Đăng ký, Đăng nhập, Quản lý hồ sơ, Đổi mật khẩu.

### 🛡️ Dành cho Quản trị viên (Admin Dashboard)
- [ ] **Thống kê:** Dashboard tổng quan, báo cáo doanh thu, khách hàng VIP.
- [ ] **Quản lý Hàng hóa:** CRUD Sản phẩm (kèm upload ảnh), CRUD Loại hàng.
- [ ] **Quản lý Đơn hàng:** Xem danh sách đơn, cập nhật trạng thái (Duyệt/Hủy).
- [ ] **Quản lý Tài khoản:** Phân quyền, khóa/mở khóa tài khoản người dùng.

## 🚀 Hướng dẫn cài đặt (Installation)

Để chạy dự án trên máy cục bộ (Localhost), vui lòng làm theo các bước sau:

**Bước 1: Clone dự án**
```bash
git clone [https://github.com/ThienlocTran/Java_5_Jenka_Coffee.git](https://github.com/ThienlocTran/Java_5_Jenka_Coffee.git)
cd Java_5_Jenka_Coffee

Bước 2: Cấu hình Cơ sở dữ liệu

Mở SQL Server, tạo Database tên: JenkaCoffeeDB.

Chạy file script database.sql (nằm trong thư mục sql/) để tạo bảng và dữ liệu mẫu.

Mở file src/main/resources/application.properties và cập nhật thông tin kết nối:

spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=JenkaCoffeeDB;encrypt=true;trustServerCertificate=true
spring.datasource.username=sa
spring.datasource.password=YOUR_PASSWORD


Bước 3: Chạy dự án

Cách 1: Mở dự án bằng IntelliJ IDEA -> Chạy file JenkaCoffeeApplication.java.

Cách 2: Dùng dòng lệnh Maven:

mvn spring-boot:run

Bước 4: Truy cập

Website: http://localhost:8080/

Admin: http://localhost:8080/admin/dashboard

📂 Cấu trúc thư mục (Project Structure)
src/main/resources/
├── static/              # Chứa file tĩnh (CSS, JS, Images cứng)
├── templates/           # Chứa file giao diện HTML (Thymeleaf)
│   ├── admin/           # Giao diện trang quản trị
│   ├── site/            # Giao diện trang khách hàng
│   ├── layout.html      # Layout chung User
│   └── admin-layout.html# Layout chung Admin
└── application.properties # Cấu hình dự án