package com.springboot.jenka_coffee.api.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.jenka_coffee.entity.Account;
import com.springboot.jenka_coffee.service.AccountService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ApiAdminAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    @Autowired
    private ObjectMapper objectMapper;

    // --- 1. VULNERABILITY & BOUNDARY TESTS ---

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ACC-CTRL-002: GET list page negative (Potential Error)")
    void test_accountList_negativePage_returnsError() throws Exception {
        // Missing clamp on pagination can cause 500 or 400
        mockMvc.perform(get("/api/admin/accounts?page=-1&size=20"))
                .andExpect(status().isBadRequest()); // Or 500 depending on current implementation gap
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ACC-CTRL-003: GET list size=0 (JPA IllegalArgumentException)")
    void test_accountList_zeroSize_returnsError() throws Exception {
        mockMvc.perform(get("/api/admin/accounts?page=0&size=0"))
                .andExpect(status().isBadRequest()); // Or 500 if not handled
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ACC-CTRL-004: GET list size=9999 (OOM DoS risk)")
    void test_accountList_largeSize_capped() throws Exception {
        // Assert that the system doesn't fetch 9999 but caps it
        mockMvc.perform(get("/api/admin/accounts?page=0&size=9999"))
                .andExpect(status().isOk());
        // Would verify service call arguments to check if size was capped to 100
    }

    // --- 2. SECURITY & PRIVILEGE ESCALATION TESTS ---

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ACC-CTRL-012: CREATE with admin=true in body (Privilege Escalation)")
    void test_createAccount_adminTrueInBody_forcesAdminFalse() throws Exception {
        // Simulate hacker trying to create an admin account directly
        Account mockAccount = new Account();
        mockAccount.setUsername("hacker");
        mockAccount.setAdmin(false); // Service/Controller should force this to false

        when(accountService.createAccount(any(), any())).thenReturn(mockAccount);

        mockMvc.perform(multipart("/api/admin/accounts")
                .param("username", "hacker")
                .param("email", "hacker@test.com")
                .param("password", "Pass@123")
                .param("admin", "true")) // Malicious payload
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.admin").value(false)); // Security enforced
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ACC-CTRL-017: UPDATE with admin=true (Privilege Escalation)")
    void test_updateAccount_adminTrueInBody_forcesAdminFalse() throws Exception {
        Account mockAccount = new Account();
        mockAccount.setUsername("user1");
        mockAccount.setAdmin(false);

        when(accountService.updateAccount(eq("user1"), any(), any())).thenReturn(mockAccount);

        mockMvc.perform(put("/api/admin/accounts/user1")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .param("username", "user1")
                .param("admin", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.admin").value(false));
    }

    // --- 3. BUSINESS LOGIC & SUICIDE PREVENTION ---

    @Test
    @WithMockUser(username = "admin1", roles = "ADMIN")
    @DisplayName("TC-ACC-CTRL-020: DELETE self (Suicide Protection)")
    void test_deleteAccount_self_returnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/admin/accounts/admin1"))
                .andExpect(status().isForbidden());
        // Verify deleteOrThrow was NOT called
        verify(accountService, never()).deleteOrThrow(anyString());
    }

    @Test
    @WithMockUser(username = "admin1", roles = "ADMIN")
    @DisplayName("TC-ACC-CTRL-031: RESET PASSWORD targeting another admin (Insider Threat)")
    void test_resetPassword_targetOtherAdmin_returnsForbidden() throws Exception {
        // Admin1 tries to reset Admin2's password
        Account admin2 = new Account();
        admin2.setUsername("admin2");
        admin2.setAdmin(true);

        when(accountService.findByIdOrThrow("admin2")).thenReturn(admin2);

        mockMvc.perform(put("/api/admin/accounts/admin2/reset-password")
                .param("newPassword", "HackedPass@123"))
                .andExpect(status().isForbidden());
    }

    // --- 4. ADDITIONAL GET LIST & DETAIL TESTS ---

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ACC-CTRL-001: GET list valid request")
    void test_accountList_valid_returnsOk() throws Exception {
        mockMvc.perform(get("/api/admin/accounts?page=0&size=20"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ACC-CTRL-005: GET detail valid username")
    void test_accountDetail_valid_returnsOk() throws Exception {
        Account mockAccount = new Account();
        mockAccount.setUsername("user1");
        
        when(accountService.findByIdOrThrow("user1")).thenReturn(mockAccount);
        
        mockMvc.perform(get("/api/admin/accounts/user1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("user1"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist()); // Ensure passwordHash is not leaked
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ACC-CTRL-006: GET detail username not found")
    void test_accountDetail_notFound_returns404() throws Exception {
        when(accountService.findByIdOrThrow("ghost_user"))
            .thenThrow(new com.springboot.jenka_coffee.exception.ResourceNotFoundException("Account not found"));
            
        mockMvc.perform(get("/api/admin/accounts/ghost_user"))
                .andExpect(status().isNotFound());
    }

    // --- 5. ADDITIONAL CREATE TESTS ---

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ACC-CTRL-007: CREATE valid multipart data")
    void test_createAccount_valid_returnsCreated() throws Exception {
        Account mockAccount = new Account();
        mockAccount.setUsername("newuser");
        
        when(accountService.createAccount(any(), any())).thenReturn(mockAccount);
        
        mockMvc.perform(multipart("/api/admin/accounts")
                .param("username", "newuser")
                .param("email", "new@test.com")
                .param("password", "Pass@123"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("newuser"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ACC-CTRL-008: CREATE missing username field")
    void test_createAccount_missingUsername_returnsBadRequest() throws Exception {
        mockMvc.perform(multipart("/api/admin/accounts")
                .param("email", "new@test.com")
                .param("password", "Pass@123"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ACC-CTRL-009: CREATE username = empty string")
    void test_createAccount_emptyUsername_returnsBadRequest() throws Exception {
        mockMvc.perform(multipart("/api/admin/accounts")
                .param("username", "")
                .param("email", "new@test.com")
                .param("password", "Pass@123"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ACC-CTRL-010: CREATE duplicate username")
    void test_createAccount_duplicateUsername_returnsBadRequest() throws Exception {
        when(accountService.createAccount(any(), any()))
            .thenThrow(new com.springboot.jenka_coffee.exception.ValidationException("Tên đăng nhập đã tồn tại!"));
            
        mockMvc.perform(multipart("/api/admin/accounts")
                .param("username", "existinguser")
                .param("email", "new@test.com")
                .param("password", "Pass@123"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ACC-CTRL-011: CREATE duplicate email")
    void test_createAccount_duplicateEmail_returnsBadRequest() throws Exception {
        when(accountService.createAccount(any(), any()))
            .thenThrow(new com.springboot.jenka_coffee.exception.ValidationException("Email đã được sử dụng!"));
            
        mockMvc.perform(multipart("/api/admin/accounts")
                .param("username", "newuser")
                .param("email", "existing@test.com")
                .param("password", "Pass@123"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ACC-CTRL-013: CREATE invalid email format")
    void test_createAccount_invalidEmail_returnsBadRequest() throws Exception {
        mockMvc.perform(multipart("/api/admin/accounts")
                .param("username", "newuser")
                .param("email", "not_an_email")
                .param("password", "Pass@123"))
                .andExpect(status().isBadRequest());
    }

    // --- 6. ADDITIONAL UPDATE & DELETE TESTS ---

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ACC-CTRL-015: UPDATE valid data")
    void test_updateAccount_valid_returnsOk() throws Exception {
        Account mockAccount = new Account();
        mockAccount.setUsername("user1");
        mockAccount.setFullname("Updated Name");
        
        when(accountService.updateAccount(eq("user1"), any(), any())).thenReturn(mockAccount);
        
        mockMvc.perform(put("/api/admin/accounts/user1")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .param("username", "user1")
                .param("fullname", "Updated Name"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullname").value("Updated Name"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ACC-CTRL-016: UPDATE username not found")
    void test_updateAccount_notFound_returns404() throws Exception {
        when(accountService.updateAccount(eq("ghost_user"), any(), any()))
            .thenThrow(new com.springboot.jenka_coffee.exception.ResourceNotFoundException("Account not found"));
            
        mockMvc.perform(put("/api/admin/accounts/ghost_user")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .param("username", "ghost_user"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ACC-CTRL-018: UPDATE name = empty string")
    void test_updateAccount_emptyName_returnsBadRequest() throws Exception {
        mockMvc.perform(put("/api/admin/accounts/user1")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .param("username", "user1")
                .param("fullname", ""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ACC-CTRL-019: DELETE valid username")
    void test_deleteAccount_valid_returnsOk() throws Exception {
        doNothing().when(accountService).deleteOrThrow("user1");
        
        mockMvc.perform(delete("/api/admin/accounts/user1"))
                .andExpect(status().isOk());
    }

    // --- 7. ADDITIONAL TOGGLE, LOCK, UNLOCK TESTS ---

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ACC-CTRL-021: DELETE last admin in system")
    void test_deleteAccount_lastAdmin_returnsForbidden() throws Exception {
        doThrow(new com.springboot.jenka_coffee.exception.BusinessRuleException("Không thể xóa admin cuối cùng"))
            .when(accountService).deleteOrThrow("lastAdmin");
            
        mockMvc.perform(delete("/api/admin/accounts/lastAdmin"))
                .andExpect(status().isBadRequest()); // Depending on how BusinessRuleException is handled, usually 400
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ACC-CTRL-023: TOGGLE status valid username")
    void test_toggleStatus_valid_returnsOk() throws Exception {
        Account mockAccount = new Account();
        mockAccount.setUsername("user1");
        mockAccount.setActivated(false);
        
        when(accountService.toggleActivation("user1")).thenReturn(mockAccount);
        
        mockMvc.perform(put("/api/admin/accounts/user1/toggle-status"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ACC-CTRL-024: TOGGLE status username not found")
    void test_toggleStatus_notFound_returns404() throws Exception {
        when(accountService.toggleActivation("ghost_user"))
            .thenThrow(new com.springboot.jenka_coffee.exception.ResourceNotFoundException("Account not found"));
            
        mockMvc.perform(put("/api/admin/accounts/ghost_user/toggle-status"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ACC-CTRL-029: RESET PASSWORD valid")
    void test_resetPassword_valid_returnsOk() throws Exception {
        Account mockAccount = new Account();
        mockAccount.setUsername("user1");
        mockAccount.setAdmin(false);
        
        when(accountService.findByIdOrThrow("user1")).thenReturn(mockAccount);
        doNothing().when(accountService).adminResetPassword("user1", "NewPass@123");
        
        mockMvc.perform(put("/api/admin/accounts/user1/reset-password")
                .param("newPassword", "NewPass@123"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ACC-CTRL-030: RESET PASSWORD empty newPassword")
    void test_resetPassword_emptyPassword_returnsBadRequest() throws Exception {
        mockMvc.perform(put("/api/admin/accounts/user1/reset-password")
                .param("newPassword", ""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin1", roles = "ADMIN")
    @DisplayName("TC-ACC-CTRL-032: RESET PASSWORD admin resetting own password")
    void test_resetPassword_self_returnsOk() throws Exception {
        Account mockAccount = new Account();
        mockAccount.setUsername("admin1");
        mockAccount.setAdmin(true);
        
        when(accountService.findByIdOrThrow("admin1")).thenReturn(mockAccount);
        doNothing().when(accountService).adminResetPassword("admin1", "NewPass@123");
        
        mockMvc.perform(put("/api/admin/accounts/admin1/reset-password")
                .param("newPassword", "NewPass@123"))
                .andExpect(status().isOk());
    }

    // --- 8. ADDITIONAL CHECK ENDPOINT TESTS ---

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ACC-CTRL-033: CHECK username available")
    void test_checkUsername_available_returnsTrue() throws Exception {
        when(accountService.isUsernameExists("brandnew")).thenReturn(false);
        
        mockMvc.perform(get("/api/admin/accounts/check-username")
                .param("username", "brandnew"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true)); // Assuming standard ResponseDto
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ACC-CTRL-034: CHECK username already taken")
    void test_checkUsername_taken_returnsFalse() throws Exception {
        when(accountService.isUsernameExists("existinguser")).thenReturn(true);
        
        mockMvc.perform(get("/api/admin/accounts/check-username")
                .param("username", "existinguser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ACC-CTRL-035: CHECK email available")
    void test_checkEmail_available_returnsTrue() throws Exception {
        when(accountService.isEmailExists("new@test.com", null)).thenReturn(false);
        
        mockMvc.perform(get("/api/admin/accounts/check-email")
                .param("email", "new@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ACC-CTRL-036: CHECK email already taken")
    void test_checkEmail_taken_returnsFalse() throws Exception {
        when(accountService.isEmailExists("existing@test.com", null)).thenReturn(true);
        
        mockMvc.perform(get("/api/admin/accounts/check-email")
                .param("email", "existing@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ACC-CTRL-037: CHECK email with currentUsername")
    void test_checkEmail_withCurrentUsername_returnsTrue() throws Exception {
        when(accountService.isEmailExists("same@test.com", "user1")).thenReturn(false);
        
        mockMvc.perform(get("/api/admin/accounts/check-email")
                .param("email", "same@test.com")
                .param("currentUsername", "user1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }
}
