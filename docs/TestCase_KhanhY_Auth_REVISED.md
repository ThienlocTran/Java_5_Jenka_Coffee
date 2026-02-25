# TEST CASES - KHÁNH Ý (Authentication & Account Module)

**Thành viên:** Khánh Ý  
**Module:** Authentication, Authorization & Account Management  
**Tổng số Test Cases:** 36  
**Phiên bản:** 2.0 (Revised)

---

## 📋 DANH SÁCH TEST CASES

### A. AUTHENTICATION (15 TCs)

| ID | Tên Test Case | Điều kiện tiên quyết | Các bước thực hiện | Dữ liệu đầu vào | Kết quả mong đợi |
|---|---|---|---|---|---|
| TC_AUTH_001 | Đăng nhập bằng username đúng | - Account "admin" tồn tại<br>- Password: "123"<br>- Activated = true | 1. Vào `/auth/login`<br>2. Nhập username và password<br>3. Click "Đăng nhập" | **Username:** admin<br>**Password:** 123 | - Method: `accountService.authenticate("admin", "123")`<br>- BCrypt verify password thành công<br>- Session: setAttribute("user", account)<br>- Redirect: `/admin` (nếu admin=true)<br>- Hoặc `/home` (nếu admin=false) |
| TC_AUTH_002 | Đăng nhập bằng email đúng | - Account có email "user@gmail.com"<br>- Password: "123" | 1. Nhập email thay vì username<br>2. Nhập password<br>3. Click "Đăng nhập" | **Identifier:** user@gmail.com<br>**Password:** 123 | - Method: `findByUsernameOrEmailOrPhone("user@gmail.com")`<br>- Tìm thấy account qua email<br>- Verify password thành công<br>- Đăng nhập thành công |
| TC_AUTH_003 | Đăng nhập bằng phone đúng | - Account có phone "0901234567"<br>- Password: "123" | 1. Nhập SĐT<br>2. Nhập password<br>3. Click "Đăng nhập" | **Identifier:** 0901234567<br>**Password:** 123 | - Method: `findByUsernameOrEmailOrPhone("0901234567")`<br>- Tìm thấy account qua phone<br>- Verify password thành công<br>- Đăng nhập thành công |
| TC_AUTH_004 | Đăng nhập sai password | - Account "admin" tồn tại<br>- Password đúng: "123" | 1. Nhập username đúng<br>2. Nhập password SAI<br>3. Click "Đăng nhập" | **Username:** admin<br>**Password:** wrongpass | - Method: `passwordSecurity.verifyPassword("wrongpass", hash)`<br>- BCrypt verify = false<br>- Return: null<br>- Flash error: "Sai tên đăng nhập hoặc mật khẩu!"<br>- Redirect về `/auth/login` |
| TC_AUTH_005 | Đăng nhập với user không tồn tại | - Username "khongco" KHÔNG tồn tại | 1. Nhập username không tồn tại<br>2. Click "Đăng nhập" | **Username:** khongco<br>**Password:** 123 | - Method: `findByUsernameOrEmailOrPhone("khongco")`<br>- Return: Optional.empty()<br>- Flash error: "Sai tên đăng nhập hoặc mật khẩu!"<br>- Không tiết lộ user không tồn tại (security) |
| TC_AUTH_006 | Đăng nhập với account chưa kích hoạt | - Account "newuser" tồn tại<br>- Activated = false | 1. Nhập username/password đúng<br>2. Click "Đăng nhập" | **Username:** newuser<br>**Password:** 123<br>**Activated:** false | - Tìm thấy account<br>- Check: `if (!account.getActivated())`<br>- Return: null<br>- Flash error: "Tài khoản chưa được kích hoạt. Vui lòng check email/SMS!"<br>- Link: "Gửi lại mã kích hoạt" |
| TC_AUTH_007 | Đăng nhập với account bị khóa | - Account "blocked" tồn tại<br>- Activated = false (bị admin khóa) | 1. Nhập username/password đúng<br>2. Click "Đăng nhập" | **Username:** blocked<br>**Activated:** false | - Check: `if (!account.getActivated())`<br>- Flash error: "Tài khoản đã bị khóa. Liên hệ admin!"<br>- **Note:** Không phân biệt "chưa kích hoạt" vs "bị khóa" |
| TC_AUTH_008 | Đăng nhập với Remember Me | - Account "user1" tồn tại | 1. Nhập username/password<br>2. Check "Ghi nhớ tôi"<br>3. Click "Đăng nhập"<br>4. Đóng browser<br>5. Mở lại browser | **Remember:** true | - Method: `cookieService.createRememberMeCookie(username, 30)`<br>- Cookie: name="rememberMe", value=username, maxAge=30 days<br>- Sau khi mở lại browser:<br>&nbsp;&nbsp;• Cookie vẫn còn<br>&nbsp;&nbsp;• Auto login (nếu có interceptor)<br>&nbsp;&nbsp;• Không cần login lại |
| TC_AUTH_009 | Đăng xuất | - Đang đăng nhập | 1. Click nút "Đăng xuất" | N/A | - Method: `session.invalidate()`<br>- Clear session: removeAttribute("user")<br>- Delete cookie: `cookieService.deleteRememberMeCookie()`<br>- Redirect: `/home`<br>- Flash message: "Đã đăng xuất thành công!" |
| TC_AUTH_010 | Đăng ký tài khoản thành công (Email) | - Username "user1" chưa tồn tại<br>- Email "user1@gmail.com" chưa tồn tại | 1. Vào `/auth/signup`<br>2. Điền đầy đủ thông tin<br>3. Click "Đăng ký" | **Username:** user1<br>**Fullname:** Nguyễn A<br>**Phone:** 0901234567<br>**Email:** user1@gmail.com<br>**Password:** 123456 | - Method: `accountService.register(...)`<br>- Hash password: BCrypt<br>- Set: activated=false, admin=false, points=0<br>- ActivationMethod = "EMAIL"<br>- Generate token (UUID)<br>- Gửi email kích hoạt<br>- Flash: "Đăng ký thành công! Check email để kích hoạt"<br>- Redirect: `/auth/login` |
| TC_AUTH_011 | Đăng ký tài khoản thành công (Phone/OTP) | - Username "user2" chưa tồn tại<br>- Email để trống hoặc NULL | 1. Điền form đăng ký<br>2. Không nhập email<br>3. Click "Đăng ký" | **Username:** user2<br>**Phone:** 0901234567<br>**Email:** [Trống] | - ActivationMethod = "PHONE"<br>- Generate OTP (6 số)<br>- Lưu OTP vào cache (expire 5 phút)<br>- Console log OTP (vì chưa có SMS service)<br>- Flash: "OTP đã được gửi đến SĐT"<br>- Redirect: `/auth/verify-otp?phone=0901234567` |
| TC_AUTH_012 | Đăng ký với username đã tồn tại | - Username "admin" đã tồn tại | 1. Nhập username = "admin"<br>2. Click "Đăng ký" | **Username:** admin | - Check: `existsByUsername("admin")` = true<br>- **Exception:** `ValidationException`<br>- Message: "Tên đăng nhập đã tồn tại!"<br>- Flash error<br>- Redirect về `/auth/signup` |
| TC_AUTH_013 | Đăng ký với email đã tồn tại | - Email "old@gmail.com" đã tồn tại | 1. Nhập email đã có<br>2. Click "Đăng ký" | **Email:** old@gmail.com | - Check: `existsByEmail("old@gmail.com")` = true<br>- **Exception:** `ValidationException`<br>- Message: "Email đã được sử dụng!"<br>- Flash error |
| TC_AUTH_014 | Đăng ký với password không khớp | - Đang ở form đăng ký | 1. Nhập Password = "123"<br>2. Nhập Confirm Password = "456"<br>3. Click "Đăng ký" | **Password:** 123<br>**Confirm:** 456 | - **Frontend validation:** JavaScript check<br>- Error: "Mật khẩu xác nhận không khớp!"<br>- Không submit form<br>- **Note:** Nếu chưa có → cần thêm |
| TC_AUTH_015 | Đăng ký với password quá ngắn | - Đang ở form đăng ký | 1. Nhập password < 6 ký tự<br>2. Click "Đăng ký" | **Password:** 123 | - **Validation:** Check length < 6<br>- Error: "Mật khẩu phải từ 6 ký tự trở lên"<br>- Không submit |

