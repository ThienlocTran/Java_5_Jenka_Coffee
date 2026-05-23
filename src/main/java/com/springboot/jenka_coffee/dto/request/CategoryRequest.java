package com.springboot.jenka_coffee.dto.request;

import com.springboot.jenka_coffee.entity.Category;
import com.springboot.jenka_coffee.util.SlugUtils;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequest {

    @Size(max = 50, message = "{CategoryRequest.id.Size}")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "{CategoryRequest.id.Pattern}")
    private String id;

    @NotBlank(message = "{CategoryRequest.name.NotBlank}")
    @Size(min = 3, max = 100, message = "{CategoryRequest.name.Size}")
    private String name;

    private String icon;

    private String slug;

    @DecimalMin(value = "0.00", message = "imageCropX must be between 0 and 100")
    @DecimalMax(value = "100.00", message = "imageCropX must be between 0 and 100")
    private BigDecimal imageCropX;

    @DecimalMin(value = "0.00", message = "imageCropY must be between 0 and 100")
    @DecimalMax(value = "100.00", message = "imageCropY must be between 0 and 100")
    private BigDecimal imageCropY;

    @DecimalMin(value = "1.00", message = "imageCropWidth must be between 1 and 100")
    @DecimalMax(value = "100.00", message = "imageCropWidth must be between 1 and 100")
    private BigDecimal imageCropWidth;

    @DecimalMin(value = "1.00", message = "imageCropHeight must be between 1 and 100")
    @DecimalMax(value = "100.00", message = "imageCropHeight must be between 1 and 100")
    private BigDecimal imageCropHeight;

    @DecimalMin(value = "1.00", message = "imageZoom must be between 1 and 2")
    @DecimalMax(value = "2.00", message = "imageZoom must be between 1 and 2")
    private BigDecimal imageZoom;

    public void normalize() {
        if (id != null)
            id = id.trim().toUpperCase();
        if (name != null)
            name = name.trim();
        if (icon != null)
            icon = icon.trim();
        if (slug != null)
            slug = slug.trim().toLowerCase();
        if (slug == null || slug.isBlank())
            slug = SlugUtils.toSlug(name);
        applyImageDisplayDefaults();
        clampImageDisplay();
    }

    public void applyImageDisplayDefaults() {
        if (imageCropX == null)
            imageCropX = BigDecimal.ZERO;
        if (imageCropY == null)
            imageCropY = BigDecimal.ZERO;
        if (imageCropWidth == null)
            imageCropWidth = new BigDecimal("100.00");
        if (imageCropHeight == null)
            imageCropHeight = new BigDecimal("100.00");
        if (imageZoom == null)
            imageZoom = new BigDecimal("1.00");
    }

    public void validateImageDisplay() {
        if (imageCropX.add(imageCropWidth).compareTo(new BigDecimal("100.00")) > 0) {
            throw new IllegalArgumentException("imageCropX + imageCropWidth must be <= 100");
        }
        if (imageCropY.add(imageCropHeight).compareTo(new BigDecimal("100.00")) > 0) {
            throw new IllegalArgumentException("imageCropY + imageCropHeight must be <= 100");
        }
    }

    private void clampImageDisplay() {
        imageCropX = clamp(imageCropX, "0.00", "100.00");
        imageCropY = clamp(imageCropY, "0.00", "100.00");
        imageCropWidth = clamp(imageCropWidth, "1.00", "100.00");
        imageCropHeight = clamp(imageCropHeight, "1.00", "100.00");
        imageZoom = clamp(imageZoom, "1.00", "2.00");

        if (imageCropX.add(imageCropWidth).compareTo(new BigDecimal("100.00")) > 0) {
            imageCropX = new BigDecimal("100.00").subtract(imageCropWidth).max(BigDecimal.ZERO);
        }
        if (imageCropY.add(imageCropHeight).compareTo(new BigDecimal("100.00")) > 0) {
            imageCropY = new BigDecimal("100.00").subtract(imageCropHeight).max(BigDecimal.ZERO);
        }
    }

    private BigDecimal clamp(BigDecimal value, String min, String max) {
        BigDecimal minValue = new BigDecimal(min);
        BigDecimal maxValue = new BigDecimal(max);
        if (value.compareTo(minValue) < 0)
            return minValue;
        if (value.compareTo(maxValue) > 0)
            return maxValue;
        return value;
    }

    public Category toEntity() {
        normalize();
        validateImageDisplay();

        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setIcon(icon);
        category.setSlug(slug);
        category.setImageCropX(imageCropX);
        category.setImageCropY(imageCropY);
        category.setImageCropWidth(imageCropWidth);
        category.setImageCropHeight(imageCropHeight);
        category.setImageZoom(imageZoom);

        return category;
    }
}
