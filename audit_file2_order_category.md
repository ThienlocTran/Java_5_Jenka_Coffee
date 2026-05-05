# 🔍 QA AUDIT REPORT — FILE 2
## TC_BATCH_02_ORDER_CATEGORY_v2.csv
### vs. Java Tests: `ApiAdminOrderControllerTest`, `ApiAdminCategoryControllerTest`, `OrderServiceImplTest`, `ApiOrderControllerTest`

---

## 1. OVERALL ASSESSMENT

| Dimension | Score | Verdict |
|---|---|---|
| Test Coverage | 8/10 | 17 TC Order + 22 TC Category + 3 TC Service Order + 3 TC Service Category + 10 TC Public Order |
| Expected Result Quality | 6/10 | Nhiều TC ghi "Return 200 OK" không kèm cụ thể field nào cần verify |
| Test Data Realism | 7/10 | Order Controller dùng real DB data; Public Order test dùng tên sản phẩm thực tế |
| Assertion Depth | 7/10 | Order/Category Controller dùng real DB verify — tốt; nhưng nhiều Service tests dừng ở exception type |
| Security Coverage | 8/10 | IDOR protection test rất tốt (TC-ORD-008/009); Category SQL injection test có nhưng weak |
| Negative/Edge Coverage | 7/10 | Có boundary, invalid range, not-found; thiếu concurrent update test cho status |
| Bug Detection Capability | 7/10 | Real-flow DB verify giúp bắt bug thực; nhưng response format inconsistency gây fail sai |

**Overall Quality Score: 7.1 / 10**

**Key Strengths:**
- Order & Category Controller dùng `@SpringBootTest + @Transactional` với real repo → phát hiện bug thực tế tốt hơn mock-only
- `ApiOrderControllerTest` có IDOR test (`TC-ORD-008`) và DoS pagination protection (`TC-ORD-011`) — rất valuable
- `OrderServiceImplTest` có test points refund khi cancel (`TC-ORD-SER-007`) — business logic critical
- DB state verification sau status update (TC-ORD-CTRL-008/009/014) là best practice đúng đắn

**Critical Weaknesses:**
- **Response key inconsistency:** Order/Category Controller check `$.success` nhưng Public Order check `$.status` — ít nhất 1 trong 2 sẽ fail
- **TC-ORD-CTRL-012 và TC-ORD-CTRL-016 documenting bugs nhưng test accepts wrong behavior** (500 thay vì 404)
- **TC-CAT-SER-001 mâu thuẫn hoàn toàn với CSV spec** — test accepts blank name, CSV yêu cầu reject
- **TC-CAT-CTRL-012 ghi `status().isOk()` cho special chars** — security gap được accept thay vì documented
- **Thiếu hoàn toàn:** State machine validation test (confirmed → cancelled không được phép, nhưng cancelled → confirmed không có test)

---

## 2. CRITICAL ISSUES

### ❌ ISSUE 1: Response Format Key Inconsistency — `$.success` vs `$.status`

**File A — `ApiAdminOrderControllerTest.java` + `ApiAdminCategoryControllerTest.java`:**
```java
.andExpect(jsonPath("$.success").value(true))   // Dùng $.success
.andExpect(jsonPath("$.success").value(false))
```

**File B — `ApiOrderControllerTest.java`:**
```java
.andExpect(jsonPath("$.status").value("SUCCESS"))  // Dùng $.status = string
.andExpect(jsonPath("$.status").value("ERROR"))
```

**Vấn đề cực kỳ nghiêm trọng:** Cùng một backend nhưng 2 response format khác nhau. Hoặc:
- A sai (API không có field `$.success`)
- B sai (API có `$.success` boolean, không phải `$.status` string)
- Hoặc có 2 response wrapper khác nhau cho admin vs public API

**Hậu quả:** Một trong hai bộ test **luôn fail 100%** ngay cả khi code đúng. Đây là lỗi tệ nhất — làm cho CI pipeline vô nghĩa.

---

### ❌ ISSUE 2: TC-ORD-CTRL-012 — Accept Wrong Behavior (500 thay vì 404)

**CSV spec:** "Return 500 Internal Server Error ⚠️ gap: nên là 404 - cần fix"

**Java test (`ApiAdminOrderControllerTest.java` line 246):**
```java
mockMvc.perform(put("/api/admin/orders/99999/status/1"))
    .andExpect(status().isInternalServerError())  // ❌ Chấp nhận bug!
    .andExpect(jsonPath("$.success").value(false));
```

