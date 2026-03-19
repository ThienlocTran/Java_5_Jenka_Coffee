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

    @NotBlank(message = "{CategoryRequest.id.NotBlank}")
    @Size(max = 10, message = "{CategoryRequest.id.Size}")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "{CategoryRequest.id.Pattern}")
    private String id;

    @NotBlank(message = "{CategoryRequest.name.NotBlank}")
    @Size(min = 3, max = 100, message = "{CategoryRequest.name.Size}")
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

}
