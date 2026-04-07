package com.springboot.jenka_coffee.api.admin;

import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.entity.Voucher;
import com.springboot.jenka_coffee.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/vouchers")
@RequiredArgsConstructor
public class ApiAdminVoucherController {

    private final VoucherService voucherService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<Voucher>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Voucher> result = voucherService.findAll(
                PageRequest.of(page, Math.min(size, 100), Sort.by("code").ascending()));
        return ResponseEntity.ok(ApiResponse.success("OK", result));
    }

    @GetMapping("/{code}")
    public ResponseEntity<ApiResponse<Voucher>> get(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.success("OK", voucherService.findByCode(code)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Voucher>> create(@RequestBody Voucher voucher) {
        Voucher saved = voucherService.save(voucher);
        return ResponseEntity.ok(ApiResponse.success("Tạo voucher thành công", saved));
    }

    @PutMapping("/{code}")
    public ResponseEntity<ApiResponse<Voucher>> update(
            @PathVariable String code,
            @RequestBody Voucher voucher) {
        voucher.setCode(code); // đảm bảo code không bị thay đổi
        Voucher saved = voucherService.save(voucher);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật voucher thành công", saved));
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String code) {
        voucherService.delete(code);
        return ResponseEntity.ok(ApiResponse.success("Xóa voucher thành công", null));
    }

    @PutMapping("/{code}/toggle")
    public ResponseEntity<ApiResponse<Voucher>> toggle(@PathVariable String code) {
        Voucher v = voucherService.toggle(code);
        String msg = Boolean.TRUE.equals(v.getActive()) ? "Đã kích hoạt voucher" : "Đã tắt voucher";
        return ResponseEntity.ok(ApiResponse.success(msg, v));
    }
}