**Vấn đề:** Test này **pass** dù behavior sai. Nếu developer fix bug (trả 404 đúng spec), test này sẽ **fail** — tức là test đang bảo vệ bug, không bảo vệ spec. Cần đổi expected thành `isNotFound()` và ghi chú là nếu test fail → bug chưa fix.

**Tương tự:** `TC-ORD-CTRL-016` (line 303-304):
```java
.andExpect(status().isInternalServerError())  // CSV ghi "Return 400"
```
CSV spec ghi là 400, Java test accept 500 — **sai cả CSV lẫn logic**.

---

### ❌ ISSUE 3: TC-CAT-SER-001 — Mâu thuẫn Hoàn Toàn với CSV Spec

**CSV spec:**
> "Expected: Throw ConstraintViolationException or BusinessRuleException"

**Java test (`CategoryServiceImplTest.java` line 66-76):**
```java
assertDoesNotThrow(() -> {
    categoryService.createCategory(testRequest); // name = "   " (blank)
});
// Alternative: If service validates, expect exception
// assertThrows(ConstraintViolationException.class, ...)
```

**Vấn đề:** Test document "current behavior" (không throw) nhưng CSV muốn "should throw". Đây là GAP documentation mà không có assertThrows. Test pass ngay cả khi bug tồn tại.

**Và khi mock được gọi với blank name:**
- `categoryService.createCategory(testRequest)` gọi `categoryRepository.save()`
- Nhưng không có `when(categoryRepository.save(...))` setup → Mockito sẽ return `null` hoặc throw `NullPointerException`
- **Test có thể throw NPE ngay cả khi test logic đúng** — unreliable test.

---

### ❌ ISSUE 4: TC-CAT-CTRL-012 — Special Characters Accepted as Expected Behavior

**CSV spec:**
> "Nếu có validate pattern → 400; verify không inject SQL"

**Java test (`ApiAdminCategoryControllerTest.java` line 237):**
```java
request.setId("CA T#1");  // space + # character
mockMvc.perform(...)
    .andExpect(status().isOk()); // ❌ Currently no pattern validation
```

**Vấn đề:** Test expect 200 OK cho ID chứa ký tự đặc biệt — test này **bảo vệ security gap**. ID chứa khoảng trắng hoặc `#` trong URL path có thể gây:
- URL encoding issues khi gọi `/api/admin/categories/CA%20T%231`
- Potential SQL injection nếu dùng string concat
- Data inconsistency khi dùng ID trong URL

**Test nên expect 400** để bảo vệ security, không phải document gap và accept 200.

---

### ❌ ISSUE 5: Order Service TC-ORD-SER-002 — Thiếu Message Verification Critical

**CSV spec:** "exception type + message"

**Java test (`OrderServiceImplTest.java` line 106-108):**
```java
assertTrue(exception.getMessage().contains("Không thể chuyển trạng thái"));
assertTrue(exception.getMessage().contains("CONFIRMED"));
assertTrue(exception.getMessage().contains("CANCELLED"));
```

**Nhận xét tốt:** Test này verify message đúng cách. **Nhưng thiếu:** verify transition reverse (CANCELLED → CONFIRMED cũng không được phép).

**Và TC-ORD-SER-007 (points refund)** — không verify:
- `pointHistoryRepository.save()` được gọi với đúng `PointHistory` object (amount = +50, reason = "CANCEL_REFUND")
- Không verify `testAccount` được save lại với points mới
- Chỉ check `assertEquals(150, testAccount.getPoints())` trên in-memory object, KHÔNG verify DB save

---

## 3. IMPROVEMENT SUGGESTIONS

### 🔧 Rewrite 1: TC-ORD-CTRL-012 — Từ Accepting Bug → Documenting Expected Fix

**Original (accepts bug):**
```java
.andExpect(status().isInternalServerError())
.andExpect(jsonPath("$.success").value(false));
```

**Rewritten (expects correct behavior + documents gap):**
```java
@Test
@DisplayName("TC-ORD-CTRL-012: [FIX NEEDED] Update status order not found - should return 404, currently 500")
void test_updateOrderStatus_orderNotFound_shouldReturn404() throws Exception {
    // CSV spec: cần là 404 Not Found
    // Current behavior: 500 Internal Server Error (unhandled exception)
    // This test will FAIL until the gap is fixed in OrderService.updateStatus()
    // Fix: catch RuntimeException from EntityManager.find() → throw ResourceNotFoundException → 404
    
    mockMvc.perform(put("/api/admin/orders/99999/status/1"))
        .andExpect(status().isNotFound())          // ✅ Target: 404
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.message").value(containsString("Không tìm thấy đơn hàng")))
        .andExpect(jsonPath("$.message").value(containsString("99999")));
    
    // If this fails with 500 → gap still exists, fix OrderService
}
```