---

### B. ACCOUNT ACTIVATION & PASSWORD RESET (9 TCs)

| ID | Tên Test Case | Điều kiện tiên quyết | Các bước thực hiện | Dữ liệu đầu vào | Kết quả mong đợi |
|---|---|---|---|---|---|
| TC_PWD_001 | Kích hoạt tài khoản qua email token | - Account "user1" chưa kích hoạt<br>- Token hợp lệ, chưa expire | 1. Click link trong email:<br>`/auth/activate/{token}` | **Token:** valid-uuid<br>**Expiry:** Chưa hết hạn | - Method: `accountService.activateAccount(token)`<br>- Find account by token<br>- Check: token expiry > now<br>- Set: activated=true<br>- Clear: activationToken=null<br>- Flash: "Kích hoạt thành công!"<br>- Redirect: `/auth/login` |
| TC_PWD_002 | Kích hoạt với token hết hạn | - Token đã expire (> 24h) | 1. Click link cũ | **Token:** expired-uuid<br>**Expiry:** < now | - Find account by token<br>- Check: `tokenExpiry.isBefore(now)` = true<br>- **Exception:** `ValidationException`<br>- Message: "Token đã hết hạn. Vui lòng yêu cầu gửi lại!"<br>- Link: "Gửi lại email kích hoạt" |
| TC_PWD_003 | Kích hoạt với token không hợp lệ | - Token không tồn tại trong DB | 1. Truy cập URL với token sai | **Token:** invalid-token | - Method: `findByActivationToken(token)`<br>- Return: Optional.empty()<br>- **Exception:** `ValidationException`<br>- Message: "Token kích hoạt không hợp lệ!" |
| TC_PWD_004 | Gửi lại email kích hoạt | - Account "user1" chưa kích hoạt | 1. Click "Gửi lại mã kích hoạt"<br>2. Nhập username | **Username:** user1 | - Method: `accountService.resendActivation("user1")`<br>- Check: activated = false<br>- Generate token mới<br>- Update tokenExpiry = now + 24h<br>- Gửi email mới<br>- Flash: "Email kích hoạt đã được gửi lại!" |
| TC_PWD_005 | Verify OTP kích hoạt (Phone) | - Account "user2" chưa kích hoạt<br>- OTP đã được gửi | 1. Vào `/auth/verify-otp`<br>2. Nhập phone và OTP<br>3. Click "Xác nhận" | **Phone:** 0901234567<br>**OTP:** 123456 | - Method: `accountService.verifyPhoneOTP(phone, otp)`<br>- Check: `otpService.verifyOTP(phone, otp)` = true<br>- Find account by phone<br>- Set: activated=true, phoneVerified=true<br>- Flash: "Kích hoạt thành công!"<br>- Redirect: `/auth/login` |
| TC_PWD_006 | Verify OTP sai | - OTP đúng: 123456 | 1. Nhập OTP sai<br>2. Click "Xác nhận" | **OTP:** 999999 | - Method: `otpService.verifyOTP()` = false<br>- **Exception:** `ValidationException`<br>- Message: "Mã OTP không chính xác!"<br>- Cho phép nhập lại (max 3 lần) |
| TC_PWD_007 | Verify OTP hết hạn | - OTP đã expire (> 5 phút) | 1. Nhập OTP đúng nhưng đã hết hạn | **OTP:** 123456<br>**Time:** > 5 min | - Check: OTP expiry < now<br>- **Exception:** `ValidationException`<br>- Message: "Mã OTP đã hết hạn!"<br>- Link: "Gửi lại OTP" |
| TC_PWD_008 | Quên mật khẩu - Gửi email reset | - Account có email "user@gmail.com" | 1. Vào `/auth/forgot-password`<br>2. Nhập email<br>3. Click "Gửi" | **Email:** user@gmail.com | - Method: `accountService.requestPasswordReset("user@gmail.com")`<br>- Find account by email<br>- Generate resetToken (UUID)<br>- Set: resetTokenExpiry = now + 1h<br>- Gửi email chứa link reset<br>- Flash: "Link đặt lại mật khẩu đã được gửi!"<br>- Redirect: `/auth/login` |
| TC_PWD_009 | Quên mật khẩu - Gửi OTP (fallback) | - Account có phone, không có email<br>- Hoặc email service fail | 1. Nhập username/phone<br>2. Click "Gửi" | **Phone:** 0901234567 | - Try gửi email → fail<br>- Fallback: Gửi OTP qua SMS<br>- Generate OTP<br>- Console log OTP<br>- Flash: "OTP đã được gửi đến SĐT"<br>- Redirect: `/auth/verify-otp` |

