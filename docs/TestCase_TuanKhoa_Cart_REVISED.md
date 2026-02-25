# TEST CASES - TUẤN KHOA (Cart & Checkout Module)

**Thành viên:** Tuấn Khoa  
**Module:** Shopping Cart & Checkout Process  
**Tổng số Test Cases:** 28  
**Phiên bản:** 2.0 (Revised)

---

## 📋 DANH SÁCH TEST CASES

### A. SHOPPING CART (10 TCs)

| ID | Tên Test Case | Điều kiện tiên quyết | Các bước thực hiện | Dữ liệu đầu vào | Kết quả mong đợi |
|---|---|---|---|---|---|
| TC_CART_001 | Thêm sản phẩm mới vào giỏ hàng | - Giỏ hàng trống<br>- Product ID=1 tồn tại, còn hàng<br>- Session active | 1. Vào trang chi tiết SP ID=1<br>2. Click nút "Thêm vào giỏ"<br>3. Kiểm tra giỏ hàng | **Product ID:** 1<br>**Quantity:** 1<br>**Price:** 50000 | - Method: `cartService.add(1)`<br>- Session cart map: {1: CartItem}<br>- CartItem: {productId=1, quantity=1, price=50000}<br>- Icon giỏ hàng hiện badge số (1)<br>- Tổng tiền = 50000đ |
| TC_CART_002 | Cộng dồn số lượng SP đã có trong giỏ | - Giỏ đã có SP ID=1 (qty=1) | 1. Thêm tiếp SP ID=1 lần nữa<br>2. Kiểm tra giỏ hàng | **Product ID:** 1<br>**Existing Qty:** 1 | - Method: `cartService.add(1)`<br>- Giỏ vẫn 1 dòng (không duplicate)<br>- Quantity tăng lên 2<br>- Tổng tiền = 50000 × 2 = 100000đ<br>- Badge giỏ hàng = 2 |
| TC_CART_003 | Xóa sản phẩm khỏi giỏ hàng | - Giỏ có SP ID=1 | 1. Vào trang `/cart/view`<br>2. Click nút "Xóa" (icon thùng rác) cho SP ID=1<br>3. Confirm xóa | **Product ID:** 1 | - Method: `cartService.remove(1)`<br>- SP biến mất khỏi giỏ<br>- Session map: remove key=1<br>- Tổng tiền trừ đi 50000đ<br>- Badge giỏ hàng giảm |
| TC_CART_004 | Cập nhật số lượng tăng | - Giỏ có SP ID=1 (qty=1) | 1. Vào `/cart/view`<br>2. Sửa ô số lượng từ 1 → 5<br>3. Click "Cập nhật" hoặc Enter | **Product ID:** 1<br>**New Quantity:** 5 | - Method: `cartService.update(1, 5)`<br>- CartItem quantity = 5<br>- Tổng tiền = 50000 × 5 = 250000đ<br>- UI cập nhật realtime |
| TC_CART_005 | Cập nhật số lượng giảm | - Giỏ có SP ID=1 (qty=5) | 1. Sửa số lượng từ 5 → 2<br>2. Click "Cập nhật" | **Product ID:** 1<br>**New Quantity:** 2 | - Method: `cartService.update(1, 2)`<br>- Quantity = 2<br>- Tổng tiền = 50000 × 2 = 100000đ |
| TC_CART_006 | Cập nhật số lượng bằng 0 | - Giỏ có SP ID=1 | 1. Nhập số lượng = 0<br>2. Click "Cập nhật" | **Product ID:** 1<br>**New Quantity:** 0 | - Method: `cartService.update(1, 0)`<br>- Logic: if (qty == 0) → remove item<br>- SP tự động bị xóa khỏi giỏ<br>- Giống như TC_CART_003 |
| TC_CART_007 | Cập nhật số lượng âm | - Giỏ có SP | 1. Nhập số lượng = -1<br>2. Click "Cập nhật" | **Product ID:** 1<br>**New Quantity:** -1 | - **Hiện tại:** Không có validation<br>- **Nên có:** Validation error "Số lượng phải >= 1"<br>- Hoặc tự động reset về 1<br>- **Note:** Cần thêm validation |
| TC_CART_008 | Cập nhật số lượng vượt tồn kho | - Product ID=1 có quantity=5 trong DB<br>- Giỏ có SP ID=1 | 1. Nhập số lượng = 10<br>2. Click "Cập nhật" | **Product ID:** 1<br>**Stock:** 5<br>**Request Qty:** 10 | - **Hiện tại:** Cho phép cập nhật (check ở checkout)<br>- **Nên có:** Validation realtime<br>- Error: "Chỉ còn 5 sản phẩm trong kho"<br>- Không cho cập nhật > stock |
| TC_CART_009 | Xóa toàn bộ giỏ hàng | - Giỏ có 3 SP khác nhau | 1. Vào `/cart/view`<br>2. Click nút "Xóa tất cả" hoặc "Clear Cart"<br>3. Confirm | **Action:** Clear all | - Method: `cartService.clear()`<br>- Session map.clear()<br>- Giỏ hàng trống hoàn toàn<br>- Tổng tiền = 0đ<br>- Badge giỏ hàng = 0<br>- Hiển thị: "Giỏ hàng trống" |
| TC_CART_010 | Kiểm tra tổng tiền chính xác | - Giỏ có 2 SP:<br>&nbsp;&nbsp;• SP A: 10000đ × 2<br>&nbsp;&nbsp;• SP B: 20000đ × 1 | 1. Vào `/cart/view`<br>2. Kiểm tra tổng tiền hiển thị | **SP A:** 10000 × 2<br>**SP B:** 20000 × 1 | - Method: `cartService.getAmount()`<br>- Tính toán: (10000×2) + (20000×1) = 40000<br>- Hiển thị: "40.000đ"<br>- Format tiền tệ đúng (dấu chấm phân cách) |

