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
        return setRepo.findAll(org.springframework.data.domain.Sort.by("createdAt").descending());
    }

    @Override
    public BannerSet findById(Long id) {
        return setRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("BannerSet not found: " + id));
    }

    @Override
    @Transactional
    public BannerSet create(String name, String effect,
                            List<MultipartFile> files,
                            List<String> titles, List<String> subtitles) {
        BannerSet set = new BannerSet();
        set.setName(name);
        set.setEffect(effect != null ? effect : "fade");
        set = setRepo.save(set);
        appendImages(set, files, titles, subtitles);
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
    @Transactional
    public BannerSet addImages(Long id, List<MultipartFile> files,
                               List<String> titles, List<String> subtitles) {
        BannerSet set = findById(id);
        appendImages(set, files, titles, subtitles);
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
        return v.replaceAll("<[^>]*>", "").trim();
    }
}
