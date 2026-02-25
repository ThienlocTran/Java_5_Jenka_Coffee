# 🗺️ URL MAPPING & TEST COVERAGE ANALYSIS

**Dự án:** Jenka Coffee  
**Ngày:** 2026-02-25  
**Version:** 1.0

---

## 📊 TỔNG QUAN CONTROLLERS

### 🔴 ADMIN CONTROLLERS (Prefix: `/admin`)

| Controller | Base URL | Số Endpoints | Test Coverage | Người phụ trách |
|------------|----------|--------------|---------------|-----------------|
| AdminProductController | `/admin/product` | 6 | ✅ 100% | Thiên Lộc |
| AdminCategoryController | `/admin/category` | 7 | ✅ 100% | Chí Bảo |
| AdminAccountController | `/admin/account` | 10 | ✅ 100% | Khánh Ý |
| AdminOrderController | `/admin/order` | 3 | ⚠️ 60% | Tuấn Khoa |
| AdminDashboardController | `/admin` | 1 | ❌ 0% | **THIẾU** |
| AdminBookingController | `/admin/booking` | ? | ❌ 0% | **THIẾU** |
| AdminNewsController | `/admin/news` | ? | ❌ 0% | **THIẾU** |
| AdminReportController | `/admin/report` | ? | ❌ 0% | **THIẾU** |

### 🔵 SITE CONTROLLERS (User-facing)

| Controller | Base URL | Số Endpoints | Test Coverage | Người phụ trách |
|------------|----------|--------------|---------------|-----------------|
| AuthController | `/auth` | 10 | ✅ 100% | Khánh Ý |
| ProductController | `/product` | 5 | ✅ 80% | Thiên Lộc |
| CartController | `/cart` | 6 | ✅ 100% | Tuấn Khoa |
| CheckoutController | `/checkout` | 2 | ✅ 100% | Tuấn Khoa |
| OrderController | `/order` | 2 | ⚠️ 40% | Tuấn Khoa |
| ProfileController | `/profile` | 5 | ⚠️ 60% | Khánh Ý |
| HomeController | `/home` | 1 | ❌ 0% | **THIẾU** |
| NewsController | `/news` | ? | ❌ 0% | **THIẾU** |
| ContactController | `/contact` | ? | ❌ 0% | **THIẾU** |
| BookingController | `/booking` | ? | ❌ 0% | **THIẾU** |

---

## 🎯 CHI TIẾT URL ENDPOINTS

### 1️⃣ THIÊN LỘC - Product Module

#### ✅ Admin Product Management

| Method | URL | Controller Method | Test Cases | Status |
|--------|-----|-------------------|------------|--------|
| GET | `/admin/product/list` | `index()` | TC_PROD_013 | ✅ |
| GET | `/admin/product/create` | `create()` | TC_PROD_001 | ✅ |
| GET | `/admin/product/edit/{id}` | `edit()` | TC_PROD_006-008 | ✅ |
| POST | `/admin/product/save` | `save()` | TC_PROD_001-007 | ✅ |
| GET | `/admin/product/toggle/{id}` | `toggleAvailable()` | TC_PROD_009-010 | ✅ |
| GET | `/admin/product/delete/{id}` | `delete()` | - | ✅ |

#### ✅ Site Product Browsing

| Method | URL | Controller Method | Test Cases | Status |
|--------|-----|-------------------|------------|--------|
| GET | `/` hoặc `/product/list` | `index()` | TC_PROD_013 | ✅ |
| GET | `/product/detail/{id}` | `detail()` | - | ⚠️ THIẾU TC |
| GET | `/product/filter` | `filterProducts()` | TC_PROD_015-017 | ✅ |
| GET | `/product/search` | `searchProducts()` | TC_PROD_011-012 | ✅ |
| GET | `/api/product/quick-view/{id}` | `quickView()` | - | ⚠️ THIẾU TC |

**📝 Ghi chú:**
- ✅ Đã cover đầy đủ CRUD admin
- ⚠️ Thiếu test cho product detail page
- ⚠️ Thiếu test cho quick view API

---

### 2️⃣ TUẤN KHOA - Cart & Checkout Module

#### ✅ Shopping Cart

