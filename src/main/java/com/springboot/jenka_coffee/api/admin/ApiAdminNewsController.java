package com.springboot.jenka_coffee.api.admin;

import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.dto.request.NewsRequest;
import com.springboot.jenka_coffee.entity.News;
import com.springboot.jenka_coffee.service.NewsService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Map;

import static com.springboot.jenka_coffee.api.ApiNewsController.getApiResponseResponseEntity;

@RestController
@RequestMapping("/api/admin/news")
public class ApiAdminNewsController {

    private final NewsService newsService;

    public ApiAdminNewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getNewsList(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        size = Math.min(Math.max(size, 1), 100);
        page = Math.max(page, 0);

        Pageable pageable = PageRequest.of(page, size);
        Page<News> newsPage = newsService.findAllPaginated(pageable);

        return getApiResponseResponseEntity(newsPage);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<News>> getNewsDetail(@PathVariable Integer id) {
        News news = newsService.findById(id);
        if (news == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Khong tim thay tin tuc: " + id));
        }
        return ResponseEntity.ok(ApiResponse.success("Lay thong tin tin tuc thanh cong", news));
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<News>> createNews(
            @Valid @ModelAttribute NewsRequest request,
            @RequestParam(value = "imageFile", required = false) MultipartFile file) {

        News news = new News();
        news.setTitle(sanitize(request.getTitle()));
        news.setSlug(normalizeOptional(request.getSlug()));
        news.setContent(request.getContent());
        news.setCreateDate(LocalDateTime.now());
        news.setAvailable(true);

        News savedNews = newsService.saveNews(news, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tao tin tuc thanh cong", savedNews));
    }

    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<News>> updateNews(
            @PathVariable Integer id,
            @Valid @ModelAttribute NewsRequest request,
            @RequestParam(value = "imageFile", required = false) MultipartFile file) {

        News existing = newsService.findById(id);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Khong tim thay tin tuc: " + id));
        }

        existing.setTitle(sanitize(request.getTitle()));
        existing.setSlug(normalizeOptional(request.getSlug()));
        existing.setContent(request.getContent());

        News savedNews = newsService.saveNews(existing, file);
        return ResponseEntity.ok(ApiResponse.success("Cap nhat tin tuc thanh cong", savedNews));
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<Void>> toggleAvailable(@PathVariable Integer id) {
        newsService.toggleAvailable(id);
        return ResponseEntity.ok(ApiResponse.success("Thay doi trang thai tin tuc thanh cong", null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNews(@PathVariable Integer id) {
        newsService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Xoa tin tuc thanh cong", null));
    }

    private String sanitize(String input) {
        if (input == null) {
            return null;
        }
        return input.replaceAll("<[^>]*>", "").trim();
    }

    private String normalizeOptional(String input) {
        if (input == null) {
            return null;
        }
        String value = input.trim();
        return value.isEmpty() ? null : value;
    }
}
