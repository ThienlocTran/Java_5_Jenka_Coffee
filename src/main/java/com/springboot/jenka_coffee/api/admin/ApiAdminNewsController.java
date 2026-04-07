package com.springboot.jenka_coffee.api.admin;

import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.dto.request.NewsRequest;
import com.springboot.jenka_coffee.entity.News;
import com.springboot.jenka_coffee.service.NewsService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

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

        // VULN-054 FIX: Giới hạn page size tránh Memory DoS
        size = Math.min(Math.max(size, 1), 100);
        page = Math.max(page, 0);

        Pageable pageable = PageRequest.of(page, size);
        Page<News> newsPage = newsService.findAllPaginated(pageable);

        Map<String, Object> data = new HashMap<>();
        data.put("items", newsPage.getContent());
        data.put("currentPage", newsPage.getNumber());
        data.put("totalPages", newsPage.getTotalPages());
        data.put("totalItems", newsPage.getTotalElements());

        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách tin tức thành công", data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<News>> getNewsDetail(@PathVariable Integer id) {
        News news = newsService.findById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin tin tức thành công", news));
    }

    /**
     * VULN-049 FIX: Dùng NewsRequest DTO thay vì @ModelAttribute News (raw entity).
     * Ngăn mass assignment: id, createDate, available, image URL không được client set.
     */
    @PostMapping(consumes = { "multipart/form-data" })
    public ResponseEntity<ApiResponse<News>> createNews(
            @Valid @ModelAttribute NewsRequest request,
            @RequestParam(value = "imageFile", required = false) MultipartFile file) {

        News news = new News();
        news.setTitle(sanitize(request.getTitle()));
        news.setContent(request.getContent()); // HTML content — sanitize ở frontend hoặc dùng OWASP
        news.setCreateDate(LocalDateTime.now()); // server-side, không tin client
        news.setAvailable(false); // default unpublished — admin toggle riêng
        // id = null → INSERT, không thể override thành UPDATE

        newsService.saveNews(news, file);
        return ResponseEntity.ok(ApiResponse.success("Tạo tin tức thành công", news));
    }

    @PutMapping(value = "/{id}", consumes = { "multipart/form-data" })
    public ResponseEntity<ApiResponse<News>> updateNews(
            @PathVariable Integer id,
            @Valid @ModelAttribute NewsRequest request,
            @RequestParam(value = "imageFile", required = false) MultipartFile file) {

        News existing = newsService.findById(id);
        existing.setTitle(sanitize(request.getTitle()));
        existing.setContent(request.getContent());
        // createDate, available, id giữ nguyên từ DB — không tin client

        newsService.saveNews(existing, file);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật tin tức thành công", existing));
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<Void>> toggleAvailable(@PathVariable Integer id) {
        newsService.toggleAvailable(id);
        return ResponseEntity.ok(ApiResponse.success("Thay đổi trạng thái tin tức thành công", null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNews(@PathVariable Integer id) {
        newsService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa tin tức thành công", null));
    }

    /** Strip HTML tags cơ bản cho title — ngăn XSS trong tiêu đề */
    private String sanitize(String input) {
        if (input == null) return null;
        return input.replaceAll("<[^>]*>", "").trim();
    }
}
