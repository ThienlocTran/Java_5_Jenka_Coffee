package com.springboot.jenka_coffee.api.admin;

import com.springboot.jenka_coffee.dto.ApiResponse;
import com.springboot.jenka_coffee.dto.request.AccountRequest;
import com.springboot.jenka_coffee.entity.Account;
import com.springboot.jenka_coffee.service.AccountService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

// 🚨 BUG-60: MISSING AUDIT LOGS (Nhật Ký Câm Lặng / Shadow Operations)
// ================================================================
// CRITICAL SECURITY GAP: No audit trail for admin actions!
// 
// Current state:
// - Admins can modify prices, delete vouchers, change data
// - NO RECORD of who did what and when
// - NO AuditLog or AdminActionLog table exists
// - Impossible to investigate insider threats or accidental changes
// 
// Real-world scenario:
// 1. Store has 5 admins managing the system
// 2. Morning: Coffee machine price changed from 10,000,000đ → 100,000đ
// 3. Morning: SALE50 voucher (50% off max 50k) → max discount removed
// 4. Customers buy expensive machines for 100,000đ
// 5. Customers use SALE50 to get 5,000,000đ discount on 10,000,000đ orders
// 6. Store loses millions of đồng
// 7. Director asks: "WHO DID THIS? WHEN?"
// 8. System answer: 🤷‍♂️ (Complete silence - no logs)
// 
// Business impact:
// - Cannot investigate fraud or mistakes
// - Cannot prove compliance for audits
// - Cannot track who approved what
// - Cannot recover from malicious insider actions
// - Cannot sell to enterprises (audit logs are mandatory)
// - Legal liability (no proof of who authorized transactions)
// 
// Required implementation (ARCHITECTURAL):
// 
// 1. Create AuditLog entity:
// ```java
// @Entity
// @Table(name = "AuditLogs")
// public class AuditLog {
//     @Id
//     @GeneratedValue(strategy = GenerationType.IDENTITY)
//     private Long id;
//     
//     @Column(nullable = false)
//     private String username; // Who performed the action
//     
//     @Column(nullable = false)
//     private String action; // CREATE, UPDATE, DELETE
//     
//     @Column(nullable = false)
//     private String entityType; // Product, Voucher, Order, Account
//     
//     @Column(nullable = false)
//     private String entityId; // ID of affected entity
//     
//     @Column(columnDefinition = "TEXT")
//     private String oldValue; // JSON of old state
//     
//     @Column(columnDefinition = "TEXT")
//     private String newValue; // JSON of new state
//     
//     @Column(nullable = false)
//     private String ipAddress; // Request IP
//     
//     @Column(nullable = false)
//     private String userAgent; // Browser/client info
//     
//     @Column(nullable = false)
//     private LocalDateTime timestamp;
//     
//     @Column
//     private String reason; // Optional: why was this change made?
// }
// ```
// 
// 2. Create AOP aspect to intercept admin actions:
// ```java
// @Aspect
// @Component
// public class AuditLogAspect {
//     
//     @Autowired
//     private AuditLogRepository auditLogRepository;
//     
//     @Autowired
//     private HttpServletRequest request;
//     
//     // Intercept all admin controller methods
//     @Around("execution(* com.springboot.jenka_coffee.api.admin..*(..))")
//     public Object logAdminAction(ProceedingJoinPoint joinPoint) throws Throwable {
//         // Get current admin username
//         String username = SecurityContextHolder.getContext()
//             .getAuthentication().getName();
//         
//         // Get method name and parameters
//         String methodName = joinPoint.getSignature().getName();
//         Object[] args = joinPoint.getArgs();
//         
//         // Capture old state (before action)
//         Object oldState = captureOldState(methodName, args);
//         
//         // Execute the actual method
//         Object result = joinPoint.proceed();
//         
//         // Capture new state (after action)
//         Object newState = captureNewState(result);
//         
//         // Save audit log
//         AuditLog log = new AuditLog();
//         log.setUsername(username);
//         log.setAction(determineAction(methodName)); // CREATE/UPDATE/DELETE
//         log.setEntityType(determineEntityType(joinPoint));
//         log.setEntityId(extractEntityId(args));
//         log.setOldValue(toJson(oldState));
//         log.setNewValue(toJson(newState));
//         log.setIpAddress(request.getRemoteAddr());
//         log.setUserAgent(request.getHeader("User-Agent"));
//         log.setTimestamp(LocalDateTime.now());
//         auditLogRepository.save(log);
//         
//         return result;
//     }
// }
// ```
// 
// 3. Add audit log viewer in admin dashboard:
// - Filter by username, action, entity type, date range
// - Show diff between old and new values
// - Export to CSV for compliance reports
// - Alert on suspicious patterns (e.g., mass deletions)
// 
// 4. Retention policy:
// - Keep audit logs for minimum 1 year (legal requirement)
// - Archive old logs to separate table/storage
// - Never delete audit logs (immutable)
// 
// What should be logged:
// - Product price changes (old price → new price)
// - Voucher creation/modification/deletion
// - Order status changes
// - Account creation/deletion/role changes
// - Category changes
// - Banner changes
// - Any admin configuration changes
// 
// What should NOT be logged:
// - Read operations (too much noise)
// - Customer actions (separate analytics)
// - Automated system actions (unless critical)
// 
// Performance considerations:
// - Audit logging should be async (don't slow down requests)
// - Use @Async or message queue (RabbitMQ, Kafka)
// - Batch insert audit logs every 5 seconds
// - Index on username, timestamp, entityType for fast queries
// 
// Security considerations:
// - Audit logs must be immutable (no UPDATE or DELETE)
// - Only super-admin can view audit logs
// - Audit log access itself should be logged (meta-audit)
// - Store in separate database to prevent tampering
// 
// Compliance benefits:
// - SOC 2 compliance (audit trail requirement)
// - GDPR compliance (track data access/changes)
// - PCI DSS compliance (payment data changes)
// - Internal audit support
// - Forensic investigation capability
// 
// Example audit log entries:
// ```
// [2026-04-12 10:23:45] admin1 | UPDATE | Product | 123 | 
//   Old: {"price": 10000000, "name": "Máy Pha Cafe"}
//   New: {"price": 100000, "name": "Máy Pha Cafe"}
//   IP: 192.168.1.100 | Chrome/120
// 
// [2026-04-12 10:24:12] admin2 | DELETE | Voucher | SALE50 |
//   Old: {"code": "SALE50", "maxDiscountAmount": 50000}
//   New: null
//   IP: 192.168.1.101 | Firefox/119
// ```
// 
// Related files that need audit logging:
// - ApiAdminProductController (price changes, deletions)
// - ApiAdminVoucherController (voucher modifications)
// - ApiAdminOrderController (status changes)
// - ApiAdminAccountController (THIS FILE - role changes, deletions)
// - ApiAdminCategoryController (category changes)
// - ApiAdminBannerController (banner changes)
// ================================================================

