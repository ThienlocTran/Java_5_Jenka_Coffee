package com.springboot.jenka_coffee.api;

import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.entity.BannerSet;
import com.springboot.jenka_coffee.service.BannerSetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/banners")
@RequiredArgsConstructor
public class ApiBannerController {

    private final BannerSetService bannerSetService;

    /** Public — trả về bộ banner đang active */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<BannerSet>> getActive() {
        BannerSet active = bannerSetService.getActive();
        return ResponseEntity.ok(ApiResponse.success("OK", active));
    }
}