---

### 🔧 Rewrite 2: TC-CAT-SER-001 — Từ Ambiguous → Clear Gap Test

**Original (ambiguous, potentially NPE):**
```java
assertDoesNotThrow(() -> categoryService.createCategory(testRequest));
// Alternative: If service validates, expect exception...
```

**Rewritten (clear behavior documentation):**
```java
@Test
@DisplayName("TC-CAT-SER-001: [GAP] createCategory với name blank — @Valid không active ở service layer, validation bỏ qua")
void test_createCategory_blankName_documents_validationGap() {
    // Arrange
    testRequest.setName("   "); // blank
    when(categoryRepository.existsById("NEW_CAT")).thenReturn(false);
    when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));
    
    // GAP: @Valid/@NotBlank chỉ kích hoạt ở Controller layer khi có @Valid annotation
    // Service layer KHÔNG tự validate — blank name được save vào DB
    // EXPECTED production behavior: reject blank name → BusinessRuleException
    // CURRENT behavior: accepted silently
    
    // Document the gap by verifying blank name is NOT rejected
    assertDoesNotThrow(() -> categoryService.createCategory(testRequest),
        "GAP CONFIRMED: blank name accepted at service layer — fix by adding explicit validation");
    
    // Verify DB save WAS called (gap confirmed: blank name went through)
    verify(categoryRepository).save(argThat(cat -> cat.getName().isBlank()));
    
    // TODO: When gap is fixed, change to:
    // assertThrows(BusinessRuleException.class, () -> categoryService.createCategory(testRequest));
    // verify(categoryRepository, never()).save(any());
}
```

---

### 🔧 Rewrite 3: TC-ORD-SER-007 — Points Refund với Full Verification

**Original (chỉ check in-memory):**
```java
assertEquals(150, testAccount.getPoints()); // 100 + 50
verify(pointHistoryRepository, times(1)).save(any());
```

**Rewritten (full business logic verification):**
```java
@Test
@DisplayName("TC-ORD-SER-007: Cancel order với pointsUsed=50 — refund đúng amount + history record")
void test_updateStatus_cancelOrder_refundsPointsWithHistory() {
    // Arrange
    testOrder.setStatus(0); // NEW
    testOrder.setPointsUsed(50);
    testAccount.setPoints(100); // Before: 100 points
    
    when(entityManager.find(Order.class, testOrder.getId(), LockModeType.PESSIMISTIC_WRITE))
        .thenReturn(testOrder);
    when(entityManager.find(Account.class, testAccount.getUsername(), LockModeType.PESSIMISTIC_WRITE))
        .thenReturn(testAccount);
    when(orderRepository.save(any())).thenReturn(testOrder);
    
    // Act
    orderService.updateStatus(testOrder.getId(), 2); // Cancel
    
    // Assert 1: Account points refunded correctly
    assertEquals(150, testAccount.getPoints(), 
        "Points should be 100 (original) + 50 (refund) = 150");
    
    // Assert 2: PointHistory saved with correct details
    verify(pointHistoryRepository).save(argThat(history ->
        history.getAccount().equals(testAccount)
        && history.getPoints() == 50    // Refund amount
        && history.getType().equals("REFUND") // or whatever type string
        && history.getOrderId().equals(testOrder.getId())
    ));
    
    // Assert 3: Account entity was persisted (not just in-memory)
    // If Account is managed entity → changes auto-saved via @Transactional
    // If not → verify entityManager.merge() or accountRepository.save()
    
    // Assert 4: Order status = 2 (CANCELLED)
    assertEquals(2, testOrder.getStatus());
    
    // Assert 5: pointsUsed NOT counted again on second cancel (idempotency)
    // Verify: if pointsUsed=0, no refund happens
}
```

---

## 4. MISSING TESTS

### 🚨 Missing Test 1: State Machine — CONFIRMED → NEW (Invalid Reverse Transition)
**CSV không cover:** Một order đã CONFIRMED (status=1) bị admin update ngược về status=0 (NEW)
- Đây là invalid state transition nhưng không có test
- Nếu service không validate → data corruption

### 🚨 Missing Test 2: Category ID Case Sensitivity
**Scenario:** Create category với id="CF" và "cf" — có bị coi là duplicate không?
- Nếu DB collation là case-insensitive → duplicate
- Nếu case-sensitive → 2 categories khác nhau
- Không có test nào verify behavior này

