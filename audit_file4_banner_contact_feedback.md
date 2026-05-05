# 🔍 QA AUDIT REPORT — FILE 4
## TC_BATCH_04_BANNER_CONTACT_FEEDBACK.csv
### vs. Java Tests: `ApiAdminBannerControllerTest`, `ApiAdminContactControllerTest`, `ApiAdminFeedbackControllerTest`

---

## 1. OVERALL ASSESSMENT

| Dimension | Score | Verdict |
|---|---|---|
| Test Coverage (Controller) | 9/10 | Banner 22/22 ✅, Contact 12/10 (+2 bonus) ✅, Feedback 12/8 (+4 bonus) ✅ |
| Service Layer Tests | 0/10 | **CSV có 9 TC Service nhưng KHÔNG CÓ Java file nào** |
| Expected Result Quality | 7/10 | Banner/Contact tốt; Feedback TC-007 sai (400 vs 404) |
| Test Data Realism | 7/10 | Dùng mock data hợp lý; thiếu test data thực tế (tiếng Việt) |
| Assertion Depth | 6/10 | Banner/Feedback chỉ check HTTP + $.status; thiếu field-level verify |
| Security Coverage | 8/10 | Có auth test (401), XSS test, whitelist effect validation |
| Negative/Edge Coverage | 8/10 | Có boundary, not-found, idempotency, concurrent delete |
| Bug Detection Capability | 7/10 | Mock-only → không bắt được impl bug; nhưng documents gaps rõ |

**Overall Quality Score: 7.8 / 10**

**Key Strengths:**
- **Coverage hoàn hảo ở Controller layer:** Banner 22/22, Contact 10/10 + 2 bonus, Feedback 8/8 + 4 bonus
- `ApiAdminBannerControllerTest.test_create_xssInName_sanitizedAndSaved()` verify service được gọi với **sanitized string** — pattern tốt
- `ApiAdminFeedbackControllerTest.test_delete_concurrentRequests_raceCondition()` simulate race condition theo đúng luồng mock
- Contact `TC-CON-CTRL-004` document gap DoS (no size cap) rõ ràng, không che giấu
- Feedback FBK-CTRL-004/005 xác nhận FeedbackController có pagination guard (khác Contact Controller)

**Critical Weaknesses:**
- **ZERO Service layer tests** cho Banner, Contact, Feedback — 9 TC trong CSV hoàn toàn không có Java implementation
- `TC-BNR-CTRL-006` CREATE trả về `isOk()` thay vì `isCreated()` — sai spec CSV ghi "201 Created"
- `TC-BNR-CTRL-018` accept 200 khi imageId không tồn tại — silent-fail documented nhưng accepted
- `TC-CON-CTRL-002/003` accept 5xx as expected — protecting bug behavior
- `TC-FBK-CTRL-007` expect `isBadRequest()` nhưng CSV spec ghi "404 Not Found"
- Tất cả Banner/Contact/Feedback tests dùng `@MockBean` → KHÔNG bắt được DB-level bug

---

## 2. CRITICAL ISSUES

### ❌ ISSUE 1: Zero Service Layer Tests — 9 TC Trong CSV Hoàn Toàn Bỏ Trống

**CSV có các TC sau chưa có Java test:**

| TC ID | Mô tả | Risk |
|---|---|---|
| TC-BNR-SER-001 | findById not exists → exception | Controller trả 500 thay vì 404 nếu không handle |
| TC-BNR-SER-002 | create blank name → exception | Service không defensive → bypass controller validation |
| TC-BNR-SER-003 | create invalid effect → exception | Injection bypass nếu effect regex không validate |
| TC-BNR-SER-004 | activate not exists → exception | Silent pass → admin confused |
| TC-BNR-SER-005 | removeImage not exists → exception | Silent-fail vs throw — undefined behavior |
| TC-CON-SER-001 | markAsRead not exists → behavior? | Silent-fail accepted (bug confirmed but untracked) |
| TC-CON-SER-002 | countUnread accuracy | Incorrect badge count nếu query sai |
| TC-FBK-SER-001 | delete not exists → exception | Silent vs throw — HTTP status unpredictable |
| TC-FBK-SER-002 | findByBranch branch isolation | Cross-branch data leak |

