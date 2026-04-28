# Jenka Coffee — Test Case Document (Part 4: REMAINING COVERAGE)
**Source mapped:** `CheckoutRequest`, `OrderServiceImpl`, `ProductServiceImpl`, `ApiProductController`

---

## MODULE 6: CHECKOUT REQUEST VALIDATION (Chi tiết)

---

### TC-VAL-001 — phone không đúng format Việt Nam → 400

| Field | Value |
|-------|-------|
| **TC ID** | TC-VAL-001 |
| **Title** | POST /api/orders/checkout — phone không theo format VN phải fail validation |
| **Method mapped** | `CheckoutRequest.phone` — `@Pattern(regexp = "^(0\|\\+84)...")` |
| **Pre-conditions** | Cart hợp lệ |
| **Test data** | `"phone": "1234567890"` (không bắt đầu bằng 0 hoặc +84) |
| **Expected result** | HTTP 400, `status="ERROR"`, message chứa lỗi phone pattern |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |

```java
@ParameterizedTest
@ValueSource(strings = {
    "1234567890",   // không bắt đầu 0/+84
    "0123456789",   // đầu số không hợp lệ (01x deprecated)
    "090123456",    // thiếu 1 số
    "09012345678",  // thừa 1 số
    "abcdefghijk",  // chữ cái
    ""              // empty
})
void checkout_invalidPhone_returns400(String phone) throws Exception {
    CheckoutRequest req = buildValidCheckoutRequest();
    req.setPhone(phone);
    mockMvc.perform(post("/api/orders/checkout")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req))
            .with(user("user")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("ERROR"));
}

@ParameterizedTest
@ValueSource(strings = {
    "0912345678",   // Viettel
    "0987654321",   // Mobifone
    "+84912345678", // +84 format
    "0765432109"    // Vietnamobile
})
void checkout_validVietnamesePhone_passes(String phone) throws Exception {
    CheckoutRequest req = buildValidCheckoutRequest();
    req.setPhone(phone);
    when(cartService.getItems()).thenReturn(List.of(buildCartItem()));
    // ... setup mocks
    mockMvc.perform(post("/api/orders/checkout")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req))
            .with(user("user")))
        .andExpect(status().isOk());
}
```

---

### TC-VAL-002 — note vượt 500 ký tự → 400

| Field | Value |
|-------|-------|
| **TC ID** | TC-VAL-002 |
| **Title** | POST /api/orders/checkout — note > 500 ký tự phải fail @Size validation |
| **Method mapped** | `CheckoutRequest.note` — `@Size(max = 500)` |
| **Pre-conditions** | Cart hợp lệ |
| **Test data** | `"note": "A".repeat(501)` (501 ký tự) |
| **Expected result** | HTTP 400, `status="ERROR"` |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |
| **Notes** | Boundary: note = 500 ký tự phải PASS. note = 501 phải FAIL. |

```java
@Test
void checkout_noteTooLong_returns400() throws Exception {
    CheckoutRequest req = buildValidCheckoutRequest();
    req.setNote("A".repeat(501));
    mockMvc.perform(post("/api/orders/checkout")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req))
            .with(user("user")))
        .andExpect(status().isBadRequest());
}

@Test
void checkout_noteExactly500Chars_passes() throws Exception {
    CheckoutRequest req = buildValidCheckoutRequest();
    req.setNote("A".repeat(500)); // boundary - phải pass
    when(cartService.getItems()).thenReturn(List.of(buildCartItem()));
    // ... setup mocks
    mockMvc.perform(post("/api/orders/checkout")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req))
            .with(user("user")))
        .andExpect(status().isOk());
}
```

---

### TC-VAL-003 — voucherCode vượt 20 ký tự → 400

| Field | Value |
|-------|-------|
| **TC ID** | TC-VAL-003 |
| **Title** | POST /api/orders/checkout — voucherCode > 20 ký tự phải fail @Size |
| **Method mapped** | `CheckoutRequest.voucherCode` — `@Size(max = 20)` |
| **Pre-conditions** | Cart hợp lệ |
| **Test data** | `"voucherCode": "ABCDEFGHIJKLMNOPQRSTU"` (21 ký tự) |
| **Expected result** | HTTP 400 |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |

---

### TC-VAL-004 — fullname có ký tự đặc biệt HTML → fail Pattern

| Field | Value |
|-------|-------|
| **TC ID** | TC-VAL-004 |
| **Title** | POST /api/orders/checkout — fullname chứa `<script>` phải fail @Pattern |
| **Method mapped** | `CheckoutRequest.fullname` — `@Pattern(regexp = "^[\\p{L}\\s\\d_-]+$")` |
| **Pre-conditions** | Cart hợp lệ |
| **Test data** | `"fullname": "<script>alert(1)</script>"` |
| **Expected result** | HTTP 400 — `<` và `>` không khớp pattern `[\p{L}\s\d_-]` |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |
| **Notes** | Server-side XSS prevention tầng validation, trước cả service layer |

---

### TC-VAL-005 — address chứa `<>` → fail Pattern

| Field | Value |
|-------|-------|
| **TC ID** | TC-VAL-005 |
| **Title** | POST /api/orders/checkout — address chứa `<>` phải fail `@Pattern(regexp = "^[^<>]+$")` |
| **Method mapped** | `CheckoutRequest.address` — `@Pattern(regexp = "^[^<>]+$")` |
| **Pre-conditions** | Cart hợp lệ |
| **Test data** | `"address": "123 <b>Nguyễn Trãi</b>"` |
| **Expected result** | HTTP 400 |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |

---

## MODULE 7: PRODUCT FLAGS — requireContact & featured

---

### TC-PRD-014 — requireContact=true: sản phẩm có thể checkout (potential bug)

| Field | Value |
|-------|-------|
| **TC ID** | TC-PRD-014 |
| **Title** | checkout — sản phẩm có requireContact=true vẫn có thể được đặt online (không có guard) |
| **Method mapped** | `OrderServiceImpl.lockProducts()` — **không có check requireContact** |
| **Pre-conditions** | Cart có product với `requireContact=true`, `available=true` |
| **Test data** | Product: `{id:20, available:true, requireContact:true, price:15,000,000}` |
| **Expected result** | ⚠️ **HIỆN TẠI**: Order được tạo thành công (200 OK) — flag `requireContact` không được enforce tại checkout |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |
| **Notes** | **POTENTIAL BUG**: Nếu `requireContact=true` có nghĩa là "không mua online", thì checkout phải throw exception. Cần confirm với business requirements. Nếu là bug → cần thêm check trong `lockProducts()`. |

```java
@Test
void checkout_productWithRequireContact_currentlySucceeds_potentialBug() {
    // Document current behavior: requireContact is NOT enforced at checkout
    Product product = buildProduct(20, new BigDecimal("15000000"), true);
    product.setRequireContact(true); // This flag is NOT checked in lockProducts()
    
    CartItem item = new CartItem(20, "Máy pha cao cấp", BigDecimal.ZERO, 1, null);
    when(cartService.getItems()).thenReturn(List.of(item));
    when(entityManager.find(eq(Product.class), eq(20), any())).thenReturn(product);
    
    Order saved = new Order(); saved.setId(1L);
    when(orderRepository.save(any())).thenReturn(saved);

    // Currently: NO exception thrown — requireContact is informational only
    assertDoesNotThrow(() -> orderService.checkout(buildValidCheckoutRequest(), mockAccount()));
    
    // If business rule is: requireContact=true MUST block checkout,
    // then this test should instead expect: assertThrows(BusinessRuleException.class, ...)
}
```

---

### TC-PRD-015 — toggleFeatured: false → true → false cycle

| Field | Value |
|-------|-------|
| **TC ID** | TC-PRD-015 |
| **Title** | toggleFeatured — product featured=false, sau toggle phải thành true; toggle lần 2 thành false |
| **Method mapped** | `ProductServiceImpl.toggleFeatured()` line 596-601 |
| **Pre-conditions** | Product ID=15 với featured=false |
| **Test data** | `toggleFeatured(15)` hai lần |
| **Expected result** | Lần 1: `product.featured=true`. Lần 2: `product.featured=false` |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |

```java
@Test
void toggleFeatured_fromFalseToTrue() {
    Product product = new Product(); product.setId(15); product.setFeatured(false);
    when(productRepository.existsById(15)).thenReturn(true);
    when(productRepository.findByIdWithCategory(15)).thenReturn(Optional.of(product));
    when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    when(productRepository.findById(15)).thenReturn(Optional.of(product));

    Product result = productService.toggleFeatured(15);
    assertTrue(result.getFeatured());
}

@Test
void toggleFeatured_null_treatedAsFalse() {
    // Edge: getFeatured() returns null — Boolean.TRUE.equals(null) = false → toggle to true
    Product product = new Product(); product.setId(16); product.setFeatured(null);
    when(productRepository.existsById(16)).thenReturn(true);
    when(productRepository.findByIdWithCategory(16)).thenReturn(Optional.of(product));
    when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    when(productRepository.findById(16)).thenReturn(Optional.of(product));

    Product result = productService.toggleFeatured(16);
    assertTrue(result.getFeatured()); // null → treated as false → toggle to true
}
```

---

## MODULE 8: PRODUCT IMAGES

---

### TC-IMG-001 — GET /api/products/{id}/images — product không tồn tại → 404

| Field | Value |
|-------|-------|
| **TC ID** | TC-IMG-001 |
| **Title** | GET /api/products/{id}/images — productId không tồn tại → 404 |
| **Method mapped** | `ApiProductController.getProductImages()` line 139-140 |
| **Pre-conditions** | Product ID 99999 không tồn tại |
| **Test data** | `GET /api/products/99999/images` |
| **Expected result** | HTTP 404, `message="Không tìm thấy sản phẩm"` |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |

```java
@Test
void getProductImages_productNotFound_returns404() throws Exception {
    when(productService.getProductImages(99999))
        .thenThrow(new ResourceNotFoundException("Product not found with id: 99999"));
    
    mockMvc.perform(get("/api/products/99999/images"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Không tìm thấy sản phẩm"));
}
```

---

### TC-IMG-002 — GET /api/products/{id}/images — trả về ordered list

| Field | Value |
|-------|-------|
| **TC ID** | TC-IMG-002 |
| **Title** | GET /api/products/{id}/images — images phải được sort theo displayOrder ASC, id ASC |
| **Method mapped** | `ProductServiceImpl.getProductImages()` → `findByProductIdOrderByDisplayOrderAscIdAsc()` |
| **Pre-conditions** | Product ID=1 có 3 images: displayOrder=[2,0,1] |
| **Test data** | `GET /api/products/1/images` |
| **Expected result** | HTTP 200, `data[0].displayOrder=0`, `data[1].displayOrder=1`, `data[2].displayOrder=2` |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |

---

## MODULE 9: CONCURRENT & ORDERING SAFETY

---

### TC-CONC-001 — lockProducts sort by ID để tránh deadlock

| Field | Value |
|-------|-------|
| **TC ID** | TC-CONC-001 |
| **Title** | lockProducts — products phải được lock theo thứ tự ID tăng dần để tránh deadlock |
| **Method mapped** | `OrderServiceImpl.lockProducts()` line 183-187 — `.sorted()` |
| **Pre-conditions** | Cart có 3 sản phẩm: productId=[5, 2, 8] (không theo thứ tự) |
| **Test data** | cartItems với productIds = [5, 2, 8] |
| **Expected result** | `entityManager.find()` được gọi theo thứ tự: ID=2, ID=5, ID=8 (sorted ascending) |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |
| **Notes** | Anti-deadlock pattern — nếu fail: concurrent checkouts có thể deadlock DB |