---

### B. CHECKOUT PROCESS (13 TCs)

| ID | Tên Test Case | Điều kiện tiên quyết | Các bước thực hiện | Dữ liệu đầu vào | Kết quả mong đợi |
|---|---|---|---|---|---|
| TC_CHK_001 | Checkout thành công (Happy Path) | - Giỏ có 2 SP (tổng 100k)<br>- Đã login (user="user1")<br>- SP còn đủ hàng trong kho | 1. Vào `/checkout`<br>2. Nhập đầy đủ thông tin giao hàng<br>3. Chọn phương thức thanh toán<br>4. Check "Đồng ý điều khoản"<br>5. Click "Đặt hàng" | **Fullname:** Nguyễn Văn A<br>**Phone:** 0901234567<br>**Email:** user@gmail.com<br>**Address:** 123 Lê Lợi<br>**Ward:** Phường 1<br>**District:** Quận 1<br>**Province:** TP.HCM<br>**Payment:** COD | - Method: `orderService.checkout(request, user)`<br>- **Transaction bắt đầu**<br>- Tạo Order mới (status=0)<br>- Tạo OrderDetails từ cart items<br>- **Trừ tồn kho:** product.quantity -= orderQty<br>- Lưu Order + OrderDetails vào DB<br>- **Clear giỏ hàng:** cartService.clear()<br>- **Transaction commit**<br>- Redirect: `/checkout/success`<br>- Flash message: "Đặt hàng thành công! Mã đơn: #123" |
| TC_CHK_002 | Checkout khi chưa đăng nhập | - Giỏ có hàng<br>- Session không có user | 1. Vào `/checkout`<br>2. Hệ thống kiểm tra session | **Session user:** NULL | - Controller check: `if (user == null)`<br>- Redirect: `/auth/login?redirect=/checkout`<br>- Flash message: "Vui lòng đăng nhập để tiếp tục"<br>- Sau login thành công → redirect về `/checkout` |
| TC_CHK_003 | Checkout với giỏ hàng trống | - Giỏ hàng trống<br>- Đã login | 1. Truy cập trực tiếp URL `/checkout` | **Cart items:** Empty list | - Method: `cartService.getItems().isEmpty()`<br>- Redirect: `/cart/view`<br>- Flash message: "Giỏ hàng trống, không thể đặt hàng" |
| TC_CHK_004 | Checkout thiếu địa chỉ giao hàng | - Đang ở form checkout | 1. Để trống trường "Địa chỉ"<br>2. Nhập đầy đủ các trường khác<br>3. Click "Đặt hàng" | **Address:** [Trống]<br>**Phone:** 0901234567<br>**Email:** user@gmail.com | - **Validation:** `@NotBlank` annotation<br>- BindingResult.hasErrors() = true<br>- Không submit form<br>- Error message: "Vui lòng nhập địa chỉ giao hàng"<br>- Focus vào field address |
| TC_CHK_005 | Checkout thiếu số điện thoại | - Đang ở form checkout | 1. Để trống trường "SĐT"<br>2. Click "Đặt hàng" | **Phone:** [Trống] | - **Validation:** `@NotBlank`<br>- Error: "Vui lòng nhập số điện thoại"<br>- Không submit |
| TC_CHK_006 | Checkout với SĐT sai format | - Đang ở form checkout | 1. Nhập SĐT không hợp lệ<br>2. Click "Đặt hàng" | **Phone:** "abc123"<br>hoặc "123" | - **Validation:** `@Pattern` regex VN phone<br>- Error: "Số điện thoại không hợp lệ"<br>- Regex: `^(0|\+84)...` (10-11 số) |
| TC_CHK_007 | Checkout với email sai format | - Đang ở form checkout | 1. Nhập email sai<br>2. Click "Đặt hàng" | **Email:** "notanemail" | - **Validation:** `@Email`<br>- Error: "Email không hợp lệ"<br>- Không submit |
| TC_CHK_008 | Checkout khi sản phẩm hết hàng | - Giỏ có SP ID=1 (qty=5)<br>- DB: Product ID=1 quantity=3 | 1. Điền form checkout<br>2. Click "Đặt hàng" | **Cart:** SP ID=1, qty=5<br>**Stock:** 3 | - **Exception:** `InsufficientStockException`<br>- Message: "Sản phẩm [Tên SP] chỉ còn 3 trong kho"<br>- **Transaction rollback**<br>- Redirect về `/checkout`<br>- Flash error message<br>- Giỏ hàng KHÔNG bị xóa |
| TC_CHK_009 | Checkout với fullname chứa số | - Đang ở form checkout | 1. Nhập họ tên có số<br>2. Click "Đặt hàng" | **Fullname:** "Nguyen123" | - **Validation:** `@Pattern(regexp = "^[\\p{L}\\s]+$")`<br>- Error: "Họ tên chỉ chứa chữ cái"<br>- Không submit |
| TC_CHK_010 | Checkout không đồng ý điều khoản | - Đang ở form checkout | 1. Điền đầy đủ thông tin<br>2. KHÔNG check "Đồng ý điều khoản"<br>3. Click "Đặt hàng" | **agreeTerms:** false | - **Validation:** `@AssertTrue`<br>- Error: "Bạn phải đồng ý với điều khoản"<br>- Không submit |
| TC_CHK_011 | Test ghép địa chỉ đầy đủ | - Đang checkout | 1. Nhập đầy đủ địa chỉ<br>2. Submit form | **Address:** 123 Lê Lợi<br>**Ward:** Phường 1<br>**District:** Quận 1<br>**Province:** TP.HCM | - Method: `buildFullAddress(request)`<br>- Result: "123 Lê Lợi, Phường 1, Quận 1, TP.HCM"<br>- Lưu vào Order.address |
| TC_CHK_012 | Test tạo OrderDetail đúng | - Giỏ có SP ID=1 (price=50k, qty=2) | 1. Checkout thành công<br>2. Kiểm tra DB OrderDetail | **Cart Item:**<br>- productId=1<br>- price=50000<br>- qty=2 | - OrderDetail record:<br>&nbsp;&nbsp;• product_id = 1<br>&nbsp;&nbsp;• price = 50000 (giá tại thời điểm mua)<br>&nbsp;&nbsp;• quantity = 2<br>&nbsp;&nbsp;• order_id = [order_id mới]<br>- Không lấy giá hiện tại từ Product |
| TC_CHK_013 | Test transaction rollback khi lỗi | - Giỏ có 2 SP<br>- SP thứ 2 bị xóa khỏi DB (simulate error) | 1. Checkout<br>2. Lỗi xảy ra khi tạo OrderDetail thứ 2 | **Simulate:** Product not found | - **Exception:** ResourceNotFoundException<br>- **@Transactional rollback**<br>- Order KHÔNG được lưu vào DB<br>- OrderDetail KHÔNG được lưu<br>- Tồn kho KHÔNG bị trừ<br>- Giỏ hàng KHÔNG bị xóa<br>- User có thể thử lại |

