# Jenka Coffee — Test Case Document (Part 1: ORDER MODULE)
**Project:** Jenka Coffee — Spring Boot Backend  
**Tester:** QA Engineer  
**Date:** 2026-04-25  
**Run Type:** Automatic (JUnit 5 + MockMvc + Mockito)  
**Source mapped:** `ApiOrderController`, `OrderServiceImpl`

---

## MODULE 1: ORDER CONTROLLER + SERVICE

---

### TC-ORD-001 — Checkout thành công (Happy Path)

| Field | Value |
|-------|-------|
| **TC ID** | TC-ORD-001 |
| **Title** | POST /api/orders/checkout — đặt hàng thành công với đủ thông tin hợp lệ |
| **Method mapped** | `ApiOrderController.processCheckout()` → `OrderServiceImpl.checkout()` |
| **Pre-conditions** | Session cart có 2 sản phẩm: productId=1 (Máy pha DeLonghi, price=5,500,000 VND, qty=1), productId=2 (Hạt cà phê Arabica, price=320,000 VND, qty=2). Product available=true. User đã login. |
| **Test step** | `POST /api/orders/checkout` với body JSON hợp lệ |
| **Test data** | `{"fullname":"Nguyen Van A","email":"nva@gmail.com","phone":"0912345678","address":"123 Nguyễn Trãi","ward":"Phường 1","district":"Quận 1","province":"TP.HCM","paymentMethod":"cod","agreeTerms":true}` |
| **Expected result** | HTTP 200, `status="SUCCESS"`, `message` chứa `"Đặt hàng thành công! Mã đơn hàng: #"`, `data.orderId` là Long > 0 |
| **Actual result** | _(để trống khi chạy)_ |
| **Status** | _(Passed/Failed)_ |
| **Notes** | `totalAmount` phải = 5,500,000 + 640,000 = **6,140,000** VND. Giá lấy từ DB, không từ cart. |

```java
@Test
void checkout_happyPath_returns200WithOrderId() throws Exception {
    // Arrange
    CartItem item1 = new CartItem(1, "Máy pha DeLonghi", new BigDecimal("5500000"), 1, null);
    CartItem item2 = new CartItem(2, "Hạt Arabica", new BigDecimal("320000"), 2, null);
    when(cartService.getItems()).thenReturn(List.of(item1, item2));
    
    Product p1 = buildProduct(1, new BigDecimal("5500000"), true);
    Product p2 = buildProduct(2, new BigDecimal("320000"), true);
    when(entityManager.find(eq(Product.class), eq(1), eq(LockModeType.PESSIMISTIC_WRITE))).thenReturn(p1);
    when(entityManager.find(eq(Product.class), eq(2), eq(LockModeType.PESSIMISTIC_WRITE))).thenReturn(p2);
    
    Order savedOrder = new Order(); savedOrder.setId(101L);
    when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

    CheckoutRequest req = buildValidCheckoutRequest();
    mockMvc.perform(post("/api/orders/checkout")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req))
            .with(user("nva@gmail.com")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUCCESS"))
        .andExpect(jsonPath("$.data.orderId").value(101L));
}
```

---

### TC-ORD-002 — Checkout với cart trống

| Field | Value |
|-------|-------|
| **TC ID** | TC-ORD-002 |
| **Title** | POST /api/orders/checkout — cart trống phải throw IllegalStateException → 400 |
| **Method mapped** | `OrderServiceImpl.checkout()` line 92-94 |
| **Pre-conditions** | Session cart rỗng (cartService.getItems() = empty) |
| **Test step** | `POST /api/orders/checkout` với body hợp lệ |
| **Test data** | Body hợp lệ như TC-ORD-001 |
| **Expected result** | HTTP 400, `status="ERROR"`, `message="Giỏ hàng trống, không thể đặt hàng!"` |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |
| **Notes** | `IllegalStateException` được catch bởi `GlobalExceptionHandler.handleBusinessExceptions()` → 400 |

```java
@Test
void checkout_emptyCart_returns400() throws Exception {
    when(cartService.getItems()).thenReturn(Collections.emptyList());
    mockMvc.perform(post("/api/orders/checkout")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(buildValidCheckoutRequest()))
            .with(user("nva@gmail.com")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("ERROR"))
        .andExpect(jsonPath("$.message").value("Giỏ hàng trống, không thể đặt hàng!"));
}
```

---

