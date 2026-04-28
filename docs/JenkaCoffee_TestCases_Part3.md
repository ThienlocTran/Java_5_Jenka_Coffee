# Jenka Coffee — Test Case Document (Part 3: BUSINESS LOGIC + EXCEPTION HANDLER)
**Source mapped:** `OrderServiceImpl` (business logic), `GlobalExceptionHandler`, `CheckoutRequest`

---

## MODULE 4: BUSINESS LOGIC & EXCEPTION HANDLING

---

### TC-BIZ-001 — totalAmount: tính đúng với nhiều sản phẩm + nhiều qty

| Field | Value |
|-------|-------|
| **TC ID** | TC-BIZ-001 |
| **Title** | checkout — totalAmount = sum(DB_price × quantity) với 3 sản phẩm khác nhau |
| **Method mapped** | `OrderServiceImpl.checkout()` line 107-109 |
| **Pre-conditions** | Cart: p1(price=5,500,000 × qty=1), p2(price=320,000 × qty=2), p3(price=150,000 × qty=3) |
| **Test data** | p1=5,500,000; p2=320,000×2=640,000; p3=150,000×3=450,000 |
| **Expected result** | `order.getTotalAmount() = 5,500,000 + 640,000 + 450,000 = 6,590,000` (BigDecimal chính xác) |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |
| **Notes** | Dùng `BigDecimal.compareTo()` khi assert, không dùng `equals()` vì scale khác nhau |

```java
@Test
void checkout_totalAmount_calculatedCorrectlyFromDBPrices() {
    // Arrange: 3 products in cart
    CartItem c1 = new CartItem(1, "Máy pha", new BigDecimal("9999"), 1, null); // price from cart IGNORED
    CartItem c2 = new CartItem(2, "Arabica", new BigDecimal("9999"), 2, null);
    CartItem c3 = new CartItem(3, "Ly sứ", new BigDecimal("9999"), 3, null);
    when(cartService.getItems()).thenReturn(List.of(c1, c2, c3));

    // DB prices (what actually gets used)
    Product p1 = buildProduct(1, new BigDecimal("5500000"), true);
    Product p2 = buildProduct(2, new BigDecimal("320000"), true);
    Product p3 = buildProduct(3, new BigDecimal("150000"), true);

    when(entityManager.find(eq(Product.class), eq(1), any())).thenReturn(p1);
    when(entityManager.find(eq(Product.class), eq(2), any())).thenReturn(p2);
    when(entityManager.find(eq(Product.class), eq(3), any())).thenReturn(p3);

    // Capture saved order
    ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
    when(orderRepository.save(orderCaptor.capture())).thenAnswer(i -> {
        Order o = i.getArgument(0); o.setId(1L); return o;
    });

    orderService.checkout(buildValidCheckoutRequest(), mockAccount());

    BigDecimal expected = new BigDecimal("6590000");
    assertEquals(0, expected.compareTo(orderCaptor.getValue().getTotalAmount()),
        "totalAmount phải = 5,500,000 + 640,000 + 450,000 = 6,590,000");
}
```

---

### TC-BIZ-002 — totalAmount boundary: đúng 500,000,000 phải PASS

| Field | Value |
|-------|-------|
| **TC ID** | TC-BIZ-002 |
| **Title** | checkout — totalAmount = 500,000,000 (boundary) phải được chấp nhận, không bị reject |
| **Method mapped** | `OrderServiceImpl.checkout()` line 114 — `compareTo(MAX_ORDER_VALUE) > 0` (strict greater than) |
| **Pre-conditions** | Cart: 1 product, DB price=500,000,000 VND, qty=1 |
| **Test data** | product.price = 500,000,000 |
| **Expected result** | Không throw exception. Order được tạo thành công với totalAmount=500,000,000 |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |
| **Notes** | Boundary value analysis — `> 0` không phải `>= 0`. Giá trị **bằng** limit phải pass. |

---

### TC-BIZ-003 — Payment record được tạo với status=PENDING sau checkout

| Field | Value |
|-------|-------|
| **TC ID** | TC-BIZ-003 |
| **Title** | checkout — sau khi lưu order, Payment record phải được tạo với status="PENDING" và paymentMethod="COD" |
| **Method mapped** | `OrderServiceImpl.createPayment()` line 235-247 |
| **Pre-conditions** | Cart hợp lệ, paymentMethod="cod" |
| **Test data** | CheckoutRequest với `paymentMethod="cod"` |
| **Expected result** | `entityManager.persist()` được gọi với Payment: `status="PENDING"`, `paymentMethod="COD"` (uppercase), `amount=totalAmount` |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |
| **Notes** | `"cod"` lowercase từ request phải được `toUpperCase()` trước khi lưu |

