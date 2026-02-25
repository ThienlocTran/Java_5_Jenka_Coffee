# 📋 ĐÁNH GIÁ & CẢI THIỆN PLANNING TASK - UNIT TEST

## 📊 TỔNG QUAN DỰ ÁN

**Dự án:** Jenka Coffee - Hệ thống quản lý cửa hàng cà phê  
**Công nghệ:** Spring Boot, JPA/Hibernate, Thymeleaf, MySQL  
**Nhóm:** 4 thành viên (Thiên Lộc, Chí Bảo, Tuấn Khoa, Khánh Ý)

### Kiến trúc dự án:
- **Controller Layer:** Admin & Site controllers
- **Service Layer:** Business logic với interface + implementation
- **Repository Layer:** JPA repositories
- **Entity Layer:** Product, Category, Order, Account, OrderDetail
- **DTO Layer:** Request/Response objects với validation
- **Exception Handling:** Custom exceptions + GlobalExceptionHandler

---

## ✅ ĐIỂM MẠNH CỦA PLANNING HIỆN TẠI

### 1. Phân chia công việc hợp lý
- ✅ Mỗi thành viên có module riêng biệt, không chồng chéo
- ✅ Số lượng test case cân đối (20-30 TCs/người)
- ✅ Bao phủ đầy đủ các chức năng chính

### 2. Cấu trúc test case rõ ràng
- ✅ Có đầy đủ: Tên TC, Điều kiện tiên quyết, Các bước, Input, Expected Result
- ✅ Đặt tên test case theo chuẩn: TC_MODULE_XXX

### 3. Bao phủ nhiều kịch bản
- ✅ Happy path (đúng)
- ✅ Negative cases (sai, thiếu, null)
- ✅ Edge cases (giới hạn, đặc biệt)
- ✅ Business rules (ràng buộc nghiệp vụ)

---

## ⚠️ VẤN ĐỀ CẦN SỬA & CẢI THIỆN

### 🔴 **THIÊN LỘC - Product & Image (25 TCs)**

#### ❌ Sai lệch với source code thực tế:

**1. TC_PROD_002 - Tên trống:**
- ❌ **Hiện tại:** "Hệ thống báo lỗi: Tên sản phẩm không được để trống"
- ✅ **Thực tế:** Entity `Product` có `@Column(nullable = false)` nhưng KHÔNG có validation `@NotBlank` trong controller
- 🔧 **Sửa:** Cần test ở tầng Service/Repository, không phải Controller

**2. TC_PROD_003, TC_PROD_004 - Giá âm/bằng 0:**
- ❌ **Hiện tại:** "Hệ thống báo lỗi"
- ✅ **Thực tế:** Không có validation giá trong code, chỉ có kiểu `BigDecimal`
- 🔧 **Sửa:** Cần thêm validation hoặc test database constraint

**3. TC_PROD_005 - Danh mục không hợp lệ:**
- ❌ **Hiện tại:** "Hệ thống báo lỗi: Danh mục không hợp lệ"
- ✅ **Thực tế:** Code sẽ throw `ResourceNotFoundException` từ `CategoryRepository`
- 🔧 **Sửa:** Expected Result = "ResourceNotFoundException: Category not found"

**4. TC_PROD_008 - Cập nhật SP không tồn tại:**
- ❌ **Hiện tại:** "Báo lỗi hoặc 404"
- ✅ **Thực tế:** `ProductServiceImpl.update()` throw `ResourceNotFoundException`
- 🔧 **Sửa:** Expected Result = "ResourceNotFoundException: Product not found with id: 999"

**5. TC_PROD_009, TC_PROD_010 - Toggle Available:**
- ✅ **Đúng:** Code có method `toggleAvailable()`
- ⚠️ **Lưu ý:** Cần test cả 2 chiều: true → false và false → true

**6. TC_IMG_001-008 - Upload ảnh:**
- ❌ **Thiếu:** Không test `ImageService.compressImage()` - tính năng NÉN ẢNH
- ❌ **Thiếu:** Không test `UploadService.saveProductImage()` với Cloudinary
- 🔧 **Thêm TC:**
  - TC_IMG_009: Test nén ảnh từ 2MB → < 500KB
  - TC_IMG_010: Test upload lên Cloudinary thành công
  - TC_IMG_011: Test Cloudinary fail → fallback local storage

**7. TC_VAL_001-003 - Validation:**
- ❌ **Thiếu:** Không có DTO validation trong `AdminProductController`
- ✅ **Thực tế:** Controller nhận trực tiếp `Product` entity, không qua DTO
- 🔧 **Sửa:** Test validation ở tầng database (length, constraints)

