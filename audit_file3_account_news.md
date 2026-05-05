# 🔍 QA AUDIT REPORT — FILE 3
## TC_BATCH_03_ACCOUNT_NEWS.csv
### vs. Java Tests: `ApiAdminAccountControllerTest`, `ApiAdminNewsControllerTest`, `AccountServiceImplTest`, `NewsServiceImplTest`

---

## 1. OVERALL ASSESSMENT

| Dimension | Score | Verdict |
|---|---|---|
| Test Coverage | 7/10 | Account: 20/37 TC có test. News: 9/21 TC có test. Service: 7/7 TC Account + 4/4 TC News |
| Expected Result Quality | 7/10 | Security tests có message verification tốt; CRUD tests thiếu field-level check |
| Test Data Realism | 8/10 | Dùng real BCrypt hash, real email, real password format — tốt |
| Assertion Depth | 8/10 | DB state verify sau security tests rất tốt (privilege escalation, suicide, insider threat) |
| Security Coverage | 9/10 | Bộ tốt nhất trong các batch: privilege escalation + self-delete + insider threat + XSS + mass assignment |
| Negative/Edge Coverage | 6/10 | Thiếu nhiều TC: toggle status 2 lần, lock idempotency, email format check, XSS in update |
| Bug Detection Capability | 8/10 | Real-flow tests bắt được nhiều bug thực; nhưng 2 service tests có expected result sai |

**Overall Quality Score: 7.4 / 10**

**Key Strengths:**
- `ApiAdminAccountControllerTest` là bộ test chất lượng cao nhất project: real DB + real BCrypt + security verification
- `TC-ACC-CTRL-031` (insider threat: admin1 reset password admin2) có cả HTTP verify + DB BCrypt verify → production-ready
- `TC-ACC-CTRL-020/021` (suicide + last admin protection) với `accountRepository.countByAdminTrue()` real check
- `AccountServiceImplTest` cover đủ 7 TC CSV + nhiều additional tests có giá trị (timing attack, expired token)
- `TC-NEWS-CTRL-010` (XSS sanitization) verify cả response lẫn DB — đúng chuẩn

**Critical Weaknesses:**
- **Response path sai:** Account Controller check `$.admin` thay vì `$.data.admin` — có thể false-positive
- **TC-NEWS-SER-001 và TC-NEWS-SER-004:** Service implementation trả `null` / silent-fail, nhưng CSV spec yêu cầu `throw ResourceNotFoundException` — Java test chấp nhận wrong behavior
- **`TC-ACC-SER-006b`:** Test vừa document gap vừa assertNotNull kết quả của gap — không ngăn bug
- **News concurrent test:** Cùng lỗi `@Transactional` class-level + `ExecutorService` threads = flaky
- **Thiếu 17/37 Account TC** và **12/21 News TC** chưa có Java implementation

---

## 2. CRITICAL ISSUES

### ❌ ISSUE 1: Response Path Sai — `$.admin` vs `$.data.admin`

**File:** `ApiAdminAccountControllerTest.java` — lines 114, 132

```java
// TC-ACC-CTRL-012
.andExpect(jsonPath("$.admin").value(false));

// TC-ACC-CTRL-017
.andExpect(jsonPath("$.admin").value(false));
```

**Vấn đề:** Các test khác trong cùng controller check `$.items` (line 201), `$.username` (line 210). Nếu response wrapper là:
```json
{ "status": "SUCCESS", "data": { "username": "hacker", "admin": false } }
```
Thì `$.admin` sẽ trả `null` (không exist) → `value(false)` sẽ fail. Nhưng nếu response không có wrapper → `$.admin` đúng.

**Nguy hiểm:** Nếu `$.admin` không exist, `jsonPath("$.admin").value(false)` có thể **pass hoặc fail** tùy JsonPath version. Phải check cả `$.admin` lẫn `$.data.admin` để chắc chắn path đúng.

**Tương tự:** `$.items` ở line 201 — nếu response là `{ "data": { "items": [...] } }` thì path sai.

