package com.springboot.jenka_coffee.api.admin;

import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.dto.request.AccountRequest;
import com.springboot.jenka_coffee.entity.Account;
import com.springboot.jenka_coffee.service.AccountService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

// TODO: BUG-60 - Implement audit logging for admin actions
@RestController
@RequestMapping("/api/admin/accounts")
@Slf4j
public class ApiAdminAccountController {

    private final AccountService accountService;

    public ApiAdminAccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<Account>>> listAccounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        // Validation: Prevent negative page, zero/negative size, and DoS attacks
        if (page < 0) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Số trang không được âm"));
        }
        if (size <= 0) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Kích thước trang phải lớn hơn 0"));
        }
        if (size > 100) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Kích thước trang không được vượt quá 100"));
        }
        
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
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Tên đăng nhập không được để trống"));
        }
        Account entity = request.toEntity();
        // VULN-057 FIX: Chỉ super-admin (username="admin") mới được tạo admin account
        // Admin thường không thể tự tạo admin khác → ngăn horizontal privilege escalation
        // Nếu muốn cho phép tạo admin, cần thêm SUPER_ADMIN role riêng
        entity.setAdmin(false); // Force false — admin flag chỉ set qua DB hoặc super-admin endpoint
        Account account = accountService.createAccount(entity, photoFile);
        account.setPasswordHash(null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Thêm tài khoản mới thành công", account));
    }

    @PutMapping(value = "/{username}", consumes = { "multipart/form-data" })
    public ResponseEntity<ApiResponse<Account>> updateAccount(
            @PathVariable String username,
            @Valid @ModelAttribute AccountRequest request,
            @RequestParam(value = "photoFile", required = false) MultipartFile photoFile) {
        Account entity = request.toEntity();
        // VULN-H01 FIX: Không cho phép thay đổi admin flag qua update endpoint
        // Admin flag chỉ được set qua DB hoặc super-admin endpoint riêng
        entity.setAdmin(false);
        Account account = accountService.updateAccount(username, entity, photoFile);
        account.setPasswordHash(null);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật tài khoản thành công", account));
    }

    @DeleteMapping("/{username}")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            @PathVariable String username,
            Authentication authentication) {

        String currentAdmin = authentication != null ? authentication.getName() : null;
        
        // BUG-54 FIX: Prevent admin self-deletion (suicide protection)
        if (username.equals(currentAdmin)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Không thể xóa chính tài khoản của bạn!"));
        }
        
        // BUG-54 FIX: Prevent deletion of last admin (headless system protection)
        // deleteOrThrow already checks this, but explicit check here for clarity
        Account targetAccount = accountService.findById(username);
        if (targetAccount != null && Boolean.TRUE.equals(targetAccount.getAdmin())) {
            long adminCount = accountService.getAdministrators().size();
            if (adminCount <= 1) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Không thể xóa admin cuối cùng trong hệ thống!"));
            }
        }
        
        accountService.deleteOrThrow(username);
        return ResponseEntity.ok(ApiResponse.success("Xóa tài khoản thành công", null));
    }

    @PutMapping("/{username}/toggle-status")
    public ResponseEntity<ApiResponse<Account>> toggleAccountStatus(@PathVariable String username) {
        Account account = accountService.toggleActivation(username);
        account.setPasswordHash(null);
        String status = account.getActivated() ? "kích hoạt" : "vô hiệu hóa";
        return ResponseEntity.ok(ApiResponse.success("Đã " + status + " tài khoản thành công!", account));
    }

    /**
     * Set admin role for an account.
     * Body JSON: { "isAdmin": true/false }
     * Tách biệt với updateAccount để tránh horizontal privilege escalation.
     */
    @PutMapping("/{username}/set-role")
    public ResponseEntity<ApiResponse<Account>> setAdminRole(
            @PathVariable String username,
            @RequestBody Map<String, Object> body,
            Authentication authentication) {

        // Lấy value isAdmin từ body (hỗ trợ boolean và string)
        Object rawValue = body.get("isAdmin");
        if (rawValue == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Thiếu trường 'isAdmin' trong body"));
        }
        boolean isAdmin;
        if (rawValue instanceof Boolean b) {
            isAdmin = b;
        } else {
            isAdmin = Boolean.parseBoolean(rawValue.toString());
        }

        // Bảo vệ: admin không thể tự xóa quyền của mình
        String currentUser = authentication != null ? authentication.getName() : null;
        if (!isAdmin && username.equals(currentUser)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Không thể tự thu hồi quyền admin của chính mình!"));
        }

        Account account = accountService.setAdminRole(username, isAdmin);
        account.setPasswordHash(null);
        String action = isAdmin ? "cấp quyền admin cho" : "thu hồi quyền admin của";
        return ResponseEntity.ok(ApiResponse.success("Đã " + action + " tài khoản '" + username + "' thành công!", account));
    }

    @PutMapping("/set-role-by-email")
    public ResponseEntity<ApiResponse<Account>> setAdminRoleByEmail(
            @RequestBody Map<String, Object> body,
            Authentication authentication) {

        Object rawEmail = body.get("email");
        if (rawEmail == null || rawEmail.toString().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Thiếu trường 'email' trong body"));
        }

        Object rawValue = body.get("isAdmin");
        if (rawValue == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Thiếu trường 'isAdmin' trong body"));
        }

        String email = rawEmail.toString().trim();
        boolean isAdmin = rawValue instanceof Boolean b
                ? b
                : Boolean.parseBoolean(rawValue.toString());

        String currentUser = authentication != null ? authentication.getName() : null;
        Account currentAccount = currentUser != null ? accountService.findById(currentUser) : null;
        if (!isAdmin && currentAccount != null && email.equalsIgnoreCase(currentAccount.getEmail())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Không thể tự thu hồi quyền admin của chính mình!"));
        }

        Account account = accountService.setAdminRoleByEmail(email, isAdmin);
        account.setPasswordHash(null);
        String action = isAdmin ? "cấp quyền admin cho" : "thu hồi quyền admin của";
        return ResponseEntity.ok(ApiResponse.success("Đã " + action + " tài khoản có email '" + email + "' thành công!", account));
    }

    @PutMapping("/{username}/lock")
    public ResponseEntity<ApiResponse<Void>> lockAccount(@PathVariable String username) {
        Account account = accountService.findByIdOrThrow(username);
        account.setActivated(false);
        accountService.save(account);
        return ResponseEntity.ok(ApiResponse.success("Đã khóa tài khoản thành công!", null));
    }

    @PutMapping("/{username}/unlock")
    public ResponseEntity<ApiResponse<Void>> unlockAccount(@PathVariable String username) {
        Account account = accountService.findByIdOrThrow(username);
        account.setActivated(true);
        accountService.save(account);
        return ResponseEntity.ok(ApiResponse.success("Đã mở khóa tài khoản thành công!", null));
    }

    @PutMapping("/{username}/reset-password")
    public ResponseEntity<ApiResponse<Void>> adminResetPassword(
            @PathVariable String username,
            @RequestBody Map<String, String> body,
            Authentication authentication) {

        String currentAdmin = authentication != null ? authentication.getName() : null;
        
        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Mật khẩu mới không được để trống"));
        }
        
        // BUG-55 FIX: Prevent admin from resetting another admin's password (insider threat)
        // Only allow password reset for non-admin users
        // For admin password reset, require email OTP or super-admin privilege
        Account targetAccount = accountService.findById(username);
        if (targetAccount != null && Boolean.TRUE.equals(targetAccount.getAdmin())) {
            // Check if current admin is trying to reset another admin's password
            if (!username.equals(currentAdmin)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error(
                            "Không thể đổi mật khẩu của admin khác! " +
                            "Admin phải tự đổi mật khẩu qua tính năng 'Quên mật khẩu' với xác thực email."));
            }
        }
        
        accountService.adminResetPassword(username, newPassword);
        return ResponseEntity.ok(ApiResponse.success("Đã reset mật khẩu thành công!", null));
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
