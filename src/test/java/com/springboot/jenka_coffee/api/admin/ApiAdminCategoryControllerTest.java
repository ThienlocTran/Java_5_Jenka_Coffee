package com.springboot.jenka_coffee.api.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.jenka_coffee.dto.request.CategoryRequest;
import com.springboot.jenka_coffee.entity.Category;
import com.springboot.jenka_coffee.exception.BusinessRuleException;
import com.springboot.jenka_coffee.exception.DuplicateResourceException;
import com.springboot.jenka_coffee.exception.ResourceNotFoundException;
import com.springboot.jenka_coffee.service.CategoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Admin Category Controller Tests")
class ApiAdminCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    private Category buildMockCategory(String id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        return category;
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-CAT-CTRL-001: test_listCategories_validRequest_returns200")
    void test_listCategories_validRequest_returns200() throws Exception {
        Category cat = buildMockCategory("CF", "Coffee");
        Page<Category> page = new PageImpl<>(List.of(cat));
        
        when(categoryService.findAllPaginated(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/admin/categories")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.totalItems").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-CAT-CTRL-007: test_createCategory_validData_returns200")
    void test_createCategory_validData_returns200() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setId("NEWCAT");
        request.setName("New Category");
        
        Category responseCat = buildMockCategory("NEWCAT", "New Category");
        
        when(categoryService.existsById("NEWCAT")).thenReturn(false);
        when(categoryService.createCategory(any(CategoryRequest.class))).thenReturn(responseCat);

        mockMvc.perform(post("/api/admin/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value("NEWCAT"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-CAT-CTRL-008: test_createCategory_duplicateId_returns400")
    void test_createCategory_duplicateId_returns400() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setId("EXISTING");
        request.setName("Existing Category");
        
        when(categoryService.existsById("EXISTING")).thenReturn(true);

        mockMvc.perform(post("/api/admin/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("ID danh mục đã tồn tại"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-CAT-CTRL-009: test_createCategory_emptyName_returns400")
    void test_createCategory_emptyName_returns400() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setId("TEST");
        request.setName(""); // Invalid name length

        mockMvc.perform(post("/api/admin/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()); // Handled by @Valid
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-CAT-CTRL-017: test_deleteCategory_hasProducts_returns400")
    void test_deleteCategory_hasProducts_returns400() throws Exception {
        doThrow(new BusinessRuleException("Không thể xóa loại hàng này vì còn 5 sản phẩm thuộc loại này!"))
            .when(categoryService).deleteOrThrow("CF");

        mockMvc.perform(delete("/api/admin/categories/CF"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("Không thể xóa loại hàng này vì còn 5 sản phẩm thuộc loại này!"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-CAT-CTRL-018: test_deleteCategory_notFound_returns500")
    void test_deleteCategory_notFound_returns500() throws Exception {
        // GAP: deleteOrThrow throws ResourceNotFoundException but controller catches Exception and maps to 500
        doThrow(new ResourceNotFoundException("Category", "id", "NOT_EXIST"))
            .when(categoryService).deleteOrThrow("NOT_EXIST");

        mockMvc.perform(delete("/api/admin/categories/NOT_EXIST"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("Có lỗi xảy ra khi xóa danh mục"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-CAT-CTRL-019: test_checkCategoryId_notExists_returnsTrue")
    void test_checkCategoryId_notExists_returnsTrue() throws Exception {
        when(categoryService.existsById("BRANDNEW")).thenReturn(false);

        mockMvc.perform(get("/api/admin/categories/check-id/BRANDNEW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data").value(true));
    }
}