---

### ❌ ISSUE 2: TC-NEWS-SER-001 — Java Test Accepts Wrong Behavior

**CSV spec:**
> "Expected: Throw exception (ResourceNotFound or NoSuchElementException)"
> "Verify exception type; Controller cần catch và map → 404"

**Java test (`NewsServiceImplTest.java` line 51-58):**
```java
@DisplayName("TC-NEWS-SER-001: FindById news id not exists")
void test_findById_notFound_returnsNull() {
    when(newsRepository.findById(99999)).thenReturn(Optional.empty());
    
    News result = newsService.findById(99999);
    
    assertNull(result, "Should return null when news ID not found");  // ❌ Accept null!
}
```

**Vấn đề kép:**
1. CSV yêu cầu throw exception, Java test expect `null` → **contradicts CSV spec**
2. Nếu service trả `null` về controller → controller phải null-check thủ công → nếu quên → `NullPointerException` ở controller
3. Test confirm pattern nguy hiểm (return null) thay vì enforce pattern an toàn (throw exception)

**Real-world impact:** Controller gọi `newsService.findById(id)` → nhận null → không check → NPE → 500 Internal Server Error thay vì clean 404

---

### ❌ ISSUE 3: TC-NEWS-SER-004 — toggleAvailable Silent-Fail vs CSV Expect Exception

**CSV spec:**
> "Expected: Throw ResourceNotFoundException"
> "Verify exception type; Controller map → 404"

**Java test (`NewsServiceImplTest.java` line 94-107):**
```java
@DisplayName("TC-NEWS-SER-004: ToggleAvailable id not exists")
void test_toggleAvailable_notFound_doesNothing() {
    when(newsRepository.findById(99999)).thenReturn(Optional.empty());
    
    assertDoesNotThrow(() -> newsService.toggleAvailable(99999));  // ❌ Accept silent-fail!
    verify(newsRepository, never()).save(any());
}
```

**Vấn đề:** Silent-fail trong `toggleAvailable()` khi ID không tồn tại:
- Admin gọi PUT `/api/admin/news/99999/toggle` → service làm gì không → trả 200 OK
- Admin nghĩ là đã toggle thành công nhưng không có gì xảy ra
- **UX bug + data integrity risk:** Admin bị mislead

**Test hiện tại bảo vệ bug silent-fail** thay vì enforce throw exception như CSV mô tả.

---

### ❌ ISSUE 4: TC-ACC-SER-006b — Document Gap nhưng Không Enforce Fix

**CSV TC-ACC-SER-006 spec:**
> "Validate trước BCrypt encode → exception; verify không lưu empty hash vào DB"

**Java test `TC_ACC_SER_006`** (correct test):
```java
// hashPassword("") throws IllegalArgumentException → adminResetPassword() throws → correct
when(passwordSecurity.hashPassword("")).thenThrow(new IllegalArgumentException(...));
assertThrows(IllegalArgumentException.class, () -> accountService.adminResetPassword("user1", ""));
```

**Nhưng `TC_ACC_SER_006b`** (contradicts it):
```java
when(passwordSecurity.hashPassword("")).thenReturn("$2a$12$hashOfEmpty"); // hash được!
accountService.adminResetPassword("user1", "");
assertNotNull(testAccount.getLastPasswordResetDate(), "GAP: admin không validate..."); // Assert GAP
```

**Vấn đề:** Test 006 và 006b có **setup mâu thuẫn** (006: hash throws, 006b: hash returns). Chạy cùng suite → cả 2 pass nhưng chúng test 2 scenario khác nhau. Thêm vào đó, `006b` assertNotNull để "confirm gap" nhưng không `fail()` → gap không bao giờ bị CI bắt.

---

### ❌ ISSUE 5: News Concurrent Test — Flaky vì `@Transactional` + Threads

**File:** `ApiAdminNewsControllerTest.java` — line 95-127

