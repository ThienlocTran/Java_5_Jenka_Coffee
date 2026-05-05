package com.springboot.jenka_coffee.api.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.jenka_coffee.entity.Account;
import com.springboot.jenka_coffee.entity.Order;
import com.springboot.jenka_coffee.entity.OrderDetail;
import com.springboot.jenka_coffee.exception.BusinessRuleException;
import com.springboot.jenka_coffee.exception.ResourceNotFoundException;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Admin Order Controller Tests")
class ApiAdminOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    private Order buildMockOrder(Long id, int status) {
        Order order = new Order();
        order.setId(id);
        order.setStatus(status);
        order.setTotalAmount(new BigDecimal("150000"));
        order.setCreateDate(LocalDateTime.now());
        
        Account account = new Account();
        account.setUsername("testuser");
        account.setFullname("Test User");
        account.setPhone("0123456789");
        order.setAccount(account);
        
        return order;
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ORD-CTRL-001: test_getOrders_validRequest_returns200")
    void test_getOrders_validRequest_returns200() throws Exception {
        Order order = buildMockOrder(1L, 0);
        Page<Order> page = new PageImpl<>(List.of(order));
        
        when(orderService.findAll(any(Pageable.class))).thenReturn(page);
        when(orderService.findAllWithAccountByIds(anyList())).thenReturn(List.of(order));

        mockMvc.perform(get("/api/admin/orders")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.totalItems").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ORD-CTRL-002: test_getOrders_negativePage_autoCorrectsTo0")
    void test_getOrders_negativePage_autoCorrectsTo0() throws Exception {
        Order order = buildMockOrder(1L, 0);
        Page<Order> page = new PageImpl<>(List.of(order));
        
        when(orderService.findAll(any(Pageable.class))).thenReturn(page);
        when(orderService.findAllWithAccountByIds(anyList())).thenReturn(List.of(order));

        mockMvc.perform(get("/api/admin/orders")
                .param("page", "-3")
                .param("size", "10"))
                .andExpect(status().isOk());
                
        verify(orderService).findAll(argThat(p -> p.getPageNumber() == 0));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ORD-CTRL-006: test_getOrderDetail_validId_returns200")
    void test_getOrderDetail_validId_returns200() throws Exception {
        Order order = buildMockOrder(1L, 0);
        order.setOrderDetails(List.of(new OrderDetail()));
        
        when(orderService.findByIdWithDetails(1L)).thenReturn(order);

        mockMvc.perform(get("/api/admin/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(1L));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ORD-CTRL-007: test_getOrderDetail_notFound_returns404")
    void test_getOrderDetail_notFound_returns404() throws Exception {
        when(orderService.findByIdWithDetails(9999L)).thenThrow(new ResourceNotFoundException("Not found"));

        mockMvc.perform(get("/api/admin/orders/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("ERROR"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ORD-CTRL-008: test_updateOrderStatus_toConfirmed_returns200")
    void test_updateOrderStatus_toConfirmed_returns200() throws Exception {
        doNothing().when(orderService).updateStatus(1L, 1);

        mockMvc.perform(put("/api/admin/orders/1/status/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ORD-CTRL-010: test_updateOrderStatus_invalidNegativeStatus_returns400")
    void test_updateOrderStatus_invalidNegativeStatus_returns400() throws Exception {
        mockMvc.perform(put("/api/admin/orders/1/status/-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("Trạng thái đơn hàng không hợp lệ (0: Mới, 1: Đã xác nhận, 2: Đã hủy)"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ORD-CTRL-012: test_updateOrderStatus_notFound_returns500")
    void test_updateOrderStatus_notFound_returns500() throws Exception {
        // Gap check: Currently throws RuntimeException which is mapped to 500 instead of 404
        doThrow(new RuntimeException("Không tìm thấy đơn hàng!")).when(orderService).updateStatus(9999L, 1);

        mockMvc.perform(put("/api/admin/orders/9999/status/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("Lỗi khi cập nhật trạng thái"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC-ORD-CTRL-013: test_updateOrderStatus_alreadyCancelled_returns400")
    void test_updateOrderStatus_alreadyCancelled_returns400() throws Exception {
        doThrow(new BusinessRuleException("Không thể chuyển trạng thái từ CANCELLED")).when(orderService).updateStatus(1L, 1);

        mockMvc.perform(put("/api/admin/orders/1/status/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"));
    }
}
