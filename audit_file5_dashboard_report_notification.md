# 🔍 QA AUDIT REPORT — FILE 5
## TC_BATCH_05_DASHBOARD_REPORT_NOTIFICATION.csv
### vs. Java Tests: `ApiAdminDashboardControllerAdvancedTest`, `ApiAdminNotificationControllerAdvancedTest`, `ReportServiceAdvancedTest`

---

## 1. OVERALL ASSESSMENT

| Dimension | Score | Verdict |
|---|---|---|
| Test Coverage (vs CSV) | 6/10 | Dashboard: 10 ADV tests vs 10 CSV TC; Report: 10 ADV tests vs 13 CSV TC; Notification: 8 ADV tests vs 6 CSV TC |
| Service Layer Tests | 7/10 | ReportServiceAdvancedTest có 10 TC — nhưng không cover TC-DSH-SER-001 đến -005 & TC-NTF-SER-001/-002 |
| Test Quality | 7/10 | Nhiều "document bug" tests — discover bug nhưng không enforce fix |
| Assertion Depth | 7/10 | ISO date check, null safety, quarterly boundary — khá tốt |
| Bug Detection | 8/10 | TC-DSH-ADV-001 (missing months gap), TC-DSH-ADV-002 (aggregation mismatch), TC-DSH-ADV-008 (unsorted) bắt real bugs |
| Security Coverage | 6/10 | Có 401 tests; thiếu test ROLE_USER access dashboard (horizontal privilege check) |
| Architectural Awareness | 9/10 | TC-NTF-ADV-005 ghi lại architectural gap repo injection — tốt nhất trong tất cả batches |

**Overall Quality Score: 7.6 / 10**

**Key Strengths:**
- `TC-DSH-ADV-002` (aggregation mismatch: totalRevenue ≠ sum of monthly) phát hiện **data inconsistency bug thực tế** — khác query path dùng cho 2 số liệu khác nhau
- `TC-DSH-ADV-005/006` (ISO week + Q4 boundary) verify đúng edge case ngày tháng — thường bị bỏ qua
- `TC-NTF-ADV-005` document architectural violation (Controller inject Repository trực tiếp) rõ ràng — tech debt quantified
- `ReportServiceAdvancedTest.TC-RPT-ADV-006` verify `avgOrderValue` divide-by-zero safe — critical production guard
- `TC-NTF-ADV-001/002` test real-time consistency và cross-controller sync — pattern tốt

**Critical Weaknesses:**
- **`TC-DSH-ADV-001`**: Assert `hasSize(3)` thay vì `hasSize(12)` — test **accepts bug output** (missing months not filled)
- **`TC-DSH-ADV-008`**: Assert `recentOrders[0].id = 1` nhưng order id=1 là **oldest** — assertion sai logic sort DESC
- **`TC-NTF-ADV-008`** (performance test): Measure thời gian của **mock call** — không có giá trị thực tế
- **`TC-RPT-ADV-007`**: Assert `33.33` nhưng Java `BigDecimal` divide có thể throw `ArithmeticException` nếu thiếu scale/rounding
- **CSV TC-DSH-SER-001 đến -005 và TC-NTF-SER-001/-002 không có Java tests tương ứng**

---

## 2. CRITICAL ISSUES

### ❌ ISSUE 1: TC-DSH-ADV-001 — Assert `hasSize(3)` Thay Vì `hasSize(12)` — Accepts Bug

**CSV spec TC-DSH-SER-003:**
> "Return list with data for all 12 months of given year; tháng không có order → revenue=0 (không null)"

**Java test (`ApiAdminDashboardControllerAdvancedTest.java` line 78):**
```java
// BUG: Missing months should have revenue=0, but current implementation doesn't fill gaps
// Expected: 12 entries with months 2,4,6-12 having revenue=0
// Actual: Only 3 entries (months with data)
.andExpect(jsonPath("$.data.monthlyRevenue", hasSize(3)));  // ❌ Accepts bug!
```

**Vấn đề nghiêm trọng:**
- Test comment ghi đúng "Expected: 12 entries" nhưng assertion lại `hasSize(3)` — accept wrong behavior
- Frontend nhận 3 điểm dữ liệu thay vì 12 → biểu đồ chart có "gap" tháng 2, 4, 6-12 → UX bug
- **Đây là classic anti-pattern "documenting bug but not failing CI"**: Test sẽ luôn pass dù bug tồn tại

