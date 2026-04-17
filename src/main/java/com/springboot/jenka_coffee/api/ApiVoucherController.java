package com.springboot.jenka_coffee.api;

// ============================================================================
// PHASE 1 SCOPE REDUCTION: Voucher feature temporarily disabled
// ============================================================================
// Customer decided to simplify checkout flow - no voucher discounts in Phase 1.
// Checkout will only use product sale prices (if any).
// Voucher functionality will be re-enabled in Phase 2.
// ============================================================================

/*
import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.entity.Voucher;
import com.springboot.jenka_coffee.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class ApiVoucherController {

    private final VoucherService voucherService;

    @PostMapping("/check")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkVoucher(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal String username) {

        if (username == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Vui lòng đăng nhập để sử dụng mã giảm giá!"));
        }

        String code = (String) body.get("code");
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Vui lòng nhập mã giảm giá!"));
        }

        BigDecimal subtotal;
        try {
            subtotal = new BigDecimal(body.getOrDefault("subtotal", "0").toString());
            if (subtotal.compareTo(BigDecimal.ZERO) < 0 ||
                    subtotal.compareTo(new BigDecimal("100000000000")) > 0) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Giá trị đơn hàng không hợp lệ!"));
            }
        } catch (NumberFormatException | ArithmeticException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Giá trị đơn hàng không hợp lệ!"));
        }
        Voucher v = voucherService.checkVoucher(code, subtotal);
        BigDecimal discount = v.calculateDiscount(subtotal);

        return ResponseEntity.ok(ApiResponse.success("Mã giảm giá hợp lệ!", Map.of(
                "code",           v.getCode(),
                "discountType",   v.getDiscountType(),
                "discountAmount", v.getDiscountAmount(),
                "discount",       discount,
                "finalAmount",    subtotal.subtract(discount)
        )));
    }
}
*/
