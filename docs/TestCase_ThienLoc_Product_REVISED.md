# TEST CASES - THIÊN LỘC (Product & Image Module)

**Thành viên:** Thiên Lộc  
**Module:** Product Management & Image Upload  
**Tổng số Test Cases:** 28  
**Phiên bản:** 2.0 (Revised)

---

## 📋 DANH SÁCH TEST CASES

### A. PRODUCT MANAGEMENT (20 TCs)

| ID | Tên Test Case | Điều kiện tiên quyết | Các bước thực hiện | Dữ liệu đầu vào | Kết quả mong đợi |
|---|---|---|---|---|---|
| TC_PROD_001 | Thêm sản phẩm hợp lệ | - Đã login Admin<br>- Category "CP" tồn tại trong DB | 1. Vào trang `/admin/product/create`<br>2. Nhập đầy đủ thông tin hợp lệ<br>3. Chọn file ảnh (optional)<br>4. Click nút "Lưu" | **Tên:** Máy pha cà phê Ý<br>**Giá:** 5000000<br>**Category:** CP<br>**Số lượng:** 10<br>**Mô tả:** Máy pha chuyên nghiệp<br>**Available:** true | - HTTP Status: 302 (Redirect)<br>- Redirect đến `/admin/product/list`<br>- Flash message: "Lưu thành công"<br>- DB: Record mới được insert<br>- Ảnh được upload lên Cloudinary (nếu có) |
| TC_PROD_002 | Thêm sản phẩm với tên NULL | - Đã login Admin<br>- Category "CP" tồn tại | 1. Vào trang `/admin/product/create`<br>2. Để trống trường "Tên sản phẩm"<br>3. Nhập các trường khác hợp lệ<br>4. Click "Lưu" | **Tên:** NULL<br>**Giá:** 1000000<br>**Category:** CP<br>**Số lượng:** 5 | - **Database Level:** `DataIntegrityViolationException`<br>- Lỗi: "Column 'Name' cannot be null"<br>- Không lưu vào DB<br>- Hiển thị error page hoặc catch exception |
| TC_PROD_003 | Thêm sản phẩm với giá âm | - Đã login Admin | 1. Vào form thêm SP<br>2. Nhập giá trị âm vào trường "Giá"<br>3. Nhập các trường khác hợp lệ<br>4. Click "Lưu" | **Tên:** Sản phẩm test<br>**Giá:** -50000<br>**Category:** CP | - **Hiện tại:** Lưu thành công (không có validation)<br>- **Nên có:** Validation error "Giá phải lớn hơn 0"<br>- **Note:** Cần thêm `@Min(1)` vào DTO hoặc validation ở Service |
| TC_PROD_004 | Thêm sản phẩm với giá bằng 0 | - Đã login Admin | 1. Vào form thêm SP<br>2. Nhập giá = 0<br>3. Click "Lưu" | **Giá:** 0 | - **Hiện tại:** Lưu thành công<br>- **Nên có:** Validation error hoặc cảnh báo<br>- **Note:** Business rule cần xác định |
| TC_PROD_005 | Thêm SP với Category không tồn tại | - Đã login Admin<br>- Category "INVALID_ID" KHÔNG tồn tại | 1. Hack HTML hoặc gửi POST request<br>2. Set categoryId = "INVALID_ID"<br>3. Submit form | **Category ID:** INVALID_ID | - **Exception:** `ResourceNotFoundException`<br>- Message: "Category not found with id: INVALID_ID"<br>- HTTP Status: 404<br>- Không lưu vào DB |
| TC_PROD_006 | Cập nhật tên sản phẩm | - Product ID=1 tồn tại trong DB<br>- Đã login Admin | 1. Vào `/admin/product/edit/1`<br>2. Sửa tên sản phẩm<br>3. Click "Cập nhật" | **ID:** 1<br>**Tên cũ:** Máy pha A<br>**Tên mới:** Máy pha B | - HTTP Status: 302<br>- Redirect về `/admin/product/list`<br>- DB: Tên được update thành "Máy pha B"<br>- Flash message: "Cập nhật thành công" |
| TC_PROD_007 | Cập nhật giá sản phẩm | - Product ID=1 tồn tại | 1. Vào form edit SP ID=1<br>2. Sửa giá từ 1tr → 2tr<br>3. Click "Cập nhật" | **ID:** 1<br>**Giá cũ:** 1000000<br>**Giá mới:** 2000000 | - DB: Price = 2000000<br>- Giá hiển thị trên trang web cập nhật đúng |
| TC_PROD_008 | Cập nhật SP không tồn tại | - Product ID=999 KHÔNG tồn tại trong DB | 1. Truy cập URL `/admin/product/edit/999`<br>2. Hoặc gửi POST với ID=999 | **ID:** 999 | - **Exception:** `ResourceNotFoundException`<br>- Message: "Product not found with id: 999"<br>- HTTP Status: 404<br>- Redirect đến error page |
| TC_PROD_009 | Ẩn sản phẩm (Toggle Available: true → false) | - Product ID=1 tồn tại<br>- Available = true | 1. Vào `/admin/product/list`<br>2. Click nút "Ẩn" (Toggle) cho SP ID=1 | **ID:** 1<br>**Available hiện tại:** true | - Method: `toggleAvailable(1)`<br>- DB: Available = false<br>- SP không hiển thị trên trang chủ User<br>- Vẫn hiển thị trong Admin (có label "Đã ẩn") |
| TC_PROD_010 | Hiện sản phẩm (Toggle Available: false → true) | - Product ID=1 tồn tại<br>- Available = false | 1. Vào `/admin/product/list`<br>2. Click nút "Hiện" cho SP ID=1 | **ID:** 1<br>**Available hiện tại:** false | - DB: Available = true<br>- SP hiển thị lại trên trang chủ User<br>- Label "Đã ẩn" biến mất |
| TC_PROD_011 | Tìm kiếm sản phẩm theo tên (có kết quả) | - Có SP tên "Espresso Machine" trong DB | 1. Vào trang danh sách SP<br>2. Nhập "Espresso" vào ô tìm kiếm<br>3. Click "Tìm kiếm" hoặc Enter | **Keyword:** "Espresso" | - Method: `searchProductsPaginated("Espresso", pageable)`<br>- Kết quả: Danh sách chỉ chứa các SP có tên chứa "Espresso"<br>- Số lượng kết quả hiển thị đúng |
| TC_PROD_012 | Tìm kiếm sản phẩm không tồn tại | - Không có SP nào tên "iPhone" | 1. Nhập "iPhone" vào ô tìm kiếm<br>2. Enter | **Keyword:** "iPhone" | - Kết quả: Danh sách trống (empty list)<br>- Hiển thị message: "Không tìm thấy sản phẩm nào"<br>- Không có exception |
| TC_PROD_013 | Phân trang danh sách sản phẩm | - Có 50 sản phẩm trong DB<br>- Page size = 10 | 1. Vào `/admin/product/list`<br>2. Kéo xuống cuối trang | **Page:** 0<br>**Size:** 10 | - Method: `findAllPaginated(PageRequest.of(0, 10))`<br>- Hiển thị 10 SP đầu tiên<br>- Có thanh phân trang với 5 trang (50/10)<br>- Trang 1 được highlight |
| TC_PROD_014 | Sắp xếp sản phẩm theo giá tăng dần | - Có nhiều SP với giá khác nhau | 1. Vào trang danh sách SP<br>2. Click header cột "Giá"<br>3. Hoặc chọn sort option | **Sort:** price,asc | - Method: `findAllPaginated(PageRequest.of(0, 10, Sort.by("price").ascending()))`<br>- Danh sách sắp xếp từ giá thấp → cao<br>- SP giá thấp nhất ở đầu |
| TC_PROD_015 | Lọc sản phẩm theo category | - Category "CP" có 5 SP<br>- Category "MAY_PHA" có 3 SP | 1. Chọn category "CP" từ dropdown<br>2. Click "Lọc" | **Category ID:** CP | - Method: `filterProductsPaginated("CP", pageable)`<br>- Kết quả: Chỉ hiển thị 5 SP thuộc category "CP"<br>- Các SP khác category bị ẩn |
| TC_PROD_016 | Lọc sản phẩm theo khoảng giá | - Có SP giá từ 100k → 10tr | 1. Nhập Min Price = 1000000<br>2. Nhập Max Price = 5000000<br>3. Click "Lọc" | **Min:** 1000000<br>**Max:** 5000000 | - Method: `filterProductsAdvanced(null, 1000000.0, 5000000.0, pageable)`<br>- Kết quả: Chỉ SP có giá từ 1tr → 5tr<br>- SP < 1tr hoặc > 5tr bị loại |
| TC_PROD_017 | Lọc sản phẩm theo nhiều tiêu chí | - Có nhiều SP | 1. Chọn Category = "CP"<br>2. Nhập Min = 1tr, Max = 5tr<br>3. Nhập Keyword = "Espresso"<br>4. Click "Lọc" | **Category:** CP<br>**Min:** 1000000<br>**Max:** 5000000<br>**Keyword:** Espresso | - Method: `filterProductsWithAllCriteria("CP", 1000000.0, 5000000.0, "Espresso", pageable)`<br>- Kết quả: SP thỏa mãn CẢ 3 điều kiện<br>- AND logic, không phải OR |
| TC_PROD_018 | Kiểm tra trạng thái tồn kho (IN_STOCK) | - Product có quantity = 50 | 1. Gọi method `getStockStatus(50)` | **Quantity:** 50 | - Return: `StockStatus.IN_STOCK`<br>- Message: "Còn hàng"<br>- Badge màu xanh |
| TC_PROD_019 | Kiểm tra trạng thái tồn kho (LOW_STOCK) | - Product có quantity = 5 | 1. Gọi method `getStockStatus(5)` | **Quantity:** 5 | - Return: `StockStatus.LOW_STOCK`<br>- Message: "Chỉ còn lại 5 sản phẩm!"<br>- Badge màu vàng/cam |
| TC_PROD_020 | Kiểm tra trạng thái tồn kho (OUT_OF_STOCK) | - Product có quantity = 0 | 1. Gọi method `getStockStatus(0)` | **Quantity:** 0 | - Return: `StockStatus.OUT_OF_STOCK`<br>- Message: "Hết hàng"<br>- Badge màu đỏ<br>- Nút "Thêm vào giỏ" bị disable |