---

### C. PASSWORD RESET (4 TCs)

| ID | Tên Test Case | Điều kiện tiên quyết | Các bước thực hiện | Dữ liệu đầu vào | Kết quả mong đợi |
|---|---|---|---|---|---|
| TC_PWD_010 | Reset password với token hợp lệ | - Có resetToken hợp lệ từ email | 1. Click link trong email:<br>`/auth/reset-password/{token}`<br>2. Nhập password mới<br>3. Nhập confirm password<br>4. Click "Đặt lại" | **Token:** valid-token<br>**New Password:** newpass123<br>**Confirm:** newpass123 | - Method: `accountService.resetPassword(token, newPass)`<br>- Find account by resetToken<br>- Check: tokenExpiry > now<br>- Hash password mới: BCrypt<br>- Update: passwordHash<br>- Clear: resetToken=null<br>- Flash: "Đặt lại mật khẩu thành công!"<br>- Redirect: `/auth/login` |
| TC_PWD_011 | Reset password với token hết hạn | - Token đã expire (> 1h) | 1. Click link cũ | **Token:** expired-token | - Check: `resetTokenExpiry.isBefore(now)` = true<br>- **Exception:** `ValidationException`<br>- Message: "Link đã hết hạn. Vui lòng yêu cầu lại!"<br>- Redirect: `/auth/forgot-password` |
| TC_PWD_012 | Reset password không khớp | - Đang ở form reset password | 1. Nhập password mới<br>2. Nhập confirm khác<br>3. Click "Đặt lại" | **Password:** newpass<br>**Confirm:** different | - **Validation:** Frontend check<br>- Error: "Mật khẩu xác nhận không khớp!"<br>- Không submit |
| TC_PWD_013 | Reset password quá ngắn | - Đang ở form reset | 1. Nhập password < 6 ký tự | **Password:** 123 | - **Validation:** Length check<br>- Error: "Mật khẩu phải từ 6 ký tự trở lên"<br>- Không submit |

