package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.dto.request.CheckoutRequest;
import com.springboot.jenka_coffee.dto.response.CartItem;
import com.springboot.jenka_coffee.entity.*;
import com.springboot.jenka_coffee.repository.OrderRepository;
import com.springboot.jenka_coffee.repository.PointHistoryRepository;
import com.springboot.jenka_coffee.service.CartService;
import com.springboot.jenka_coffee.service.EmailService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CartService cartService;
    @Mock
    private EmailService emailService;
    @Mock
    private EntityManager entityManager;
    @Mock
    private PointHistoryRepository pointHistoryRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Account testAccount;
    private Product testProduct;
    private CartItem testCartItem;

    @BeforeEach
    void setUp() {
        testAccount = new Account();
        testAccount.setUsername("testuser");
        testAccount.setFullname("Test User");
        testAccount.setPoints(100);

        testProduct = new Product();
        testProduct.setId(1);
        testProduct.setName("Coffee");
        testProduct.setPrice(new BigDecimal("50000"));
        testProduct.setAvailable(true);

        testCartItem = new CartItem();
        testCartItem.setProductId(1);
        testCartItem.setName("Coffee");
        testCartItem.setPrice(new BigDecimal("50000"));
        testCartItem.setQuantity(2);
    }

    @Test
    @DisplayName("Test checkout - Success")
    void checkout_Success() {
        CheckoutRequest request = new CheckoutRequest();
        request.setPhone("0123456789");
        request.setAddress("123 Street");
        request.setProvince("Hanoi");
        request.setDistrict("Hoan Kiem");
        request.setWard("Hang Bac");

        when(cartService.getItems()).thenReturn(Collections.singletonList(testCartItem));
        when(entityManager.find(eq(Account.class), eq("testuser"), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(testAccount);
        when(entityManager.find(eq(Product.class), eq(1), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(testProduct);
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> {
            Order o = i.getArgument(0);
            o.setId(100L);
            return o;
        });

        Order result = orderService.checkout(request, testAccount);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertTrue(new BigDecimal("100000").compareTo(result.getTotalAmount()) == 0);
        verify(cartService).clear();
        verify(orderRepository).save(any(Order.class));
        verify(entityManager).persist(any(Payment.class));
    }

    @Test
    @DisplayName("Test checkout - Empty Cart")
    void checkout_EmptyCart() {
        when(cartService.getItems()).thenReturn(Collections.emptyList());
        CheckoutRequest request = new CheckoutRequest();

        assertThrows(IllegalStateException.class, () -> {
            orderService.checkout(request, testAccount);
        });
    }

    @Test
    @DisplayName("Test updateStatus - Success transition to CONFIRMED")
    void updateStatus_Success() {
        Order order = new Order();
        order.setId(1L);
        order.setStatus(0); // NEW

        when(entityManager.find(eq(Order.class), eq(1L), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(order);
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        orderService.updateStatus(1L, 1); // 1 = CONFIRMED

        assertEquals(1, order.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("Test updateStatus - Success transition to CANCELLED and Refund Points")
    void updateStatus_CancelAndRefund() {
        Order order = new Order();
        order.setId(1L);
        order.setStatus(0); // NEW
        order.setAccount(testAccount);
        order.setPointsUsed(50);

        when(entityManager.find(eq(Order.class), eq(1L), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(order);
        when(entityManager.find(eq(Account.class), eq("testuser"), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(testAccount);

        orderService.updateStatus(1L, 2); // 2 = CANCELLED (in new branch)

        assertEquals(2, order.getStatus());
        assertEquals(150, testAccount.getPoints());
        verify(pointHistoryRepository).save(any(PointHistory.class));
    }
}
