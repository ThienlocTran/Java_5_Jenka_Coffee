package com.springboot.jenka_coffee.dto.response;

import com.springboot.jenka_coffee.entity.Product;
import com.springboot.jenka_coffee.entity.ProductImage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailDTO {
    private Integer id;
    private String name;
    private String image;
    private BigDecimal price;
    private String description;
    private LocalDateTime createDate;
    private Boolean available;
    private CategoryDTO category;
    private List<ProductImageDTO> images;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryDTO {
        private String id;
        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductImageDTO {
        private Integer id;
        private String imageUrl;
        private Integer displayOrder;
        private Boolean isPrimary;
    }

    public static ProductDetailDTO fromEntity(Product product) {
        ProductDetailDTO dto = new ProductDetailDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setImage(product.getImage());
        dto.setPrice(product.getPrice());
        dto.setDescription(product.getDescription());
        dto.setCreateDate(product.getCreateDate());
        dto.setAvailable(product.getAvailable());
        
        if (product.getCategory() != null) {
            CategoryDTO categoryDTO = new CategoryDTO();
            categoryDTO.setId(product.getCategory().getId());
            categoryDTO.setName(product.getCategory().getName());
            dto.setCategory(categoryDTO);
        }
        
        if (product.getImages() != null) {
            dto.setImages(product.getImages().stream()
                .map(img -> new ProductImageDTO(
                    img.getId(),
                    img.getImageUrl(),
                    img.getDisplayOrder(),
                    img.getIsPrimary()
                ))
                .collect(Collectors.toList()));
        }
        
        return dto;
    }
}