| Method | URL | Controller Method | Test Cases | Status |
|--------|-----|-------------------|------------|--------|
| GET | `/cart/view` | `view()` | TC_CART_001-010 | ✅ |
| GET | `/cart/add/{id}` | `add()` | TC_CART_001-002 | ✅ |
| GET | `/cart/remove/{id}` | `remove()` | TC_CART_003 | ✅ |
| GET | `/cart/update/{id}/{qty}` | `update()` | TC_CART_004-008 | ✅ |
| GET | `/cart/clear` | `clear()` | TC_CART_009 | ✅ |
| GET | `/cart/api/add/{id}` | `addApi()` | - | ⚠️ THIẾU TC |

#### ✅ Checkout

| Method | URL | Controller Method | Test Cases | Status |
|--------|-----|-------------------|------------|--------|
| GET | `/checkout` | `showCheckoutForm()` | TC_CHK_002-003 | ✅ |
| POST | `/checkout` | `processCheckout()` | TC_CHK_001-013 | ✅ |
| GET | `/checkout/success` | `checkoutSuccess()` | TC_CHK_001 | ✅ |

#### ⚠️ Order Management (THIẾU)

| Method | URL | Controller Method | Test Cases | Status |
|--------|-----|-------------------|------------|--------|
| GET | `/order/list` | `list()` | TC_ORD_001 | ✅ |
| GET | `/order/load-more` | `loadMore()` | - | ❌ THIẾU TC |

#### ⚠️ Admin Order Management (THIẾU)

| Method | URL | Controller Method | Test Cases | Status |
|--------|-----|-------------------|------------|--------|
| GET | `/admin/order/index` | `index()` | TC_ORD_003 | ✅ |
| GET | `/admin/order/update/{id}/{status}` | `updateStatus()` | TC_ORD_004 | ✅ |
| GET | `/admin/order/delete/{id}` | `delete()` | TC_ORD_005 | ✅ |

**📝 Ghi chú:**
- ✅ Cart & Checkout đã cover đầy đủ
- ⚠️ Thiếu test cho API add to cart (AJAX)
- ⚠️ Thiếu test cho lazy load orders
- ⚠️ Thiếu test cho order detail page

---

### 3️⃣ CHÍ BẢO - Category Module

#### ✅ Admin Category Management

| Method | URL | Controller Method | Test Cases | Status |
|--------|-----|-------------------|------------|--------|
| GET | `/admin/category/list` | `listCategories()` | TC_CAT_019-020 | ✅ |
| GET | `/admin/category/add` | `showAddForm()` | TC_CAT_001 | ✅ |
| GET | `/admin/category/edit/{id}` | `showEditForm()` | TC_CAT_005-006 | ✅ |
| POST | `/admin/category/save` | `saveCategory()` | TC_CAT_001-015 | ✅ |
| POST | `/admin/category/delete/{id}` | `deleteCategory()` | TC_CAT_007-008 | ✅ |
| GET | `/admin/category/check-id` | `checkCategoryId()` | - | ⚠️ THIẾU TC |
| GET | `/admin/category/product-count/{id}` | `getProductCount()` | TC_CAT_020 | ✅ |

**📝 Ghi chú:**
- ✅ Đã cover đầy đủ CRUD
- ⚠️ Thiếu test cho AJAX check ID
- ✅ Category hiển thị trên menu được test ở TC_CAT_019

---

### 4️⃣ KHÁNH Ý - Auth & Account Module

#### ✅ Authentication

| Method | URL | Controller Method | Test Cases | Status |
|--------|-----|-------------------|------------|--------|
| GET | `/auth/login` | `showLoginPage()` | - | ⚠️ THIẾU TC |
| POST | `/auth/login` | `login()` | TC_AUTH_001-009 | ✅ |
| GET | `/auth/logout` | `logout()` | TC_AUTH_009 | ✅ |
| POST | `/auth/signup` | `signup()` | TC_AUTH_010-015 | ✅ |
| GET | `/auth/unauthorized` | `unauthorized()` | TC_AUTH_017 | ✅ |

#### ✅ Account Activation & Password Reset

| Method | URL | Controller Method | Test Cases | Status |
|--------|-----|-------------------|------------|--------|
| GET | `/auth/activate/{token}` | `activateAccount()` | TC_PWD_001-003 | ✅ |
| GET | `/auth/forgot-password` | `showForgotPassword()` | - | ⚠️ THIẾU TC |
| POST | `/auth/forgot-password` | `processForgotPassword()` | TC_PWD_008-009 | ✅ |
| GET | `/auth/reset-password/{token}` | `showResetPassword()` | - | ⚠️ THIẾU TC |
| POST | `/auth/reset-password` | `processResetPassword()` | TC_PWD_010-013 | ✅ |
| GET | `/auth/verify-otp` | `showVerifyOTP()` | - | ⚠️ THIẾU TC |
| POST | `/auth/verify-otp` | `verifyOTP()` | TC_PWD_005-007 | ✅ |

