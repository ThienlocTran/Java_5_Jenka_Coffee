package com.springboot.jenka_coffee.chi_bao;

import com.springboot.jenka_coffee.dto.request.CategoryRequest;
import com.springboot.jenka_coffee.entity.Account;
import com.springboot.jenka_coffee.entity.Category;
import com.springboot.jenka_coffee.entity.Product;
import com.springboot.jenka_coffee.repository.CategoryRepository;
import com.springboot.jenka_coffee.repository.ProductRepository;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.springboot.jenka_coffee.service.CategoryService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CategoryServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductRepository productRepository;

    private Product createProduct(String name, Category category) {
        Product p = new Product();
        p.setName(name);
        p.setCategory(category);
        p.setPrice(new BigDecimal("10000"));
        p.setQuantity(10);
        p.setAvailable(true);
        p.setCreateDate(LocalDateTime.now());
        p.setDescription("Test product");
        p.setImage("test.webp");

        return productRepository.save(p);
    }

    @Test
    void TC_CAT_020_CountProducts_ShouldMatchDatabase() throws Exception {

        Category cp = new Category();
        cp.setId("CP");
        cp.setName("Cà phê");
        categoryRepository.save(cp);

        Category tea = new Category();
        tea.setId("TEA");
        tea.setName("Trà");
        categoryRepository.save(tea);

        for (int i = 1; i <= 5; i++) {
            createProduct("CP_" + i, cp);
        }

        for (int i = 1; i <= 2; i++) {
            createProduct("TEA_" + i, tea);
        }

        Account admin = new Account();
        admin.setAdmin(true);

        mockMvc.perform(get("/admin/category/list")
                        .sessionAttr("user", admin))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("categories"));

        long cpCount = productRepository.countByCategoryId("CP");
        long teaCount = productRepository.countByCategoryId("TEA");

        assertEquals(5L, cpCount);
        assertEquals(2L, teaCount);
    }
    /**
     * TC_CAT_021
     * - Gọi AJAX endpoint trả về số lượng sản phẩm theo category:
     *   GET /admin/category/product-count/CP -> body = "5"
     */
    @Test
    void TC_CAT_021_ProductCountEndpoint_ShouldReturnCorrectCount() throws Exception {

        Category cp = new Category();
        cp.setId("CP");
        cp.setName("Cà phê");
        categoryRepository.save(cp);

        for (int i = 1; i <= 5; i++) {
            createProduct("CP_" + i, cp);
        }

        Account admin = new Account();
        admin.setAdmin(true);

        mockMvc.perform(get("/admin/category/product-count/{id}", "CP")
                        .sessionAttr("user", admin))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }

    /**
     * TC_CAT_022
     * - Kiểm tra method findByIdOrThrow via controller edit form:
     *   GET /admin/category/edit/CP -> model contains "item" with id = "CP"
     */
    @Test
    void TC_CAT_013_NormalizeId_ToUppercase() {
        CategoryRequest request = new CategoryRequest();
        request.setId("tea");
        request.setName("Trà");

        Category result = categoryService.createCategory(request);

        assertEquals("TEA", result.getId());
    }

    @Test
    void saveCategory_InvalidRequest_ShouldReturnForm() throws Exception {

        Account account = new Account();
        account.setAdmin(true);

        mockMvc.perform(post("/admin/category/save")
                        .sessionAttr("user", account)
                        .param("id", "TEA")   // nếu id phải là số → sẽ fail
                        .param("name", "Trà"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/categories/category-form"));

        // kiểm tra không lưu DB
        assertEquals(0, categoryRepository.count());
    }
}
