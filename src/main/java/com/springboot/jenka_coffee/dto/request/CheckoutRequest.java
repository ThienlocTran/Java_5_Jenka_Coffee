package com.springboot.jenka_coffee.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CheckoutRequest {

    @NotBlank(message = "{CheckoutRequest.fullname.NotBlank}")
    @Size(min = 3, max = 100, message = "{CheckoutRequest.fullname.Size}")
    @Pattern(regexp = "^[\\p{L}\\s]+$", message = "{CheckoutRequest.fullname.Pattern}")
    private String fullname;

    @NotBlank(message = "{CheckoutRequest.email.NotBlank}")
    @Email(message = "{CheckoutRequest.email.Email}")
    private String email;

    @NotBlank(message = "{CheckoutRequest.phone.NotBlank}")
    @Pattern(regexp = "^(0|\\+84)(\\s|\\.)?((3[2-9])|(5[689])|(7[06-9])|(8[1-689])|(9[0-46-9]))(\\d)(\\s|\\.)?(\\d{3})(\\s|\\.)?(\\d{3})$", 
             message = "{CheckoutRequest.phone.Pattern}")
    private String phone;

    @NotBlank(message = "{CheckoutRequest.address.NotBlank}")
    @Size(min = 3, max = 255, message = "{CheckoutRequest.address.Size}")
    @Pattern(regexp = "^[^<>]+$", message = "{CheckoutRequest.address.Pattern}")
    private String address;

    @NotBlank(message = "{CheckoutRequest.province.NotBlank}")
    private String province;

    @NotBlank(message = "{CheckoutRequest.district.NotBlank}")
    private String district;

    @NotBlank(message = "{CheckoutRequest.ward.NotBlank}")
    private String ward;

    // 🚨 BUG-59: MISSING SHIPPING FEE FIELD (Lỗ Sâu Vận Chuyển)
    // ================================================================
    // See detailed documentation in Order.java entity
    // 
    // This DTO needs shippingFee field to support shipping fee calculation.
    // Currently system has NO WAY to charge shipping fees.
    // 
    // Required addition:
    // ```java
    // @DecimalMin(value = "0.0", message = "Phí vận chuyển không được âm")
    // @Digits(integer = 18, fraction = 2, message = "Phí vận chuyển không hợp lệ")
    // private BigDecimal shippingFee;
    // ```
    // 
    // Note: Shipping fee can be:
    // 1. Calculated server-side based on address (recommended)
    // 2. Sent from frontend after calling shipping API
    // 3. Set to 0 for free shipping promotions
    // ================================================================

    // VULN-050 FIX: Whitelist payment method — không cho phép giá trị tùy ý
    @NotBlank(message = "{CheckoutRequest.paymentMethod.NotBlank}")
    @Pattern(regexp = "^(cod|bank|momo)$", message = "Phương thức thanh toán không hợp lệ")
    private String paymentMethod;

    // VULN-058 FIX: note có size limit — sanitize XSS trong service
    @Size(max = 500, message = "{CheckoutRequest.note.Size}")
    private String note;

    /** Mã giảm giá (tùy chọn) */
    @Size(max = 20)
    private String voucherCode;

    // VULN-CONSENT-BYPASS FIX: Validate terms agreement on server-side
    // Frontend validation can be bypassed via curl/Postman
    // Server must verify user consent for legal compliance
    @AssertTrue(message = "Bạn phải đồng ý với điều khoản và điều kiện để đặt hàng")
    private boolean agreeTerms;
}
