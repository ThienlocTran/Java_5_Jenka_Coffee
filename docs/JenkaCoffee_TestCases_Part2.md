# Jenka Coffee — Test Case Document (Part 2: PRODUCT + CATEGORY MODULE)
**Source mapped:** `ApiProductController`, `ProductServiceImpl`, `ApiCategoryController`, `CategoryServiceImpl`

---

## MODULE 2: PRODUCT CONTROLLER + SERVICE

---

### TC-PRD-001 — GET /api/products?keyword quá 100 ký tự → 400

| Field | Value |
|-------|-------|
| **TC ID** | TC-PRD-001 |
| **Title** | GET /api/products — keyword > 100 ký tự phải bị reject trước khi query DB |
| **Method mapped** | `ApiProductController.getProducts()` line 44-47 |
| **Pre-conditions** | Không cần DB |
| **Test data** | `GET /api/products?keyword=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA` (101 chars) |
| **Expected result** | HTTP 400, `status="ERROR"`, `message="Từ khóa tìm kiếm quá dài (tối đa 100 ký tự)"` |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |
| **Notes** | Security — ngăn heavy LIKE query DoS trên DB |

```java
@Test
void getProducts_keywordTooLong_returns400() throws Exception {
    String longKeyword = "A".repeat(101);
    mockMvc.perform(get("/api/products").param("keyword", longKeyword))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message")
            .value("Từ khóa tìm kiếm quá dài (tối đa 100 ký tự)"));
}
```

---

### TC-PRD-002 — GET /api/products?page=1001 → Deep Pagination DoS

| Field | Value |
|-------|-------|
| **TC ID** | TC-PRD-002 |
| **Title** | GET /api/products?page=1001 — page > 1000 phải bị reject |
| **Method mapped** | `ApiProductController.getProducts()` line 56-59 |
| **Pre-conditions** | Không cần DB |
| **Test data** | `GET /api/products?page=1001` |
| **Expected result** | HTTP 400, `message="Số trang vượt quá giới hạn (tối đa 1000)"` |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |

```java
@Test
void getProducts_pageExceedsLimit_returns400() throws Exception {
    mockMvc.perform(get("/api/products").param("page", "1001"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Số trang vượt quá giới hạn (tối đa 1000)"));
}
```

---

### TC-PRD-003 — GET /api/products?size=9999 — size bị cap ở 50

| Field | Value |
|-------|-------|
| **TC ID** | TC-PRD-003 |
| **Title** | GET /api/products?size=9999 — size phải bị giới hạn tối đa 50 |
| **Method mapped** | `ApiProductController.getProducts()` line 62 — `Math.min(Math.max(size, 1), 50)` |
| **Pre-conditions** | DB có sản phẩm |
| **Test data** | `GET /api/products?size=9999` |
| **Expected result** | HTTP 200, `data.items.length <= 50` |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |
| **Notes** | Boundary: size=0 phải được normalize thành 1. size=-1 cũng thành 1. |

---

### TC-PRD-004 — GET /api/products?size=0 — normalize thành 1

| Field | Value |
|-------|-------|
| **TC ID** | TC-PRD-004 |
| **Title** | GET /api/products?size=0 — size=0 phải được normalize thành 1 (Math.max) |
| **Method mapped** | `ApiProductController.getProducts()` line 62 |
| **Pre-conditions** | DB có sản phẩm |
| **Test data** | `GET /api/products?size=0` |
| **Expected result** | HTTP 200, pageable được tạo với pageSize=1 (không throw ArithmeticException) |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |
| **Notes** | Edge case — pageSize=0 gây lỗi trong JPA Pageable |

---

### TC-PRD-005 — GET /api/products/{id} — product không tồn tại → 404

| Field | Value |
|-------|-------|
| **TC ID** | TC-PRD-005 |
| **Title** | GET /api/products/{id} — productId không tồn tại phải trả 404 với message rõ ràng |
| **Method mapped** | `ApiProductController.getProductDetail()` line 93-94 |
| **Pre-conditions** | Product ID 99999 không tồn tại |
| **Test data** | `GET /api/products/99999` |
| **Expected result** | HTTP 404, `status="ERROR"`, `message="Không tìm thấy sản phẩm với ID: 99999"` |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |

