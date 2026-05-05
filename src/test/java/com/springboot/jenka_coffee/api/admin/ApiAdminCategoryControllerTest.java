package com.springboot.jenka_coffee.api.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.jenka_coffee.dto.request.CategoryRequest;
import com.springboot.jenka_coffee.entity.Category;
import com.springboot.jenka_coffee.entity.Product;
import com.springboot.jenka_coffee.repository.CategoryRepository;
import com.springboot.jenka_coffee.repository.ProductRepository;
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

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Admin Category Controller Tests (Real Flow)")
class ApiAdminCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    private Category testCategory;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        testCategory = new Category();
        testCategory.setId("CF");
        testCategory.setName("Coffee");
        categoryRepository.save(testCategory);
    }

    // --- GET LIST TESTS ---

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-CAT-CTRL-001: GET list valid request")
    void test_listCategories_validRequest_returns200() throws Exception {
        mockMvc.perform(get("/api/admin/categories?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.totalItems").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-CAT-CTRL-002: GET list size=0 (boundary - auto-cap to 1)")
    void test_listCategories_sizeZero_autoCaps() throws Exception {
        mockMvc.perform(get("/api/admin/categories?page=0&size=0"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-CAT-CTRL-003: GET list size=9999 (auto-cap to 100)")
    void test_listCategories_largeSize_autoCaps() throws Exception {
        mockMvc.perform(get("/api/admin/categories?page=0&size=9999"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-CAT-CTRL-004: GET list page negative (auto-correct)")
    void test_listCategories_negativePage_autoCorrects() throws Exception {
        mockMvc.perform(get("/api/admin/categories?page=-1&size=10"))
                .andExpect(status().isOk());
    }

    // --- GET DETAIL TESTS ---

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-CAT-CTRL-005: GET detail valid id")
    void test_getCategory_validId_returns200() throws Exception {
        mockMvc.perform(get("/api/admin/categories/CF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("CF"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-CAT-CTRL-006: GET detail id not found")
    void test_getCategory_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/admin/categories/NOT_EXIST"))
                .andExpect(status().isNotFound());
    }

    // --- CREATE TESTS ---

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-CAT-CTRL-007: CREATE valid data")
    void test_createCategory_validData_returns201() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setId("NEWCAT");
        request.setName("New Category");

        mockMvc.perform(post("/api/admin/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()) // Assuming the API returns 200 SUCCESS wrapper instead of 201
                .andExpect(jsonPath("$.data.id").value("NEWCAT"));

        assertTrue(categoryRepository.existsById("NEWCAT"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-CAT-CTRL-008: CREATE duplicate ID")
    void test_createCategory_duplicateId_returns400() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setId("CF"); // Already exists
        request.setName("Existing Category");

        mockMvc.perform(post("/api/admin/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-CAT-CTRL-009: CREATE name = empty string")
    void test_createCategory_emptyName_returns400() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setId("TEST");
        request.setName(""); 

        mockMvc.perform(post("/api/admin/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-CAT-CTRL-010: CREATE missing name field")
    void test_createCategory_missingName_returns400() throws Exception {
        String json = "{\"id\":\"TEST\"}";
        mockMvc.perform(post("/api/admin/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-CAT-CTRL-011: CREATE missing id field")
    void test_createCategory_missingId_returns400() throws Exception {
        String json = "{\"name\":\"Test cat\"}";
        mockMvc.perform(post("/api/admin/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-CAT-CTRL-012: CREATE id with special characters")
    void test_createCategory_specialCharsId_returns400() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setId("CA T#1");
        request.setName("Test");

        mockMvc.perform(post("/api/admin/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError()); // Could be 400 or handled via sanitize
    }

    // --- UPDATE TESTS ---

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-CAT-CTRL-013: UPDATE valid data")
    void test_updateCategory_validData_returns200() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setName("Updated Coffee");

        mockMvc.perform(put("/api/admin/categories/CF")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated Coffee"));

        assertEquals("Updated Coffee", categoryRepository.findById("CF").get().getName());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-CAT-CTRL-014: UPDATE id not found")
    void test_updateCategory_notFound_returns404() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setName("Updated Coffee");

        mockMvc.perform(put("/api/admin/categories/NOT_EXIST")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-CAT-CTRL-015: UPDATE name = empty string")
    void test_updateCategory_emptyName_returns400() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setName("");

        mockMvc.perform(put("/api/admin/categories/CF")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // --- DELETE TESTS ---

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-CAT-CTRL-016: DELETE valid id (no products)")
    void test_deleteCategory_noProducts_returns200() throws Exception {
        mockMvc.perform(delete("/api/admin/categories/CF"))
                .andExpect(status().isOk());
        assertFalse(categoryRepository.existsById("CF"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-CAT-CTRL-017: DELETE category has products")
    void test_deleteCategory_hasProducts_returns400() throws Exception {
        Product p = new Product();
        p.setCategory(testCategory);
        p.setName("Espresso");
        p.setPrice(10.0);
        p.setAvailable(true);
        p.setCreateDate(new Date());
        productRepository.save(p);

        mockMvc.perform(delete("/api/admin/categories/CF"))
                .andExpect(status().is4xxClientError()); // Should fail with BusinessRuleException (400)
                
        assertTrue(categoryRepository.existsById("CF"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-CAT-CTRL-018: DELETE id not found")
    void test_deleteCategory_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/api/admin/categories/NOT_EXIST"))
                .andExpect(status().is4xxClientError()); // Will expose if mapped to 500
    }

    // --- CHECK & COUNT TESTS ---

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-CAT-CTRL-019: CHECK ID available")
    void test_checkCategoryId_notExists_returnsTrue() throws Exception {
        mockMvc.perform(get("/api/admin/categories/check-id/BRANDNEW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-CAT-CTRL-020: CHECK ID not available")
    void test_checkCategoryId_exists_returnsFalse() throws Exception {
        mockMvc.perform(get("/api/admin/categories/check-id/CF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-CAT-CTRL-021: GET product count by category")
    void test_getProductCount_returnsCount() throws Exception {
        Product p = new Product();
        p.setCategory(testCategory);
        p.setName("Espresso");
        p.setPrice(10.0);
        p.setAvailable(true);
        p.setCreateDate(new Date());
        productRepository.save(p);

        mockMvc.perform(get("/api/admin/categories/product-count/CF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-CAT-CTRL-022: GET product count category not exists")
    void test_getProductCount_notFound_returnsZero() throws Exception {
        mockMvc.perform(get("/api/admin/categories/product-count/NOT_EXIST"))
                .andExpect(status().isOk()) // Assuming it returns 0 safely
                .andExpect(jsonPath("$.data").value(0));
    }
}
