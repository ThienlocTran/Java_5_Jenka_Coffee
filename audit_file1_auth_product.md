# 🔍 QA AUDIT REPORT — FILE 1
## TC_BATCH_01_AUTH_PRODUCT_v2.csv + TC_BATCH_01_PRODUCT_ADDENDUM_v2.csv
### vs. Java Tests: `ApiAdminProductControllerTest`, `ApiAdminProductAddendumTest`, `ProductServiceImplTest`, `ApiAuthControllerPublicTest`

---

## 1. OVERALL ASSESSMENT

| Dimension | Score | Verdict |
|---|---|---|
| Test Coverage | 8/10 | Tốt — 38 TC controller + 13 TC service, đầy đủ happy/unhappy path |
| Expected Result Quality | 6/10 | Nhiều TC chỉ ghi "Return 400" mà thiếu field-level error message cụ thể |
| Test Data Realism | 6/10 | Dữ liệu thiếu tên sản phẩm thực tế (dùng "Test Product" thay vì tên tiếng Việt thực) |
| Assertion Depth | 5/10 | Phần lớn chỉ check HTTP status; DB state verification chỉ có ở Addendum |
| Security Coverage | 7/10 | Có RCE/DoS tests, nhưng magic bytes check vẫn là GAP chưa sửa được |
| Negative/Edge Coverage | 8/10 | Tốt — có boundary, null, empty, negative, oversized, concurrent |
| Bug Detection Capability | 7/10 | Tốt nhờ gap-documenting tests; nhưng một số mock tests không fail khi impl sai |

**Overall Quality Score: 6.7 / 10**

**Key Strengths:**
- Hệ thống test có cấu trúc rõ ràng, tách biệt Controller vs Service layer
- Có "gap-documenting tests" (TC-039, TC-046) — rất có giá trị cho production
- Addendum file bổ sung real-flow DB verification — đây là điểm mạnh đúng hướng
- `ApiAdminProductAddendumTest` dùng `@SpringBootTest` + real repo — phát hiện bug thực tế

**Critical Weaknesses:**
- `ApiAuthControllerPublicTest.testLoginNotActivated()` có **expected result SAI** (message không khớp)
- `ApiAdminProductAddendumTest.test_deleteProduct_concurrentRequests()` có **assertion lỗi** do `@Transactional` conflict với concurrency
- Service tests `TC-PRD-SER-010` và `TC-PRD-SER-012/013` có **mâu thuẫn nội bộ** giữa CSV spec và Java implementation
- `TC-PRD-CTRL-008` kiểm tra `status().isOk()` thay vì `status().isCreated()` — sai spec

---

## 2. CRITICAL ISSUES

### ❌ ISSUE 1: `testLoginNotActivated()` — Expected Result SAI

**File:** `ApiAuthControllerPublicTest.java` — line 181

**CSV spec:** TC không explicitly cover login-not-activated, nhưng test hiện tại kỳ vọng:
```java
.andExpect(jsonPath("$.message").value("Sai tên đăng nhập hoặc mật khẩu!"))
```

**Vấn đề:** Account not-activated nên trả message **khác** với invalid credentials:
- `invalidCredentials` → "Sai tên đăng nhập hoặc mật khẩu!"
- `notActivated` → "Tài khoản chưa được kích hoạt. Vui lòng kiểm tra email."

Nếu controller thực sự dùng cùng message → đây là **business logic bug**: user không biết tại sao đăng nhập thất bại.

**Verdict:** Test này sẽ pass ngay cả khi implementation trả về sai message cho not-activated — **zero bug detection**.

---

### ❌ ISSUE 2: `TC-PRD-CTRL-008` — Sai Status Code Expected

**File:** `ApiAdminProductControllerTest.java` — line 257

```java
.andExpect(status().isOk())  // ❌ SAI
```

**CSV spec ghi:** "Return 201 Created with product object"

**Java test kỳ vọng 200 OK** thay vì 201 Created. Nếu controller bị refactor trả về 201, test này sẽ **fail sai** (false negative). Tất cả các TC CREATE tương tự cũng dùng `isOk()`.

---

