package com.springboot.jenka_coffee.api.admin;

import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.dto.BannerImageUpdateRequest;
import com.springboot.jenka_coffee.entity.BannerSet;
import com.springboot.jenka_coffee.service.BannerSetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/admin/banners")
@RequiredArgsConstructor
public class ApiAdminBannerController {

    private static final String SUPPORTED_EFFECT_PATTERN =
            "^(fade|slide|zoom|kenburns|push|curtain|parallax|liquid|wave|magnetic|blur|vortex|glitch|cube|flip|dissolve|scale-rotate|prism|none)$";

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
            @RequestParam(value = "effect", defaultValue = "fade") String effect,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @RequestParam(value = "titles", required = false) List<String> titles,
            @RequestParam(value = "subtitles", required = false) List<String> subtitles,
            @RequestParam(value = "headlines", required = false) List<String> headlines,
            @RequestParam(value = "subHeadlines", required = false) List<String> subHeadlines,
            @RequestParam(value = "primaryCtaTexts", required = false) List<String> primaryCtaTexts,
            @RequestParam(value = "primaryCtaLinks", required = false) List<String> primaryCtaLinks,
            @RequestParam(value = "secondaryCtaTexts", required = false) List<String> secondaryCtaTexts,
            @RequestParam(value = "secondaryCtaLinks", required = false) List<String> secondaryCtaLinks,
            @RequestParam(value = "displayModes", required = false) List<String> displayModes,
            @RequestParam(value = "targetLinks", required = false) List<String> targetLinks,
            @RequestParam(value = "actives", required = false) List<Boolean> actives,
            @RequestParam(value = "sortOrders", required = false) List<Integer> sortOrders,
            @RequestParam(value = "imageCropXs", required = false) List<BigDecimal> imageCropXs,
            @RequestParam(value = "imageCropYs", required = false) List<BigDecimal> imageCropYs,
            @RequestParam(value = "imageCropWidths", required = false) List<BigDecimal> imageCropWidths,
            @RequestParam(value = "imageCropHeights", required = false) List<BigDecimal> imageCropHeights,
            @RequestParam(value = "imageZooms", required = false) List<BigDecimal> imageZooms) {

        if (name == null || name.isBlank() || name.length() > 100) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Ten banner khong hop le (toi da 100 ky tu)"));
        }
        if (!effect.matches(SUPPORTED_EFFECT_PATTERN)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Hieu ung khong hop le"));
        }

        String safeName = name.replaceAll("<[^>]*>", "").trim();
        BannerSet saved = bannerSetService.create(
                safeName, effect, images, titles, subtitles,
                headlines, subHeadlines, primaryCtaTexts, primaryCtaLinks,
                secondaryCtaTexts, secondaryCtaLinks, displayModes, targetLinks, actives, sortOrders,
                imageCropXs, imageCropYs, imageCropWidths, imageCropHeights, imageZooms
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tao bo banner thanh cong", saved));
    }

    @PutMapping("/{id}/meta")
    public ResponseEntity<ApiResponse<BannerSet>> updateMeta(
            @PathVariable Long id,
            @RequestParam("name") String name,
            @RequestParam(value = "effect", defaultValue = "fade") String effect) {

        if (name == null || name.isBlank() || name.length() > 100) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Ten banner khong hop le (toi da 100 ky tu)"));
        }
        if (!effect.matches(SUPPORTED_EFFECT_PATTERN)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Hieu ung khong hop le"));
        }

        String safeName = name.replaceAll("<[^>]*>", "").trim();
        return ResponseEntity.ok(ApiResponse.success("Cap nhat thanh cong",
                bannerSetService.updateMeta(id, safeName, effect)));
    }

    @PostMapping(value = "/{id}/images", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<BannerSet>> addImages(
            @PathVariable Long id,
            @RequestParam("images") List<MultipartFile> images,
            @RequestParam(value = "titles", required = false) List<String> titles,
            @RequestParam(value = "subtitles", required = false) List<String> subtitles,
            @RequestParam(value = "headlines", required = false) List<String> headlines,
            @RequestParam(value = "subHeadlines", required = false) List<String> subHeadlines,
            @RequestParam(value = "primaryCtaTexts", required = false) List<String> primaryCtaTexts,
            @RequestParam(value = "primaryCtaLinks", required = false) List<String> primaryCtaLinks,
            @RequestParam(value = "secondaryCtaTexts", required = false) List<String> secondaryCtaTexts,
            @RequestParam(value = "secondaryCtaLinks", required = false) List<String> secondaryCtaLinks,
            @RequestParam(value = "displayModes", required = false) List<String> displayModes,
            @RequestParam(value = "targetLinks", required = false) List<String> targetLinks,
            @RequestParam(value = "actives", required = false) List<Boolean> actives,
            @RequestParam(value = "sortOrders", required = false) List<Integer> sortOrders,
            @RequestParam(value = "imageCropXs", required = false) List<BigDecimal> imageCropXs,
            @RequestParam(value = "imageCropYs", required = false) List<BigDecimal> imageCropYs,
            @RequestParam(value = "imageCropWidths", required = false) List<BigDecimal> imageCropWidths,
            @RequestParam(value = "imageCropHeights", required = false) List<BigDecimal> imageCropHeights,
            @RequestParam(value = "imageZooms", required = false) List<BigDecimal> imageZooms) {

        return ResponseEntity.ok(ApiResponse.success("Da them anh",
                bannerSetService.addImages(
                        id, images, titles, subtitles,
                        headlines, subHeadlines, primaryCtaTexts, primaryCtaLinks,
                        secondaryCtaTexts, secondaryCtaLinks, displayModes, targetLinks, actives, sortOrders,
                        imageCropXs, imageCropYs, imageCropWidths, imageCropHeights, imageZooms
                )));
    }

    @PutMapping("/{id}/images")
    public ResponseEntity<ApiResponse<BannerSet>> updateImages(
            @PathVariable Long id,
            @RequestBody List<BannerImageUpdateRequest> images) {
        return ResponseEntity.ok(ApiResponse.success("Cap nhat anh banner thanh cong",
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
}
