# TEST CASES - CHÍ BẢO (Category Module)

**Thành viên:** Chí Bảo  
**Module:** Category Management  
**Tổng số Test Cases:** 24  
**Phiên bản:** 2.0 (Revised)

---

## 📋 DANH SÁCH TEST CASES

### A. CATEGORY CRUD OPERATIONS (15 TCs)

| ID | Tên Test Case | Điều kiện tiên quyết | Các bước thực hiện | Dữ liệu đầu vào | Kết quả mong đợi |
|---|---|---|---|---|---|
| TC_CAT_001 | Thêm danh mục hợp lệ | - Đã login Admin<br>- Category ID "TEA" chưa tồn tại | 1. Vào `/admin/category/add`<br>2. Nhập ID và Tên<br>3. Chọn Icon (optional)<br>4. Click "Lưu" | **ID:** TEA<br>**Name:** Trà<br>**Icon:** (auto) | - Method: `categoryService.createCategory(request)`<br>- **Normalize:** ID uppercase → "TEA"<br>- DB: Insert record mới<br>- Icon tự động map từ `getCategoryIcons()`<br>- Redirect: `/admin/category/list`<br>- Flash message: "Thêm loại hàng thành công!" |
| TC_CAT_002 | Thêm danh mục trùng ID | - Category "CP" đã tồn tại trong DB | 1. Vào form thêm DM<br>2. Nhập ID = "CP"<br>3. Click "Lưu" | **ID:** CP<br>**Name:** Cà phê mới | - **Exception:** `DuplicateResourceException`<br>- Message: "Category with id 'CP' already exists"<br>- HTTP Status: 409 Conflict<br>- Không lưu vào DB<br>- Hiển thị error message |
| TC_CAT_003 | Thêm danh mục với tên trống | - Đang ở form thêm DM | 1. Nhập ID hợp lệ<br>2. Để trống trường "Tên"<br>3. Click "Lưu" | **ID:** TEST<br>**Name:** [Trống] | - **Validation:** `@NotBlank` annotation<br>- BindingResult.hasErrors() = true<br>- Error message: "Tên danh mục không được để trống"<br>- Không submit form<br>- Focus vào field name |
| TC_CAT_004 | Thêm danh mục với ID chứa ký tự đặc biệt | - Đang ở form thêm DM | 1. Nhập ID = "C@F#"<br>2. Nhập tên hợp lệ<br>3. Click "Lưu" | **ID:** C@F#<br>**Name:** Cà phê | - **Validation:** `@Pattern(regexp = "^[A-Z0-9_]+$")`<br>- Error: "ID chỉ chứa chữ HOA, số và dấu gạch dưới"<br>- Không submit<br>- **Note:** Frontend nên có input mask |
| TC_CAT_005 | Cập nhật tên danh mục | - Category "CP" tồn tại<br>- Tên hiện tại: "Cà phê" | 1. Vào `/admin/category/edit/CP`<br>2. Sửa tên thành "Cà phê máy"<br>3. Click "Lưu" | **ID:** CP (readonly)<br>**Name mới:** Cà phê máy | - Method: `categoryService.updateCategory("CP", request)`<br>- DB: Update name = "Cà phê máy"<br>- ID KHÔNG thay đổi (Primary Key)<br>- Flash message: "Cập nhật loại hàng thành công!" |
| TC_CAT_006 | Cố gắng cập nhật ID (Chặn) | - Category "CP" tồn tại<br>- Đang ở form edit | 1. Cố tình sửa ô ID (hack HTML)<br>2. Submit form | **ID cũ:** CP<br>**ID mới:** CP2 | - **Frontend:** Input ID bị `disabled` hoặc `readonly`<br>- **Backend:** ID không được update (PK không đổi)<br>- Nếu hack → ID vẫn giữ nguyên "CP"<br>- Chỉ update các field khác |
| TC_CAT_007 | Xóa danh mục rỗng (không có SP) | - Category "TEST" tồn tại<br>- Không có SP nào thuộc "TEST" | 1. Vào `/admin/category/list`<br>2. Click nút "Xóa" cho DM "TEST"<br>3. Confirm xóa | **ID:** TEST<br>**Product count:** 0 | - Method: `categoryService.deleteOrThrow("TEST")`<br>- Check: `countProductsByCategory("TEST")` = 0<br>- DB: DELETE FROM Categories WHERE id='TEST'<br>- Redirect về list<br>- Flash message: "Xóa loại hàng thành công!" |
| TC_CAT_008 | Xóa danh mục có sản phẩm (Chặn) | - Category "CP" có 5 SP | 1. Click nút "Xóa" cho DM "CP"<br>2. Confirm | **ID:** CP<br>**Product count:** 5 | - Method: `categoryService.deleteOrThrow("CP")`<br>- Check: `countProductsByCategory("CP")` = 5<br>- **Exception:** `BusinessRuleException`<br>- Message: "Không thể xóa loại hàng này vì còn 5 sản phẩm thuộc loại này!"<br>- KHÔNG xóa khỏi DB<br>- Hiển thị error message |
| TC_CAT_009 | Tìm kiếm danh mục theo tên | - Có DM "Trà" trong DB | 1. Vào `/admin/category/list`<br>2. Nhập "Trà" vào ô tìm kiếm<br>3. Enter | **Keyword:** Trà | - Filter danh sách theo tên<br>- Hiển thị các DM có tên chứa "Trà"<br>- **Note:** Nếu chưa có search → cần implement |
| TC_CAT_010 | Validation độ dài tên (quá dài) | - Đang ở form thêm DM | 1. Nhập tên > 100 ký tự<br>2. Click "Lưu" | **Name:** "A" × 101 ký tự | - **Validation:** `@Size(min = 3, max = 100)`<br>- Error: "Tên danh mục phải từ 3-100 ký tự"<br>- Không submit |
| TC_CAT_011 | Validation độ dài tên (quá ngắn) | - Đang ở form thêm DM | 1. Nhập tên < 3 ký tự<br>2. Click "Lưu" | **Name:** "AB" | - **Validation:** `@Size(min = 3, max = 100)`<br>- Error: "Tên danh mục phải từ 3-100 ký tự"<br>- Không submit |
| TC_CAT_012 | Validation độ dài ID (quá dài) | - Đang ở form thêm DM | 1. Nhập ID > 10 ký tự<br>2. Click "Lưu" | **ID:** "ABCDEFGHIJK" (11 ký tự) | - **Validation:** `@Size(max = 10)`<br>- Error: "Mã danh mục tối đa 10 ký tự"<br>- Không submit |
| TC_CAT_013 | Test normalize ID (chữ thường → HOA) | - Đang ở form thêm DM | 1. Nhập ID = "tea" (chữ thường)<br>2. Click "Lưu" | **ID input:** tea<br>**Name:** Trà | - Method: `CategoryRequest.normalize()`<br>- ID tự động uppercase: "tea" → "TEA"<br>- DB: Lưu với id = "TEA"<br>- **Note:** Test ở Service layer |
| TC_CAT_014 | Test normalize tên (trim spaces) | - Đang ở form thêm DM | 1. Nhập tên có khoảng trắng đầu/cuối<br>2. Click "Lưu" | **Name input:** "&nbsp;&nbsp;Trà&nbsp;&nbsp;" | - Method: `CategoryRequest.normalize()`<br>- Name.trim(): "  Trà  " → "Trà"<br>- DB: Lưu "Trà" (không có space) |
| TC_CAT_015 | Reset form thêm danh mục | - Đang nhập dở form | 1. Nhập một số trường<br>2. Click nút "Reset" hoặc "Làm mới" | **Partial data** | - Tất cả input field trở về trống<br>- Không submit<br>- **Note:** Nếu chưa có nút Reset → skip TC này |

