package com.springboot.jenka_coffee.service.impl;

import com.springboot.jenka_coffee.service.CookieService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

@Service
public class CookieServiceImpl implements CookieService {

    private static final String COOKIE_NAME = "remember";

    @Override
    public Cookie createRememberMeCookie(String username, int days) {
        Cookie cookie = new Cookie(COOKIE_NAME, username);
        cookie.setMaxAge(days * 24 * 60 * 60); // Convert days to seconds
        cookie.setPath("/");
        cookie.setHttpOnly(true); // Security: prevent XSS attacks
        return cookie;
    }

    @Override
    public String getRememberMeUsername(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    @Override
    public void deleteRememberMeCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(COOKIE_NAME, "");
        cookie.setMaxAge(0); // Delete immediately
        cookie.setPath("/");
        response.addCookie(cookie);
    }
}
