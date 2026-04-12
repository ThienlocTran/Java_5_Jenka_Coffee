package com.springboot.jenka_coffee.api.admin;

import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.entity.Contact;
import com.springboot.jenka_coffee.service.ContactService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/contacts")
public class ApiAdminContactController {

    private final ContactService contactService;

    public ApiAdminContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getContacts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<Contact> contactPage = contactService.findAll(PageRequest.of(page, size));

        Map<String, Object> data = new HashMap<>();
        data.put("items", contactPage.getContent());
        data.put("currentPage", contactPage.getNumber());
        data.put("totalPages", contactPage.getTotalPages());
        data.put("totalItems", contactPage.getTotalElements());
        data.put("unreadCount", contactService.countUnread());

        return ResponseEntity.ok(ApiResponse.success("OK", data));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(@PathVariable Long id) {
        contactService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success("Đã đánh dấu đã đọc", null));
    }

    @PatchMapping("/mark-all-read")
    public ResponseEntity<ApiResponse<Void>> markAllRead() {
        contactService.markAllAsRead();
        return ResponseEntity.ok(ApiResponse.success("Đã đánh dấu tất cả là đã đọc", null));
    }
}
