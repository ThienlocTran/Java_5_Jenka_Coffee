package com.springboot.jenka_coffee.controller.site;

import com.springboot.jenka_coffee.entity.Account;
import com.springboot.jenka_coffee.entity.Order;
import com.springboot.jenka_coffee.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class OrderController {

    @Autowired
    private OrderService orderService;

    // Cấu hình số lượng đơn hàng mỗi lần tải (Dễ dàng sửa tại đây)
    private static final int PAGE_SIZE = 5;

    // 1. TRANG DANH SÁCH (Load lần đầu)
    @GetMapping("/order/list")
    public String list(Model model, HttpSession session) {
        // Kiểm tra đăng nhập
        Account user = (Account) session.getAttribute("user");
        if (user == null) {
            return "redirect:/auth/login?message=Vui lòng đăng nhập";
        }

        // Lấy trang đầu tiên (Trang 0)
        Pageable pageable = PageRequest.of(0, PAGE_SIZE, Sort.by("id").descending());
        Page<Order> page = orderService.findByUsername(user.getUsername(), pageable);

        // Gửi dữ liệu sang View
        model.addAttribute("orders", page);
        model.addAttribute("currentPage", 0);
        model.addAttribute("totalPages", page.getTotalPages());

        return "site/list";
    }

    // 2. API LAZY LOAD (Gọi ngầm khi bấm 'Xem thêm')
    @GetMapping("/order/load-more")
    public String loadMore(@RequestParam(defaultValue = "1") int page,
                           Model model,
                           HttpSession session) {
        // Kiểm tra đăng nhập (Bảo mật cho API)
        Account user = (Account) session.getAttribute("user");
        if (user == null) return ""; // Trả về rỗng nếu chưa đăng nhập

        // Lấy trang tiếp theo (Theo tham số page gửi lên)
        Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.by("id").descending());
        Page<Order> pageData = orderService.findByUsername(user.getUsername(), pageable);

        model.addAttribute("orders", pageData);

        // Chỉ trả về đoạn HTML các dòng <tr> (Fragment)
        return "site/list :: order_rows";
    }
}