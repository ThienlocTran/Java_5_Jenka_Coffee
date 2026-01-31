package com.springboot.jenka_coffee.dto.request;

import com.springboot.jenka_coffee.entity.Category;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequest {

    @NotBlank(message = "Mã loại hàng không được để trống")
    @Size(max = 10, message = "Mã loại hàng tối đa 10 ký tự")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "Mã loại hàng chỉ chứa chữ in hoa, số và dấu gạch dưới")
    private String id;

    @NotBlank(message = "Tên loại hàng không được để trống")
    @Size(min = 3, max = 100, message = "Tên loại hàng phải từ 3-100 ký tự")
    private String name;

    private String icon;

    /**
     * Format dữ liệu trước khi validation
     */
    public void normalize() {
        if (id != null)
            id = id.trim().toUpperCase();
        if (name != null)
            name = name.trim();
        if (icon != null)
            icon = icon.trim();
    }

    /**
     * Convert DTO to Entity
     */
    public Category toEntity() {
        normalize();

        Category category = new Category();
        category.setId(id);
        category.setName(name);

        return category;
    }

    /**
     * Create DTO from Entity (for edit form)
     */
    public static CategoryRequest fromEntity(Category category) {
        CategoryRequest dto = new CategoryRequest();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setIcon(category.getIcon());
        return dto;
    }
}
