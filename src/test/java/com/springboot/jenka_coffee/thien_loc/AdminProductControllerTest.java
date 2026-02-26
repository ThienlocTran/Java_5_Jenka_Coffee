package com.springboot.jenka_coffee.thien_loc;

import com.springboot.jenka_coffee.controller.admin.AdminProductController;
import com.springboot.jenka_coffee.entity.Account;
import com.springboot.jenka_coffee.entity.Category;
import com.springboot.jenka_coffee.entity.Product;
import com.springboot.jenka_coffee.interceptor.AuthInterceptor;
import com.springboot.jenka_coffee.service.*;
import com.springboot.jenka_coffee.util.ImageHelper;
import com.springboot.jenka_coffee.util.MessageHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

/**
 * 
 * 
 * @WebMvcTest: Báo cho Spring biết "Chỉ nạp Controller này thôi, bỏ qua toàn bộ
 *              Database & Service".
 *              Nó giúp Test chạy cực nhanh nhờ việc test cô lập phần điều hướng
 *              (Routing / View / Form).
 *
 *              includeFilters = ...AuthInterceptor: Yêu cầu Spring CHẮC CHẮN
 *              phải nạp thêm cái Interceptor
 *              chặn quyền đăng nhập của mình (vì mặc định nó cũng
 *              bị @WebMvcTest vứt đi mất).
 */
@WebMvcTest(controllers = AdminProductController.class, includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = AuthInterceptor.class))
public class AdminProductControllerTest {

    // Đây là cốt lõi của WebMvcTest: Trình duyệt Ảo giúp ta gọi API, bấm nút Submit
    // Form
    @Autowired
    private MockMvc mockMvc;

    // --- KHUYẾT THIẾU DEPENDENCY (@MockBean) ---
    // Do @WebMvcTest đã cắt bỏ toàn bộ @Service thật (để chạy nhanh không cần DB).
    // Nên ta PHẢI dùng @MockBean để cấp bù những phiên bản "Đồ giả" của các
    // Service,
    // vừa dùng để phục vụ Controller, vừa dùng để chữa lỗi
    // "UnsatisfiedDependencyException"
    // của các Interceptor/GlobalExceptionHandler.

    @MockBean
    private ProductService productService; // Dành cho AdminProductController

    @MockBean
    private CategoryService categoryService; // Dành cho AdminProductController

    // Mocking interceptor dependencies
    @MockBean
    private AccountService accountService; // Dành cho AuthInterceptor

    @MockBean
    private CookieService cookieService; // Dành cho AuthInterceptor

    @MockBean
    private CartService cartService; // Dành cho GlobalInterceptor (chặn mọi request)

    @MockBean
    private ImageHelper imageHelper; // Phục vụ Config/Component nào đó

    @MockBean
    private MessageHelper messageHelper; // Dành cho GlobalExceptionHandler

    private MockHttpSession session;

    /**
     * @BeforeEach: Hàm này sẽ tự động chạy TRƯỚC MỖI Test Case (@Test).
     *              Dùng để dọn dẹp hoặc chuẩn bị một môi trường sạch sẽ (Session
     *              đóng giả, Dữ liệu Mock cơ bản).
     */
    @BeforeEach
    void setUp() {
        // 1. Chuẩn bị 1 Session cục bộ để AuthInterceptor kiểm tra "Đã đăng nhập hay
        // chưa?"
        Account adminAccount = new Account();
        adminAccount.setUsername("admin");
        adminAccount.setAdmin(true);
        session = new MockHttpSession();
        session.setAttribute("user", adminAccount);

        // 2. Kịch bản yêu cầu: Category "CP" tồn tại trong DB.
        // Ta dùng Mockito bảo với CategoryService giả rằng: "Nếu ai đó tìm ID = CP, hãy
        // ném cái Category này ra"
        Category categoryCP = new Category();
        categoryCP.setId("CP");
        categoryCP.setName("Cà phê");
        Mockito.when(categoryService.findById("CP")).thenReturn(categoryCP);
        // Phục vụ cho việc load trang admin/products/form chứa danh sách Categories
        Mockito.when(categoryService.findAll()).thenReturn(java.util.List.of(categoryCP));
    }

