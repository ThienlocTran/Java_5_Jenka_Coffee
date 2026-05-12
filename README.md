# Jenka Coffee

Jenka Coffee la he thong website ban may pha ca phe, may xay ca phe va phu kien pha che. Source du an gom hai ung dung chinh:

- Backend: Spring Boot 3, Java 17, PostgreSQL.
- Frontend: Vue 3, Vite, Tailwind CSS.

Du an tap trung vao luong ban hang thuc te cua Jenka Coffee: khach dat don, admin xac nhan hoac huy don, sau do shop lien he khach de trao doi chi tiet ve giao hang, thanh toan va tinh trang san pham.

## Cau Truc Thu Muc

```text
D:\JenkaCoffee
+-- Java_5_Jenka_Coffee/                    # Backend Spring Boot
|   +-- src/main/java/com/springboot/jenka_coffee/
|   |   +-- api/                            # REST controllers public va admin
|   |   +-- config/                         # Security, CORS, cache, Cloudinary
|   |   +-- dto/                            # Request/response DTO
|   |   +-- entity/                         # JPA entities
|   |   +-- repository/                     # Spring Data repositories
|   |   +-- security/                       # JWT service/filter
|   |   +-- service/                        # Business services
|   |   +-- util/                           # Slug, image, password helpers
|   +-- src/main/resources/
|   |   +-- application.properties          # Local config, khong day len git
|   |   +-- db/                             # SQL bootstrap/fix scripts
|   +-- pom.xml
+-- front_end_Jenka_Coffee/jenka-coffee-ui/ # Frontend Vue/Vite
|   +-- src/
|   |   +-- components/
|   |   +-- composables/
|   |   +-- layouts/
|   |   +-- router/
|   |   +-- services/
|   |   +-- utils/
|   |   +-- views/
|   +-- public/
|   +-- package.json
|   +-- vite.config.js
+-- docs/                                   # Test cases va execution results
+-- doc_testcase/                           # Audit/fix notes
+-- deploy/                                 # Script deploy VPS/service
+-- Test Defect/                            # Ket qua test defect
```

## Cong Nghe

Backend:

- Java 17
- Spring Boot 3.2.4
- Spring Security
- Spring Data JPA
- PostgreSQL
- JWT via `jjwt`
- Bucket4j va Caffeine cho rate limiting/cache
- Cloudinary SDK
- Gmail SMTP
- Apache Tika va OWASP Java HTML Sanitizer

Frontend:

- Vue 3
- Vite 7
- Vue Router
- Pinia
- Axios
- Tailwind CSS
- Chart.js
- GSAP, Swiper
- `@vueuse/head` cho SEO meta

## Tinh Nang Hien Co

### Khach Hang

- Trang chu voi banner, danh muc icon, san pham noi bat va tin tuc.
- Danh sach san pham `/product/list` voi loc danh muc, loc gia, sap xep va phan trang.
- Chi tiet san pham `/san-pham/:slug`, anh san pham, mo ta, trang thai con/het, san pham lien quan.
- Gio hang `/cart`: them, sua so luong, xoa, mini cart va hieu ung them vao gio.
- Thanh toan `/checkout`: form thong tin khach, dia chi 63 tinh/thanh, validate UI, chon `cod`, `bank`, `momo`.
- Tai khoan: dang ky, dang nhap bang username/email/so dien thoai, Google OAuth, OTP, quen/reset mat khau.
- Ho so ca nhan `/profile`: cap nhat thong tin, doi mat khau, avatar Cloudinary.
- Lich su don hang `/orders` va chi tiet don theo public order code.
- Tin tuc `/news` va chi tiet bai viet.
- Lien he `/contact`.
- Feedback popup danh gia cua hang.
- SEO: canonical URL, sitemap, prerender script va robots.txt.

### Admin

- Dashboard `/admin/dashboard`: KPI, doanh thu theo thang, don moi, top san pham ban chay.
- San pham `/admin/products`: CRUD, upload anh, nhieu anh, bat/tat hien thi, san pham noi bat.
- Danh muc `/admin/categories`: CRUD va kiem tra ID trung lap.
- Don hang `/admin/orders`: danh sach, chi tiet, xac nhan don, huy don.
- Tai khoan `/admin/accounts`: CRUD, khoa/mo khoa, reset mat khau, kiem soat avatar.
- Tin tuc `/admin/news`: CRUD, upload anh, bat/tat hien thi.
- Banner `/admin/banners`: bo banner, upload nhieu anh, hieu ung, kich hoat bo banner.
- Lien he `/admin/contacts`: xem tin nhan, danh dau da doc.
- Feedback `/admin/feedbacks`: xem va xoa feedback.
- Bao cao doanh thu `/admin/reports/revenue`: loc theo tuan/thang/quy/nam, bieu do va bang chi tiet.
- Notification count cho don moi va lien he moi.