**8. TC_PROD_013, TC_PROD_014 - Phân trang & Sort:**
- ✅ **Đúng:** Code có `findAllPaginated()` và `filterProductsPaginated()`
- ⚠️ **Thiếu:** Không test `searchProductsPaginated()` và `filterProductsWithAllCriteria()`

#### 📝 Test Cases CẦN THÊM:

```
TC_PROD_015: Test searchProductsPaginated() với keyword
TC_PROD_016: Test filterProductsWithAllCriteria() (category + price + keyword)
TC_PROD_017: Test getStockStatus() - IN_STOCK, LOW_STOCK, OUT_OF_STOCK
TC_PROD_018: Test getStockMessage() - hiển thị đúng message
TC_PROD_019: Test getRelatedProducts() - lấy 4 SP cùng loại
TC_PROD_020: Test getCategoryCounts() - đếm SP theo category
```

---

### 🟡 **TUẤN KHOA - Cart & Checkout (25 TCs)**

#### ✅ Phần Cart (TC_CART_001-010) - TỐT:
- ✅ Bao phủ đầy đủ CRUD operations
- ✅ Test cộng dồn số lượng (TC_CART_002)
- ✅ Test edge cases (SL = 0, SL âm, quá tồn kho)

#### ⚠️ Phần Checkout (TC_CHK_001-010) - CẦN SỬA:

**1. TC_CHK_001 - Checkout thành công:**
- ❌ **Thiếu:** Không test trừ tồn kho trong `OrderServiceImpl.checkout()`
- ✅ **Thực tế:** Code có logic trừ tồn kho: `product.setQuantity(product.getQuantity() - detail.getQuantity())`
- 🔧 **Sửa:** Thêm vào Expected Result: "Tồn kho giảm đúng số lượng đã mua"

**2. TC_CHK_002 - Checkout chưa login:**
- ✅ **Đúng:** `CheckoutController` kiểm tra session user
- ⚠️ **Lưu ý:** Cần test cả `AuthInterceptor` chặn request

**3. TC_CHK_007, TC_CHK_008 - Trừ tồn kho:**
- ✅ **Đúng:** Có logic trong code
- ❌ **Trùng:** Đã test ở TC_CHK_001
- 🔧 **Gộp:** Gộp vào TC_CHK_001 hoặc tách riêng test Service layer

**4. TC_CHK_009, TC_CHK_010 - Voucher:**
- ❌ **SAI HOÀN TOÀN:** Code KHÔNG có logic áp dụng voucher trong `CheckoutServiceImpl`
- ✅ **Thực tế:** Entity `Order` có field `voucherCode` nhưng chưa implement
- 🔧 **Xóa hoặc đánh dấu:** "TODO - Chưa implement"

#### 📝 Test Cases CẦN THÊM:

```
TC_CHK_011: Test validation CheckoutRequest (phone, email, address format)
TC_CHK_012: Test buildFullAddress() - ghép địa chỉ đầy đủ
TC_CHK_013: Test InsufficientStockException khi hết hàng
TC_CHK_014: Test transaction rollback khi lỗi
TC_CHK_015: Test tạo OrderDetail đúng (price, quantity, product)
```

#### ⚠️ Phần Order (TC_ORD_001-005) - CẦN SỬA:

**1. TC_ORD_002 - User hủy đơn:**
- ❌ **Thiếu:** Không có logic hủy đơn trong `OrderServiceImpl`
- 🔧 **Kiểm tra:** Xem có method `cancelOrder()` không

**2. TC_ORD_003, TC_ORD_004 - Admin duyệt/hủy đơn:**
- ❌ **Thiếu:** Không có `AdminOrderController` logic update status
- 🔧 **Kiểm tra:** Xem có method `updateOrderStatus()` không

**3. TC_ORD_004 - Hoàn tồn kho:**
- ❌ **Thiếu:** Code KHÔNG có logic hoàn tồn kho khi hủy đơn
- 🔧 **Thêm:** Cần implement hoặc đánh dấu "TODO"

---

### 🟢 **CHÍ BẢO - Category (20 TCs)**

#### ✅ Phần tốt:
- ✅ Test CRUD đầy đủ
- ✅ Test duplicate ID (TC_CAT_002)
- ✅ Test xóa category có sản phẩm (TC_CAT_008) - đúng với `BusinessRuleException`

#### ⚠️ Cần sửa:

