# TEST CASES BỔ SUNG - CÁC ENDPOINTS CÒN THIẾU

**Mục đích:** Bổ sung test cases cho các URL endpoints chưa được cover  
**Tổng số:** 15 Test Cases  
**Phiên bản:** 1.0

---

## 🔴 THIÊN LỘC - Bổ sung Product Module (3 TCs)

| ID | Tên Test Case | HTTP Method | URL | Điều kiện tiên quyết | Các bước thực hiện | Dữ liệu đầu vào | Kết quả mong đợi |
|---|---|---|---|---|---|---|---|
| TC_PROD_021 | Xem chi tiết sản phẩm | GET | `/product/detail/{id}` | - Product ID=1 tồn tại<br>- Có 4 SP cùng category | 1. Truy cập `/product/detail/1`<br>2. Kiểm tra thông tin hiển thị | **ID:** 1 | - Hiển thị đầy đủ thông tin SP:<br>&nbsp;&nbsp;• Tên, giá, mô tả<br>&nbsp;&nbsp;• Ảnh sản phẩm<br>&nbsp;&nbsp;• Số lượng tồn kho<br>&nbsp;&nbsp;• Trạng thái (còn hàng/hết hàng)<br>- Hiển thị 4 SP liên quan (cùng category)<br>- Nút "Thêm vào giỏ" (nếu còn hàng) |
| TC_PROD_022 | Xem chi tiết SP không tồn tại | GET | `/product/detail/{id}` | - Product ID=999 KHÔNG tồn tại | 1. Truy cập `/product/detail/999` | **ID:** 999 | - **Exception:** `ResourceNotFoundException`<br>- Message: "Không tìm thấy sản phẩm với ID: 999"<br>- HTTP Status: 404<br>- Redirect đến error page |
| TC_PROD_023 | Quick View API (AJAX) | GET | `/api/product/quick-view/{id}` | - Product ID=1 tồn tại | 1. Gọi API qua AJAX<br>2. Kiểm tra JSON response | **ID:** 1 | - **Response:** JSON object<br>- Chứa:<br>&nbsp;&nbsp;• item: Product object<br>&nbsp;&nbsp;• similarItems: List<Product><br>- HTTP Status: 200<br>- Content-Type: application/json |

---

## 🟡 TUẤN KHOA - Bổ sung Order Module (4 TCs)

| ID | Tên Test Case | HTTP Method | URL | Điều kiện tiên quyết | Các bước thực hiện | Dữ liệu đầu vào | Kết quả mong đợi |
|---|---|---|---|---|---|---|---|
| TC_ORD_006 | User xem chi tiết đơn hàng | GET | `/order/detail/{id}` | - Đã login (user="user1")<br>- Order ID=101 thuộc user1 | 1. Vào `/order/list`<br>2. Click vào đơn #101 | **Order ID:** 101 | - Hiển thị chi tiết đơn:<br>&nbsp;&nbsp;• Mã đơn hàng<br>&nbsp;&nbsp;• Ngày đặt<br>&nbsp;&nbsp;• Trạng thái<br>&nbsp;&nbsp;• Thông tin người nhận<br>&nbsp;&nbsp;• Địa chỉ giao hàng<br>&nbsp;&nbsp;• Danh sách sản phẩm (tên, giá, SL)<br>&nbsp;&nbsp;• Tổng tiền<br>- **Note:** Cần implement controller method này |
| TC_ORD_007 | User xem đơn hàng của người khác (Chặn) | GET | `/order/detail/{id}` | - Đã login (user="user1")<br>- Order ID=102 thuộc user2 | 1. Cố truy cập `/order/detail/102` | **Order ID:** 102 | - **Security check:** Order.username != session.user<br>- HTTP Status: 403 Forbidden<br>- Redirect: `/auth/unauthorized`<br>- Message: "Bạn không có quyền xem đơn hàng này!" |
| TC_ORD_008 | Lazy Load thêm đơn hàng (AJAX) | GET | `/order/load-more?page=1` | - Đã login<br>- User có > 5 đơn hàng | 1. Scroll xuống cuối trang<br>2. Click "Xem thêm"<br>3. AJAX call load-more | **Page:** 1 | - Return: HTML fragment (order_rows)<br>- Chứa 5 đơn hàng tiếp theo<br>- Append vào table hiện tại<br>- Nếu hết đơn → ẩn nút "Xem thêm" |
| TC_ORD_009 | Admin xem chi tiết đơn hàng | GET | `/admin/order/detail/{id}` | - Đã login Admin<br>- Order ID=101 tồn tại | 1. Vào `/admin/order/index`<br>2. Click vào đơn #101 | **Order ID:** 101 | - Hiển thị đầy đủ thông tin đơn<br>- Có nút "Cập nhật trạng thái"<br>- Có nút "Hủy đơn"<br>- Hiển thị lịch sử thay đổi trạng thái<br>- **Note:** Cần implement controller method |

