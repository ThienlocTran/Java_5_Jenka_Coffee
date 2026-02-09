package com.springboot.jenka_coffee.controller.admin;

import com.springboot.jenka_coffee.entity.News;
import com.springboot.jenka_coffee.service.NewsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/admin/news")
public class AdminNewsController {

    private final NewsService newsService;

    public AdminNewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    // 1. Admin List View with Pagination
    @GetMapping("/list")
    public String index(Model model,
            @RequestParam(value = "page", defaultValue = "0") int page) {
        Pageable pageable = PageRequest.of(page, 10);
        Page<News> newsPage = newsService.findAllPaginated(pageable);

        model.addAttribute("items", newsPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", newsPage.getTotalPages());
        return "admin/news/news-index";
    }

    // 2. Create Form
    @GetMapping("/create")
    public String create(Model model) {
        News news = new News();
        model.addAttribute("item", news);
        return "admin/news/news-form";
    }

    // 3. Edit Form
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Integer id, Model model) {
        News news = newsService.findById(id);
        model.addAttribute("item", news);
        return "admin/news/news-form";
    }

    // 4. Save Action
    @PostMapping("/save")
    public String save(@ModelAttribute("item") News news,
            @RequestParam(value = "imageFile", required = false) MultipartFile file) {
        newsService.saveNews(news, file);
        return "redirect:/admin/news/list";
    }

    // 5. Toggle Available
    @GetMapping("/toggle/{id}")
    public String toggleAvailable(@PathVariable("id") Integer id) {
        newsService.toggleAvailable(id);
        return "redirect:/admin/news/list";
    }
}
