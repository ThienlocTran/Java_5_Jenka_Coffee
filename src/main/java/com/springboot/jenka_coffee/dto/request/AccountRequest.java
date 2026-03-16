package com.springboot.jenka_coffee.dto.request;

import com.springboot.jenka_coffee.entity.Account;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountRequest {

    // username validated manually in controller (not required for update)
    @Size(min = 3, max = 50, message = "{AccountRequest.username.Size}")
    @Pattern(regexp = "^[a-zA-Z0-9_]*$", message = "{AccountRequest.username.Pattern}")
    private String username;

    @NotBlank(message = "{AccountRequest.fullname.NotBlank}")
    @Size(min = 3, max = 100, message = "{AccountRequest.fullname.Size}")
    private String fullname;

    // @Email rejects empty string — use custom check instead
    @Pattern(regexp = "^$|^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$", message = "{AccountRequest.email.Email}")
    private String email;

    @Pattern(regexp = "^$|(0|\\+84)(\\s|\\.)?((3[2-9])|(5[689])|(7[06-9])|(8[1-689])|(9[0-46-9]))(\\d)(\\s|\\.)?(\\d{3})(\\s|\\.)?(\\d{3})$", message = "{AccountRequest.phone.Pattern}")
    private String phone;

    @Size(min = 6, message = "{AccountRequest.password.Size}")
    private String password;

    private Boolean admin = false;
    private Boolean activated = true;
    private String photo;

    /**
     * Format dữ liệu trước khi validation
     */
    public void normalize() {
        if (username != null)
            username = username.trim().toLowerCase();
        if (fullname != null)
            fullname = fullname.trim();
        if (email != null)
            email = email.trim().toLowerCase();
        if (phone != null)
            phone = phone.trim().replaceAll("\\s", "");
    }

    /**
     * Convert DTO to Entity
     */
    public Account toEntity() {
        normalize();

        Account account = new Account();
        account.setUsername(username);
        account.setFullname(fullname);
        account.setEmail(email != null && !email.isEmpty() ? email : "");
        account.setPhone(phone);
        account.setPasswordHash(password);
        account.setAdmin(admin != null ? admin : false);
        account.setActivated(activated != null ? activated : true);
        account.setPhoto(photo);
        account.setPoints(0);
        account.setCustomerRank("MEMBER");

        return account;
    }

    /**
     * Create DTO from Entity (for edit form)
     */
    public static AccountRequest fromEntity(Account account) {
        AccountRequest dto = new AccountRequest();
        dto.setUsername(account.getUsername());
        dto.setFullname(account.getFullname());
        dto.setEmail(account.getEmail());
        dto.setPhone(account.getPhone());
        dto.setAdmin(account.getAdmin());
        dto.setActivated(account.getActivated());
        dto.setPhoto(account.getPhoto());
        // Don't set password - it should remain empty for edit form
        return dto;
    }
}