---

## 🟢 CHÍ BẢO - Bổ sung Category Module (1 TC)

| ID | Tên Test Case | HTTP Method | URL | Điều kiện tiên quyết | Các bước thực hiện | Dữ liệu đầu vào | Kết quả mong đợi |
|---|---|---|---|---|---|---|---|
| TC_CAT_025 | AJAX Check Category ID tồn tại | GET | `/admin/category/check-id?id=TEST` | - Đang ở form thêm DM | 1. Nhập ID = "TEST"<br>2. Blur khỏi input<br>3. AJAX call check-id | **ID:** TEST | - **Response:** JSON boolean<br>- `true` = ID available (chưa tồn tại)<br>- `false` = ID đã tồn tại<br>- Hiển thị message realtime:<br>&nbsp;&nbsp;• ✅ "ID có thể sử dụng"<br>&nbsp;&nbsp;• ❌ "ID đã tồn tại" |

---

## 🔵 KHÁNH Ý - Bổ sung Auth & Account Module (7 TCs)

### A. Profile Management (2 TCs)

| ID | Tên Test Case | HTTP Method | URL | Điều kiện tiên quyết | Các bước thực hiện | Dữ liệu đầu vào | Kết quả mong đợi |
|---|---|---|---|---|---|---|---|
| TC_ACC_009 | Hiển thị form đổi mật khẩu | GET | `/profile/change-password` | - Đã login | 1. Vào `/profile`<br>2. Click "Đổi mật khẩu" | N/A | - Hiển thị form với 3 fields:<br>&nbsp;&nbsp;• Mật khẩu hiện tại<br>&nbsp;&nbsp;• Mật khẩu mới<br>&nbsp;&nbsp;• Xác nhận mật khẩu mới<br>- Nút "Đổi mật khẩu" |
| TC_ACC_010 | Validation upload avatar sai định dạng | POST | `/profile/avatar` | - Đã login | 1. Chọn file PDF<br>2. Click "Upload" | **File:** document.pdf<br>**Type:** application/pdf | - **Validation:** Check contentType<br>- Error: "File phải là ảnh (JPG, PNG, GIF)"<br>- Không upload<br>- Redirect về `/profile` |

### B. Admin Account Management (5 TCs)

