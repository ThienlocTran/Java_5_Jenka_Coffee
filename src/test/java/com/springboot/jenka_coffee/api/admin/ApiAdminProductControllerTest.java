package com.springboot.jenka_coffee.api.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.jenka_coffee.dto.request.ProductRequest;
import com.springboot.jenka_coffee.entity.Category;
import com.springboot.jenka_coffee.entity.Product;
import com.springboot.jenka_coffee.entity.ProductImage;
import com.springboot.jenka_coffee.exception.BusinessRuleException;
import com.springboot.jenka_coffee.exception.ResourceNotFoundException;
import com.springboot.jenka_coffee.service.ProductService;
import com.springboot.jenka_coffee.validator.ProductValidator;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test Case Document: PRODUCT MODULE - ADMIN CONTROLLER
 * Tests for ApiAdminProductController endpoints
 * 
 * Coverage from TC_BATCH_01_AUTH_PRODUCT_v2.csv:
 * - TC-PRD-CTRL-001 to TC-PRD-CTRL-038: Controller tests (38 test cases)
 * 
 * ✅ STRATEGY:
 * - Use @SpringBootTest with @AutoConfigureMockMvc
 * - Use @WithMockUser(roles = "ADMIN") for authenticated admin tests
 * - Mock ProductService and ProductValidator
 * - Test real HTTP behavior, not just service logic
 * - Verify status codes, response structure, and error messages
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("API Admin Product Controller Tests")
class ApiAdminProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @MockBean
    private ProductValidator productValidator;

    @MockBean
    private com.springboot.jenka_coffee.config.RateLimitFilter rateLimitFilter;

    // ========== HELPER METHODS ==========

    private Product createMockProduct(Integer id, String name, BigDecimal price) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setPrice(price);
        product.setAvailable(true);
        product.setFeatured(false);
        
        Category category = new Category();
        category.setId("CF");
        category.setName("Cà phê");
        product.setCategory(category);
        
        return product;
    }

    // ========== TC-PRD-CTRL-001: GET LIST VALID REQUEST ==========
    
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-PRD-CTRL-001: Product - GET list valid request - Return 200 OK with paginated list")
    void TC_PRD_CTRL_001() throws Exception {
        // Arrange
        List<Product> products = List.of(
            createMockProduct(1, "Cà phê sữa", new BigDecimal("35000")),
            createMockProduct(2, "Cà phê đen", new BigDecimal("30000"))
        );
        Page<Product> productPage = new PageImpl<>(products);
        
        when(productService.findAllPaginated(any(Pageable.class))).thenReturn(productPage);

        // Act & Assert
        mockMvc.perform(get("/api/admin/products")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.currentPage").exists())
                .andExpect(jsonPath("$.data.totalPages").exists())
                .andExpect(jsonPath("$.data.totalItems").exists());
        
        verify(productService).findAllPaginated(any(Pageable.class));
    }

    // ========== TC-PRD-CTRL-002: GET LIST PAGE NEGATIVE ==========
    
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-PRD-CTRL-002: Product - GET list page negative (auto-correct) - Return 200 OK")
    void TC_PRD_CTRL_002() throws Exception {
        // Arrange
        Page<Product> productPage = new PageImpl<>(Collections.emptyList());
        when(productService.findAllPaginated(any(Pageable.class))).thenReturn(productPage);

        // Act & Assert - page=-5 should be auto-corrected to 0
        mockMvc.perform(get("/api/admin/products")
                .param("page", "-5")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.currentPage").value(0));
    }

    // ========== TC-PRD-CTRL-003: GET LIST SIZE=0 ==========
    
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-PRD-CTRL-003: Product - GET list size=0 (auto-cap to 1) - Return 200 OK")
    void TC_PRD_CTRL_003() throws Exception {
        // Arrange
        Page<Product> productPage = new PageImpl<>(Collections.emptyList());
        when(productService.findAllPaginated(any(Pageable.class))).thenReturn(productPage);

        // Act & Assert - size=0 should be auto-capped to 1
        mockMvc.perform(get("/api/admin/products")
                .param("page", "0")
                .param("size", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    // ========== TC-PRD-CTRL-004: GET LIST SIZE EXCEEDS MAX ==========
    
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-PRD-CTRL-004: Product - GET list size exceeds max 9999 (auto-cap to 100) - Return 200 OK")
    void TC_PRD_CTRL_004() throws Exception {
        // Arrange
        Page<Product> productPage = new PageImpl<>(Collections.emptyList());
        when(productService.findAllPaginated(any(Pageable.class))).thenReturn(productPage);

        // Act & Assert - size=9999 should be auto-capped to 100
        mockMvc.perform(get("/api/admin/products")
                .param("page", "0")
                .param("size", "9999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    // ========== TC-PRD-CTRL-005: GET LIST PAGE VERY LARGE WITH EMPTY DB ==========
    
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-PRD-CTRL-005: Product - GET list page very large with empty DB - Return 200 OK with empty items")
    void TC_PRD_CTRL_005() throws Exception {
        // Arrange
        Page<Product> productPage = new PageImpl<>(Collections.emptyList());
        when(productService.findAllPaginated(any(Pageable.class))).thenReturn(productPage);

        // Act & Assert
        mockMvc.perform(get("/api/admin/products")
                .param("page", "999")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.data.totalPages").value(0))
                .andExpect(jsonPath("$.data.totalItems").value(0));
    }

    // ========== TC-PRD-CTRL-006: GET DETAIL VALID ID ==========
    
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-PRD-CTRL-006: Product - GET detail valid id - Return 200 OK with product object")
    void TC_PRD_CTRL_006() throws Exception {
        // Arrange
        Product product = createMockProduct(1, "Cà phê sữa", new BigDecimal("35000"));
        when(productService.findById(1)).thenReturn(product);

        // Act & Assert
        mockMvc.perform(get("/api/admin/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Cà phê sữa"))
                .andExpect(jsonPath("$.data.price").value(35000))
                .andExpect(jsonPath("$.data.available").value(true))
                .andExpect(jsonPath("$.data.featured").value(false));
        
        verify(productService).findById(1);
    }

    // ========== TC-PRD-CTRL-007: GET DETAIL ID NOT FOUND ==========
    
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-PRD-CTRL-007: Product - GET detail id not found - Return 404 Not Found")
    void TC_PRD_CTRL_007() throws Exception {
        // Arrange
        when(productService.findById(99999))
                .thenThrow(new ResourceNotFoundException("Product not found with id: 99999"));

        // Act & Assert
        mockMvc.perform(get("/api/admin/products/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").exists());
        
        verify(productService).findById(99999);
    }

    // ========== TC-PRD-CTRL-008: CREATE VALID MULTIPART DATA ==========
    
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-PRD-CTRL-008: Product - CREATE valid multipart data - Return 201 Created")
    void TC_PRD_CTRL_008() throws Exception {
        // Arrange
        Product savedProduct = createMockProduct(1, "Cà phê sữa", new BigDecimal("35000"));
        MockMultipartFile imageFile = new MockMultipartFile(
            "imageFile", "test.jpg", "image/jpeg", "test image content".getBytes()
        );
        
        doNothing().when(productValidator).validateImageFile(any());
        when(productService.createProductFromRequest(any(), eq("CF"), any()))
                .thenReturn(savedProduct);

        // Act & Assert
        mockMvc.perform(multipart("/api/admin/products")
                .file(imageFile)
                .param("name", "Cà phê sữa")
                .param("price", "35000")
                .param("categoryId", "CF")
                .param("available", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Cà phê sữa"));
        
        verify(productValidator).validateImageFile(any());
        verify(productService).createProductFromRequest(any(), eq("CF"), any());
    }

    // ========== TC-PRD-CTRL-009: CREATE MISSING NAME FIELD ==========
    
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-PRD-CTRL-009: Product - CREATE missing name field - Return 400 Bad Request")
    void TC_PRD_CTRL_009() throws Exception {
        // Arrange
        doThrow(new BusinessRuleException("Tên sản phẩm không được để trống"))
                .when(productService).createProductFromRequest(any(), eq("CF"), any());

        // Act & Assert
        mockMvc.perform(multipart("/api/admin/products")
                .param("price", "35000")
                .param("categoryId", "CF"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"));
    }

    // ========== TC-PRD-CTRL-010: CREATE NAME EMPTY STRING ==========
    
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-PRD-CTRL-010: Product - CREATE name = empty string - Return 400 Bad Request")
    void TC_PRD_CTRL_010() throws Exception {
        // Arrange
        doThrow(new BusinessRuleException("Tên sản phẩm không được để trống"))
                .when(productService).createProductFromRequest(any(), eq("CF"), any());

        // Act & Assert
        mockMvc.perform(multipart("/api/admin/products")
                .param("name", "")
                .param("price", "35000")
                .param("categoryId", "CF"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"));
    }
}

    // ========== TC-PRD-CTRL-012: CREATE PRICE < 0 ==========
    
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-PRD-CTRL-012: Product - CREATE price < 0 (negative price) - Return 400 Bad Request")
    void TC_PRD_CTRL_012() throws Exception {
        // Arrange
        doThrow(new BusinessRuleException("Giá sản phẩm không thể âm"))
                .when(productService).createProductFromRequest(any(), eq("CF"), any());

        // Act & Assert
        mockMvc.perform(multipart("/api/admin/products")
                .param("name", "Test Product")
                .param("price", "-1000")
                .param("categoryId", "CF"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("Giá sản phẩm không thể âm"));
    }

    // ========== TC-PRD-CTRL-014: CREATE CATEGORYID NOT EXISTS ==========
    
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-PRD-CTRL-014: Product - CREATE categoryId not exists - Return 400 Bad Request")
    void TC_PRD_CTRL_014() throws Exception {
        // Arrange
        doThrow(new BusinessRuleException("Không tìm thấy danh mục với ID: INVALID"))
                .when(productService).createProductFromRequest(any(), eq("INVALID"), any());

        // Act & Assert
        mockMvc.perform(multipart("/api/admin/products")
                .param("name", "Test Product")
                .param("price", "35000")
                .param("categoryId", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"));
    }

    // ========== TC-PRD-CTRL-015: CREATE IMAGEFILE INVALID MIME TYPE ==========
    
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-PRD-CTRL-015: Product - CREATE imageFile invalid MIME type - Return 400 Bad Request")
    void TC_PRD_CTRL_015() throws Exception {
        // Arrange
        MockMultipartFile pdfFile = new MockMultipartFile(
            "imageFile", "document.pdf", "application/pdf", "pdf content".getBytes()
        );
        
        doThrow(new BusinessRuleException("Chỉ chấp nhận file ảnh định dạng: JPG, PNG, WEBP"))
                .when(productValidator).validateImageFile(any());

        // Act & Assert
        mockMvc.perform(multipart("/api/admin/products")
                .file(pdfFile)
                .param("name", "Test Product")
                .param("price", "35000")
                .param("categoryId", "CF"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"));
        
        verify(productValidator).validateImageFile(any());
    }

    // ========== TC-PRD-CTRL-016: CREATE IMAGEFILE TOO LARGE ==========
    
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-PRD-CTRL-016: Product - CREATE imageFile too large (>5MB) - Return 400 Bad Request")
    void TC_PRD_CTRL_016() throws Exception {
        // Arrange
        byte[] largeContent = new byte[6 * 1024 * 1024]; // 6MB
        MockMultipartFile largeFile = new MockMultipartFile(
            "imageFile", "large.jpg", "image/jpeg", largeContent
        );
        
        doThrow(new BusinessRuleException("Kích thước file không được vượt quá 5MB"))
                .when(productValidator).validateImageFile(any());

        // Act & Assert
        mockMvc.perform(multipart("/api/admin/products")
                .file(largeFile)
                .param("name", "Test Product")
                .param("price", "35000")
                .param("categoryId", "CF"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"));
    }

    // ========== TC-PRD-CTRL-017: CREATE WITHOUT IMAGEFILE ==========
    
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-PRD-CTRL-017: Product - CREATE without imageFile (optional field) - Return 201 Created")
    void TC_PRD_CTRL_017() throws Exception {
        // Arrange
        Product savedProduct = createMockProduct(1, "Test Product", new BigDecimal("35000"));
        
        doNothing().when(productValidator).validateImageFile(null);
        when(productService.createProductFromRequest(any(), eq("CF"), isNull()))
                .thenReturn(savedProduct);

        // Act & Assert
        mockMvc.perform(multipart("/api/admin/products")
                .param("name", "Test Product")
                .param("price", "35000")
                .param("categoryId", "CF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    // ========== TC-PRD-CTRL-018: UPDATE VALID DATA VIA PUT ==========
    
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-PRD-CTRL-018: Product - UPDATE valid data via PUT - Return 200 OK with updated product")
    void TC_PRD_CTRL_018() throws Exception {
        // Arrange
        Product updatedProduct = createMockProduct(1, "Updated Name", new BigDecimal("45000"));
        
        doNothing().when(productValidator).validateImageFile(any());
        when(productService.updateProductFromRequest(eq(1), eq("Updated Name"), any(), 
                eq(new BigDecimal("45000")), eq("CF"), eq(true), any()))
                .thenReturn(updatedProduct);

        // Act & Assert
        mockMvc.perform(multipart("/api/admin/products/1")
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                })
                .param("name", "Updated Name")
                .param("price", "45000")
                .param("categoryId", "CF")
                .param("available", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.name").value("Updated Name"))
                .andExpect(jsonPath("$.data.price").value(45000));
    }

    // ========== TC-PRD-CTRL-019: UPDATE ID NOT FOUND ==========
    
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-PRD-CTRL-019: Product - UPDATE id not found - Return 404 Not Found")
    void TC_PRD_CTRL_019() throws Exception {
        // Arrange
        doThrow(new ResourceNotFoundException("Product not found with id: 99999"))
                .when(productService).updateProductFromRequest(eq(99999), any(), any(), any(), any(), any(), any());

        // Act & Assert
        mockMvc.perform(multipart("/api/admin/products/99999")
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                })
                .param("name", "Test")
                .param("price", "35000")
                .param("categoryId", "CF")
                .param("available", "true"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("ERROR"));
    }

    // ========== TC-PRD-CTRL-020: UPDATE NAME EMPTY STRING ==========
    
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-PRD-CTRL-020: Product - UPDATE name = empty string - Return 400 Bad Request")
    void TC_PRD_CTRL_020() throws Exception {
        // Arrange
        doThrow(new BusinessRuleException("Tên sản phẩm không được để trống"))
                .when(productService).updateProductFromRequest(eq(1), eq(""), any(), any(), any(), any(), any());

        // Act & Assert
        mockMvc.perform(multipart("/api/admin/products/1")
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                })
                .param("name", "")
                .param("price", "35000")
                .param("categoryId", "CF")
                .param("available", "true"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"));
    }

    // ========== TC-PRD-CTRL-021: UPDATE PRICE < 0 ==========
    
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-PRD-CTRL-021: Product - UPDATE price < 0 - Return 400 Bad Request")
    void TC_PRD_CTRL_021() throws Exception {
        // Arrange
        doThrow(new BusinessRuleException("Giá sản phẩm không thể âm"))
                .when(productService).updateProductFromRequest(eq(1), any(), any(), 
                        eq(new BigDecimal("-500")), any(), any(), any());

        // Act & Assert
        mockMvc.perform(multipart("/api/admin/products/1")
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                })
                .param("name", "Test")
                .param("price", "-500")
                .param("categoryId", "CF")
                .param("available", "true"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"));
    }

    // ========== TC-PRD-CTRL-022: DELETE VALID ID ==========
    
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-PRD-CTRL-022: Product - DELETE valid id - Return 200 OK with success message")
    void TC_PRD_CTRL_022() throws Exception {
        // Arrange
        doNothing().when(productService).deleteProductWithValidation(1);

        // Act & Assert
        mockMvc.perform(delete("/api/admin/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Xóa sản phẩm thành công"));
        
        verify(productService).deleteProductWithValidation(1);
    }

    // ========== TC-PRD-CTRL-023: DELETE ID NOT FOUND ==========
    
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-PRD-CTRL-023: Product - DELETE id not found - Return 404 Not Found")
    void TC_PRD_CTRL_023() throws Exception {
        // Arrange
        doThrow(new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: 99999"))
                .when(productService).deleteProductWithValidation(99999);

        // Act & Assert
        mockMvc.perform(delete("/api/admin/products/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("ERROR"));
        
        verify(productService).deleteProductWithValidation(99999);
    }

    // ========== TC-PRD-CTRL-024: DELETE PRODUCT WITH ACTIVE ORDERS ==========
    
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-PRD-CTRL-024: Product - DELETE product that has active orders - Return 400 Bad Request")
    void TC_PRD_CTRL_024() throws Exception {
        // Arrange
        doThrow(new BusinessRuleException("Không thể xóa sản phẩm này vì đã có đơn hàng sử dụng"))
                .when(productService).deleteProductWithValidation(1);

        // Act & Assert
        mockMvc.perform(delete("/api/admin/products/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").exists());
        
        verify(productService).deleteProductWithValidation(1);
    }

    // ========== TC-PRD-CTRL-025: TOGGLE AVAILABLE VALID ID ==========
    
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-PRD-CTRL-025: Product - TOGGLE available valid id - Return 200 OK")
    void TC_PRD_CTRL_025() throws Exception {
        // Arrange
        doNothing().when(productService).toggleAvailable(1);

        // Act & Assert
        mockMvc.perform(put("/api/admin/products/1/toggle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Đổi trạng thái sản phẩm thành công"));
        
        verify(productService).toggleAvailable(1);
    }

    // ========== TC-PRD-CTRL-026: TOGGLE AVAILABLE ID NOT FOUND ==========
    
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-PRD-CTRL-026: Product - TOGGLE available id not found - Return 404 Not Found")
    void TC_PRD_CTRL_026() throws Exception {
        // Arrange
        doThrow(new ResourceNotFoundException("Product not found with id: 99999"))
                .when(productService).toggleAvailable(99999);

        // Act & Assert
        mockMvc.perform(put("/api/admin/products/99999/toggle"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("ERROR"));
        
        verify(productService).toggleAvailable(99999);
    }

    // ========== TC-PRD-CTRL-027: TOGGLE FEATURED VALID ID ==========
    
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-PRD-CTRL-027: Product - TOGGLE featured valid id - Return 200 OK with updated product")
    void TC_PRD_CTRL_027() throws Exception {
        // Arrange
        Product product = createMockProduct(1, "Test Product", new BigDecimal("35000"));
        product.setFeatured(true);
        
        when(productService.toggleFeatured(1)).thenReturn(product);

        // Act & Assert
        mockMvc.perform(put("/api/admin/products/1/toggle-featured"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.featured").value(true))
                .andExpect(jsonPath("$.message").value("Đã đánh dấu sản phẩm nổi bật"));
        
        verify(productService).toggleFeatured(1);
    }

    // ========== TC-PRD-CTRL-028: TOGGLE FEATURED ID NOT FOUND ==========
    
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-PRD-CTRL-028: Product - TOGGLE featured id not found - Return 404 Not Found")
    void TC_PRD_CTRL_028() throws Exception {
        // Arrange
        when(productService.toggleFeatured(99999))
                .thenThrow(new ResourceNotFoundException("Product not found with id: 99999"));

        // Act & Assert
        mockMvc.perform(put("/api/admin/products/99999/toggle-featured"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("ERROR"));
        
        verify(productService).toggleFeatured(99999);
    }

    // ========== TC-PRD-CTRL-029: GET INVENTORY VALID REQUEST ==========
    
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-PRD-CTRL-029: Product - GET inventory valid request - Return 200 OK with inventory list")
    void TC_PRD_CTRL_029() throws Exception {
        // Arrange
        List<Product> products = List.of(createMockProduct(1, "Test", new BigDecimal("35000")));
        Page<Product> productPage = new PageImpl<>(products);
        
        when(productService.findAllPaginated(any(Pageable.class))).thenReturn(productPage);

        // Act & Assert
        mockMvc.perform(get("/api/admin/products/inventory")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items[0].id").exists())
                .andExpect(jsonPath("$.data.items[0].name").exists())
                .andExpect(jsonPath("$.data.items[0].price").exists());
    }

    // ========== TC-PRD-CTRL-030: GET INVENTORY WITH KEYWORD SEARCH ==========
    
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-PRD-CTRL-030: Product - GET inventory with keyword search - Return 200 OK with filtered list")
    void TC_PRD_CTRL_030() throws Exception {
        // Arrange
        List<Product> products = List.of(createMockProduct(1, "Cà phê sữa", new BigDecimal("35000")));
        Page<Product> productPage = new PageImpl<>(products);
        
        when(productService.searchProductsPaginated(eq("cà phê"), any(Pageable.class)))
                .thenReturn(productPage);

        // Act & Assert
        mockMvc.perform(get("/api/admin/products/inventory")
                .param("keyword", "cà phê")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items").isArray());
        
        verify(productService).searchProductsPaginated(eq("cà phê"), any(Pageable.class));
    }

    // ========== TC-PRD-CTRL-032: UPLOAD PRODUCT IMAGES VALID FILES ==========
    
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-PRD-CTRL-032: Product - UPLOAD product images valid files - Return 200 OK")
    void TC_PRD_CTRL_032() throws Exception {
        // Arrange
        MockMultipartFile image1 = new MockMultipartFile(
            "images", "img1.jpg", "image/jpeg", "image1 content".getBytes()
        );
        MockMultipartFile image2 = new MockMultipartFile(
            "images", "img2.jpg", "image/jpeg", "image2 content".getBytes()
        );
        
        doNothing().when(productValidator).validateImageFiles(anyList());
        doNothing().when(productService).saveProductImages(eq(1), anyList());

        // Act & Assert
        mockMvc.perform(multipart("/api/admin/products/1/images")
                .file(image1)
                .file(image2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Upload ảnh thành công"));
        
        verify(productValidator).validateImageFiles(anyList());
        verify(productService).saveProductImages(eq(1), anyList());
    }

    // ========== TC-PRD-CTRL-033: UPLOAD PRODUCT IMAGES INVALID MIME TYPE ==========
    
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-PRD-CTRL-033: Product - UPLOAD product images invalid MIME type - Return 400 Bad Request")
    void TC_PRD_CTRL_033() throws Exception {
        // Arrange
        MockMultipartFile exeFile = new MockMultipartFile(
            "images", "malware.exe", "application/x-msdownload", "malware content".getBytes()
        );
        
        doThrow(new BusinessRuleException("Chỉ chấp nhận file ảnh định dạng: JPG, PNG, WEBP"))
                .when(productValidator).validateImageFiles(anyList());

        // Act & Assert
        mockMvc.perform(multipart("/api/admin/products/1/images")
                .file(exeFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"));
        
        verify(productValidator).validateImageFiles(anyList());
    }

    // ========== TC-PRD-CTRL-034: UPLOAD 0 FILES ==========
    
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-PRD-CTRL-034: Product - UPLOAD 0 files (empty list) - Return 400 Bad Request")
    void TC_PRD_CTRL_034() throws Exception {
        // Arrange
        doThrow(new BusinessRuleException("Vui lòng chọn ít nhất 1 ảnh"))
                .when(productValidator).validateImageFiles(anyList());

        // Act & Assert
        mockMvc.perform(multipart("/api/admin/products/1/images"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"));
    }

    // ========== TC-PRD-CTRL-036: GET PRODUCT IMAGES BY PRODUCT ID ==========
    
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-PRD-CTRL-036: Product - GET product images by product id - Return 200 OK with image list")
    void TC_PRD_CTRL_036() throws Exception {
        // Arrange
        List<ProductImage> images = new ArrayList<>();
        ProductImage img1 = new ProductImage();
        img1.setId(1);
        img1.setImageUrl("http://example.com/img1.jpg");
        images.add(img1);
        
        when(productService.getProductImages(1)).thenReturn(images);

        // Act & Assert
        mockMvc.perform(get("/api/admin/products/1/images"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1));
        
        verify(productService).getProductImages(1);
    }

    // ========== TC-PRD-CTRL-037: DELETE PRODUCT IMAGE VALID IMAGEID ==========
    
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-PRD-CTRL-037: Product - DELETE product image valid imageId - Return 200 OK")
    void TC_PRD_CTRL_037() throws Exception {
        // Arrange
        doNothing().when(productService).deleteProductImage(5);

        // Act & Assert
        mockMvc.perform(delete("/api/admin/products/images/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Xóa ảnh thành công"));
        
        verify(productService).deleteProductImage(5);
    }

    // ========== TC-PRD-CTRL-038: DELETE PRODUCT IMAGE NOT FOUND ==========
    
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-PRD-CTRL-038: Product - DELETE product image not found - Return 404 Not Found")
    void TC_PRD_CTRL_038() throws Exception {
        // Arrange
        doThrow(new ResourceNotFoundException("Không tìm thấy ảnh với ID: 99999"))
                .when(productService).deleteProductImage(99999);

        // Act & Assert
        mockMvc.perform(delete("/api/admin/products/images/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("ERROR"));
        
        verify(productService).deleteProductImage(99999);
    }
}
