package com.springboot.jenka_coffee.api;

import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.dto.request.CheckoutRequest;
import com.springboot.jenka_coffee.dto.response.OrderHistoryDTO;
import com.springboot.jenka_coffee.entity.Account;
import com.springboot.jenka_coffee.entity.Order;
import com.springboot.jenka_coffee.service.AccountService;
import com.springboot.jenka_coffee.service.CartService;
import com.springboot.jenka_coffee.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class ApiOrderController {

    private final CartService cartService;
    private final OrderService orderService;
    private final AccountService accountService;

    public ApiOrderController(CartService cartService, OrderService orderService, AccountService accountService) {
        this.cartService = cartService;
        this.orderService = orderService;
        this.accountService = accountService;
    }

    @GetMapping("/checkout-info")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCheckoutInfo(
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Chưa đăng nhập"));
        }
        if (cartService.getItems().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Giỏ hàng trống"));
        }
        Account user = accountService.findById(username);
        CheckoutRequest request = orderService.prepareCheckoutRequest(user);

        Map<String, Object> data = new HashMap<>();
        data.put("checkoutRequest", request);
        data.put("cartItems",  cartService.getItems());
        data.put("cartTotal",  cartService.getTotal());
        data.put("cartCount",  cartService.getCount());
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin thanh toán thành công", data));
    }

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<Map<String, Object>>> processCheckout(
            @Valid @RequestBody CheckoutRequest request,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Chưa đăng nhập"));
        }
        Account user = accountService.findById(username);
        Order order = orderService.checkout(request, user);
        orderService.postCheckout(order, user);
        return ResponseEntity.ok(ApiResponse.success(
                "Đặt hàng thành công! Mã đơn hàng: #" + order.getId(),
                Map.of("orderId", order.getId())));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrderHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Chưa đăng nhập"));
        }
        // BUG-43 FIX: Add page limit validation to prevent deep pagination DoS
        // Attacker can request page=99999999 causing database to scan billions of rows
        // Same protection as ApiProductController (max page 1000)
        if (page > 1000) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Số trang không được vượt quá 1000"));
        }
        
        Pageable pageable = PageRequest.of(page, Math.min(size, 20), Sort.by("id").descending());
        Page<Order> orderPage = orderService.findByUsernameWithDetails(username, pageable);

        // Convert to DTO to include orderDetails with product info
        List<OrderHistoryDTO> orderDTOs = orderPage.getContent().stream()
                .map(this::convertToOrderHistoryDTO)
                .toList();

        Map<String, Object> data = new HashMap<>();
        data.put("items",       orderDTOs);
        data.put("currentPage", orderPage.getNumber());
        data.put("totalPages",  orderPage.getTotalPages());
        data.put("totalItems",  orderPage.getTotalElements());
        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử đơn hàng thành công", data));
    }
    
    /**
     * Convert Order entity to OrderHistoryDTO with orderDetails and product info
     */
    private com.springboot.jenka_coffee.dto.response.OrderHistoryDTO convertToOrderHistoryDTO(Order order) {
        com.springboot.jenka_coffee.dto.response.OrderHistoryDTO dto = new com.springboot.jenka_coffee.dto.response.OrderHistoryDTO();
        dto.setId(order.getId());
        dto.setAddress(order.getAddress());
        dto.setCreateDate(order.getCreateDate());
        dto.setPhone(order.getPhone());
        dto.setStatus(order.getStatus());
        dto.setVoucherCode(order.getVoucherCode());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setPointsUsed(order.getPointsUsed());
        dto.setNote(order.getNote());
        
        // Convert orderDetails
        if (order.getOrderDetails() != null) {
            List<com.springboot.jenka_coffee.dto.response.OrderHistoryDTO.OrderDetailDTO> detailDTOs = 
                order.getOrderDetails().stream()
                    .map(detail -> {
                        com.springboot.jenka_coffee.dto.response.OrderHistoryDTO.OrderDetailDTO detailDTO = 
                            new com.springboot.jenka_coffee.dto.response.OrderHistoryDTO.OrderDetailDTO();
                        detailDTO.setId(detail.getId());
                        detailDTO.setPrice(detail.getPrice());
                        detailDTO.setQuantity(detail.getQuantity());
                        
                        // Convert product
                        if (detail.getProduct() != null) {
                            com.springboot.jenka_coffee.dto.response.OrderHistoryDTO.ProductDTO productDTO = 
                                new com.springboot.jenka_coffee.dto.response.OrderHistoryDTO.ProductDTO();
                            productDTO.setId(detail.getProduct().getId());
                            productDTO.setName(detail.getProduct().getName());
                            productDTO.setImage(detail.getProduct().getImage());
                            detailDTO.setProduct(productDTO);
                        }
                        
                        return detailDTO;
                    })
                    .toList();
            dto.setOrderDetails(detailDTOs);
        }
        
        return dto;
    }
    
    /**
     * VULN-ORDER-DETAIL-BLACKHOLE FIX: Add endpoint to get order details
     * Allows users to view what products they ordered
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrderDetail(
            @PathVariable Long orderId,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Chưa đăng nhập"));
        }
        Order order = orderService.findById(orderId);
        
        if (order == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Không tìm thấy đơn hàng"));
        }
        
        // VULN-IDOR FIX: Proper authorization check for both logged-in and guest orders
        // Guest orders (account == null) cannot be accessed via this API
        // Guest users must create account or contact support to view order
        if (order.getAccount() == null) {
            // Guest order - deny access via API
            // TODO: Implement access token mechanism for guest orders
            // For now, guest must contact support with order ID
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(
                        "Đơn hàng này được đặt bởi khách vãng lai. " +
                        "Vui lòng đăng ký tài khoản hoặc liên hệ hotline để tra cứu đơn hàng."));
        }
        
        // Logged-in user - check ownership
        if (!order.getAccount().getUsername().equals(username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Bạn không có quyền xem đơn hàng này"));
        }
        
        // Build response with order details
        Map<String, Object> data = new HashMap<>();
        data.put("id", order.getId());
        data.put("address", order.getAddress());
        data.put("phone", order.getPhone());
        data.put("createDate", order.getCreateDate());
        data.put("status", order.getStatus());
        data.put("totalAmount", order.getTotalAmount());
        data.put("voucherCode", order.getVoucherCode());
        data.put("note", order.getNote());
        data.put("pointsUsed", order.getPointsUsed());
        
        // Add order details (products)
        var orderDetails = order.getOrderDetails().stream()
                .map(detail -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("productId", detail.getProduct().getId());
                    item.put("productName", detail.getProduct().getName());
                    item.put("productImage", detail.getProduct().getImage());
                    item.put("price", detail.getPrice());
                    item.put("quantity", detail.getQuantity());
                    return item;
                }).toList();
        data.put("orderDetails", orderDetails);
        
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết đơn hàng thành công", data));
    }
}
