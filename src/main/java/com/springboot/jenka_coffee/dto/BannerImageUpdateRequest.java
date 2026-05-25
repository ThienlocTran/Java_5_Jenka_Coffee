package com.springboot.jenka_coffee.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class BannerImageUpdateRequest {
    private Long id;
    private String title;
    private String subtitle;
    private String headline;
    private String subHeadline;
    private String primaryCtaText;
    private String primaryCtaLink;
    private String secondaryCtaText;
    private String secondaryCtaLink;
    private String targetLink;
    private String displayMode;
    private Boolean active;
    private Integer sortOrder;
    private BigDecimal imageCropX;
    private BigDecimal imageCropY;
    private BigDecimal imageCropWidth;
    private BigDecimal imageCropHeight;
    private BigDecimal imageZoom;
}
