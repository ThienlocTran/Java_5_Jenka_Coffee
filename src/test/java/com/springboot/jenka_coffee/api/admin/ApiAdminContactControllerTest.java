package com.springboot.jenka_coffee.api.admin;

import com.springboot.jenka_coffee.entity.Contact;
import com.springboot.jenka_coffee.service.ContactService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TC-CON-CTRL-001 to TC-CON-CTRL-010: Contact Controller Integration Tests
 * Tests HTTP endpoints with security and pagination validation
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiAdminContactControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ContactService contactService;

    private Contact testContact;

    @BeforeEach
    void setUp() {
        testContact = new Contact();
        testContact.setId(1L);
        testContact.setFullName("John Doe");
        testContact.setEmail("john@example.com");
        testContact.setSubject("Test Subject");
        testContact.setMessage("Test Message");
        testContact.setIsRead(false);
    }

    @Test
    @DisplayName("TC-CON-CTRL-001: GET list valid request - returns paginated list with unreadCount")
    @WithMockUser(roles = "ADMIN")
    void test_getContacts_validRequest_returnsPaginatedList() throws Exception {
        // Arrange
        List<Contact> contacts = List.of(testContact);
        Page<Contact> page = new PageImpl<>(contacts, PageRequest.of(0, 20), 1);
        when(contactService.findAll(any(PageRequest.class))).thenReturn(page);
        when(contactService.countUnread()).thenReturn(3L);

        // Act & Assert
        mockMvc.perform(get("/api/admin/contacts")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.currentPage").value(0))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.totalItems").value(1))
                .andExpect(jsonPath("$.data.unreadCount").value(3));

        verify(contactService).findAll(any(PageRequest.class));
        verify(contactService).countUnread();
    }

    @Test
    @DisplayName("TC-CON-CTRL-002: GET list page negative - NO validation (potential 500)")
    @WithMockUser(roles = "ADMIN")
    void test_getContacts_negativePage_noValidation() throws Exception {
        // Arrange - PageRequest.of() throws IllegalArgumentException for negative page
        when(contactService.findAll(any(PageRequest.class)))
                .thenThrow(new IllegalArgumentException("Page index must not be less than zero"));

        // Act & Assert - GAP: Controller doesn't validate, passes to service
        mockMvc.perform(get("/api/admin/contacts")
                        .param("page", "-1")
                        .param("size", "20"))
                .andExpect(status().is5xxServerError());

        verify(contactService).findAll(any(PageRequest.class));
    }

    @Test
    @DisplayName("TC-CON-CTRL-003: GET list size=0 - NO validation (CRITICAL GAP - 500)")
    @WithMockUser(roles = "ADMIN")
    void test_getContacts_sizeZero_noValidation() throws Exception {
        // Arrange - PageRequest.of() throws IllegalArgumentException for size <= 0
        when(contactService.findAll(any(PageRequest.class)))
                .thenThrow(new IllegalArgumentException("Page size must not be less than one"));

        // Act & Assert - CRITICAL GAP: size=0 causes 500 error
        mockMvc.perform(get("/api/admin/contacts")
                        .param("page", "0")
                        .param("size", "0"))
                .andExpect(status().is5xxServerError());

        verify(contactService).findAll(any(PageRequest.class));
    }

    @Test
    @DisplayName("TC-CON-CTRL-004: GET list size=9999 - NO cap (DoS risk)")
    @WithMockUser(roles = "ADMIN")
    void test_getContacts_largeSize_noCap() throws Exception {
        // Arrange - No max size validation, could load 9999 records
        List<Contact> contacts = List.of(testContact);
        Page<Contact> page = new PageImpl<>(contacts, PageRequest.of(0, 9999), 1);
        when(contactService.findAll(any(PageRequest.class))).thenReturn(page);
        when(contactService.countUnread()).thenReturn(0L);

        // Act & Assert - SECURITY GAP: No max size cap (memory DoS)
        mockMvc.perform(get("/api/admin/contacts")
                        .param("page", "0")
                        .param("size", "9999"))
                .andExpect(status().isOk());

        verify(contactService).findAll(PageRequest.of(0, 9999));
    }

    @Test
    @DisplayName("TC-CON-CTRL-005: GET list verify unreadCount accuracy")
    @WithMockUser(roles = "ADMIN")
    void test_getContacts_unreadCountAccuracy() throws Exception {
        // Arrange - 5 contacts, 3 unread
        List<Contact> contacts = List.of(testContact);
        Page<Contact> page = new PageImpl<>(contacts, PageRequest.of(0, 20), 5);
        when(contactService.findAll(any(PageRequest.class))).thenReturn(page);
        when(contactService.countUnread()).thenReturn(3L);

        // Act & Assert
        mockMvc.perform(get("/api/admin/contacts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(3))
                .andExpect(jsonPath("$.data.totalItems").value(5));

        verify(contactService).countUnread();
    }

    @Test
    @DisplayName("TC-CON-CTRL-006: MARK READ valid contact id - returns 200")
    @WithMockUser(roles = "ADMIN")
    void test_markRead_validId_returns200() throws Exception {
        // Arrange
        doNothing().when(contactService).markAsRead(1L);

        // Act & Assert
        mockMvc.perform(patch("/api/admin/contacts/1/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Đã đánh dấu đã đọc"));

        verify(contactService).markAsRead(1L);
    }

    @Test
    @DisplayName("TC-CON-CTRL-007: MARK READ already-read contact - idempotent (returns 200)")
    @WithMockUser(roles = "ADMIN")
    void test_markRead_alreadyRead_idempotent() throws Exception {
        // Arrange - contact already read, service does nothing
        doNothing().when(contactService).markAsRead(1L);

        // Act & Assert - should return 200 OK (idempotent)
        mockMvc.perform(patch("/api/admin/contacts/1/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(contactService).markAsRead(1L);
    }

    @Test
    @DisplayName("TC-CON-CTRL-008: MARK READ id not found - returns 200 (silent fail)")
    @WithMockUser(roles = "ADMIN")
    void test_markRead_notFound_silentFail() throws Exception {
        // Arrange - service does silent fail (no exception)
        doNothing().when(contactService).markAsRead(99999L);

        // Act & Assert - returns 200 even if ID doesn't exist (confusing behavior)
        mockMvc.perform(patch("/api/admin/contacts/99999/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(contactService).markAsRead(99999L);
    }

    @Test
    @DisplayName("TC-CON-CTRL-009: MARK ALL READ - returns 200")
    @WithMockUser(roles = "ADMIN")
    void test_markAllRead_success_returns200() throws Exception {
        // Arrange
        doNothing().when(contactService).markAllAsRead();

        // Act & Assert
        mockMvc.perform(patch("/api/admin/contacts/mark-all-read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Đã đánh dấu tất cả là đã đọc"));

        verify(contactService).markAllAsRead();
    }

    @Test
    @DisplayName("TC-CON-CTRL-010: MARK ALL READ when all already read - idempotent")
    @WithMockUser(roles = "ADMIN")
    void test_markAllRead_allAlreadyRead_idempotent() throws Exception {
        // Arrange - all contacts already read
        doNothing().when(contactService).markAllAsRead();

        // Act & Assert - should return 200 OK (idempotent)
        mockMvc.perform(patch("/api/admin/contacts/mark-all-read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(contactService).markAllAsRead();
    }

    @Test
    @DisplayName("TC-CON-CTRL-011: GET list without auth - returns 401")
    void test_getContacts_noAuth_returns401() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/admin/contacts"))
                .andExpect(status().isUnauthorized());

        verify(contactService, never()).findAll(any());
    }

    @Test
    @DisplayName("TC-CON-CTRL-012: MARK READ without auth - returns 401")
    void test_markRead_noAuth_returns401() throws Exception {
        // Act & Assert
        mockMvc.perform(patch("/api/admin/contacts/1/read"))
                .andExpect(status().isUnauthorized());

        verify(contactService, never()).markAsRead(anyLong());
    }
}