```java
@Transactional  // Class-level annotation
void test_createNews_concurrentSameTitle_createsBoth() throws InterruptedException {
    ExecutorService executor = Executors.newFixedThreadPool(2);
    // ...threads submit mockMvc requests...
    assertEquals(initialCount + 2, newsRepository.count()); // ❌ sai!
}
```

**Vấn đề giống Batch 02:**
- Child threads không kế thừa transaction của test thread → mỗi request trong thread riêng có transaction riêng
- `newsRepository.count()` trong test thread thấy transaction của test thread, không thấy data từ child threads
- **Kết quả: `newsRepository.count()` luôn bằng `initialCount`** → assertion luôn fail
- Hoặc nếu threads share connection pool → deadlock → test hang indefinitely

---

## 3. IMPROVEMENT SUGGESTIONS

### 🔧 Rewrite 1: TC-NEWS-SER-001 — Null Return → Exception Pattern

**Original (accepts null, dangerous):**
```java
assertNull(result, "Should return null when news ID not found");
```

**Rewritten (enforce throw exception per CSV spec):**
```java
@Test
@DisplayName("TC-NEWS-SER-001: findById() với id không tồn tại PHẢI throw ResourceNotFoundException (không return null)")
void test_findById_notFound_throwsResourceNotFoundException() {
    when(newsRepository.findById(99999)).thenReturn(Optional.empty());
    
    // CSV spec: Throw exception → Controller map → 404
    ResourceNotFoundException exception = assertThrows(
        ResourceNotFoundException.class,
        () -> newsService.findById(99999),
        "findById() MUST throw ResourceNotFoundException, not return null. " +
        "Returning null forces controller to null-check manually → NPE risk"
    );
    
    assertTrue(exception.getMessage().contains("99999"),
        "Exception message must include the missing ID for debugging");
    
    verify(newsRepository).findById(99999);
    
    // IMPORTANT: If this test fails (assertDoesNotThrow passes instead):
    // → Implementation returns null → controller will NPE → 500 instead of 404
    // Fix: newsService.findById() must throw ResourceNotFoundException when empty
}
```

---

### 🔧 Rewrite 2: TC-ACC-CTRL-012 — Privilege Escalation với Full Response Path Check

**Original (response path uncertain):**
```java
.andExpect(jsonPath("$.admin").value(false))
```

**Rewritten (robust path + full security check):**
```java
@Test
@WithMockUser(roles = "ADMIN")
@DisplayName("TC-ACC-CTRL-012: CREATE với admin=true trong body - Server phải force admin=false (VULN-057)")
void test_createAccount_adminTrueInBody_forcesAdminFalse() throws Exception {
    mockMvc.perform(multipart("/api/admin/accounts")
            .param("username", "hacker")
            .param("email", "hacker@test.com")
            .param("password", "Pass@123")
            .param("fullname", "Hacker Attempt")
            .param("admin", "true")) // Malicious payload
        .andExpect(status().isCreated())
        // Check BOTH possible response paths — adjust to match actual API
        .andExpect(result -> {
            String body = result.getResponse().getContentAsString();
            assertFalse(body.contains("\"admin\":true"),
                "SECURITY BUG: Response body contains admin=true. Privilege escalation succeeded!");
        });
    
    // CRITICAL: DB must confirm admin=false regardless of what response says
    Account saved = accountRepository.findById("hacker")
        .orElseThrow(() -> new AssertionError("Account not created"));
    
    assertFalse(saved.getAdmin(),
        "SECURITY BREACH: Account 'hacker' was saved with admin=true in DB. VULN-057 not fixed!");
    
    // Verify the forced override happened
    assertEquals("hacker", saved.getUsername());
    assertNotNull(saved.getPasswordHash(), "Password must be BCrypt hashed");
    assertFalse(saved.getPasswordHash().equals("Pass@123"), "Password must NOT be stored in plaintext!");
}
```

---

### 🔧 Rewrite 3: TC-ACC-CTRL-001 — passwordHash Leak Check cho Toàn Bộ List

**Original (only checks first item, incomplete):**
```java
.andExpect(jsonPath("$.items[0].passwordHash").doesNotExist())
```

