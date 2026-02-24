package com.springboot.jenka_coffee.controller.admin;

import com.springboot.jenka_coffee.entity.Order;
import com.springboot.jenka_coffee.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
@RequestMapping("/admin/order")
public class AdminOrderController {

    @Autowired
    OrderService orderService; // Gọi Service để lấy dữ liệu thật từ DB

    // 1. Hiển thị danh sách + Phân trang (Pagination)
    // Link chạy thử: /admin/order/index?p=0 (Trang 1), ?p=1 (Trang 2)...
    @GetMapping("/index")
    public String index(Model model, @RequestParam("p") Optional<Integer> p) {
        // Cấu hình: Mặc định trang 0, Lấy 10 đơn mỗi trang, Xếp đơn mới nhất lên đầu
        Pageable pageable = PageRequest.of(p.orElse(0), 10, Sort.by("createDate").descending());

        // Gọi Service lấy 1 trang đơn hàng
        Page<Order> page = orderService.findAll(pageable);

        model.addAttribute("page", page); // Gửi object Page sang View để hiện thanh phân trang 1,2,3...

        // Trả về đúng file giao diện bạn đang dùng
        return "admin/orders/order-index";
    }

    // 2. Chức năng Duyệt đơn / Hủy đơn
    // Link ví dụ: /admin/order/update/100/1 (Duyệt đơn số 100 thành trạng thái 1)
    @GetMapping("/update/{id}/{status}")
    public String updateStatus(@PathVariable("id") Long id, @PathVariable("status") int status) {
        orderService.updateStatus(id, status);
        return "redirect:/admin/order/index"; // Xử lý xong thì tải lại trang danh sách
    }

    // (Optional) Xóa đơn hàng (Thường ít dùng xóa hẳn, chỉ nên Hủy - status 4)
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable("id") Long id) {
        // orderService.delete(id); // Nếu bạn thực sự muốn xóa vĩnh viễn
        // Thay vì xóa, ta chuyển sang trạng thái Hủy (4)
        orderService.updateStatus(id, 4);
        return "redirect:/admin/order/index";
    }
}