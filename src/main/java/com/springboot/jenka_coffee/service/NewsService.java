package com.springboot.jenka_coffee.service;

import com.springboot.jenka_coffee.entity.News;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface NewsService {

    /**
     * Find all news (Admin)
     */
    List<News> findAll();

    /**
     * Find news by ID
     */
    News findById(Integer id);

    /**
     * Create new news
     */
    News create(News news);

    /**
     * Update existing news
     */
    News update(News news);

    /**
     * Delete news by ID
     */
    void delete(Integer id);

    /**
     * Save news with image upload
     */
    News saveNews(News news, MultipartFile file);

    /**
     * Toggle news availability
     */
    void toggleAvailable(Integer id);

    /**
     * Find all available news (Site)
     */
    List<News> findAvailableNews();

    /**
     * Find all news with pagination (Admin)
     */
    Page<News> findAllPaginated(Pageable pageable);

    /**
     * Find available news with pagination (Site)
     */
    Page<News> findAvailableNewsPaginated(Pageable pageable);
}
