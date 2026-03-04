package com.springboot.jenka_coffee.tuan_khoa;

import com.springboot.jenka_coffee.entity.Account;
import com.springboot.jenka_coffee.entity.Order;
import com.springboot.jenka_coffee.repository.OrderRepository;
import com.springboot.jenka_coffee.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class OrderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @MockBean
    private OrderRepository orderRepository;

    private MockHttpSession userSession;
    private MockHttpSession adminSession;
    private Account mockUser;

    @BeforeEach
    void setUp() {
        //user
        userSession = new MockHttpSession();
        mockUser = new Account();
        mockUser.setUsername("user");
        userSession.setAttribute("user", mockUser);

        //admin
        adminSession = new MockHttpSession();
        Account admin = new Account();
        admin.setUsername("admin");
        admin.setAdmin(true); // Cấp quyền Admin
        adminSession.setAttribute("user", admin);
    }

    @Test
    @DisplayName("TC_ORD_001: User xem lịch sử đơn hàng")
    void testTC_ORD_001_UserOrderList() throws Exception {
        // 1. Tạo một Page ảo chứa 3 đơn hàng
        List<Order> fakeOrders = List.of(new Order(), new Order(), new Order());
        Page<Order> mockPage = new PageImpl<>(fakeOrders);

        // 2. QUAN TRỌNG: Mock đúng tên "user" (trùng với session user đang login)
        when(orderService.findByUsername(eq("user"), any(Pageable.class))).thenReturn(mockPage);

        // 3. Thực hiện GET request
        mockMvc.perform(get("/order/list").session(userSession))
                .andExpect(status().isOk())
                .andExpect(view().name("site/list")) // Trả về view đúng như Controller
                // Kiểm tra 3 attributes mà Controller ném ra
                .andExpect(model().attributeExists("orders"))
                .andExpect(model().attribute("currentPage", 0))
                .andExpect(model().attribute("totalPages", 1)); // 3 phần tử thì ngầm định là 1 trang
    }

    @Test
    @DisplayName("TC_ORD_008: Lazy Load thêm đơn hàng (AJAX)")
    void testTC_ORD_008_AjaxLoadMore() throws Exception {
        // Có > 5 đơn hàng, mock trả về trang tiếp theo
        List<Order> fakeOrders = List.of(new Order(), new Order());
        Page<Order> mockPage = new PageImpl<>(fakeOrders);

        when(orderService.findByUsername(eq("user"), any(Pageable.class))).thenReturn(mockPage);

        mockMvc.perform(get("/order/load-more")
                        .session(userSession)
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("site/list :: order_rows"))
                .andExpect(model().attributeExists("orders"));
    }

}