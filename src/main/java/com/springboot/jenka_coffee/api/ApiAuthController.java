package com.springboot.jenka_coffee.api;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.dto.LoginRequest;
import com.springboot.jenka_coffee.dto.SignupRequest;
import com.springboot.jenka_coffee.dto.request.ForgotPasswordRequest;
import com.springboot.jenka_coffee.dto.request.GoogleLoginRequest;
import com.springboot.jenka_coffee.dto.request.ResetPasswordRequest;
import com.springboot.jenka_coffee.dto.request.VerifyOtpRequest;
import com.springboot.jenka_coffee.dto.response.AuthResult;
import com.springboot.jenka_coffee.dto.response.AuthStatus;
import com.springboot.jenka_coffee.entity.Account;
import com.springboot.jenka_coffee.security.JwtService;
import com.springboot.jenka_coffee.service.AccountService;
import com.springboot.jenka_coffee.service.CookieService;
import com.springboot.jenka_coffee.service.GoogleOAuthService;
import com.springboot.jenka_coffee.service.JwtBlacklistService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static java.lang.System.currentTimeMillis;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class ApiAuthController {

    private final AccountService accountService;
    private final JwtService jwtService;
    private final GoogleOAuthService googleOAuthService;
    private final JwtBlacklistService jwtBlacklistService;
    private final com.springboot.jenka_coffee.service.CookieService cookieService;

    // VULN-L02 FIX: Detect production vs local để set Secure flag đúng
    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    public ApiAuthController(AccountService accountService, JwtService jwtService, 
                           GoogleOAuthService googleOAuthService,
                           JwtBlacklistService jwtBlacklistService,
                           CookieService cookieService) {
        this.accountService = accountService;
        this.jwtService = jwtService;
        this.googleOAuthService = googleOAuthService;
        this.jwtBlacklistService = jwtBlacklistService;
        this.cookieService = cookieService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        AuthResult result = accountService.authenticateWithResult(request.getUsername(), request.getPassword());

        if (result.status() == AuthStatus.NOT_ACTIVATED) {
            // UX FIX: Return clear message so user knows what to do
            // Security note: Many production systems (Google, Facebook) return clear activation messages
            // Real security is not leaking passwords, not hiding activation status
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Tài khoản chưa được kích hoạt. Vui lòng kiểm tra email."));
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

        // REMEMBER ME: Nếu user chọn "Ghi nhớ đăng nhập", tạo remember cookie
        if (request.isRemember()) {
            Cookie rememberCookie = cookieService.createRememberMeCookie(account.getUsername(), 30); // 30 ngày
            response.addCookie(rememberCookie);
        }

        Map<String, Object> data = buildUserData(account);
        // DEV FALLBACK: Trả accessToken trong body để frontend lưu vào sessionStorage
        // Cần thiết cho dev cross-origin (localhost:5173 → localhost:8080) vì HttpOnly cookie
        // không gửi được qua cross-origin requests (SameSite=Lax).
        // Trên production (same-origin qua reverse proxy), cookie tự động gửi → không cần body token.
        // sessionStorage an toàn hơn localStorage (xóa khi đóng tab) và tốt hơn không có gì.
        data.put("accessToken", accessToken);

        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", data));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request, HttpServletResponse response) {
        // VULN-MOBILE-LOGOUT-BYPASS FIX: Extract token from both cookies AND headers
        // Mobile apps use Authorization header, web browsers use cookies
        String accessToken = extractCookie(request, "access_token");
        String refreshToken = extractCookie(request, "refresh_token");
        
        // If not in cookies, check Authorization header (for mobile apps)
        if (accessToken == null) {
            accessToken = extractBearerToken(request);
        }
        
        // Blacklist tokens - JwtBlacklistService tự động expire sau 7 ngày
        if (accessToken != null && jwtService.isValid(accessToken)) {
            // Tính expiration time: current time + 1 ngày (access token TTL)
            long expirationMs = currentTimeMillis() + 86400000L; // 1 day
            jwtBlacklistService.blacklistToken(accessToken, expirationMs);
        }
        
        if (refreshToken != null && jwtService.isValid(refreshToken)) {
            // Tính expiration time: current time + 7 ngày (refresh token TTL)
            long expirationMs = currentTimeMillis() + 604800000L; // 7 days
            jwtBlacklistService.blacklistToken(refreshToken, expirationMs);
        }
        
        // REMEMBER ME: Xóa remember cookie khi logout
        cookieService.deleteRememberMeCookie(response);
        
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

        // VULN-JWT-BLACKLIST-BYPASS FIX: Check if refresh token is blacklisted
        if (jwtBlacklistService.isBlacklisted(refreshToken)) {
            clearTokenCookies(response);
            log.warn("SECURITY: Attempted to use blacklisted refresh token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Token đã bị vô hiệu hóa. Vui lòng đăng nhập lại."));
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
        // DEV FALLBACK: Trả accessToken trong body (xem lý giải tại /login endpoint)
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

    /**
     * Google OAuth Login
     * Verify Google ID token, create/update account, return JWT
     */
    @PostMapping("/google-login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> googleLogin(
            @Valid @RequestBody GoogleLoginRequest request,
            HttpServletResponse response) {
        
        // Verify Google ID token
        GoogleIdToken.Payload payload = googleOAuthService.verifyIdToken(request.getIdToken());
        
        if (payload == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Google token không hợp lệ"));
        }
        
        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String picture = (String) payload.get("picture");
        Boolean emailVerified = payload.getEmailVerified();

        if (!Boolean.TRUE.equals(emailVerified)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Email Google chua duoc xac thuc"));
        }
        
        // Google login may link to an activated password account when the verified
        // Google email matches. Unactivated accounts stay blocked below.
        Account account = accountService.findByEmail(email);
        boolean needsPhone = false;
        
        if (account == null) {
            // Create new account from Google - generate unique username from email
            // Clean email prefix: remove dots and special characters, keep only alphanumeric
            String emailPrefix = email.split("@")[0]
                    .replaceAll("[^a-zA-Z0-9]", "")  // Remove dots, dashes, etc.
                    .toLowerCase();
            if (emailPrefix.length() > 40) {
                emailPrefix = emailPrefix.substring(0, 40);
            }
            
            // Use only last 6 digits of timestamp for shorter username
            String timestamp = String.valueOf(currentTimeMillis());
            String shortTimestamp = timestamp.substring(timestamp.length() - 6);
            
            String username = emailPrefix + "_" + shortTimestamp;
            
            account = new Account();
            account.setUsername(username);
            account.setEmail(email);
            account.setFullname(name);
            account.setPhoto(picture);
            account.setPasswordHash("GOOGLE_OAUTH_NO_PASSWORD"); // OAuth users don't have password
            account.setActivated(true);
            account.setAdmin(false);
            account.setPoints(0);
            account.setCustomerRank("MEMBER");
            account.setPhone(null); // NULL phone, will be updated later
            
            // JPA FIX: Mark as new entity to force persist() instead of merge()
            account.setNew(true);
            
            accountService.save(account);
            needsPhone = true; // New user needs to provide phone
        } else {
            // Account exists - check if it's safe to allow OAuth login
            
            // VULN-OAUTH-ACCOUNT-TAKEOVER FIX: Only block OAuth if account was created with password AND never used OAuth
            // Allow OAuth login if:
            // 1. Account was created via OAuth (passwordHash = GOOGLE_OAUTH_NO_PASSWORD)
            // 2. Account username matches OAuth pattern (email_timestamp)
            
            String username = account.getUsername();
            String passwordHash = account.getPasswordHash();
            
            // Check if this is an OAuth account
            // OAuth accounts have either:
            // - passwordHash = "GOOGLE_OAUTH_NO_PASSWORD" (original OAuth account)
            // - username ending with _<13-digit-timestamp> (OAuth username pattern)
            boolean isOAuthAccount = "GOOGLE_OAUTH_NO_PASSWORD".equals(passwordHash);
            
            if (!isOAuthAccount && username != null) {
                // Check username pattern: ends with underscore followed by 6-13 digits
                try {
                    int lastUnderscore = username.lastIndexOf('_');
                    if (lastUnderscore > 0 && lastUnderscore < username.length() - 1) {
                        String suffix = username.substring(lastUnderscore + 1);
                        if (suffix.matches("\\d{6,13}")) {
                            isOAuthAccount = true;
                        }
                    }
                } catch (Exception e) {
                    log.debug("Error checking OAuth username pattern: {}", e.getMessage());
                }
            }
            
            if (!isOAuthAccount && Boolean.TRUE.equals(account.getActivated())) {
                // Verified Google email matches an active account email, so allow Google
                // as an additional login method without changing password/admin role.
                isOAuthAccount = true;
                log.info("Linked verified Google login for existing account: {}", account.getUsername());
            }

            if (!isOAuthAccount) {
                // Account exists with password and NOT created via OAuth
                if (!Boolean.TRUE.equals(account.getActivated())) {
                    // SECURITY: Do NOT allow OAuth to take over unactivated password accounts
                    log.warn("SECURITY: Blocked OAuth takeover attempt for unactivated password account: {}", email);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(ApiResponse.error("Email này đã được đăng ký. Vui lòng kích hoạt tài khoản hoặc đăng nhập bằng mật khẩu."));
                } else {
                    // Activated account with password - deny Google OAuth
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(ApiResponse.error("Email này đã được đăng ký bằng mật khẩu. Vui lòng đăng nhập bằng mật khẩu."));
                }
            }
            
            // OAuth account - allow login even if password was set later
            // This allows users to have both OAuth and password login methods
            boolean changed = false;
            if ((account.getPhoto() == null || account.getPhoto().isBlank())
                    && picture != null && !picture.isBlank()) {
                account.setPhoto(picture);
                changed = true;
            }
            if ((account.getFullname() == null || account.getFullname().isBlank())
                    && name != null && !name.isBlank()) {
                account.setFullname(name);
                changed = true;
            }
            if (changed) {
                accountService.save(account);
            }
            
            if (account.getPhone() == null || account.getPhone().trim().isEmpty()) {
                needsPhone = true; // Existing user without phone
            }
        }
        
        // Generate JWT tokens
        String accessToken = jwtService.generateAccessToken(account.getUsername(), account.getAdmin());
        String refreshToken = jwtService.generateRefreshToken(account.getUsername());
        
        // Set cookies
        addTokenCookie(response, "access_token", accessToken, 86400);
        addTokenCookie(response, "refresh_token", refreshToken, 604800);
        
        Map<String, Object> data = buildUserData(account);
        // DEV FALLBACK: Trả accessToken trong body (xem lý giải tại /login endpoint)
        data.put("accessToken", accessToken);
        data.put("needsPhone", needsPhone);
        
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", data));
    }

    /**
     * Update phone number for authenticated user
     */
    @PatchMapping("/update-phone")
    public ResponseEntity<ApiResponse<Void>> updatePhone(
            org.springframework.security.core.Authentication authentication,
            @RequestBody Map<String, String> request) {
        
        String username = authentication != null ? authentication.getName() : null;
        log.info("UPDATE_PHONE: Received request from username: {}", username);
        
        if (username == null) {
            log.error("UPDATE_PHONE: Username is null - authentication failed");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Chưa đăng nhập"));
        }
        
        String phone = request.get("phone");
        if (phone == null || phone.trim().isEmpty()) {
            log.warn("UPDATE_PHONE: Invalid phone number for user: {}", username);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Số điện thoại không hợp lệ"));
        }
        
        log.info("UPDATE_PHONE: Updating phone for user {} to {}", username, phone);
        accountService.updatePhone(username, phone);
        log.info("UPDATE_PHONE: Successfully updated phone for user {}", username);
        
        return ResponseEntity.ok(ApiResponse.success("Cập nhật số điện thoại thành công", null));
    }

    /**
     * VULN-ZOMBIE-OTP FIX: Add resend OTP endpoint
     * Allows users to request new OTP if they didn't receive the first one
     */
    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<Void>> resendOTP(@RequestBody Map<String, String> request) {
        String phone = request.get("phone");
        if (phone == null || phone.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Số điện thoại không hợp lệ"));
        }
        
        try {
            accountService.resendActivation(phone);
            return ResponseEntity.ok(ApiResponse.success(
                    "Đã gửi lại mã OTP. Vui lòng kiểm tra tin nhắn.", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /** Lấy thông tin user hiện tại từ JWT (thay thế session /me) */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMe(
            org.springframework.security.core.Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Chưa đăng nhập"));
        }
        Account account = accountService.findById(username);
        if (account == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Tài khoản không tồn tại"));
        }
        return ResponseEntity.ok(ApiResponse.success("Đã đăng nhập", buildUserData(account)));
    }

    /**
     * REMEMBER ME: Check if remember cookie exists and return username for auto-fill
     * Frontend sẽ gọi endpoint này khi load trang login để auto-fill username
     */
    @GetMapping("/check-remember")
    public ResponseEntity<ApiResponse<Map<String, String>>> checkRememberMe(HttpServletRequest request) {
        String username = cookieService.getRememberMeUsername(request);
        
        if (username != null) {
            // Verify account still exists and is active
            Account account = accountService.findById(username);
            if (account != null && account.getActivated()) {
                Map<String, String> data = new HashMap<>();
                data.put("username", username);
                return ResponseEntity.ok(ApiResponse.success("Remember cookie found", data));
            }
        }
        
        return ResponseEntity.ok(ApiResponse.success("No remember cookie", null));
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
    
    /**
     * VULN-MOBILE-LOGOUT-BYPASS FIX: Extract JWT from Authorization header
     * Mobile apps send tokens via "Authorization: Bearer <token>" header
     */
    private String extractBearerToken(HttpServletRequest req) {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