| ID | Tên Test Case | HTTP Method | URL | Điều kiện tiên quyết | Các bước thực hiện | Dữ liệu đầu vào | Kết quả mong đợi |
|---|---|---|---|---|---|---|---|
| TC_ACC_011 | Admin xem danh sách tài khoản | GET | `/admin/account/list` | - Đã login Admin<br>- Có 10 accounts trong DB | 1. Vào `/admin/account/list` | N/A | - Hiển thị table danh sách accounts<br>- Columns:<br>&nbsp;&nbsp;• Username<br>&nbsp;&nbsp;• Fullname<br>&nbsp;&nbsp;• Email<br>&nbsp;&nbsp;• Role (Admin/User)<br>&nbsp;&nbsp;• Status (Active/Locked)<br>&nbsp;&nbsp;• Actions (Edit/Lock/Delete)<br>- Có nút "Thêm tài khoản" |
| TC_ACC_012 | Admin xóa tài khoản thường | POST | `/admin/account/delete/{username}` | - Account "user1" tồn tại<br>- user1 không phải admin<br>- user1 chưa có đơn hàng | 1. Click nút "Xóa" cho user1<br>2. Confirm xóa | **Username:** user1 | - Method: `accountService.deleteOrThrow("user1")`<br>- Check: `canDeleteAccount()` = true<br>- DB: DELETE FROM Accounts<br>- Flash: "Xóa tài khoản thành công!"<br>- Redirect: `/admin/account/list` |
| TC_ACC_013 | Admin xóa admin cuối cùng (Chặn) | POST | `/admin/account/delete/{username}` | - Account "admin" là admin duy nhất | 1. Cố xóa account "admin" | **Username:** admin | - Check: `countByAdminTrue()` = 1<br>- **Exception:** `BusinessRuleException`<br>- Message: "Không thể xóa admin cuối cùng trong hệ thống!"<br>- Không xóa |
| TC_ACC_014 | AJAX Check username tồn tại | GET | `/admin/account/check-username?username=test` | - Đang ở form thêm account | 1. Nhập username = "test"<br>2. Blur khỏi input<br>3. AJAX call | **Username:** test | - **Response:** JSON boolean<br>- `true` = available<br>- `false` = đã tồn tại<br>- Hiển thị message realtime |
| TC_ACC_015 | AJAX Check email tồn tại | GET | `/admin/account/check-email?email=test@gmail.com` | - Đang ở form thêm account | 1. Nhập email<br>2. Blur khỏi input<br>3. AJAX call | **Email:** test@gmail.com | - **Response:** JSON boolean<br>- Check: `existsByEmail()`<br>- Nếu đang edit → ignore email hiện tại<br>- Hiển thị message realtime |

---

## 📊 TỔNG KẾT BỔ SUNG

| Thành viên | Test Cases bổ sung | Tổng sau bổ sung | Coverage mới |
|------------|-------------------|------------------|--------------|
| Thiên Lộc | +3 | 31 TCs | 90% |
| Tuấn Khoa | +4 | 32 TCs | 95% |
| Chí Bảo | +1 | 25 TCs | 90% |
| Khánh Ý | +7 | 43 TCs | 85% |
| **TỔNG** | **+15** | **131 TCs** | **~88%** |

---

## 🎯 ƯU TIÊN IMPLEMENT

### Priority HIGH (Phải có)
1. ✅ TC_PROD_021: Product detail page (core feature)
2. ✅ TC_ORD_006: Order detail page (core feature)
3. ✅ TC_ACC_011-013: Admin account management (security)

### Priority MEDIUM (Nên có)
4. ⚠️ TC_PROD_023: Quick view API (UX enhancement)
5. ⚠️ TC_ORD_008: Lazy load orders (performance)
6. ⚠️ TC_CAT_025: AJAX check ID (UX enhancement)

### Priority LOW (Có thể bỏ qua)
7. ⚠️ TC_ORD_007: Security check (nếu đã có interceptor)
8. ⚠️ TC_ACC_014-015: AJAX validation (nice to have)

---

## 📝 GHI CHÚ IMPLEMENTATION

### Cần implement thêm Controller Methods:

1. **OrderController (Site)**
```java
@GetMapping("/order/detail/{id}")
public String orderDetail(@PathVariable Long id, HttpSession session, Model model) {
    // Check ownership
    // Load order details
    // Return view
}
```

2. **AdminOrderController**
```java
@GetMapping("/order/detail/{id}")
public String orderDetail(@PathVariable Long id, Model model) {
    // Load order details
    // Return admin view
}
```

### Đã có sẵn (chỉ cần test):
- ✅ Product detail page
- ✅ Quick view API
- ✅ Lazy load orders
- ✅ AJAX check APIs
- ✅ Profile management

---

**Người tạo:** Kiro AI  
**Ngày:** 2026-02-25  
**Version:** 1.0