**Impact:** Controller tests dùng `@MockBean` nên pass bất kể service implementation. Không có service tests → service bugs **không bao giờ bị bắt** trong CI pipeline.

---

### ❌ ISSUE 2: TC-BNR-CTRL-006 — Expect `isOk()` thay vì `isCreated()`

**CSV spec:** "Return **201 Created** with BannerSet object"

**Java test (`ApiAdminBannerControllerTest.java` line 148):**
```java
.andExpect(status().isOk())   // ❌ 200, không phải 201
.andExpect(jsonPath("$.status").value("SUCCESS"))
.andExpect(jsonPath("$.data.id").value(1));
```

**Vấn đề đã gặp ở Batch 01 và 02:** Test accept 200 nhưng CSV ghi 201. Nếu controller refactor để trả đúng 201:
- Test fail → Developer nghĩ refactor broke something → revert về 200 (sai spec)
- Test đang **bảo vệ wrong status code**, không bảo vệ spec

Tương tự: `TC-BNR-CTRL-019` (ADD images), `TC-BNR-CTRL-021` (ACTIVATE) cũng `isOk()` nhưng logic thay đổi state → có thể nên là 200 cho PUT/PATCH. Cần verify từng endpoint.

---

### ❌ ISSUE 3: TC-BNR-CTRL-018 — imageId Not Found Accepts Silent-Fail (200)

**CSV spec:** "Return **404 Not Found**"
> "bannerSetService.removeImage() → exception → verify 404"

**Java test (`ApiAdminBannerControllerTest.java` line 335-347):**
```java
@DisplayName("TC-BNR-CTRL-018: DELETE image imageId not found - returns 200 (no error)")
void test_removeImage_notFound_returns200() throws Exception {
    // Arrange - deleteById doesn't throw exception even if ID doesn't exist
    doNothing().when(bannerSetService).removeImage(99999L);  // ❌ Mock doesn't throw!
    
    mockMvc.perform(delete("/api/admin/banners/images/99999"))
        .andExpect(status().isOk())   // ❌ Accept 200 mặc dù CSV ghi 404
        .andExpect(jsonPath("$.status").value("SUCCESS"));
}
```

**Mâu thuẫn rõ ràng:**
- `TC-BNR-SER-005` trong CSV: "Throw ResourceNotFoundException (không silent-fail)"
- `TC-BNR-CTRL-018` trong Java: mock `doNothing()` + expect 200

**Test vừa contradicts CSV spec, vừa contradicts TC-BNR-SER-005.** Admin xóa image không tồn tại → nhận "SUCCESS" → không biết thao tác có thực sự xảy ra không.

---

### ❌ ISSUE 4: TC-CON-CTRL-002/003 — Accepting Bug Behavior (5xx)

**CSV spec TC-CON-CTRL-002:** "Return 500 ⚠️ GAP"  
**CSV spec TC-CON-CTRL-003:** "Return 500 ⚠️ GAP CRITICAL"

**Java test (`ApiAdminContactControllerTest.java` line 90):**
```java
// TC-CON-CTRL-002
.andExpect(status().is5xxServerError());  // ❌ Accepts bug

// TC-CON-CTRL-003
.andExpect(status().is5xxServerError());  // ❌ Accepts bug
```

**Pattern đã thấy ở Batch 02:** Test accept wrong behavior thay vì target correct behavior. Nếu developer fix ContactController (add pagination clamp), test sẽ **fail** → developer bị CI force revert fix → **CI bảo vệ bug**.

**Đúng pattern nên là:**
```java
// Target: 400 Bad Request (sau khi fix)
.andExpect(status().isBadRequest());  // Fail until fixed → CI drives fix
```

---

### ❌ ISSUE 5: TC-FBK-CTRL-007 — Status Code Mismatch với CSV

**CSV spec:** "Return **404 Not Found**"