### Bao Mat Va Nen Tang

- JWT access/refresh token qua HttpOnly cookie, co fallback Authorization header cho dev cross-origin.
- Blacklist token khi logout.
- Kiem tra quyen admin tu server, khong tin localStorage.
- Rate limit cho login, forgot password, OTP, checkout, contact, feedback, visitor ping va mot so public/admin endpoints.
- Security headers va CSP.
- Validate DTO bang Bean Validation.
- XSS protection cho cac field text nhay cam.
- Upload image co validate kich thuoc, dinh dang, magic bytes.
- JPA parameterized query, khong noi chuoi SQL tuy tien.

## Pham Vi Khong Lam

Nhung muc duoi day da duoc loai khoi pham vi hien tai, khong tinh la bug hay thieu tinh nang:

- Booking/dat lich sua chua `/booking`, `/admin/booking`.
- Voucher/ma giam gia/flash sale.
- Bao cao khach VIP `/admin/reports/vip`.
- Cong thanh toan tu dong SePay/VNPay/MoMo webhook. Hien tai chi ghi nhan phuong thuc thanh toan de shop lien he xac nhan.
- Quan ly ton kho bat buoc. San pham co the hien nut lien he thay vi them gio hang khi can.
- Phi van chuyen tu dong. Shop se trao doi voi khach sau khi xac nhan don.
- Theo doi giao hang chi tiet. Trang thai don chi gom `Cho xac nhan`, `Da xac nhan`, `Da huy`.

## Trang Thai Don Hang

Backend va frontend dung chung mapping:

```text
0 = Cho xac nhan
1 = Da xac nhan
2 = Da huy
```

Ly do nghiep vu: san pham gia tri cao, shop can goi dien xac nhan lai voi khach truoc khi chot giao hang/thanh toan.

## Yeu Cau Moi Truong

Backend:

- JDK 17
- Maven hoac Maven Wrapper
- PostgreSQL/Neon database

Frontend:

- Node.js theo `package.json`: `^20.19.0 || >=22.12.0`
- npm

## Cau Hinh Backend

`Java_5_Jenka_Coffee/src/main/resources/application.properties` la file local config va khong nen day len git. Khi deploy production, cau hinh bang environment variables.

Cac bien quan trong:

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://...
SPRING_DATASOURCE_USERNAME=...
SPRING_DATASOURCE_PASSWORD=...
JWT_SECRET=...
CLOUDINARY_CLOUD_NAME=...
CLOUDINARY_API_KEY=...
CLOUDINARY_API_SECRET=...
SPRING_MAIL_USERNAME=...
SPRING_MAIL_PASSWORD=...
ADMIN_EMAIL=...
APP_BASE_URL=https://jenkacoffee.com
APP_CORS_ALLOWED_ORIGINS=https://jenkacoffee.com
GOOGLE_OAUTH_CLIENT_ID=...
GOOGLE_OAUTH_CLIENT_SECRET=...
VERCEL_DEPLOY_HOOK_URL=...
```

Luu y:

- Khong commit secret that len git.
- Neu file config tung bi day len remote, can rotate secret.
- `spring.jpa.hibernate.ddl-auto=validate` duoc dung de tranh Hibernate tu y sua schema production.

## Chay Backend

```powershell
cd D:\JenkaCoffee\Java_5_Jenka_Coffee
.\mvnw.cmd spring-boot:run
```

Neu may co Maven global:

```powershell
cd D:\JenkaCoffee\Java_5_Jenka_Coffee
mvn spring-boot:run
```

Backend mac dinh chay o:

```text
http://localhost:8080
```

## Chay Frontend

```powershell
cd D:\JenkaCoffee\front_end_Jenka_Coffee\jenka-coffee-ui
npm install
npm.cmd run dev
```

Frontend dev server mac dinh:

```text
http://localhost:5173
```

Build production:

```powershell
cd D:\JenkaCoffee\front_end_Jenka_Coffee\jenka-coffee-ui
npm.cmd run build
```

Build kem prerender SEO:

```powershell
npm.cmd run build:seo
```

## API Chinh

Public:

```text
GET  /api/products
GET  /api/products/slug/{slug}
GET  /api/products/{id}/images
GET  /api/categories
GET  /api/news
GET  /api/news/{id}
GET  /api/banners/active
POST /api/contact/send
POST /api/feedbacks
POST /api/visitors/ping
GET  /sitemap.xml
```

Auth:

```text
POST  /api/auth/login
POST  /api/auth/logout
POST  /api/auth/refresh
POST  /api/auth/signup
POST  /api/auth/activate
POST  /api/auth/forgot-password
POST  /api/auth/reset-password
POST  /api/auth/verify-otp
POST  /api/auth/resend-otp
POST  /api/auth/google-login
GET   /api/auth/me
PATCH /api/auth/update-phone
GET   /api/auth/check-remember
```

Customer:

```text
GET    /api/cart
POST   /api/cart/add/{id}
PUT    /api/cart/update/{id}
DELETE /api/cart/remove/{id}
DELETE /api/cart/clear