**1. TC_CAT_004 - ID có ký tự đặc biệt:**
- ❌ **Hiện tại:** "Báo lỗi hoặc tự động xóa"
- ✅ **Thực tế:** `CategoryRequest` có `@Pattern(regexp = "^[A-Z0-9_]+$")`
- 🔧 **Sửa:** Expected Result = "Validation error: ID chỉ chứa chữ HOA, số và _"

**2. TC_CAT_006 - Cập nhật ID:**
- ✅ **Đúng:** ID là Primary Key, không cho sửa
- ⚠️ **Lưu ý:** Form HTML nên disable input ID khi edit

**3. TC_CAT_011, TC_CAT_012 - Icon:**
- ✅ **Đúng:** Code có logic icon mặc định trong `getCategoryIcons()`
- ⚠️ **Lưu ý:** Icon được map cứng, không upload file

**4. TC_CAT_017 - ID chữ thường:**
- ✅ **Đúng:** `CategoryRequest.normalize()` tự động uppercase
- ⚠️ **Lưu ý:** Test cả ở Service layer

**5. TC_CAT_019 - Xóa nhiều DM:**
- ❌ **SAI:** Code KHÔNG có chức năng xóa nhiều (bulk delete)
- 🔧 **Xóa TC này** hoặc đánh dấu "TODO"

#### 📝 Test Cases CẦN THÊM:

```
TC_CAT_021: Test findByIdOrThrow() - throw ResourceNotFoundException
TC_CAT_022: Test deleteOrThrow() - throw BusinessRuleException khi có SP
TC_CAT_023: Test createCategory() - normalize data (uppercase ID)
TC_CAT_024: Test updateCategory() - giữ nguyên ID
TC_CAT_025: Test countProductsByCategory() - đếm đúng số SP
```

---

### 🔵 **KHÁNH Ý - Auth & Account (30 TCs)**

#### ✅ Phần tốt:
- ✅ Bao phủ đầy đủ authentication flow
- ✅ Test activation & password reset (TC_PWD_001-009)
- ✅ Test role-based access (TC_PWD_006, TC_PWD_007)

#### ⚠️ Cần sửa:

**1. TC_AUTH_01 - Đăng nhập đúng:**
- ✅ **Đúng:** `AccountService.authenticate()` verify BCrypt password
- ⚠️ **Lưu ý:** Cần test cả 3 cách login: username, email, phone

**2. TC_AUTH_04 - Chưa Active:**
- ✅ **Đúng:** Code kiểm tra `account.getActivated()`
- ⚠️ **Lưu ý:** Cần test cả activation qua EMAIL và PHONE (OTP)

**3. TC_AUTH_05 - Bị Khóa:**
- ✅ **Đúng:** `activated = false` = bị khóa
- ⚠️ **Lưu ý:** Không có field `banned` riêng, dùng chung `activated`

**4. TC_AUTH_06 - Remember Me:**
- ✅ **Đúng:** Code có `CookieService.createRememberMeCookie()`
- ⚠️ **Lưu ý:** Cookie expire 30 days

**5. TC_AUTH_08 - Đăng ký thành công:**
- ❌ **Thiếu:** Không test gửi email/OTP activation
- ✅ **Thực tế:** `AccountServiceImpl.register()` gửi email hoặc OTP
- 🔧 **Thêm:** Expected Result = "Gửi email/OTP kích hoạt thành công"

**6. TC_AUTH_09, TC_AUTH_10 - Trùng username/email:**
- ✅ **Đúng:** Code có validation trong `createAccount()`
- ⚠️ **Lưu ý:** Throw `ValidationException`

**7. TC_ACC_006-009 - Admin quản lý User:**
- ✅ **Đúng:** Code có `AdminAccountController`
- ⚠️ **Lưu ý:** Test cả `toggleActivation()`, `lockAccount()`, `unlockAccount()`

**8. TC_ACC_009 - Xóa User có đơn:**
- ❌ **SAI:** Code KHÔNG kiểm tra đơn hàng, chỉ kiểm tra admin cuối cùng
- ✅ **Thực tế:** `canDeleteAccount()` chỉ check admin count
- 🔧 **Sửa:** Expected Result = "Không thể xóa admin cuối cùng"

**9. TC_PWD_001-004 - Quên mật khẩu:**
- ✅ **Đúng:** Code có `requestPasswordReset()` và `resetPassword()`
- ⚠️ **Lưu ý:** Có 2 flow: EMAIL (token) và PHONE (OTP)

**10. TC_PWD_005 - Kích hoạt tài khoản:**
- ✅ **Đúng:** Code có `activateAccount(token)`
- ⚠️ **Lưu ý:** Token expire 24 hours

