package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.entity.Category;
import com.springboot.jenka_coffee.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category testCategory;

    @BeforeEach
    void setUp() {
        testCategory = new Category();
        testCategory.setId("coffee");
        testCategory.setName("Coffee");
    }

    @Test
    @DisplayName("Test find all categories")
    void findAll_Categories() {
        when(categoryRepository.findAll()).thenReturn(Arrays.asList(testCategory));

        List<Category> result = categoryService.findAll();

        assertEquals(1, result.size());
        assertEquals("Coffee", result.get(0).getName());
    }

    @Test
    @DisplayName("Test find by ID")
    void findById_Success() {
        when(categoryRepository.findById("coffee")).thenReturn(Optional.of(testCategory));

        Category result = categoryService.findById("coffee");

        assertNotNull(result);
        assertEquals("coffee", result.getId());
    }

    @Test
    @DisplayName("Test save category")
    void save_Category() {
        when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);

        Category result = categoryService.save(testCategory);

        assertNotNull(result);
        verify(categoryRepository).save(testCategory);
    }
}
