package com.springboot.jenka_coffee.service;

import com.springboot.jenka_coffee.dto.BannerImageUpdateRequest;
import com.springboot.jenka_coffee.entity.BannerSet;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

public interface BannerSetService {
    List<BannerSet> findAll();
    BannerSet findById(Long id);

    default BannerSet create(String name, String effect, List<MultipartFile> files,
                             List<String> titles, List<String> subtitles,
                             List<String> targetLinks, List<Integer> sortOrders) {
        return create(
                name, effect, files, titles, subtitles,
                null, null, null, null,
                null, null, null, targetLinks, null, sortOrders,
                null, null, null, null, null
        );
    }

    BannerSet create(String name, String effect, List<MultipartFile> files,
                     List<String> titles, List<String> subtitles,
                     List<String> headlines, List<String> subHeadlines,
                     List<String> primaryCtaTexts, List<String> primaryCtaLinks,
                     List<String> secondaryCtaTexts, List<String> secondaryCtaLinks, List<String> displayModes,
                     List<String> targetLinks, List<Boolean> actives,
                     List<Integer> sortOrders,
                     List<BigDecimal> imageCropXs, List<BigDecimal> imageCropYs,
                     List<BigDecimal> imageCropWidths, List<BigDecimal> imageCropHeights,
                     List<BigDecimal> imageZooms);
    BannerSet updateMeta(Long id, String name, String effect);

    default BannerSet addImages(Long id, List<MultipartFile> files,
                                List<String> titles, List<String> subtitles,
                                List<String> targetLinks, List<Integer> sortOrders) {
        return addImages(
                id, files, titles, subtitles,
                null, null, null, null,
                null, null, null, targetLinks, null, sortOrders,
                null, null, null, null, null
        );
    }

    BannerSet addImages(Long id, List<MultipartFile> files,
                        List<String> titles, List<String> subtitles,
                        List<String> headlines, List<String> subHeadlines,
                        List<String> primaryCtaTexts, List<String> primaryCtaLinks,
                        List<String> secondaryCtaTexts, List<String> secondaryCtaLinks, List<String> displayModes,
                        List<String> targetLinks, List<Boolean> actives,
                        List<Integer> sortOrders,
                        List<BigDecimal> imageCropXs, List<BigDecimal> imageCropYs,
                        List<BigDecimal> imageCropWidths, List<BigDecimal> imageCropHeights,
                        List<BigDecimal> imageZooms);
    BannerSet updateImages(Long id, List<BannerImageUpdateRequest> images);
    void removeImage(Long imageId);
    void delete(Long id);
    BannerSet activate(Long id);
    BannerSet getActive();
}
