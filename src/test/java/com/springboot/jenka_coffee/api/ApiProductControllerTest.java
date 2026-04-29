package com.springboot.jenka_coffee.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.jenka_coffee.entity.Product;
import com.springboot.jenka_coffee.exception.ResourceNotFoundException;
import com.springboot.jenka_coffee.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test Case Document Part 2: PRODUCT MODULE - Controller Layer
 * Tests for ApiProductController endpoints
 * 
 * Coverage:
 * - TC-PRD-001: Keyword too long (> 100 chars) → 400
 * - TC-PRD-002: Page exceeds limit (> 1000) → 400
 * - TC-PRD-003: Size capped at 50
 * - TC-PRD-004: Size=0 normalized to 1
 * - TC-PRD-005: Product not found → 404
 * - TC-PRD-006: Slug not found → 404
 * - TC-PRD-007: Filter by categoryId + price range
 * - TC-IMG-001: Get product images - product not found → 404
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)  // ✅ Disable RateLimitFilter for tests
@DisplayName("API Product Controller Tests")
class ApiProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    // ========== HELPER METHODS ==========

    private Product buildProduct(Integer id, BigDecimal price, boolean available) {
        Product product = new Product();
        product.setId(id);
        product.setName("Product " + id);
        product.setPrice(price);
        product.setAvailable(available);
        product.setImage("/images/product" + id + ".jpg");
        return product;
    }

    // ========== TEST CASES ==========

    /**
     * TC-PRD-001 — GET /api/products?keyword quá 100 ký tự → 400
     * Expected: HTTP 400, message="Từ khóa tìm kiếm quá dài (tối đa 100 ký tự)"
     */
    @Test
    @DisplayName("TC-PRD-001: GET /api/products với keyword > 100 ký tự phải bị reject")
    void getProducts_keywordTooLong_returns400() throws Exception {
        // Arrange
        String longKeyword = "A".repeat(101);

        // Act & Assert
        mockMvc.perform(get("/api/products").param("keyword", longKeyword))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("Từ khóa tìm kiếm quá dài (tối đa 100 ký tự)"));
    }

    /**
     * TC-PRD-002 — GET /api/products?page=1001 → Deep Pagination DoS
     * Expected: HTTP 400, message="Số trang vượt quá giới hạn (tối đa 1000)"
     */
    @Test
    @DisplayName("TC-PRD-002: GET /api/products với page > 1000 phải bị reject (DoS protection)")
    void getProducts_pageExceedsLimit_returns400() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/products").param("page", "1001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("Số trang vượt quá giới hạn (tối đa 1000)"));
    }

    /**
     * TC-PRD-003 — GET /api/products?size=9999 — size bị cap ở 50
     * Expected: HTTP 200, items.length <= 50
     */
    @Test
    @DisplayName("TC-PRD-003: GET /api/products với size=9999 phải bị giới hạn tối đa 50")
    void getProducts_sizeExceedsLimit_cappedAt50() throws Exception {
        // Arrange
        List<Product> products = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            products.add(buildProduct(i, new BigDecimal("100000"), true));
        }
        Page<Product> page = new PageImpl<>(products);

        when(productService.filterProductsWithAllCriteria(
                isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/products").param("size", "9999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(lessThanOrEqualTo(50)));

        // Verify that size was capped to 50
        verify(productService).filterProductsWithAllCriteria(
                isNull(), isNull(), isNull(), isNull(),
                argThat(pageable -> pageable.getPageSize() == 50)
        );
    }

    /**
     * TC-PRD-004 — GET /api/products?size=0 — normalize thành 1
     * Expected: HTTP 200, pageable được tạo với pageSize=1
     */
    @Test
    @DisplayName("TC-PRD-004: GET /api/products với size=0 phải được normalize thành 1")
    void getProducts_sizeZero_normalizedToOne() throws Exception {
        // Arrange
        List<Product> products = List.of(buildProduct(1, new BigDecimal("100000"), true));
        Page<Product> page = new PageImpl<>(products);

        when(productService.filterProductsWithAllCriteria(
                isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/products").param("size", "0"))
                .andExpect(status().isOk());

        // Verify that size was normalized to 1
        verify(productService).filterProductsWithAllCriteria(
                isNull(), isNull(), isNull(), isNull(),
                argThat(pageable -> pageable.getPageSize() == 1)
        );
    }

    /**
     * TC-PRD-005 — GET /api/products/{id} — product không tồn tại → 404
     * Expected: HTTP 404, message="Không tìm thấy sản phẩm với ID: 99999"
     */
    @Test
    @DisplayName("TC-PRD-005: GET /api/products/{id} với ID không tồn tại phải trả 404")
    void getProductDetail_notFound_returns404() throws Exception {
        // Arrange
        when(productService.getProductDetail(99999))
                .thenThrow(new ResourceNotFoundException("Product", "id", 99999));

        // Act & Assert
        mockMvc.perform(get("/api/products/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("Không tìm thấy sản phẩm với ID: 99999"));
    }

    /**
     * TC-PRD-006 — GET /api/products/slug/{slug} — slug không tồn tại → 404
     * Expected: HTTP 404, message="Không tìm thấy sản phẩm với slug: may-pha-khong-ton-tai"
     */
    @Test
    @DisplayName("TC-PRD-006: GET /api/products/slug/{slug} với slug không tồn tại phải trả 404")
    void getProductDetailBySlug_notFound_returns404() throws Exception {
        // Arrange
        when(productService.findBySlug("may-pha-khong-ton-tai"))
                .thenThrow(new ResourceNotFoundException("Product", "slug", "may-pha-khong-ton-tai"));

        // Act & Assert
        mockMvc.perform(get("/api/products/slug/may-pha-khong-ton-tai"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("Không tìm thấy sản phẩm với slug: may-pha-khong-ton-tai"));
    }

    /**
     * TC-PRD-007 — GET /api/products — filter theo categoryId và price range
     * 
     * ⚠️ BUG DISCOVERED: Controller không check null cho productPage
     * Line 82 trong ApiProductController.java:
     *   responseData.put("items", productPage.getContent());  // ❌ NPE if productPage is null
     * 
     * Expected (sau khi fix bug): HTTP 200, data.items chỉ chứa sản phẩm MAY_PHA có price trong 3M-6M
     * Actual (hiện tại): HTTP 500 nếu service return null
     */
    @Test
    @DisplayName("TC-PRD-007: GET /api/products với filter categoryId + minPrice + maxPrice trả đúng kết quả")
    void getProducts_filterByCategoryAndPrice_returnsCorrectItems() throws Exception {
        // Arrange
        BigDecimal min = new BigDecimal("3000000");
        BigDecimal max = new BigDecimal("6000000");
        
        Product product = buildProduct(1, new BigDecimal("5500000"), true);
        Page<Product> mockPage = new PageImpl<>(List.of(product));

        when(productService.filterProductsWithAllCriteria(
                eq("MAY_PHA"), eq(min), eq(max), isNull(), any(Pageable.class)))
                .thenReturn(mockPage);

        // Act & Assert
        mockMvc.perform(get("/api/products")
                        .param("categoryId", "MAY_PHA")
                        .param("minPrice", "3000000")
                        .param("maxPrice", "6000000"))
                    .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items.length()").value(1));

        verify(productService).filterProductsWithAllCriteria(
                eq("MAY_PHA"), eq(min), eq(max), isNull(), any(Pageable.class));
    }
    
    /**
     * TC-PRD-007b — GET /api/products — service return null → 500 (BUG)
     * 
     * 🐛 PRODUCTION BUG: Controller crashes với NPE khi service return null
     * Root cause: ApiProductController line 82 không check null
     * 
     * Expected behavior (sau khi fix):
     *   - Check if productPage == null
     *   - Return 500 với message rõ ràng hoặc empty result
     * 
     * Current behavior: NPE → 500 với stack trace
     */
    @Test
    @DisplayName("TC-PRD-007b: [BUG] Service return null → Controller crash với NPE")
    void getProducts_serviceReturnsNull_crashes500() throws Exception {
        // Arrange - Service return null (edge case)
        when(productService.filterProductsWithAllCriteria(
                any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(null);  // ❌ BUG: Controller không handle null

        // Act & Assert - Document current buggy behavior
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isInternalServerError());  // 500 vì NPE
        
        // TODO: Sau khi fix bug trong controller, test này phải expect:
        // .andExpect(status().isOk())
        // .andExpect(jsonPath("$.data.items").isEmpty());
    }

    /**
     * TC-IMG-001 — GET /api/products/{id}/images — product không tồn tại → 404
     * Expected: HTTP 404, message="Không tìm thấy sản phẩm"
     */
    @Test
    @DisplayName("TC-IMG-001: GET /api/products/{id}/images với product không tồn tại phải trả 404")
    void getProductImages_productNotFound_returns404() throws Exception {
        // Arrange
        when(productService.getProductImages(99999))
                .thenThrow(new ResourceNotFoundException("Product", "id", 99999));

        // Act & Assert
        mockMvc.perform(get("/api/products/99999/images"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("Không tìm thấy sản phẩm"));
    }
}
