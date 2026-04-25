package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.entity.Product;
import com.springboot.jenka_coffee.entity.Category;
import com.springboot.jenka_coffee.exception.ResourceNotFoundException;
import com.springboot.jenka_coffee.repository.ProductRepository;
import com.springboot.jenka_coffee.repository.CategoryRepository;
import com.springboot.jenka_coffee.repository.ProductImageRepository;
import com.springboot.jenka_coffee.service.UploadService;
import com.springboot.jenka_coffee.service.VercelWebhookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {

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

    @InjectMocks
    private ProductServiceImpl productService;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setId(1);
        testProduct.setName("Cà phê Đen");
        testProduct.setPrice(new BigDecimal("30000"));
        testProduct.setAvailable(true);
    }

    @Test
    @DisplayName("Test findById - Success")
    void findById_Success() {
        when(productRepository.findByIdWithCategory(1)).thenReturn(Optional.of(testProduct));

        Product result = productService.findById(1);

        assertNotNull(result);
        assertEquals("Cà phê Đen", result.getName());
    }

    @Test
    @DisplayName("Test saveProduct - Success with image")
    void saveProduct_Success() throws Exception {
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);
        when(productRepository.findByIdWithCategory(any())).thenReturn(Optional.of(testProduct));

        Product result = productService.saveProduct(testProduct, null);

        assertNotNull(result);
        verify(productRepository).save(testProduct);
        verify(vercelWebhookService).triggerRebuild();
    }

    @Test
    @DisplayName("Test delete - Success")
    void delete_Success() {
        when(productRepository.existsById(1)).thenReturn(true);
        doNothing().when(productRepository).deleteById(1);

        productService.delete(1);

        verify(productRepository).deleteById(1);
        verify(vercelWebhookService).triggerRebuild();
    }

    @Test
    @DisplayName("Test findById - Throws Exception")
    void findById_Throws() {
        when(productRepository.findByIdWithCategory(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            productService.findById(999);
        });
    }

    @Test
    @DisplayName("Test toggleAvailable")
    void toggleAvailable() {
        when(productRepository.findById(1)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        productService.toggleAvailable(1);

        assertFalse(testProduct.getAvailable());
        verify(productRepository).save(testProduct);
    }
}
