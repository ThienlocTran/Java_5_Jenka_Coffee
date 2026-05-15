package com.springboot.jenka_coffee.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface CookieService {
    Cookie createRememberMeCookie(String username, int days);

    String getRememberMeUsername(HttpServletRequest request);

    void deleteRememberMeCookie(HttpServletResponse response);
}