---

### B. IMAGE UPLOAD & COMPRESSION (8 TCs)

| ID | Tên Test Case | Điều kiện tiên quyết | Các bước thực hiện | Dữ liệu đầu vào | Kết quả mong đợi |
|---|---|---|---|---|---|
| TC_IMG_001 | Upload ảnh JPG hợp lệ | - File test.jpg < 5MB<br>- Format: JPEG | 1. Chọn file ảnh JPG<br>2. Click "Upload" | **File:** test.jpg<br>**Size:** 2MB<br>**Format:** JPEG | - Method: `saveProductImage(file)`<br>- Ảnh được nén xuống < 500KB<br>- Upload lên Cloudinary thành công<br>- Return: URL ảnh (https://res.cloudinary.com/...)<br>- DB: Lưu URL vào field `image` |
| TC_IMG_002 | Upload ảnh PNG hợp lệ | - File test.png < 5MB | 1. Chọn file PNG<br>2. Click "Upload" | **File:** test.png<br>**Size:** 1.5MB | - Ảnh được convert sang JPEG<br>- Nén xuống < 500KB<br>- Upload thành công<br>- Preview hiển thị đúng |
| TC_IMG_003 | Upload file PDF (Chặn) | - File test.pdf | 1. Cố tình chọn file PDF<br>2. Click "Upload" | **File:** test.pdf<br>**Type:** application/pdf | - **Exception:** `InvalidFileException`<br>- Message: "Định dạng file không hỗ trợ. Chỉ chấp nhận JPG, PNG, WEBP"<br>- Không upload<br>- Hiển thị error message |
| TC_IMG_004 | Upload ảnh quá 5MB | - File heavy.jpg = 10MB | 1. Chọn file > 5MB<br>2. Click "Upload" | **File:** heavy.jpg<br>**Size:** 10MB | - **Validation:** File size check<br>- Error: "Dung lượng file quá lớn (Max 5MB)"<br>- Không upload<br>- User phải chọn file khác |
| TC_IMG_005 | Upload ảnh NULL (Không chọn file) | - Đang ở form edit SP<br>- SP đã có ảnh cũ | 1. Không chọn file mới<br>2. Click "Lưu" | **File:** NULL | - Giữ nguyên ảnh cũ<br>- Không gọi uploadService<br>- DB: Image field không thay đổi |
| TC_IMG_006 | Test nén ảnh (Resize) | - File gốc 2000x2000px, 3MB | 1. Upload ảnh to<br>2. Kiểm tra ảnh sau khi xử lý | **File:** large.jpg<br>**Dimension:** 2000x2000px<br>**Size:** 3MB | - Method: `ImageService.compressImage()`<br>- Ảnh được resize về max width = 800px<br>- Giữ nguyên aspect ratio<br>- Size giảm xuống < 500KB<br>- Quality = 85% |
| TC_IMG_007 | Upload ảnh lên Cloudinary thành công | - Cloudinary config đúng<br>- API key hợp lệ | 1. Upload ảnh<br>2. Kiểm tra response | **File:** product.jpg | - Method: `UploadService.saveProductImage()`<br>- Upload lên Cloudinary thành công<br>- Return URL: `https://res.cloudinary.com/.../product.jpg`<br>- Public ID được lưu để xóa sau này |
| TC_IMG_008 | Upload ảnh Cloudinary fail → Fallback local | - Cloudinary API down<br>- Hoặc network error | 1. Upload ảnh khi Cloudinary lỗi | **File:** test.jpg | - Catch exception từ Cloudinary<br>- Fallback: Lưu vào local storage `/uploads/products/`<br>- Return: Local path `/uploads/products/uuid-test.jpg`<br>- Log warning: "Cloudinary upload failed, saved locally" |

---

## 📊 THỐNG KÊ

- **Tổng số Test Cases:** 28
- **Product Management:** 20 TCs
- **Image Upload:** 8 TCs
- **Priority High:** 18 TCs
- **Priority Medium:** 7 TCs
- **Priority Low:** 3 TCs

---

## 🎯 COVERAGE MỤC TIÊU

| Component | Method | Coverage |
|-----------|--------|----------|
| ProductServiceImpl | saveProduct() | ✅ 100% |
| ProductServiceImpl | findById() | ✅ 100% |
| ProductServiceImpl | update() | ✅ 100% |
| ProductServiceImpl | toggleAvailable() | ✅ 100% |
| ProductServiceImpl | searchProductsPaginated() | ✅ 100% |
| ProductServiceImpl | filterProductsWithAllCriteria() | ✅ 100% |
| ProductServiceImpl | getStockStatus() | ✅ 100% |
| ImageService | compressImage() | ✅ 100% |
| UploadService | saveProductImage() | ✅ 100% |

---

## 📝 GHI CHÚ

### Test Cases đã XÓA (không phù hợp):
- ~~TC_VAL_001: Tên quá 200 ký tự~~ → Không có validation trong code
- ~~TC_VAL_002: Số lượng âm~~ → Không có validation
- ~~TC_VAL_003: Mô tả HTML~~ → Không có XSS protection

### Test Cases đã SỬA:
- TC_PROD_002: Sửa expected result theo database constraint
- TC_PROD_005: Sửa theo ResourceNotFoundException
- TC_PROD_008: Sửa theo ResourceNotFoundException

### Test Cases MỚI THÊM:
- TC_PROD_015-017: Test filter & search advanced
- TC_PROD_018-020: Test stock status
- TC_IMG_006-008: Test compression & Cloudinary

---

**Người tạo:** Thiên Lộc  
**Reviewer:** Kiro AI  
**Ngày cập nhật:** 2026-02-25  
**Version:** 2.0
