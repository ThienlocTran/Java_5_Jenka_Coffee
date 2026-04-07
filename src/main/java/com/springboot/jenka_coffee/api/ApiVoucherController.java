package com.springboot.jenka_coffee.api;

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

    /**
     * POST /api/vouchers/check — Yêu cầu đăng nhập (VULN-020 FIX)
     * Chỉ trả về thông tin cần thiết cho checkout — không leak scope/productIds
     */
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

        BigDecimal subtotal = new BigDecimal(body.getOrDefault("subtotal", "0").toString());
        Voucher v = voucherService.checkVoucher(code, subtotal);
        BigDecimal discount = v.calculateDiscount(subtotal);

        // VULN-020 FIX: Không trả về scope và applicableProductIds (business intelligence leak)
        return ResponseEntity.ok(ApiResponse.success("Mã giảm giá hợp lệ!", Map.of(
                "code",           v.getCode(),
                "discountType",   v.getDiscountType(),
                "discountAmount", v.getDiscountAmount(),
                "discount",       discount,
                "finalAmount",    subtotal.subtract(discount)
        )));
    }
}