**Fix:**
```java
.andExpect(jsonPath("$.data.monthlyRevenue", hasSize(12)));  // Force 12 months
// All entries must have non-null revenue
.andExpect(jsonPath("$.data.monthlyRevenue[1].revenue").value(0.0))  // Month 2 = 0
```

---

### ❌ ISSUE 2: TC-DSH-ADV-008 — Sort Assertion Logic Sai

**CSV spec TC-DSH-SER-004:**
> "Return exactly N most recent orders (sorted by date desc)"

**Java test (`ApiAdminDashboardControllerAdvancedTest.java` line 286):**
```java
// Orders: id=1 (Apr 10), id=2 (Apr 12 - NEWEST), id=3 (Apr 11)
// DESC sort → expected: [id=2, id=3, id=1]

// BUG: Controller doesn't sort, relies on service
List<Order> unsortedOrders = List.of(order1, order2, order3); // [id=1, id=2, id=3]

.andExpect(jsonPath("$.data.recentOrders[0].id").value(1));  // ❌ Expects id=1 (OLDEST)!
```

**Mâu thuẫn logic:**
- Test tên: "must be DESC by createDate" → id=2 (Apr 12) nên ở vị trí [0]
- Assertion thực: `value(1)` → expect id=1 (Apr 10) ở [0] — **oldest first, not newest first**
- Tên test nói DESC, assertion kiểm tra ASC → **test logic internally contradicts itself**

**Comment trong test ghi:** "BUG: Controller doesn't sort, relies on service" → Đây là test document current wrong behavior, nhưng assertion tên phương thức và giá trị mâu thuẫn hoàn toàn.

---

### ❌ ISSUE 3: TC-NTF-ADV-008 — Performance Test Trên Mock Không Có Giá Trị

**Java test (`ApiAdminNotificationControllerAdvancedTest.java` line 213-238):**
```java
long startTime = System.currentTimeMillis();
mockMvc.perform(get("/api/admin/notifications/counts"))...
long endTime = System.currentTimeMillis();
long duration = endTime - startTime;

assertTrue(duration < 100, "Notification counts should be fast (<100ms)");
```

**Vấn đề:**
- `orderRepository` và `contactRepository` đều là `@MockBean` → gọi `countByStatus()` trong mock trả về ngay lập tức (< 1ms)
- Test đang measure thời gian **Spring MVC dispatch + serialization** không phải DB query
- Trong production, DB `COUNT(*)` query có thể mất **200-500ms** nếu thiếu index
- Test sẽ **luôn pass** (mock < 5ms) dù production chậm hơn 50x

**Test này cho false sense of security** — passes CI nhưng production có thể chậm.

**Cách đúng:** Performance test phải dùng real DB + đo với dữ liệu lớn (1M records).

---

### ❌ ISSUE 4: TC-RPT-ADV-007 — BigDecimal Division Assertion Rủi Ro

**Java test (`ReportServiceAdvancedTest.java` line 185):**
```java
// Arrange: totalOrders=3, totalRevenue=100.00
assertEquals(new BigDecimal("33.33"), result.getAvgOrderValue());
```

**Vấn đề:**
1. `100.00 / 3` = `33.333333...` — Java `BigDecimal.divide()` **không có scale/rounding mặc định** → throw `ArithmeticException: Non-terminating decimal expansion`
2. Nếu service dùng `divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP)` → 33.33 ✓
3. Nếu service dùng `divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_EVEN)` → 33.33 ✓ (same here)
4. Nếu service dùng `divide(BigDecimal.valueOf(3))` → **ArithmeticException** → test fails with exception

**Test không kiểm soát được rounding mode** — phụ thuộc implementation. Cần:
```java
// Explicit scale + rounding expectation
assertEquals(0, new BigDecimal("33.33").compareTo(result.getAvgOrderValue()),
    "avgOrderValue must be 33.33 with scale=2, HALF_UP rounding");
```

---

### ❌ ISSUE 5: TC-NTF-ADV-005 — Architectural Gap Documented But CI Never Fails

**Java test (`ApiAdminNotificationControllerAdvancedTest.java` line 143-178):**
```java
@DisplayName("TC-NTF-ADV-005: Architectural issue - controller injects repositories directly")
void test_architecture_controllerInjectsRepositoriesDirectly() throws Exception {
    // ... perform request, verify repos called ...
    
    // ARCHITECTURAL GAP:
    // ApiAdminNotificationController injects OrderRepository and ContactRepository directly
    // Violates 3-tier architecture
    // Fix: Create NotificationService
    // Mark as TECH DEBT
    
    verify(orderRepository).countByStatus(0);
    verify(contactRepository).countByIsReadFalse();
}
```

**Vấn đề:**
- Test **verify** repos được gọi → test PASSES khi architectural gap EXISTS
- Test nên **fail** nếu muốn enforce architecture fix
- Đúng cách: Dùng ArchUnit để assert `"No class in layer Controller should directly depend on Repository"`

**Hiện tại test này chỉ là một integration test bình thường, không enforce architecture.**

---

## 3. COVERAGE GAP — CSV TC Không Có Java Tests

### CSV TC-DSH-SER-001 đến -005 (5 TC) — Không Có Java Tests

| TC ID | Mô tả | Gap Risk |
|---|---|---|
| TC-DSH-SER-001 | `getOrderStats()` totalRevenue chỉ tính CONFIRMED | Revenue overcounting nếu tính cả CANCELLED |
| TC-DSH-SER-002 | `getDashboardCounts()` verify 3 repos mỗi cái 1 lần | Duplicate query (N+1) có thể xuất hiện |
| TC-DSH-SER-003 | `getMonthlyRevenue()` trả 12 tháng (với 0 cho tháng trống) | **Bug confirmed ở DSH-ADV-001** — service không fill gap |
| TC-DSH-SER-004 | `getRecentOrders(5)` trả đúng 5 orders mới nhất | `ReportServiceAdvancedTest.TC-RPT-ADV-008/009` cover một phần |
| TC-DSH-SER-005 | `getTopProducts(5)` sorted by qty sold desc | Không có test nào verify sort order của topProducts |

### CSV TC-NTF-SER-001/-002 (2 TC) — Không Có Java Tests

| TC ID | Mô tả | Gap Risk |
|---|---|---|
| TC-NTF-SER-001 | `countByStatus(0)` returns exact NEW order count | Cross-repo count mismatch |
| TC-NTF-SER-002 | `countUnreadContacts()` returns exact unread count | isRead filter correctness |

**Lưu ý:** Notification "service" hiện tại là repository trực tiếp trong controller → `TC-NTF-SER-*` thực chất là repository method tests. Khi refactor sang NotificationService, cần tạo `NotificationServiceImplTest`.

### Các TC Report không có Java tests

| TC ID | Mô tả |
|---|---|
| TC-RPT-SER-001 | year=0 defaults to current year — `TC-RPT-ADV-001` cover nhưng test controller logic (không phải service) |
| TC-RPT-SER-002/003 | limit clamp 100/1 — `TC-RPT-ADV-004/005` cover nhưng test sau clamp, không test trước |
| TC-RPT-SER-004 | totalRevenue=0 not null — `TC-RPT-ADV-006` cover ✅ |
| TC-RPT-CTRL-013 | All report endpoints require auth — Chỉ Dashboard có test 401, Report endpoints thiếu |

---

## 4. IMPROVEMENT SUGGESTIONS

### 🔧 Rewrite 1: TC-DSH-ADV-001 — Enforce 12-Month Fill

**Original (accepts bug):**
```java
.andExpect(jsonPath("$.data.monthlyRevenue", hasSize(3)));  // Only 3 months
```

**Rewritten (enforce correct behavior):**
```java
@Test
@DisplayName("TC-DSH-SER-003: Monthly revenue PHẢI có đủ 12 tháng — tháng trống = revenue 0")
void test_monthlyRevenue_missingMonths_MUST_fillWithZero() throws Exception {
    // Only months 1, 3, 5 have actual data
    List<RevenueReportDTO> partialData = List.of(
        new RevenueReportDTO(2024, 1, new BigDecimal("100.00"), 2L),
        new RevenueReportDTO(2024, 3, new BigDecimal("200.00"), 3L),
        new RevenueReportDTO(2024, 5, new BigDecimal("150.00"), 1L)
    );
    when(reportService.getMonthlyRevenue(anyInt())).thenReturn(partialData);
    // ... other mocks ...

    mockMvc.perform(get("/api/admin/dashboard"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.monthlyRevenue", hasSize(12)))   // ✅ MUST be 12
        // Month 2 (index 1) must have revenue=0, not missing
        .andExpect(jsonPath("$.data.monthlyRevenue[1].month").value(2))
        .andExpect(jsonPath("$.data.monthlyRevenue[1].revenue").value(0))
        // Month 4 (index 3) must have revenue=0
        .andExpect(jsonPath("$.data.monthlyRevenue[3].month").value(4))
        .andExpect(jsonPath("$.data.monthlyRevenue[3].revenue").value(0));
    
    // If this test FAILS with hasSize(3) → service/controller gap: doesn't fill missing months
    // Fix: Controller or service must fill months 1-12, defaulting to 0
}
```

---

### 🔧 Rewrite 2: TC-DSH-ADV-008 — Fix Sort Assertion Logic

**Original (assertion contradicts test name):**
```java
// Test says DESC, assertion expects oldest first
.andExpect(jsonPath("$.data.recentOrders[0].id").value(1));  // id=1 = oldest
```

**Rewritten (consistent with DESC spec):**
```java
@Test
@DisplayName("TC-DSH-SER-004: Recent orders PHẢI được sort DESC theo createDate — mới nhất đầu tiên")
void test_recentOrders_sorting_MUST_be_descByCreateDate() throws Exception {
    // id=1: Apr 10 (oldest), id=2: Apr 12 (newest), id=3: Apr 11 (middle)
    // After DESC sort: [id=2, id=3, id=1]
    
    when(reportService.getRecentOrders(5)).thenReturn(List.of(order1, order2, order3));
    // ... other mocks ...

    mockMvc.perform(get("/api/admin/dashboard"))
        .andExpect(status().isOk())
        // ✅ Newest order (id=2, Apr 12) must be first
        .andExpect(jsonPath("$.data.recentOrders[0].id").value(2))
        // ✅ Middle order (id=3, Apr 11) must be second
        .andExpect(jsonPath("$.data.recentOrders[1].id").value(3))
        // ✅ Oldest order (id=1, Apr 10) must be last
        .andExpect(jsonPath("$.data.recentOrders[2].id").value(1));
    
    // If test FAILS with [id=1, id=2, id=3] → sort bug confirmed
    // Fix: Controller must sort recentOrders by createDate DESC before returning
}
```

---

### 🔧 Rewrite 3: TC-NTF-ADV-005 — Từ "Document Gap" → "Enforce Architecture"

**Original (passes when architectural gap exists):**
```java
// Just verifies repo calls - passes even when violation exists
verify(orderRepository).countByStatus(0);
verify(contactRepository).countByIsReadFalse();
```

**Rewritten (với ArchUnit để fail khi violation exists):**
```java
// Thêm vào architecture test directory
// Cần dependency: com.tngtech.archunit:archunit-junit5

@AnalyzeClasses(packages = "com.springboot.jenka_coffee")
class ArchitectureLayerRulesTest {
    
    @ArchTest
    static final ArchRule controllers_should_not_depend_on_repositories =
        noClasses().that().resideInAPackage("..api..")
            .should().dependOnClassesThat()
            .resideInAPackage("..repository..")
            .because("Controllers must use Service layer, not Repository directly. " +
                     "Violation: ApiAdminNotificationController injects OrderRepository + ContactRepository. " +
                     "Fix: Create NotificationService");
}
```

---

## 5. NOTABLE STRENGTHS (This Batch Stands Out)

### ✅ Strength 1: TC-DSH-ADV-002 Aggregation Mismatch — Production-Critical Bug

Test phát hiện: `getOrderStats()` và `getMonthlyRevenue()` có thể dùng **query khác nhau** dẫn đến `totalRevenue != sum(monthly)`. Đây là bug thực tế rất khó phát hiện:

```java
// totalRevenue = 1000.00 (từ getOrderStats())
// sum(monthly) = 300 + 400 + 200 = 900.00
// Mismatch 100! Admin thấy 2 con số không khớp trên dashboard → confuse
```

**Root cause thường gặp:** `getOrderStats()` tính tất cả năm, `getMonthlyRevenue()` chỉ tính năm hiện tại.