```java
@Test
void checkout_createsPaymentRecordWithPendingStatus() {
    setupValidCart(new BigDecimal("1500000"));
    when(orderRepository.save(any())).thenAnswer(i -> { Order o = i.getArgument(0); o.setId(1L); return o; });

    ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
    doNothing().when(entityManager).persist(paymentCaptor.capture());

    orderService.checkout(buildCheckoutRequest("cod"), mockAccount());

    Payment payment = paymentCaptor.getValue();
    assertEquals("COD", payment.getPaymentMethod());         // must be uppercase
    assertEquals("PENDING", payment.getStatus());
    assertEquals(0, new BigDecimal("1500000").compareTo(payment.getAmount()));
}
```

---

### TC-BIZ-004 — updateStatus với status value không hợp lệ (status=99)

| Field | Value |
|-------|-------|
| **TC ID** | TC-BIZ-004 |
| **Title** | updateStatus — truyền status=99 (không có trong enum) phải throw BusinessRuleException |
| **Method mapped** | `OrderServiceImpl.updateStatus()` line 336-340 — `Order.OrderStatus.fromValue(99)` → `IllegalArgumentException` → wrap `BusinessRuleException` |
| **Pre-conditions** | Order tồn tại, status=0 (NEW) |
| **Test data** | `updateStatus(orderId, 99)` |
| **Expected result** | `BusinessRuleException("Trạng thái đơn hàng không hợp lệ: 99")` |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |

```java
@Test
void updateStatus_invalidStatusValue_throwsBusinessRuleException() {
    Order order = new Order(); order.setId(1L); order.setStatus(0);
    when(entityManager.find(Order.class, 1L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(order);

    BusinessRuleException ex = assertThrows(BusinessRuleException.class,
        () -> orderService.updateStatus(1L, 99));
    assertTrue(ex.getMessage().contains("99"));
}
```

---

### TC-BIZ-005 — prepareCheckoutRequest: user null (guest) → không set thông tin

| Field | Value |
|-------|-------|
| **TC ID** | TC-BIZ-005 |
| **Title** | prepareCheckoutRequest — account=null (guest) phải trả CheckoutRequest rỗng, không NPE |
| **Method mapped** | `OrderServiceImpl.prepareCheckoutRequest()` line 311-321 |
| **Pre-conditions** | N/A (unit test thuần) |
| **Test data** | `prepareCheckoutRequest(null)` |
| **Expected result** | Trả về `CheckoutRequest` mới (không null), `fullname=null`, `email=null`, `phone=null` — không throw NPE |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |

```java
@Test
void prepareCheckoutRequest_nullAccount_returnsEmptyRequest() {
    CheckoutRequest req = orderService.prepareCheckoutRequest(null);
    assertNotNull(req);
    assertNull(req.getFullname());
    assertNull(req.getEmail());
    assertNull(req.getPhone());
}
```

---

### TC-BIZ-006 — prepareCheckoutRequest: user đã login → pre-fill đủ thông tin

| Field | Value |
|-------|-------|
| **TC ID** | TC-BIZ-006 |
| **Title** | prepareCheckoutRequest — account hợp lệ phải pre-fill fullname, email, phone vào CheckoutRequest |
| **Method mapped** | `OrderServiceImpl.prepareCheckoutRequest()` line 314-318 |
| **Pre-conditions** | N/A |
| **Test data** | Account: `{fullname:"Nguyễn Văn A", email:"nva@gmail.com", phone:"0912345678"}` |
| **Expected result** | `request.getFullname()="Nguyễn Văn A"`, `request.getEmail()="nva@gmail.com"`, `request.getPhone()="0912345678"` |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |

---

## MODULE 5: GLOBAL EXCEPTION HANDLER

---

### TC-EXC-001 — AccessDeniedException → HTTP 403 (không phải 500)

| Field | Value |
|-------|-------|
| **TC ID** | TC-EXC-001 |
| **Title** | GlobalExceptionHandler — AccessDeniedException phải trả 403, không phải 500 |
| **Method mapped** | `GlobalExceptionHandler.handleAccessDenied()` line 340-347 |
| **Pre-conditions** | User ROLE_USER gọi endpoint yêu cầu ROLE_ADMIN |
| **Test data** | `GET /api/admin/products` với JWT của ROLE_USER |
| **Expected result** | HTTP **403**, `status="ERROR"`, `message="Bạn không có quyền truy cập tài nguyên này!"` |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |
| **Notes** | Critical — nếu fail (trả 500 thay vì 403): frontend xử lý sai, UX broken. BUG-64 documented trong code. |

