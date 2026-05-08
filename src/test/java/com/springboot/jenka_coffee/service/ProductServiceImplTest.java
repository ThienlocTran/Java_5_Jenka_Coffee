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
        // Arrange - No stub needed, price validation happens before repository query

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
        verify(productRepository, times(2)).existsById(1);  // Called by deleteProductWithValidation + delete
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
        // NOTE: uploadService stub removed — imageFile=null so upload path is never reached

        // Act
        Product result = productService.updateProductFromRequest(
            1, "Updated Name", "Description", new BigDecimal("45000"), "CF", true, null
        );

        // Assert
        assertNotNull(result);
        verify(productRepository, times(2)).findByIdWithCategory(1);  // Called by updateProductFromRequest + saveProduct
        verify(categoryRepository).findById("CF");
    }

    // ========== TC-PRD-SER-009: REPOSITORY SAVE THROWS RUNTIMEEXCEPTION ==========

    @Test
    @DisplayName("TC-PRD-SER-009: [GAP] Repository save() throws RuntimeException - create() wraps it after 3 retries → RuntimeException propagates (NOT rolled back to BusinessRuleException)")
    void TC_PRD_SER_009() {
        // Source: create() has retry loop (3 attempts), catches DataIntegrityViolationException
        // After 3 retries → throws RuntimeException("Không thể tạo sản phẩm sau nhiều lần thử...")
        // This RuntimeException is NOT @Transactional-aware → DB state depends on JPA flush behavior
        // CRITICAL: verify that no record persists after the exception

        ProductRequest request = new ProductRequest();
        request.setName("Test Product");
        request.setPrice(new java.math.BigDecimal("35000"));
        request.setAvailable(true);

        when(categoryRepository.findById("CF")).thenReturn(java.util.Optional.of(mockCategory));
        // After slug generation check (existsBySlug), save() throws DataIntegrityViolationException on all 3 attempts
        when(productRepository.existsBySlug(anyString())).thenReturn(false);
        when(productRepository.save(any(Product.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("Duplicate key"));

        // After 3 retries, create() wraps into RuntimeException
        RuntimeException thrown = assertThrows(
            RuntimeException.class,
            () -> productService.createProductFromRequest(request, "CF", null)
        );

        // The wrapped message must contain meaningful info — not just "Internal Server Error"
        assertNotNull(thrown.getMessage(), "RuntimeException message must NOT be null");
        // Accept both generic creation failure message OR slug collision message
        assertTrue(
            thrown.getMessage().contains("Không thể tạo sản phẩm") ||
            thrown.getMessage().contains("tên này đã tồn tại"),
            "Expected creation failure or slug collision message, got: " + thrown.getMessage()
        );

        // save() is called exactly 3 times (max retry attempts in create())
        verify(productRepository, times(3)).save(any(Product.class));
    }

    // ========== TC-PRD-SER-010: UPDATE WITH NULL NAME ==========

    @Test
    @DisplayName("TC-PRD-SER-010: updateProductFromRequest() with name=null - service does NOT validate name → null set on entity (GAP)")
    void TC_PRD_SER_010() {
        // Source: updateProductFromRequest() only validates price (null check, < 0 check)
        // There is NO name null/blank validation in service
        // existing.setName(name) → name=null → Product.name=null → save() called with null name
        // This is a GAP: PUT is full replace, null name should be rejected

        when(productRepository.findByIdWithCategory(1)).thenReturn(java.util.Optional.of(mockProduct));
        when(categoryRepository.findById("CF")).thenReturn(java.util.Optional.of(mockCategory));
        // saveProduct() calls: uploadService (no file), then saveProductToDatabase() → productRepository.save()
        when(productRepository.findById(1)).thenReturn(java.util.Optional.of(mockProduct));
        when(productRepository.save(any(Product.class))).thenReturn(mockProduct);

        // EXPECTATION: service accepts null name (GAP - no validation)
        // If BusinessRuleException is thrown here → validation was added → test must be updated
        assertDoesNotThrow(() ->
            productService.updateProductFromRequest(
                1, null, "Description", new java.math.BigDecimal("50000"), "CF", true, null
            ),
            "GAP: updateProductFromRequest accepts null name without throwing — name validation is missing"
        );

        // Verify that save() was called with name=null (the gap is confirmed)
        verify(productRepository).save(argThat(p -> p.getName() == null));
    }

    // ========== TC-PRD-SER-011: TOGGLEAVAILABLE IDEMPOTENCY ==========

    @Test
    @DisplayName("TC-PRD-SER-011: toggleAvailable() called twice - state returns to original (true→false→true)")
    void TC_PRD_SER_011() {
        // This verifies the toggle is a pure flip — NOT a set-to-false
        // Call 1: available=true → false; Call 2: available=false → true
        mockProduct.setAvailable(true);

        // First call: true → false
        when(productRepository.findById(1)).thenReturn(java.util.Optional.of(mockProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            mockProduct.setAvailable(p.getAvailable()); // persist the change in mock state
            return p;
        });

        productService.toggleAvailable(1);
        assertFalse(mockProduct.getAvailable(), "After 1st toggle: should be false");

        // Second call: false → true
        productService.toggleAvailable(1);
        assertTrue(mockProduct.getAvailable(), "After 2nd toggle: should be back to true (idempotency confirmed)");

        // save() must be called exactly twice
        verify(productRepository, times(2)).save(any(Product.class));
        // No side effects on other fields
        verify(productRepository, never()).deleteById(any());
    }

    // ========== TC-PRD-SER-012: DELETE PRODUCT CASCADES IMAGE DELETION ==========

    @Test
    @DisplayName("TC-PRD-SER-012: deleteProductWithValidation() - images NOT cascade-deleted via productImageRepo (delete() only calls productRepository.deleteById)")
    void TC_PRD_SER_012() {
        // Source: deleteProductWithValidation() → calls delete(id) → productRepository.deleteById(id)
        // delete() does NOT call productImageRepository.deleteByProductId()
        // Image cascade must happen at DB level (e.g. ON DELETE CASCADE) — NOT in Java code
        // This test documents actual behavior: productImageRepo is NEVER called explicitly

        when(productRepository.existsById(1)).thenReturn(true);
        when(productRepository.countOrdersByProductId(1)).thenReturn(0L);
        doNothing().when(productRepository).deleteById(1);

        productService.deleteProductWithValidation(1);

        // Confirm product IS deleted
        verify(productRepository).deleteById(1);

        // CRITICAL: productImageRepository is NOT called explicitly in Java
        // Cascade must rely on DB-level constraint — if DB has no cascade, images are orphaned
        verify(productImageRepository, never()).deleteByProductId(1);

        // If this verify fails (productImageRepo WAS called) → explicit Java cascade was added
        // In that case: update this test to assert imageRepo.deleteByProductId() WAS called
    }

    // ========== TC-PRD-SER-013: DELETE PRODUCT SUCCEEDS BUT IMAGE DELETION FAILS ==========

    @Test
    @DisplayName("TC-PRD-SER-013: [GAP] deleteProductWithValidation() - no explicit image deletion in Java; if DB cascade missing, product deleted but images orphaned")
    void TC_PRD_SER_013() {
        // Source: deleteProductWithValidation() → delete(id) → productRepository.deleteById(id)
        // No image deletion code in service → consistency depends entirely on DB schema cascade
        // GAP: If DB schema has no ON DELETE CASCADE, ProductImages remain as orphan rows

        when(productRepository.existsById(1)).thenReturn(true);
        when(productRepository.countOrdersByProductId(1)).thenReturn(0L);
        doNothing().when(productRepository).deleteById(1);

        // Simulate: DB cascade exists → deleteById succeeds, no exception
        assertDoesNotThrow(() -> productService.deleteProductWithValidation(1));

        // Product was deleted
        verify(productRepository).deleteById(1);

        // Service does NOT call productImageRepository at all
        // → Consistency guarantee is DB-schema responsibility, NOT Java code
        // This test DOCUMENTS that Java does not protect against orphaned images if DB cascade is absent
        verifyNoInteractions(productImageRepository);
    }
}

