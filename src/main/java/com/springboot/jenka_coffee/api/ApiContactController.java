package com.springboot.jenka_coffee.api;

import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.dto.request.ContactRequest;
import com.springboot.jenka_coffee.service.ContactService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contact")
public class ApiContactController {

    private final ContactService contactService;

    public ApiContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<String>> sendContact(@Valid @RequestBody ContactRequest contact) {
        try {
            contactService.sendContactEmail(contact);
            return ResponseEntity.ok(ApiResponse.success("Tin nhắn của bạn đã được gửi thành công!", "OK"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Không thể gửi tin nhắn vào lúc này. Vui lòng thử lại sau."));
        }
    }
}