### ❌ ISSUE 3: `TC-PRD-SER-010` — Mâu thuẫn CSV vs Java Implementation

**CSV spec:**
> "Expected: strict validation → reject null name → BusinessRuleException"

**Java test (`ProductServiceImplTest.TC_PRD_SER_010`):**
```java
assertDoesNotThrow(() -> productService.updateProductFromRequest(1, null, ...))
// + verify save() called with name=null
```

**Addendum test (`ApiAdminProductAddendumTest.test_productService_updateNullName`):**
```java
assertThrows(Exception.class, ...)
```

**Hai file test CÓ KẾT QUẢ KỲ VỌNG MÂU THUẪN NHAU.** Một cái expect DoesNotThrow, cái kia expect throws. Cả hai không thể đúng cùng lúc. Đây là lỗi QA nghiêm trọng: ai chạy 2 test này sẽ thấy 1 cái luôn fail.

---

### ❌ ISSUE 4: `TC-PRD-SER-012/013` — Tự mâu thuẫn về cascade behavior

**TC-PRD-SER-012 CSV spec:**
> "ProductImages deleted from DB + physical files deleted from storage"
> "Verify: productImageRepo.deleteByProductId() được gọi"

**Java `ProductServiceImplTest.TC_PRD_SER_012`:**
```java
verify(productImageRepository, never()).deleteByProductId(1); // ❌ NGƯỢC lại!
```

**TC-PRD-SER-013 CSV spec:**
> "Transaction rollback — product vẫn còn trong DB sau failed image delete"

**Java `ProductServiceImplTest.TC_PRD_SER_013`:**
```java
verifyNoInteractions(productImageRepository); // Tức là image delete KHÔNG được gọi
```

**Verdict:** CSV mô tả hành vi mong muốn (có cascade), Java test mô tả hành vi thực tế (không có cascade = GAP). Hai file nói về hai thứ khác nhau mà không có annotation rõ ràng.

---

### ❌ ISSUE 5: `TC-PRD-CTRL-042` Concurrent Delete — Test Không Reliable

**File:** `ApiAdminProductAddendumTest.java` — line 131

```java
@Transactional  // Class-level annotation
void test_deleteProduct_concurrentRequests()
```

**Vấn đề:** Class được annotate `@Transactional` ở cấp class, nhưng concurrent test dùng `ExecutorService` với nhiều thread. Các thread con **KHÔNG kế thừa transaction của test thread** → mỗi thread tạo transaction riêng, nhưng test thread đang hold lock → behavior không xác định, test có thể **deadlock hoặc fail ngẫu nhiên** (flaky test).

---

## 3. IMPROVEMENT SUGGESTIONS

### 🔧 Rewrite 1: TC-AUTH — Login Not Activated (WEAK → STRONG)

**Original (weak):**
```java
.andExpect(jsonPath("$.message").value("Sai tên đăng nhập hoặc mật khẩu!"))
```

**Rewritten (strong):**
```java
@Test
@DisplayName("TC-AUTH-NOT-ACTIVATED: Login với tài khoản chưa kích hoạt - trả 401 với message phân biệt rõ ràng")
void testLoginNotActivated_distinctMessage() throws Exception {
    // ...setup...
    mockMvc.perform(post("/api/auth/login")...)
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value("ERROR"))
        // CRITICAL: message PHẢI khác với invalid credentials
        .andExpect(jsonPath("$.message").value("Tài khoản chưa được kích hoạt. Vui lòng kiểm tra email."))
        // Verify: KHÔNG lộ thông tin nhạy cảm trong body
        .andExpect(jsonPath("$.data.accessToken").doesNotExist())
        .andExpect(jsonPath("$.data.password").doesNotExist());
    
    // Verify: generateAccessToken KHÔNG được gọi — tài khoản chưa active không được issue token
    verify(jwtService, never()).generateAccessToken(anyString(), anyBoolean());
}
```

---

### 🔧 Rewrite 2: TC-PRD-CTRL-008 CREATE Valid (WEAK → STRONG)

**Original (weak):**
```java
.andExpect(status().isOk())  // Sai spec
.andExpect(jsonPath("$.data.id").value(1))
.andExpect(jsonPath("$.data.name").value("Cà phê sữa"))
```

