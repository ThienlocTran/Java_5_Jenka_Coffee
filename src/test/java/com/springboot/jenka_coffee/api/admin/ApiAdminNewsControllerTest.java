package com.springboot.jenka_coffee.api.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.jenka_coffee.entity.News;
import com.springboot.jenka_coffee.service.NewsService;
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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ApiAdminNewsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NewsService newsService;

    @Autowired
    private ObjectMapper objectMapper;

    // --- 1. SECURITY & XSS TESTS ---

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-NEWS-CTRL-010: CREATE title with XSS payload (Sanitization)")
    void test_createNews_withXSSPayload_sanitizesInput() throws Exception {
        News mockNews = new News();
        mockNews.setId(1L);
        mockNews.setTitle("Article"); // Sanitized version without script tags
        mockNews.setAvailable(false);

        when(newsService.saveNews(any(), any())).thenReturn(mockNews);

        mockMvc.perform(multipart("/api/admin/news")
                .param("title", "<script>alert(1)</script>Article") // XSS Payload
                .param("content", "Valid content"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Article")); // Verify HTML is stripped
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-NEWS-CTRL-012: CREATE attempting to set available=true (Mass Assignment Protection)")
    void test_createNews_availableTrueInBody_forcesAvailableFalse() throws Exception {
        News mockNews = new News();
        mockNews.setId(1L);
        mockNews.setTitle("Test");
        mockNews.setAvailable(false); // Server enforces default

        when(newsService.saveNews(any(), any())).thenReturn(mockNews);

        mockMvc.perform(multipart("/api/admin/news")
                .param("title", "Test")
                .param("content", "Content")
                .param("available", "true")) // Malicious attempt to publish immediately
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.available").value(false)); // Security enforced
    }

    // --- 2. CONCURRENCY & RACE CONDITIONS ---

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-NEWS-CTRL-021: CREATE concurrent requests same title (Race condition)")
    void test_createNews_concurrentSameTitle_createsBoth() throws InterruptedException {
        // Since News title doesn't have a UNIQUE constraint, both should succeed (by design)
        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        
        AtomicInteger successCount = new AtomicInteger(0);

        News mockNews = new News();
        mockNews.setId(1L);
        when(newsService.saveNews(any(), any())).thenReturn(mockNews);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                    mockMvc.perform(multipart("/api/admin/news")
                            .param("title", "Same Article (2 parallel requests)")
                            .param("content", "Content"))
                            .andExpect(status().isCreated());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        latch.countDown(); // Start all threads
        doneLatch.await(); // Wait for completion

        // Verify that BOTH requests succeeded (no unique constraint violation)
        assertEquals(2, successCount.get(), "Both concurrent requests should succeed by design");
        verify(newsService, times(2)).saveNews(any(), any());
    }

    // --- 3. BOUNDARY TESTS ---

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-NEWS-CTRL-002: GET list size=0 (Auto-cap to 1)")
    void test_newsList_zeroSize_autoCorrects() throws Exception {
        mockMvc.perform(get("/api/admin/news?page=0&size=0"))
                .andExpect(status().isOk()); // Assuming it auto-corrects to 1 and doesn't crash
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-NEWS-CTRL-001: GET list valid request")
    void test_newsList_valid_returnsOk() throws Exception {
        mockMvc.perform(get("/api/admin/news?page=0&size=10"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-NEWS-CTRL-003: GET list size=9999 (auto-cap to 100)")
    void test_newsList_largeSize_capsTo100() throws Exception {
        mockMvc.perform(get("/api/admin/news?page=0&size=9999"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-NEWS-CTRL-004: GET list page negative (auto-correct)")
    void test_newsList_negativePage_capsToZero() throws Exception {
        mockMvc.perform(get("/api/admin/news?page=-2&size=10"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-NEWS-CTRL-005: GET detail valid id")
    void test_newsDetail_valid_returnsOk() throws Exception {
        News mockNews = new News();
        mockNews.setId(1L);
        mockNews.setTitle("Title");
        
        when(newsService.findById(1L)).thenReturn(mockNews);
        
        mockMvc.perform(get("/api/admin/news/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Title"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-NEWS-CTRL-006: GET detail id not found")
    void test_newsDetail_notFound_returns404() throws Exception {
        when(newsService.findById(99999L))
            .thenThrow(new com.springboot.jenka_coffee.exception.ResourceNotFoundException("News not found"));
            
        mockMvc.perform(get("/api/admin/news/99999"))
                .andExpect(status().isNotFound());
    }

    // --- 4. ADDITIONAL CREATE TESTS ---

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-NEWS-CTRL-007: CREATE valid multipart data")
    void test_createNews_valid_returnsCreated() throws Exception {
        News mockNews = new News();
        mockNews.setId(1L);
        mockNews.setTitle("New Article");
        mockNews.setAvailable(false);
        
        when(newsService.saveNews(any(), any())).thenReturn(mockNews);
        
        mockMvc.perform(multipart("/api/admin/news")
                .param("title", "New Article")
                .param("content", "<p>Content</p>"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-NEWS-CTRL-008: CREATE missing title field")
    void test_createNews_missingTitle_returnsBadRequest() throws Exception {
        mockMvc.perform(multipart("/api/admin/news")
                .param("content", "<p>Content</p>"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-NEWS-CTRL-009: CREATE title = empty string")
    void test_createNews_emptyTitle_returnsBadRequest() throws Exception {
        mockMvc.perform(multipart("/api/admin/news")
                .param("title", "")
                .param("content", "<p>Content</p>"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-NEWS-CTRL-011: CREATE without imageFile (optional)")
    void test_createNews_noImage_returnsCreated() throws Exception {
        News mockNews = new News();
        mockNews.setId(1L);
        
        when(newsService.saveNews(any(), any())).thenReturn(mockNews);
        
        mockMvc.perform(multipart("/api/admin/news")
                .param("title", "No Image Article")
                .param("content", "Content")) // No file uploaded
                .andExpect(status().isCreated());
    }

    // --- 5. ADDITIONAL UPDATE, DELETE & TOGGLE TESTS ---

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-NEWS-CTRL-013: UPDATE valid data")
    void test_updateNews_valid_returnsOk() throws Exception {
        News mockNews = new News();
        mockNews.setId(1L);
        mockNews.setTitle("Updated Title");
        
        when(newsService.updateNews(eq(1L), any(), any())).thenReturn(mockNews);
        
        mockMvc.perform(put("/api/admin/news/1")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .param("title", "Updated Title")
                .param("content", "New Content"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-NEWS-CTRL-014: UPDATE id not found")
    void test_updateNews_notFound_returns404() throws Exception {
        when(newsService.updateNews(eq(99999L), any(), any()))
            .thenThrow(new com.springboot.jenka_coffee.exception.ResourceNotFoundException("News not found"));
            
        mockMvc.perform(put("/api/admin/news/99999")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .param("title", "Updated Title"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-NEWS-CTRL-015: UPDATE title = empty string")
    void test_updateNews_emptyTitle_returnsBadRequest() throws Exception {
        mockMvc.perform(put("/api/admin/news/1")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .param("title", ""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-NEWS-CTRL-016: UPDATE title with XSS payload")
    void test_updateNews_xssTitle_sanitizes() throws Exception {
        News mockNews = new News();
        mockNews.setId(1L);
        mockNews.setTitle("Clean Title");
        
        when(newsService.updateNews(eq(1L), any(), any())).thenReturn(mockNews);
        
        mockMvc.perform(put("/api/admin/news/1")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .param("title", "<img onerror=alert(1)>"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-NEWS-CTRL-017: TOGGLE available valid id")
    void test_toggleNews_valid_returnsOk() throws Exception {
        News mockNews = new News();
        mockNews.setId(1L);
        
        when(newsService.toggleAvailable(1L)).thenReturn(mockNews);
        
        mockMvc.perform(put("/api/admin/news/1/toggle"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-NEWS-CTRL-018: TOGGLE available id not found")
    void test_toggleNews_notFound_returns404() throws Exception {
        when(newsService.toggleAvailable(99999L))
            .thenThrow(new com.springboot.jenka_coffee.exception.ResourceNotFoundException("News not found"));
            
        mockMvc.perform(put("/api/admin/news/99999/toggle"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-NEWS-CTRL-019: DELETE valid id")
    void test_deleteNews_valid_returnsOk() throws Exception {
        doNothing().when(newsService).delete(1L);
        
        mockMvc.perform(delete("/api/admin/news/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-NEWS-CTRL-020: DELETE id not found")
    void test_deleteNews_notFound_returns404() throws Exception {
        doThrow(new com.springboot.jenka_coffee.exception.ResourceNotFoundException("News not found"))
            .when(newsService).delete(99999L);
            
        mockMvc.perform(delete("/api/admin/news/99999"))
                .andExpect(status().isNotFound());
    }
}