```java
@Test
void lockProducts_locksInAscendingIdOrder_preventsDeadlock() {
    CartItem c5 = new CartItem(5, "P5", BigDecimal.ZERO, 1, null);
    CartItem c2 = new CartItem(2, "P2", BigDecimal.ZERO, 1, null);
    CartItem c8 = new CartItem(8, "P8", BigDecimal.ZERO, 1, null);
    when(cartService.getItems()).thenReturn(List.of(c5, c2, c8));

    Product p2 = buildProduct(2, new BigDecimal("100000"), true);
    Product p5 = buildProduct(5, new BigDecimal("200000"), true);
    Product p8 = buildProduct(8, new BigDecimal("300000"), true);

    when(entityManager.find(eq(Product.class), eq(2), eq(LockModeType.PESSIMISTIC_WRITE))).thenReturn(p2);
    when(entityManager.find(eq(Product.class), eq(5), eq(LockModeType.PESSIMISTIC_WRITE))).thenReturn(p5);
    when(entityManager.find(eq(Product.class), eq(8), eq(LockModeType.PESSIMISTIC_WRITE))).thenReturn(p8);

    when(orderRepository.save(any())).thenAnswer(i -> { Order o = i.getArgument(0); o.setId(1L); return o; });

    orderService.checkout(buildValidCheckoutRequest(), mockAccount());

    // Verify lock order: 2 → 5 → 8 (ascending)
    InOrder inOrder = inOrder(entityManager);
    inOrder.verify(entityManager).find(Product.class, 2, LockModeType.PESSIMISTIC_WRITE);
    inOrder.verify(entityManager).find(Product.class, 5, LockModeType.PESSIMISTIC_WRITE);
    inOrder.verify(entityManager).find(Product.class, 8, LockModeType.PESSIMISTIC_WRITE);
}
```

---

### TC-CONC-002 — postCheckout email fail không rollback order

| Field | Value |
|-------|-------|
| **TC ID** | TC-CONC-002 |
| **Title** | postCheckout — email gửi thất bại (SMTP error) không được ảnh hưởng tới order đã tạo |
| **Method mapped** | `OrderServiceImpl.postCheckout()` line 252-269 — try-catch nuốt exception |
| **Pre-conditions** | Order #200 đã được tạo thành công. EmailService throw RuntimeException. |
| **Test data** | `emailService.sendNewOrderNotification()` throw `RuntimeException("SMTP connection failed")` |
| **Expected result** | `postCheckout()` **không throw exception**. Order #200 vẫn tồn tại trong DB. |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |
| **Notes** | Resilience test — email failure là side effect, không được rollback business transaction |

```java
@Test
void postCheckout_emailFails_doesNotThrowException() {
    Order order = new Order(); order.setId(200L);
    order.setPhone("0912345678"); order.setAddress("Hà Nội");
    order.setTotalAmount(new BigDecimal("1000000"));

    Account account = new Account();
    account.setFullname("Test User");

    doThrow(new RuntimeException("SMTP connection failed"))
        .when(emailService).sendNewOrderNotification(any(), any(), any(), any(), any(), any());

    // Must NOT throw
    assertDoesNotThrow(() -> orderService.postCheckout(order, account));
}
```

---

### TC-CONC-003 — cart trùng productId: lockProducts chỉ lock 1 lần (distinct)

| Field | Value |
|-------|-------|
| **TC ID** | TC-CONC-003 |
| **Title** | lockProducts — cart có 2 items cùng productId=3, chỉ nên lock product đó 1 lần |
| **Method mapped** | `OrderServiceImpl.lockProducts()` line 185 — `.distinct()` |
| **Pre-conditions** | Cart: `[{productId:3, qty:1}, {productId:3, qty:2}]` |
| **Test data** | cartItems với productId=3 xuất hiện 2 lần |
| **Expected result** | `entityManager.find(Product.class, 3, PESSIMISTIC_WRITE)` chỉ được gọi **1 lần** |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |
| **Notes** | `.distinct()` trong stream — nếu thiếu: lock 2 lần cùng row → không cần thiết, tăng latency |

```java
@Test
void lockProducts_duplicateProductIds_locksOnlyOnce() {
    CartItem c1 = new CartItem(3, "P3", BigDecimal.ZERO, 1, null);
    CartItem c2 = new CartItem(3, "P3", BigDecimal.ZERO, 2, null); // same productId
    when(cartService.getItems()).thenReturn(List.of(c1, c2));

    Product p3 = buildProduct(3, new BigDecimal("500000"), true);
    when(entityManager.find(eq(Product.class), eq(3), eq(LockModeType.PESSIMISTIC_WRITE))).thenReturn(p3);
    when(orderRepository.save(any())).thenAnswer(i -> { Order o = i.getArgument(0); o.setId(1L); return o; });

    orderService.checkout(buildValidCheckoutRequest(), mockAccount());

    // Verify find called exactly once for product 3
    verify(entityManager, times(1))
        .find(Product.class, 3, LockModeType.PESSIMISTIC_WRITE);
}
```