**11. TC_PWD_006, TC_PWD_007 - Role Admin/User:**
- ❌ **SAI:** Code KHÔNG dùng Spring Security roles
- ✅ **Thực tế:** Dùng `AuthInterceptor` check session + `account.getAdmin()`
- 🔧 **Sửa:** Test `AuthInterceptor.preHandle()` thay vì Spring Security

**12. TC_PWD_008, TC_PWD_009 - OTP:**
- ✅ **Đúng:** Code có `OTPService` và `verifyPhoneOTP()`
- ⚠️ **Lưu ý:** OTP expire 5 minutes

#### 📝 Test Cases CẦN THÊM:

```
TC_AUTH_13: Test login bằng email
TC_AUTH_14: Test login bằng phone
TC_AUTH_15: Test BCrypt password hashing
TC_ACC_010: Test upload avatar với ImageService
TC_ACC_011: Test adminResetPassword()
TC_PWD_010: Test activation method (EMAIL vs PHONE)
TC_PWD_011: Test resendActivation()
TC_PWD_012: Test OTP expire (> 5 minutes)
```

---

## 🎯 KHUYẾN NGHỊ TỔNG THỂ

### 1. Phân loại Test Cases theo Layer

#### ❌ **Hiện tại:** Tất cả test cases trộn lẫn Controller + Service + Repository

#### ✅ **Nên làm:** Tách riêng theo layer

**A. Unit Test - Service Layer (Ưu tiên cao)**
```
- Test business logic thuần túy
- Mock dependencies (Repository, UploadService, EmailService)
- Ví dụ: ProductServiceImpl, CartServiceImpl, CheckoutServiceImpl
```

**B. Integration Test - Controller Layer**
```
- Test HTTP request/response
- Test validation
- Test exception handling
- Ví dụ: AdminProductController, CheckoutController
```

**C. Repository Test**
```
- Test custom queries
- Test pagination
- Ví dụ: ProductRepository.findByCategoryAndPriceRange()
```

### 2. Cấu trúc Test Case chuẩn

```java
// ✅ TỐT - Test Service Layer
@Test
void testAddToCart_NewProduct_ShouldAddSuccessfully() {
    // Given
    Integer productId = 1;
    Product mockProduct = new Product();
    mockProduct.setId(1);
    mockProduct.setName("Espresso");
    mockProduct.setPrice(BigDecimal.valueOf(50000));
    
    when(productService.findById(productId)).thenReturn(mockProduct);
    
    // When
    cartService.add(productId);
    
    // Then
    assertEquals(1, cartService.getCount());
    assertEquals(50000.0, cartService.getAmount());
}
```

### 3. Coverage mục tiêu

| Layer | Coverage Target | Priority |
|-------|----------------|----------|
| Service | 80-90% | ⭐⭐⭐ Cao nhất |
| Controller | 70-80% | ⭐⭐ Trung bình |
| Repository | 60-70% | ⭐ Thấp (đã có Spring Data JPA) |
| Entity | 50% | ⭐ Thấp (chỉ test validation) |

### 4. Test Cases ưu tiên

#### 🔴 **PHẢI CÓ (Must Have):**
1. Happy path - các luồng chính thành công
2. Validation errors - dữ liệu sai/thiếu
3. Business rules - ràng buộc nghiệp vụ
4. Exception handling - xử lý lỗi

#### 🟡 **NÊN CÓ (Should Have):**
5. Edge cases - giới hạn, đặc biệt
6. Concurrent access - đồng thời
7. Performance - hiệu năng

#### 🟢 **TỐT NẾU CÓ (Nice to Have):**
8. Security - bảo mật
9. Integration - tích hợp
10. End-to-end - toàn bộ luồng

---

## 📊 BẢNG TỔNG HỢP SỐ LƯỢNG TEST CASES

| Thành viên | Module | TCs Hiện tại | TCs Cần thêm | TCs Cần xóa/sửa | Tổng sau sửa |
|------------|--------|--------------|--------------|-----------------|--------------|
| **Thiên Lộc** | Product & Image | 25 | +6 | -3 (sửa) | 28 |
| **Tuấn Khoa** | Cart & Checkout | 25 | +5 | -2 (xóa voucher) | 28 |
| **Chí Bảo** | Category | 20 | +5 | -1 (xóa bulk delete) | 24 |
| **Khánh Ý** | Auth & Account | 30 | +8 | -2 (sửa) | 36 |
| **TỔNG** | | **100** | **+24** | **-8** | **116** |

