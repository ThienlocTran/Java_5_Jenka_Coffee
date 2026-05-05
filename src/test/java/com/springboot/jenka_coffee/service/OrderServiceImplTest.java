package com.springboot.jenka_coffee.service;

import com.springboot.jenka_coffee.dto.request.CheckoutRequest;
import com.springboot.jenka_coffee.dto.response.CartItem;
import com.springboot.jenka_coffee.entity.Account;
import com.springboot.jenka_coffee.entity.Order;
import com.springboot.jenka_coffee.entity.Product;
import com.springboot.jenka_coffee.exception.BusinessRuleException;
import com.springboot.jenka_coffee.exception.ResourceNotFoundException;
import com.springboot.jenka_coffee.repository.OrderRepository;
import com.springboot.jenka_coffee.repository.PointHistoryRepository;
import com.springboot.jenka_coffee.service.impl.OrderServiceImpl;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test Case Document Part 1: ORDER MODULE - Service Layer
 * Tests for OrderServiceImpl business logic
 * 
 * Coverage:
 * - TC-ORD-003: Product unavailable
 * - TC-ORD-004: Max order value exceeded
 * - TC-ORD-007: XSS in note
 * - TC-ORD-013: FSM CONFIRMED → NEW invalid
 * - TC-ORD-014: FSM CANCELLED → any invalid
 * - TC-ORD-015: Cancel order refunds points
 * - TC-ORD-016: Price from DB, not cart
 * - TC-ORD-017: Address format validation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Order Service Implementation Tests")
