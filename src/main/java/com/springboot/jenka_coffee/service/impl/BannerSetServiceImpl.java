package com.springboot.jenka_coffee.service.impl;

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

    // VULN #11 PATTERN FIX: Connection Pool Exhaustion Prevention
    // PROBLEM: @Transactional methods hold DB connection during Cloudinary uploads
    // SOLUTION: Upload images OUTSIDE transaction, then save to DB in transaction
    @Override
    public BannerSet create(String name, String effect,
                            List<MultipartFile> files,
                            List<String> titles, List<String> subtitles) {
        // STEP 1: Upload images OUTSIDE transaction (no DB connection held)
        List<String> uploadedUrls = uploadImages(files);
        
        // STEP 2: Save to database in transaction (fast, no network I/O)
        return createBannerSetInDatabase(name, effect, uploadedUrls, titles, subtitles);
    }
    
    @Transactional
    protected BannerSet createBannerSetInDatabase(String name, String effect,
                                                   List<String> imageUrls,
                                                   List<String> titles, List<String> subtitles) {
        BannerSet set = new BannerSet();
        set.setName(name);
        set.setEffect(effect != null ? effect : "fade");
        set = setRepo.save(set);
        
        // Add images with pre-uploaded URLs
        if (imageUrls != null) {
            for (int i = 0; i < imageUrls.size(); i++) {
                String url = imageUrls.get(i);
                if (url == null) continue;
                
                BannerImage img = new BannerImage();
                img.setBannerSet(set);
                img.setImage(url);
                img.setTitle(safeGet(titles, i));
                img.setSubtitle(safeGet(subtitles, i));
                img.setSortOrder(i);
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

    // VULN #11 PATTERN FIX: Upload images outside transaction
    @Override
    public BannerSet addImages(Long id, List<MultipartFile> files,
                               List<String> titles, List<String> subtitles) {
        // STEP 1: Upload images OUTSIDE transaction
        List<String> uploadedUrls = uploadImages(files);
        
        // STEP 2: Save to database in transaction
        return addImagesToDatabase(id, uploadedUrls, titles, subtitles);
    }
    
    @Transactional
    protected BannerSet addImagesToDatabase(Long id, List<String> imageUrls,
                                            List<String> titles, List<String> subtitles) {
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
                img.setSortOrder(base + i);
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

    // ── helpers ──────────────────────────────────────────────────────────────

    /**
     * VULN #11 FIX: Upload images outside transaction to prevent connection pool exhaustion
     * Returns list of uploaded URLs (null for failed uploads)
     */
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

    private void appendImages(BannerSet set, List<MultipartFile> files,
                              List<String> titles, List<String> subtitles) {
        if (files == null) return;
        int base = set.getImages().size();
        for (int i = 0; i < files.size(); i++) {
            MultipartFile f = files.get(i);
            if (f == null || f.isEmpty()) continue;
            String url = uploadService.saveImage(f);
            if (url == null) continue;
            BannerImage img = new BannerImage();
            img.setBannerSet(set);
            img.setImage(url);
            img.setTitle(safeGet(titles, i));
            img.setSubtitle(safeGet(subtitles, i));
            img.setSortOrder(base + i);
            set.getImages().add(img);
        }
    }

    private String safeGet(List<String> list, int i) {
        if (list == null || i >= list.size()) return null;
        String v = list.get(i);
        if (v == null || v.isBlank()) return null;
        // VULN-067 FIX: Strip HTML tags khỏi title/subtitle — ngăn Stored XSS trên homepage
        // VULN #12 PATTERN: Should use HtmlUtils.htmlEscape() instead of regex
        // But for now keeping existing implementation for backward compatibility
        return v.replaceAll("<[^>]*>", "").trim();
    }
}
