package com.springboot.jenka_coffee.chi_bao;

import com.springboot.jenka_coffee.controller.admin.AdminCategoryController;
import com.springboot.jenka_coffee.dto.request.CategoryRequest;
import com.springboot.jenka_coffee.entity.Account;
import com.springboot.jenka_coffee.entity.Category;
import com.springboot.jenka_coffee.exception.BusinessRuleException;
import com.springboot.jenka_coffee.exception.DuplicateResourceException;
import com.springboot.jenka_coffee.interceptor.AuthInterceptor;
import com.springboot.jenka_coffee.repository.CategoryRepository;
import com.springboot.jenka_coffee.service.*;
import com.springboot.jenka_coffee.util.ImageHelper;
import com.springboot.jenka_coffee.util.MessageHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.InternalResourceView;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AdminCategoryController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
        }
)
@AutoConfigureMockMvc


class AdminCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ThymeleafViewResolver thymeleafViewResolver;

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
    private CategoryRepository categoryRepository;

    @MockBean
    private CookieService cookieService; // Dành cho AuthInterceptor

    @MockBean
    private CartService cartService; // Dành cho GlobalInterceptor (chặn mọi request)

    @MockBean
    private ImageHelper imageHelper; // Phục vụ Config/Component nào đó

    @MockBean
    private MessageHelper messageHelper; // Dành cho GlobalExceptionHandler

    private MockHttpSession session;

    @BeforeEach
    void setup() throws Exception {
        View mockView = Mockito.mock(View.class);

        Mockito.when(thymeleafViewResolver.resolveViewName(
                        Mockito.anyString(),
                        Mockito.any()))
                .thenReturn(mockView);
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public ViewResolver viewResolver() {
            return (viewName, locale) -> new InternalResourceView(viewName);
        }
    }



    //--------------TC_CAT_002------------
    @Test
    void updateCategory_DuplicateId_ShouldReturnConflict() throws Exception {

        Account account = new Account();
        account.setAdmin(true);

        when(categoryService.updateCategory(any(), any()))
                .thenThrow(new DuplicateResourceException("ID đã tồn tại"));

        mockMvc.perform(post("/admin/category/save")
                        .sessionAttr("user", account)
                        .param("id", "CP")
                        .param("name", "Cà phê"))
                .andExpect(status().isOk())
                .andExpect(flash().attributeExists("error"))
                .andExpect(flash().attribute("error", "ID đã tồn tại"));

        verify(categoryService, times(1)).updateCategory(any(), any());
    }

    //---------------TC_CAT_003-------------
    @Test
    void saveCategory_EmptyName_ShouldReturnForm() throws Exception {

        Account account = new Account();
        account.setAdmin(true);

        mockMvc.perform(post("/admin/category/save")
                        .sessionAttr("user", account)
                        .param("id", "TEST")
                        .param("name", ""))   // rỗng
                .andExpect(status().isOk())
                .andExpect(view().name("admin/categories/category-form"))
                .andExpect(model().attributeHasFieldErrors("item", "name"));

        verify(categoryService, never()).createCategory(any());
    }

    //--------------TC_CAT_004-------------
    @Test
    void saveCategory_InvalidIdPattern_ShouldReturnForm() throws Exception {

        Account account = new Account();
        account.setAdmin(true);

        mockMvc.perform(post("/admin/category/save")
                        .sessionAttr("user", account)
                        .param("id", "C@F#")   // sai pattern
                        .param("name", "Cà phê"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/categories/category-form"));

        verify(categoryService, never()).createCategory(any());
    }

    @Test
    @DisplayName("TC_CAT_005 - Cập nhật danh mục thành công")
    @WithMockUser(roles = "ADMIN")
    void updateCategory_Success() throws Exception {

        Category existing = new Category();
        existing.setId("CP");
        existing.setName("Cà phê phin");

        when(categoryService.findByIdOrThrow("CP"))
                .thenReturn(existing);

        Account account = new Account();
        account.setAdmin(true);

        mockMvc.perform(post("/admin/category/save")
                        .sessionAttr("user", account)
                        .param("id", "CP")
                        .param("name", "Cà phê máy")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("redirect:/admin/category/list"))
                .andExpect(flash().attribute("success",
                        "Cập nhật loại hàng thành công!"));

        verify(categoryService)
                .updateCategory(eq("CP"), any(CategoryRequest.class));
    }






    //Category "TEST" tồn tại
    //- Không có SP nào thuộc "TEST"
    @Test
    @DisplayName("TC_CAT_007 - Xóa danh mục thành công khi không có sản phẩm")
    void deleteCategory_Success() throws Exception {

        Account account = new Account();
        account.setAdmin(true);

        mockMvc.perform(post("/admin/category/delete/TEST")
                        .sessionAttr("user", account))
                .andExpect(status().isOk())
                .andExpect(view().name("redirect:/admin/category/list"))
                .andExpect(flash().attribute("success", "Xóa loại hàng thành công!"));

        verify(categoryService).deleteOrThrow("TEST");
    }

    // ==============================
    // TC_CAT_008 - Xóa có sản phẩm (Chặn)
    //Loại sản phẩm đang có số lượng sản phẩm k xóa được
    // ==============================
    @Test
    void deleteCategory_HasProducts_ShouldThrowException() throws Exception {

        Account admin = new Account();
        admin.setAdmin(true);

        doThrow(new BusinessRuleException("Không thể xóa loại hàng này vì còn sản phẩm"))
                .when(categoryService).deleteOrThrow("CP");

        mockMvc.perform(post("/admin/category/delete/CP")
                        .sessionAttr("user", admin))
                .andExpect(status().isOk())
                .andExpect(view().name("redirect:/admin/product/list"))
                .andExpect(flash().attributeExists("error"));

        verify(categoryService).deleteOrThrow("CP");
    }

    // ==============================
    // TC_CAT_010 - Validation tên quá dài (>100 ký tự)
    // ==============================
    @Test
    void saveCategory_NameTooLong_ShouldReturnForm() throws Exception {

        Account account = new Account();
        account.setAdmin(true);

        String longName = "A".repeat(101);

        mockMvc.perform(post("/admin/category/save")
                        .sessionAttr("user", account)
                        .param("name", longName))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/categories/category-form"))
                .andExpect(model().attributeHasFieldErrors("item", "name"));// ==> Kiểm tra trong Model có lỗi validation ở field name của object item.


        verify(categoryService, never()).createCategory(any());
    }

    // ==============================
    // TC_CAT_011 - Validation tên quá ngắn (<3 ký tự)
    // ==============================
    @Test
    void saveCategory_NameTooShort_ShouldReturnForm() throws Exception {

        Account account = new Account();
        account.setAdmin(true);

        mockMvc.perform(post("/admin/category/save")
                        .sessionAttr("user", account)
                        .param("name", "AB")
                        .with(csrf()))
                .andExpect(status().isOk()) // ==> Return view bình thường
                .andExpect(view().name("admin/categories/category-form"))
                .andExpect(model().attributeHasFieldErrors("item", "name"));// ==> Kiểm tra trong Model có lỗi validation ở field name của object item.

        verify(categoryService, never()).createCategory(any());
    }

    // Test cho TC_CAT_015 - Reset form (mở form thêm → item rỗng)
    @Test
    void TC_CAT_015_ResetForm_AddForm_ShouldReturnEmptyItem() throws Exception {
        // giả lập admin đang login
        Account account = new Account();
        account.setAdmin(true);

        mockMvc.perform(get("/admin/category/add")
                        .sessionAttr("user", account))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("admin/categories/category-form"))
                .andExpect(model().attributeExists("item"))
                // item.id có thể null hoặc rỗng tuỳ implementation của CategoryRequest
                .andExpect(model().attribute("item", hasProperty("id", anyOf(nullValue(), isEmptyString()))))
                // item.name cũng null hoặc rỗng
                .andExpect(model().attribute("item", hasProperty("name", anyOf(nullValue(), isEmptyString()))));
    }









}
