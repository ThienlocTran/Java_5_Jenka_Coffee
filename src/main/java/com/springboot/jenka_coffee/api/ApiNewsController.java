package com.springboot.jenka_coffee.api;

import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.entity.News;
import com.springboot.jenka_coffee.service.NewsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/news")
public class ApiNewsController {

    private final NewsService newsService;

    public ApiNewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getNews(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "9") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<News> newsPage = newsService.findAvailableNewsPaginated(pageable);

        return getApiResponseResponseEntity(newsPage);
    }

    public static ResponseEntity<ApiResponse<Map<String, Object>>> getApiResponseResponseEntity(Page<News> newsPage) {
        Map<String, Object> data = new HashMap<>();
        data.put("items", newsPage.getContent());
        data.put("currentPage", newsPage.getNumber());
        data.put("totalPages", newsPage.getTotalPages());
        data.put("totalItems", newsPage.getTotalElements());

        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách tin tức thành công", data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<News>> getNewsDetail(@PathVariable("id") Integer id) {
        News news = newsService.findById(id);
        if (news == null || !news.getAvailable()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Không tìm thấy tin tức với ID: " + id));
        }

        return ResponseEntity.ok(ApiResponse.success("Lắng chi tiết tin tức thành công", news));
    }
}