---

### D. ACCOUNT MANAGEMENT (8 TCs)

| ID | Tên Test Case | Điều kiện tiên quyết | Các bước thực hiện | Dữ liệu đầu vào | Kết quả mong đợi |
|---|---|---|---|---|---|
| TC_ACC_001 | User xem profile | - Đã login (user="user1") | 1. Vào `/profile` | N/A | - Hiển thị thông tin:<br>&nbsp;&nbsp;• Username<br>&nbsp;&nbsp;• Fullname<br>&nbsp;&nbsp;• Email<br>&nbsp;&nbsp;• Phone<br>&nbsp;&nbsp;• Avatar<br>&nbsp;&nbsp;• Points<br>&nbsp;&nbsp;• Customer Rank |
| TC_ACC_002 | User cập nhật profile | - Đã login | 1. Vào `/profile/edit`<br>2. Sửa fullname<br>3. Click "Lưu" | **Fullname mới:** Nguyễn Văn B | - Method: `profileService.updateProfile()`<br>- Update: fullname<br>- Không update: username, password<br>- Flash: "Cập nhật thành công!"<br>- Session: Update user object |
| TC_ACC_003 | User đổi mật khẩu đúng | - Đã login<br>- Password hiện tại: "123" | 1. Vào `/profile/change-password`<br>2. Nhập password cũ đúng<br>3. Nhập password mới<br>4. Click "Đổi mật khẩu" | **Old:** 123<br>**New:** newpass123 | - Method: `profileService.changePassword()`<br>- Verify old password: BCrypt<br>- Hash new password<br>- Update: passwordHash<br>- Flash: "Đổi mật khẩu thành công!"<br>- **Note:** Nên logout và yêu cầu login lại |
| TC_ACC_004 | User đổi mật khẩu sai cũ | - Password hiện tại: "123" | 1. Nhập password cũ SAI<br>2. Nhập password mới<br>3. Click "Đổi" | **Old:** wrongpass<br>**New:** newpass | - Verify old password = false<br>- **Exception:** `ValidationException`<br>- Message: "Mật khẩu cũ không đúng!"<br>- Không update |
| TC_ACC_005 | User upload avatar | - Đã login<br>- File avatar.jpg < 5MB | 1. Vào profile<br>2. Chọn ảnh đại diện<br>3. Click "Upload" | **File:** avatar.jpg<br>**Size:** 1MB | - Method: `uploadService.saveImageWithCompression()`<br>- Resize: max 200x200px<br>- Compress: quality 85%<br>- Upload lên Cloudinary hoặc local<br>- Update: account.photo = URL<br>- Avatar góc phải màn hình thay đổi |
| TC_ACC_006 | Admin tạo tài khoản User | - Đã login Admin | 1. Vào `/admin/account/add`<br>2. Điền thông tin<br>3. Chọn role<br>4. Click "Tạo" | **Username:** staff1<br>**Role:** User (admin=false) | - Method: `accountService.createAccount()`<br>- Hash password<br>- Set: activated=true (admin tạo → auto active)<br>- Flash: "Thêm tài khoản thành công!"<br>- Redirect: `/admin/account/list` |
| TC_ACC_007 | Admin khóa tài khoản User | - Account "user1" đang active | 1. Vào `/admin/account/list`<br>2. Click "Khóa" cho user1 | **Username:** user1 | - Method: `accountService.lockAccount("user1")`<br>- Set: activated=false<br>- Flash: "Đã khóa tài khoản!"<br>- User1 không thể login được nữa |
| TC_ACC_008 | Admin mở khóa tài khoản | - Account "user1" đang bị khóa | 1. Click "Mở khóa" | **Username:** user1 | - Method: `accountService.unlockAccount("user1")`<br>- Set: activated=true<br>- Flash: "Đã mở khóa tài khoản!"<br>- User1 có thể login lại |

