package com.springboot.jenka_coffee.controller;

import com.springboot.jenka_coffee.entity.Order; //
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin/order")
public class AdminOrderController {

    // Đường dẫn: /admin/order/index
    @GetMapping("/index")
    public String index(Model model) {
        // 1. Tạo list rỗng từ Entity thật (để tránh lỗi null bên View)
        List<Order> list = new ArrayList<>();

        // (Sau này sẽ thay bằng: orderDAO.findAll())

        model.addAttribute("orders", list);

        // 2. Trả về đúng tên file trong file Excel yêu cầu: order-index.html
        return "admin/orders/order-index";
    }

    // Xử lý khi bấm nút "Đồng ý" trên Modal
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable("id") Long id) {
        System.out.println(">>> Admin yêu cầu xóa đơn số: " + id);
        // Code xóa thật sẽ viết ở đây sau

        // Xóa xong quay lại trang danh sách
        return "redirect:/admin/order/index";
    }
}