@RestController
@RequestMapping("/api/admin/accounts")
@Slf4j
public class ApiAdminAccountController {

    private final AccountService accountService;

    public ApiAdminAccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<Account>>> listAccounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Account> result = accountService.findAllPaginated(PageRequest.of(page, size));
        result.forEach(a -> a.setPasswordHash(null));
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách người dùng thành công", result));
    }

    @GetMapping("/{username}")
    public ResponseEntity<ApiResponse<Account>> getAccount(@PathVariable String username) {
        Account account = accountService.findByIdOrThrow(username);
        account.setPasswordHash(null);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin tài khoản thành công", account));
    }

    @PostMapping(consumes = { "multipart/form-data" })
    public ResponseEntity<ApiResponse<Account>> createAccount(
            @Valid @ModelAttribute AccountRequest request,
            @RequestParam(value = "photoFile", required = false) MultipartFile photoFile) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Tên đăng nhập không được để trống"));
        }
        Account entity = request.toEntity();
        // VULN-057 FIX: Chỉ super-admin (username="admin") mới được tạo admin account
        // Admin thường không thể tự tạo admin khác → ngăn horizontal privilege escalation
        // Nếu muốn cho phép tạo admin, cần thêm SUPER_ADMIN role riêng
        entity.setAdmin(false); // Force false — admin flag chỉ set qua DB hoặc super-admin endpoint
        Account account = accountService.createAccount(entity, photoFile);
        account.setPasswordHash(null);
        return ResponseEntity.ok(ApiResponse.success("Thêm tài khoản mới thành công", account));
    }

    @PutMapping(value = "/{username}", consumes = { "multipart/form-data" })
    public ResponseEntity<ApiResponse<Account>> updateAccount(
            @PathVariable String username,
            @Valid @ModelAttribute AccountRequest request,
            @RequestParam(value = "photoFile", required = false) MultipartFile photoFile) {
        Account entity = request.toEntity();
        // VULN-H01 FIX: Không cho phép thay đổi admin flag qua update endpoint
        // Admin flag chỉ được set qua DB hoặc super-admin endpoint riêng
        entity.setAdmin(false);
        Account account = accountService.updateAccount(username, entity, photoFile);
        account.setPasswordHash(null);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật tài khoản thành công", account));
    }

    @DeleteMapping("/{username}")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            @PathVariable String username,
            @org.springframework.security.core.annotation.AuthenticationPrincipal String currentAdmin) {
        
        // BUG-54 FIX: Prevent admin self-deletion (suicide protection)
        if (username.equals(currentAdmin)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Không thể xóa chính tài khoản của bạn!"));
        }
        
        // BUG-54 FIX: Prevent deletion of last admin (headless system protection)
        // deleteOrThrow already checks this, but explicit check here for clarity
        Account targetAccount = accountService.findById(username);
        if (targetAccount != null && Boolean.TRUE.equals(targetAccount.getAdmin())) {
            long adminCount = accountService.getAdministrators().size();
            if (adminCount <= 1) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Không thể xóa admin cuối cùng trong hệ thống!"));
            }
        }
        
        accountService.deleteOrThrow(username);
        return ResponseEntity.ok(ApiResponse.success("Xóa tài khoản thành công", null));
    }

    @PutMapping("/{username}/toggle-status")
    public ResponseEntity<ApiResponse<Account>> toggleAccountStatus(@PathVariable String username) {
        Account account = accountService.toggleActivation(username);
        account.setPasswordHash(null);
        String status = account.getActivated() ? "kích hoạt" : "vô hiệu hóa";
        return ResponseEntity.ok(ApiResponse.success("Đã " + status + " tài khoản thành công!", account));
    }

    @PutMapping("/{username}/lock")
    public ResponseEntity<ApiResponse<Void>> lockAccount(@PathVariable String username) {
        accountService.lockAccount(username);
        return ResponseEntity.ok(ApiResponse.success("Đã khóa tài khoản thành công!", null));
    }

    @PutMapping("/{username}/unlock")
    public ResponseEntity<ApiResponse<Void>> unlockAccount(@PathVariable String username) {
        accountService.unlockAccount(username);
        return ResponseEntity.ok(ApiResponse.success("Đã mở khóa tài khoản thành công!", null));
    }

    @PutMapping("/{username}/reset-password")
    public ResponseEntity<ApiResponse<Void>> adminResetPassword(
            @PathVariable String username,
            @RequestBody Map<String, String> body,
            @org.springframework.security.core.annotation.AuthenticationPrincipal String currentAdmin) {
        
        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Mật khẩu mới không được để trống"));
        }
        
        // BUG-55 FIX: Prevent admin from resetting another admin's password (insider threat)
        // Only allow password reset for non-admin users
        // For admin password reset, require email OTP or super-admin privilege
        Account targetAccount = accountService.findById(username);
        if (targetAccount != null && Boolean.TRUE.equals(targetAccount.getAdmin())) {
            // Check if current admin is trying to reset another admin's password
            if (!username.equals(currentAdmin)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error(
                            "Không thể đổi mật khẩu của admin khác! " +
                            "Admin phải tự đổi mật khẩu qua tính năng 'Quên mật khẩu' với xác thực email."));
            }
        }
        
        accountService.adminResetPassword(username, newPassword);
        return ResponseEntity.ok(ApiResponse.success("Đã reset mật khẩu thành công!", null));
    }

    @GetMapping("/check-username")
    public ResponseEntity<ApiResponse<Boolean>> checkUsername(@RequestParam String username) {
        boolean isAvailable = !accountService.existsByUsername(username);
        return ResponseEntity.ok(ApiResponse.success("Kiểm tra username", isAvailable));
    }

    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<Boolean>> checkEmail(
            @RequestParam String email,
            @RequestParam(required = false) String currentUsername) {

        if (currentUsername != null && !currentUsername.isEmpty()) {
            Account currentAccount = accountService.findById(currentUsername);
            if (currentAccount != null && currentAccount.getEmail().equals(email)) {
                return ResponseEntity.ok(ApiResponse.success("Kiểm tra email hợp lệ (gmail cũ)", true));
            }
        }
        boolean isAvailable = !accountService.existsByEmail(email);
        return ResponseEntity.ok(ApiResponse.success("Kiểm tra email trùng lặp", isAvailable));
    }
}