---

## 🚀 KẾ HOẠCH THỰC HIỆN

### Tuần 1: Chuẩn bị (Hiện tại)
- [x] Review source code
- [x] Đánh giá planning task
- [ ] Sửa lại test cases theo khuyến nghị
- [ ] Tạo Excel test case template

### Tuần 2: Implement Unit Tests
- [ ] Setup JUnit 5 + Mockito
- [ ] Viết test cho Service layer (ưu tiên cao)
- [ ] Viết test cho Controller layer
- [ ] Đạt coverage 70%+

### Tuần 3: Integration & Report
- [ ] Viết integration tests
- [ ] Chạy toàn bộ test suite
- [ ] Tạo báo cáo coverage
- [ ] Demo & presentation

---

## 📝 CHECKLIST TRƯỚC KHI BẮT ĐẦU CODE

### Thiên Lộc - Product & Image
- [ ] Sửa TC_PROD_002-005 theo exception thực tế
- [ ] Thêm TC test ImageService.compressImage()
- [ ] Thêm TC test UploadService với Cloudinary
- [ ] Thêm TC test search & filter advanced
- [ ] Thêm TC test stock status & message

### Tuấn Khoa - Cart & Checkout
- [ ] Gộp TC_CHK_007-008 vào TC_CHK_001
- [ ] Xóa TC_CHK_009-010 (voucher chưa implement)
- [ ] Thêm TC test CheckoutRequest validation
- [ ] Thêm TC test InsufficientStockException
- [ ] Thêm TC test transaction rollback
- [ ] Kiểm tra xem có logic hủy đơn/hoàn tồn kho không

### Chí Bảo - Category
- [ ] Sửa TC_CAT_004 theo @Pattern validation
- [ ] Xóa TC_CAT_019 (bulk delete chưa có)
- [ ] Thêm TC test CategoryRequest.normalize()
- [ ] Thêm TC test exception handling
- [ ] Thêm TC test icon mapping

### Khánh Ý - Auth & Account
- [ ] Thêm TC test login bằng email/phone
- [ ] Sửa TC_ACC_009 theo logic thực tế
- [ ] Sửa TC_PWD_006-007 test AuthInterceptor
- [ ] Thêm TC test activation EMAIL vs PHONE
- [ ] Thêm TC test OTP expire
- [ ] Thêm TC test BCrypt hashing

---

## 🎓 TÀI LIỆU THAM KHẢO

### 1. JUnit 5 + Mockito
```java
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    @Mock
    private ProductRepository productRepository;
    
    @InjectMocks
    private ProductServiceImpl productService;
    
    @Test
    void testFindById_ExistingProduct_ShouldReturnProduct() {
        // Test implementation
    }
}
```

### 2. Spring Boot Test
```java
@SpringBootTest
@AutoConfigureMockMvc
class AdminProductControllerTest {
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void testCreateProduct_ValidData_ShouldReturn200() throws Exception {
        mockMvc.perform(post("/admin/product/save")
            .param("name", "Test Product")
            .param("price", "100000"))
            .andExpect(status().is3xxRedirection());
    }
}
```

### 3. AssertJ - Assertions mạnh mẽ
```java
assertThat(cartService.getCount()).isEqualTo(1);
assertThat(cartService.getAmount()).isCloseTo(50000.0, within(0.01));
assertThat(product.getName()).isNotBlank().hasSize(10);
```

---

## ✅ KẾT LUẬN

### Điểm mạnh:
1. ✅ Planning task có cấu trúc tốt, bao phủ đầy đủ chức năng
2. ✅ Phân chia công việc hợp lý giữa 4 thành viên
3. ✅ Có nhiều test cases cho edge cases và validation

### Cần cải thiện:
1. ⚠️ Một số test cases không khớp với source code thực tế
2. ⚠️ Thiếu test cho một số tính năng quan trọng (nén ảnh, OTP, transaction)
3. ⚠️ Chưa phân loại rõ ràng Unit Test vs Integration Test
4. ⚠️ Một số test cases test chức năng chưa implement (voucher, bulk delete)

### Khuyến nghị:
1. 🎯 Ưu tiên test Service layer trước (business logic)
2. 🎯 Sửa lại test cases theo source code thực tế
3. 🎯 Thêm test cases cho các tính năng còn thiếu
4. 🎯 Đạt coverage 70%+ cho Service layer

---

**Người đánh giá:** Kiro AI Assistant  
**Ngày:** 2026-02-25  
**Version:** 1.0