---

### B. ICON MANAGEMENT (3 TCs)

| ID | Tên Test Case | Điều kiện tiên quyết | Các bước thực hiện | Dữ liệu đầu vào | Kết quả mong đợi |
|---|---|---|---|---|---|
| TC_CAT_016 | Kiểm tra Icon mặc định khi không chọn | - Thêm DM mới<br>- Không chọn icon | 1. Nhập ID = "MAY_PHA"<br>2. Không chọn icon<br>3. Click "Lưu" | **ID:** MAY_PHA<br>**Icon:** NULL | - Method: `getCategoryIcons()`<br>- Icon tự động map: "MAY_PHA" → "May_Pha_Ca_Phe.webp"<br>- DB: icon = "May_Pha_Ca_Phe.webp"<br>- **Note:** Icon được hardcode trong map |
| TC_CAT_017 | Chọn Icon từ danh sách có sẵn | - Đang ở form thêm DM<br>- Có dropdown icon | 1. Chọn icon từ dropdown<br>2. Click "Lưu" | **Icon selected:** "dung_cu_pha_che.webp" | - DB: Lưu tên file icon<br>- Hiển thị icon trên menu trang chủ<br>- **Note:** Không upload file, chỉ chọn từ list có sẵn |
| TC_CAT_018 | Cập nhật Icon cho danh mục | - Category "CP" đang có icon cũ | 1. Vào form edit<br>2. Chọn icon mới<br>3. Click "Lưu" | **ID:** CP<br>**Icon cũ:** ca_phe_do_an.webp<br>**Icon mới:** May_Pha_Ca_Phe.webp | - DB: Update icon = "May_Pha_Ca_Phe.webp"<br>- Menu trang chủ hiển thị icon mới |