```java
@Test
void getProductDetail_notFound_returns404() throws Exception {
    when(productService.getProductDetail(99999))
        .thenThrow(new ResourceNotFoundException("Product not found with id: 99999"));
    
    mockMvc.perform(get("/api/products/99999"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Không tìm thấy sản phẩm với ID: 99999"));
}
```

---

### TC-PRD-006 — GET /api/products/slug/{slug} — slug không tồn tại → 404

| Field | Value |
|-------|-------|
| **TC ID** | TC-PRD-006 |
| **Title** | GET /api/products/slug/{slug} — slug không tồn tại trả 404 |
| **Method mapped** | `ApiProductController.getProductDetailBySlug()` line 115-116 |
| **Pre-conditions** | Slug "may-pha-khong-ton-tai" không có trong DB |
| **Test data** | `GET /api/products/slug/may-pha-khong-ton-tai` |
| **Expected result** | HTTP 404, `message="Không tìm thấy sản phẩm với slug: may-pha-khong-ton-tai"` |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |

---

### TC-PRD-007 — GET /api/products — filter theo categoryId và price range

| Field | Value |
|-------|-------|
| **TC ID** | TC-PRD-007 |
| **Title** | GET /api/products — filter đồng thời categoryId + minPrice + maxPrice trả đúng kết quả |
| **Method mapped** | `ApiProductController.getProducts()` → `ProductServiceImpl.filterProductsWithAllCriteria()` |
| **Pre-conditions** | DB có 10 sản phẩm: 5 thuộc "MAY_PHA" với price 2M-8M, 5 thuộc "MAY_XAY" |
| **Test data** | `GET /api/products?categoryId=MAY_PHA&minPrice=3000000&maxPrice=6000000` |
| **Expected result** | HTTP 200, `data.items` chỉ chứa sản phẩm MAY_PHA có price trong 3M-6M |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |

```java
@Test
void getProducts_filterByCategoryAndPrice_returnsCorrectItems() throws Exception {
    BigDecimal min = new BigDecimal("3000000");
    BigDecimal max = new BigDecimal("6000000");
    Page<Product> mockPage = new PageImpl<>(List.of(buildProduct(1, new BigDecimal("5500000"), true)));
    
    when(productService.filterProductsWithAllCriteria(
            eq("MAY_PHA"), eq(min), eq(max), isNull(), any(Pageable.class)))
        .thenReturn(mockPage);
    
    mockMvc.perform(get("/api/products")
            .param("categoryId", "MAY_PHA")
            .param("minPrice", "3000000")
            .param("maxPrice", "6000000"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items").isArray())
        .andExpect(jsonPath("$.data.items.length()").value(1));
}
```

---

### TC-PRD-008 — ProductService: createProductFromRequest — price âm → exception

| Field | Value |
|-------|-------|
| **TC ID** | TC-PRD-008 |
| **Title** | createProductFromRequest — price < 0 phải throw BusinessRuleException |
| **Method mapped** | `ProductServiceImpl.createProductFromRequest()` line 484-486 |
| **Pre-conditions** | Category "MAY_PHA" tồn tại |
| **Test data** | `ProductRequest {name:"Test", price:-1000}`, categoryId="MAY_PHA" |
| **Expected result** | `BusinessRuleException("Giá sản phẩm không thể âm")` |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |

```java
@Test
void createProductFromRequest_negativePrice_throwsBusinessRuleException() {
    ProductRequest req = new ProductRequest();
    req.setName("Test product");
    req.setPrice(new BigDecimal("-1000"));
    
    Category cat = new Category(); cat.setId("MAY_PHA");
    when(categoryRepository.findById("MAY_PHA")).thenReturn(Optional.of(cat));
    
    assertThrows(BusinessRuleException.class,
        () -> productService.createProductFromRequest(req, "MAY_PHA", null));
}
```

---

### TC-PRD-009 — ProductService: createProductFromRequest — categoryId không tồn tại