GET  /api/orders/checkout-info
POST /api/orders/checkout
GET  /api/orders
GET  /api/orders/{orderCode}

GET  /api/profile
PUT  /api/profile
POST /api/profile/avatar
POST /api/profile/change-password
```

Admin:

```text
GET    /api/admin/dashboard
GET    /api/admin/dashboard/revenue
GET    /api/admin/notifications/counts

GET    /api/admin/products
POST   /api/admin/products
PUT    /api/admin/products/{id}
POST   /api/admin/products/{id}
PUT    /api/admin/products/{id}/toggle
PUT    /api/admin/products/{id}/toggle-featured
PUT    /api/admin/products/{id}/featured-position
GET    /api/admin/products/inventory
DELETE /api/admin/products/{id}

GET    /api/admin/categories
POST   /api/admin/categories
PUT    /api/admin/categories/{id}
DELETE /api/admin/categories/{id}

GET    /api/admin/orders
GET    /api/admin/orders/{id}
PUT    /api/admin/orders/{id}/status/{status}
POST   /api/admin/orders/{id}/cancel
DELETE /api/admin/orders/{id}

GET    /api/admin/accounts
POST   /api/admin/accounts
PUT    /api/admin/accounts/{username}
DELETE /api/admin/accounts/{username}
PUT    /api/admin/accounts/{username}/toggle-status
PUT    /api/admin/accounts/{username}/lock
PUT    /api/admin/accounts/{username}/unlock
PUT    /api/admin/accounts/{username}/reset-password

GET    /api/admin/news
POST   /api/admin/news
PUT    /api/admin/news/{id}
PUT    /api/admin/news/{id}/toggle
DELETE /api/admin/news/{id}

GET    /api/admin/banners
POST   /api/admin/banners
PUT    /api/admin/banners/{id}/meta
POST   /api/admin/banners/{id}/images
DELETE /api/admin/banners/images/{imageId}
PUT    /api/admin/banners/{id}/activate
DELETE /api/admin/banners/{id}

GET    /api/admin/contacts
PATCH  /api/admin/contacts/{id}/read
PATCH  /api/admin/contacts/mark-all-read

GET    /api/admin/feedbacks
DELETE /api/admin/feedbacks/{id}

GET    /api/admin/reports/revenue/monthly
GET    /api/admin/reports/revenue/yearly
GET    /api/admin/reports/customers/top
GET    /api/admin/reports/stats/overview
```

## Kiem Thu

Frontend:

```powershell
cd D:\JenkaCoffee\front_end_Jenka_Coffee\jenka-coffee-ui
npm.cmd run build
```

Backend:

```powershell
cd D:\JenkaCoffee\Java_5_Jenka_Coffee
.\mvnw.cmd test
```

Neu Maven Wrapper loi tren Windows, cai Maven global hoac sua moi truong PowerShell/Java truoc khi chay:

```powershell
mvn test
```

Ket qua test case va defect hien co nam trong:

```text
docs/test_cases/
docs/test_cases/execution_results/
doc_testcase/
Test Defect/
```

## Deploy

Frontend:

- Vercel.
- Build command: `npm run build` hoac `npm run build:seo`.
- Output directory: `dist`.
- Production API URL cau hinh qua `VITE_API_BASE_URL`.

Backend:

- Railway hoac VPS.
- JAR Spring Boot chay port `8080`.
- PostgreSQL/Neon.
- Cloudinary cho anh.
- Gmail SMTP cho email OTP/reset/don hang.

Script ho tro nam trong:

```text
deploy/deploy.sh
deploy/setup-nginx.sh
deploy/setup-service.sh
deploy/setup-vps.sh
```

## Luu Y Van Hanh

- Chi deploy voi origin hop le trong `APP_CORS_ALLOWED_ORIGINS`.
- Neu chi dung mot domain `jenkacoffee.com`, CSRF risk thap hon. Neu sau nay tach subdomain cho app/api/blog/admin, nen bat CSRF token hoac chinh lai SameSite/CORS.
- Thanh toan bank/momo hien la thong tin de shop lien he, chua co webhook xac minh giao dich.
- Khong coi nut dat hang la cam ket giao hang ngay; admin can xac nhan don truoc.
- Khong xoa cac test/audit docs neu con dung de doi chieu nghiep vu.

## Lien He Du An

- Website: https://jenkacoffee.com
- Backend production du kien: Railway
- Frontend production du kien: Vercel
- Database: PostgreSQL/Neon
- Storage: Cloudinary