---

### C. ORDER MANAGEMENT (5 TCs)

| ID | Tên Test Case | Điều kiện tiên quyết | Các bước thực hiện | Dữ liệu đầu vào | Kết quả mong đợi |
|---|---|---|---|---|---|
| TC_ORD_001 | User xem lịch sử đơn hàng | - User đã mua 3 đơn hàng<br>- Đã login | 1. Vào `/profile/orders`<br>2. Xem danh sách đơn hàng | **Username:** user1 | - Method: `orderRepository.findByAccountUsername("user1")`<br>- Hiển thị 3 đơn hàng<br>- Thông tin: Mã đơn, Ngày đặt, Tổng tiền, Trạng thái<br>- Sắp xếp: Mới nhất trước |
| TC_ORD_002 | User xem chi tiết đơn hàng | - Đơn hàng ID=101 tồn tại<br>- Thuộc về user đang login | 1. Click vào đơn hàng #101<br>2. Xem chi tiết | **Order ID:** 101 | - Hiển thị:<br>&nbsp;&nbsp;• Thông tin người nhận<br>&nbsp;&nbsp;• Địa chỉ giao hàng<br>&nbsp;&nbsp;• Danh sách sản phẩm (tên, giá, SL)<br>&nbsp;&nbsp;• Tổng tiền<br>&nbsp;&nbsp;• Trạng thái đơn<br>&nbsp;&nbsp;• Ngày đặt |
| TC_ORD_003 | Admin xem tất cả đơn hàng | - Đã login Admin<br>- Có 50 đơn hàng trong hệ thống | 1. Vào `/admin/orders`<br>2. Xem danh sách | **Role:** ADMIN | - Hiển thị tất cả đơn hàng (không filter user)<br>- Có phân trang (10 đơn/trang)<br>- Có filter theo trạng thái<br>- Có search theo mã đơn/tên KH |
| TC_ORD_004 | Admin cập nhật trạng thái đơn hàng | - Đơn ID=101 (status=0: Mới đặt) | 1. Vào chi tiết đơn #101<br>2. Chọn trạng thái "Đang giao"<br>3. Click "Cập nhật" | **Order ID:** 101<br>**New Status:** 2 (Shipping) | - Method: `orderService.updateStatus(101, 2)`<br>- DB: Order.status = 2<br>- Hiển thị badge "Đang giao"<br>- **Note:** Nếu có email service → gửi email thông báo |
| TC_ORD_005 | Admin hủy đơn hàng | - Đơn ID=101 (status=0 hoặc 1) | 1. Click nút "Hủy đơn"<br>2. Nhập lý do hủy<br>3. Confirm | **Order ID:** 101<br>**Reason:** Khách yêu cầu | - Method: `orderService.cancelOrder(101, reason)`<br>- DB: Order.status = 3 (Cancelled)<br>- **TODO:** Hoàn tồn kho (chưa implement)<br>- **Nên có:** product.quantity += orderDetail.quantity<br>- Gửi email thông báo khách hàng |

