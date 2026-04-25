package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.dto.response.CartItem;
import com.springboot.jenka_coffee.entity.Product;
import com.springboot.jenka_coffee.exception.BusinessRuleException;
import com.springboot.jenka_coffee.service.ProductService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CartServiceTest {

    @Mock
    private ObjectProvider<ProductService> productServiceProvider;
    @Mock
    private ProductService productService;
    @Mock
    private Authentication authentication;
    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private CartServiceImpl cartService;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setId(1);
        testProduct.setName("Coffee");
        testProduct.setPrice(new BigDecimal("50000"));
        testProduct.setAvailable(true);

        when(productServiceProvider.getObject()).thenReturn(productService);
        
        // Mock SecurityContext
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testuser");
        when(authentication.getPrincipal()).thenReturn("testuser");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Test add product to cart")
    void add_Product() {
        when(productService.findById(1)).thenReturn(testProduct);

        cartService.add(1);

        Collection<CartItem> items = cartService.getItems();
        assertEquals(1, items.size());
        CartItem item = items.iterator().next();
        assertEquals(1, item.getProductId());
        assertEquals(1, item.getQuantity());
    }

    @Test
    @DisplayName("Test increment quantity in cart")
    void add_Product_Increment() {
        when(productService.findById(1)).thenReturn(testProduct);

        cartService.add(1);
        cartService.add(1);

        Collection<CartItem> items = cartService.getItems();
        assertEquals(1, items.size());
        assertEquals(2, items.iterator().next().getQuantity());
    }

    @Test
    @DisplayName("Test update quantity")
    void update_Quantity() {
        when(productService.findById(1)).thenReturn(testProduct);
        cartService.add(1);

        cartService.update(1, 5);

        assertEquals(5, cartService.getItems().iterator().next().getQuantity());
    }

    @Test
    @DisplayName("Test remove product")
    void remove_Product() {
        when(productService.findById(1)).thenReturn(testProduct);
        cartService.add(1);

        cartService.remove(1);

        assertTrue(cartService.getItems().isEmpty());
    }

    @Test
    @DisplayName("Test get total price")
    void getTotal_Price() {
        when(productService.findById(1)).thenReturn(testProduct);
        cartService.add(1); // 50000

        Product p2 = new Product();
        p2.setId(2);
        p2.setPrice(new BigDecimal("30000"));
        p2.setAvailable(true);
        when(productService.findById(2)).thenReturn(p2);
        cartService.add(2); // 30000

        assertEquals(new BigDecimal("80000"), cartService.getTotal());
    }

    @Test
    @DisplayName("Test add unavailable product - Throws Exception")
    void add_Unavailable() {
        testProduct.setAvailable(false);
        when(productService.findById(1)).thenReturn(testProduct);

        assertThrows(BusinessRuleException.class, () -> {
            cartService.add(1);
        });
    }
}
