package com.springboot.jenka_coffee.api;

import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.dto.request.ProfileUpdateRequest;
import com.springboot.jenka_coffee.entity.Account;
import com.springboot.jenka_coffee.exception.BusinessRuleException;
import com.springboot.jenka_coffee.exception.ValidationException;
import com.springboot.jenka_coffee.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Slf4j
public class ApiProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ResponseEntity<ApiResponse<Account>> getProfile(@AuthenticationPrincipal String username) {
        if (username == null) return unauthorized();
        Account account = profileService.getProfile(username);
        account.setPasswordHash(null);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin hồ sơ thành công", account));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<Account>> updateProfile(
            @Valid @RequestBody ProfileUpdateRequest request,
            @AuthenticationPrincipal String username) {
        if (username == null) return unauthorized();
        try {
            Account updated = profileService.updateProfile(username, request);
            updated.setPasswordHash(null);
            return ResponseEntity.ok(ApiResponse.success("Cập nhật hồ sơ thành công", updated));
        } catch (ValidationException | BusinessRuleException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/avatar")
    public ResponseEntity<ApiResponse<Account>> updateAvatar(
            @RequestParam("avatarFile") MultipartFile avatarFile,
            @AuthenticationPrincipal String username) {
        if (username == null) return unauthorized();
        if (avatarFile.isEmpty()) return ResponseEntity.badRequest().body(ApiResponse.error("Vui lòng chọn file ảnh"));
        String ct = avatarFile.getContentType();
        if (ct == null || !ct.startsWith("image/"))
            return ResponseEntity.badRequest().body(ApiResponse.error("File phải là ảnh"));
        if (avatarFile.getSize() > 5 * 1024 * 1024)
            return ResponseEntity.badRequest().body(ApiResponse.error("File không được vượt quá 5MB"));
        try {
            Account updated = profileService.updateAvatar(username, avatarFile);
            updated.setPasswordHash(null);
            return ResponseEntity.ok(ApiResponse.success("Cập nhật ảnh đại diện thành công", updated));
        } catch (BusinessRuleException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ProfileUpdateRequest request,
            @AuthenticationPrincipal String username) {
        if (username == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Vui lòng đăng nhập"));
        try {
            profileService.changePassword(username, request.getCurrentPassword(), request.getNewPassword());
            return ResponseEntity.ok(ApiResponse.success("Đổi mật khẩu thành công!", null));
        } catch (ValidationException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @SuppressWarnings("unchecked")
    private <T> ResponseEntity<ApiResponse<T>> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body((ApiResponse<T>) ApiResponse.error("Vui lòng đăng nhập"));
    }
}