**Rewritten (strong):**
```java
@Test
@DisplayName("TC-PRD-CTRL-008: CREATE valid product - 201 Created với full response body")
void TC_PRD_CTRL_008_strong() throws Exception {
    // Test data realistic theo spec
    Product saved = createMockProduct(1, "Máy pha cà phê Delonghi ECAM 22.110.B", new BigDecimal("15990000"));
    saved.setFeatured(false);
    saved.setAvailable(true);
    
    when(productService.createProductFromRequest(...)).thenReturn(saved);
    
    mockMvc.perform(multipart("/api/admin/products")
            .file(imageFile)
            .param("name", "Máy pha cà phê Delonghi ECAM 22.110.B")
            .param("price", "15990000")
            .param("categoryId", "CF")
            .param("available", "true"))
        .andExpect(status().isCreated())  // 201 — khớp CSV spec
        .andExpect(jsonPath("$.status").value("SUCCESS"))
        .andExpect(jsonPath("$.data.id").value(1))
        .andExpect(jsonPath("$.data.name").value("Máy pha cà phê Delonghi ECAM 22.110.B"))
        .andExpect(jsonPath("$.data.price").value(15990000))
        .andExpect(jsonPath("$.data.available").value(true))
        .andExpect(jsonPath("$.data.featured").value(false))
        // CRITICAL: Verify slug được generate (không null)
        .andExpect(jsonPath("$.data.slug").exists())
        // CRITICAL: Category được populate
        .andExpect(jsonPath("$.data.category.id").value("CF"));
    
    // DB verification: service.create() phải được gọi đúng 1 lần
    verify(productService, times(1)).createProductFromRequest(any(), eq("CF"), any());
}
```

---

### 🔧 Rewrite 3: TC-PRD-SER-012 Delete Cascade (CONTRADICTORY → CLEAR)

**Original (contradicts CSV spec):**
```java
verify(productImageRepository, never()).deleteByProductId(1); // Mâu thuẫn với CSV
```

**Rewritten (documents actual behavior AND expected fix):**
```java
@Test
@DisplayName("TC-PRD-SER-012: [GAP DOCUMENTED] deleteProductWithValidation - cascade images qua DB constraint, KHÔNG qua Java code")
void TC_PRD_SER_012_documented() {
    // ACTUAL behavior: service không gọi productImageRepository.deleteByProductId()
    // Cascade phụ thuộc vào DB schema ON DELETE CASCADE
    // GAP: nếu DB thiếu CASCADE, ProductImages bị orphan
    
    when(productRepository.existsById(1)).thenReturn(true);
    when(productRepository.countOrdersByProductId(1)).thenReturn(0L);
    doNothing().when(productRepository).deleteById(1);
    
    productService.deleteProductWithValidation(1);
    
    verify(productRepository).deleteById(1);
    
    // Document the GAP clearly:
    // ❌ GAP: Java code does NOT explicitly delete images
    // ✅ EXPECTED (production-safe): service should call productImageRepository.deleteByProductId(1)
    //    AND uploadService.delete() for each image URL
    // TODO: When gap is fixed, change this to:
    //   verify(productImageRepository).deleteByProductId(1);
    verify(productImageRepository, never()).deleteByProductId(1); // DOCUMENTS current (incorrect) behavior
}
```

---

## 4. MISSING TESTS

### 🚨 Missing Test 1: Slug Generation Uniqueness (HIGH PRIORITY)
**Scenario:** Tạo 2 sản phẩm có tên giống hệt nhau ("Cà phê sữa") → slug collision
- Hiện tại: có retry loop 3 lần với suffix random
- **Missing:** Test verify slug thực sự unique sau khi tạo, không phải chỉ mock

### 🚨 Missing Test 2: TC-AUTH-001 (No Token → 401) không có Java test
**CSV ghi:** "Return 401 Unauthorized" khi không có token
Không có test nào trong `ApiAdminProductControllerTest` test **hoàn toàn không có token** — tất cả dùng `@WithMockUser`. Cần:
```java
@Test
void TC_AUTH_001_noToken_returns401() throws Exception {
    // Không dùng @WithMockUser
    mockMvc.perform(get("/api/admin/products"))
        .andExpect(status().isUnauthorized()); // 401
}
```