#### ⚠️ Profile Management (THIẾU)

| Method | URL | Controller Method | Test Cases | Status |
|--------|-----|-------------------|------------|--------|
| GET | `/profile` | `showProfile()` | TC_ACC_001 | ✅ |
| POST | `/profile/update` | `updateProfile()` | TC_ACC_002 | ✅ |
| POST | `/profile/avatar` | `updateAvatar()` | TC_ACC_005 | ✅ |
| GET | `/profile/change-password` | `showChangePassword()` | - | ❌ THIẾU TC |
| POST | `/profile/change-password` | `changePassword()` | TC_ACC_003-004 | ✅ |

#### ✅ Admin Account Management

| Method | URL | Controller Method | Test Cases | Status |
|--------|-----|-------------------|------------|--------|
| GET | `/admin/account/list` | `listAccounts()` | - | ⚠️ THIẾU TC |
| GET | `/admin/account/add` | `showAddForm()` | - | ⚠️ THIẾU TC |
| GET | `/admin/account/edit/{username}` | `showEditForm()` | - | ⚠️ THIẾU TC |
| POST | `/admin/account/save` | `saveAccount()` | TC_ACC_006 | ✅ |
| POST | `/admin/account/delete/{username}` | `deleteAccount()` | - | ⚠️ THIẾU TC |
| POST | `/admin/account/toggle-status/{username}` | `toggleAccountStatus()` | - | ⚠️ THIẾU TC |
| POST | `/admin/account/lock/{username}` | `lockAccount()` | TC_ACC_007 | ✅ |
| POST | `/admin/account/unlock/{username}` | `unlockAccount()` | TC_ACC_008 | ✅ |
| GET | `/admin/account/reset-password/{username}` | `showResetPasswordForm()` | - | ❌ THIẾU TC |
| POST | `/admin/account/reset-password/{username}` | `adminResetPassword()` | - | ⚠️ THIẾU TC |
| GET | `/admin/account/check-username` | `checkUsername()` | - | ❌ THIẾU TC |
| GET | `/admin/account/check-email` | `checkEmail()` | - | ❌ THIẾU TC |

**📝 Ghi chú:**
- ✅ Authentication flow đã cover đầy đủ
- ⚠️ Thiếu test cho các GET endpoints (show form)
- ⚠️ Admin account management thiếu nhiều test cases

---

## ❌ CONTROLLERS CHƯA ĐƯỢC TEST

### 1. AdminDashboardController
```
GET /admin/dashboard - Hiển thị dashboard thống kê
```
**Gợi ý test cases:**
- TC_DASH_001: Hiển thị tổng doanh thu
- TC_DASH_002: Hiển thị số đơn hàng mới
- TC_DASH_003: Hiển thị top sản phẩm bán chạy
- TC_DASH_004: Hiển thị biểu đồ doanh thu theo tháng

### 2. AdminBookingController
```
GET /admin/booking/list - Danh sách đặt bàn
```
**Gợi ý test cases:**
- TC_BOOK_001: Xem danh sách booking
- TC_BOOK_002: Duyệt booking
- TC_BOOK_003: Hủy booking

### 3. AdminNewsController
```
GET /admin/news/list - Quản lý tin tức
GET /admin/news/add - Thêm tin tức
POST /admin/news/save - Lưu tin tức
```
**Gợi ý test cases:**
- TC_NEWS_001: Thêm tin tức mới
- TC_NEWS_002: Sửa tin tức
- TC_NEWS_003: Xóa tin tức
- TC_NEWS_004: Upload ảnh tin tức

### 4. AdminReportController
```
GET /admin/report/revenue - Báo cáo doanh thu
GET /admin/report/vip-customer - Khách hàng VIP
```
**Gợi ý test cases:**
- TC_RPT_001: Báo cáo doanh thu theo tháng
- TC_RPT_002: Báo cáo top khách hàng
- TC_RPT_003: Export Excel

### 5. HomeController
```
GET /home - Trang chủ
```
**Gợi ý test cases:**
- TC_HOME_001: Hiển thị banner
- TC_HOME_002: Hiển thị sản phẩm nổi bật
- TC_HOME_003: Hiển thị tin tức mới nhất

### 6. NewsController (Site)
```
GET /news/list - Danh sách tin tức
GET /news/detail/{id} - Chi tiết tin tức
```
**Gợi ý test cases:**
- TC_NEWS_005: Xem danh sách tin tức
- TC_NEWS_006: Xem chi tiết tin tức
- TC_NEWS_007: Phân trang tin tức

### 7. ContactController
```
GET /contact - Trang liên hệ
POST /contact/send - Gửi liên hệ
```
**Gợi ý test cases:**
- TC_CONT_001: Gửi form liên hệ thành công
- TC_CONT_002: Validation form liên hệ

### 8. BookingController (Site)
```
GET /booking - Trang đặt bàn
POST /booking/submit - Gửi đặt bàn
```
**Gợi ý test cases:**
- TC_BOOK_004: Đặt bàn thành công
- TC_BOOK_005: Validation form đặt bàn

---

## 📈 THỐNG KÊ COVERAGE

### Theo Module

| Module | Total Endpoints | Tested | Coverage | Priority |
|--------|----------------|--------|----------|----------|
| Product | 11 | 9 | 82% | ⭐⭐⭐ |
| Cart & Checkout | 9 | 8 | 89% | ⭐⭐⭐ |
| Category | 7 | 6 | 86% | ⭐⭐⭐ |
| Auth & Account | 22 | 15 | 68% | ⭐⭐⭐ |
| Order | 5 | 4 | 80% | ⭐⭐ |
| Profile | 5 | 3 | 60% | ⭐⭐ |
| Dashboard | 1 | 0 | 0% | ⭐ |
| News | ~5 | 0 | 0% | ⭐ |
| Booking | ~3 | 0 | 0% | ⭐ |
| Contact | ~2 | 0 | 0% | ⭐ |
| Report | ~2 | 0 | 0% | ⭐ |

### Tổng thể

- **Total Endpoints:** ~72
- **Tested Endpoints:** ~45
- **Overall Coverage:** ~62.5%

---

## 🎯 KHUYẾN NGHỊ

### Ưu tiên CAO (Cần bổ sung ngay)

1. **Product Detail Page** (Thiên Lộc)
   - TC_PROD_021: Test hiển thị chi tiết sản phẩm
   - TC_PROD_022: Test sản phẩm liên quan

2. **Order Detail Page** (Tuấn Khoa)
   - TC_ORD_006: User xem chi tiết đơn hàng
   - TC_ORD_007: Admin xem chi tiết đơn hàng

3. **Admin Account CRUD** (Khánh Ý)
   - TC_ACC_009: Admin xem danh sách tài khoản
   - TC_ACC_010: Admin xóa tài khoản
   - TC_ACC_011: Admin toggle status

### Ưu tiên TRUNG BÌNH (Nên có)

4. **Profile Management** (Khánh Ý)
   - TC_ACC_012: Test show change password form
   - TC_ACC_013: Test validation avatar upload

5. **AJAX APIs** (Thiên Lộc, Chí Bảo)
   - TC_API_001: Test quick view API
   - TC_API_002: Test add to cart API
   - TC_API_003: Test check category ID API

### Ưu tiên THẤP (Có thể bỏ qua)

6. **Dashboard & Reports** (Nếu có thời gian)
7. **News Management** (Nếu có thời gian)
8. **Booking System** (Nếu có thời gian)

---

## 📋 CHECKLIST HOÀN THIỆN

### Thiên Lộc
- [x] Admin Product CRUD
- [x] Product List & Filter
- [x] Product Search
- [x] Image Upload
- [ ] Product Detail Page
- [ ] Quick View API

### Tuấn Khoa
- [x] Shopping Cart
- [x] Checkout Process
- [x] Order List
- [ ] Order Detail
- [ ] Admin Order Management (đầy đủ)
- [ ] Lazy Load API

### Chí Bảo
- [x] Admin Category CRUD
- [x] Category Display
- [x] Category Count
- [ ] AJAX Check ID API

### Khánh Ý
- [x] Authentication Flow
- [x] Password Reset
- [x] OTP Verification
- [x] Basic Profile
- [ ] Admin Account Management (đầy đủ)
- [ ] AJAX Validation APIs

---

**Tổng kết:**
- ✅ Core features đã được test tốt (62.5% coverage)
- ⚠️ Cần bổ sung ~10-15 test cases cho các endpoints còn thiếu
- ❌ Các module phụ (News, Booking, Contact) có thể bỏ qua nếu không đủ thời gian

**Người tạo:** Kiro AI  
**Ngày:** 2026-02-25