| Field | Value |
|-------|-------|
| **TC ID** | TC-PRD-009 |
| **Title** | createProductFromRequest — categoryId không tồn tại trong DB phải throw ResourceNotFoundException |
| **Method mapped** | `ProductServiceImpl.createProductFromRequest()` line 489-490 |
| **Pre-conditions** | Category "KHONG_CO" không tồn tại |
| **Test data** | `ProductRequest {name:"Test", price:1000000}`, categoryId="KHONG_CO" |
| **Expected result** | `ResourceNotFoundException("Không tìm thấy danh mục với ID: KHONG_CO")` |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |

---

### TC-PRD-010 — ProductService: deleteProductWithValidation — product đã có orders

| Field | Value |
|-------|-------|
| **TC ID** | TC-PRD-010 |
| **Title** | deleteProductWithValidation — sản phẩm đã có đơn hàng không được xóa |
| **Method mapped** | `ProductServiceImpl.deleteProductWithValidation()` line 580-586 |
| **Pre-conditions** | Product ID=7 đã được dùng trong 3 đơn hàng |
| **Test data** | `deleteProductWithValidation(7)` |
| **Expected result** | `BusinessRuleException` với message chứa `"Không thể xóa sản phẩm này vì đã có 3 đơn hàng sử dụng"` |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |
| **Notes** | Business rule — tránh mất lịch sử đơn hàng |

```java
@Test
void deleteProductWithValidation_productHasOrders_throwsBusinessRuleException() {
    when(productRepository.existsById(7)).thenReturn(true);
    when(productRepository.countOrdersByProductId(7)).thenReturn(3L);
    
    BusinessRuleException ex = assertThrows(BusinessRuleException.class,
        () -> productService.deleteProductWithValidation(7));
    assertTrue(ex.getMessage().contains("3 đơn hàng sử dụng"));
}
```

---

### TC-PRD-011 — ProductService: toggleAvailable — available true → false

| Field | Value |
|-------|-------|
| **TC ID** | TC-PRD-011 |
| **Title** | toggleAvailable — sản phẩm available=true sau toggle phải thành available=false |
| **Method mapped** | `ProductServiceImpl.toggleAvailable()` line 355-365 |
| **Pre-conditions** | Product ID=10 có available=true |
| **Test data** | `toggleAvailable(10)` |
| **Expected result** | `productRepository.save()` được gọi với product.available = **false** |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |

```java
@Test
void toggleAvailable_trueToFalse_savesWithFalse() {
    Product product = new Product(); product.setId(10); product.setAvailable(true);
    when(productRepository.findById(10)).thenReturn(Optional.of(product));
    
    productService.toggleAvailable(10);
    
    verify(productRepository).save(argThat(p -> !p.getAvailable()));
}
```

---

### TC-PRD-012 — ProductService: generateUniqueSlug — slug đã tồn tại thì append counter

| Field | Value |
|-------|-------|
| **TC ID** | TC-PRD-012 |
| **Title** | generateUniqueSlug — khi "may-pha-ca-phe" đã tồn tại, slug mới phải là "may-pha-ca-phe-1" |
| **Method mapped** | `ProductServiceImpl.generateUniqueSlug()` line 401-419 |
| **Pre-conditions** | DB đã có slug "may-pha-ca-phe" |
| **Test data** | `productName="Máy Pha Cà Phê"` |
| **Expected result** | Slug trả về = `"may-pha-ca-phe-1"` (hoặc counter tiếp theo chưa tồn tại) |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |

---

### TC-PRD-013 — ProductService: price được round HALF_UP về 0 decimal

| Field | Value |
|-------|-------|
| **TC ID** | TC-PRD-013 |
| **Title** | buildProductFromRequest — price=5499999.6 phải được round thành 5,500,000 |
| **Method mapped** | `ProductServiceImpl.buildProductFromRequest()` line 522 — `price.setScale(0, RoundingMode.HALF_UP)` |
| **Pre-conditions** | N/A (unit test) |
| **Test data** | `price = new BigDecimal("5499999.6")` |
| **Expected result** | `product.getPrice() = new BigDecimal("5500000")` |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |

