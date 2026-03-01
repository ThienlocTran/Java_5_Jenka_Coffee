# Hướng Dẫn Chạy Selenium Tests

## 📋 Tổng Quan

Dự án có 8 Selenium tests cho module Product (Thiên Lộc):
- 3 tests cho Pagination (ProductListPaginationTest)
- 5 tests cho Search & Filter (ProductSearchFilterTest)

Tất cả tests đã được tối ưu với:
- ✅ Retry logic cho connection reset (3 lần retry)
- ✅ Delay 2 giây giữa các tests
- ✅ Explicit waits và scroll handling
- ✅ JavaScript click fallback

---

## 🚀 Cách Chạy Tests

### Bước 1: Start Spring Boot Application

**Terminal 1:**
```bash
mvnw spring-boot:run
```

Đợi cho đến khi thấy:
```
Started JenkaCoffeeApplication in X.XXX seconds
```

Verify application đang chạy:
```bash
curl http://localhost:8080/product/list
```

### Bước 2: Chạy Tests

**Terminal 2 (mở terminal mới):**

#### Chạy tất cả tests của ProductSearchFilterTest:
```bash
mvnw test -Dtest=ProductSearchFilterTest
```

#### Chạy tất cả tests của ProductListPaginationTest:
```bash
mvnw test -Dtest=ProductListPaginationTest
```

#### Chạy từng test riêng lẻ:
```bash
# Search & Filter Tests
mvnw test -Dtest=ProductSearchFilterTest#testSearchByKeyword
mvnw test -Dtest=ProductSearchFilterTest#testFilterByPriceRange
mvnw test -Dtest=ProductSearchFilterTest#testQuickPriceFilter
mvnw test -Dtest=ProductSearchFilterTest#testFilterByCategory
mvnw test -Dtest=ProductSearchFilterTest#testCombinedSearchAndFilter

# Pagination Tests
mvnw test -Dtest=ProductListPaginationTest#testProductListDisplay
mvnw test -Dtest=ProductListPaginationTest#testProductListPagination
mvnw test -Dtest=ProductListPaginationTest#testProductsPerPage
```

---

## 📊 Danh Sách Tests

### ProductSearchFilterTest (5 tests)

| Test | Mô tả | Dữ liệu đầu vào |
|------|-------|-----------------|
| testSearchByKeyword | Tìm kiếm sản phẩm theo từ khóa | Keyword: "Máy" |
| testFilterByPriceRange | Lọc theo khoảng giá | Min: 100,000 - Max: 5,000,000 |
| testQuickPriceFilter | Lọc nhanh theo link có sẵn | Click "1 triệu - 5 triệu" |
| testFilterByCategory | Lọc theo danh mục | Click danh mục đầu tiên |
| testCombinedSearchAndFilter | Kết hợp tìm kiếm + lọc | Keyword: "Máy" + Min: 100,000 |

### ProductListPaginationTest (3 tests)

| Test | Mô tả | Kết quả mong đợi |
|------|-------|------------------|
| testProductListDisplay | Hiển thị danh sách SP | Có ít nhất 1 SP với đầy đủ thông tin |
| testProductListPagination | Phân trang | Chuyển trang 2, Previous hoạt động |
| testProductsPerPage | Số SP mỗi trang | 1-12 sản phẩm/trang |

---

## 🔧 Retry Logic

Tất cả tests có retry logic tự động:

```
Attempt 1: driver.get(URL)
  ↓ (nếu ERR_CONNECTION_RESET)
Đợi 3 giây
  ↓
Attempt 2: driver.get(URL)
  ↓ (nếu vẫn fail)
Đợi 3 giây
  ↓
Attempt 3: driver.get(URL)
  ↓ (nếu vẫn fail)
FAIL test
```

Logs khi retry:
```
⚠ Connection reset, retry 1/3
⚠ Connection reset, retry 2/3
```

---

## ✅ Kết Quả Mong Đợi

### Test Success Output:
```
=== TEST: Tìm kiếm sản phẩm theo từ khóa ===
✓ Đã truy cập: http://localhost:8080/product/list
✓ Tìm thấy search input
✓ Đã nhập từ khóa: Máy
✓ Đã submit form tìm kiếm
✓ URL sau tìm kiếm: http://localhost:8080/product/filter?keyword=Máy
✓ Số sản phẩm tìm thấy: 5
✓ Sản phẩm khớp: Máy Pha Cà Phê
=== TEST PASSED: Tìm kiếm hoạt động đúng ===
```