### 🚨 Missing Test 3: Order Cancel — Email Notification Verification
**CSV TC-ORD-CTRL-014 notes:** không đề cập, nhưng business-critical
- Khi order bị cancel, hệ thống có gửi email thông báo không?
- `OrderServiceImplTest` có mock `emailService` nhưng **không có bất kỳ test nào** verify `emailService.send()` được gọi sau cancel

### 🚨 Missing Test 4: Category Delete Cascade Check
**TC-CAT-CTRL-016:** Delete empty category → 200 OK, verify `!categoryRepository.existsById()`
- Nhưng không verify: các entity khác có foreign key đến category không bị orphan
- Và không test: nếu Category có icon file, file vật lý có bị xóa không?

### 🚨 Missing Test 5: Public Order — Checkout với Points Redemption
**TC-ORD-001** chỉ test happy path không dùng points
- Thiếu test: checkout với `pointsToUse=500` (redeem điểm)
- Thiếu test: checkout với `pointsToUse` > tài khoản có → 400
- `TC-ORD-SER-007` test refund nhưng không có test checkout WITH points deduction

---

## 5. TOP 5 MOST IMPORTANT TESTS

| Rank | Test ID | File | Lý do quan trọng |
|---|---|---|---|
| 🥇 1 | `TC-ORD-008` IDOR protection | `ApiOrderControllerTest` | User B xem order của User A = data breach. Critical security |
| 🥈 2 | `TC-ORD-SER-007` Points refund on cancel | `OrderServiceImplTest` | Financial integrity: points phải hoàn lại đúng khi cancel |
| 🥉 3 | `TC-CAT-CTRL-017` Delete category with products | `ApiAdminCategoryControllerTest` | Data integrity: orphan products nếu delete được category |
| 4 | `TC-ORD-CTRL-008/009` Status update + DB verify | `ApiAdminOrderControllerTest` | Real-flow DB check bắt được bug implement nhưng không persist |
| 5 | `TC-ORD-011` Deep pagination DoS | `ApiOrderControllerTest` | page=1001 DoS protection — production availability risk |

---

## 6. ADDITIONAL FINDINGS

### ⚠️ Finding 1: `assert` thay vì `assertEquals` trong Controller Tests

**File:** `ApiAdminOrderControllerTest.java` — lines 197, 215, 280, 322:
```java
assert updated.getStatus() == 1;  // ❌ Java assert — bị disable khi chạy không có -ea flag
```

**Đúng phải dùng:**
```java
assertEquals(1, updated.getStatus(), "Order status should be CONFIRMED (1)");
```

Java `assert` statements **bị disabled mặc định** trong JVM → những assertion này **không bao giờ chạy** trong CI pipeline → test luôn pass dù DB state sai!

---

### ⚠️ Finding 2: Order Controller dùng Real DB nhưng `ApiOrderControllerTest` dùng Mock

Hai bộ test **cùng domain Order** nhưng chiến lược hoàn toàn khác:
- `ApiAdminOrderControllerTest` → real DB (`@Transactional`, `@Autowired` repo)
- `ApiOrderControllerTest` → full mock (`@MockBean OrderService`)

**Nguy cơ:** Khi `OrderService` implementation thay đổi, Admin test sẽ bắt được bug, Public test sẽ không (vì mock không phản ánh real behavior). Cần thống nhất chiến lược hoặc bổ sung integration test cho public endpoints.

---

## 7. SUMMARY CHECKLIST

| Item | Status |
|---|---|
| TC-ORD-CTRL-001 đến -017 đầy đủ | ✅ 17/17 |
| TC-ORD-SER-001 đến -003 đầy đủ | ✅ 3/3 (+ 5 additional) |
| TC-CAT-CTRL-001 đến -022 đầy đủ | ✅ 22/22 |
| TC-CAT-SER-001 đến -003 đầy đủ | ✅ 3/3 (+ 7 additional) |
| DB state verification sau status update | ✅ Có (nhưng dùng `assert` sai) |
| Response format consistent ($.success vs $.status) | ❌ Inconsistent — phải fix |
| TC-ORD-CTRL-012/016 accept wrong 500 | ❌ Test bảo vệ bug |
| TC-CAT-SER-001 mâu thuẫn CSV spec | ❌ |
| TC-CAT-CTRL-012 accept special chars | ❌ Security gap |
| Email notification test sau cancel | ❌ Thiếu |
| State machine reverse transition test | ❌ Thiếu |
| Java `assert` thay vì `assertEquals` | ❌ Assertions không chạy trong CI |
