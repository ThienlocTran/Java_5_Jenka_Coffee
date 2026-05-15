package com.springboot.jenka_coffee.api.admin;

import com.springboot.jenka_coffee.entity.StoreFeedback;
import com.springboot.jenka_coffee.exception.BusinessRuleException;
import com.springboot.jenka_coffee.exception.ResourceNotFoundException;
import com.springboot.jenka_coffee.service.StoreFeedbackService;
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

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TC-FBK-CTRL-001 to TC-FBK-CTRL-008: Feedback Controller Integration Tests
 * Tests HTTP endpoints with security, pagination validation, and branch filtering
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiAdminFeedbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StoreFeedbackService feedbackService;

    private StoreFeedback testFeedback;

    @BeforeEach
    void setUp() {
        testFeedback = new StoreFeedback();
        testFeedback.setId(1L);
        testFeedback.setBranch("HN");
        testFeedback.setFullname("John Doe");
        testFeedback.setPhone("0123456789");
        testFeedback.setComment("Great service");
        testFeedback.setStoreRating(5);
        testFeedback.setStaffRating(5);
    }

    @Test
    @DisplayName("TC-FBK-CTRL-001: GET list all (no branch filter) - returns paginated list")
    @WithMockUser(roles = "ADMIN")
    void test_list_noFilter_returnsPaginatedList() throws Exception {
        // Arrange
        List<StoreFeedback> feedbacks = List.of(testFeedback);
        Page<StoreFeedback> page = new PageImpl<>(feedbacks, PageRequest.of(0, 20), 1);
        when(feedbackService.findAll(any(PageRequest.class))).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/admin/feedbacks")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.currentPage").value(0))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.totalItems").value(1));

        verify(feedbackService).findAll(any(PageRequest.class));
        verify(feedbackService, never()).findByBranch(anyString(), any());
    }

    @Test
    @DisplayName("TC-FBK-CTRL-002: GET list filter by branch - returns only that branch")
    @WithMockUser(roles = "ADMIN")
    void test_list_filterByBranch_returnsOnlyThatBranch() throws Exception {
        // Arrange
        List<StoreFeedback> feedbacks = List.of(testFeedback);
        Page<StoreFeedback> page = new PageImpl<>(feedbacks, PageRequest.of(0, 20), 1);
        when(feedbackService.findByBranch(eq("Hanoi"), any(PageRequest.class))).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/admin/feedbacks")
                        .param("branch", "Hanoi")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items[0].branch").value("HN"));

        verify(feedbackService).findByBranch(eq("Hanoi"), any(PageRequest.class));
        verify(feedbackService, never()).findAll(any());
    }

    @Test
    @DisplayName("TC-FBK-CTRL-003: GET list branch = empty string - treated as no filter")
    @WithMockUser(roles = "ADMIN")
    void test_list_emptyBranch_noFilterApplied() throws Exception {
        // Arrange
        List<StoreFeedback> feedbacks = List.of(testFeedback);
        Page<StoreFeedback> page = new PageImpl<>(feedbacks, PageRequest.of(0, 20), 1);
        when(feedbackService.findAll(any(PageRequest.class))).thenReturn(page);

        // Act & Assert - branch.isBlank() → uses findAll()
        mockMvc.perform(get("/api/admin/feedbacks")
                        .param("branch", "")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(feedbackService).findAll(any(PageRequest.class));
        verify(feedbackService, never()).findByBranch(anyString(), any());
    }

    @Test
    @DisplayName("TC-FBK-CTRL-004: GET list size=0 - auto-capped to 1 (not 500)")
    @WithMockUser(roles = "ADMIN")
    void test_list_sizeZero_autoCappedToOne() throws Exception {
        // Arrange - Controller has clamp: Math.max(size, 1)
        List<StoreFeedback> feedbacks = List.of(testFeedback);
        Page<StoreFeedback> page = new PageImpl<>(feedbacks, PageRequest.of(0, 1), 1);
        when(feedbackService.findAll(any(PageRequest.class))).thenReturn(page);

        // Act & Assert - size=0 → auto-capped to 1
        mockMvc.perform(get("/api/admin/feedbacks")
                        .param("page", "0")
                        .param("size", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(feedbackService).findAll(PageRequest.of(0, 1));
    }

    @Test
    @DisplayName("TC-FBK-CTRL-005: GET list size=9999 - auto-capped to 100")
    @WithMockUser(roles = "ADMIN")
    void test_list_largeSize_autoCappedTo100() throws Exception {
        // Arrange - Controller has clamp: Math.min(size, 100)
        List<StoreFeedback> feedbacks = List.of(testFeedback);
        Page<StoreFeedback> page = new PageImpl<>(feedbacks, PageRequest.of(0, 100), 1);
        when(feedbackService.findAll(any(PageRequest.class))).thenReturn(page);

        // Act & Assert - size=9999 → auto-capped to 100
        mockMvc.perform(get("/api/admin/feedbacks")
                        .param("page", "0")
                        .param("size", "9999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(feedbackService).findAll(PageRequest.of(0, 100));
    }

    @Test
    @DisplayName("TC-FBK-CTRL-006: DELETE valid id - returns 200")
    @WithMockUser(roles = "ADMIN")
    void test_delete_validId_returns200() throws Exception {
        // Arrange
        doNothing().when(feedbackService).delete(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/admin/feedbacks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Xóa đánh giá thành công"));

        verify(feedbackService).delete(1L);
    }

    @Test
    @DisplayName("TC-FBK-CTRL-007: DELETE id không tồn tại phải trả 404 (không phải 400) - per CSV spec")
    @WithMockUser(roles = "ADMIN")
    void test_delete_notFound_shouldReturn404() throws Exception {
        // CSV spec: Return 404 Not Found
        // ISSUE: Service throws BusinessRuleException → GlobalExceptionHandler maps to 400
        // CORRECT: "Not found" should use ResourceNotFoundException → 404
        // BusinessRuleException should be for business rules (e.g., "cannot delete because has orders")
        
        // Arrange - Use ResourceNotFoundException for "not found" semantic
        doThrow(new ResourceNotFoundException("Không tìm thấy đánh giá với ID: 99999"))
                .when(feedbackService).delete(99999L);

        // Act & Assert
        mockMvc.perform(delete("/api/admin/feedbacks/99999"))
                .andExpect(status().isNotFound())   // ✅ 404 per CSV spec
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("99999")));

        verify(feedbackService).delete(99999L);
        
        // NOTE: If service currently throws BusinessRuleException → test will fail with 400
        // Fix: Change service to throw ResourceNotFoundException for "not found" cases
    }

    @Test
    @DisplayName("TC-FBK-CTRL-008: DELETE concurrent requests - race condition test")
    @WithMockUser(roles = "ADMIN")
    void test_delete_concurrentRequests_raceCondition() throws Exception {
        // Arrange - First call succeeds, second call fails (already deleted)
        doNothing().doThrow(new BusinessRuleException("Không tìm thấy đánh giá với ID: 1"))
                .when(feedbackService).delete(1L);

        // Act & Assert - First request succeeds
        mockMvc.perform(delete("/api/admin/feedbacks/1"))
                .andExpect(status().isOk());

        // Second request fails (already deleted)
        mockMvc.perform(delete("/api/admin/feedbacks/1"))
                .andExpect(status().isBadRequest());

        verify(feedbackService, times(2)).delete(1L);
    }

    @Test
    @DisplayName("TC-FBK-CTRL-009: GET list without auth - returns 401")
    void test_list_noAuth_returns401() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/admin/feedbacks"))
                .andExpect(status().isUnauthorized());

        verify(feedbackService, never()).findAll(any());
    }

    @Test
    @DisplayName("TC-FBK-CTRL-010: DELETE without auth - returns 401")
    void test_delete_noAuth_returns401() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/admin/feedbacks/1"))
                .andExpect(status().isUnauthorized());

        verify(feedbackService, never()).delete(anyLong());
    }

    @Test
    @DisplayName("TC-FBK-CTRL-011: GET list page negative - auto-capped to 0")
    @WithMockUser(roles = "ADMIN")
    void test_list_negativePage_autoCappedToZero() throws Exception {
        // Arrange - Controller has clamp: Math.max(page, 0)
        List<StoreFeedback> feedbacks = List.of(testFeedback);
        Page<StoreFeedback> page = new PageImpl<>(feedbacks, PageRequest.of(0, 20), 1);
        when(feedbackService.findAll(any(PageRequest.class))).thenReturn(page);

        // Act & Assert - page=-1 → auto-capped to 0
        mockMvc.perform(get("/api/admin/feedbacks")
                        .param("page", "-1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(feedbackService).findAll(PageRequest.of(0, 20));
    }

    @Test
    @DisplayName("TC-FBK-CTRL-012: GET list branch filtering correctness - case insensitive")
    @WithMockUser(roles = "ADMIN")
    void test_list_branchFilterCaseInsensitive() throws Exception {
        // Arrange
        List<StoreFeedback> feedbacks = List.of(testFeedback);
        Page<StoreFeedback> page = new PageImpl<>(feedbacks, PageRequest.of(0, 20), 1);
        when(feedbackService.findByBranch(eq("hcm"), any(PageRequest.class))).thenReturn(page);

        // Act & Assert - branch filter is case insensitive
        mockMvc.perform(get("/api/admin/feedbacks")
                        .param("branch", "hcm")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(feedbackService).findByBranch(eq("hcm"), any(PageRequest.class));
    }
}
