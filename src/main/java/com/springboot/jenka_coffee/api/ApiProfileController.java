package com.springboot.jenka_coffee.api;

import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.dto.request.ProfileUpdateRequest;
import com.springboot.jenka_coffee.entity.Account;
import com.springboot.jenka_coffee.exception.BusinessRuleException;
import com.springboot.jenka_coffee.exception.ValidationException;
import com.springboot.jenka_coffee.service.ProfileService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Slf4j
public class ApiProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ResponseEntity<ApiResponse<Account>> getProfile(HttpSession session) {
        String username = getCurrentUsername(session);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Vui lòng đăng nhập"));
        }

        try {
            Account account = profileService.getProfile(username);

            // Ẩn mật khẩu khi trả về JSON (Có thể dùng DTO ở tương lai)
            account.setPasswordHash(null);

            return ResponseEntity.ok(ApiResponse.success("Lấy thông tin hồ sơ thành công", account));
        } catch (Exception e) {
            log.error("Error loading profile for user: {}", username, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Không thể tải thông tin hồ sơ"));
        }
    }

    @PutMapping
    public ResponseEntity<ApiResponse<Account>> updateProfile(
            @RequestBody ProfileUpdateRequest request,
            HttpSession session) {

        String username = getCurrentUsername(session);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Vui lòng đăng nhập"));
        }

        try {
            Account updatedAccount = profileService.updateProfile(username, request);

            // Cập nhật lại session
            session.setAttribute("user", updatedAccount);
            updatedAccount.setPasswordHash(null);

            return ResponseEntity.ok(ApiResponse.success("Cập nhật hồ sơ thành công", updatedAccount));

        } catch (ValidationException | BusinessRuleException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating profile for user: {}", username, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Có lỗi xảy ra khi cập nhật hồ sơ"));
        }
    }

    @PostMapping("/avatar")
    public ResponseEntity<ApiResponse<Account>> updateAvatar(
            @RequestParam("avatarFile") MultipartFile avatarFile,
            HttpSession session) {

        String username = getCurrentUsername(session);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Vui lòng đăng nhập"));
        }

        if (avatarFile.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("Vui lòng chọn file ảnh"));
        }

        String contentType = avatarFile.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("File phải là ảnh (JPG, PNG, GIF)"));
        }

        if (avatarFile.getSize() > 5 * 1024 * 1024) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Kích thước file không được vượt quá 5MB"));
        }

        try {
            Account updatedAccount = profileService.updateAvatar(username, avatarFile);

            // Update session
            session.setAttribute("user", updatedAccount);
            updatedAccount.setPasswordHash(null);

            return ResponseEntity.ok(ApiResponse.success("Cập nhật ảnh đại diện thành công", updatedAccount));

        } catch (BusinessRuleException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating avatar for user: {}", username, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Có lỗi xảy ra khi cập nhật ảnh đại diện"));
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestBody ProfileUpdateRequest request,
            HttpSession session) {

        String username = getCurrentUsername(session);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Vui lòng đăng nhập"));
        }

        try {
            profileService.changePassword(username, request.getCurrentPassword(), request.getNewPassword());
            return ResponseEntity.ok(ApiResponse.success("Đổi mật khẩu thành công!", null));

        } catch (ValidationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error changing password for user: {}", username, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Có lỗi xảy ra khi đổi mật khẩu"));
        }
    }

    private String getCurrentUsername(HttpSession session) {
        Account user = (Account) session.getAttribute("user");
        return user != null ? user.getUsername() : null;
    }
}
