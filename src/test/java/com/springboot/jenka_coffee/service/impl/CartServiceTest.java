package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.dto.response.CartItem;
import com.springboot.jenka_coffee.entity.Product;
import com.springboot.jenka_coffee.exception.BusinessRuleException;
import com.springboot.jenka_coffee.repository.CartItemRepository;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CartServiceImpl (DB-backed implementation).
 *
 * Strategy:
 * - Mock CartItemRepository để test business logic mà không cần DB thật.
 * - Set cart key via CartServiceImpl.setCartKey() thay vì SecurityContext mock.
 */
@ExtendWith(MockitoExtension.class)
public class CartServiceTest {

    @Mock
    private ObjectProvider<ProductService> productServiceProvider;
    @Mock
    private ProductService productService;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private Authentication authentication;
    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private CartServiceImpl cartService;

    private static final String TEST_USER = "testuser";
    private Product testProduct;
    private com.springboot.jenka_coffee.entity.CartItem existingCartItem;

    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setId(1);
        testProduct.setName("Coffee");
        testProduct.setImage("coffee.jpg");
        testProduct.setPrice(new BigDecimal("50000"));
        testProduct.setAvailable(true);
        testProduct.setRequireContact(false);

        when(productServiceProvider.getObject()).thenReturn(productService);