---

### E. AUTHORIZATION & ROLE-BASED ACCESS (4 TCs)

| ID | Tên Test Case | Điều kiện tiên quyết | Các bước thực hiện | Dữ liệu đầu vào | Kết quả mong đợi |
|---|---|---|---|---|---|
| TC_AUTH_016 | Admin truy cập trang Admin | - Đã login<br>- Account.admin = true | 1. Vào `/admin` | **Role:** ADMIN | - **Interceptor:** `AuthInterceptor.preHandle()`<br>- Check: session.user != null<br>- Check: user.getAdmin() = true<br>- Allow access<br>- Hiển thị Admin Dashboard |
| TC_AUTH_017 | User truy cập trang Admin (Chặn) | - Đã login<br>- Account.admin = false | 1. Cố truy cập `/admin` | **Role:** USER | - **Interceptor:** Check admin = false<br>- Redirect: `/auth/unauthorized`<br>- HTTP Status: 403 Forbidden<br>- Message: "Bạn không có quyền truy cập!" |
| TC_AUTH_018 | Guest truy cập trang yêu cầu login | - Chưa login (session.user = null) | 1. Cố truy cập `/profile` | **Session:** NULL | - **Interceptor:** Check session.user = null<br>- Redirect: `/auth/login?redirect=/profile`<br>- Flash: "Vui lòng đăng nhập!"<br>- Sau login → redirect về `/profile` |
| TC_AUTH_019 | Test BCrypt password hashing | - Service layer | 1. Gọi `passwordSecurity.hashPassword("123")` | **Plain:** 123 | - Return: BCrypt hash (60 ký tự)<br>- Format: `$2a$12$...`<br>- Mỗi lần hash → kết quả khác nhau (salt)<br>- Verify: `verifyPassword("123", hash)` = true |

