package com.springboot.jenka_coffee.service;

import com.springboot.jenka_coffee.dto.request.CheckoutRequest;
import com.springboot.jenka_coffee.entity.Order;

public interface CheckoutService {
    /**
     * Xử lý đặt hàng từ giỏ hàng
     * @param request Thông tin checkout
     * @return Order đã tạo
     * @throws IllegalStateException nếu giỏ hàng trống
     */
    Order processCheckout(CheckoutRequest request);
    
    /**
     * Kiểm tra giỏ hàng có trống không
     * @return true nếu giỏ hàng trống
     */
    boolean isCartEmpty();
}
