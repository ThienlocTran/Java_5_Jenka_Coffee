package com.springboot.jenka_coffee.chi_bao;

import com.springboot.jenka_coffee.dto.request.CategoryRequest;
import com.springboot.jenka_coffee.entity.Category;
import com.springboot.jenka_coffee.repository.CategoryRepository;
import com.springboot.jenka_coffee.repository.ProductRepository;
import com.springboot.jenka_coffee.service.CategoryService;
import com.springboot.jenka_coffee.service.impl.CategoryServiceImpl;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceIconTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Mock
    private ProductRepository productRepository;

    @Test
    void createCategory_WhenIconNull_ShouldAutoMapIconById() {

        // Arrange
        CategoryRequest request = new CategoryRequest();
        request.setId("MAY_PHA");
        request.setName("Máy pha");
        request.setIcon(null);

        when(categoryRepository.existsById("MAY_PHA"))
                .thenReturn(false);

        // Act
        categoryService.createCategory(request);

        // Capture dữ liệu được save
        ArgumentCaptor<Category> captor =
                ArgumentCaptor.forClass(Category.class);

        verify(categoryRepository).save(captor.capture());

        Category saved = captor.getValue();

        // Assert
        assertEquals("May_Pha_Ca_Phe.webp", saved.getIcon());
    }

    @Test
    void createCategory_WhenIconSelected_ShouldSaveSelectedIcon() {

        // Arrange
        CategoryRequest request = new CategoryRequest();
        request.setId("DUNG_CU");
        request.setName("Dụng cụ");
        request.setIcon("dung_cu_pha_che.webp");

        when(categoryRepository.existsById("DUNG_CU"))
                .thenReturn(false);

        // Act
        categoryService.createCategory(request);

        // Capture object save vào DB
        ArgumentCaptor<Category> captor =
                ArgumentCaptor.forClass(Category.class);

        verify(categoryRepository).save(captor.capture());

        Category saved = captor.getValue();

        // Assert
        assertEquals("dung_cu_pha_che.webp", saved.getIcon());
    }

    @Test
    void updateCategory_WhenChangeIcon_ShouldUpdateIcon() {

        // Arrange
        Category existing = new Category();
        existing.setId("CP");
        existing.setName("Cà phê");
        existing.setIcon("ca_phe_do_an.webp");

        CategoryRequest request = new CategoryRequest();
        request.setName("Cà phê");
        request.setIcon("May_Pha_Ca_Phe.webp");

        when(categoryRepository.findById("CP"))
                .thenReturn(Optional.of(existing));

        // Act
        categoryService.updateCategory("CP", request);

        // Assert
        assertEquals("May_Pha_Ca_Phe.webp", existing.getIcon());

        verify(categoryRepository).save(existing);
    }

    @Test
    void countProductsByCategory_ShouldReturnCorrectCount() {

        when(productRepository.countByCategoryId("CP"))
                .thenReturn(5L);

        long count = categoryService.countProductsByCategory("CP");

        assertEquals(5L, count);

        verify(productRepository).countByCategoryId("CP");
    }


}