# 📁 Cấu trúc Admin Assets

## 📂 Tổ chức file

```
static/
├── css/
│   ├── style.css          # CSS cho trang Site (User)
│   └── admin.css          # CSS cho trang Admin ⭐
├── js/
│   ├── cart.js            # JS cho giỏ hàng (Site)
│   └── admin.js           # JS cho Admin Panel ⭐
└── images/
    ├── banner/
    ├── brand/
    ├── icon/
    └── product/
```

## 🎨 admin.css - CSS cho Admin Panel

### Nội dung:
- ✅ Sidebar styling (menu, active states, hover effects)
- ✅ Navbar styling (search, notifications, user dropdown)
- ✅ Main content area
- ✅ Page header & breadcrumb
- ✅ Admin cards
- ✅ Delete modal styling
- ✅ Responsive design (mobile, tablet, desktop)
- ✅ Sidebar collapsed state

### Sử dụng:
File được tự động load trong `admin-layout.html`:
```html
<link rel="stylesheet" th:href="@{/css/admin.css}">
```

### Tùy chỉnh:
Chỉnh sửa biến CSS trong `:root`:
```css
:root {
    --sidebar-width: 260px;
    --navbar-height: 70px;
    --primary-color: #2c1a16;
    --accent-color: #D32F2F;
    --sidebar-bg: #1a0f0d;
    --sidebar-hover: rgba(255, 255, 255, 0.1);
}
```

## 🔧 admin.js - JavaScript cho Admin Panel

### Chức năng chính:

#### 1. Active Menu Auto-Detection
```javascript
// Tự động set active menu dựa trên URL
setActiveMenu();
```

#### 2. Sidebar Toggle
```javascript
// Toggle sidebar collapsed/expanded
toggleSidebar();

// Lưu trạng thái vào localStorage
restoreSidebarState();
```

#### 3. Delete Modal Handler
```javascript
// Mở modal xác nhận xóa
openDeleteModal('/admin/product/delete/123', 'Tên sản phẩm');
```

#### 4. Table Utilities
```javascript
// Select all checkboxes
initTableCheckboxes();

// Search filter
initTableSearch('searchInput', 3); // Column index 3
```

#### 5. Toast Notification
```javascript
// Hiển thị thông báo
showToast('Xóa thành công!', 'success');
showToast('Có lỗi xảy ra!', 'error');
showToast('Cảnh báo!', 'warning');
showToast('Thông tin', 'info');
```

#### 6. Confirm Action
```javascript
// Confirm trước khi thực hiện
confirmAction('Bạn có chắc?', function() {
    // Do something
});
```

### Sử dụng:
File được tự động load trong `admin-layout.html`:
```html
<script th:src="@{/js/admin.js}"></script>
```

### Các hàm có thể gọi từ HTML:

```html
<!-- Toggle Sidebar -->
<button onclick="toggleSidebar()">Toggle</button>

<!-- Delete Modal -->
<button onclick="openDeleteModal('/admin/product/delete/1', 'Product Name')">
    Xóa
</button>

<!-- Toast -->
<button onclick="showToast('Success!', 'success')">Show Toast</button>

<!-- Confirm -->
<button onclick="confirmAction('Sure?', function() { alert('OK'); })">
    Confirm
</button>
```

## 🔄 Workflow phát triển

### Khi thêm tính năng mới cho Admin:

1. **Thêm CSS vào `admin.css`**
   ```css
   /* Thêm style mới */
   .my-new-component {
       /* styles */
   }
   ```

2. **Thêm JavaScript vào `admin.js`**
   ```javascript
   // Thêm function mới
   function myNewFunction() {
       // logic
   }
   
   // Export để có thể gọi từ HTML
   window.myNewFunction = myNewFunction;
   ```

3. **Sử dụng trong template**
   ```html
   <div class="my-new-component">
       <button onclick="myNewFunction()">Click</button>
   </div>
   ```

## 📝 Best Practices

### ✅ DO:
- Tách CSS và JS ra file riêng
- Đặt tên class/function rõ ràng
- Comment code phức tạp
- Sử dụng biến CSS cho màu sắc, kích thước
- Export function cần thiết ra window
- Lưu state vào localStorage khi cần

### ❌ DON'T:
- Viết inline CSS trong HTML
- Viết inline JavaScript trong HTML (trừ onclick đơn giản)
- Duplicate code
- Hardcode màu sắc, kích thước
- Quên comment code phức tạp

## 🐛 Debugging

### Kiểm tra file CSS/JS có load không:
```javascript
// Mở Console (F12)
console.log('Admin CSS loaded:', !!document.querySelector('link[href*="admin.css"]'));
console.log('Admin JS loaded:', typeof toggleSidebar === 'function');
```

### Kiểm tra active menu:
```javascript
// Xem menu nào đang active
document.querySelectorAll('.nav-link.active').forEach(el => {
    console.log('Active:', el.textContent.trim());
});
```

### Kiểm tra localStorage:
```javascript
// Xem trạng thái sidebar
console.log('Sidebar collapsed:', localStorage.getItem('sidebarCollapsed'));
```

## 📚 Tham khảo

- **Layout**: `templates/admin/admin-layout.html`
- **CSS**: `static/css/admin.css`
- **JS**: `static/js/admin.js`
- **Example**: `templates/admin/products/list.html`
- **Delete Modal Guide**: `templates/admin/README-DELETE-MODAL.md`
