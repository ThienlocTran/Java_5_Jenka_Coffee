package com.springboot.jenka_coffee.interceptor;

import com.springboot.jenka_coffee.entity.Account;
import com.springboot.jenka_coffee.service.AccountService;
import com.springboot.jenka_coffee.service.CookieService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private AccountService accountService;

    @Autowired
    private CookieService cookieService;

    @Override
    public boolean preHandle(HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {

        String uri = request.getRequestURI();
        HttpSession session = request.getSession();
        Account user = (Account) session.getAttribute("user");

        // Auto-login from cookie if not logged in
        if (user == null) {
            String username = cookieService.getRememberMeUsername(request);
            if (username != null) {
                user = accountService.findById(username);
                if (user != null && user.getActivated()) {
                    session.setAttribute("user", user);
                }
            }
        }

        // Check admin routes
        if (uri.startsWith("/admin")) {
            if (user == null) {
                response.sendRedirect("/auth/login?redirect=" + uri);
                return false;
            }
            if (user.getAdmin() == null || !user.getAdmin()) {
                response.sendRedirect("/auth/unauthorized");
                return false;
            }
        }

        // Check user-only routes (e.g., /order, /profile)
        if (uri.startsWith("/order") || uri.startsWith("/profile")) {
            if (user == null) {
                response.sendRedirect("/auth/login?redirect=" + uri);
                return false;
            }
        }

        return true;
    }
}