---

## MODULE 10: ORDER STATUS ENUM

---

### TC-ENUM-001 — OrderStatus.fromValue — valid values

| Field | Value |
|-------|-------|
| **TC ID** | TC-ENUM-001 |
| **Title** | Order.OrderStatus.fromValue — các giá trị hợp lệ 0,1,2 phải map đúng |
| **Method mapped** | `Order.OrderStatus.fromValue()` line 53-58 |
| **Pre-conditions** | N/A (pure unit test) |
| **Test data** | `fromValue(0)`, `fromValue(1)`, `fromValue(2)` |
| **Expected result** | `NEW`, `CONFIRMED`, `CANCELLED` tương ứng |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |

```java
@Test
void orderStatus_fromValue_mapsCorrectly() {
    assertEquals(Order.OrderStatus.NEW,       Order.OrderStatus.fromValue(0));
    assertEquals(Order.OrderStatus.CONFIRMED, Order.OrderStatus.fromValue(1));
    assertEquals(Order.OrderStatus.CANCELLED, Order.OrderStatus.fromValue(2));
}

@Test
void orderStatus_fromValue_invalidValue_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> Order.OrderStatus.fromValue(99));
    assertThrows(IllegalArgumentException.class, () -> Order.OrderStatus.fromValue(-1));
    assertThrows(IllegalArgumentException.class, () -> Order.OrderStatus.fromValue(3));
}
```

---

## BẢNG TỔNG KẾT ĐẦY ĐỦ (Part 4 bổ sung)

| # | TC ID | Module | Loại Test | Bug Risk |
|---|-------|--------|-----------|----------|
| 50 | TC-VAL-001 | Validation | Parameterized | Phone format bypass |
| 51 | TC-VAL-002 | Validation | Boundary | Note overflow → DB truncate |
| 52 | TC-VAL-003 | Validation | Boundary | VoucherCode overflow |
| 53 | TC-VAL-004 | Validation | Security | XSS via fullname |
| 54 | TC-VAL-005 | Validation | Security | XSS via address |
| 55 | TC-PRD-014 | Product | **Potential Bug** | requireContact không được enforce |
| 56 | TC-PRD-015 | Product | Logic | toggleFeatured null safety |
| 57 | TC-IMG-001 | Image | Negative | Product not found → 404 |
| 58 | TC-IMG-002 | Image | Logic | Sort order displayOrder |
| 59 | TC-CONC-001 | Concurrency | Anti-deadlock | Lock order ascending ID |
| 60 | TC-CONC-002 | Resilience | Email fail | Order không rollback |
| 61 | TC-CONC-003 | Concurrency | Logic | Distinct lock productId |
| 62 | TC-ENUM-001 | Enum | Boundary | OrderStatus valid + invalid |

**Tổng cộng: 62 test cases** across 10 modules.

---

## ⚠️ POTENTIAL BUGS PHÁT HIỆN QUA QUÁ TRÌNH VIẾT TEST

| Bug | Location | Mô tả | Severity |
|-----|----------|-------|----------|
| **BUG-A** | `OrderServiceImpl.lockProducts()` | `requireContact=true` không bị block tại checkout — product vẫn được đặt online | 🟡 Medium |
| **BUG-B** | `CheckoutRequest` | Không có `shippingFee` field — hệ thống không thể tính phí ship (BUG-59 trong code) | 🔴 High |
| **BUG-C** | `GlobalExceptionHandler` | Nếu `AccessDeniedException` bị catch bởi catch-all trước `handleAccessDenied` → 500 thay vì 403 | 🔴 High |
| **BUG-D** | `Order.pointsUsed` | Luôn được set = 0 (line 129) — voucher/points logic chưa implement | 🟡 Medium |
| **BUG-E** | `CategoryRequest` | `normalize()` gọi `toUpperCase()` nhưng `@Pattern(^[A-Z0-9_]+$)` chạy sau `normalize()` trong Controller → pattern check uppercase sau normalize có thể pass sai | 🟢 Low |
