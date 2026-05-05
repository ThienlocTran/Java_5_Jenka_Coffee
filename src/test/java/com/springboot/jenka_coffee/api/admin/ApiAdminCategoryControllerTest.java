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

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * CATEGORY CONTROLLER TEST CASES (Batch 02)
 * TC-CAT-CTRL-001 to TC-CAT-CTRL-022
 * 
 * Focus: Category CRUD operations, pagination, validation, product count
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiAdminCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Category testCategory;

    @BeforeEach
    void setUp() {
        // Create test category
        testCategory = new Category();
        testCategory.setId("TEST_CAT");
        testCategory.setName("Test Category");
        testCategory.setIcon("test_icon.webp");
        categoryRepository.save(testCategory);
    }

    @Test
    @DisplayName("TC-CAT-CTRL-001: Category - GET list valid request")
    @WithMockUser(roles = "ADMIN")
    void test_getCategoryList_validRequest_returns200() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/admin/categories")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.currentPage").value(0))
                .andExpect(jsonPath("$.data.totalPages").exists())
                .andExpect(jsonPath("$.data.totalItems").exists());
    }

    @Test
    @DisplayName("TC-CAT-CTRL-002: Category - GET list size=0 (boundary - auto-cap to 1)")
    @WithMockUser(roles = "ADMIN")
    void test_getCategoryList_sizeZero_autoCapTo1() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/admin/categories")
                        .param("page", "0")
                        .param("size", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    @DisplayName("TC-CAT-CTRL-003: Category - GET list size=9999 (auto-cap to 100)")
    @WithMockUser(roles = "ADMIN")
    void test_getCategoryList_sizeTooLarge_autoCapTo100() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/admin/categories")
                        .param("page", "0")
                        .param("size", "9999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items.length()").value(lessThanOrEqualTo(100)));
    }

    @Test
    @DisplayName("TC-CAT-CTRL-004: Category - GET list page negative (auto-correct)")
    @WithMockUser(roles = "ADMIN")
    void test_getCategoryList_pageNegative_autoCorrectTo0() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/admin/categories")
                        .param("page", "-1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.currentPage").value(0));
    }

    @Test
    @DisplayName("TC-CAT-CTRL-005: Category - GET detail valid id")
    @WithMockUser(roles = "ADMIN")
    void test_getCategoryDetail_validId_returns200() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/admin/categories/" + testCategory.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(testCategory.getId()))
                .andExpect(jsonPath("$.data.name").value(testCategory.getName()));
    }

    @Test
    @DisplayName("TC-CAT-CTRL-006: Category - GET detail id not found")
    @WithMockUser(roles = "ADMIN")
    void test_getCategoryDetail_idNotFound_returns404() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/admin/categories/NOT_EXIST"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("ERROR"));
    }

    @Test
    @DisplayName("TC-CAT-CTRL-007: Category - CREATE valid data")
    @WithMockUser(roles = "ADMIN")
    void test_createCategory_validData_returns200() throws Exception {
        // Arrange
        CategoryRequest request = new CategoryRequest();
        request.setId("NEWCAT");
        request.setName("Danh mục mới");

        // Act & Assert
        mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value("NEWCAT"))
                .andExpect(jsonPath("$.data.name").value("Danh mục mới"));

        // Verify in DB
        assert categoryRepository.existsById("NEWCAT");
    }

    @Test
    @DisplayName("TC-CAT-CTRL-008: Category - CREATE duplicate ID")
    @WithMockUser(roles = "ADMIN")
    void test_createCategory_duplicateId_returns400() throws Exception {
        // Arrange - Use existing category ID
        CategoryRequest request = new CategoryRequest();
        request.setId(testCategory.getId());
        request.setName("Test");

        // Act & Assert
        mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("đã tồn tại")));
    }

    @Test
    @DisplayName("TC-CAT-CTRL-009: Category - CREATE name = empty string (boundary)")
    @WithMockUser(roles = "ADMIN")
    void test_createCategory_emptyName_returns400() throws Exception {
        // Arrange
        CategoryRequest request = new CategoryRequest();
        request.setId("TEST");
        request.setName("");

        // Act & Assert
        mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC-CAT-CTRL-010: Category - CREATE missing name field")
    @WithMockUser(roles = "ADMIN")
    void test_createCategory_missingName_returns400() throws Exception {
        // Arrange - Only id, no name
        String json = "{\"id\": \"TEST\"}";

        // Act & Assert
        mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC-CAT-CTRL-011: Category - CREATE missing id field")
    @WithMockUser(roles = "ADMIN")
    void test_createCategory_missingId_returns400() throws Exception {
        // Arrange - Only name, no id
        String json = "{\"name\": \"Test cat\"}";

        // Act & Assert
        mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC-CAT-CTRL-012: [SECURITY GAP] Category - CREATE id with special characters - should reject")
    @WithMockUser(roles = "ADMIN")
    void test_createCategory_idWithSpecialChars_shouldReject() throws Exception {
        // Arrange
        CategoryRequest request = new CategoryRequest();
        request.setId("CA T#1");  // space + # character - INVALID
        request.setName("Test");

        // SECURITY GAP DOCUMENTATION:
        // ID with spaces/# can cause URL encoding issues and potential SQL injection
        // EXPECTED: reject with 400 Bad Request
        // CURRENT: accepts with 200 OK (no pattern validation)
        // This test will FAIL until validation is added to CategoryRequest
        // Fix: Add @Pattern(regexp = "^[A-Z0-9_]+$") to CategoryRequest.id field
        
        // Act & Assert - Should reject special characters for security
        mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())  // ✅ Target: reject invalid ID
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("ID không hợp lệ")));
        
        // NOTE: If this test fails with 200 OK → validation gap exists
    }

    @Test
    @DisplayName("TC-CAT-CTRL-013: Category - UPDATE valid data")
    @WithMockUser(roles = "ADMIN")
    void test_updateCategory_validData_returns200() throws Exception {
        // Arrange
        CategoryRequest request = new CategoryRequest();
        request.setName("Cà phê đặc biệt");

        // Act & Assert
        mockMvc.perform(put("/api/admin/categories/" + testCategory.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.name").value("Cà phê đặc biệt"));

        // Verify in DB
        Category updated = categoryRepository.findById(testCategory.getId()).orElseThrow();
        assert updated.getName().equals("Cà phê đặc biệt");
    }

    @Test
    @DisplayName("TC-CAT-CTRL-014: Category - UPDATE id not found")
    @WithMockUser(roles = "ADMIN")
    void test_updateCategory_idNotFound_returns404() throws Exception {
        // Arrange
        CategoryRequest request = new CategoryRequest();
        request.setName("Test");

        // Act & Assert
        mockMvc.perform(put("/api/admin/categories/INVALID_CAT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("ERROR"));
    }

    @Test
    @DisplayName("TC-CAT-CTRL-015: Category - UPDATE name = empty string (boundary)")
    @WithMockUser(roles = "ADMIN")
    void test_updateCategory_emptyName_returns400() throws Exception {
        // Arrange
        CategoryRequest request = new CategoryRequest();
        request.setName("");

        // Act & Assert
        mockMvc.perform(put("/api/admin/categories/" + testCategory.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC-CAT-CTRL-016: Category - DELETE valid id (no products)")
    @WithMockUser(roles = "ADMIN")
    void test_deleteCategory_noProducts_returns200() throws Exception {
        // Arrange - Create category without products
        Category emptyCategory = new Category();
        emptyCategory.setId("EMPTY_CAT");
        emptyCategory.setName("Empty Category");
        categoryRepository.save(emptyCategory);

        // Act & Assert
        mockMvc.perform(delete("/api/admin/categories/EMPTY_CAT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        // Verify deleted from DB
        assert !categoryRepository.existsById("EMPTY_CAT");
    }

    @Test
    @DisplayName("TC-CAT-CTRL-017: Category - DELETE category has products")
    @WithMockUser(roles = "ADMIN")
    void test_deleteCategory_hasProducts_returns400() throws Exception {
        // Arrange - Create product in test category
        Product product = new Product();
        product.setName("Test Product");
        product.setPrice(new BigDecimal("100000"));
        product.setCategory(testCategory);
        product.setAvailable(true);
        productRepository.save(product);

        // Act & Assert
        mockMvc.perform(delete("/api/admin/categories/" + testCategory.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("còn")));
    }

    @Test
    @DisplayName("TC-CAT-CTRL-018: Category - DELETE id not found")
    @WithMockUser(roles = "ADMIN")
    void test_deleteCategory_idNotFound_returns404() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/admin/categories/NOT_EXIST"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("ERROR"));
    }

    @Test
    @DisplayName("TC-CAT-CTRL-019: Category - CHECK ID available (id chưa tồn tại)")
    @WithMockUser(roles = "ADMIN")
    void test_checkCategoryId_available_returnsTrue() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/admin/categories/check-id/BRANDNEW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("TC-CAT-CTRL-020: Category - CHECK ID not available (id đã tồn tại)")
    @WithMockUser(roles = "ADMIN")
    void test_checkCategoryId_notAvailable_returnsFalse() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/admin/categories/check-id/" + testCategory.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data").value(false));
    }

    @Test
    @DisplayName("TC-CAT-CTRL-021: Category - GET product count by category")
    @WithMockUser(roles = "ADMIN")
    void test_getProductCount_categoryWithProducts_returnsCount() throws Exception {
        // Arrange - Create products in test category
        Product product1 = new Product();
        product1.setName("Product 1");
        product1.setPrice(new BigDecimal("100000"));
        product1.setCategory(testCategory);
        product1.setAvailable(true);
        productRepository.save(product1);

        Product product2 = new Product();
        product2.setName("Product 2");
        product2.setPrice(new BigDecimal("200000"));
        product2.setCategory(testCategory);
        product2.setAvailable(true);
        productRepository.save(product2);

        // Act & Assert
        mockMvc.perform(get("/api/admin/categories/product-count/" + testCategory.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data").value(greaterThanOrEqualTo(2)));
    }

    @Test
    @DisplayName("TC-CAT-CTRL-022: Category - GET product count category not exists")
    @WithMockUser(roles = "ADMIN")
    void test_getProductCount_categoryNotExists_returnsZero() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/admin/categories/product-count/NOT_EXIST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data").value(0));
    }
}
