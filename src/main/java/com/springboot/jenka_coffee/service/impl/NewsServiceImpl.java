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
    public News create(News news) {
        News savedNews = newsRepository.save(news);
        log.info("Successfully created news with ID: {}", savedNews.getId());
        
        // Trigger Vercel rebuild after successful create
        vercelWebhookService.triggerRebuild();
        
        return savedNews;
    }

    @Override
    public News update(News news) {
        News updatedNews = newsRepository.save(news);
        log.info("Successfully updated news with ID: {}", updatedNews.getId());
        
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
    public void saveNews(News news, MultipartFile file) {
        // Handle image upload
        if (file != null && !file.isEmpty()) {
            String url = uploadService.saveNewsImage(file);
            if (url != null) {
                news.setImage(url);
            }
        }
        News savedNews = newsRepository.save(news);
        log.info("Successfully saved news with ID: {}", savedNews.getId());
        
        // Trigger Vercel rebuild after successful save
        vercelWebhookService.triggerRebuild();

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
}
