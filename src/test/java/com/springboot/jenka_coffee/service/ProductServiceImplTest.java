package com.springboot.jenka_coffee.service;

import com.springboot.jenka_coffee.dto.request.ProductRequest;
import com.springboot.jenka_coffee.entity.Category;
import com.springboot.jenka_coffee.entity.Product;
import com.springboot.jenka_coffee.entity.ProductImage;
import com.springboot.jenka_coffee.exception.BusinessRuleException;
import com.springboot.jenka_coffee.exception.ResourceNotFoundException;
import com.springboot.jenka_coffee.repository.CategoryRepository;
import com.springboot.jenka_coffee.repository.ProductImageRepository;
import com.springboot.jenka_coffee.repository.ProductRepository;
import com.springboot.jenka_coffee.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test Case Document: PRODUCT MODULE - SERVICE LAYER
 * Tests for ProductServiceImpl business logic
 * 
 * Coverage from TC_BATCH_01_AUTH_PRODUCT_v2.csv:
 * - TC-PRD-SER-001 to TC-PRD-SER-008: Service tests (8 test cases)
 * 
 * ✅ STRATEGY:
 * - Use @ExtendWith(MockitoExtension.class) for pure unit tests
 * - Mock all dependencies (repositories, services)
 * - Test business logic validation
 * - Test exception handling
 * - NO Spring context loaded (fast tests)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Product Service Implementation Tests")
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private UploadService uploadService;

    @Mock
    private VercelWebhookService vercelWebhookService;

    @InjectMocks
    private ProductServiceImpl productService;

    private Category mockCategory;
    private Product mockProduct;

    @BeforeEach
    void setUp() {
        mockCategory = new Category();
        mockCategory.setId("CF");
        mockCategory.setName("Cà phê");

        mockProduct = new Product();
        mockProduct.setId(1);
        mockProduct.setName("Cà phê sữa");
        mockProduct.setPrice(new BigDecimal("35000"));
        mockProduct.setAvailable(true);
        mockProduct.setCategory(mockCategory);
    }

    // ========== TC-PRD-SER-001: CREATE PRODUCT NAME IS BLANK ==========
    
    @Test
    @DisplayName("TC-PRD-SER-001: Product Service - Create product name is blank - Throw BusinessRuleException")
    void TC_PRD_SER_001() {
        // Arrange
        ProductRequest request = new ProductRequest();
        request.setName("");  // Blank name
        request.setPrice(new BigDecimal("35000"));

        // Act & Assert
        BusinessRuleException exception = assertThrows(
            BusinessRuleException.class,
            () -> productService.createProductFromRequest(request, "CF", null)
        );
        
        // Verify exception message
        assertNotNull(exception.getMessage());
        
        // Verify repository was NOT called
        verify(productRepository, never()).save(any());
    }

    // ========== TC-PRD-SER-002: CREATE PRODUCT PRICE IS NULL ==========
    
    @Test
    @DisplayName("TC-PRD-SER-002: Product Service - Create product price is null - Throw BusinessRuleException")
    void TC_PRD_SER_002() {
        // Arrange
        ProductRequest request = new ProductRequest();
        request.setName("Test Product");
        request.setPrice(null);  // Null price

        // Act & Assert
        BusinessRuleException exception = assertThrows(
            BusinessRuleException.class,
            () -> productService.createProductFromRequest(request, "CF", null)
        );
        
        assertEquals("Giá sản phẩm không được để trống", exception.getMessage());
        
        // Verify repository was NOT called
        verify(productRepository, never()).save(any());
    }

    // ========== TC-PRD-SER-003: CREATE PRODUCT PRICE < 0 ==========
    
    @Test
    @DisplayName("TC-PRD-SER-003: Product Service - Create product price < 0 - Throw BusinessRuleException")
    void TC_PRD_SER_003() {
        // Arrange
        ProductRequest request = new ProductRequest();
        request.setName("Test Product");
        request.setPrice(new BigDecimal("-1"));  // Negative price

        // Act & Assert
        BusinessRuleException exception = assertThrows(
            BusinessRuleException.class,
            () -> productService.createProductFromRequest(request, "CF", null)
        );
        
        assertEquals("Giá sản phẩm không thể âm", exception.getMessage());
        
        // Verify repository was NOT called
        verify(productRepository, never()).save(any());
    }

    // ========== TC-PRD-SER-004: CREATE PRODUCT CATEGORYID NOT EXISTS ==========
    
    @Test
    @DisplayName("TC-PRD-SER-004: Product Service - Create product categoryId not exists - Throw BusinessRuleException")
    void TC_PRD_SER_004() {
        // Arrange
        ProductRequest request = new ProductRequest();
        request.setName("Test Product");
        request.setPrice(new BigDecimal("35000"));
        
        when(categoryRepository.findById("INVALID")).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> productService.createProductFromRequest(request, "INVALID", null)
        );
        
        assertTrue(exception.getMessage().contains("Không tìm thấy danh mục"));
        
        // Verify repository was called but save was NOT
        verify(categoryRepository).findById("INVALID");
        verify(productRepository, never()).save(any());
    }

    // ========== TC-PRD-SER-005: DELETE PRODUCT HAS ACTIVE ORDER DETAILS ==========
    
    @Test
    @DisplayName("TC-PRD-SER-005: Product Service - Delete product has active order details - Throw BusinessRuleException")
    void TC_PRD_SER_005() {
        // Arrange
        when(productRepository.existsById(1)).thenReturn(true);
        when(productRepository.countOrdersByProductId(1)).thenReturn(5L);  // Has 5 orders

        // Act & Assert
        BusinessRuleException exception = assertThrows(
            BusinessRuleException.class,
            () -> productService.deleteProductWithValidation(1)
        );
        
        assertTrue(exception.getMessage().contains("đã có"));
        assertTrue(exception.getMessage().contains("đơn hàng"));
        
        // Verify delete was NOT called
        verify(productRepository).existsById(1);
        verify(productRepository).countOrdersByProductId(1);
        verify(productRepository, never()).deleteById(any());
    }

    // ========== TC-PRD-SER-006: UPDATE PRODUCT PRICE = NULL ==========
    
    @Test
    @DisplayName("TC-PRD-SER-006: Product Service - Update product price = null - Throw BusinessRuleException")
    void TC_PRD_SER_006() {
        // Arrange
        when(productRepository.findByIdWithCategory(1)).thenReturn(Optional.of(mockProduct));

        // Act & Assert
        BusinessRuleException exception = assertThrows(
            BusinessRuleException.class,
            () -> productService.updateProductFromRequest(
                1, "Updated Name", "Description", null, "CF", true, null
            )
        );
        
        assertEquals("Giá sản phẩm không được để trống", exception.getMessage());
        
        // Verify save was NOT called
        verify(productRepository, never()).save(any());
    }

    // ========== TC-PRD-SER-007: FINDBYID PRODUCT NOT EXISTS ==========
    
    @Test
    @DisplayName("TC-PRD-SER-007: Product Service - FindById product not exists - Throw ResourceNotFoundException")
    void TC_PRD_SER_007() {
        // Arrange
        when(productRepository.findByIdWithCategory(99999)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> productService.findById(99999)
        );
        
        assertEquals("Product not found with id: 99999", exception.getMessage());
        
        verify(productRepository).findByIdWithCategory(99999);
    }

    // ========== TC-PRD-SER-008: TOGGLEAVAILABLE PRODUCT NOT EXISTS ==========
    
    @Test
    @DisplayName("TC-PRD-SER-008: Product Service - ToggleAvailable product not exists - Throw ResourceNotFoundException")
    void TC_PRD_SER_008() {
        // Arrange
        when(productRepository.findById(99999)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> productService.toggleAvailable(99999)
        );
        
        assertEquals("Product not found with id: 99999", exception.getMessage());
        
        verify(productRepository).findById(99999);
        verify(productRepository, never()).save(any());
    }

    // ========== ADDITIONAL TEST: TOGGLE AVAILABLE SUCCESS ==========
    
    @Test
    @DisplayName("Product Service - ToggleAvailable success - Changes available status")
    void testToggleAvailableSuccess() {
        // Arrange
        mockProduct.setAvailable(true);
        when(productRepository.findById(1)).thenReturn(Optional.of(mockProduct));
        when(productRepository.save(any(Product.class))).thenReturn(mockProduct);

        // Act
        productService.toggleAvailable(1);

        // Assert
        verify(productRepository).findById(1);
        verify(productRepository).save(argThat(product -> 
            product.getId().equals(1) && !product.getAvailable()
        ));
    }

    // ========== ADDITIONAL TEST: CREATE PRODUCT SUCCESS ==========
    
    @Test
    @DisplayName("Product Service - Create product success - Returns saved product")
    void testCreateProductSuccess() {
        // Arrange
        ProductRequest request = new ProductRequest();
        request.setName("New Product");
        request.setPrice(new BigDecimal("50000"));
        request.setAvailable(true);
        
        when(categoryRepository.findById("CF")).thenReturn(Optional.of(mockCategory));
        when(productRepository.existsBySlug(anyString())).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(mockProduct);

        // Act
        Product result = productService.createProductFromRequest(request, "CF", null);

        // Assert
        assertNotNull(result);
        verify(categoryRepository).findById("CF");
        verify(productRepository).save(any(Product.class));
    }

    // ========== ADDITIONAL TEST: DELETE PRODUCT SUCCESS ==========
    
    @Test
    @DisplayName("Product Service - Delete product success - Product deleted")
    void testDeleteProductSuccess() {
        // Arrange
        when(productRepository.existsById(1)).thenReturn(true);
        when(productRepository.countOrdersByProductId(1)).thenReturn(0L);  // No orders
        doNothing().when(productRepository).deleteById(1);

        // Act
        productService.deleteProductWithValidation(1);

        // Assert
        verify(productRepository).existsById(1);
        verify(productRepository).countOrdersByProductId(1);
        verify(productRepository).deleteById(1);
    }

    // ========== ADDITIONAL TEST: UPDATE PRODUCT SUCCESS ==========
    
    @Test
    @DisplayName("Product Service - Update product success - Returns updated product")
    void testUpdateProductSuccess() {
        // Arrange
        when(productRepository.findByIdWithCategory(1)).thenReturn(Optional.of(mockProduct));
        when(categoryRepository.findById("CF")).thenReturn(Optional.of(mockCategory));
        when(productRepository.save(any(Product.class))).thenReturn(mockProduct);
        when(uploadService.saveProductImage(any(MultipartFile.class))).thenReturn("http://example.com/image.jpg");

        // Act
        Product result = productService.updateProductFromRequest(
            1, "Updated Name", "Description", new BigDecimal("45000"), "CF", true, null
        );

        // Assert
        assertNotNull(result);
        verify(productRepository).findByIdWithCategory(1);
        verify(categoryRepository).findById("CF");
    }
}
