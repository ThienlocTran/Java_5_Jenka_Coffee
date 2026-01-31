package com.springboot.jenka_coffee.controller.site;

import com.springboot.jenka_coffee.entity.Account;
import com.springboot.jenka_coffee.exception.ValidationException;
import com.springboot.jenka_coffee.service.AccountService;
import com.springboot.jenka_coffee.service.CookieService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private final AccountService accountService;

    private final CookieService cookieService;

    public AuthController(AccountService accountService, CookieService cookieService) {
        this.accountService = accountService;
        this.cookieService = cookieService;
    }

    /**
     * Hiển thị trang đăng nhập
     */
    @GetMapping("/login")
    public String showLoginPage(@RequestParam(required = false) String redirect,
            @RequestParam(required = false) String error,
            Model model) {
        model.addAttribute("redirect", redirect);
        if (error != null) {
            model.addAttribute("error", "Sai tên đăng nhập hoặc mật khẩu!");
        }
        return "site/auth/login";
    }

    /**
     * Xử lý đăng nhập
     */
    @PostMapping("/login")
    public String login(@RequestParam String username,
            @RequestParam String password,
            @RequestParam(required = false) boolean remember,
            @RequestParam(required = false) String redirect,
            HttpSession session,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes) {

        // Authenticate user
        Account account = accountService.authenticate(username, password);

        if (account == null) {
            redirectAttributes.addFlashAttribute("error", "Sai tên đăng nhập hoặc mật khẩu!");
            if (redirect != null && !redirect.isEmpty()) {
                return "redirect:/auth/login?redirect=" + redirect;
            }
            return "redirect:/auth/login";
        }

        // Save to session
        session.setAttribute("user", account);

        // Remember me cookie
        if (remember) {
            Cookie cookie = cookieService.createRememberMeCookie(username, 30);
            response.addCookie(cookie);
        }

        // Redirect based on role or requested page
        if (redirect != null && !redirect.isEmpty()) {
            return "redirect:" + redirect;
        }

        if (account.getAdmin() != null && account.getAdmin()) {
            return "redirect:/admin";
        }

        return "redirect:/home";
    }

    /**
     * Logout - Clear session and cookies
     */
    @GetMapping("/logout")
    public String logout(HttpSession session,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes) {

        // Clear session
        session.removeAttribute("user");
        session.invalidate();

        // Delete cookie
        cookieService.deleteRememberMeCookie(response);

        redirectAttributes.addFlashAttribute("success", "Đã đăng xuất thành công!");
        return "redirect:/home";
    }

    /**
     * Trang unauthorized (không có quyền truy cập)
     */
    @GetMapping("/unauthorized")
    public String unauthorized() {
        return "site/auth/unauthorized";
    }

    /**
     * Xử lý đăng ký tài khoản mới
     */
    @PostMapping("/signup")
    public String signup(@RequestParam String username,
            @RequestParam String fullname,
            @RequestParam String phone,
            @RequestParam(required = false) String email,
            @RequestParam String password,
            RedirectAttributes redirectAttributes) {

        try {
            // Delegate all business logic to service layer
            accountService.register(username, fullname, phone, email, password);

            // Success - redirect to login
            redirectAttributes.addFlashAttribute("success",
                    "Đăng ký thành công! Vui lòng đăng nhập.");
            return "redirect:/auth/login";

        } catch (ValidationException e) {
            // Validation error from service layer
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/auth/signup";
        } catch (Exception e) {
            // Unexpected error
            redirectAttributes.addFlashAttribute("error",
                    "Có lỗi xảy ra khi đăng ký. Vui lòng thử lại!");
            return "redirect:/auth/signup";
        }
    }

    // ===== ACCOUNT ACTIVATION & PASSWORD RESET =====

    /**
     * Activate account using token from email
     */
    @GetMapping("/activate/{token}")
    public String activateAccount(@PathVariable String token, RedirectAttributes ra) {
        try {
            accountService.activateAccount(token);
            ra.addFlashAttribute("success", "Tài khoản đã được kích hoạt thành công! Vui lòng đăng nhập.");
            return "redirect:/auth/login";
        } catch (ValidationException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/auth/login";
        }
    }

    /**
     * Show forgot password form
     */
    @GetMapping("/forgot-password")
    public String showForgotPassword() {
        return "site/auth/forgot-password";
    }

    /**
     * Process forgot password request
     */
    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam String identifier, RedirectAttributes ra) {
        try {
            String method = accountService.requestPasswordReset(identifier);

            if ("PHONE".equals(method)) {
                // If OTP sent, redirect to OTP verification page
                // Ideally, we should find the phone number to prepopulate, but identifier might
                // be username/email
                // For simplified flow, we forward the identifier as phone if it looks like one,
                // or ask user to re-enter
                ra.addFlashAttribute("success",
                        "OTP đã được gửi đến số điện thoại đăng ký. Vui lòng kiểm tra và nhập mã.");
                return "redirect:/auth/verify-otp";
            } else {
                ra.addFlashAttribute("success", "Link đặt lại mật khẩu đã được gửi đến email của bạn!");
                return "redirect:/auth/login";
            }
        } catch (ValidationException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/auth/forgot-password";
        }
    }

    /**
     * Show reset password form with token
     */
    @GetMapping("/reset-password/{token}")
    public String showResetPassword(@PathVariable String token, Model model) {
        model.addAttribute("token", token);
        return "site/auth/reset-password";
    }

    /**
     * Process password reset
     */
    @PostMapping("/reset-password")
    public String processResetPassword(
            @RequestParam String token,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            RedirectAttributes ra) {

        // Validate passwords match
        if (!newPassword.equals(confirmPassword)) {
            ra.addFlashAttribute("error", "Mật khẩu xác nhận không khớp!");
            return "redirect:/auth/reset-password/" + token;
        }

        try {
            accountService.resetPassword(token, newPassword);
            ra.addFlashAttribute("success", "Mật khẩu đã được đặt lại thành công! Vui lòng đăng nhập.");
            return "redirect:/auth/login";
        } catch (ValidationException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/auth/reset-password/" + token;
        }
    }

    /**
     * Show OTP verification page for phone activation
     */
    @GetMapping("/verify-otp")
    public String showVerifyOTP(@RequestParam(required = false) String phone, Model model) {
        model.addAttribute("phone", phone);
        return "site/auth/verify-otp";
    }

    /**
     * Verify OTP for phone activation
     */
    @PostMapping("/verify-otp")
    public String verifyOTP(
            @RequestParam String phone,
            @RequestParam String otp,
            RedirectAttributes ra) {

        try {
            accountService.verifyPhoneOTP(phone, otp);
            ra.addFlashAttribute("success", "Tài khoản đã được kích hoạt thành công! Vui lòng đăng nhập.");
            return "redirect:/auth/login";
        } catch (ValidationException e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addAttribute("phone", phone);
            return "redirect:/auth/verify-otp";
        }
    }
}
