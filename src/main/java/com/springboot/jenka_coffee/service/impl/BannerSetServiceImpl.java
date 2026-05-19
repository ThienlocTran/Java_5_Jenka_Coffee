package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.dto.request.BannerImageUpdateRequest;
import com.springboot.jenka_coffee.entity.BannerEffect;
import com.springboot.jenka_coffee.entity.BannerImage;
import com.springboot.jenka_coffee.entity.BannerSet;
import com.springboot.jenka_coffee.exception.BusinessRuleException;
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

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BannerSetServiceImpl implements BannerSetService {

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
    public BannerSet create(String name, String effect, List<MultipartFile> files,
                            List<String> titles, List<String> subtitles,
                            List<String> objectPositions, List<Double> zooms) {
        List<String> uploadedUrls = uploadImages(files);
        return createBannerSetInDatabase(name, effect, uploadedUrls, titles, subtitles, objectPositions, zooms);
    }

    @Transactional
    protected BannerSet createBannerSetInDatabase(String name, String effect, List<String> imageUrls,
                                                  List<String> titles, List<String> subtitles,
                                                  List<String> objectPositions, List<Double> zooms) {
        BannerSet set = new BannerSet();
        set.setName(name);
        set.setEffect(BannerEffect.normalize(effect));
        set = setRepo.save(set);

        appendUploadedImages(set, imageUrls, titles, subtitles, objectPositions, zooms, 0);
        return setRepo.save(set);
    }

    @Override
    @Transactional
    public BannerSet updateMeta(Long id, String name, String effect) {
        BannerSet set = findById(id);
        set.setName(name);
        set.setEffect(BannerEffect.normalize(effect));
        return setRepo.save(set);
    }

    @Override
    public BannerSet addImages(Long id, List<MultipartFile> files,
                               List<String> titles, List<String> subtitles,
                               List<String> objectPositions, List<Double> zooms) {
        List<String> uploadedUrls = uploadImages(files);
        return addImagesToDatabase(id, uploadedUrls, titles, subtitles, objectPositions, zooms);
    }

    @Transactional
    protected BannerSet addImagesToDatabase(Long id, List<String> imageUrls,
                                            List<String> titles, List<String> subtitles,
                                            List<String> objectPositions, List<Double> zooms) {
        BannerSet set = findById(id);
        appendUploadedImages(set, imageUrls, titles, subtitles, objectPositions, zooms, set.getImages().size());
        return setRepo.save(set);
    }

    @Override
    @Transactional
    public BannerSet updateImages(Long id, List<BannerImageUpdateRequest> images) {
        BannerSet set = findById(id);
        if (images == null) {
            return set;
        }

        for (BannerImageUpdateRequest update : images) {
            if (update == null || update.getId() == null) continue;
            BannerImage image = set.getImages().stream()
                    .filter(item -> update.getId().equals(item.getId()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("BannerImage not found in BannerSet: " + update.getId()));
            image.setTitle(sanitizeNullable(update.getTitle(), 200));
            image.setSubtitle(sanitizeNullable(update.getSubtitle(), 300));
            image.setObjectPosition(normalizeObjectPosition(update.getObjectPosition()));
            image.setZoom(normalizeZoom(update.getZoom()));
            if (update.getSortOrder() != null && update.getSortOrder() >= 0) {
                image.setSortOrder(update.getSortOrder());
            }
        }
        return setRepo.save(set);
    }

    @Override
    @Transactional
    public void removeImage(Long imageId) {
        if (!imageRepo.existsById(imageId)) {
            throw new ResourceNotFoundException("BannerImage not found: " + imageId);
        }
        imageRepo.deleteById(imageId);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!setRepo.existsById(id)) {
            throw new ResourceNotFoundException("BannerSet not found: " + id);
        }
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
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                urls.add(null);
                continue;
            }
            try {
                String url = uploadService.saveImage(file);
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

    private void appendUploadedImages(BannerSet set, List<String> imageUrls,
                                      List<String> titles, List<String> subtitles,
                                      List<String> objectPositions, List<Double> zooms,
                                      int baseSortOrder) {
        if (imageUrls == null) return;
        for (int i = 0; i < imageUrls.size(); i++) {
            String url = imageUrls.get(i);
            if (url == null) continue;

            BannerImage img = new BannerImage();
            img.setBannerSet(set);
            img.setImage(url);
            img.setTitle(safeGet(titles, i, 200));
            img.setSubtitle(safeGet(subtitles, i, 300));
            img.setObjectPosition(normalizeObjectPosition(safeGetRaw(objectPositions, i)));
            img.setZoom(normalizeZoom(safeGetDouble(zooms, i)));
            img.setSortOrder(baseSortOrder + i);
            set.getImages().add(img);
        }
    }

    private String safeGet(List<String> list, int i, int maxLength) {
        return sanitizeNullable(safeGetRaw(list, i), maxLength);
    }

    private String safeGetRaw(List<String> list, int i) {
        if (list == null || i >= list.size()) return null;
        return list.get(i);
    }

    private Double safeGetDouble(List<Double> list, int i) {
        if (list == null || i >= list.size()) return null;
        return list.get(i);
    }

    private String sanitizeNullable(String value, int maxLength) {
        if (value == null || value.isBlank()) return null;
        String sanitized = value.replaceAll("<[^>]*>", "").trim();
        return sanitized.length() > maxLength ? sanitized.substring(0, maxLength) : sanitized;
    }

    private String normalizeObjectPosition(String value) {
        if (value == null || value.isBlank()) return "center";
        String normalized = value.trim().toLowerCase();
        return switch (normalized) {
            case "center", "top", "bottom", "left", "right" -> normalized;
            default -> throw new BusinessRuleException("Vi tri anh banner khong hop le");
        };
    }

    private Double normalizeZoom(Double value) {
        if (value == null) return 1.0;
        if (value < 1.0 || value > 1.5) {
            throw new BusinessRuleException("Zoom anh banner phai nam trong khoang 1.0 den 1.5");
        }
        return Math.round(value * 100.0) / 100.0;
    }
}