### Test với Retry:
```
=== TEST: Lọc nhanh theo khoảng giá ===
⚠ Connection reset, retry 1/3
✓ Đã truy cập: http://localhost:8080/product/list
✓ Số lượng link lọc nhanh: 4
✓ Đã click: 1 triệu - 5 triệu
=== TEST PASSED: Lọc nhanh hoạt động đúng ===
```

---

## ⚠️ Troubleshooting

### Lỗi: ERR_CONNECTION_REFUSED
**Nguyên nhân:** Spring Boot application chưa chạy

**Giải pháp:**
```bash
# Terminal 1
mvnw spring-boot:run
```

### Lỗi: ERR_CONNECTION_RESET (sau 3 retries)
**Nguyên nhân:** Database connection issues

**Giải pháp:**
1. Restart Spring Boot application
2. Kiểm tra application logs
3. Verify database connection trong application.properties
4. Chạy từng test riêng lẻ thay vì tất cả cùng lúc

### Lỗi: TimeoutException waiting for .product-card
**Nguyên nhân:** Không có sản phẩm trong database hoặc filter không có kết quả

**Giải pháp:**
1. Kiểm tra database có dữ liệu không
2. Test sẽ log warning nhưng vẫn pass nếu là empty result hợp lệ
3. Thử với keyword/filter khác

### Lỗi: Element not clickable
**Nguyên nhân:** Element bị che hoặc chưa load xong

**Giải pháp:** Tests đã có JavaScript click fallback, nếu vẫn lỗi:
1. Tăng timeout trong @BeforeEach
2. Kiểm tra page có load đúng không

### Tests chạy chậm
**Nguyên nhân:** Remote database latency

**Giải pháp:**
1. Chấp nhận - remote DB sẽ chậm hơn local
2. Có thể giảm delay trong @AfterEach nếu muốn
3. Xem xét dùng local database cho testing

---

## 📈 Performance Tips

### Chạy tests nhanh hơn:
```bash
# Giảm delay giữa tests (edit trong test files)
Thread.sleep(1000); // Từ 2000 xuống 1000

# Chạy parallel (cẩn thận với database)
mvnw test -Dtest=ProductSearchFilterTest -DforkCount=2
```

### Chạy tests ổn định hơn:
```bash
# Tăng delay giữa tests
Thread.sleep(5000); // Từ 2000 lên 5000

# Chạy từng test riêng
mvnw test -Dtest=ProductSearchFilterTest#testSearchByKeyword
# Đợi 5 giây
mvnw test -Dtest=ProductSearchFilterTest#testFilterByPriceRange
```

---

## 📝 Checklist Trước Khi Chạy Tests

- [ ] Spring Boot application đang chạy ở http://localhost:8080
- [ ] Database connection hoạt động (check application logs)
- [ ] ChromeDriver đã được cài đặt và trong PATH
- [ ] Có dữ liệu sản phẩm trong database
- [ ] Port 8080 không bị conflict
- [ ] Đủ RAM cho Chrome browser (tests mở Chrome)

---

## 🎯 Test Coverage

### TC_PROD_001: Hiển thị và Phân trang
- ✅ TC_PROD_001_01: Hiển thị danh sách sản phẩm
- ✅ TC_PROD_001_02: Phân trang danh sách sản phẩm
- ✅ TC_PROD_001_03: Số lượng sản phẩm mỗi trang

### TC_PROD_002: Tìm kiếm và Filter
- ✅ TC_PROD_002_01: Tìm kiếm theo từ khóa
- ✅ TC_PROD_002_02: Lọc theo khoảng giá
- ✅ TC_PROD_002_03: Lọc nhanh theo giá
- ✅ TC_PROD_002_04: Lọc theo danh mục
- ✅ TC_PROD_002_05: Kết hợp tìm kiếm và lọc

**Tổng: 8/8 tests implemented với retry logic**

---

## 📚 Tài Liệu Liên Quan

- [SELENIUM_COMMON_ERRORS.md](./SELENIUM_COMMON_ERRORS.md) - Các lỗi Selenium thường gặp
- [FIX_DATABASE_CONNECTION_RESET.md](./FIX_DATABASE_CONNECTION_RESET.md) - Fix lỗi connection reset
- [HOW_TO_RUN_SELENIUM_TESTS.md](./HOW_TO_RUN_SELENIUM_TESTS.md) - Hướng dẫn chi tiết
- [SETUP_CHROMEDRIVER.md](./SETUP_CHROMEDRIVER.md) - Cài đặt ChromeDriver

---

**Cập nhật:** 2024 - Tất cả tests đã có retry logic và delay để xử lý connection issues