---

### C. BUSINESS LOGIC & INTEGRATION (6 TCs)

| ID | Tên Test Case | Điều kiện tiên quyết | Các bước thực hiện | Dữ liệu đầu vào | Kết quả mong đợi |
|---|---|---|---|---|---|
| TC_CAT_019 | Hiển thị danh mục trên Menu User | - Có 3 DM active trong DB:<br>&nbsp;&nbsp;• CP (Cà phê)<br>&nbsp;&nbsp;• TEA (Trà)<br>&nbsp;&nbsp;• MAY_PHA (Máy pha) | 1. Vào trang chủ User<br>2. Kiểm tra menu navigation | **Categories:** 3 active | - Method: `categoryService.findAll()`<br>- Menu hiển thị đủ 3 danh mục<br>- Mỗi DM có icon tương ứng<br>- Click vào DM → filter SP theo category |
| TC_CAT_020 | Đếm số sản phẩm theo danh mục | - Category "CP" có 5 SP<br>- Category "TEA" có 2 SP | 1. Vào `/admin/category/list`<br>2. Xem cột "Số lượng SP" | **Category IDs:** CP, TEA | - Method: `countProductsByCategory("CP")`<br>- Hiển thị:<br>&nbsp;&nbsp;• CP: 5 sản phẩm<br>&nbsp;&nbsp;• TEA: 2 sản phẩm<br>- Số liệu chính xác từ DB |
| TC_CAT_021 | Click danh mục → Lọc sản phẩm | - Trang chủ User<br>- Category "CP" có 5 SP | 1. Click menu "Cà phê"<br>2. Xem danh sách SP | **Category ID:** CP | - Redirect: `/products?category=CP`<br>- Method: `productService.findByCategoryId("CP")`<br>- Hiển thị chỉ 5 SP thuộc "Cà phê"<br>- Các SP khác category bị ẩn |
| TC_CAT_022 | Test findByIdOrThrow() - Category tồn tại | - Category "CP" tồn tại | 1. Gọi `categoryService.findByIdOrThrow("CP")` | **ID:** CP | - Return: Category object<br>- Không throw exception<br>- Object chứa đầy đủ thông tin |
| TC_CAT_023 | Test findByIdOrThrow() - Category không tồn tại | - Category "INVALID" KHÔNG tồn tại | 1. Gọi `categoryService.findByIdOrThrow("INVALID")` | **ID:** INVALID | - **Exception:** `ResourceNotFoundException`<br>- Message: "Category not found with id: INVALID"<br>- HTTP Status: 404 |
| TC_CAT_024 | Test getCategoryIcons() - Map đầy đủ | - Service layer | 1. Gọi `categoryService.getCategoryIcons()` | N/A | - Return: Map<String, String><br>- Chứa 6 entries:<br>&nbsp;&nbsp;• CF_AN_VAT → ca_phe_do_an.webp<br>&nbsp;&nbsp;• DUNG_CU → dung_cu_pha_che.webp<br>&nbsp;&nbsp;• HANG_CU → may_pha_may_xay_cu.webp<br>&nbsp;&nbsp;• MAY_PHA → May_Pha_Ca_Phe.webp<br>&nbsp;&nbsp;• MAY_XAY → May_Xay_Ca_Phe.webp<br>&nbsp;&nbsp;• XAY_EP → may_xay_sinh_to_may_ep.webp |

