package com.springboot.jenka_coffee.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class BannerImageUpdateRequest {
    private Long id;
    private String title;
    private String subtitle;
    private Integer sortOrder;
    private String objectPosition;
    private Double zoom;
}