    /**
     * KỊCH BẢN: Thêm Sản Phẩm (Happy Path)
     * 
     * @WithMockUser: Đây là "Thẻ Khách VIP" đưa cho Spring Security vòng ngoài.
     *                Báo cho Security biết: "Trong phiên Test này, tôi tên admin và
     *                có quyền ADMIN. Đừng chặn tôi bằng lỗi 401 Unauthorized"
     */
    @Test
    @DisplayName("TC_PROD_001: Thêm sản phẩm hợp lệ")
    @WithMockUser(username = "admin", roles = { "ADMIN" })
    void testSaveProduct_Success() throws Exception {

        // --- CHUẨN BỊ (ARRANGE) ---
        // 1. Tạo 1 file ảnh giả lập y như người dùng vừa ấn [Choose File] lên
        MockMultipartFile imageFile = new MockMultipartFile(
                "imageFile",
                "coffee_machine.jpg",
                "image/jpeg",
                "test image content".getBytes());

        // 2. Cài đặt kịch bản cho ProductService giả:
        // "Khi Controller gọi tao lưu sản phẩm và ảnh, tao giả bộ lưu xong và ném ra 1
        // Product có ID = 1"
        Product expectedSavedProduct = new Product();
        expectedSavedProduct.setId(1);
        expectedSavedProduct.setName("Máy pha cà phê Ý");
        Mockito.when(productService.saveProduct(any(Product.class), any(MockMultipartFile.class)))
                .thenReturn(expectedSavedProduct);

        // --- THỰC THI & KIỂM TRA (ACT & ASSERT) ---
        // Dùng Trình Duyệt Ảo (MockMvc) Submit Form tới đường dẫn /admin/product/save y
        // như thật
        mockMvc.perform(multipart("/admin/product/save")
                .file(imageFile) // Đính kèm ảnh
                .param("name", "Máy pha cà phê Ý") // Tương đương thẻ <input name="name">
                .param("price", "5000000")
                .param("category.id", "CP")
                .param("quantity", "10")
                .param("description", "Máy pha chuyên nghiệp")
                .param("available", "true")
                .session(session) // Nạp phiên Session nội bộ vào cho AuthInterceptor duyệt
                .with(csrf())) // Vượt rào mã Token CSRF của thẻ Form bảo mật (Đừng chặn bằng lỗi 403
                               // Forbidden)

                // => BẮT ĐẦU KIỂM TRA MONG ĐỢI (ASSERTIONS) <=
                // Mong đợi 1: Mã phản hồi mạng là 3xx (Redirect chứ không phải 200 OK hay 404,
                // 500)
                .andExpect(status().is3xxRedirection())
                // Mong đợi 2: Trang web chuyển hướng về /admin/product/list
                .andExpect(redirectedUrl("/admin/product/list"))
                // Mong đợi 3: Test xem Flash Attribute chứa câu thông báo màu xanh có hiện lên
                // không?
                .andExpect(flash().attributeExists("successMessage"))
                .andExpect(flash().attribute("successMessage", "Lưu thành công"));

        // Mong đợi cuối cùng: Chắc chắn rằng hàm saveProduct() dưới tầng Service đã
        // được gọi ĐÚNG 1 LẦN với file ảnh và object đã cho.
        // (Nếu không gọi chứng tỏ Form đã bị rớt / lỗi ở đâu đó trên tầng Controller).
        verify(productService, Mockito.times(1)).saveProduct(any(Product.class), any(MockMultipartFile.class));
    }

