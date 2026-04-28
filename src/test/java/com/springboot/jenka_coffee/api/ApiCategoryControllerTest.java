package com.springboot.jenka_coffee.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.jenka_coffee.entity.Category;
import com.springboot.jenka_coffee.service.CategoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test Case Document Part 2: CATEGORY MODULE - Controller Layer
 * Tests for ApiCategoryController endpoints
 * 
 * Coverage:
 * - TC-CAT-001: GET /api/categories — trả danh sách đầy đủ
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)  // ✅ Disable RateLimitFilter for tests
@DisplayName("API Category Controller Tests")
class ApiCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    // ========== HELPER METHODS ==========

    private Category buildCategory(String id, String name, String icon) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setIcon(icon);
        return category;
    }

    // ========== TEST CASES ==========

    /**
     * TC-CAT-001 — GET /api/categories — trả danh sách đầy đủ
     * Expected: HTTP 200, status="SUCCESS", data là array 6 phần tử
     */
    @Test
    @DisplayName("TC-CAT-001: GET /api/categories phải trả về tất cả categories với status 200")
    void getAllCategories_returns200WithList() throws Exception {
        // Arrange
        List<Category> categories = List.of(
                buildCategory("MAY_PHA", "Máy Pha Cà Phê", "May_Pha_Ca_Phe.webp"),
                buildCategory("MAY_XAY", "Máy Xay Cà Phê", "May_Xay_Ca_Phe.webp"),
                buildCategory("CA_PHE_DO_AN", "Cà Phê & Đồ Ăn", "ca_phe_do_an.webp"),
                buildCategory("DUNG_CU_PHA_CHE", "Dụng Cụ Pha Chế", "dung_cu_pha_che.webp"),
                buildCategory("MAY_PHA_MAY_XAY_CU", "Máy Pha & Máy Xay Cũ", "may_pha_may_xay_cu.webp"),
                buildCategory("MAY_XAY_SINH_TO_MAY_EP", "Máy Xay Sinh Tố & Máy Ép", "may_xay_sinh_to_may_ep.webp")
        );
        
        when(categoryService.findAll()).thenReturn(categories);

        // Act & Assert
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(6))
                .andExpect(jsonPath("$.data[0].id").value("MAY_PHA"))
                .andExpect(jsonPath("$.data[0].name").value("Máy Pha Cà Phê"))
                .andExpect(jsonPath("$.data[0].icon").value("May_Pha_Ca_Phe.webp"))
                .andExpect(jsonPath("$.data[1].id").value("MAY_XAY"));

        verify(categoryService).findAll();
    }
}
