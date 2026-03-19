package com.springboot.jenka_coffee.api;

import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.dto.LoginRequest;
import com.springboot.jenka_coffee.dto.SignupRequest;
import com.springboot.jenka_coffee.dto.request.ForgotPasswordRequest;
import com.springboot.jenka_coffee.dto.request.ResetPasswordRequest;
import com.springboot.jenka_coffee.dto.request.VerifyOtpRequest;
import com.springboot.jenka_coffee.entity.Account;
import com.springboot.jenka_coffee.exception.ValidationException;
import com.springboot.jenka_coffee.service.AccountService;
import com.springboot.jenka_coffee.service.CookieService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class ApiAuthController {

    private final AccountService accountService;
    private final CookieService cookieService;

    public ApiAuthController(AccountService accountService, CookieService cookieService) {
        this.accountService = accountService;
        this.cookieService = cookieService;
    }

    // ============================================================
    // POST /api/auth/login
    // Body: { "username": "...", "password": "...", "remember": true/false }
    // ============================================================
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(
            @Valid @RequestBody LoginRequest request,
            HttpSession session,
            HttpServletResponse response) {

        Account account = accountService.authenticate(request.getUsername(), request.getPassword());

        if (account == null) {
            // Phân biệt: tài khoản tồn tại nhưng chưa kích hoạt vs sai thông tin
            Account existing = accountService.findById(request.getUsername());
            if (existing != null && !existing.getActivated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Tài khoản chưa được kích hoạt. Vui lòng kiểm tra email/SMS để kích hoạt."));
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Sai tên đăng nhập hoặc mật khẩu!"));
        }

        session.setAttribute("user", account);

        if (request.isRemember()) {
            Cookie cookie = cookieService.createRememberMeCookie(request.getUsername(), 30);
            cookie.setPath("/");
            response.addCookie(cookie);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("username", account.getUsername());
        data.put("fullname", account.getFullname());
        data.put("email", account.getEmail());
        data.put("isAdmin", account.getAdmin());

        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", data));
    }

    // ============================================================
    // POST /api/auth/logout
    // ============================================================
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpSession session, HttpServletResponse response) {
        session.removeAttribute("user");
        session.invalidate();
        cookieService.deleteRememberMeCookie(response);

        Cookie cookie = new Cookie("user-jwt", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);

        return ResponseEntity.ok(ApiResponse.success("Đã đăng xuất thành công!", null));
    }

    // ============================================================
    // POST /api/auth/signup
    // Body: { "username", "fullname", "phone", "email"(optional), "password" }
    // ============================================================
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@Valid @RequestBody SignupRequest request) {
        try {
            accountService.register(
                    request.getUsername(),
                    request.getFullname(),
                    request.getPhone(),
                    request.getEmail(),
                    request.getPassword()
            );
            return ResponseEntity.ok(ApiResponse.success("Đăng ký thành công! Vui lòng đăng nhập.", null));
        } catch (ValidationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        }
    }

    // ============================================================
    // POST /api/auth/activate?token=...
    // (Query param – token đến từ link email, giữ @RequestParam)
    // ============================================================
    @PostMapping("/activate")
    public ResponseEntity<ApiResponse<Void>> activateAccountFrontend(@RequestParam String token) {
        try {
            accountService.activateAccount(token);
            return ResponseEntity.ok(ApiResponse.success("Tài khoản đã được kích hoạt thành công!", null));
        } catch (ValidationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        }
    }

    // ============================================================
    // POST /api/auth/forgot-password
    // Body: { "identifier": "email_or_phone" }
    // ============================================================
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Map<String, String>>> processForgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        try {
            String method = accountService.requestPasswordReset(request.getIdentifier());
            Map<String, String> data = new HashMap<>();
            data.put("method", method);

            if ("PHONE".equals(method)) {
                return ResponseEntity.ok(ApiResponse.success(
                        "OTP đã được gửi đến số điện thoại đăng ký. Vui lòng kiểm tra và nhập mã.", data));
            } else {
                return ResponseEntity.ok(ApiResponse.success(
                        "Link đặt lại mật khẩu đã được gửi đến email của bạn!", data));
            }
        } catch (ValidationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        }
    }

    // ============================================================
    // POST /api/auth/reset-password
    // Body: { "token": "...", "newPassword": "...", "confirmPassword": "..." }
    // ============================================================
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> processResetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Mật khẩu xác nhận không khớp!"));
        }

        try {
            accountService.resetPassword(request.getToken(), request.getNewPassword());
            return ResponseEntity.ok(
                    ApiResponse.success("Mật khẩu đã được đặt lại thành công! Vui lòng đăng nhập.", null));
        } catch (ValidationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        }
    }

    // ============================================================
    // POST /api/auth/verify-otp
    // Body: { "phone": "...", "otp": "..." }
    // ============================================================
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<Void>> verifyOTP(@Valid @RequestBody VerifyOtpRequest request) {
        try {
            accountService.verifyPhoneOTP(request.getPhone(), request.getOtp());
            return ResponseEntity.ok(ApiResponse.success(
                    "Tài khoản đã được kích hoạt thành công! Vui lòng đăng nhập.", null));
        } catch (ValidationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        }
    }

    // ============================================================
    // GET /api/auth/me  – Lấy thông tin session hiện tại
    // ============================================================
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMe(HttpSession session) {
        Account account = (Account) session.getAttribute("user");
        if (account == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Chưa đăng nhập"));
        }

        Map<String, Object> data = new HashMap<>();
        data.put("username", account.getUsername());
        data.put("fullname", account.getFullname());
        data.put("email", account.getEmail());
        data.put("isAdmin", account.getAdmin());

        return ResponseEntity.ok(ApiResponse.success("Đã đăng nhập", data));
    }
}
