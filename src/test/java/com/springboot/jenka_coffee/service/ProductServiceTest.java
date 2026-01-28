package com.springboot.jenka_coffee.service;

import com.springboot.jenka_coffee.entity.Product;
import com.springboot.jenka_coffee.repository.ProductRepository;
import com.springboot.jenka_coffee.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UploadService uploadService;

    @InjectMocks
    private ProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getRelatedProducts_shouldCallDaoWithCorrectParameters() {
        // Arrange
        String categoryId = "CAT1";
        Integer productId = 1;
        List<Product> expectedProducts = Arrays.asList(new Product(), new Product());

        when(productRepository.findTop4ByCategoryIdAndIdNot(categoryId, productId)).thenReturn(expectedProducts);

        // Act
        List<Product> result = productService.getRelatedProducts(categoryId, productId);

        // Assert
        assertEquals(expectedProducts, result);
        verify(productRepository, times(1)).findTop4ByCategoryIdAndIdNot(categoryId, productId);
    }
}