---

## MODULE 3: CATEGORY CONTROLLER + SERVICE

---

### TC-CAT-001 — GET /api/categories — trả danh sách đầy đủ

| Field | Value |
|-------|-------|
| **TC ID** | TC-CAT-001 |
| **Title** | GET /api/categories — trả về tất cả categories với status 200 |
| **Method mapped** | `ApiCategoryController.getAllCategories()` → `CategoryServiceImpl.findAll()` |
| **Pre-conditions** | DB có 6 categories |
| **Test data** | `GET /api/categories` |
| **Expected result** | HTTP 200, `status="SUCCESS"`, `data` là array 6 phần tử, mỗi phần tử có `id`, `name`, `icon` |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |

```java
@Test
void getAllCategories_returns200WithList() throws Exception {
    List<Category> cats = List.of(
        buildCategory("MAY_PHA", "Máy Pha Cà Phê", "May_Pha_Ca_Phe.webp"),
        buildCategory("MAY_XAY", "Máy Xay", "May_Xay_Ca_Phe.webp")
    );
    when(categoryService.findAll()).thenReturn(cats);
    
    mockMvc.perform(get("/api/categories"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUCCESS"))
        .andExpect(jsonPath("$.data.length()").value(2))
        .andExpect(jsonPath("$.data[0].id").value("MAY_PHA"));
}
```

---

### TC-CAT-002 — CategoryService: deleteOrThrow — category có sản phẩm → block xóa

| Field | Value |
|-------|-------|
| **TC ID** | TC-CAT-002 |
| **Title** | deleteOrThrow — category đang có 5 sản phẩm không được xóa |
| **Method mapped** | `CategoryServiceImpl.deleteOrThrow()` line 101-111 |
| **Pre-conditions** | Category "MAY_PHA" có 5 sản phẩm |
| **Test data** | `deleteOrThrow("MAY_PHA")` |
| **Expected result** | `BusinessRuleException` với message `"Không thể xóa loại hàng này vì còn 5 sản phẩm thuộc loại này!"` |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |

```java
@Test
void deleteOrThrow_categoryHasProducts_throwsBusinessRuleException() {
    Category cat = buildCategory("MAY_PHA", "Máy Pha", null);
    when(categoryRepository.findById("MAY_PHA")).thenReturn(Optional.of(cat));
    when(productRepository.countByCategoryId("MAY_PHA")).thenReturn(5L);
    
    BusinessRuleException ex = assertThrows(BusinessRuleException.class,
        () -> categoryService.deleteOrThrow("MAY_PHA"));
    assertTrue(ex.getMessage().contains("5 sản phẩm"));
}
```

---

### TC-CAT-003 — CategoryService: deleteOrThrow — category không tồn tại → 400

| Field | Value |
|-------|-------|
| **TC ID** | TC-CAT-003 |
| **Title** | deleteOrThrow — category ID không tồn tại phải throw ResourceNotFoundException |
| **Method mapped** | `CategoryServiceImpl.deleteOrThrow()` → `findByIdOrThrow()` line 93-97 |
| **Pre-conditions** | Category "KHONG_CO" không tồn tại |
| **Test data** | `deleteOrThrow("KHONG_CO")` |
| **Expected result** | `ResourceNotFoundException("Category", "id", "KHONG_CO")` |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |

---

### TC-CAT-004 — CategoryService: createCategory — duplicate ID → exception

| Field | Value |
|-------|-------|
| **TC ID** | TC-CAT-004 |
| **Title** | createCategory — ID đã tồn tại phải throw DuplicateResourceException |
| **Method mapped** | `CategoryServiceImpl.createCategory()` line 121-124 |
| **Pre-conditions** | Category "MAY_PHA" đã tồn tại trong DB |
| **Test data** | `CategoryRequest {id:"MAY_PHA", name:"Máy Pha Cà Phê"}` |
| **Expected result** | `DuplicateResourceException("Category", "id", "MAY_PHA")` |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |

```java
@Test
void createCategory_duplicateId_throwsDuplicateResourceException() {
    when(categoryRepository.existsById("MAY_PHA")).thenReturn(true);
    CategoryRequest req = new CategoryRequest("MAY_PHA", "Máy Pha Cà Phê", null);
    
    assertThrows(DuplicateResourceException.class,
        () -> categoryService.createCategory(req));
}
```

---

### TC-CAT-005 — CategoryService: createCategory — ID lowercase phải được normalize thành UPPERCASE

| Field | Value |
|-------|-------|
| **TC ID** | TC-CAT-005 |
| **Title** | createCategory — ID nhập vào "may_pha" phải được normalize thành "MAY_PHA" |
| **Method mapped** | `CategoryServiceImpl.createCategory()` line 118 → `CategoryRequest.normalize()` |
| **Pre-conditions** | Category "MAY_PHA" chưa tồn tại |
| **Test data** | `CategoryRequest {id:" may_pha ", name:" Máy Pha "}` (có khoảng trắng thừa + lowercase) |
| **Expected result** | Category được tạo với `id="MAY_PHA"`, `name="Máy Pha"` (trim) |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |

---

### TC-CAT-006 — CategoryService: createCategory — icon tự động assign nếu không truyền

| Field | Value |
|-------|-------|
| **TC ID** | TC-CAT-006 |
| **Title** | createCategory — khi icon=null, hệ thống tự gán icon từ predefined map |
| **Method mapped** | `CategoryServiceImpl.createCategory()` line 129-132 |
| **Pre-conditions** | Category "MAY_PHA" chưa tồn tại |
| **Test data** | `CategoryRequest {id:"MAY_PHA", name:"Máy Pha", icon:null}` |
| **Expected result** | Category được tạo với `icon="May_Pha_Ca_Phe.webp"` (từ `getCategoryIcons()`) |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |

```java
@Test
void createCategory_noIcon_autoAssignsFromPredefinedMap() {
    when(categoryRepository.existsById("MAY_PHA")).thenReturn(false);
    when(categoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    
    CategoryRequest req = new CategoryRequest("MAY_PHA", "Máy Pha Cà Phê", null);
    Category saved = categoryService.createCategory(req);
    
    assertEquals("May_Pha_Ca_Phe.webp", saved.getIcon());
}
```

---

### TC-CAT-007 — CategoryService: createCategory — icon ID không có trong map thì icon=null

| Field | Value |
|-------|-------|
| **TC ID** | TC-CAT-007 |
| **Title** | createCategory — ID không có trong predefined icon map thì icon phải là null (không crash) |
| **Method mapped** | `CategoryServiceImpl.createCategory()` line 131 — `icons.get(category.getId())` trả null |
| **Pre-conditions** | Category "CUSTOM_CAT" chưa tồn tại, không có trong getCategoryIcons() |
| **Test data** | `CategoryRequest {id:"CUSTOM_CAT", name:"Danh mục tùy chỉnh", icon:null}` |
| **Expected result** | Category được tạo thành công với `icon=null`, không throw exception |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |
| **Notes** | Edge case — getCategoryIcons().get("CUSTOM_CAT") = null, không được NPE |

---

### TC-CAT-008 — CategoryRequest validation: ID không hợp lệ (chứa ký tự thường)

| Field | Value |
|-------|-------|
| **TC ID** | TC-CAT-008 |
| **Title** | CategoryRequest — ID chứa ký tự lowercase phải fail @Pattern trước khi vào service |
| **Method mapped** | `CategoryRequest.id` — `@Pattern(regexp = "^[A-Z0-9_]+$")` |
| **Pre-conditions** | N/A |
| **Test data** | `CategoryRequest {id:"may_pha_nho", name:"Test"}` (có chữ thường) |
| **Expected result** | Bean Validation fail — `@Pattern` violation |
| **Actual result** | _(để trống)_ |
| **Status** | _(Passed/Failed)_ |
| **Notes** | Nhưng lưu ý: `normalize()` gọi `id.toUpperCase()` TRƯỚC khi validation chạy — cần test luồng đầy đủ qua Controller |