**Java test (`ApiAdminFeedbackControllerTest.java` line 186-189):**
```java
doThrow(new BusinessRuleException("Không tìm thấy đánh giá với ID: 99999"))
    .when(feedbackService).delete(99999L);

mockMvc.perform(delete("/api/admin/feedbacks/99999"))
    .andExpect(status().isBadRequest())  // ❌ 400, CSV ghi 404
    .andExpect(jsonPath("$.message").value("Không tìm thấy đánh giá với ID: 99999"));
```

**Vấn đề:** Service throw `BusinessRuleException` → GlobalExceptionHandler map sang **400 Bad Request**. Nhưng "không tìm thấy" là **404 Not Found** theo REST semantics. Đây là lỗi thiết kế:
- `BusinessRuleException` nên dùng cho **business rules** (không thể xóa vì có order liên quan)
- "Resource not found" nên dùng `ResourceNotFoundException` → 404
- CSV spec đúng; Java code và test đều sai

---

## 3. IMPROVEMENT SUGGESTIONS

### 🔧 Rewrite 1: TC-BNR-CTRL-018 — Silent-Fail → Exception Per CSV Spec

**Original (accepts silent-fail, contradicts CSV + BNR-SER-005):**
```java
doNothing().when(bannerSetService).removeImage(99999L);
.andExpect(status().isOk())
```

**Rewritten (enforce exception per spec):**
```java
@Test
@DisplayName("TC-BNR-CTRL-018: DELETE image - imageId not found phải trả 404 (per CSV spec + TC-BNR-SER-005)")
void test_removeImage_notFound_returns404() throws Exception {
    // Arrange - service MUST throw ResourceNotFoundException (not silent-fail)
    doThrow(new ResourceNotFoundException("BannerImage not found: 99999"))
        .when(bannerSetService).removeImage(99999L);
    
    // Act & Assert
    mockMvc.perform(delete("/api/admin/banners/images/99999"))
        .andExpect(status().isNotFound())    // ✅ 404 per CSV spec
        .andExpect(jsonPath("$.status").value("ERROR"))
        .andExpect(jsonPath("$.message").value(containsString("99999")));
    
    // Verify: service WAS called (not bypassed)
    verify(bannerSetService).removeImage(99999L);
    
    // If this test FAILS with 200 OK → service has silent-fail bug
    // Fix: bannerSetService.removeImage() must throw ResourceNotFoundException
}
```

---

### 🔧 Rewrite 2: TC-CON-CTRL-002/003 — Target Fix, Not Current Bug

**Original (protects bug):**
```java
.andExpect(status().is5xxServerError());
```

**Rewritten (drives fix):**
```java
@Test
@DisplayName("TC-CON-CTRL-002: [FIX NEEDED] page=-1 phải trả 400, hiện trả 500 - cần add pagination clamp")
void test_getContacts_negativePage_shouldReturn400() throws Exception {
    // Target behavior after fix: 400 Bad Request with validation message
    // Current behavior (bug): 500 because PageRequest.of(-1, 20) throws IllegalArgumentException
    // Fix: ContactController should add: page = Math.max(page, 0)
    
    mockMvc.perform(get("/api/admin/contacts").param("page", "-1").param("size", "20"))
        .andExpect(status().isBadRequest())   // ✅ Target: 400 (test fails until fix applied)
        .andExpect(jsonPath("$.status").value("ERROR"));
    
    // If test FAILS with 500 → pagination clamp not yet implemented
    // Add to ContactController: int page = Math.max(rawPage, 0);
}
```

---

### 🔧 Rewrite 3: TC-FBK-CTRL-007 — Exception Type Correction

**Original (wrong exception type → wrong HTTP status):**
```java
doThrow(new BusinessRuleException("Không tìm thấy đánh giá với ID: 99999"))
.andExpect(status().isBadRequest())  // 400
```