---

## 📊 THỐNG KÊ

- **Tổng số Test Cases:** 24
- **CRUD Operations:** 15 TCs
- **Icon Management:** 3 TCs
- **Business Logic:** 6 TCs
- **Priority High:** 12 TCs
- **Priority Medium:** 9 TCs
- **Priority Low:** 3 TCs

---

## 🎯 COVERAGE MỤC TIÊU

| Component | Method | Coverage |
|-----------|--------|----------|
| CategoryServiceImpl | findAll() | ✅ 100% |
| CategoryServiceImpl | findById() | ✅ 100% |
| CategoryServiceImpl | createCategory() | ✅ 100% |
| CategoryServiceImpl | updateCategory() | ✅ 100% |
| CategoryServiceImpl | deleteOrThrow() | ✅ 100% |
| CategoryServiceImpl | findByIdOrThrow() | ✅ 100% |
| CategoryServiceImpl | countProductsByCategory() | ✅ 100% |
| CategoryServiceImpl | getCategoryIcons() | ✅ 100% |
| CategoryRequest | normalize() | ✅ 100% |
| AdminCategoryController | saveCategory() | ✅ 90% |

---

## 📝 GHI CHÚ

### Test Cases đã XÓA (không phù hợp):
- ~~TC_CAT_019: Xóa nhiều danh mục~~ → Code không có chức năng bulk delete
- ~~TC_CAT_020: Reset form~~ → Không có nút Reset trong form

### Test Cases đã SỬA:
- TC_CAT_004: Sửa theo @Pattern validation
- TC_CAT_006: Làm rõ ID là Primary Key, không cho sửa
- TC_CAT_011-012: Thêm validation độ dài tên và ID

### Test Cases MỚI THÊM:
- TC_CAT_013-014: Test normalize() method
- TC_CAT_016-018: Test icon management
- TC_CAT_019-024: Test business logic & integration

### Lưu ý quan trọng:

1. **ID là Primary Key:**
   - Không cho phép sửa sau khi tạo
   - Form edit nên disable input ID
   - Backend không update ID field

2. **Icon Management:**
   - Icon được map cứng trong code (không upload file)
   - Có 6 icon mặc định cho 6 category
   - Nếu không chọn → tự động map theo ID

3. **Business Rule:**
   - Không xóa category có sản phẩm
   - Phải xóa hết SP trước khi xóa category
   - Hoặc chuyển SP sang category khác

4. **Validation:**
   - ID: Chữ HOA, số, dấu gạch dưới, max 10 ký tự
   - Name: 3-100 ký tự, không trống
   - Tự động normalize (uppercase ID, trim name)

---

## 🔗 LIÊN KẾT VỚI MODULE KHÁC

### Với Product Module (Thiên Lộc):
- Product.category → Category (ManyToOne)
- Khi xóa Category → check Product count
- Filter product by category

### Với Menu Navigation:
- Hiển thị danh mục trên trang chủ
- Click category → filter products
- Icon hiển thị bên cạnh tên category

---

**Người tạo:** Chí Bảo  
**Reviewer:** Kiro AI  
**Ngày cập nhật:** 2026-02-25  
**Version:** 2.0