class OrderServiceImplTest {

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

    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(
                orderRepository,
                cartService,
                emailService,
                entityManager,
                pointHistoryRepository
        );
    }

    // ========== HELPER METHODS ==========

    private CheckoutRequest buildValidCheckoutRequest() {
        CheckoutRequest request = new CheckoutRequest();
        request.setFullname("Nguyen Van A");
        request.setEmail("nva@gmail.com");
        request.setPhone("0912345678");
        request.setAddress("123 Nguyễn Trãi");
        request.setWard("Phường 1");
        request.setDistrict("Quận 1");
        request.setProvince("TP.HCM");
        request.setPaymentMethod("cod");
        request.setAgreeTerms(true);
        return request;
    }

    private Product buildProduct(Integer id, BigDecimal price, boolean available) {
        Product product = new Product();
        product.setId(id);
        product.setName("Product " + id);
        product.setPrice(price);
        product.setAvailable(available);
        product.setImage("/images/product" + id + ".jpg");
        return product;
    }

    private Account buildAccount(String username) {
        Account account = new Account();
        account.setUsername(username);
        account.setFullname("Test User");
        account.setEmail(username + "@test.com");
        account.setPoints(0);
        return account;
    }

    // ========== TEST CASES ==========

    /**
     * TC-ORD-003 — Checkout khi product bị unavailable
     * Expected: BusinessRuleException với message chứa "không còn kinh doanh"
     */
    @Test
    @DisplayName("TC-ORD-003: Checkout khi product available=false phải throw BusinessRuleException")
    void checkout_productUnavailable_throwsBusinessRuleException() {
        // Arrange
        CartItem item = new CartItem(5, "Máy xay cũ", "/images/product5.jpg", 
                new BigDecimal("1000000"), 1);
        when(cartService.getItems()).thenReturn(List.of(item));

        Account account = buildAccount("user_test");
        when(entityManager.find(eq(Account.class), eq("user_test"), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(account);

        Product unavailableProduct = buildProduct(5, new BigDecimal("1000000"), false);
        unavailableProduct.setName("Máy xay cũ");
        when(entityManager.find(eq(Product.class), eq(5), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(unavailableProduct);

        CheckoutRequest request = buildValidCheckoutRequest();

        // Act & Assert
        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> {
            orderService.checkout(request, account);
        });

        assertTrue(exception.getMessage().contains("Máy xay cũ"));
        assertTrue(exception.getMessage().contains("không còn kinh doanh"));
    }

    /**
     * TC-ORD-004 — Checkout vượt giới hạn 500 triệu VND
     * Expected: BusinessRuleException với message chứa "vượt quá giới hạn"
     */
    @Test
    @DisplayName("TC-ORD-004: Checkout với totalAmount > 500,000,000 VND phải bị reject")
    void checkout_exceedsMaxOrderValue_throwsBusinessRuleException() {
        // Arrange
        CartItem item = new CartItem(1, "Máy pha siêu đắt", "/images/product1.jpg", 
                new BigDecimal("600000000"), 1);
        when(cartService.getItems()).thenReturn(List.of(item));

        Account account = buildAccount("user_test");
        when(entityManager.find(eq(Account.class), eq("user_test"), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(account);

        Product expensiveProduct = buildProduct(1, new BigDecimal("600000000"), true);
        when(entityManager.find(eq(Product.class), eq(1), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(expensiveProduct);

        CheckoutRequest request = buildValidCheckoutRequest();

        // Act & Assert
        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> {
            orderService.checkout(request, account);
        });

        assertTrue(exception.getMessage().contains("vượt quá giới hạn"));
        assertTrue(exception.getMessage().contains("500"));
    }

    /**
     * TC-ORD-007 — Checkout với XSS trong note
     * Expected: note được escape, không execute script
     */
    @Test
    @DisplayName("TC-ORD-007: Checkout với XSS trong note phải được escape")
    void checkout_xssInNote_escapesHtml() {
        // Arrange
        CartItem item = new CartItem(1, "Product", "/images/product1.jpg", 
                new BigDecimal("100000"), 1);
        when(cartService.getItems()).thenReturn(List.of(item));

        Account account = buildAccount("user_test");
        when(entityManager.find(eq(Account.class), eq("user_test"), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(account);

        Product product = buildProduct(1, new BigDecimal("100000"), true);
        when(entityManager.find(eq(Product.class), eq(1), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(product);

        Order savedOrder = new Order();
        savedOrder.setId(1L);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        CheckoutRequest request = buildValidCheckoutRequest();
        request.setNote("<script>alert('xss')</script>Giao giờ hành chính");

        // Act
        Order result = orderService.checkout(request, account);

        // Assert
        assertNotNull(result);
        verify(orderRepository).save(argThat(order -> {
            String note = order.getNote();
            return note != null &&
                    note.contains("&lt;script&gt;") &&
                    note.contains("&lt;/script&gt;") &&
                    !note.contains("<script>");
        }));
    }

    /**
     * TC-ORD-013 — updateStatus — FSM: CONFIRMED → NEW không hợp lệ
     * Expected: BusinessRuleException
     */
    @Test
    @DisplayName("TC-ORD-013: Không thể chuyển CONFIRMED → NEW (invalid FSM transition)")
    void updateStatus_confirmedToNew_throwsBusinessRuleException() {
        // Arrange
        Order order = new Order();
        order.setId(77L);
        order.setStatus(1); // CONFIRMED

        when(entityManager.find(eq(Order.class), eq(77L), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(order);

        // Act & Assert
        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> {
            orderService.updateStatus(77L, 0); // Try to change to NEW
        });

        assertTrue(exception.getMessage().contains("Không thể chuyển trạng thái"));
        assertTrue(exception.getMessage().contains("CONFIRMED"));
        assertTrue(exception.getMessage().contains("NEW"));
    }

    /**
     * TC-ORD-014 — updateStatus — CANCELLED → bất kỳ đều bị block
     * Expected: BusinessRuleException for both attempts
     */
    @Test
    @DisplayName("TC-ORD-014: Đơn hàng CANCELLED không thể chuyển về bất kỳ trạng thái nào")
    void updateStatus_cancelledToAny_throwsBusinessRuleException() {
        // Arrange
        Order order = new Order();
        order.setId(88L);
        order.setStatus(2); // CANCELLED

        when(entityManager.find(eq(Order.class), eq(88L), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(order);

        // Act & Assert - Try to change to NEW
        assertThrows(BusinessRuleException.class, () -> {
            orderService.updateStatus(88L, 0);
        });

        // Try to change to CONFIRMED
        assertThrows(BusinessRuleException.class, () -> {
            orderService.updateStatus(88L, 1);
        });
    }

    /**
     * TC-ORD-015 — updateStatus CANCEL — hoàn điểm cho user
     * Expected: Account points = 200 + 500 = 700, PointHistory record created
     */
    @Test
    @DisplayName("TC-ORD-015: Hủy đơn hàng phải hoàn điểm cho user")
    void updateStatus_cancelWithPoints_refundsPointsToAccount() {
        // Arrange
        Account account = buildAccount("user_x");
        account.setPoints(200);

        Order order = new Order();
        order.setId(99L);
        order.setStatus(0); // NEW
        order.setAccount(account);
        order.setPointsUsed(500);

        when(entityManager.find(eq(Order.class), eq(99L), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(order);
        when(entityManager.find(eq(Account.class), eq("user_x"), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(account);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // Act
        orderService.updateStatus(99L, 2); // CANCELLED

        // Assert
        assertEquals(700, account.getPoints());
        verify(pointHistoryRepository).save(argThat(history ->
                history.getAmount() == 500 &&
                        history.getOrderId().equals(99L) &&
                        history.getReason().contains("Hoàn điểm") &&
                        history.getReason().contains("#99")
        ));
        verify(entityManager).merge(account);
    }

    /**
     * TC-ORD-016 — totalAmount tính từ DB price, không từ cart
     * Expected: Order totalAmount = 5,500,000 (từ DB), không phải 1,000 (từ cart)
     */
    @Test
    @DisplayName("TC-ORD-016: Giá trong totalAmount phải lấy từ DB, không từ cart")
    void checkout_priceFromDatabase_notFromCart() {
        // Arrange
        // Cart gửi giá giả: 1,000 VND
        CartItem fakeItem = new CartItem(3, "Product", "/images/product3.jpg", 
                new BigDecimal("1000"), 1);
        when(cartService.getItems()).thenReturn(List.of(fakeItem));

        Account account = buildAccount("user_test");
        when(entityManager.find(eq(Account.class), eq("user_test"), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(account);

        // DB có giá thật: 5,500,000 VND
        Product realProduct = buildProduct(3, new BigDecimal("5500000"), true);
        when(entityManager.find(eq(Product.class), eq(3), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(realProduct);

        Order savedOrder = new Order();
        savedOrder.setId(1L);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        CheckoutRequest request = buildValidCheckoutRequest();

        // Act
        Order result = orderService.checkout(request, account);

        // Assert
        verify(orderRepository).save(argThat(order -> {
            BigDecimal total = order.getTotalAmount();
            // Total phải là 5,500,000 (từ DB), không phải 1,000 (từ cart)
            return total != null && total.compareTo(new BigDecimal("5500000")) == 0;
        }));
    }

    /**
     * TC-ORD-017 — buildOrder — address được concat đúng format
     * Expected: address = "123 Nguyễn Trãi, Phường 1, Quận 1, TP.HCM"
     */
    @Test
    @DisplayName("TC-ORD-017: Address phải được concat đúng format")
    void buildOrder_addressFormat_correctlyConcatenated() {
        // Arrange
        CartItem item = new CartItem(1, "Product", "/images/product1.jpg", 
                new BigDecimal("100000"), 1);
        when(cartService.getItems()).thenReturn(List.of(item));

        Account account = buildAccount("user_test");
        when(entityManager.find(eq(Account.class), eq("user_test"), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(account);

        Product product = buildProduct(1, new BigDecimal("100000"), true);
        when(entityManager.find(eq(Product.class), eq(1), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(product);

        Order savedOrder = new Order();
        savedOrder.setId(1L);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        CheckoutRequest request = buildValidCheckoutRequest();

        // Act
        orderService.checkout(request, account);

        // Assert
        verify(orderRepository).save(argThat(order -> {
            String address = order.getAddress();
            return address != null &&
                    address.equals("123 Nguyễn Trãi, Phường 1, Quận 1, TP.HCM");
        }));
    }
    @Test
    @DisplayName("TC-ORD-SER-001: updateStatus trên order không tồn tại phải throw RuntimeException")
    void updateStatus_orderNotFound_throwsRuntimeException() {
        // Arrange
        when(entityManager.find(eq(Order.class), eq(999L), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(null);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.updateStatus(999L, 1);
        });

        assertEquals("Không tìm thấy đơn hàng!", exception.getMessage());
    }

    @Test
    @DisplayName("TC-ORD-SER-002: Hủy đơn hàng đã confirmed (GAP CHECK)")
    void updateStatus_cancelConfirmedOrder_gapCheck() {
        // Business Rule trong CSV yêu cầu không cho phép hủy đơn đã CONFIRMED (phải throw BusinessRuleException)
        // NHƯNG code hiện tại cho phép: case CONFIRMED -> to == Order.OrderStatus.CANCELLED (trả về true)
        // Đây là một GAP giữa Requirement và Implementation.
        Order order = new Order();
        order.setId(111L);
        order.setStatus(1); // CONFIRMED

        when(entityManager.find(eq(Order.class), eq(111L), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(order);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // Act - Không throw exception mà chạy thành công -> Gap confirmed
        assertDoesNotThrow(() -> {
            orderService.updateStatus(111L, 2); // CANCELLED
        });
        
        verify(orderRepository).save(argThat(o -> o.getStatus() == 2));
    }

    @Test
    @DisplayName("TC-ORD-SER-003: findByIdWithDetails với id không tồn tại throw ResourceNotFoundException")
    void findByIdWithDetails_notFound_throwsResourceNotFoundException() {
        // Arrange
        when(orderRepository.findByIdWithDetails(888L)).thenReturn(java.util.Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            orderService.findByIdWithDetails(888L);
        });

        assertTrue(exception.getMessage().contains("Không tìm thấy đơn hàng #888"));
    }
}
