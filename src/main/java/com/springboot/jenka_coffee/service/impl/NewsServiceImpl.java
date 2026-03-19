package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.entity.News;
import com.springboot.jenka_coffee.repository.NewsRepository;
import com.springboot.jenka_coffee.service.NewsService;
import com.springboot.jenka_coffee.service.UploadService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class NewsServiceImpl implements NewsService {

    private final NewsRepository newsRepository;
    private final UploadService uploadService;

    public NewsServiceImpl(NewsRepository newsRepository, UploadService uploadService) {
        this.newsRepository = newsRepository;
        this.uploadService = uploadService;
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
        return newsRepository.save(news);
    }

    @Override
    public News update(News news) {
        return newsRepository.save(news);
    }

    @Override
    public void delete(Integer id) {
        newsRepository.deleteById(id);
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
        return newsRepository.save(news);
    }

    @Override
    public void toggleAvailable(Integer id) {
        News news = newsRepository.findById(id).orElse(null);
        if (news != null) {
            news.setAvailable(!news.getAvailable());
            newsRepository.save(news);
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
