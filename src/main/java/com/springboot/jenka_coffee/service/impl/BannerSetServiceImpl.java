package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.dto.BannerImageUpdateRequest;
import com.springboot.jenka_coffee.entity.BannerImage;
import com.springboot.jenka_coffee.entity.BannerSet;
import com.springboot.jenka_coffee.exception.ResourceNotFoundException;
import com.springboot.jenka_coffee.repository.BannerImageRepository;
import com.springboot.jenka_coffee.repository.BannerSetRepository;
import com.springboot.jenka_coffee.service.BannerSetService;
import com.springboot.jenka_coffee.service.UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BannerSetServiceImpl implements BannerSetService {

    private static final BigDecimal CROP_MIN = BigDecimal.ZERO;
    private static final BigDecimal CROP_MAX = new BigDecimal("100.00");
    private static final BigDecimal CROP_SIZE_MIN = new BigDecimal("1.00");
    private static final BigDecimal DEFAULT_CROP_SIZE = new BigDecimal("100.00");
    private static final BigDecimal DEFAULT_ZOOM = new BigDecimal("1.00");
    private static final BigDecimal ZOOM_MAX = new BigDecimal("2.00");

    private final BannerSetRepository setRepo;
    private final BannerImageRepository imageRepo;
    private final UploadService uploadService;

    @Override
    public List<BannerSet> findAll() {
        return setRepo.findAll(Sort.by("createdAt").descending());
    }

    @Override
    public BannerSet findById(Long id) {
        return setRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BannerSet not found: " + id));
    }

    @Override
    public BannerSet create(String name, String effect,
                            List<MultipartFile> files,
                            List<String> titles, List<String> subtitles,
                            List<String> headlines, List<String> subHeadlines,
                            List<String> primaryCtaTexts, List<String> primaryCtaLinks,
                            List<String> secondaryCtaTexts, List<String> secondaryCtaLinks,
                            List<String> targetLinks, List<Boolean> actives, List<Integer> sortOrders,
                            List<BigDecimal> imageCropXs, List<BigDecimal> imageCropYs,
                            List<BigDecimal> imageCropWidths, List<BigDecimal> imageCropHeights,
                            List<BigDecimal> imageZooms) {
        List<String> uploadedUrls = uploadImages(files);
        return createBannerSetInDatabase(
                name, effect, uploadedUrls, titles, subtitles,
                headlines, subHeadlines, primaryCtaTexts, primaryCtaLinks,
                secondaryCtaTexts, secondaryCtaLinks, targetLinks, actives, sortOrders,
                imageCropXs, imageCropYs, imageCropWidths, imageCropHeights, imageZooms
        );
    }

    @Transactional
    protected BannerSet createBannerSetInDatabase(String name, String effect,
                                                  List<String> imageUrls,
                                                  List<String> titles, List<String> subtitles,
                                                  List<String> headlines, List<String> subHeadlines,
                                                  List<String> primaryCtaTexts, List<String> primaryCtaLinks,
                                                  List<String> secondaryCtaTexts, List<String> secondaryCtaLinks,
                                                  List<String> targetLinks, List<Boolean> actives, List<Integer> sortOrders,
                                                  List<BigDecimal> imageCropXs, List<BigDecimal> imageCropYs,
                                                  List<BigDecimal> imageCropWidths, List<BigDecimal> imageCropHeights,
                                                  List<BigDecimal> imageZooms) {
        BannerSet set = new BannerSet();
        set.setName(name);
        set.setEffect(effect != null ? effect : "fade");
        set = setRepo.save(set);

        if (imageUrls != null) {
            for (int i = 0; i < imageUrls.size(); i++) {
                String url = imageUrls.get(i);
                if (url == null) continue;

                BannerImage img = new BannerImage();
                img.setBannerSet(set);
                img.setImage(url);
                img.setTitle(safeGet(titles, i));
                img.setSubtitle(safeGet(subtitles, i));
                applyHeroContent(
                        img,
                        safeGet(headlines, i),
                        safeGet(subHeadlines, i),
                        safeGet(primaryCtaTexts, i),
                        safeGet(primaryCtaLinks, i),
                        safeGet(secondaryCtaTexts, i),
                        safeGet(secondaryCtaLinks, i),
                        safeGet(targetLinks, i),
                        safeBooleanGet(actives, i)
                );
                img.setSortOrder(safeIntegerGet(sortOrders, i, i));
                applyImageDisplay(
                        img,
                        safeDecimalGet(imageCropXs, i),
                        safeDecimalGet(imageCropYs, i),
                        safeDecimalGet(imageCropWidths, i),
                        safeDecimalGet(imageCropHeights, i),
                        safeDecimalGet(imageZooms, i)
                );
                set.getImages().add(img);
            }
        }

        return setRepo.save(set);
    }

    @Override
    @Transactional
    public BannerSet updateMeta(Long id, String name, String effect) {
        BannerSet set = findById(id);
        set.setName(name);
        set.setEffect(effect != null ? effect : "fade");
        return setRepo.save(set);
    }

    @Override
    public BannerSet addImages(Long id, List<MultipartFile> files,
                               List<String> titles, List<String> subtitles,
                               List<String> headlines, List<String> subHeadlines,
                               List<String> primaryCtaTexts, List<String> primaryCtaLinks,
                               List<String> secondaryCtaTexts, List<String> secondaryCtaLinks,
                               List<String> targetLinks, List<Boolean> actives, List<Integer> sortOrders,
                               List<BigDecimal> imageCropXs, List<BigDecimal> imageCropYs,
                               List<BigDecimal> imageCropWidths, List<BigDecimal> imageCropHeights,
                               List<BigDecimal> imageZooms) {
        List<String> uploadedUrls = uploadImages(files);
        return addImagesToDatabase(
                id, uploadedUrls, titles, subtitles,
                headlines, subHeadlines, primaryCtaTexts, primaryCtaLinks,
                secondaryCtaTexts, secondaryCtaLinks, targetLinks, actives, sortOrders,
                imageCropXs, imageCropYs, imageCropWidths, imageCropHeights, imageZooms
        );
    }

    @Override
    @Transactional
    public BannerSet updateImages(Long id, List<BannerImageUpdateRequest> images) {
        BannerSet set = findById(id);
        if (images == null) {
            return set;
        }

        Map<Long, BannerImage> existingImages = set.getImages().stream()
                .collect(Collectors.toMap(BannerImage::getId, Function.identity()));

        for (BannerImageUpdateRequest request : images) {
            if (request == null || request.getId() == null) {
                continue;
            }

            BannerImage image = existingImages.get(request.getId());
            if (image == null) {
                throw new IllegalArgumentException("Khong tim thay anh banner #" + request.getId() + " trong bo #" + id);
            }

            image.setTitle(sanitizeText(request.getTitle()));
            image.setSubtitle(sanitizeText(request.getSubtitle()));
            applyHeroContent(
                    image,
                    request.getHeadline(),
                    request.getSubHeadline(),
                    request.getPrimaryCtaText(),
                    request.getPrimaryCtaLink(),
                    request.getSecondaryCtaText(),
                    request.getSecondaryCtaLink(),
                    request.getTargetLink(),
                    request.getActive()
            );
            if (request.getSortOrder() != null) {
                image.setSortOrder(request.getSortOrder());
            }
            applyImageDisplay(
                    image,
                    request.getImageCropX(),
                    request.getImageCropY(),
                    request.getImageCropWidth(),
                    request.getImageCropHeight(),
                    request.getImageZoom()
            );
        }

        set.getImages().sort(java.util.Comparator.comparing(img -> img.getSortOrder() == null ? Integer.MAX_VALUE : img.getSortOrder()));
        return setRepo.save(set);
    }

    @Transactional
    protected BannerSet addImagesToDatabase(Long id, List<String> imageUrls,
                                            List<String> titles, List<String> subtitles,
                                            List<String> headlines, List<String> subHeadlines,
                                            List<String> primaryCtaTexts, List<String> primaryCtaLinks,
                                            List<String> secondaryCtaTexts, List<String> secondaryCtaLinks,
                                            List<String> targetLinks, List<Boolean> actives, List<Integer> sortOrders,
                                            List<BigDecimal> imageCropXs, List<BigDecimal> imageCropYs,
                                            List<BigDecimal> imageCropWidths, List<BigDecimal> imageCropHeights,
                                            List<BigDecimal> imageZooms) {
        BannerSet set = findById(id);
        int base = set.getImages().size();

        if (imageUrls != null) {
            for (int i = 0; i < imageUrls.size(); i++) {
                String url = imageUrls.get(i);
                if (url == null) continue;

                BannerImage img = new BannerImage();
                img.setBannerSet(set);
                img.setImage(url);
                img.setTitle(safeGet(titles, i));
                img.setSubtitle(safeGet(subtitles, i));
                applyHeroContent(
                        img,
                        safeGet(headlines, i),
                        safeGet(subHeadlines, i),
                        safeGet(primaryCtaTexts, i),
                        safeGet(primaryCtaLinks, i),
                        safeGet(secondaryCtaTexts, i),
                        safeGet(secondaryCtaLinks, i),
                        safeGet(targetLinks, i),
                        safeBooleanGet(actives, i)
                );
                img.setSortOrder(safeIntegerGet(sortOrders, i, base + i));
                applyImageDisplay(
                        img,
                        safeDecimalGet(imageCropXs, i),
                        safeDecimalGet(imageCropYs, i),
                        safeDecimalGet(imageCropWidths, i),
                        safeDecimalGet(imageCropHeights, i),
                        safeDecimalGet(imageZooms, i)
                );
                set.getImages().add(img);
            }
        }

        return setRepo.save(set);
    }

    @Override
    @Transactional
    public void removeImage(Long imageId) {
        imageRepo.deleteById(imageId);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        setRepo.deleteById(id);
    }

    @Override
    @Transactional
    public BannerSet activate(Long id) {
        setRepo.deactivateAll();
        BannerSet set = findById(id);
        set.setActive(true);
        return setRepo.save(set);
    }

    @Override
    public BannerSet getActive() {
        return setRepo.findByActiveTrue().orElse(null);
    }

    private List<String> uploadImages(List<MultipartFile> files) {
        if (files == null) return null;

        List<String> urls = new java.util.ArrayList<>();
        for (MultipartFile f : files) {
            if (f == null || f.isEmpty()) {
                urls.add(null);
                continue;
            }

            try {
                String url = uploadService.saveImage(f);
                urls.add(url);
                if (url != null) {
                    log.info("Successfully uploaded banner image: {}", url);
                }
            } catch (Exception e) {
                log.error("Failed to upload banner image: {}", e.getMessage());
                urls.add(null);
            }
        }
        return urls;
    }

    private String safeGet(List<String> list, int i) {
        if (list == null || i >= list.size()) return null;
        return sanitizeText(list.get(i));
    }

    private BigDecimal safeDecimalGet(List<BigDecimal> list, int i) {
        if (list == null || i >= list.size()) return null;
        return list.get(i);
    }

    private Boolean safeBooleanGet(List<Boolean> list, int i) {
        if (list == null || i >= list.size()) return null;
        return list.get(i);
    }

    private Integer safeIntegerGet(List<Integer> list, int i, int fallback) {
        if (list == null || i >= list.size() || list.get(i) == null) return fallback;
        return list.get(i);
    }

    private String sanitizeText(String value) {
        if (value == null || value.isBlank()) return null;
        return value.replaceAll("<[^>]*>", "").trim();
    }

    private void applyHeroContent(BannerImage image,
                                  String headline,
                                  String subHeadline,
                                  String primaryCtaText,
                                  String primaryCtaLink,
                                  String secondaryCtaText,
                                  String secondaryCtaLink,
                                  String targetLink,
                                  Boolean active) {
        image.setHeadline(sanitizeText(headline));
        image.setSubHeadline(sanitizeText(subHeadline));
        image.setPrimaryCtaText(sanitizeText(primaryCtaText));
        image.setPrimaryCtaLink(sanitizeText(primaryCtaLink));
        image.setSecondaryCtaText(sanitizeText(secondaryCtaText));
        image.setSecondaryCtaLink(sanitizeText(secondaryCtaLink));
        image.setTargetLink(sanitizeText(targetLink));
        if (active != null) {
            image.setActive(active);
        } else if (image.getActive() == null) {
            image.setActive(true);
        }
    }

    private void applyImageDisplay(BannerImage image,
                                   BigDecimal cropX,
                                   BigDecimal cropY,
                                   BigDecimal cropWidth,
                                   BigDecimal cropHeight,
                                   BigDecimal zoom) {
        BigDecimal normalizedWidth = clamp(defaultDecimal(cropWidth, DEFAULT_CROP_SIZE), CROP_SIZE_MIN, CROP_MAX);
        BigDecimal normalizedHeight = clamp(defaultDecimal(cropHeight, DEFAULT_CROP_SIZE), CROP_SIZE_MIN, CROP_MAX);
        BigDecimal normalizedX = clamp(defaultDecimal(cropX, CROP_MIN), CROP_MIN, CROP_MAX);
        BigDecimal normalizedY = clamp(defaultDecimal(cropY, CROP_MIN), CROP_MIN, CROP_MAX);
        BigDecimal normalizedZoom = clamp(defaultDecimal(zoom, DEFAULT_ZOOM), DEFAULT_ZOOM, ZOOM_MAX);

        if (normalizedX.add(normalizedWidth).compareTo(CROP_MAX) > 0) {
            normalizedX = CROP_MAX.subtract(normalizedWidth).max(BigDecimal.ZERO);
        }
        if (normalizedY.add(normalizedHeight).compareTo(CROP_MAX) > 0) {
            normalizedY = CROP_MAX.subtract(normalizedHeight).max(BigDecimal.ZERO);
        }

        image.setImageCropX(normalizedX);
        image.setImageCropY(normalizedY);
        image.setImageCropWidth(normalizedWidth);
        image.setImageCropHeight(normalizedHeight);
        image.setImageZoom(normalizedZoom);
    }

    private BigDecimal defaultDecimal(BigDecimal value, BigDecimal fallback) {
        return value == null ? fallback : value;
    }

    private BigDecimal clamp(BigDecimal value, BigDecimal min, BigDecimal max) {
        if (value.compareTo(min) < 0) {
            return min;
        }
        if (value.compareTo(max) > 0) {
            return max;
        }
        return value;
    }
}
