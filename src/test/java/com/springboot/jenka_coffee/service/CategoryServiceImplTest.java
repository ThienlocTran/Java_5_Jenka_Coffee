package com.springboot.jenka_coffee.service;

import com.springboot.jenka_coffee.dto.request.CategoryRequest;
import com.springboot.jenka_coffee.entity.Category;
import com.springboot.jenka_coffee.exception.BusinessRuleException;
import com.springboot.jenka_coffee.exception.DuplicateResourceException;
import com.springboot.jenka_coffee.exception.ResourceNotFoundException;
import com.springboot.jenka_coffee.repository.CategoryRepository;
import com.springboot.jenka_coffee.repository.ProductRepository;
import com.springboot.jenka_coffee.service.impl.CategoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test Case Document Part 2: CATEGORY MODULE - Service Layer
 * Tests for CategoryServiceImpl business logic
 * 
 * Coverage:
 * - TC-CAT-002: deleteOrThrow - category có sản phẩm → block xóa
 * - TC-CAT-003: deleteOrThrow - category không tồn tại → 400
 * - TC-CAT-004: createCategory - duplicate ID → exception
 * - TC-CAT-005: createCategory - ID lowercase phải được normalize thành UPPERCASE
 * - TC-CAT-006: createCategory - icon tự động assign nếu không truyền
 * - TC-CAT-007: createCategory - icon ID không có trong map thì icon=null
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Category Service Implementation Tests")
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    private CategoryServiceImpl categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryServiceImpl(categoryRepository, productRepository);
    }

    // ========== HELPER METHODS ==========

    private Category buildCategory(String id, String name, String icon) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setIcon(icon);
        return category;
    }

    // ========== TEST CASES ==========

    /**
     * TC-CAT-002 — deleteOrThrow — category có sản phẩm → block xóa
     * Expected: BusinessRuleException với message "Không thể xóa loại hàng này vì còn 5 sản phẩm thuộc loại này!"
     */
    @Test
    @DisplayName("TC-CAT-002: deleteOrThrow với category có sản phẩm phải throw BusinessRuleException")
    void deleteOrThrow_categoryHasProducts_throwsBusinessRuleException() {
        // Arrange
        Category category = buildCategory("MAY_PHA", "Máy Pha Cà Phê", "May_Pha_Ca_Phe.webp");
        
        when(categoryRepository.findById("MAY_PHA")).thenReturn(Optional.of(category));
        when(productRepository.countByCategoryId("MAY_PHA")).thenReturn(5L);

        // Act & Assert
        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> {
            categoryService.deleteOrThrow("MAY_PHA");
        });

        assertTrue(exception.getMessage().contains("5 sản phẩm"));
        assertTrue(exception.getMessage().contains("Không thể xóa loại hàng này"));
        
        // Verify delete was NOT called
        verify(categoryRepository, never()).deleteById(anyString());
    }

    /**
     * TC-CAT-003 — deleteOrThrow — category không tồn tại → 400
     * Expected: ResourceNotFoundException("Category", "id", "KHONG_CO")
     */
    @Test
    @DisplayName("TC-CAT-003: deleteOrThrow với category không tồn tại phải throw ResourceNotFoundException")
    void deleteOrThrow_categoryNotFound_throwsResourceNotFoundException() {
        // Arrange
        when(categoryRepository.findById("KHONG_CO")).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            categoryService.deleteOrThrow("KHONG_CO");
        });

        assertTrue(exception.getMessage().contains("Category"));
        assertTrue(exception.getMessage().contains("KHONG_CO"));
    }

    /**
     * TC-CAT-004 — createCategory — duplicate ID → exception
     * Expected: DuplicateResourceException("Category", "id", "MAY_PHA")
     */
    @Test
    @DisplayName("TC-CAT-004: createCategory với ID đã tồn tại phải throw DuplicateResourceException")
    void createCategory_duplicateId_throwsDuplicateResourceException() {
        // Arrange
        when(categoryRepository.existsById("MAY_PHA")).thenReturn(true);
        
        CategoryRequest request = new CategoryRequest();
        request.setId("MAY_PHA");
        request.setName("Máy Pha Cà Phê");

        // Act & Assert
        DuplicateResourceException exception = assertThrows(DuplicateResourceException.class, () -> {
            categoryService.createCategory(request);
        });

        assertTrue(exception.getMessage().contains("Category"));
        assertTrue(exception.getMessage().contains("MAY_PHA"));
        
        // Verify save was NOT called
        verify(categoryRepository, never()).save(any(Category.class));
    }

    /**
     * TC-CAT-005 — createCategory — ID lowercase phải được normalize thành UPPERCASE
     * Expected: Category được tạo với id="MAY_PHA", name="Máy Pha" (trim)
     */
    @Test
    @DisplayName("TC-CAT-005: createCategory phải normalize ID thành UPPERCASE và trim name")
    void createCategory_normalizesIdToUppercase() {
        // Arrange
        CategoryRequest request = new CategoryRequest();
        request.setId(" may_pha "); // lowercase + spaces
        request.setName(" Máy Pha Cà Phê "); // spaces
        
        when(categoryRepository.existsById("MAY_PHA")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        Category result = categoryService.createCategory(request);

        // Assert
        assertEquals("MAY_PHA", result.getId(), "ID phải được normalize thành UPPERCASE");
        assertEquals("Máy Pha Cà Phê", result.getName(), "Name phải được trim");
        
        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(categoryCaptor.capture());
        
        Category savedCategory = categoryCaptor.getValue();
        assertEquals("MAY_PHA", savedCategory.getId());
    }

    /**
     * TC-CAT-006 — createCategory — icon tự động assign nếu không truyền
     * Expected: Category được tạo với icon="May_Pha_Ca_Phe.webp" (từ getCategoryIcons())
     */
    @Test
    @DisplayName("TC-CAT-006: createCategory với icon=null phải auto assign từ predefined map")
    void createCategory_noIcon_autoAssignsFromPredefinedMap() {
        // Arrange
        CategoryRequest request = new CategoryRequest();
        request.setId("MAY_PHA");
        request.setName("Máy Pha Cà Phê");
        request.setIcon(null); // No icon provided
        
        when(categoryRepository.existsById("MAY_PHA")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        Category result = categoryService.createCategory(request);

        // Assert
        assertEquals("May_Pha_Ca_Phe.webp", result.getIcon(), 
                "Icon phải được auto assign từ predefined map");
        
        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(categoryCaptor.capture());
        
        Category savedCategory = categoryCaptor.getValue();
        assertEquals("May_Pha_Ca_Phe.webp", savedCategory.getIcon());
    }

    /**
     * TC-CAT-007 — createCategory — icon ID không có trong map thì icon=null
     * Expected: Category được tạo thành công với icon=null, không throw exception
     */
    @Test
    @DisplayName("TC-CAT-007: createCategory với ID không có trong predefined map phải set icon=null")
    void createCategory_unknownId_iconIsNull() {
        // Arrange
        CategoryRequest request = new CategoryRequest();
        request.setId("CUSTOM_CAT");
        request.setName("Danh mục tùy chỉnh");
        request.setIcon(null);
        
        when(categoryRepository.existsById("CUSTOM_CAT")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        Category result = categoryService.createCategory(request);

        // Assert
        assertNull(result.getIcon(), "Icon phải là null khi ID không có trong predefined map");
        
        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(categoryCaptor.capture());
        
        Category savedCategory = categoryCaptor.getValue();
        assertNull(savedCategory.getIcon());
    }

    @Test
    @DisplayName("TC-CAT-SER-001: createCategory với name rỗng - Không bị throw ở Service (GAP CHECK)")
    void createCategory_blankName_gapCheck() {
        // Requirement CSV mong muốn throw ConstraintViolationException hoặc BusinessRuleException ở Service
        // NHƯNG CategoryServiceImpl không validate name rỗng, nó phụ thuộc vào Controller @Valid
        // Đây là GAP nếu ta coi Service là layer độc lập cần validate business rules
        CategoryRequest request = new CategoryRequest();
        request.setId("TEST");
        request.setName("   "); // Blank name
        
        when(categoryRepository.existsById("TEST")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));

        // Act - Không throw exception
        Category result = assertDoesNotThrow(() -> {
            return categoryService.createCategory(request);
        });

        // Assert
        assertEquals("", result.getName()); // do request.normalize() gọi trim() biến "   " thành ""
        verify(categoryRepository).save(any(Category.class));
    }
}
