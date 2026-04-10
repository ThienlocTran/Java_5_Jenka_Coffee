package com.springboot.jenka_coffee.api;

import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.dto.LoginRequest;
import com.springboot.jenka_coffee.dto.SignupRequest;
import com.springboot.jenka_coffee.dto.request.ForgotPasswordRequest;
import com.springboot.jenka_coffee.dto.request.ResetPasswordRequest;
import com.springboot.jenka_coffee.dto.request.VerifyOtpRequest;
import com.springboot.jenka_coffee.dto.response.AuthResult;
import com.springboot.jenka_coffee.entity.Account;
import com.springboot.jenka_coffee.security.JwtService;
import com.springboot.jenka_coffee.service.AccountService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class ApiAuthController {

    private final AccountService accountService;
    private final JwtService jwtService;

    // VULN-L02 FIX: Detect production vs local để set Secure flag đúng
    @org.springframework.beans.factory.annotation.Value("${spring.profiles.active:default}")
    private String activeProfile;

    public ApiAuthController(AccountService accountService, JwtService jwtService) {
        this.accountService = accountService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        AuthResult result = accountService.authenticateWithResult(request.getUsername(), request.getPassword());

        if (result.status() == AuthResult.Status.NOT_ACTIVATED) {
            // VULN-C03 FIX: Cùng message với INVALID_CREDENTIALS — không leak user existence
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Sai tên đăng nhập hoặc mật khẩu!"));
        }
        if (!result.isSuccess()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Sai tên đăng nhập hoặc mật khẩu!"));
        }

        Account account = result.account();
        String accessToken  = jwtService.generateAccessToken(account.getUsername(), account.getAdmin());
        String refreshToken = jwtService.generateRefreshToken(account.getUsername());

        // Gửi access token qua httpOnly cookie (an toàn hơn localStorage)
        addTokenCookie(response, "access_token",  accessToken,  86400);      // 1 ngày
        addTokenCookie(response, "refresh_token", refreshToken, 604800);     // 7 ngày

        Map<String, Object> data = buildUserData(account);
        // Trả token trong body để frontend dùng cho Authorization header fallback
        // (cần thiết cho dev cross-origin: localhost:5173 → localhost:8080)
        // Frontend lưu vào sessionStorage (tự xóa khi đóng tab), không phải localStorage
        data.put("accessToken", accessToken);

        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", data));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response) {
        clearTokenCookies(response);
        return ResponseEntity.ok(ApiResponse.success("Đã đăng xuất thành công!", null));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, Object>>> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {

        String refreshToken = extractCookie(request, "refresh_token");
        if (refreshToken == null || !jwtService.isValid(refreshToken) || !jwtService.isRefreshToken(refreshToken)) {
            clearTokenCookies(response);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại."));
        }

        String username = jwtService.extractUsername(refreshToken);
        Account account = accountService.findById(username);
        if (account == null || !account.getActivated()) {
            clearTokenCookies(response);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Tài khoản không hợp lệ."));
        }

        String newAccessToken = jwtService.generateAccessToken(account.getUsername(), account.getAdmin());
        addTokenCookie(response, "access_token", newAccessToken, 86400);

        Map<String, Object> data = buildUserData(account);
        data.put("accessToken", newAccessToken);
        return ResponseEntity.ok(ApiResponse.success("Làm mới token thành công", data));
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@Valid @RequestBody SignupRequest request) {
        accountService.register(
                request.getUsername(), request.getFullname(),
                request.getPhone(), request.getEmail(), request.getPassword());
        return ResponseEntity.ok(ApiResponse.success("Đăng ký thành công! Vui lòng đăng nhập.", null));
    }

    @PostMapping("/activate")
    public ResponseEntity<ApiResponse<Void>> activate(@RequestParam String token) {
        accountService.activateAccount(token);
        return ResponseEntity.ok(ApiResponse.success("Tài khoản đã được kích hoạt thành công!", null));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        // VULN-063 FIX: Không trả 'method' field — tránh account profiling
        // Luôn trả về cùng message dù user tồn tại hay không
        accountService.requestPasswordReset(request.getIdentifier());
        return ResponseEntity.ok(ApiResponse.success(
                "Nếu tài khoản tồn tại, chúng tôi đã gửi hướng dẫn khôi phục mật khẩu.", null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Mật khẩu xác nhận không khớp!"));
        }
        accountService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success("Mật khẩu đã được đặt lại thành công!", null));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<Void>> verifyOTP(@Valid @RequestBody VerifyOtpRequest request) {
        accountService.verifyPhoneOTP(request.getPhone(), request.getOtp());
        return ResponseEntity.ok(ApiResponse.success("Tài khoản đã được kích hoạt thành công!", null));
    }

    /** Lấy thông tin user hiện tại từ JWT (thay thế session /me) */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMe(
            @AuthenticationPrincipal String username) {
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Chưa đăng nhập"));
        }
        Account account = accountService.findById(username);
        if (account == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Tài khoản không tồn tại"));
        }
        return ResponseEntity.ok(ApiResponse.success("Đã đăng nhập", buildUserData(account)));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Map<String, Object> buildUserData(Account account) {
        Map<String, Object> data = new HashMap<>();
        data.put("username", account.getUsername());
        data.put("fullname", account.getFullname());
        data.put("email",    account.getEmail());
        data.put("isAdmin",  account.getAdmin());
        data.put("photo",    account.getPhoto());
        data.put("points",   account.getPoints());
        return data;
    }

    private void addTokenCookie(HttpServletResponse res, String name, String value, int maxAge) {
        // VULN-L02 FIX: Chỉ set Secure flag trên HTTPS (production)
        // Trên HTTP localhost, Secure flag khiến browser không gửi cookie
        boolean isProduction = !"default".equals(activeProfile) && !"local".equals(activeProfile);
        String secureFlag = isProduction ? "; Secure" : "";
        res.addHeader("Set-Cookie",
            name + "=" + value +
            "; Max-Age=" + maxAge +
            "; Path=/" +
            "; HttpOnly" +
            secureFlag +
            "; SameSite=Lax");
    }

    private void clearTokenCookies(HttpServletResponse res) {
        boolean isProduction = !"default".equals(activeProfile) && !"local".equals(activeProfile);
        String secureFlag = isProduction ? "; Secure" : "";
        for (String name : new String[]{"access_token", "refresh_token"}) {
            res.addHeader("Set-Cookie",
                name + "=; Max-Age=0; Path=/; HttpOnly" + secureFlag + "; SameSite=Lax");
        }
    }

    private String extractCookie(HttpServletRequest req, String name) {
        if (req.getCookies() == null) return null;
        return Arrays.stream(req.getCookies())
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst().orElse(null);
    }
}