### TC-ORD-003 — Checkout khi product bị unavailable

| Field | Value |
|-------|-------|
| **TC ID** | TC-ORD-003 |
| **Title** | POST /api/orders/checkout — sản phẩm available=false phải throw BusinessRuleException → 400 |
| **Method mapped** | `OrderServiceImpl.lockProducts()` line 199-202 |
| **Pre-conditions** | Cart có productId=5 (available=false) |
| **Test data** | Product: `{id:5, name:"Máy xay cũ", available:false}` |
| **Expected result** | HTTP 400, `message` chứa `"Sản phẩm 'Máy xay cũ' hiện không còn kinh doanh!"` |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |
| **Notes** | Business rule cốt lõi — sản phẩm bị ẩn vẫn có thể có trong cart cũ của user |

---

### TC-ORD-004 — Checkout vượt giới hạn 500 triệu VND

| Field | Value |
|-------|-------|
| **TC ID** | TC-ORD-004 |
| **Title** | POST /api/orders/checkout — totalAmount > 500,000,000 VND phải bị reject |
| **Method mapped** | `OrderServiceImpl.checkout()` line 113-119 |
| **Pre-conditions** | Cart có 1 sản phẩm: price=600,000,000 VND, qty=1 |
| **Test data** | Product price = 600,000,000 VND |
| **Expected result** | HTTP 400, `message` chứa `"Giá trị đơn hàng vượt quá giới hạn cho phép (500 triệu VNĐ)"` |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |
| **Notes** | Edge case: giá trị đúng bằng 500,000,000 phải **pass** (chỉ reject khi >). Test thêm boundary value 500,000,001. |

---

### TC-ORD-005 — Checkout với paymentMethod không hợp lệ

| Field | Value |
|-------|-------|
| **TC ID** | TC-ORD-005 |
| **Title** | POST /api/orders/checkout — paymentMethod="bitcoin" phải fail Bean Validation → 400 |
| **Method mapped** | `CheckoutRequest` field `paymentMethod` — `@Pattern(regexp = "^(cod\|bank\|momo)$")` |
| **Pre-conditions** | Cart có sản phẩm hợp lệ |
| **Test data** | `{"paymentMethod":"bitcoin", ...}` |
| **Expected result** | HTTP 400, `status="ERROR"`, message chứa `"Phương thức thanh toán không hợp lệ"` |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |
| **Notes** | Whitelist validation — ngăn attacker inject giá trị tùy ý vào trường payment |

---

### TC-ORD-006 — Checkout với agreeTerms=false

| Field | Value |
|-------|-------|
| **TC ID** | TC-ORD-006 |
| **Title** | POST /api/orders/checkout — agreeTerms=false phải fail @AssertTrue → 400 |
| **Method mapped** | `CheckoutRequest.agreeTerms` — `@AssertTrue` |
| **Pre-conditions** | Cart hợp lệ |
| **Test data** | Body với `"agreeTerms": false` |
| **Expected result** | HTTP 400, message = `"Bạn phải đồng ý với điều khoản và điều kiện để đặt hàng"` |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |
| **Notes** | Server-side validation — không thể bypass bằng curl/Postman |

---

### TC-ORD-007 — Checkout với XSS trong note

| Field | Value |
|-------|-------|
| **TC ID** | TC-ORD-007 |
| **Title** | POST /api/orders/checkout — note chứa XSS phải được escape, không được execute |
| **Method mapped** | `OrderServiceImpl.buildOrder()` line 295-296 — `HtmlUtils.htmlEscape()` |
| **Pre-conditions** | Cart hợp lệ |
| **Test data** | `"note": "<script>alert('xss')</script>Giao giờ hành chính"` |
| **Expected result** | HTTP 200, order được tạo. Khi query lại order, `note` = `"&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt;Giao giờ hành chính"` |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |
| **Notes** | Kiểm tra log có warning SECURITY về XSS attempt |

---

### TC-ORD-008 — GET /api/orders/{orderId} — IDOR protection (khác user)

| Field | Value |
|-------|-------|
| **TC ID** | TC-ORD-008 |
| **Title** | GET /api/orders/{orderId} — user B không được xem order của user A |
| **Method mapped** | `ApiOrderController.getOrderDetail()` line 119-122 |
| **Pre-conditions** | Order #55 thuộc về user "user_a". Request đến từ authenticated user "user_b" |
| **Test data** | `GET /api/orders/55` với JWT của "user_b" |
| **Expected result** | HTTP 403, `status="ERROR"`, `message="Bạn không có quyền xem đơn hàng này"` |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |
| **Notes** | Critical security test — IDOR vulnerability nếu fail |

