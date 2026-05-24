package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.entity.News;
import com.springboot.jenka_coffee.repository.NewsRepository;
import com.springboot.jenka_coffee.service.NewsService;
import com.springboot.jenka_coffee.service.UploadService;
import com.springboot.jenka_coffee.service.VercelWebhookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.text.Normalizer;
import java.util.List;

@Slf4j
@Service
public class NewsServiceImpl implements NewsService {

    private final NewsRepository newsRepository;
    private final UploadService uploadService;
    private final VercelWebhookService vercelWebhookService;

    public NewsServiceImpl(NewsRepository newsRepository, UploadService uploadService, VercelWebhookService vercelWebhookService) {
        this.newsRepository = newsRepository;
        this.uploadService = uploadService;
        this.vercelWebhookService = vercelWebhookService;
    }

    @Override
    public List<News> findAll() {
        return newsRepository.findAll();
    }

    @Override
    public News findById(Integer id) {
        return newsRepository.findById(id).orElse(null);
    }

    @Override
    public News findBySlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return null;
        }
        return newsRepository.findBySlugIgnoreCase(slug.trim()).orElse(null);
    }

    @Override
    public News findAvailableBySlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return null;
        }
        return newsRepository.findBySlugIgnoreCaseAndAvailableTrue(slug.trim()).orElse(null);
    }

    @Override
    public News create(News news) {
        if (news.getAvailable() == null) {
            news.setAvailable(true);
        }
        news.setSlug(resolveUniqueSlug(news.getSlug(), news.getTitle(), null));
        News savedNews = newsRepository.save(news);
        log.info("Successfully created news with ID: {} and slug: {}", savedNews.getId(), savedNews.getSlug());
        
        // Trigger Vercel rebuild after successful create
        vercelWebhookService.triggerRebuild();
        
        return savedNews;
    }

    @Override
    public News update(News news) {
        news.setSlug(resolveUniqueSlug(news.getSlug(), news.getTitle(), news.getId()));
        News updatedNews = newsRepository.save(news);
        log.info("Successfully updated news with ID: {} and slug: {}", updatedNews.getId(), updatedNews.getSlug());
        
        // Trigger Vercel rebuild after successful update
        vercelWebhookService.triggerRebuild();
        
        return updatedNews;
    }

    @Override
    public void delete(Integer id) {
        newsRepository.deleteById(id);
        log.info("Successfully deleted news with ID: {}", id);
        
        // Trigger Vercel rebuild after successful delete
        vercelWebhookService.triggerRebuild();
    }

    @Override
    public News saveNews(News news, MultipartFile file) {
        // Handle image upload
        if (file != null && !file.isEmpty()) {
            String url = uploadService.saveNewsImage(file);
            if (url != null) {
                news.setImage(url);
            }
        }
        if (news.getId() == null && news.getAvailable() == null) {
            news.setAvailable(true);
        }
        news.setSlug(resolveUniqueSlug(news.getSlug(), news.getTitle(), news.getId()));
        News savedNews = newsRepository.save(news);
        log.info("Successfully saved news with ID: {} and slug: {}", savedNews.getId(), savedNews.getSlug());
        
        // Trigger Vercel rebuild after successful save
        vercelWebhookService.triggerRebuild();

        return savedNews;
    }

    @Override
    public void toggleAvailable(Integer id) {
        News news = newsRepository.findById(id).orElse(null);
        if (news != null) {
            news.setAvailable(!news.getAvailable());
            newsRepository.save(news);
            log.info("Successfully toggled availability for news ID: {} to {}", id, news.getAvailable());
            
            // Trigger Vercel rebuild after successful toggle
            vercelWebhookService.triggerRebuild();
        }
    }

    @Override
    public List<News> findAvailableNews() {
        return newsRepository.findByAvailableTrueOrderByCreateDateDesc();
    }

    @Override
    public Page<News> findAllPaginated(Pageable pageable) {
        return newsRepository.findAllByOrderByCreateDateDesc(pageable);
    }

    @Override
    public Page<News> findAvailableNewsPaginated(Pageable pageable) {
        return newsRepository.findByAvailableTrueOrderByCreateDateDesc(pageable);
    }

    private String resolveUniqueSlug(String requestedSlug, String title, Integer currentId) {
        String baseSlug = toNewsSlug(
                requestedSlug != null && !requestedSlug.isBlank() ? requestedSlug : title
        );

        if (baseSlug == null || baseSlug.isBlank()) {
            baseSlug = "news";
        }

        String candidate = baseSlug;
        int suffix = 2;
        while (slugExistsForAnotherNews(candidate, currentId)) {
            candidate = baseSlug + "-" + suffix;
            suffix++;
        }

        return candidate;
    }

    private boolean slugExistsForAnotherNews(String slug, Integer currentId) {
        if (currentId == null) {
            return newsRepository.existsBySlugIgnoreCase(slug);
        }
        return newsRepository.existsBySlugIgnoreCaseAndIdNot(slug, currentId);
    }

    private String toNewsSlug(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String normalized = Normalizer.normalize(
                value.replace('Đ', 'D').replace('đ', 'd'),
                Normalizer.Form.NFD
        );

        String slug = normalized
                .replaceAll("\\p{M}+", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-+|-+$", "");

        return slug;
    }
}
