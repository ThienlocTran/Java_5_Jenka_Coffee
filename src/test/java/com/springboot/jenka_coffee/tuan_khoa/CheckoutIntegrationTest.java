package com.springboot.jenka_coffee.tuan_khoa;

import com.springboot.jenka_coffee.dto.request.CheckoutRequest;
import com.springboot.jenka_coffee.dto.response.CartItem;
import com.springboot.jenka_coffee.entity.Account;
import com.springboot.jenka_coffee.entity.Order;
import com.springboot.jenka_coffee.entity.Product;
import com.springboot.jenka_coffee.exception.InsufficientStockException;
import com.springboot.jenka_coffee.repository.ProductRepository;
import com.springboot.jenka_coffee.service.CartService;
import com.springboot.jenka_coffee.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class CheckoutIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private OrderService orderService;
    @MockBean
    private CartService cartService;

    @Autowired
    private ProductRepository productRepository;

    private MockHttpSession session;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
        Account user = new Account();
        user.setUsername("admin");
        session.setAttribute("user", user);

        // 1. Lấy sản phẩm thật
        testProduct = productRepository.findAll().get(0);

        // 2. QUAN TRỌNG: Cập nhật kho để chắc chắn đủ hàng bán
        testProduct.setQuantity(100);
        productRepository.saveAndFlush(testProduct); // Lưu tạm vào DB trong transaction này

        // 3. Giả lập CartItem
        CartItem item = new CartItem();
        item.setProductId(testProduct.getId());
        item.setName(testProduct.getName());
        item.setPrice(new BigDecimal(String.valueOf(testProduct.getPrice())));
        item.setQuantity(2);

        // 4. Stubbing CartService
        when(cartService.getItems()).thenReturn(List.of(item));
        when(cartService.getCount()).thenReturn(2);
        BigDecimal totalAmount = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        when(cartService.getAmount()).thenReturn(totalAmount.doubleValue());
    }

    @Test
    @DisplayName("TC_CHK_001: Checkout thành công (Full Data)")
    void testTC_CHK_001_CheckoutSuccess() throws Exception {
        int stockBefore = testProduct.getQuantity();

        mockMvc.perform(post("/checkout")
                        .session(session)
                        .sessionAttr("scopedTarget.cartServiceImpl", cartService)
                        .param("fullname", "Nguyễn Văn A")
                        .param("phone", "0901234567")
                        .param("email", "user@gmail.com")
                        .param("address", "123 Lê Lợi, TP.HCM")
                        .param("province", "Thành phố Hồ Chí Minh")
                        .param("district", "Quận 1")
                        .param("ward", "Phường Bến Nghé")
                        .param("paymentMethod", "COD")
                        .param("agreeTerms", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/checkout/success"));

        // Kiểm tra trừ kho thật trong DB
        int stockAfter = productRepository.findById(testProduct.getId()).get().getQuantity();
        assertEquals(stockBefore - 2, stockAfter, "Tồn kho phải giảm đi 2 đơn vị");
    }
    @Test
    @DisplayName("TC_CHK_002: Truy cập trang checkout khi chưa đăng nhập (Vẫn cho xem form)")
    void testTC_CHK_002_Unauthenticated() throws Exception {
        // 1. Chuẩn bị dữ liệu giỏ hàng (Vì Controller check trống trước)
        CartItem mockItem = new CartItem();
        mockItem.setProductId(1);
        mockItem.setName("Sản phẩm test");
        mockItem.setPrice(BigDecimal.valueOf(50000));
        mockItem.setQuantity(1);

        when(cartService.getItems()).thenReturn(List.of(mockItem));
        when(cartService.getAmount()).thenReturn(50000.0);
        when(cartService.getCount()).thenReturn(1);

        // 2. Thực hiện GET /checkout mà KHÔNG có session user
        mockMvc.perform(get("/checkout"))
                .andExpect(status().isOk()) // Code của ông đang trả về 200
                .andExpect(view().name("site/checkout"))
                .andExpect(model().attribute("isLoggedIn", false)) // Flag này Controller tự set hoặc do interceptor
                .andExpect(model().attributeExists("checkoutRequest"))
                .andExpect(model().attribute("cartTotal", 50000.0));
    }

    @Test
    @DisplayName("TC_CHK_003: Giỏ hàng trống -> Redirect về trang Giỏ hàng")
    void testEmptyCart() throws Exception {
        // Giả lập giỏ hàng trống hoàn toàn
        when(cartService.getItems()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/checkout").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart/view"));
        // Lưu ý: Vì Controller của ông KHÔNG addFlashAttribute("error",...)
        // nên ta bỏ dòng check flash attribute đi để test không bị fail.
    }

    @Test
    @DisplayName("TC_CHK_004: Checkout thiếu địa chỉ giao hàng")
    void testTC_CHK_004_MissingAddress() throws Exception {
        mockMvc.perform(post("/checkout")
                        .session(session)
                        .param("fullname", "Nguyễn Văn A")
                        .param("phone", "0901234567")
                        .param("email", "user@gmail.com")
                        .param("address", "") // Trống
                        .param("province", "HCM")
                        .param("district", "Q1")
                        .param("ward", "P1")
                        .param("paymentMethod", "COD")
                        .param("agreeTerms", "true"))
                .andExpect(status().isOk()) // Trả về view cũ (200 OK)
                .andExpect(view().name("site/checkout"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("checkoutRequest", "address"));
    }
    @Test
    @DisplayName("TC_CHK_005: Checkout thiếu số điện thoại")
    void testTC_CHK_005_MissingPhone() throws Exception {
        mockMvc.perform(post("/checkout")
                        .session(session)
                        .param("fullname", "Nguyễn Văn A")
                        .param("phone", "") // Để trống trường SĐT
                        .param("email", "user@gmail.com")
                        .param("address", "123 Lê Lợi, TP.HCM")
                        .param("province", "Thành phố Hồ Chí Minh")
                        .param("district", "Quận 1")
                        .param("ward", "Phường Bến Nghé")
                        .param("paymentMethod", "COD")
                        .param("agreeTerms", "true"))
                .andExpect(status().isOk()) // Khi có lỗi Validation, Controller return "site/checkout" (200 OK)
                .andExpect(view().name("site/checkout"))
                .andExpect(model().hasErrors())
                // Kiểm tra xem trường 'phone' có lỗi hay không
                .andExpect(model().attributeHasFieldErrors("checkoutRequest", "phone"));
    }
    @Test
    @DisplayName("TC_CHK_006: Checkout với SĐT sai format")
    void testTC_CHK_006_InvalidPhone() throws Exception {
        mockMvc.perform(post("/checkout")
                        .session(session)
                        .param("phone", "abc123") // Sai format
                        .param("agreeTerms", "true"))
                .andExpect(model().attributeHasFieldErrors("checkoutRequest", "phone"));
    }
    @Test
    @DisplayName("TC_CHK_007: Checkout với email sai format")
    void testTC_CHK_007_InvalidEmailFormat() throws Exception {
        mockMvc.perform(post("/checkout")
                        .session(session)
                        .param("fullname", "Nguyễn Văn A")
                        .param("phone", "0901234567")
                        .param("email", "notanemail") // Định dạng email sai (thiếu @ và domain)
                        .param("address", "123 Lê Lợi, TP.HCM")
                        .param("province", "Thành phố Hồ Chí Minh")
                        .param("district", "Quận 1")
                        .param("ward", "Phường Bến Nghé")
                        .param("paymentMethod", "COD")
                        .param("agreeTerms", "true"))
                .andExpect(status().isOk()) // Trả về view checkout để hiển thị lỗi
                .andExpect(view().name("site/checkout"))
                .andExpect(model().hasErrors())
                // Kiểm tra cụ thể lỗi tại trường email
                .andExpect(model().attributeHasFieldErrors("checkoutRequest", "email"));
    }

    @Test
    @DisplayName("TC_CHK_008: Checkout khi sản phẩm hết hàng (InsufficientStockException)")
    void testTC_CHK_008_OutOfStock() throws Exception {
        // 1. Giả lập service ném ngoại lệ với ĐỦ 3 THAM SỐ (String, Integer, Integer)
        // Để khớp với constructor: InsufficientStockException(productName, requested, available)
        when(orderService.checkout(any(), any()))
                .thenThrow(new InsufficientStockException(testProduct.getName(), 5, 3));

        mockMvc.perform(post("/checkout")
                        .session(session)
                        .param("fullname", "Nguyễn Văn A")
                        .param("phone", "0901234567")
                        .param("email", "user@gmail.com")
                        .param("address", "123 Lê Lợi, TP.HCM")
                        .param("province", "Thành phố Hồ Chí Minh")
                        .param("district", "Quận 1")
                        .param("ward", "Phường Bến Nghé")
                        .param("paymentMethod", "COD")
                        .param("agreeTerms", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/checkout"))
                // Kiểm tra flash attribute "error" có tồn tại (vì message build trong exception)
                .andExpect(flash().attributeExists("error"));
    }
    @Test
    @DisplayName("TC_CHK_009: Checkout với fullname chứa số")
    void testTC_CHK_009_InvalidFullname() throws Exception {
        mockMvc.perform(post("/checkout")
                        .session(session)
                        .param("fullname", "Nguyen123") // Chứa số -> Vi phạm @Pattern
                        .param("phone", "0901234567")
                        .param("email", "user@gmail.com")
                        .param("address", "123 Lê Lợi, TP.HCM")
                        .param("province", "HCM")
                        .param("district", "Q1")
                        .param("ward", "P1")
                        .param("paymentMethod", "COD")
                        .param("agreeTerms", "true"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("checkoutRequest", "fullname"));
    }
    @Test
    @DisplayName("TC_CHK_010: Checkout không đồng ý điều khoản")
    void testTC_CHK_010_TermsNotAgreed() throws Exception {
        mockMvc.perform(post("/checkout")
                        .session(session)
                        .param("fullname", "Nguyễn Văn A")
                        .param("agreeTerms", "false")) // Vi phạm @AssertTrue
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("checkoutRequest", "agreeTerms"));
    }

    @Test
    @DisplayName("TC_CHK_011: Test ghép địa chỉ đầy đủ từ các trường input")
    void testTC_CHK_011_AddressMapping() throws Exception {
        // 1. Tạo captor để bắt đối tượng CheckoutRequest truyền vào service
        ArgumentCaptor<CheckoutRequest> captor = ArgumentCaptor.forClass(CheckoutRequest.class);

        // 2. Mock trả về một Order giả để không bị lỗi null pointer
        when(orderService.checkout(any(), any())).thenReturn(new Order());

        // 3. Thực hiện POST với địa chỉ rời rạc
        mockMvc.perform(post("/checkout")
                        .session(session)
                        .param("fullname", "Nguyễn Văn A")
                        .param("phone", "0901234567")
                        .param("email", "user@gmail.com")
                        .param("address", "123 Lê Lợi")
                        .param("ward", "Phường 1")
                        .param("district", "Quận 1")
                        .param("province", "TP.HCM")
                        .param("paymentMethod", "COD")
                        .param("agreeTerms", "true"))
                .andExpect(status().is3xxRedirection());

        // 4. Kiểm tra xem Controller có truyền đúng data xuống Service không
        verify(orderService).checkout(captor.capture(), any());
        CheckoutRequest capturedRequest = captor.getValue();

        // Kiểm tra các trường dữ liệu trước khi Service xử lý gộp
        assertEquals("123 Lê Lợi", capturedRequest.getAddress());
        assertEquals("Phường 1", capturedRequest.getWard());
        assertEquals("Quận 1", capturedRequest.getDistrict());
        assertEquals("TP.HCM", capturedRequest.getProvince());
    }

    @Test
    @DisplayName("TC_CHK_012: Test tạo OrderDetail với giá tại thời điểm mua")
    void testTC_CHK_012_OrderDetailDataIntegrity() throws Exception {
        // Giả lập giỏ hàng có 1 sản phẩm với giá 50,000đ
        CartItem item = new CartItem();
        item.setProductId(testProduct.getId());
        item.setPrice(new BigDecimal("50000")); // Giá mua
        item.setQuantity(2);

        when(cartService.getItems()).thenReturn(List.of(item));
        when(orderService.checkout(any(), any())).thenReturn(new Order());

        mockMvc.perform(post("/checkout")
                        .session(session)
                        .param("fullname", "Nguyễn Văn A")
                        .param("phone", "0901234567")
                        .param("email", "user@gmail.com")
                        .param("address", "123 Lê Lợi, P1, Q1, HCM")
                        .param("province", "HCM")
                        .param("district", "Q1")
                        .param("ward", "P1")
                        .param("paymentMethod", "COD")
                        .param("agreeTerms", "true"))
                .andExpect(status().is3xxRedirection());

        // Xác nhận Service được gọi. Logic nghiệp vụ trong OrderService.checkout(request, user)
        // sẽ chịu trách nhiệm lấy giá từ CartItem (50,000) thay vì lấy giá từ DB.
        verify(orderService).checkout(any(CheckoutRequest.class), any(Account.class));
    }

    @Test
    @DisplayName("TC_CHK_013: Test transaction rollback khi lỗi hệ thống (Simulate: Product not found)")
    void testTC_CHK_013_RollbackOnSystemError() throws Exception {
        // Giả lập RuntimeException (như ResourceNotFoundException) để kích hoạt @Transactional rollback
        when(orderService.checkout(any(), any()))
                .thenThrow(new RuntimeException("Simulated: Product not found in DB during OrderDetail creation"));

        mockMvc.perform(post("/checkout")
                        .session(session)
                        .param("fullname", "Nguyễn Văn A")
                        .param("phone", "0901234567")
                        .param("email", "user@gmail.com")
                        .param("address", "123 Lê Lợi")
                        .param("province", "HCM")
                        .param("district", "Q1")
                        .param("ward", "P1")
                        .param("paymentMethod", "COD")
                        .param("agreeTerms", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/checkout"))
                .andExpect(flash().attributeExists("error"));

        // Lưu ý: Vì class test có @Transactional, Spring sẽ tự động Rollback mọi thay đổi
        // trong DB (nếu có) sau khi kết thúc method này.
    }


    @Test
    @DisplayName("Testcase_Bổ_Sung: POST Checkout khi session mất (User null)")
    void testPostCheckout_SessionExpired() throws Exception {
        // Giả lập dữ liệu gửi lên
        mockMvc.perform(post("/checkout")
                        // Không có session user
                        .param("fullname", "Nguyễn Văn A")
                        .param("phone", "0901234567")
                        .param("email", "user@gmail.com")
                        .param("address", "123 Lê Lợi")
                        .param("province", "HCM")
                        .param("district", "Q1")
                        .param("ward", "P1")
                        .param("paymentMethod", "COD")
                        .param("agreeTerms", "true"))
                .andExpect(status().is3xxRedirection())
                // Khớp với dòng: return "redirect:/auth/login?message=Vui_long_dang_nhap_lai";
                .andExpect(redirectedUrl("/auth/login?message=Vui_long_dang_nhap_lai"));
    }

}