package com.springboot.jenka_coffee.interceptor;

import com.springboot.jenka_coffee.service.CartService;
import com.springboot.jenka_coffee.service.CategoryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component; // Bắt buộc phải có
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
public class GlobalInterceptor implements HandlerInterceptor {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CartService cartService;

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        // Chỉ thực hiện khi có dữ liệu trả về View (HTML)
        if (modelAndView != null && !modelAndView.isEmpty()) {

            // 1. Đổ danh mục cho Menu
            modelAndView.addObject("categories", categoryService.findAll());

            // 2. Đổ số lượng món trong giỏ
            modelAndView.addObject("cartCount", cartService.getCount());

            // 3. Đổ danh sách món trong giỏ (Cho Mini-Cart popup)
            modelAndView.addObject("cartItems", cartService.getItems());

            // 4. Đổ tổng tiền giỏ hàng
            modelAndView.addObject("cartTotal", cartService.getAmount());
        }
    }
}