**Rewritten (correct exception → 404):**
```java
@Test
@DisplayName("TC-FBK-CTRL-007: DELETE feedback id không tồn tại phải trả 404 (không phải 400)")
void test_delete_notFound_returns404() throws Exception {
    // ResourceNotFoundException maps to 404 (correct semantic for "not found")
    // BusinessRuleException maps to 400 (for rule violations, not "not found")
    doThrow(new ResourceNotFoundException("Không tìm thấy đánh giá với ID: 99999"))
        .when(feedbackService).delete(99999L);
    
    mockMvc.perform(delete("/api/admin/feedbacks/99999"))
        .andExpect(status().isNotFound())   // ✅ 404 per CSV spec
        .andExpect(jsonPath("$.status").value("ERROR"))
        .andExpect(jsonPath("$.message").value(containsString("99999")));
    
    verify(feedbackService).delete(99999L);
}
```

---

## 4. MISSING TESTS

### 🚨 Missing: Tất Cả 9 Service Layer Tests

**Không có file nào tương ứng với:**
- `BannerSetServiceImplTest.java`
- `ContactServiceImplTest.java`
- `StoreFeedbackServiceImplTest.java`

**Cần tạo với coverage tối thiểu:**

```java
// BannerSetServiceImplTest - TC-BNR-SER-001
@Test
void test_findById_notFound_throwsResourceNotFoundException() {
    when(bannerSetRepository.findById(99999L)).thenReturn(Optional.empty());
    assertThrows(ResourceNotFoundException.class, () -> bannerSetService.findById(99999L));
}

// TC-BNR-SER-003 - Effect bypass
@Test
void test_create_invalidEffect_throwsBusinessRuleException() {
    assertThrows(BusinessRuleException.class, () ->
        bannerSetService.create("Test", "INVALID_EFFECT", null, null, null));
}

// ContactServiceImplTest - TC-CON-SER-002
@Test  
void test_countUnread_returnsExactCount() {
    when(contactRepository.countByIsReadFalse()).thenReturn(3L);
    assertEquals(3L, contactService.countUnread());
}

// StoreFeedbackServiceImplTest - TC-FBK-SER-002
@Test
void test_findByBranch_returnOnlyThatBranch() {
    // Verify service calls repository with correct branch filter
    verify(feedbackRepository).findByBranch(eq("Hanoi"), any(Pageable.class));
    // And never calls findAll()
    verify(feedbackRepository, never()).findAll(any(Pageable.class));
}
```

### 🚨 Missing: Banner DB State Verification

Tất cả Banner tests dùng `@MockBean` nên không verify DB state. Cần thêm:
- `TC-BNR-CTRL-006 (Addendum)`: Sau CREATE, verify `bannerSetRepository.count()` tăng 1
- `TC-BNR-CTRL-015 (Addendum)`: Sau DELETE, verify cả BannerSet + BannerImages bị xóa (cascade)
- `TC-BNR-CTRL-021 (Addendum)`: Sau ACTIVATE, verify chỉ 1 banner có `active=true`

### 🚨 Missing: TC-BNR-CTRL-021 — Single Active Banner Logic

CSV ghi: "nếu có logic 'chỉ 1 banner active' → các banner khác bị deactivate"

Test hiện tại chỉ check banner được activate có `active=true`, **không verify** các banner khác bị set `active=false`. Đây là critical business rule nếu homepage chỉ show 1 banner.

### 🚨 Missing: TC-CON-CTRL-009 DB Verify

`TC-CON-CTRL-009` (MARK ALL READ) dùng mock `doNothing()` → không verify thực sự tất cả contacts được update trong DB. Cần integration test:
```java
// After markAllRead, verify count
when(contactService.countUnread()).thenReturn(0L);
// Then call GET list and verify unreadCount = 0
```

---

## 5. TOP 5 MOST IMPORTANT TESTS

| Rank | Test | File | Lý do |
|---|---|---|---|
| 🥇 1 | `test_create_xssInName_sanitizedAndSaved` | BannerTest | Verify service called với clean string — bắt XSS ở layer đúng |
| 🥈 2 | `test_getContacts_unreadCountAccuracy` | ContactTest | countUnread() phải chính xác — admin decision based on this |
| 🥉 3 | `test_activate_validId_returns200` | BannerTest | Homepage banner activation — direct UX impact |
| 4 | `test_delete_concurrentRequests_raceCondition` | FeedbackTest | Race condition pattern — correct sequential simulation |
| 5 | `test_create_invalidEffect_returns400` | BannerTest | Whitelist validation — prevents injection via effect param |