**Rewritten (check ALL items + pagination info):**
```java
@Test
@WithMockUser(roles = "ADMIN")
@DisplayName("TC-ACC-CTRL-001: GET list - passwordHash KHÔNG được xuất hiện trong BẤT KỲ item nào")
void test_accountList_noPasswordHashLeak() throws Exception {
    mockMvc.perform(get("/api/admin/accounts").param("page", "0").param("size", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray())
        .andExpect(jsonPath("$.items").isNotEmpty())
        // SECURITY: passwordHash must never appear anywhere in response
        .andExpect(result -> {
            String body = result.getResponse().getContentAsString();
            assertFalse(body.contains("passwordHash"),
                "SECURITY BREACH: passwordHash field leaked in account list response!");
            assertFalse(body.contains("$2a$"),
                "SECURITY BREACH: BCrypt hash leaked (starts with $2a$)!");
        })
        // Pagination must be accurate
        .andExpect(jsonPath("$.currentPage").value(0))
        .andExpect(jsonPath("$.totalItems").isNumber())
        .andExpect(jsonPath("$.totalPages").isNumber());
    
    // The DB has user1 + admin1 + admin2 → totalItems >= 3
    // (actual count depends on other tests, but must be >= 1 since setUp created data)
}
```

---

## 4. MISSING TESTS (Coverage Gaps)

### 🚨 Missing Account Controller Tests (17 TC chưa có Java)

| TC ID | Title | Risk nếu không test |
|---|---|---|
| TC-ACC-CTRL-008/009 | CREATE missing/empty username | Input validation gap |
| TC-ACC-CTRL-013 | CREATE invalid email format | `@Email` validation không active |
| TC-ACC-CTRL-014 | CREATE photo file invalid MIME | Security: file upload bypass |
| TC-ACC-CTRL-015 | UPDATE valid data | Core CRUD không verified |
| TC-ACC-CTRL-016 | UPDATE username not found | 404 vs 500 gap |
| TC-ACC-CTRL-018 | UPDATE name empty string | Boundary validation |
| TC-ACC-CTRL-022 | DELETE username not found | Gap: 500 thay vì 404 |
| TC-ACC-CTRL-023 | TOGGLE status 2 lần | Idempotency — trả về đúng state không |
| TC-ACC-CTRL-025/026/027 | LOCK: valid, idempotent, not found | Lock mechanism untested |
| TC-ACC-CTRL-028 | UNLOCK account | Symmetry with lock |
| TC-ACC-CTRL-029/030 | RESET PASSWORD valid, empty | Core security function |
| TC-ACC-CTRL-035/036/037 | CHECK email APIs | Business utility functions |

### 🚨 Missing News Controller Tests (12 TC chưa có Java)

| TC ID | Title | Risk |
|---|---|---|
| TC-NEWS-CTRL-003/004 | size=9999 cap + page negative | Pagination DoS |
| TC-NEWS-CTRL-007 | CREATE valid (full field verify) | Core CRUD |
| TC-NEWS-CTRL-009 | CREATE title empty | Validation |
| TC-NEWS-CTRL-011 | CREATE without imageFile | Null handling |
| TC-NEWS-CTRL-013 | UPDATE valid data | Core CRUD |
| TC-NEWS-CTRL-014/015 | UPDATE not found + empty title | Error handling |
| TC-NEWS-CTRL-016 | UPDATE title XSS | **Security — same risk as CREATE** |
| TC-NEWS-CTRL-018/020 | TOGGLE/DELETE not found | 404 vs 500 |

### 🚨 Missing Test: passwordHash Leak trong Single Account Response

**TC-ACC-CTRL-005** check `$.passwordHash.doesNotExist()` — tốt.  
Nhưng không check `$.data.passwordHash` nếu response có wrapper. Cần test cả hai paths.

---

## 5. TOP 5 MOST IMPORTANT TESTS

