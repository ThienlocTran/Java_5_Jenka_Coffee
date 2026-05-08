package com.springboot.jenka_coffee.api.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.jenka_coffee.entity.Account;
import com.springboot.jenka_coffee.repository.AccountRepository;
import com.springboot.jenka_coffee.util.PasswordSecurity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@org.springframework.test.annotation.Rollback  // Explicit rollback after each test
class ApiAdminAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordSecurity passwordSecurity;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @BeforeEach
    void setUp() {
        // DON'T use deleteAll() - causes foreign key constraint violations with orders table
        // @Transactional will rollback changes after each test automatically
        
        // AGGRESSIVE CLEANUP: Delete test accounts using native query to bypass Hibernate cache
        entityManager.createNativeQuery("DELETE FROM Accounts WHERE Username IN ('user1', 'admin1', 'admin2', 'hacker', 'newuser', 'newuser2', 'brandnew')")
                .executeUpdate();
        
        // CRITICAL: Flush deletes AND clear session to prevent merge() on REMOVED entities
        entityManager.flush();
        entityManager.clear();  // Clear Hibernate session cache completely
        
        // EXTRA: Verify "newuser" is really deleted from DB
        Long count = (Long) entityManager.createNativeQuery("SELECT COUNT(*) FROM Accounts WHERE Username = 'newuser'")
                .getSingleResult();
        assertEquals(0L, count, "newuser should be deleted before test");

        Account testUser = new Account();
        testUser.setUsername("user1");
        testUser.setFullname("Test User");
        testUser.setEmail("user1@test.com");
        // DON'T set phone to avoid duplicate key conflicts
        // testUser.setPhone("0123456789");
        testUser.setPasswordHash(passwordSecurity.hashPassword("Password@123"));
        testUser.setActivated(true);
        testUser.setAdmin(false);
        testUser.setNew(true);  // Mark as new for Persistable
        accountRepository.save(testUser);

        Account admin1 = new Account();
        admin1.setUsername("admin1");
        admin1.setFullname("Admin One");
        admin1.setEmail("admin1@test.com");
        // admin1.setPhone("0123456781"); // Unique phone if needed
        admin1.setPasswordHash(passwordSecurity.hashPassword("Admin@123"));
        admin1.setActivated(true);
        admin1.setAdmin(true);
        admin1.setNew(true);  // Mark as new for Persistable
        accountRepository.save(admin1);

        Account admin2 = new Account();
        admin2.setUsername("admin2");
        admin2.setFullname("Admin Two");
        admin2.setEmail("admin2@test.com");
        // admin2.setPhone("0123456782"); // Unique phone if needed
        admin2.setPasswordHash(passwordSecurity.hashPassword("Admin@123"));
        admin2.setActivated(true);
        admin2.setAdmin(true);
        admin2.setNew(true);  // Mark as new for Persistable
        accountRepository.save(admin2);
        
        // Flush inserts to ensure they're committed before tests run
        entityManager.flush();
        entityManager.clear();  // Clear again after setup
    }

    // --- 1. VULNERABILITY & BOUNDARY TESTS (REAL FLOW) ---

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ACC-CTRL-002: GET list page negative (Potential Error)")
    void test_accountList_negativePage_returnsError() throws Exception {
        // Will expose if the system lacks pagination bounds checking (likely 500 or 400)
        mockMvc.perform(get("/api/admin/accounts?page=-1&size=20"))
                .andExpect(status().isBadRequest()); // Expect 400 Bad Request, NOT 500!
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ACC-CTRL-003: GET list size=0 (JPA IllegalArgumentException)")
    void test_accountList_zeroSize_returnsError() throws Exception {
        // Will expose if size=0 throws unhandled JPA error (500)
        mockMvc.perform(get("/api/admin/accounts?page=0&size=0"))
                .andExpect(status().isBadRequest()); // Expect 400 Bad Request, NOT 500!
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ACC-CTRL-004: GET list size=9999 (OOM DoS risk)")
    void test_accountList_largeSize_capped() throws Exception {
        mockMvc.perform(get("/api/admin/accounts?page=0&size=999999"))
                .andExpect(status().isBadRequest()); // DoS prevention: reject abnormally large size with 400
    }

    // --- 2. SECURITY & PRIVILEGE ESCALATION TESTS ---

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ACC-CTRL-012: CREATE với admin=true trong body - Server phải force admin=false (VULN-057)")
    void test_createAccount_adminTrueInBody_forcesAdminFalse() throws Exception {
        mockMvc.perform(multipart("/api/admin/accounts")
                .param("username", "hacker")
                .param("fullname", "Hacker User")  // FIX: fullname is required
                .param("email", "hacker@test.com")
                .param("password", "Pass@123")
                .param("admin", "true")) // Malicious payload
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                // Check BOTH possible response paths — adjust to match actual API
                .andExpect(result -> {
                    String body = result.getResponse().getContentAsString();
                    assertFalse(body.contains("\"admin\":true"),
                        "SECURITY BUG: Response body contains admin=true. Privilege escalation succeeded!");
                });

        // CRITICAL: Clear session before DB check to prevent auto-flush of detached entity
        entityManager.flush();
        entityManager.clear();
        
        // CRITICAL: DB must confirm admin=false regardless of what response says
        Account saved = accountRepository.findById("hacker")
            .orElseThrow(() -> new AssertionError("Account not created"));
        
        assertFalse(saved.getAdmin(),
            "SECURITY BREACH: Account 'hacker' was saved with admin=true in DB. VULN-057 not fixed!");
        
        // Verify the forced override happened
        assertEquals("hacker", saved.getUsername());
        assertNotNull(saved.getPasswordHash(), "Password must be BCrypt hashed");
        assertTrue(saved.getPasswordHash().startsWith("$2a$"), "Password must be BCrypt format");
        assertFalse(saved.getPasswordHash().equals("Pass@123"), "Password must NOT be stored in plaintext!");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ACC-CTRL-017: UPDATE với admin=true (Privilege Escalation)")
    void test_updateAccount_adminTrueInBody_forcesAdminFalse() throws Exception {
        mockMvc.perform(put("/api/admin/accounts/user1")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .param("username", "user1")
                .param("fullname", "Updated Name")
                .param("admin", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                // Check response body doesn't contain admin=true
                .andExpect(result -> {
                    String body = result.getResponse().getContentAsString();
                    assertFalse(body.contains("\"admin\":true"),
                        "SECURITY BUG: Response body contains admin=true. Privilege escalation succeeded!");
                });

        // REAL DB check:
        Account dbUser = accountRepository.findById("user1").orElseThrow();
        assertFalse(dbUser.getAdmin(), "Security Bug: Regular user was escalated to admin!");
    }

    // --- 3. BUSINESS LOGIC & SUICIDE PREVENTION ---

    @Test
    @DisplayName("TC-ACC-CTRL-020: DELETE self (Suicide Protection)")
    @WithMockUser(username = "admin1", roles = "ADMIN")
    void test_deleteAccount_self_returnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/admin/accounts/admin1"))
                .andExpect(status().isForbidden()); // Or 400 with custom message

        // REAL DB check:
        assertTrue(accountRepository.findById("admin1").isPresent(), "Admin suicide protection failed, account was deleted!");
    }

    @Test
    @DisplayName("TC-ACC-CTRL-021: DELETE last admin in system")
    @WithMockUser(username = "admin1", roles = "ADMIN")
    void test_deleteAccount_lastAdmin_returnsForbidden() throws Exception {
        // First delete admin2 manually so admin1 is the last admin
        accountRepository.deleteById("admin2");
        entityManager.flush();
        
        // FIX: Don't assert exact count - DB may have real admins
        // Just verify admin1 exists and is admin
        Account admin1 = accountRepository.findById("admin1").orElseThrow();
        assertTrue(admin1.getAdmin(), "admin1 must be an admin");
        
        long adminCountBefore = accountRepository.countByAdminTrue();
        assertTrue(adminCountBefore >= 1, "Must have at least 1 admin");

        // Now admin1 tries to delete the last admin (which happens to be themselves, but even if it was someone else)
        mockMvc.perform(delete("/api/admin/accounts/admin1"))
                .andExpect(status().is4xxClientError()); 

        assertTrue(accountRepository.findById("admin1").isPresent());
    }

    @Test
    @DisplayName("TC-ACC-CTRL-031: RESET PASSWORD targeting another admin (Insider Threat)")
    @WithMockUser(username = "admin1", roles = "ADMIN")
    void test_resetPassword_targetOtherAdmin_returnsForbidden() throws Exception {
        mockMvc.perform(put("/api/admin/accounts/admin2/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newPassword\": \"HackedPass@123\"}"))
                .andExpect(status().isForbidden());

        // Verify password hash didn't change
        Account admin2Db = accountRepository.findById("admin2").orElseThrow();
        assertTrue(passwordSecurity.verifyPassword("Admin@123", admin2Db.getPasswordHash()), "Insider threat: Admin2 password was changed by Admin1!");
    }

    @Test
    @DisplayName("TC-ACC-CTRL-032: RESET PASSWORD admin resetting own password")
    @WithMockUser(username = "admin1", roles = "ADMIN")
    void test_resetPassword_self_returnsOk() throws Exception {
        mockMvc.perform(put("/api/admin/accounts/admin1/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newPassword\": \"NewPass@123\"}"))
                .andExpect(status().isOk());

        Account admin1Db = accountRepository.findById("admin1").orElseThrow();
        assertTrue(passwordSecurity.verifyPassword("NewPass@123", admin1Db.getPasswordHash()), "Self password reset failed");
    }

    // --- 4. CRUD & VALIDATION TESTS ---

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ACC-CTRL-001: GET list - passwordHash KHÔNG được xuất hiện trong BẤT KỲ item nào")
    void test_accountList_noPasswordHashLeak() throws Exception {
        mockMvc.perform(get("/api/admin/accounts?page=0&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content").isNotEmpty())
                // SECURITY: passwordHash must never appear anywhere in response
                .andExpect(result -> {
                    String body = result.getResponse().getContentAsString();
                    assertFalse(body.contains("passwordHash"),
                        "SECURITY BREACH: passwordHash field leaked in account list response!");
                    assertFalse(body.contains("$2a$"),
                        "SECURITY BREACH: BCrypt hash leaked (starts with $2a$)!");
                })
                // Pagination must be accurate (Spring Page structure)
                .andExpect(jsonPath("$.data.number").value(0))
                .andExpect(jsonPath("$.data.totalElements").isNumber())
                .andExpect(jsonPath("$.data.totalPages").isNumber());
        
        // The DB has user1 + admin1 + admin2 → totalElements >= 3
        // (actual count depends on other tests, but must be >= 1 since setUp created data)
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ACC-CTRL-005: GET detail valid username")
    void test_accountDetail_valid_returnsOk() throws Exception {
        mockMvc.perform(get("/api/admin/accounts/user1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.username").value("user1"))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ACC-CTRL-006: GET detail username not found")
    void test_accountDetail_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/admin/accounts/ghost_user"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ACC-CTRL-007: CREATE valid multipart data")
    void test_createAccount_valid_returnsCreated() throws Exception {
        mockMvc.perform(multipart("/api/admin/accounts")
                .param("username", "newuser")
                .param("fullname", "New User")  // FIX: fullname is required
                .param("email", "new@test.com")
                .param("password", "Pass@123"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.username").value("newuser"));

        // CRITICAL: Clear session before DB check to prevent auto-flush of detached entity
        entityManager.flush();
        entityManager.clear();
        
        // Verify account was created with correct data
        Account saved = accountRepository.findById("newuser")
                .orElseThrow(() -> new AssertionError("Account not created"));
        
        assertEquals("newuser", saved.getUsername());
        assertEquals("New User", saved.getFullname());
        assertEquals("new@test.com", saved.getEmail());
        assertNotNull(saved.getPasswordHash(), "Password must be BCrypt hashed");
        assertTrue(saved.getPasswordHash().startsWith("$2a$"), "Password must be BCrypt format");
        assertFalse(saved.getPasswordHash().equals("Pass@123"), "Password must NOT be stored in plaintext!");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ACC-CTRL-010: CREATE duplicate username")
    void test_createAccount_duplicateUsername_returnsBadRequest() throws Exception {
        mockMvc.perform(multipart("/api/admin/accounts")
                .param("username", "user1") // Already exists
                .param("fullname", "Duplicate User")  // FIX: fullname is required
                .param("email", "newemail@test.com")
                .param("password", "Pass@123"))
                .andExpect(status().isBadRequest()); // Will fail if controller returns 500 (bug)
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ACC-CTRL-011: CREATE duplicate email")
    void test_createAccount_duplicateEmail_returnsBadRequest() throws Exception {
        mockMvc.perform(multipart("/api/admin/accounts")
                .param("username", "newuser2")
                .param("fullname", "Another User")  // FIX: fullname is required
                .param("email", "user1@test.com") // Already exists
                .param("password", "Pass@123"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ACC-CTRL-019: DELETE valid username")
    void test_deleteAccount_valid_returnsOk() throws Exception {
        mockMvc.perform(delete("/api/admin/accounts/user1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        assertFalse(accountRepository.existsById("user1"));
    }

    // --- 5. CHECK ENDPOINTS ---

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ACC-CTRL-033: CHECK username available")
    void test_checkUsername_available_returnsTrue() throws Exception {
        mockMvc.perform(get("/api/admin/accounts/check-username")
                .param("username", "brandnew"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ACC-CTRL-034: CHECK username already taken")
    void test_checkUsername_taken_returnsFalse() throws Exception {
        mockMvc.perform(get("/api/admin/accounts/check-username")
                .param("username", "user1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data").value(false));
    }
}
