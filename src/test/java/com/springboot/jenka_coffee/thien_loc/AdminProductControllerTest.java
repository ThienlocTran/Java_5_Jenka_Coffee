package com.springboot.jenka_coffee.thien_loc;

import com.springboot.jenka_coffee.controller.admin.AdminProductController;
import com.springboot.jenka_coffee.entity.Account;
import com.springboot.jenka_coffee.entity.Category;
import com.springboot.jenka_coffee.entity.Product;
import com.springboot.jenka_coffee.interceptor.AuthInterceptor;
import com.springboot.jenka_coffee.service.AccountService;
import com.springboot.jenka_coffee.service.CategoryService;
import com.springboot.jenka_coffee.service.CookieService;
import com.springboot.jenka_coffee.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminProductController.class, includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = AuthInterceptor.class))
public class AdminProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @MockBean
    private CategoryService categoryService;

    // Mocking interceptor dependencies
    @MockBean
    private AccountService accountService;

    @MockBean
    private CookieService cookieService;

    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        // Condition: Logged in Admin
        Account adminAccount = new Account();
        adminAccount.setUsername("admin");
        adminAccount.setAdmin(true);
        session = new MockHttpSession();
        session.setAttribute("user", adminAccount);

        // Condition: Category "CP" exists
        Category categoryCP = new Category();
        categoryCP.setId("CP");
        categoryCP.setName("Cà phê");
        Mockito.when(categoryService.findById("CP")).thenReturn(categoryCP);
    }

    @Test
    @DisplayName("TC_PROD_001: Thêm sản phẩm hợp lệ")
    void testSaveProduct_Success() throws Exception {
        // Steps 1 & 2 & 3: Prepare valid full information and optional image file
        MockMultipartFile imageFile = new MockMultipartFile(
                "imageFile",
                "coffee_machine.jpg",
                "image/jpeg",
                "test image content".getBytes());

        // Step 4: Click 'Lưu' button (performs POST request to /admin/product/save)
        mockMvc.perform(multipart("/admin/product/save")
                .file(imageFile)
                .param("name", "Máy pha cà phê Ý")
                .param("price", "5000000")
                .param("category.id", "CP")
                .param("quantity", "10")
                .param("description", "Máy pha chuyên nghiệp")
                .param("available", "true")
                .session(session)) // Simulate logged in Admin
                // Expected Result 1: HTTP Status 302 (Redirect)
                .andExpect(status().is3xxRedirection())
                // Expected Result 2: Redirect to /admin/product/list
                .andExpect(redirectedUrl("/admin/product/list"))
                // Expected Result 3: Flash message 'Lưu thành công'
                // Note: The actual code might fail this assertion if the flash message is not
                // implemented accurately yet.
                .andExpect(flash().attributeExists("successMessage"))
                .andExpect(flash().attribute("successMessage", "Lưu thành công"));

        // Expected Result 4 & 5: DB Record is inserted & Image uploaded to Cloudinary
        // Since we are mocking ProductService, we verify it is called with the correct
        // parameters,
        // which means the controller successfully delegated the save & upload logic to
        // the service layer.
        verify(productService).saveProduct(any(Product.class), any(MockMultipartFile.class));
    }
}