```java
@Test
void accessDenied_returns403NotServerError() throws Exception {
    mockMvc.perform(get("/api/admin/products")
            .with(user("user_x").roles("USER")))  // ROLE_USER, not ROLE_ADMIN
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value("ERROR"))
        .andExpect(jsonPath("$.message").value("Bạn không có quyền truy cập tài nguyên này!"));
}
```

---

### TC-EXC-002 — MethodArgumentNotValidException → HTTP 400, first error message

| Field | Value |
|-------|-------|
| **TC ID** | TC-EXC-002 |
| **Title** | GlobalExceptionHandler — @Valid fail phải trả 400 với message của lỗi đầu tiên |
| **Method mapped** | `GlobalExceptionHandler.handleValidationErrors()` line 283-299 |
| **Pre-conditions** | N/A |
| **Test data** | `POST /api/orders/checkout` với body `{}` (thiếu tất cả required fields) |
| **Expected result** | HTTP 400, `status="ERROR"`, `message` là một trong các validation messages (không phải stack trace) |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |

---

### TC-EXC-003 — DataIntegrityViolationException email trùng → HTTP 409

| Field | Value |
|-------|-------|
| **TC ID** | TC-EXC-003 |
| **Title** | GlobalExceptionHandler — duplicate email → 409 Conflict với friendly message |
| **Method mapped** | `GlobalExceptionHandler.handleDataIntegrity()` line 266-281 |
| **Pre-conditions** | DB unique constraint trên email |
| **Test data** | Exception message chứa "email" |
| **Expected result** | HTTP **409**, `message="Email này đã được đăng ký. Vui lòng dùng email khác."` |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |

```java
@Test
void dataIntegrityViolation_duplicateEmail_returns409WithFriendlyMessage() throws Exception {
    when(accountService.register(any()))
        .thenThrow(new DataIntegrityViolationException("duplicate key: accounts_email_key"));

    mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"existing@gmail.com\", ...}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message")
            .value("Email này đã được đăng ký. Vui lòng dùng email khác."));
}
```

---

### TC-EXC-004 — InsufficientStockException → HTTP 400

| Field | Value |
|-------|-------|
| **TC ID** | TC-EXC-004 |
| **Title** | GlobalExceptionHandler — InsufficientStockException phải trả 400 |
| **Method mapped** | `GlobalExceptionHandler.handleInsufficientStock()` line 239-243 |
| **Pre-conditions** | N/A |
| **Test data** | `InsufficientStockException("Sản phẩm X hết hàng")` |
| **Expected result** | HTTP 400, `status="ERROR"`, `message="Sản phẩm X hết hàng"` |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |
| **Notes** | Tách riêng khỏi BusinessRuleException handler để có thể customize sau |

---

## JUNIT BOILERPLATE — Setup mẫu

```java
// OrderServiceImplTest.java
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock OrderRepository orderRepository;
    @Mock CartService cartService;
    @Mock EmailService emailService;
    @Mock EntityManager entityManager;
    @Mock PointHistoryRepository pointHistoryRepository;

    @InjectMocks OrderServiceImpl orderService;

    // Helper: build valid CheckoutRequest
    private CheckoutRequest buildValidCheckoutRequest() {
        CheckoutRequest req = new CheckoutRequest();
        req.setFullname("Nguyễn Văn A");
        req.setEmail("nva@gmail.com");
        req.setPhone("0912345678");
        req.setAddress("123 Nguyễn Trãi");
        req.setWard("Phường 1");
        req.setDistrict("Quận 1");
        req.setProvince("TP.HCM");
        req.setPaymentMethod("cod");
        req.setAgreeTerms(true);
        return req;
    }

    // Helper: build Product
    private Product buildProduct(int id, BigDecimal price, boolean available) {
        Product p = new Product();
        p.setId(id);
        p.setName("Product " + id);
        p.setPrice(price);
        p.setAvailable(available);
        return p;
    }

    // Helper: mock locked account
    private Account mockAccount() {
        Account acc = new Account();
        acc.setUsername("nva@gmail.com");
        acc.setFullname("Nguyễn Văn A");
        acc.setPoints(0);
        when(entityManager.find(eq(Account.class), any(), eq(LockModeType.PESSIMISTIC_WRITE)))
            .thenReturn(acc);
        return acc;
    }
}

// ApiOrderControllerTest.java
@WebMvcTest(ApiOrderController.class)
@Import(SecurityConfig.class)
class ApiOrderControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean CartService cartService;
    @MockBean OrderService orderService;
    @MockBean AccountService accountService;

    // Helper
    private Category buildCategory(String id, String name, String icon) {
        Category c = new Category(); c.setId(id); c.setName(name); c.setIcon(icon);
        return c;
    }
}
```

