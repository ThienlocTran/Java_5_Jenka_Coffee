package com.springboot.jenka_coffee.controller;

import com.springboot.jenka_coffee.dto.request.CheckoutRequest;
import com.springboot.jenka_coffee.dto.response.CartItem;
import com.springboot.jenka_coffee.entity.Account;
import com.springboot.jenka_coffee.entity.Order;
import com.springboot.jenka_coffee.entity.OrderDetail;
import com.springboot.jenka_coffee.entity.Product;
import com.springboot.jenka_coffee.service.CartService;
import com.springboot.jenka_coffee.service.OrderService;
import com.springboot.jenka_coffee.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Controller
@RequestMapping("/checkout")
public class CheckoutController {

    private final CartService cartService;
    private final OrderService orderService;
    private final ProductService productService;

    public CheckoutController(CartService cartService, OrderService orderService, ProductService productService) {
        this.cartService = cartService;
        this.orderService = orderService;
        this.productService = productService;
    }

    @GetMapping
    public String showCheckoutForm(Model model) {
        // Kiểm tra giỏ hàng trống
        Collection<CartItem> items = cartService.getItems();
        if (items.isEmpty()) {
            return "redirect:/cart/view";
        }

        // Tạo form object mới
        if (!model.containsAttribute("checkoutRequest")) {
            model.addAttribute("checkoutRequest", new CheckoutRequest());
        }

        // Đưa dữ liệu giỏ hàng vào model
        model.addAttribute("cartItems", items);
        model.addAttribute("cartTotal", cartService.getAmount());
        model.addAttribute("cartCount", cartService.getCount());

        return "site/checkout";
    }

    @PostMapping
    public String processCheckout(@Valid @ModelAttribute("checkoutRequest") CheckoutRequest request,
                                   BindingResult bindingResult,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {

        // Kiểm tra validation errors
        if (bindingResult.hasErrors()) {
            model.addAttribute("cartItems", cartService.getItems());
            model.addAttribute("cartTotal", cartService.getAmount());
            model.addAttribute("cartCount", cartService.getCount());
            return "site/checkout";
        }

        // Kiểm tra giỏ hàng trống
        Collection<CartItem> items = cartService.getItems();
        if (items.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Giỏ hàng trống, không thể đặt hàng!");
            return "redirect:/cart/view";
        }

        try {
            // Tạo Order
            Order order = new Order();
            
            // TODO: Lấy user từ session khi có authentication
            // Tạm thời dùng account mặc định
            Account account = new Account();
            account.setUsername("user"); // Hardcode tạm
            order.setAccount(account);
            
            // Ghép địa chỉ đầy đủ
            String fullAddress = request.getAddress() + ", " + 
                               request.getWard() + ", " + 
                               request.getDistrict() + ", " + 
                               request.getProvince();
            order.setAddress(fullAddress);
            order.setPhone(request.getPhone());
            order.setCreateDate(LocalDateTime.now());
            order.setStatus(0); // Trạng thái: Mới đặt

            // Tạo OrderDetails
            List<OrderDetail> orderDetails = new ArrayList<>();
            for (CartItem item : items) {
                OrderDetail detail = new OrderDetail();
                
                Product product = productService.findById(item.getProductId());
                detail.setProduct(product);
                detail.setPrice(item.getPrice());
                detail.setQuantity(item.getQuantity());
                detail.setOrder(order);
                
                orderDetails.add(detail);
            }
            order.setOrderDetails(orderDetails);

            // Lưu order
            orderService.create(order);

            // Xóa giỏ hàng
            cartService.clear();

            // Thông báo thành công
            redirectAttributes.addFlashAttribute("success", "Đặt hàng thành công! Mã đơn hàng: #" + order.getId());
            redirectAttributes.addFlashAttribute("orderId", order.getId());
            
            return "redirect:/checkout/success";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/checkout";
        }
    }

    @GetMapping("/success")
    public String checkoutSuccess(Model model) {
        return "site/checkout-success";
    }
}