---

## 📊 THỐNG KÊ

- **Tổng số Test Cases:** 28
- **Shopping Cart:** 10 TCs
- **Checkout Process:** 13 TCs
- **Order Management:** 5 TCs
- **Priority High:** 16 TCs
- **Priority Medium:** 9 TCs
- **Priority Low:** 3 TCs

---

## 🎯 COVERAGE MỤC TIÊU

| Component | Method | Coverage |
|-----------|--------|----------|
| CartServiceImpl | add() | ✅ 100% |
| CartServiceImpl | remove() | ✅ 100% |
| CartServiceImpl | update() | ✅ 100% |
| CartServiceImpl | clear() | ✅ 100% |
| CartServiceImpl | getAmount() | ✅ 100% |
| CheckoutServiceImpl | processCheckout() | ✅ 100% |
| CheckoutServiceImpl | buildOrder() | ✅ 100% |
| CheckoutServiceImpl | buildOrderDetails() | ✅ 100% |
| OrderService | checkout() | ✅ 100% |
| CheckoutController | processCheckout() | ✅ 90% |

---

## 📝 GHI CHÚ

### Test Cases đã XÓA (không phù hợp):
- ~~TC_CHK_009: Áp dụng Voucher~~ → Chưa implement trong code
- ~~TC_CHK_010: Voucher sai/hết hạn~~ → Chưa implement
- ~~TC_ORD_002: User hủy đơn~~ → Chưa có logic hủy đơn

### Test Cases đã SỬA:
- TC_CHK_001: Thêm chi tiết về transaction và trừ tồn kho
- TC_CHK_008: Sửa theo InsufficientStockException
- TC_ORD_005: Đánh dấu TODO hoàn tồn kho

### Test Cases MỚI THÊM:
- TC_CHK_007: Validation email format
- TC_CHK_009: Validation fullname pattern
- TC_CHK_010: Validation agree terms
- TC_CHK_011: Test buildFullAddress()
- TC_CHK_012: Test tạo OrderDetail
- TC_CHK_013: Test transaction rollback

### TODO - Chức năng cần implement:
1. ⚠️ Validation số lượng âm trong cart (TC_CART_007)
2. ⚠️ Check tồn kho realtime khi update cart (TC_CART_008)
3. ⚠️ Logic hoàn tồn kho khi hủy đơn (TC_ORD_005)
4. ⚠️ Voucher system (đã có entity nhưng chưa implement)

---

**Người tạo:** Tuấn Khoa  
**Reviewer:** Kiro AI  
**Ngày cập nhật:** 2026-02-25  
**Version:** 2.0