| Rank | Test ID | File | Lý do |
|---|---|---|---|
| 🥇 1 | `TC-ACC-CTRL-031` Insider threat reset password | `ApiAdminAccountControllerTest` | Admin1 reset Admin2 password = internal privilege abuse. Real BCrypt verify |
| 🥈 2 | `TC-ACC-CTRL-012` Privilege escalation on CREATE | `ApiAdminAccountControllerTest` | VULN-057: admin=true bypass → production security breach |
| 🥉 3 | `TC-NEWS-CTRL-010` XSS sanitization | `ApiAdminNewsControllerTest` | Stored XSS: title saved with script tags → all users affected |
| 4 | `TC-ACC-CTRL-020/021` Self-delete + last admin | `ApiAdminAccountControllerTest` | Headless system (no admin) = complete service lockout |
| 5 | `testResetPasswordExpiredToken` | `AccountServiceImplTest` | Expired token still accepted → account takeover after reset link leak |

---

## 6. ADDITIONAL FINDINGS

### ⚠️ Finding 1: `AccountServiceImplTest` — Timing Attack Test có Logic Lỗi

**Test `testAuthenticateUserNotFound` (line 303-314):**
```java
when(passwordSecurity.verifyPassword(anyString(), anyString())).thenReturn(false);
// ...
verify(passwordSecurity).verifyPassword(eq("password123"), anyString());
```

**Vấn đề:** Mock `verifyPassword(anyString(), anyString())` không verify được *dummy BCrypt hash* được truyền vào. Để test timing attack prevention đúng, cần verify rằng `verifyPassword` được gọi với **dummy hash cụ thể** (thường là một constant), không phải `anyString()`. Nếu implementation dùng `null` thay vì dummy hash → NPE, không phải timing-safe.

---

### ⚠️ Finding 2: `ApiAdminNewsControllerTest` dùng `import java.util.Date` nhưng Entity dùng `LocalDateTime`

**Line 18:** `import java.util.Date;` — không dùng, gây confusion.  
`testNews.setCreateDate(LocalDateTime.now())` — đúng.  
Nhưng unused import cho thấy test có thể có sự nhầm lẫn về type trong quá trình develop.

---

### ⚠️ Finding 3: `ApiAdminAccountControllerTest` — `deleteAll()` trong `setUp()` Có Thể Conflict

**Line 43:** `accountRepository.deleteAll();` trong `@BeforeEach`

Với `@Transactional` class-level, `setUp()` chạy trong cùng transaction với test. Nếu có FK constraint từ bảng khác (orders, notifications...) → `deleteAll()` sẽ throw `ConstraintViolationException`.

Production DB thường có accounts liên kết với orders → test suite sẽ fail hoàn toàn nếu DB không phải fresh H2/test DB.

---

## 7. SUMMARY CHECKLIST

| Item | Status |
|---|---|
| TC-ACC-SER-001 đến -007 đầy đủ | ✅ 7/7 (+ nhiều additional) |
| TC-NEWS-SER-001 đến -004 đầy đủ | ✅ 4/4 |
| TC-ACC-CTRL-001 đến -037 | ⚠️ 20/37 (~54%) |
| TC-NEWS-CTRL-001 đến -021 | ⚠️ 9/21 (~43%) |
| passwordHash không leak (list + detail) | ⚠️ Partial (detail ok, list chỉ [0]) |
| Privilege escalation admin=true | ✅ Có + DB verify |
| Insider threat (admin reset admin) | ✅ Có + BCrypt verify |
| Self-delete + last admin protection | ✅ Có + DB verify |
| XSS sanitization | ✅ Có (CREATE) — ❌ thiếu UPDATE |
| News TC-NEWS-SER-001 khớp CSV spec | ❌ Mâu thuẫn (null vs throw) |
| News TC-NEWS-SER-004 khớp CSV spec | ❌ Mâu thuẫn (silent-fail vs throw) |
| Concurrent test reliable | ❌ Flaky (@Transactional + threads) |
| Response path $.admin verified | ⚠️ Uncertain — cần confirm path |
| Timing attack prevention verified đúng | ⚠️ anyString() không đủ |
