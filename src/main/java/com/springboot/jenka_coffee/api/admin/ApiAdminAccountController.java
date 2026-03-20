package com.springboot.jenka_coffee.api.admin;

import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.dto.request.AccountRequest;
import com.springboot.jenka_coffee.entity.Account;
import com.springboot.jenka_coffee.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/accounts")
public class ApiAdminAccountController {

    private final AccountService accountService;

    public ApiAdminAccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<Account>>> listAccounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Account> result = accountService.findAllPaginated(PageRequest.of(page, size));
        result.forEach(a -> a.setPasswordHash(null));
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách người dùng thành công", result));
    }

    @GetMapping("/{username}")
    public ResponseEntity<ApiResponse<Account>> getAccount(@PathVariable String username) {
        Account account = accountService.findByIdOrThrow(username);
        account.setPasswordHash(null);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin tài khoản thành công", account));
    }

    @PostMapping(consumes = { "multipart/form-data" })
    public ResponseEntity<ApiResponse<Account>> createAccount(
            @Valid @ModelAttribute AccountRequest request,
            @RequestParam(value = "photoFile", required = false) MultipartFile photoFile) {
        try {
            // username is required for create — validate manually since DTO is shared with update
            if (request.getUsername() == null || request.getUsername().isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Tên đăng nhập không được để trống"));
            }
            Account account = accountService.createAccount(request.toEntity(), photoFile);
            account.setPasswordHash(null);
            return ResponseEntity.ok(ApiResponse.success("Thêm tài khoản mới thành công", account));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Lỗi khi thêm tài khoản: " + e.getMessage()));
        }
    }

    @PutMapping(value = "/{username}", consumes = { "multipart/form-data" })
    public ResponseEntity<ApiResponse<Account>> updateAccount(
            @PathVariable String username,
            @Valid @ModelAttribute AccountRequest request,
            @RequestParam(value = "photoFile", required = false) MultipartFile photoFile) {
        try {
            Account account = accountService.updateAccount(username, request.toEntity(), photoFile);
            account.setPasswordHash(null);
            return ResponseEntity.ok(ApiResponse.success("Cập nhật tài khoản thành công", account));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Lỗi khi cập nhật tài khoản: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{username}")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(@PathVariable String username) {
        try {
            accountService.deleteOrThrow(username);
            return ResponseEntity.ok(ApiResponse.success("Xóa tài khoản thành công", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Không thể xóa tài khoản: " + e.getMessage()));
        }
    }

    @PutMapping("/{username}/toggle-status")
    public ResponseEntity<ApiResponse<Account>> toggleAccountStatus(@PathVariable String username) {
        try {
            Account account = accountService.toggleActivation(username);
            account.setPasswordHash(null);
            String status = account.getActivated() ? "kích hoạt" : "vô hiệu hóa";
            return ResponseEntity.ok(ApiResponse.success("Đã " + status + " tài khoản thành công!", account));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Lỗi: " + e.getMessage()));
        }
    }

    @PutMapping("/{username}/lock")
    public ResponseEntity<ApiResponse<Void>> lockAccount(@PathVariable String username) {
        try {
            accountService.lockAccount(username);
            return ResponseEntity.ok(ApiResponse.success("Đã khóa tài khoản thành công!", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{username}/unlock")
    public ResponseEntity<ApiResponse<Void>> unlockAccount(@PathVariable String username) {
        try {
            accountService.unlockAccount(username);
            return ResponseEntity.ok(ApiResponse.success("Đã mở khóa tài khoản thành công!", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        }
    }

    // PUT /api/admin/accounts/{username}/reset-password
    // Body JSON: { "newPassword": "..." }
    @PutMapping("/{username}/reset-password")
    public ResponseEntity<ApiResponse<Void>> adminResetPassword(
            @PathVariable String username,
            @RequestBody Map<String, String> body) {
        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Mật khẩu mới không được để trống"));
        }
        try {
            accountService.adminResetPassword(username, newPassword);
            return ResponseEntity.ok(ApiResponse.success("Đã reset mật khẩu thành công!", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/check-username")
    public ResponseEntity<ApiResponse<Boolean>> checkUsername(@RequestParam String username) {
        boolean isAvailable = !accountService.existsByUsername(username);
        return ResponseEntity.ok(ApiResponse.success("Kiểm tra username", isAvailable));
    }

    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<Boolean>> checkEmail(
            @RequestParam String email,
            @RequestParam(required = false) String currentUsername) {

        if (currentUsername != null && !currentUsername.isEmpty()) {
            Account currentAccount = accountService.findById(currentUsername);
            if (currentAccount != null && currentAccount.getEmail().equals(email)) {
                return ResponseEntity.ok(ApiResponse.success("Kiểm tra email hợp lệ (gmail cũ)", true));
            }
        }
        boolean isAvailable = !accountService.existsByEmail(email);
        return ResponseEntity.ok(ApiResponse.success("Kiểm tra email trùng lặp", isAvailable));
    }
}
