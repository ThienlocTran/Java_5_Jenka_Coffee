package com.springboot.jenka_coffee.api.admin;

import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.dto.request.BannerImageUpdateRequest;
import com.springboot.jenka_coffee.entity.BannerEffect;
import com.springboot.jenka_coffee.entity.BannerSet;
import com.springboot.jenka_coffee.service.BannerSetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<BannerSet>> create(
            @RequestParam("name") String name,
            @RequestParam(value = "effect", defaultValue = BannerEffect.DEFAULT_VALUE) String effect,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @RequestParam(value = "titles", required = false) List<String> titles,
            @RequestParam(value = "subtitles", required = false) List<String> subtitles,
            @RequestParam(value = "objectPositions", required = false) List<String> objectPositions,
            @RequestParam(value = "zooms", required = false) List<Double> zooms) {

        String safeName = validateAndSanitizeName(name);
        String safeEffect = validateAndNormalizeEffect(effect);

        BannerSet saved = bannerSetService.create(safeName, safeEffect, images, titles, subtitles, objectPositions, zooms);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tao bo banner thanh cong", saved));
    }

    @PutMapping("/{id}/meta")
    public ResponseEntity<ApiResponse<BannerSet>> updateMeta(
            @PathVariable Long id,
            @RequestParam("name") String name,
            @RequestParam(value = "effect", defaultValue = BannerEffect.DEFAULT_VALUE) String effect) {

        String safeName = validateAndSanitizeName(name);
        String safeEffect = validateAndNormalizeEffect(effect);

        return ResponseEntity.ok(ApiResponse.success("Cap nhat thanh cong",
                bannerSetService.updateMeta(id, safeName, safeEffect)));
    }

    @PostMapping(value = "/{id}/images", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<BannerSet>> addImages(
            @PathVariable Long id,
            @RequestParam("images") List<MultipartFile> images,
            @RequestParam(value = "titles", required = false) List<String> titles,
            @RequestParam(value = "subtitles", required = false) List<String> subtitles,
            @RequestParam(value = "objectPositions", required = false) List<String> objectPositions,
            @RequestParam(value = "zooms", required = false) List<Double> zooms) {

        return ResponseEntity.ok(ApiResponse.success("Da them anh",
                bannerSetService.addImages(id, images, titles, subtitles, objectPositions, zooms)));
    }

    @PutMapping("/{id}/images")
    public ResponseEntity<ApiResponse<BannerSet>> updateImages(
            @PathVariable Long id,
            @RequestBody List<BannerImageUpdateRequest> images) {

        return ResponseEntity.ok(ApiResponse.success("Cap nhat anh thanh cong",
                bannerSetService.updateImages(id, images)));
    }

    @DeleteMapping("/images/{imageId}")
    public ResponseEntity<ApiResponse<Void>> removeImage(@PathVariable Long imageId) {
        bannerSetService.removeImage(imageId);
        return ResponseEntity.ok(ApiResponse.success("Da xoa anh", null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        bannerSetService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Da xoa bo banner", null));
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<BannerSet>> activate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Da kich hoat bo banner",
                bannerSetService.activate(id)));
    }

    private String validateAndSanitizeName(String name) {
        if (name == null || name.isBlank() || name.length() > 100) {
            throw new IllegalArgumentException("Ten banner khong hop le (toi da 100 ky tu)");
        }
        String safeName = name.replaceAll("<[^>]*>", "").trim();
        if (safeName.isBlank()) {
            throw new IllegalArgumentException("Ten banner khong hop le");
        }
        return safeName;
    }

    private String validateAndNormalizeEffect(String effect) {
        if (!BannerEffect.isValid(effect)) {
            throw new IllegalArgumentException("Hieu ung khong hop le");
        }
        return BannerEffect.normalize(effect);
    }
}
