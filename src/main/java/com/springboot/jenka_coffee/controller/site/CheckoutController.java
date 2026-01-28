package com.springboot.jenka_coffee.controller.site;

import com.springboot.jenka_coffee.dto.request.CheckoutRequest;
import com.springboot.jenka_coffee.entity.Order;
import com.springboot.jenka_coffee.service.CartService;
import com.springboot.jenka_coffee.service.CheckoutService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/checkout")
public class CheckoutController {

    private final CartService cartService;
    private final CheckoutService checkoutService;

    public CheckoutController(CartService cartService, CheckoutService checkoutService) {
        this.cartService = cartService;
        this.checkoutService = checkoutService;
    }

    @GetMapping
    public String showCheckoutForm(Model model) {
        // Kiểm tra giỏ hàng trống
        if (checkoutService.isCartEmpty()) {
            return "redirect:/cart/view";
        }

        // Tạo form object mới nếu chưa có
        if (!model.containsAttribute("checkoutRequest")) {
            model.addAttribute("checkoutRequest", new CheckoutRequest());
        }

        // Đưa dữ liệu giỏ hàng vào model
        model.addAttribute("cartItems", cartService.getItems());
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

        try {
            // Xử lý checkout qua service
            Order order = checkoutService.processCheckout(request);

            // Thông báo thành công
            redirectAttributes.addFlashAttribute("success", "Đặt hàng thành công! Mã đơn hàng: #" + order.getId());
            redirectAttributes.addFlashAttribute("orderId", order.getId());

            return "redirect:/checkout/success";

        } catch (IllegalStateException e) {
            // Lỗi nghiệp vụ (giỏ hàng trống, sản phẩm không tồn tại...)
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/cart/view";

        } catch (Exception e) {
            // Lỗi hệ thống
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra khi đặt hàng. Vui lòng thử lại!");
            return "redirect:/checkout";
        }
    }

    @GetMapping("/success")
    public String checkoutSuccess() {
        return "site/checkout-success";
    }
}
