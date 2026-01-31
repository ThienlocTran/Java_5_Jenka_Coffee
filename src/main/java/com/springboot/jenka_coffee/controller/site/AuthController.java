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
            return "redirect:/admin/product/list";
        }

        return "redirect:/home";
    }

    /**
     * Xử lý đăng xuất
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
}