---

## 📊 THỐNG KÊ

- **Tổng số Test Cases:** 36
- **Authentication:** 15 TCs
- **Activation & Password Reset:** 9 TCs
- **Password Reset:** 4 TCs
- **Account Management:** 8 TCs
- **Authorization:** 4 TCs
- **Priority High:** 22 TCs
- **Priority Medium:** 10 TCs
- **Priority Low:** 4 TCs

---

## 🎯 COVERAGE MỤC TIÊU

| Component | Method | Coverage |
|-----------|--------|----------|
| AccountServiceImpl | authenticate() | ✅ 100% |
| AccountServiceImpl | register() | ✅ 100% |
| AccountServiceImpl | createAccount() | ✅ 100% |
| AccountServiceImpl | updateAccount() | ✅ 100% |
| AccountServiceImpl | activateAccount() | ✅ 100% |
| AccountServiceImpl | requestPasswordReset() | ✅ 100% |
| AccountServiceImpl | resetPassword() | ✅ 100% |
| AccountServiceImpl | verifyPhoneOTP() | ✅ 100% |
| AccountServiceImpl | lockAccount() | ✅ 100% |
| PasswordSecurity | hashPassword() | ✅ 100% |
| PasswordSecurity | verifyPassword() | ✅ 100% |
| AuthController | login() | ✅ 90% |
| AuthController | signup() | ✅ 90% |
| AuthInterceptor | preHandle() | ✅ 100% |

---

## 📝 GHI CHÚ

### Test Cases đã SỬA:
- TC_AUTH_001-003: Thêm test login bằng email và phone
- TC_AUTH_006-007: Làm rõ activated=false (chưa kích hoạt vs bị khóa)
- TC_AUTH_016-017: Sửa test AuthInterceptor thay vì Spring Security
- TC_PWD_008-009: Thêm fallback OTP khi email fail

### Test Cases MỚI THÊM:
- TC_AUTH_002-003: Login bằng email/phone
- TC_AUTH_011: Đăng ký với OTP
- TC_PWD_005-007: Verify OTP activation
- TC_PWD_009: Quên mật khẩu với OTP fallback
- TC_AUTH_019: Test BCrypt hashing

### Lưu ý quan trọng:

1. **Authentication Flow:**
   - Hỗ trợ 3 cách login: username, email, phone
   - Password được hash bằng BCrypt (strength=12)
   - Session-based authentication (không dùng JWT)

2. **Activation Methods:**
   - **EMAIL:** Gửi link kích hoạt (token expire 24h)
   - **PHONE:** Gửi OTP (expire 5 phút)
   - Tự động chọn method dựa vào có email hay không

3. **Password Reset:**
   - **Có email:** Gửi link reset (token expire 1h)
   - **Không email:** Fallback OTP qua phone
   - Email service fail → tự động chuyển sang OTP

4. **Authorization:**
   - Dùng `AuthInterceptor` thay vì Spring Security
   - Check session.user và user.getAdmin()
   - Redirect về login nếu chưa đăng nhập
   - Redirect về unauthorized nếu không có quyền

5. **Security Best Practices:**
   - Không tiết lộ user có tồn tại hay không
   - Token có thời gian expire
   - OTP giới hạn số lần thử (max 3)
   - BCrypt với salt tự động

---

## 🔗 LIÊN KẾT VỚI MODULE KHÁC

### Với Order Module (Tuấn Khoa):
- Checkout yêu cầu login
- Order.account → Account (ManyToOne)
- Xem lịch sử đơn hàng

### Với Profile Module:
- Cập nhật thông tin cá nhân
- Đổi mật khẩu
- Upload avatar

### Với Email Service:
- Gửi email kích hoạt
- Gửi email reset password
- Gửi email thông báo đơn hàng

### Với OTP Service:
- Generate OTP (6 số)
- Verify OTP
- Cache OTP với expire time

---

**Người tạo:** Khánh Ý  
**Reviewer:** Kiro AI  
**Ngày cập nhật:** 2026-02-25  
**Version:** 2.0