```java
@Test
void getOrderDetail_belongsToOtherUser_returns403() throws Exception {
    Order order = new Order();
    order.setId(55L);
    Account owner = new Account(); owner.setUsername("user_a");
    order.setAccount(owner);
    
    when(orderService.findById(55L)).thenReturn(order);
    
    mockMvc.perform(get("/api/orders/55").with(user("user_b")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("Bạn không có quyền xem đơn hàng này"));
}
```

---

### TC-ORD-009 — GET /api/orders/{orderId} — guest order bị block

| Field | Value |
|-------|-------|
| **TC ID** | TC-ORD-009 |
| **Title** | GET /api/orders/{orderId} — order của khách vãng lai (account=null) phải bị block → 403 |
| **Method mapped** | `ApiOrderController.getOrderDetail()` line 108-116 |
| **Pre-conditions** | Order #66 có `account = null` (guest order) |
| **Test data** | `GET /api/orders/66` với JWT của bất kỳ user nào |
| **Expected result** | HTTP 403, message chứa `"Đơn hàng này được đặt bởi khách vãng lai"` |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |
| **Notes** | Edge case — phân biệt guest order vs user order |

---

### TC-ORD-010 — GET /api/orders/{orderId} — không tìm thấy

| Field | Value |
|-------|-------|
| **TC ID** | TC-ORD-010 |
| **Title** | GET /api/orders/{orderId} — orderId không tồn tại → 404 |
| **Method mapped** | `ApiOrderController.getOrderDetail()` line 100-103 |
| **Pre-conditions** | Order #9999 không tồn tại trong DB |
| **Test data** | `GET /api/orders/9999` |
| **Expected result** | HTTP 404, `status="ERROR"`, `message="Không tìm thấy đơn hàng"` |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |

---

### TC-ORD-011 — GET /api/orders?page=1001 — Deep Pagination DoS

| Field | Value |
|-------|-------|
| **TC ID** | TC-ORD-011 |
| **Title** | GET /api/orders?page=1001 — page vượt 1000 phải bị reject → 400 |
| **Method mapped** | `ApiOrderController.getOrderHistory()` line 74-77 |
| **Pre-conditions** | User đã login |
| **Test data** | `GET /api/orders?page=1001&size=5` |
| **Expected result** | HTTP 400, `message="Số trang không được vượt quá 1000"` |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |
| **Notes** | Security test — ngăn DB scan hàng triệu rows qua OFFSET |

---

### TC-ORD-012 — GET /api/orders?size=9999 — Size bị cap ở 20

| Field | Value |
|-------|-------|
| **TC ID** | TC-ORD-012 |
| **Title** | GET /api/orders?size=9999 — size phải bị giới hạn tối đa 20 |
| **Method mapped** | `ApiOrderController.getOrderHistory()` line 79 — `Math.min(size, 20)` |
| **Pre-conditions** | User đã login, có nhiều orders |
| **Test data** | `GET /api/orders?size=9999` |
| **Expected result** | HTTP 200, `data.items.length <= 20` (không thể vượt 20) |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |

---

### TC-ORD-013 — updateStatus — FSM: CONFIRMED → NEW không hợp lệ

| Field | Value |
|-------|-------|
| **TC ID** | TC-ORD-013 |
| **Title** | updateStatus — không thể chuyển CONFIRMED(1) → NEW(0) — invalid FSM transition |
| **Method mapped** | `OrderServiceImpl.updateStatus()` line 342-351 |
| **Pre-conditions** | Order #77 có status=1 (CONFIRMED) |
| **Test data** | `updateStatus(77L, 0)` |
| **Expected result** | `BusinessRuleException` với message `"Không thể chuyển trạng thái từ CONFIRMED sang NEW"` |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |

```java
@Test
void updateStatus_confirmedToNew_throwsBusinessRuleException() {
    Order order = new Order(); order.setId(77L); order.setStatus(1); // CONFIRMED
    when(entityManager.find(Order.class, 77L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(order);
    
    assertThrows(BusinessRuleException.class, () -> orderService.updateStatus(77L, 0));
}
```

---