### ✅ Strength 2: TC-DSH-ADV-005/006 Time Boundary Tests

```java
// Week 1 of 2024: from=2024-01-01, to=2024-01-07
.andExpect(jsonPath("$.data.from").value(containsString("2024-01-01")))
.andExpect(jsonPath("$.data.to").value(containsString("2024-01-07")));

// Q4 2024: from=2024-10-01, to=2024-12-31
.andExpect(jsonPath("$.data.from").value(containsString("2024-10-01")))
.andExpect(jsonPath("$.data.to").value(containsString("2024-12-31")));
```

Off-by-one bugs trong date range calculation rất phổ biến và khó debug → tests này có giá trị cao.

### ✅ Strength 3: TC-NTF-ADV-001/002 Cross-Controller Consistency

Test sequence: Update state via mock → verify notification count phản ánh thay đổi → đây là **cross-controller integration pattern** đúng đắn.

### ✅ Strength 4: TC-RPT-ADV-010 — No Duplicate Queries

```java
verify(orderRepository, times(1)).count();    // Exactly once
verify(productRepository, times(1)).count();  // Exactly once
verify(accountRepository, times(1)).count();  // Exactly once
```

Verify N+1 query prevention — critical cho dashboard performance.

---

## 6. MISSING TESTS

### 🚨 Missing: Report Endpoints Auth Test (TC-RPT-CTRL-013)

CSV ghi: "verify TẤT CẢ report endpoints đều require JWT"

Hiện tại chỉ có:
- `TC-DSH-CTRL-010` auth test cho dashboard
- `TC-NTF-ADV-006` auth test cho notifications

**Thiếu:** Auth tests cho `/api/admin/reports/revenue/monthly`, `/yearly`, `/customers/top`, `/stats/overview`

### 🚨 Missing: TC-DSH-SER-001 — Only CONFIRMED Orders Count for Revenue

```java
// Service phải KHÔNG include CANCELLED orders trong totalRevenue
// Test: mock 3 confirmed (100/200/300) + 2 cancelled (500/1000)
// Expected: totalRevenue = 600 (không phải 2100)
```

### 🚨 Missing: TC-DSH-CTRL-009 — Invalid Period Fallback Documented But Not Tested Against 400

CSV notes: "Better UX: Return 400 Bad Request with error message"
Test hiện tại accept 200 fallback → CSV suggests this should be 400 → gap undecided.

### 🚨 Missing: TC-RPT-CTRL-004 — Negative Year Behavior

```java
GET /api/admin/reports/revenue/monthly?year=-1
// Verify: empty list (or 400 validation error)
// Current: passes through to DB → empty result
```

---

## 7. SUMMARY CHECKLIST

| Item | Status |
|---|---|
| TC-DSH-CTRL-001 đến -010 | ⚠️ 10 ADV tests thay CSV TC trực tiếp |
| TC-DSH-SER-001 đến -005 | ❌ 0/5 — không có Java test tương ứng |
| TC-RPT-CTRL-001 đến -013 | ⚠️ Có ADV tests nhưng không map 1:1 với CSV |
| TC-RPT-SER-001 đến -004 | ⚠️ Partial — RPT-ADV cover 3/4 |
| TC-NTF-CTRL-001 đến -006 | ✅ NTF-ADV-001 đến -008 (over-cover) |
| TC-NTF-SER-001/-002 | ❌ 0/2 — không có Java test |
| 12-month fill cho monthly revenue | ❌ Bug documented, test accepts wrong output |
| Recent orders DESC sort verified | ❌ Assertion logic sai (expects oldest) |
| Aggregation mismatch documented | ✅ TC-DSH-ADV-002 rất valuable |
| Time boundary (week/quarter) | ✅ TC-DSH-ADV-005/006 |
| Null safety (BigDecimal null) | ✅ TC-DSH-ADV-010 |
| Divide-by-zero (avgOrderValue) | ⚠️ TC-RPT-ADV-006 có risk ArithmeticException |
| Performance test valid | ❌ Mock-based performance test vô nghĩa |
| Architecture enforcement (ArchUnit) | ❌ Chỉ document, không enforce |
| Report endpoints auth test | ❌ TC-RPT-CTRL-013 thiếu |
| Negative year validation | ❌ Gap documented, no enforcement |
