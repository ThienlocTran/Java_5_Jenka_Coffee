package com.springboot.jenka_coffee.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.jenka_coffee.dto.request.CheckoutRequest;
import com.springboot.jenka_coffee.dto.response.CartItem;
import com.springboot.jenka_coffee.entity.Account;
import com.springboot.jenka_coffee.entity.Order;
import com.springboot.jenka_coffee.service.AccountService;
import com.springboot.jenka_coffee.service.CartService;
import com.springboot.jenka_coffee.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test Case Document Part 1: ORDER MODULE - Controller Layer
 * Tests for ApiOrderController endpoints

 * Coverage:
 * - TC-ORD-001: Checkout happy path
 * - TC-ORD-002: Checkout empty cart
 * - TC-ORD-005: Invalid payment method validation
 * - TC-ORD-006: AgreeTerms validation
 * - TC-ORD-008: IDOR protection
 * - TC-ORD-009: Guest order blocked
 * - TC-ORD-010: Order not found
 * - TC-ORD-011: Deep pagination DoS protection
 * - TC-ORD-012: Size capped at 20
 * - TC-ORD-018: Checkout-info empty cart
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)  // ✅ Disable RateLimitFilter for tests
@DisplayName("API Order Controller Tests")
class ApiOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CartService cartService;

    @MockBean
    private OrderService orderService;

    @MockBean
    private AccountService accountService;

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
     * TC-ORD-001 — Checkout thành công (Happy Path)
     * Expected: HTTP 200, status="SUCCESS", orderId > 0
     */
    @Test
    @WithMockUser(username = "nva@gmail.com")
    @DisplayName("TC-ORD-001: Checkout thành công với đủ thông tin hợp lệ")
    void checkout_happyPath_returns200WithOrderId() throws Exception {
        // Arrange
        CartItem item1 = new CartItem(1, "Máy pha DeLonghi", "/images/product1.jpg", 
                new BigDecimal("5500000"), 1);
        CartItem item2 = new CartItem(2, "Hạt Arabica", "/images/product2.jpg", 
                new BigDecimal("320000"), 2);
        when(cartService.getItems()).thenReturn(List.of(item1, item2));

        Account account = buildAccount("nva@gmail.com");
        when(accountService.findById("nva@gmail.com")).thenReturn(account);

        Order savedOrder = new Order();
        savedOrder.setId(101L);
        savedOrder.setTotalAmount(new BigDecimal("6140000"));

        when(orderService.checkout(any(CheckoutRequest.class), any(Account.class)))
                .thenReturn(savedOrder);
        doNothing().when(orderService).postCheckout(any(Order.class), any(Account.class));

        CheckoutRequest req = buildValidCheckoutRequest();

        // Act & Assert
        mockMvc.perform(post("/api/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Đặt hàng thành công! Mã đơn hàng: #101"))
                .andExpect(jsonPath("$.data.orderId").value(101L));

        verify(orderService).checkout(any(CheckoutRequest.class), eq(account));
        verify(orderService).postCheckout(eq(savedOrder), eq(account));
    }

    /**
     * TC-ORD-002 — Checkout với cart trống
     * Expected: HTTP 400, message="Giỏ hàng trống, không thể đặt hàng!"
     */
    @Test
    @WithMockUser(username = "nva@gmail.com")
    @DisplayName("TC-ORD-002: Checkout với cart trống phải throw IllegalStateException")
    void checkout_emptyCart_returns400() throws Exception {
        // Arrange
        when(cartService.getItems()).thenReturn(Collections.emptyList());
        Account account = buildAccount("nva@gmail.com");
        when(accountService.findById("nva@gmail.com")).thenReturn(account);
        when(orderService.checkout(any(CheckoutRequest.class), any(Account.class)))
                .thenThrow(new IllegalStateException("Giỏ hàng trống, không thể đặt hàng!"));

        CheckoutRequest req = buildValidCheckoutRequest();

        // Act & Assert
        mockMvc.perform(post("/api/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("Giỏ hàng trống, không thể đặt hàng!"));
    }

    /**
     * TC-ORD-005 — Checkout với paymentMethod không hợp lệ
     * Expected: HTTP 400, validation error
     */
    @Test
    @WithMockUser(username = "nva@gmail.com")
    @DisplayName("TC-ORD-005: Checkout với paymentMethod='bitcoin' phải fail Bean Validation")
    void checkout_invalidPaymentMethod_returns400() throws Exception {
        // Arrange
        CheckoutRequest req = buildValidCheckoutRequest();
        req.setPaymentMethod("bitcoin"); // Invalid payment method

        // Act & Assert
        mockMvc.perform(post("/api/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"));
    }

    /**
     * TC-ORD-006 — Checkout với agreeTerms=false
     * Expected: HTTP 400, validation error
     */
    @Test
    @WithMockUser(username = "nva@gmail.com")
    @DisplayName("TC-ORD-006: Checkout với agreeTerms=false phải fail @AssertTrue")
    void checkout_agreeTermsFalse_returns400() throws Exception {
        // Arrange
        CheckoutRequest req = buildValidCheckoutRequest();
        req.setAgreeTerms(false);

        // Act & Assert
        mockMvc.perform(post("/api/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"));
    }

    /**
     * TC-ORD-008 — GET /api/orders/{orderId} — IDOR protection
     * Expected: HTTP 403, message="Bạn không có quyền xem đơn hàng này"
     */
    @Test
    @WithMockUser(username = "user_b")
    @DisplayName("TC-ORD-008: User B không được xem order của user A (IDOR protection)")
    void getOrderDetail_belongsToOtherUser_returns403() throws Exception {
        // Arrange
        Order order = new Order();
        order.setId(55L);
        Account owner = buildAccount("user_a");
        order.setAccount(owner);
        order.setOrderDetails(new ArrayList<>());

        when(orderService.findById(55L)).thenReturn(order);

        // Act & Assert
        mockMvc.perform(get("/api/orders/55"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("Bạn không có quyền xem đơn hàng này"));
    }

    /**
     * TC-ORD-009 — GET /api/orders/{orderId} — guest order bị block
     * Expected: HTTP 403, message chứa "khách vãng lai"
     */
    @Test
    @WithMockUser(username = "any_user")
    @DisplayName("TC-ORD-009: Order của khách vãng lai (account=null) phải bị block")
    void getOrderDetail_guestOrder_returns403() throws Exception {
        // Arrange
        Order order = new Order();
        order.setId(66L);
        order.setAccount(null); // Guest order
        order.setOrderDetails(new ArrayList<>());

        when(orderService.findById(66L)).thenReturn(order);

        // Act & Assert
        mockMvc.perform(get("/api/orders/66"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("khách vãng lai")));
    }

    /**
     * TC-ORD-010 — GET /api/orders/{orderId} — không tìm thấy
     * Expected: HTTP 404, message="Không tìm thấy đơn hàng"
     */
    @Test
    @WithMockUser(username = "user_test")
    @DisplayName("TC-ORD-010: GET order không tồn tại phải trả 404")
    void getOrderDetail_notFound_returns404() throws Exception {
        // Arrange
        when(orderService.findById(9999L)).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/api/orders/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("Không tìm thấy đơn hàng"));
    }

    /**
     * TC-ORD-011 — GET /api/orders?page=1001 — Deep Pagination DoS
     * Expected: HTTP 400, message="Số trang không được vượt quá 1000"
     */
    @Test
    @WithMockUser(username = "user_test")
    @DisplayName("TC-ORD-011: GET orders với page > 1000 phải bị reject (DoS protection)")
    void getOrderHistory_deepPagination_returns400() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/orders")
                        .param("page", "1001")
                        .param("size", "5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("Số trang không được vượt quá 1000"));
    }

    /**
     * TC-ORD-012 — GET /api/orders?size=9999 — Size bị cap ở 20
     * Expected: HTTP 200, items.length <= 20
     */
    @Test
    @WithMockUser(username = "user_test")
    @DisplayName("TC-ORD-012: GET orders với size=9999 phải bị giới hạn tối đa 20")
    void getOrderHistory_largeSize_cappedAt20() throws Exception {
        // Arrange
        List<Order> orders = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            Order order = new Order();
            order.setId((long) i);
            orders.add(order);
        }
        Page<Order> page = new PageImpl<>(orders);

        when(orderService.findByUsername(eq("user_test"), any(Pageable.class)))
                .thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/orders")
                        .param("size", "9999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(org.hamcrest.Matchers.lessThanOrEqualTo(20)));

        // Verify that size was capped to 20
        verify(orderService).findByUsername(eq("user_test"), argThat(pageable ->
                pageable.getPageSize() == 20
        ));
    }

    /**
     * TC-ORD-018 — GET /api/orders/checkout-info khi cart trống
     * Expected: HTTP 400, message="Giỏ hàng trống"
     */
    @Test
    @WithMockUser(username = "user_test")
    @DisplayName("TC-ORD-018: GET checkout-info với cart trống phải trả 400")
    void getCheckoutInfo_emptyCart_returns400() throws Exception {
        // Arrange
        when(cartService.getItems()).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/orders/checkout-info"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("Giỏ hàng trống"));
    }
}