    /**
     * KỊCH BẢN: Thêm Sản Phẩm (Lỗi do Tên là NULL)
     * Kịch bản TC_PROD_002 mong đợi lỗi DataIntegrityViolationException tung ra từ
     * Database
     */
    @Test
    @DisplayName("TC_PROD_002: Thêm sản phẩm với tên NULL")
    @WithMockUser(username = "admin", roles = { "ADMIN" })
    void testSaveProduct_NullName() throws Exception {

        // --- CHUẨN BỊ (ARRANGE) ---
        // 1. Giả lập lỗi ở DB: Khi gọi saveProduct với bất kỳ tham số nào, ném ra lỗi
        // DataIntegrityViolationException
        Mockito.when(productService.saveProduct(any(Product.class), any()))
                .thenThrow(new DataIntegrityViolationException("Column 'Name' cannot be null"));

        // --- THỰC THI & KIỂM TRA (ACT & ASSERT) ---
        // Gửi Form nhưng KHÔNG có fie ld "name" (Tương đương tên NULL)
        mockMvc.perform(multipart("/admin/product/save")
                .param("price", "1000000")
                .param("category.id", "CP")
                .param("quantity", "5")
                .session(session)
                .with(csrf()))

                // Trông đợi Controller sẽ văng cái lỗi đó ra (Nó chứng minh DB đã chặn được sự
                // cố Name=Null)
                .andExpect(result -> {
                    org.junit.jupiter.api.Assertions.assertTrue(
                            result.getResolvedException() instanceof org.springframework.dao.DataIntegrityViolationException);
                    org.junit.jupiter.api.Assertions.assertEquals(
                            "Column 'Name' cannot be null",
                            result.getResolvedException().getMessage());
                });

        // Xác nhận Service giả đã bị gọi
        verify(productService, Mockito.times(1)).saveProduct(any(Product.class), any());
    }

    /**
     * KỊCH BẢN: Thêm Sản Phẩm (Lỗi do Giá Âm)
     * Kịch bản TC_PROD_003 mong đợi lỗi Validation "Giá phải lớn hơn 0"
     */
    @Test
    @DisplayName("TC_PROD_003: Thêm sản phẩm với giá âm")
    @WithMockUser(username = "admin", roles = { "ADMIN" })
    void testSaveProduct_NegativePrice() throws Exception {

        // --- CHUẨN BỊ (ARRANGE) ---
        // Không cần giả lập ProductService vì Controller sẽ chặn lại ở bước Validation
        // và không bao giờ gọi xuống tầng Service.

        // --- THỰC THI & KIỂM TRA (ACT & ASSERT) ---
        try {
            mockMvc.perform(multipart("/admin/product/save")
                    .param("name", "Sản phẩm test")
                    .param("price", "-50000") // Giá trị âm
                    .param("category.id", "CP")
                    // Các tham số khác
                    .param("quantity", "10")
                    .param("description", "")
                    .param("available", "true")
                    .session(session)
                    .with(csrf()))

                    // => KIỂM TRA MONG ĐỢI <=
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/products/form"))
                    .andExpect(model().attributeHasFieldErrors("item", "price"));

            verify(productService, Mockito.never()).saveProduct(any(), any());
        } catch (Exception e) {
            try (java.io.PrintWriter pw = new java.io.PrintWriter(
                    new java.io.FileWriter("D:\\Java_5_Jenka_Coffee\\error.txt"))) {
                e.printStackTrace(pw);
            }
            throw e;
        }
    }

    /**
     * KỊCH BẢN: Thêm Sản Phẩm (Lỗi do Giá Bằng 0)
     * Kịch bản TC_PROD_004
     */
    @Test
    @DisplayName("TC_PROD_004: Thêm sản phẩm với giá bằng 0")
    @WithMockUser(username = "admin", roles = { "ADMIN" })
    void testSaveProduct_ZeroPrice() throws Exception {

        Product expectedSavedProduct = new Product();
        expectedSavedProduct.setId(2);
        expectedSavedProduct.setName("Sản phẩm test giá 0");
        Mockito.when(productService.saveProduct(any(Product.class), any()))
                .thenReturn(expectedSavedProduct);

        mockMvc.perform(multipart("/admin/product/save")
                .param("name", "Sản phẩm test giá 0")
                .param("price", "0") // Giá = 0
                .param("category.id", "CP")
                .param("quantity", "10")
                .param("description", "")
                .param("available", "true")
                .session(session)
                .with(csrf()))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/product/list"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(productService, Mockito.times(1)).saveProduct(any(Product.class), any());
    }
}
