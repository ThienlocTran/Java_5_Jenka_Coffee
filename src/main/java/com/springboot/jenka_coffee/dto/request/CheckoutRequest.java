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
    @Size(min = 10, max = 255, message = "{CheckoutRequest.address.Size}")
    private String address;

    @NotBlank(message = "{CheckoutRequest.province.NotBlank}")
    private String province;

    @NotBlank(message = "{CheckoutRequest.district.NotBlank}")
    private String district;

    @NotBlank(message = "{CheckoutRequest.ward.NotBlank}")
    private String ward;

    @NotBlank(message = "{CheckoutRequest.paymentMethod.NotBlank}")
    private String paymentMethod;

    @Size(max = 500, message = "{CheckoutRequest.note.Size}")
    private String note;

    @AssertTrue(message = "{CheckoutRequest.agreeTerms.AssertTrue}")
    private boolean agreeTerms;
}