---

## TỔNG KẾT TEST CASES

| # | TC ID | Module | Loại Test | Business Rule |
|---|-------|--------|-----------|---------------|
| 1 | TC-ORD-001 | Order | Happy path | totalAmount từ DB |
| 2 | TC-ORD-002 | Order | Negative | Cart trống |
| 3 | TC-ORD-003 | Order | Business rule | Product unavailable |
| 4 | TC-ORD-004 | Order | Edge case | Max order value 500M |
| 5 | TC-ORD-005 | Order | Negative | Payment method whitelist |
| 6 | TC-ORD-006 | Order | Negative | agreeTerms validation |
| 7 | TC-ORD-007 | Order | Security | XSS in note |
| 8 | TC-ORD-008 | Order | Security | IDOR protection |
| 9 | TC-ORD-009 | Order | Edge case | Guest order block |
| 10 | TC-ORD-010 | Order | Negative | Order not found |
| 11 | TC-ORD-011 | Order | Security | Deep pagination DoS |
| 12 | TC-ORD-012 | Order | Edge case | Size cap |
| 13 | TC-ORD-013 | Order | Business rule | FSM invalid transition |
| 14 | TC-ORD-014 | Order | Business rule | Cancelled is terminal |
| 15 | TC-ORD-015 | Order | Business rule | Points refund on cancel |
| 16 | TC-ORD-016 | Order | Security | Price from DB not cart |
| 17 | TC-ORD-017 | Order | Logic | Address concat format |
| 18 | TC-ORD-018 | Order | Negative | Checkout-info cart empty |
| 19 | TC-PRD-001 | Product | Security | Keyword length DoS |
| 20 | TC-PRD-002 | Product | Security | Page limit DoS |
| 21 | TC-PRD-003 | Product | Edge case | Size cap 50 |
| 22 | TC-PRD-004 | Product | Edge case | size=0 normalize |
| 23 | TC-PRD-005 | Product | Negative | Product not found |
| 24 | TC-PRD-006 | Product | Negative | Slug not found |
| 25 | TC-PRD-007 | Product | Happy path | Multi-filter |
| 26 | TC-PRD-008 | Product | Negative | Negative price |
| 27 | TC-PRD-009 | Product | Negative | Invalid categoryId |
| 28 | TC-PRD-010 | Product | Business rule | Delete with orders |
| 29 | TC-PRD-011 | Product | Logic | toggleAvailable |
| 30 | TC-PRD-012 | Product | Logic | Slug collision +counter |
| 31 | TC-PRD-013 | Product | Logic | Price rounding HALF_UP |
| 32 | TC-CAT-001 | Category | Happy path | Get all |
| 33 | TC-CAT-002 | Category | Business rule | Delete with products |
| 34 | TC-CAT-003 | Category | Negative | Delete not found |
| 35 | TC-CAT-004 | Category | Negative | Duplicate ID |
| 36 | TC-CAT-005 | Category | Logic | Normalize uppercase |
| 37 | TC-CAT-006 | Category | Logic | Auto icon assign |
| 38 | TC-CAT-007 | Category | Edge case | Unknown ID → icon=null |
| 39 | TC-CAT-008 | Category | Validation | Pattern check |
| 40 | TC-BIZ-001 | Business | Logic | totalAmount multi-item |
| 41 | TC-BIZ-002 | Business | Boundary | 500M boundary pass |
| 42 | TC-BIZ-003 | Business | Logic | Payment PENDING |
| 43 | TC-BIZ-004 | Business | Negative | Invalid status value |
| 44 | TC-BIZ-005 | Business | Edge case | Guest prepareCheckout |
| 45 | TC-BIZ-006 | Business | Happy path | User prepareCheckout |
| 46 | TC-EXC-001 | Exception | Security | 403 not 500 |
| 47 | TC-EXC-002 | Exception | Logic | Validation → 400 |
| 48 | TC-EXC-003 | Exception | Logic | Duplicate email → 409 |
| 49 | TC-EXC-004 | Exception | Logic | InsufficientStock → 400 |

**Tổng: 49 test cases** — đủ để phát hiện các bug thực tế trong production.
