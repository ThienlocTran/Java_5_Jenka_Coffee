# 📊 TỔNG KẾT TEST SCRIPTS TỰ ĐỘNG (Y4)

**Dự án:** Jenka Coffee  
**Tổng số Test Scripts:** 8 (4 người × 2 scripts)  
**Tổng số Test Cases:** 27  
**Công nghệ:** Selenium WebDriver 4.15 + JUnit 5

---

## 📋 PHÂN CÔNG & THỐNG KÊ

| Thành viên | Test Scripts | Test Cases | Files |
|------------|--------------|------------|-------|
| **Thiên Lộc** | 2 | 6 | `AutoTest_ThienLoc_Product.md` |
| **Tuấn Khoa** | 2 | 6 | `AutoTest_TuanKhoa_Cart.md` |
| **Chí Bảo** | 2 | 6 | `AutoTest_ChiBao_Category.md` |
| **Khánh Ý** | 2 | 9 | `AutoTest_KhanhY_Auth.md` |
| **TỔNG** | **8** | **27** | 4 files + 1 base |

---

## 🎯 CHI TIẾT TEST SCRIPTS

### 1️⃣ THIÊN LỘC - Product Module

#### ProductListTest.java (3 tests)
- ✅ TS_PROD_001: Hiển thị danh sách sản phẩm
- ✅ TS_PROD_002: Test phân trang sản phẩm
- ✅ TS_PROD_003: Test click vào sản phẩm

#### ProductSearchTest.java (3 tests)
- ✅ TS_PROD_004: Tìm kiếm theo keyword
- ✅ TS_PROD_005: Lọc theo category
- ✅ TS_PROD_006: Lọc theo khoảng giá

---

### 2️⃣ TUẤN KHOA - Cart & Checkout Module

#### AddToCartTest.java (3 tests)
- ✅ TS_CART_001: Thêm sản phẩm vào giỏ
- ✅ TS_CART_002: Cập nhật số lượng
- ✅ TS_CART_003: Xóa sản phẩm khỏi giỏ

#### CheckoutFlowTest.java (3 tests)
- ✅ TS_CART_004: Checkout thành công (E2E)
- ✅ TS_CART_005: Chặn checkout chưa login
- ✅ TS_CART_006: Chặn checkout giỏ trống

---

### 3️⃣ CHÍ BẢO - Category Module

#### CategoryCRUDTest.java (3 tests)
- ✅ TS_CAT_001: Thêm danh mục mới
- ✅ TS_CAT_002: Sửa danh mục
- ✅ TS_CAT_003: Xóa danh mục rỗng

#### CategoryFilterTest.java (3 tests)
- ✅ TS_CAT_004: Hiển thị menu danh mục
- ✅ TS_CAT_005: Lọc sản phẩm theo danh mục
- ✅ TS_CAT_006: Đếm số sản phẩm theo danh mục

---

### 4️⃣ KHÁNH Ý - Authentication Module

#### LoginTest.java (6 tests)
- ✅ TS_AUTH_001: Đăng nhập admin
- ✅ TS_AUTH_002: Đăng nhập user
- ✅ TS_AUTH_003: Sai password
- ✅ TS_AUTH_004: Remember Me
- ✅ TS_AUTH_005: Logout
- ✅ TS_AUTH_006: Chặn user vào admin

#### SignupTest.java (3 tests)
- ✅ TS_AUTH_007: Đăng ký thành công
- ✅ TS_AUTH_008: Username trùng
- ✅ TS_AUTH_009: Password không khớp

---

## 🛠️ CẤU TRÚC DỰ ÁN

```
src/test/java/com/springboot/jenka_coffee/
├── selenium/
│   ├── BaseSeleniumTest.java          ← Base class chung
│   ├── product/
│   │   ├── ProductListTest.java       ← Thiên Lộc
│   │   └── ProductSearchTest.java     ← Thiên Lộc
│   ├── cart/
│   │   ├── AddToCartTest.java         ← Tuấn Khoa
│   │   └── CheckoutFlowTest.java      ← Tuấn Khoa
│   ├── category/
│   │   ├── CategoryCRUDTest.java      ← Chí Bảo
│   │   └── CategoryFilterTest.java    ← Chí Bảo
│   └── auth/
│       ├── LoginTest.java             ← Khánh Ý
│       └── SignupTest.java            ← Khánh Ý
```

---

## 📦 DEPENDENCIES (pom.xml)

```xml
<!-- Selenium WebDriver -->
<dependency>
    <groupId>org.seleniumhq.selenium</groupId>
    <artifactId>selenium-java</artifactId>
    <version>4.15.0</version>
    <scope>test</scope>
</dependency>

<!-- WebDriverManager -->
<dependency>
    <groupId>io.github.bonigarcia</groupId>
    <artifactId>webdrivermanager</artifactId>
    <version>5.6.2</version>
    <scope>test</scope>
</dependency>

<!-- JUnit 5 (đã có sẵn) -->
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.10.0</version>
    <scope>test</scope>
</dependency>
```

---

## 🚀 CÁCH CHẠY TEST

### 1. Chạy tất cả Selenium tests
```bash
mvn test -Dtest="**/selenium/**/*Test.java"
```

### 2. Chạy theo module
```bash
# Product tests
mvn test -Dtest="**/product/*Test.java"

# Cart tests
mvn test -Dtest="**/cart/*Test.java"

# Category tests
mvn test -Dtest="**/category/*Test.java"

# Auth tests
mvn test -Dtest="**/auth/*Test.java"
```

### 3. Chạy từng file
```bash
mvn test -Dtest=ProductListTest
mvn test -Dtest=CheckoutFlowTest
mvn test -Dtest=LoginTest
```

### 4. Chạy từ IDE
```
Right-click vào file test → Run 'TestClassName'
```

---

## ⚙️ CẤU HÌNH

### BaseSeleniumTest.java - Cấu hình chung

```java
protected String baseUrl = "http://localhost:8080";  // URL ứng dụng
protected WebDriver driver;                          // Chrome driver
protected WebDriverWait wait;                        // Wait 10 seconds

// Chrome options
options.addArguments("--start-maximized");           // Mở full screen
options.addArguments("--disable-notifications");     // Tắt thông báo
// options.addArguments("--headless");               // Chạy không hiển thị
```

### Chạy Headless (không hiển thị browser)
Uncomment dòng này trong `BaseSeleniumTest.java`:
```java
options.addArguments("--headless");
```

---

## 📊 KẾT QUẢ MONG ĐỢI

### Console Output
```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.springboot.jenka_coffee.selenium.product.ProductListTest
✅ Số sản phẩm hiển thị: 12
✅ Test hiển thị danh sách sản phẩm: PASSED
📄 Trang 1 có: 12 sản phẩm
📄 Trang 2 có: 8 sản phẩm
✅ Test phân trang: PASSED
🔍 Click vào sản phẩm: Máy pha cà phê Ý
✅ Test click sản phẩm: PASSED
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0

[INFO] Running com.springboot.jenka_coffee.selenium.cart.CheckoutFlowTest
🔐 Phase 1: Login
✅ Login thành công
🛒 Phase 2: Add to Cart
📦 Sản phẩm: Máy pha Ý - 5000000đ
✅ Đã thêm vào giỏ
👀 Phase 3: View Cart
✅ Đã vào trang checkout
📝 Phase 4: Fill Checkout Form
✅ Đã điền form
🚀 Phase 5: Submit Order
✅ Phase 6: Verify Success
🎉 Mã đơn hàng: #123
🧹 Phase 7: Verify Cart Cleared
✅ Giỏ hàng đã được xóa
🎊 ========== CHECKOUT FLOW TEST PASSED ========== 🎊
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0

[INFO] -------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] -------------------------------------------------------
[INFO] Total time:  45.123 s
[INFO] Finished at: 2026-02-25T10:30:00+07:00
[INFO] -------------------------------------------------------
```

---

## 📝 LƯU Ý QUAN TRỌNG

### 1. Trước khi chạy test:
- ✅ Đảm bảo ứng dụng đang chạy ở `http://localhost:8080`
- ✅ Database có dữ liệu test (sản phẩm, danh mục, user)
- ✅ Đã cài đặt Chrome browser

### 2. Dữ liệu test cần có:
- ✅ Admin account: `admin / 123`
- ✅ User account: `user1 / 123`
- ✅ Ít nhất 12 sản phẩm (để test phân trang)
- ✅ Ít nhất 2 danh mục

### 3. Nếu test fail:
- 🔍 Kiểm tra console log để xem lỗi ở đâu
- 🔍 Kiểm tra URL có đúng không
- 🔍 Kiểm tra CSS selector có thay đổi không
- 🔍 Tăng timeout nếu mạng chậm

### 4. Tối ưu hóa:
- ⚡ Chạy headless để nhanh hơn
- ⚡ Chạy parallel tests (nếu có nhiều test)
- ⚡ Sử dụng test data fixtures

---

## 🎯 SO SÁNH VỚI UNIT TEST (Y3)

| Tiêu chí | Unit Test (Y3) | Automated Test (Y4) |
|----------|----------------|---------------------|
| **Mục đích** | Test logic code | Test UI/UX flow |
| **Layer** | Service, Repository | Controller, View |
| **Công nghệ** | JUnit + Mockito | Selenium + JUnit |
| **Tốc độ** | Rất nhanh (ms) | Chậm hơn (giây) |
| **Coverage** | Code coverage | User flow coverage |
| **Số lượng** | 15+ tests/người | 2 scripts/người |
| **Chạy khi** | Mỗi lần build | Trước release |

---

## ✅ CHECKLIST HOÀN THÀNH

### Thiên Lộc
- [x] ProductListTest.java (3 tests)
- [x] ProductSearchTest.java (3 tests)
- [x] Tổng: 6 tests ✅

### Tuấn Khoa
- [x] AddToCartTest.java (3 tests)
- [x] CheckoutFlowTest.java (3 tests)
- [x] Tổng: 6 tests ✅

### Chí Bảo
- [x] CategoryCRUDTest.java (3 tests)
- [x] CategoryFilterTest.java (3 tests)
- [x] Tổng: 6 tests ✅

### Khánh Ý
- [x] LoginTest.java (6 tests)
- [x] SignupTest.java (3 tests)
- [x] Tổng: 9 tests ✅

---

## 🎓 TÀI LIỆU THAM KHẢO

1. **Selenium Documentation:** https://www.selenium.dev/documentation/
2. **JUnit 5 Guide:** https://junit.org/junit5/docs/current/user-guide/
3. **WebDriverManager:** https://github.com/bonigarcia/webdrivermanager

---

**Tổng kết:**
- ✅ 8 Test Scripts hoàn chỉnh
- ✅ 27 Test Cases tự động
- ✅ Coverage: Product, Cart, Checkout, Category, Auth
- ✅ Sẵn sàng chạy và demo

**Người tạo:** Kiro AI Assistant  
**Ngày:** 2026-02-25  
**Version:** 1.0 Final