        // Set cart key directly (bypasses SecurityContext complexity)
        CartServiceImpl.setCartKey(TEST_USER);
    }

    @AfterEach
    void tearDown() {
        CartServiceImpl.clearCartKey();
        SecurityContextHolder.clearContext();
    }

    // ===========================================================
    // ADD TESTS
    // ===========================================================

    @Test
    @DisplayName("Add new product to cart — creates new CartItem in DB")
    void add_NewProduct_CreatesItem() {
        when(productService.findById(1)).thenReturn(testProduct);
        when(cartItemRepository.findByCartKeyAndProductId(TEST_USER, 1)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cartItemRepository.findByCartKey(TEST_USER)).thenAnswer(inv -> {
            com.springboot.jenka_coffee.entity.CartItem item = new com.springboot.jenka_coffee.entity.CartItem();
            item.setCartKey(TEST_USER);
            item.setProductId(1);
            item.setQuantity(1);
            item.setPriceSnapshot(new BigDecimal("50000"));
            item.setProductName("Coffee");
            item.setProductImage("coffee.jpg");
            return List.of(item);
        });

        cartService.add(1);

        Collection<CartItem> items = cartService.getItems();
        assertEquals(1, items.size());
        CartItem item = items.iterator().next();
        assertEquals(1, item.getProductId());
        assertEquals(1, item.getQuantity());
        verify(cartItemRepository).save(any());
    }

    @Test
    @DisplayName("Add existing product to cart — increments quantity")
    void add_ExistingProduct_IncrementsQuantity() {
        com.springboot.jenka_coffee.entity.CartItem existing = buildCartItemEntity(1, 3);
        when(cartItemRepository.findByCartKeyAndProductId(TEST_USER, 1)).thenReturn(Optional.of(existing));
        when(cartItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        cartService.add(1);

        assertEquals(4, existing.getQuantity());
        verify(cartItemRepository).save(existing);
        // productService should NOT be called — item already exists
        verify(productService, never()).findById(any());
    }

    @Test
    @DisplayName("Add existing product at max quantity (99) — throws BusinessRuleException")
    void add_AtMaxQuantity_ThrowsException() {
        com.springboot.jenka_coffee.entity.CartItem existing = buildCartItemEntity(1, 99);
        when(cartItemRepository.findByCartKeyAndProductId(TEST_USER, 1)).thenReturn(Optional.of(existing));

        assertThrows(BusinessRuleException.class, () -> cartService.add(1));
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Add unavailable product — throws BusinessRuleException")
    void add_UnavailableProduct_ThrowsException() {
        testProduct.setAvailable(false);
        when(productService.findById(1)).thenReturn(testProduct);
        when(cartItemRepository.findByCartKeyAndProductId(TEST_USER, 1)).thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class, () -> cartService.add(1));
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Add null product — throws BusinessRuleException")
    void add_NullProduct_ThrowsException() {
        when(productService.findById(99)).thenReturn(null);
        when(cartItemRepository.findByCartKeyAndProductId(TEST_USER, 99)).thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class, () -> cartService.add(99));
    }

    @Test
    @DisplayName("Add requireContact product — throws BusinessRuleException")
    void add_RequireContactProduct_ThrowsException() {
        testProduct.setRequireContact(true);
        when(productService.findById(1)).thenReturn(testProduct);
        when(cartItemRepository.findByCartKeyAndProductId(TEST_USER, 1)).thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class, () -> cartService.add(1));
    }

    // ===========================================================
    // UPDATE TESTS
    // ===========================================================

    @Test
    @DisplayName("Update quantity — saves with new quantity")
    void update_Quantity_Saved() {
        com.springboot.jenka_coffee.entity.CartItem existing = buildCartItemEntity(1, 2);
        when(cartItemRepository.findByCartKeyAndProductId(TEST_USER, 1)).thenReturn(Optional.of(existing));
        when(cartItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        cartService.update(1, 7);

        assertEquals(7, existing.getQuantity());
        verify(cartItemRepository).save(existing);
    }

    @Test
    @DisplayName("Update with qty <= 0 — removes item")
    void update_ZeroQty_RemovesItem() {
        cartService.update(1, 0);
        verify(cartItemRepository).deleteByCartKeyAndProductId(TEST_USER, 1);
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update with qty > 99 — throws BusinessRuleException")
    void update_OverMaxQty_ThrowsException() {
        assertThrows(BusinessRuleException.class, () -> cartService.update(1, 100));
        verify(cartItemRepository, never()).save(any());
    }

    // ===========================================================
    // REMOVE TESTS
    // ===========================================================

    @Test
    @DisplayName("Remove product — delegates to repository")
    void remove_Product_CallsRepository() {
        cartService.remove(1);
        verify(cartItemRepository).deleteByCartKeyAndProductId(TEST_USER, 1);
    }

    // ===========================================================
    // CLEAR TESTS
    // ===========================================================

    @Test
    @DisplayName("Clear cart — deletes all items by cart key")
    void clear_DeletesAllByCartKey() {
        cartService.clear();
        verify(cartItemRepository).deleteByCartKey(TEST_USER);
    }

    // ===========================================================
    // TOTAL TESTS
    // ===========================================================

    @Test
    @DisplayName("Get total — sums price * quantity for all items")
    void getTotal_SumsCorrectly() {
        com.springboot.jenka_coffee.entity.CartItem item1 = buildCartItemEntity(1, 2); // 50000 * 2
        item1.setPriceSnapshot(new BigDecimal("50000"));

        com.springboot.jenka_coffee.entity.CartItem item2 = buildCartItemEntity(2, 3); // 30000 * 3
        item2.setPriceSnapshot(new BigDecimal("30000"));

        when(cartItemRepository.findByCartKey(TEST_USER)).thenReturn(List.of(item1, item2));

        BigDecimal total = cartService.getTotal();
        assertEquals(new BigDecimal("190000"), total); // 100000 + 90000
    }

    @Test
    @DisplayName("Get total with empty cart — returns 0")
    void getTotal_EmptyCart_ReturnsZero() {
        when(cartItemRepository.findByCartKey(TEST_USER)).thenReturn(List.of());
        assertEquals(BigDecimal.ZERO, cartService.getTotal());
    }

    // ===========================================================
    // HELPER
    // ===========================================================

    private com.springboot.jenka_coffee.entity.CartItem buildCartItemEntity(int productId, int quantity) {
        com.springboot.jenka_coffee.entity.CartItem item = new com.springboot.jenka_coffee.entity.CartItem();
        item.setCartKey(TEST_USER);
        item.setProductId(productId);
        item.setQuantity(quantity);
        item.setPriceSnapshot(new BigDecimal("50000"));
        item.setProductName("Product " + productId);
        item.setProductImage("img" + productId + ".jpg");
        return item;
    }
}