---

## 6. ADDITIONAL FINDINGS

### ⚠️ Finding 1: Banner vs Contact/Feedback — Inconsistent Testing Strategy

| Module | Strategy | Tradeoff |
|---|---|---|
| Banner | `@MockBean` → mock service | Fast, isolates controller, NO real DB verification |
| Contact | `@MockBean` → mock service | Same as Banner |
| Feedback | `@MockBean` → mock service | Same — but FBK-CTRL-008 simulates race condition via mock |

**Contrast với Batch 02 (Order/Category):** Dùng `@SpringBootTest + @Transactional + real repo` → bắt được real bugs.

**Risk:** Nếu BannerSetService.create() có bug (ví dụ: không save images), Banner controller test sẽ **pass** vì service bị mock → production bug undetected.

---

### ⚠️ Finding 2: TC-CON-CTRL-008 — Document của Gap Nguy Hiểm Nhưng Test Accept It

**Line 182-193:**
```java
@DisplayName("TC-CON-CTRL-008: MARK READ id not found - returns 200 (silent fail)")
void test_markRead_notFound_silentFail() throws Exception {
    doNothing().when(contactService).markAsRead(99999L);  // Mock silent-fail
    mockMvc.perform(patch("/api/admin/contacts/99999/read"))
        .andExpect(status().isOk());  // Accept 200 even though ID doesn't exist
```

**Admin behavior:** PATCH `/api/admin/contacts/99999/read` → 200 "Đã đánh dấu đã đọc" → Admin thinks contact was marked read, but nothing happened. **This is misleading UX** — users will be confused.

**CSV TC-CON-SER-001 ghi:** "Cần define rõ: throw hay silent-fail?"  
Test chọn silent-fail mà không document là "this is a gap we ACCEPT" vs "this is EXPECTED behavior".

---

### ⚠️ Finding 3: FBK-CTRL-012 — Branch Filter Case Sensitivity Not Fully Tested

**`test_list_branchFilterCaseInsensitive`** (line 253-270):
```java
when(feedbackService.findByBranch(eq("hcm"), any(PageRequest.class))).thenReturn(page);
// Sends "hcm" → expects service called with "hcm"
```

**Vấn đề:** Test chỉ verify service được gọi với string "hcm" nguyên bản — không verify controller có lowercase/normalize trước khi truyền vào service hay không. Real case-sensitivity test cần:
- Request `branch=HCM` → verify service called với `"HCM"` hoặc `"hcm"` (tùy convention)
- Request `branch=Hcm` → verify consistent normalization

---

## 7. SUMMARY CHECKLIST

| Item | Status |
|---|---|
| TC-BNR-CTRL-001 đến -022 | ✅ 22/22 (100%) |
| TC-CON-CTRL-001 đến -010 | ✅ 10/10 + 2 bonus (120%) |
| TC-FBK-CTRL-001 đến -008 | ✅ 8/8 + 4 bonus (150%) |
| TC-BNR-SER-001 đến -005 | ❌ 0/5 (không có Java file) |
| TC-CON-SER-001 đến -002 | ❌ 0/2 (không có Java file) |
| TC-FBK-SER-001 đến -002 | ❌ 0/2 (không có Java file) |
| Service layer test file tồn tại | ❌ Không có file nào |
| TC-BNR-CTRL-006 CREATE → 201 | ❌ Expect 200 (sai spec) |
| TC-BNR-CTRL-018 NOT FOUND → 404 | ❌ Accept 200 (silent-fail accepted) |
| TC-CON-CTRL-002/003 target 400 | ❌ Accept 500 (protect bug) |
| TC-FBK-CTRL-007 NOT FOUND → 404 | ❌ Expect 400 (wrong exception type) |
| DB state verify sau Banner ops | ❌ All mocked — no real DB |
| Single-active-banner logic verified | ❌ Thiếu |
| XSS sanitization (Banner name) | ✅ Verify service arg |
| Auth (401) test cho tất cả modules | ✅ Có |
| Race condition (Feedback delete) | ✅ Có (mock-based sequential) |