### TC-ORD-014 — updateStatus — CANCELLED → bất kỳ đều bị block

| Field | Value |
|-------|-------|
| **TC ID** | TC-ORD-014 |
| **Title** | updateStatus — đơn hàng đã CANCELLED(2) không thể chuyển về bất kỳ trạng thái nào |
| **Method mapped** | `OrderServiceImpl.updateStatus()` — FSM `case CANCELLED -> false` |
| **Pre-conditions** | Order #88 có status=2 (CANCELLED) |
| **Test data** | `updateStatus(88L, 0)` và `updateStatus(88L, 1)` |
| **Expected result** | Cả 2 đều throw `BusinessRuleException` |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |

---

### TC-ORD-015 — updateStatus CANCEL — hoàn điểm cho user

| Field | Value |
|-------|-------|
| **TC ID** | TC-ORD-015 |
| **Title** | updateStatus NEW→CANCELLED — nếu order có pointsUsed > 0 phải hoàn điểm cho account |
| **Method mapped** | `OrderServiceImpl.updateStatus()` line 354-376 |
| **Pre-conditions** | Order #99: status=NEW, account="user_x", pointsUsed=500. Account user_x hiện có points=200. |
| **Test data** | `updateStatus(99L, 2)` |
| **Expected result** | Account user_x có points = 200 + 500 = **700**. PointHistory record được tạo với amount=500, reason chứa "Hoàn điểm do hủy đơn hàng #99" |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |
| **Notes** | Business rule quan trọng — nếu fail: user mất điểm oan |

```java
@Test
void updateStatus_cancelWithPoints_refundsPointsToAccount() {
    Account account = new Account(); account.setUsername("user_x"); account.setPoints(200);
    Order order = new Order(); order.setId(99L); order.setStatus(0); // NEW
    order.setAccount(account); order.setPointsUsed(500);
    
    when(entityManager.find(Order.class, 99L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(order);
    when(entityManager.find(Account.class, "user_x", LockModeType.PESSIMISTIC_WRITE)).thenReturn(account);
    
    orderService.updateStatus(99L, 2); // CANCELLED
    
    assertEquals(700, account.getPoints());
    verify(pointHistoryRepository).save(argThat(h -> h.getAmount() == 500 && h.getOrderId() == 99L));
}
```

---

### TC-ORD-016 — totalAmount tính từ DB price, không từ cart

| Field | Value |
|-------|-------|
| **TC ID** | TC-ORD-016 |
| **Title** | checkout — giá trong totalAmount phải lấy từ locked product trong DB, không từ cart item |
| **Method mapped** | `OrderServiceImpl.buildOrderDetails()` line 225 — `detail.setPrice(product.getPrice())` |
| **Pre-conditions** | Cart item gửi price=1000 VND cho productId=3. DB price của product 3 = 5,500,000 VND. |
| **Test data** | Cart: `{productId:3, quantity:1, price:1000}`. DB Product 3: price=5,500,000 |
| **Expected result** | Order `totalAmount = 5,500,000` (từ DB), **không phải** 1,000 (từ cart) |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |
| **Notes** | Critical security — ngăn attacker tự sửa giá trong cart |

---

### TC-ORD-017 — buildOrder — address được concat đúng format

| Field | Value |
|-------|-------|
| **TC ID** | TC-ORD-017 |
| **Title** | buildOrder — address phải là "address, ward, district, province" |
| **Method mapped** | `OrderServiceImpl.buildOrder()` line 276-282 |
| **Pre-conditions** | N/A (unit test) |
| **Test data** | `address="123 Nguyễn Trãi"`, `ward="Phường 1"`, `district="Quận 1"`, `province="TP.HCM"` |
| **Expected result** | `order.getAddress() = "123 Nguyễn Trãi, Phường 1, Quận 1, TP.HCM"` |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |

---

### TC-ORD-018 — GET /api/orders/checkout-info khi cart trống

| Field | Value |
|-------|-------|
| **TC ID** | TC-ORD-018 |
| **Title** | GET /api/orders/checkout-info — cart trống phải trả 400 |
| **Method mapped** | `ApiOrderController.getCheckoutInfo()` line 40-42 |
| **Pre-conditions** | Cart rỗng |
| **Test data** | `GET /api/orders/checkout-info` với user đã login |
| **Expected result** | HTTP 400, `status="ERROR"`, `message="Giỏ hàng trống"` |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |
