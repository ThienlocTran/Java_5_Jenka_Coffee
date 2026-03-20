package com.springboot.jenka_coffee.api.admin;

import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.entity.News;
import com.springboot.jenka_coffee.service.NewsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping(consumes = { "multipart/form-data" })
    public ResponseEntity<ApiResponse<News>> saveNews(
            @ModelAttribute News news,
            @RequestParam(value = "imageFile", required = false) MultipartFile file) {
        newsService.saveNews(news, file);
        return ResponseEntity.ok(ApiResponse.success("Lưu tin tức thành công", news));
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
}
