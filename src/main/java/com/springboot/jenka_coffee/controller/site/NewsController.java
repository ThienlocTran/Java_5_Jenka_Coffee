package com.springboot.jenka_coffee.controller.site;

import com.springboot.jenka_coffee.entity.News;
import com.springboot.jenka_coffee.service.NewsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/news")
public class NewsController {

    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    // 1. News List Page
    @GetMapping("/list")
    public String list(Model model,
            @RequestParam(value = "page", defaultValue = "0") int page) {
        Pageable pageable = PageRequest.of(page, 9);
        Page<News> newsPage = newsService.findAvailableNewsPaginated(pageable);

        model.addAttribute("items", newsPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", newsPage.getTotalPages());
        return "site/news/news-list";
    }

    // 2. News Detail Page
    @GetMapping("/detail/{id}")
    public String detail(@PathVariable("id") Integer id, Model model) {
        News news = newsService.findById(id);
        if (news == null || !news.getAvailable()) {
            throw new com.springboot.jenka_coffee.exception.ResourceNotFoundException(
                    "Không tìm thấy tin tức với ID: " + id);
        }
        model.addAttribute("item", news);
        return "site/news/news-detail";
    }
}
