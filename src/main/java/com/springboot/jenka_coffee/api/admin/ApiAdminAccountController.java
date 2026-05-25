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
        if (page < 0) {
            return ResponseEntity.badRequest().body(ApiResponse.error("So trang khong duoc am"));
        }
        if (size <= 0) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Kich thuoc trang phai lon hon 0"));
        }
        if (size > 100) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Kich thuoc trang khong duoc vuot qua 100"));
        }

        Page<Account> result = accountService.findAllPaginated(PageRequest.of(page, size));
        result.forEach(a -> a.setPasswordHash(null));
        return ResponseEntity.ok(ApiResponse.success("Lay danh sach nguoi dung thanh cong", result));
    }

    @GetMapping("/{username}")
    public ResponseEntity<ApiResponse<Account>> getAccount(@PathVariable String username) {
        Account account = accountService.findByIdOrThrow(username);
        account.setPasswordHash(null);
        return ResponseEntity.ok(ApiResponse.success("Lay thong tin tai khoan thanh cong", account));
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<Account>> createAccount(
            @Valid @ModelAttribute AccountRequest request,
            @RequestParam(value = "photoFile", required = false) MultipartFile photoFile) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Ten dang nhap khong duoc de trong"));
        }

        Account entity = request.toEntity();
        entity.setAdmin(false);

        Account account = accountService.createAccount(entity, photoFile);
        account.setPasswordHash(null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Them tai khoan moi thanh cong", account));
    }

    @PutMapping(value = "/{username}", consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<Account>> updateAccount(
            @PathVariable String username,
            @Valid @ModelAttribute AccountRequest request,
            @RequestParam(value = "photoFile", required = false) MultipartFile photoFile) {
        Account entity = request.toEntity();
        entity.setAdmin(false);

        Account account = accountService.updateAccount(username, entity, photoFile);
        account.setPasswordHash(null);
        return ResponseEntity.ok(ApiResponse.success("Cap nhat tai khoan thanh cong", account));
    }

    @DeleteMapping("/{username}")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            @PathVariable String username,
            Authentication authentication) {
        String currentAdmin = authentication != null ? authentication.getName() : null;

        if (username.equals(currentAdmin)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Khong the xoa chinh tai khoan cua ban!"));
        }

        Account targetAccount = accountService.findById(username);
        if (targetAccount != null && Boolean.TRUE.equals(targetAccount.getAdmin())) {
            long adminCount = accountService.getAdministrators().size();
            if (adminCount <= 1) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Khong the xoa admin cuoi cung trong he thong!"));
            }
        }

        accountService.deleteOrThrow(username);
        return ResponseEntity.ok(ApiResponse.success("Xoa tai khoan thanh cong", null));
    }

    @PutMapping("/{username}/toggle-status")
    public ResponseEntity<ApiResponse<Account>> toggleAccountStatus(@PathVariable String username) {
        Account account = accountService.toggleActivation(username);
        account.setPasswordHash(null);
        String status = account.getActivated() ? "kich hoat" : "vo hieu hoa";
        return ResponseEntity.ok(ApiResponse.success("Da " + status + " tai khoan thanh cong!", account));
    }

    @PutMapping("/{username}/set-role")
    public ResponseEntity<ApiResponse<Account>> setAdminRole(
            @PathVariable String username,
            @RequestBody Map<String, Object> body,
            Authentication authentication) {
        Object rawValue = body.get("isAdmin");
        if (rawValue == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Thieu truong 'isAdmin' trong body"));
        }

        boolean isAdmin = rawValue instanceof Boolean b
                ? b
                : Boolean.parseBoolean(rawValue.toString());

        String currentUser = authentication != null ? authentication.getName() : null;
        Account account = accountService.setAdminRole(username, isAdmin, currentUser);
        account.setPasswordHash(null);

        String action = isAdmin ? "cap quyen admin cho" : "thu hoi quyen admin cua";
        return ResponseEntity.ok(
                ApiResponse.success("Da " + action + " tai khoan '" + username + "' thanh cong!", account));
    }

    @PutMapping("/set-role-by-email")
    public ResponseEntity<ApiResponse<Account>> setAdminRoleByEmail(
            @RequestBody Map<String, Object> body,
            Authentication authentication) {
        Object rawEmail = body.get("email");
        if (rawEmail == null || rawEmail.toString().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Thieu truong 'email' trong body"));
        }

        Object rawValue = body.get("isAdmin");
        if (rawValue == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Thieu truong 'isAdmin' trong body"));
        }

        String email = rawEmail.toString().trim();
        boolean isAdmin = rawValue instanceof Boolean b
                ? b
                : Boolean.parseBoolean(rawValue.toString());

        String currentUser = authentication != null ? authentication.getName() : null;
        Account currentAccount = currentUser != null ? accountService.findById(currentUser) : null;
        if (!isAdmin && currentAccount != null && email.equalsIgnoreCase(currentAccount.getEmail())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Khong the tu thu hoi quyen admin cua chinh minh!"));
        }

        Account account = accountService.setAdminRoleByEmail(email, isAdmin);
        account.setPasswordHash(null);
        String action = isAdmin ? "cap quyen admin cho" : "thu hoi quyen admin cua";
        return ResponseEntity.ok(
                ApiResponse.success("Da " + action + " tai khoan co email '" + email + "' thanh cong!", account));
    }

    @PutMapping("/{username}/lock")
    public ResponseEntity<ApiResponse<Void>> lockAccount(@PathVariable String username) {
        Account account = accountService.findByIdOrThrow(username);
        account.setActivated(false);
        accountService.save(account);
        return ResponseEntity.ok(ApiResponse.success("Da khoa tai khoan thanh cong!", null));
    }

    @PutMapping("/{username}/unlock")
    public ResponseEntity<ApiResponse<Void>> unlockAccount(@PathVariable String username) {
        Account account = accountService.findByIdOrThrow(username);
        account.setActivated(true);
        accountService.save(account);
        return ResponseEntity.ok(ApiResponse.success("Da mo khoa tai khoan thanh cong!", null));
    }

    @PutMapping("/{username}/reset-password")
    public ResponseEntity<ApiResponse<Void>> adminResetPassword(
            @PathVariable String username,
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        String currentAdmin = authentication != null ? authentication.getName() : null;
        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Mat khau moi khong duoc de trong"));
        }

        Account targetAccount = accountService.findById(username);
        if (targetAccount != null && Boolean.TRUE.equals(targetAccount.getAdmin()) && !username.equals(currentAdmin)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(
                            "Khong the doi mat khau cua admin khac! Admin phai tu doi mat khau qua quen mat khau."));
        }

        accountService.adminResetPassword(username, newPassword);
        return ResponseEntity.ok(ApiResponse.success("Da reset mat khau thanh cong!", null));
    }

    @GetMapping("/check-username")
    public ResponseEntity<ApiResponse<Boolean>> checkUsername(@RequestParam String username) {
        boolean isAvailable = !accountService.existsByUsername(username);
        return ResponseEntity.ok(ApiResponse.success("Kiem tra username", isAvailable));
    }

    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<Boolean>> checkEmail(
            @RequestParam String email,
            @RequestParam(required = false) String currentUsername) {
        if (currentUsername != null && !currentUsername.isEmpty()) {
            Account currentAccount = accountService.findById(currentUsername);
            if (currentAccount != null && currentAccount.getEmail().equals(email)) {
                return ResponseEntity.ok(ApiResponse.success("Kiem tra email hop le", true));
            }
        }

        boolean isAvailable = !accountService.existsByEmail(email);
        return ResponseEntity.ok(ApiResponse.success("Kiem tra email trung lap", isAvailable));
    }
}
