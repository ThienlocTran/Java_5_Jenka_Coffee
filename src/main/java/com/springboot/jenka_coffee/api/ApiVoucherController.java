package com.springboot.jenka_coffee.api;

import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.entity.Voucher;
import com.springboot.jenka_coffee.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class ApiVoucherController {

    private final VoucherService voucherService;

    /**
     * POST /api/vouchers/check
     * Body: { "code": "SALE10", "subtotal": 500000 }
     * Trả về thông tin voucher + số tiền giảm nếu hợp lệ
     */
    @PostMapping("/check")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkVoucher(
            @RequestBody Map<String, Object> body) {
        String code = (String) body.get("code");
        BigDecimal subtotal = new BigDecimal(body.getOrDefault("subtotal", "0").toString());

        Voucher v = voucherService.checkVoucher(code, subtotal);
        BigDecimal discount = v.calculateDiscount(subtotal);

        return ResponseEntity.ok(ApiResponse.success("Mã giảm giá hợp lệ!", Map.of(
                "code",         v.getCode(),
                "discountType", v.getDiscountType(),
                "discountAmount", v.getDiscountAmount(),
                "discount",     discount,
                "finalAmount",  subtotal.subtract(discount),
                "scope",        v.getScope(),
                "applicableProductIds", v.getApplicableProductIds() != null ? v.getApplicableProductIds() : "[]"
        )));
    }
}
