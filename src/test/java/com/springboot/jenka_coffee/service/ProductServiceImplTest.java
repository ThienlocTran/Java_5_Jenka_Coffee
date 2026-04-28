package com.springboot.jenka_coffee.service;

import com.springboot.jenka_coffee.dto.request.ProductRequest;
import com.springboot.jenka_coffee.entity.Category;
import com.springboot.jenka_coffee.entity.Product;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test Case Document Part 2: PRODUCT MODULE - Service Layer
 * Tests for ProductServiceImpl business logic
 * 
 * Coverage:
 * - TC-PRD-008: createProductFromRequest - price âm → exception
 * - TC-PRD-009: createProductFromRequest - categoryId không tồn tại
 * - TC-PRD-010: deleteProductWithValidation - product đã có orders
 * - TC-PRD-011: toggleAvailable - available true → false
 * - TC-PRD-013: price được round HALF_UP về 0 decimal
 * - TC-PRD-015: toggleFeatured - false → true → false cycle
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Product Service Implementation Tests")
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UploadService uploadService;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private VercelWebhookService vercelWebhookService;

    private ProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        productService = new ProductServiceImpl(
                productRepository,
                uploadService,
                categoryRepository,
                productImageRepository,
                vercelWebhookService
        );
    }

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

    private Category buildCategory(String id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        return category;
    }

    private ProductRequest buildProductRequest(String name, BigDecimal price) {
        ProductRequest request = new ProductRequest();
        request.setName(name);
        request.setPrice(price);
        request.setDescription("Test description");
        return request;
    }

    // ========== TEST CASES ==========

    /**
     * TC-PRD-008 — createProductFromRequest — price âm → exception
     * Expected: BusinessRuleException("Giá sản phẩm không thể âm")
     */
    @Test
    @DisplayName("TC-PRD-008: createProductFromRequest với price < 0 phải throw BusinessRuleException")
    void createProductFromRequest_negativePrice_throwsBusinessRuleException() {
        // Arrange
        ProductRequest request = buildProductRequest("Test product", new BigDecimal("-1000"));
        Category category = buildCategory("MAY_PHA", "Máy Pha Cà Phê");
        
        when(categoryRepository.findById("MAY_PHA")).thenReturn(Optional.of(category));

        // Act & Assert
        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> {
            productService.createProductFromRequest(request, "MAY_PHA", null);
        });

        assertTrue(exception.getMessage().contains("Giá sản phẩm không thể âm"));
    }

    /**
     * TC-PRD-009 — createProductFromRequest — categoryId không tồn tại
     * Expected: ResourceNotFoundException("Không tìm thấy danh mục với ID: KHONG_CO")
     */
    @Test
    @DisplayName("TC-PRD-009: createProductFromRequest với categoryId không tồn tại phải throw ResourceNotFoundException")
    void createProductFromRequest_categoryNotFound_throwsResourceNotFoundException() {
        // Arrange
        ProductRequest request = buildProductRequest("Test product", new BigDecimal("1000000"));
        
        when(categoryRepository.findById("KHONG_CO")).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            productService.createProductFromRequest(request, "KHONG_CO", null);
        });

        assertTrue(exception.getMessage().contains("Không tìm thấy danh mục với ID: KHONG_CO"));
    }

    /**
     * TC-PRD-010 — deleteProductWithValidation — product đã có orders
     * Expected: BusinessRuleException với message chứa "Không thể xóa sản phẩm này vì đã có 3 đơn hàng sử dụng"
     */
    @Test
    @DisplayName("TC-PRD-010: deleteProductWithValidation với product đã có orders phải throw BusinessRuleException")
    void deleteProductWithValidation_productHasOrders_throwsBusinessRuleException() {
        // Arrange
        when(productRepository.existsById(7)).thenReturn(true);
        when(productRepository.countOrdersByProductId(7)).thenReturn(3L);

        // Act & Assert
        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> {
            productService.deleteProductWithValidation(7);
        });

        assertTrue(exception.getMessage().contains("3 đơn hàng sử dụng"));
        assertTrue(exception.getMessage().contains("Không thể xóa sản phẩm này"));
        
        // Verify delete was NOT called
        verify(productRepository, never()).deleteById(anyInt());
    }

    /**
     * TC-PRD-011 — toggleAvailable — available true → false
     * Expected: productRepository.save() được gọi với product.available = false
     */
    @Test
    @DisplayName("TC-PRD-011: toggleAvailable từ true → false phải save với available=false")
    void toggleAvailable_trueToFalse_savesWithFalse() {
        // Arrange
        Product product = buildProduct(10, new BigDecimal("1000000"), true);
        product.setAvailable(true);
        
        when(productRepository.findById(10)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        productService.toggleAvailable(10);

        // Assert
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        
        Product savedProduct = productCaptor.getValue();
        assertFalse(savedProduct.getAvailable(), "Product available phải được toggle thành false");
    }

    /**
     * TC-PRD-013 — price được round HALF_UP về 0 decimal
     * Expected: product.getPrice() = new BigDecimal("5500000")
     */
    @Test
    @DisplayName("TC-PRD-013: buildProductFromRequest phải round price HALF_UP về 0 decimal")
    void buildProductFromRequest_priceRounding_halfUp() {
        // Arrange
        ProductRequest request = buildProductRequest("Test product", new BigDecimal("5499999.6"));
        Category category = buildCategory("MAY_PHA", "Máy Pha Cà Phê");
        
        when(categoryRepository.findById("MAY_PHA")).thenReturn(Optional.of(category));
        when(productRepository.existsBySlug(anyString())).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(i -> {
            Product p = i.getArgument(0);
            p.setId(1);
            return p;
        });

        // Act
        Product result = productService.createProductFromRequest(request, "MAY_PHA", null);

        // Assert
        BigDecimal expected = new BigDecimal("5499999.6").setScale(0, RoundingMode.HALF_UP);
        assertEquals(0, expected.compareTo(result.getPrice()), 
                "Price phải được round HALF_UP: 5499999.6 → 5500000");
        assertEquals(new BigDecimal("5500000"), result.getPrice());
    }

    /**
     * TC-PRD-015 — toggleFeatured: false → true → false cycle
     * Expected: Lần 1: product.featured=true. Lần 2: product.featured=false
     */
    @Test
    @DisplayName("TC-PRD-015: toggleFeatured từ false → true phải save với featured=true")
    void toggleFeatured_fromFalseToTrue() {
        // Arrange
        Product product = buildProduct(15, new BigDecimal("1000000"), true);
        product.setFeatured(false);
        
        when(productRepository.existsById(15)).thenReturn(true);
        when(productRepository.findByIdWithCategory(15)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));
        when(productRepository.findById(15)).thenReturn(Optional.of(product));

        // Act
        Product result = productService.toggleFeatured(15);

        // Assert
        assertTrue(result.getFeatured(), "Featured phải được toggle thành true");
        
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        assertTrue(productCaptor.getValue().getFeatured());
    }

    /**
     * TC-PRD-015 (part 2) — toggleFeatured: null treated as false
     * Expected: featured=null → toggle to true
     */
    @Test
    @DisplayName("TC-PRD-015: toggleFeatured với featured=null phải treat as false và toggle to true")
    void toggleFeatured_null_treatedAsFalse() {
        // Arrange
        Product product = buildProduct(16, new BigDecimal("1000000"), true);
        product.setFeatured(null); // null case
        
        when(productRepository.existsById(16)).thenReturn(true);
        when(productRepository.findByIdWithCategory(16)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));
        when(productRepository.findById(16)).thenReturn(Optional.of(product));

        // Act
        Product result = productService.toggleFeatured(16);

        // Assert
        assertTrue(result.getFeatured(), "Featured null phải được treat as false và toggle to true");
    }
}