### 🚨 Missing Test 3: Pagination totalItems/totalPages Accuracy
**Scenario:** DB có 25 sản phẩm, request page=0&size=10 → totalPages=3, totalItems=25
- Hiện tại: chỉ check `exists()`, không check giá trị chính xác
- **Missing:** `andExpect(jsonPath("$.data.totalItems").value(25))`

### 🚨 Missing Test 4: Product Image URL Verification after Upload
**Scenario:** Upload ảnh hợp lệ → verify `imageUrl` trong DB là absolute URL (không phải relative path)
- Hiện tại: `TC-PRD-CTRL-032` chỉ check `message = "Upload ảnh thành công"`, không verify URL format

### 🚨 Missing Test 5: Toggle Featured message theo state (mentioned in CSV notes)
**CSV TC-PRD-CTRL-027 notes:** "verify message thay đổi theo trạng thái"
- featured=false→true: message = "Đã đánh dấu sản phẩm nổi bật"
- featured=true→false: message = "Đã bỏ đánh dấu sản phẩm nổi bật"
- Java test chỉ test 1 chiều (false→true), thiếu test chiều ngược lại

---

## 5. TOP 5 MOST IMPORTANT TESTS

| Rank | Test ID | File | Lý do quan trọng |
|---|---|---|---|
| 🥇 1 | `TC_PRD_SER_009` | `ProductServiceImplTest` | Verify retry loop + exception wrapping — production crash nếu DB unique constraint hit |
| 🥈 2 | `TC_PRD_CTRL_039` (Addendum real-flow) | `ApiAdminProductAddendumTest` | Duy nhất test thực sự hit DB với duplicate → bắt được unhandled DataIntegrityViolationException → 500 bug |
| 🥉 3 | `TC_PRD_CTRL_046` | Cả 2 files | RCE risk: PE binary disguised as image — critical security |
| 4 | `TC_PRD_SER_012` | `ProductServiceImplTest` | Orphan image bug — data inconsistency nếu DB cascade thiếu |
| 5 | `TC_AUTH_002` | `ApiAdminProductControllerTest` | Authorization gap — ROLE_USER access admin = privilege escalation |

---

## 6. COMPILE/RUNTIME ERRORS DETECTED

### ⚠️ Error 1: `ApiAdminProductAddendumTest` — Concurrency Test với `@Transactional`
```
Class-level @Transactional + ExecutorService → child threads không thể tham gia transaction test
→ ConcurrentModificationException hoặc test luôn pass/fail ngẫu nhiên (flaky)
```
**Fix:** Remove `@Transactional` từ concurrent test, hoặc tách riêng test class.

### ⚠️ Error 2: `TC_PRD_SER_010` vs `test_productService_updateNullName`
```
Hai test cùng test null name update:
- ProductServiceImplTest: assertDoesNotThrow → PASS nếu service không validate
- ApiAdminProductAddendumTest: assertThrows → FAIL vì service không validate
→ CI pipeline sẽ có 1 test luôn fail
```

---

## 7. SUMMARY CHECKLIST

| Item | Status |
|---|---|
| TC-AUTH-001 (no token) có Java test | ❌ Thiếu |
| TC-AUTH-002 (wrong role) có Java test | ✅ Có (TC_AUTH_002) |
| TC-PRD-CTRL-001 đến -038 đầy đủ | ✅ 38/38 |
| TC-PRD-SER-001 đến -008 đầy đủ | ✅ 8/8 |
| TC Addendum -039 đến -046 đầy đủ | ✅ 8/8 |
| DB state verification trong controller tests | ❌ Chỉ có ở Addendum |
| Concurrent test reliable | ❌ Flaky (@Transactional conflict) |
| Expected status code 201 cho CREATE | ❌ Dùng 200 sai spec |
| Message distinction for notActivated vs invalidCredentials | ❌ Dùng cùng message |
| Mâu thuẫn TC-PRD-SER-010/012/013 | ❌ Cần resolve |
