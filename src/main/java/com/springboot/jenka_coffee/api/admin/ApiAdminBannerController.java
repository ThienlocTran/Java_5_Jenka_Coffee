package com.springboot.jenka_coffee.api.admin;

import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.entity.BannerSet;
import com.springboot.jenka_coffee.service.BannerSetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/banners")
@RequiredArgsConstructor
public class ApiAdminBannerController {

    private final BannerSetService bannerSetService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BannerSet>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("OK", bannerSetService.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BannerSet>> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("OK", bannerSetService.findById(id)));
    }

    /** Tạo bộ mới + upload ảnh cùng lúc */
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<BannerSet>> create(
            @RequestParam("name") String name,
            @RequestParam(value = "effect", defaultValue = "fade") String effect,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @RequestParam(value = "titles", required = false) List<String> titles,
            @RequestParam(value = "subtitles", required = false) List<String> subtitles) {

        // VULN-069 FIX: Validate name và effect
        if (name == null || name.isBlank() || name.length() > 100) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Tên banner không hợp lệ (tối đa 100 ký tự)"));
        }
        if (!effect.matches("^(fade|slide|zoom|kenburns|none)$")) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Hiệu ứng không hợp lệ"));
        }
        // Strip HTML từ name
        String safeName = name.replaceAll("<[^>]*>", "").trim();

        BannerSet saved = bannerSetService.create(safeName, effect, images, titles, subtitles);
        return ResponseEntity.ok(ApiResponse.success("Tạo bộ banner thành công", saved));
    }

    /** Cập nhật tên + hiệu ứng */
    @PutMapping("/{id}/meta")
    public ResponseEntity<ApiResponse<BannerSet>> updateMeta(
            @PathVariable Long id,
            @RequestParam("name") String name,
            @RequestParam(value = "effect", defaultValue = "fade") String effect) {

        return ResponseEntity.ok(ApiResponse.success("Cập nhật thành công",
            bannerSetService.updateMeta(id, name, effect)));
    }

    /** Thêm ảnh vào bộ đã có */
    @PostMapping(value = "/{id}/images", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<BannerSet>> addImages(
            @PathVariable Long id,
            @RequestParam("images") List<MultipartFile> images,
            @RequestParam(value = "titles", required = false) List<String> titles,
            @RequestParam(value = "subtitles", required = false) List<String> subtitles) {

        return ResponseEntity.ok(ApiResponse.success("Đã thêm ảnh",
            bannerSetService.addImages(id, images, titles, subtitles)));
    }

    /** Xóa 1 ảnh */
    @DeleteMapping("/images/{imageId}")
    public ResponseEntity<ApiResponse<Void>> removeImage(@PathVariable Long imageId) {
        bannerSetService.removeImage(imageId);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa ảnh", null));
    }

    /** Xóa cả bộ */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        bannerSetService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa bộ banner", null));
    }

    /** Kích hoạt bộ này lên trang chủ */
    @PutMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<BannerSet>> activate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Đã kích hoạt bộ banner",
            bannerSetService.activate(id)));
    }
}
