package com.springboot.jenka_coffee.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BannerImageUpdateRequest {
    private Long id;
    private String title;
    private String subtitle;
    private String objectPosition;
    private Double zoom;
    private Integer sortOrder;
}
