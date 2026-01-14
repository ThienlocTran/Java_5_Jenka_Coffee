/**
 * ADMIN PANEL JAVASCRIPT - Jenka Coffee
 * Xử lý các chức năng chung cho trang Admin
 */

// ========== SET ACTIVE MENU ==========
document.addEventListener('DOMContentLoaded', function() {
    setActiveMenu();
});

/**
 * Tự động set active menu dựa trên URL hiện tại
 */
function setActiveMenu() {
    const currentPath = window.location.pathname;
    const menuLinks = document.querySelectorAll('.sidebar-menu .nav-link[data-menu]');
    
    menuLinks.forEach(link => {
        const href = link.getAttribute('href');
        // Check if current path contains the menu href
        if (currentPath.includes(href) || currentPath.startsWith(href)) {
            link.classList.add('active');
        }
    });
}

// ========== SIDEBAR TOGGLE ==========
/**
 * Toggle sidebar collapsed/expanded
 */
function toggleSidebar() {
    document.body.classList.toggle('sidebar-collapsed');
    
    // Lưu trạng thái vào localStorage
    const isCollapsed = document.body.classList.contains('sidebar-collapsed');
    localStorage.setItem('sidebarCollapsed', isCollapsed);
}

/**
 * Khôi phục trạng thái sidebar từ localStorage
 */
function restoreSidebarState() {
    const isCollapsed = localStorage.getItem('sidebarCollapsed') === 'true';
    if (isCollapsed) {
        document.body.classList.add('sidebar-collapsed');
    }
}

// Khôi phục trạng thái khi load trang
document.addEventListener('DOMContentLoaded', restoreSidebarState);

// ========== MOBILE SIDEBAR ==========
/**
 * Xử lý sidebar trên mobile
 */
if (window.innerWidth <= 768) {
    const toggleBtn = document.querySelector('.navbar-toggle');
    if (toggleBtn) {
        toggleBtn.addEventListener('click', function() {
            document.body.classList.toggle('sidebar-open');
        });
    }

    // Đóng sidebar khi click bên ngoài
    document.addEventListener('click', function(e) {
        const sidebar = document.querySelector('.admin-sidebar');
        const toggle = document.querySelector('.navbar-toggle');
        
        if (sidebar && toggle && 
            !sidebar.contains(e.target) && 
            !toggle.contains(e.target) &&
            document.body.classList.contains('sidebar-open')) {
            document.body.classList.remove('sidebar-open');
        }
    });
}

// ========== DELETE MODAL HANDLER ==========
let deleteUrl = '';

/**
 * Mở modal xác nhận xóa
 * @param {string} url - URL để xóa (VD: /admin/product/delete/123)
 * @param {string} itemName - Tên item để hiển thị (optional)
 */
function openDeleteModal(url, itemName = '') {
    deleteUrl = url;
    
    // Cập nhật message nếu có tên item
    const messageElement = document.querySelector('.delete-message');
    if (messageElement) {
        if (itemName) {
            messageElement.textContent = `Bạn có chắc chắn muốn xóa "${itemName}"?`;
        } else {
            messageElement.textContent = 'Bạn có chắc chắn muốn xóa?';
        }
    }
    
    // Hiển thị modal
    const modalElement = document.getElementById('deleteModal');
    if (modalElement) {
        const modal = new bootstrap.Modal(modalElement);
        modal.show();
    }
}

/**
 * Xử lý khi click nút Xóa trong modal
 */
document.addEventListener('DOMContentLoaded', function() {
    const confirmBtn = document.getElementById('confirmDeleteBtn');
    if (confirmBtn) {
        confirmBtn.addEventListener('click', function(e) {
            e.preventDefault();
            if (deleteUrl) {
                window.location.href = deleteUrl;
            }
        });
    }

    // Reset khi đóng modal
    const modalElement = document.getElementById('deleteModal');
    if (modalElement) {
        modalElement.addEventListener('hidden.bs.modal', function() {
            deleteUrl = '';
            const messageElement = document.querySelector('.delete-message');
            if (messageElement) {
                messageElement.textContent = 'Bạn có chắc chắn muốn xóa?';
            }
        });
    }
});

// ========== TABLE UTILITIES ==========
/**
 * Select all checkboxes trong table
 */
function initTableCheckboxes() {
    const selectAllCheckbox = document.querySelector('thead input[type="checkbox"]');
    if (selectAllCheckbox) {
        selectAllCheckbox.addEventListener('change', function() {
            const checkboxes = document.querySelectorAll('tbody input[type="checkbox"]');
            checkboxes.forEach(checkbox => {
                checkbox.checked = this.checked;
            });
        });
    }
}

document.addEventListener('DOMContentLoaded', initTableCheckboxes);

// ========== SEARCH FILTER ==========
/**
 * Lọc table theo search input
 * @param {string} inputId - ID của input search
 * @param {number} columnIndex - Index của cột cần search (bắt đầu từ 1)
 */
function initTableSearch(inputId, columnIndex = 3) {
    const searchInput = document.getElementById(inputId);
    if (searchInput) {
        searchInput.addEventListener('keyup', function() {
            const searchValue = this.value.toLowerCase();
            const rows = document.querySelectorAll('tbody tr');
            
            rows.forEach(row => {
                const cell = row.querySelector(`td:nth-child(${columnIndex})`);
                if (cell) {
                    const text = cell.textContent.toLowerCase();
                    row.style.display = text.includes(searchValue) ? '' : 'none';
                }
            });
        });
    }
}

// ========== TOAST NOTIFICATION ==========
/**
 * Hiển thị toast notification
 * @param {string} message - Nội dung thông báo
 * @param {string} type - Loại: success, error, warning, info
 */
function showToast(message, type = 'success') {
    // Tạo toast element nếu chưa có
    let toastContainer = document.getElementById('toastContainer');
    if (!toastContainer) {
        toastContainer = document.createElement('div');
        toastContainer.id = 'toastContainer';
        toastContainer.style.cssText = 'position: fixed; top: 20px; right: 20px; z-index: 9999;';
        document.body.appendChild(toastContainer);
    }

    const toastId = 'toast-' + Date.now();
    const bgColor = {
        success: '#28a745',
        error: '#dc3545',
        warning: '#ffc107',
        info: '#17a2b8'
    }[type] || '#28a745';

    const toast = document.createElement('div');
    toast.id = toastId;
    toast.className = 'toast align-items-center text-white border-0';
    toast.style.cssText = `background-color: ${bgColor}; min-width: 300px;`;
    toast.setAttribute('role', 'alert');
    toast.innerHTML = `
        <div class="d-flex">
            <div class="toast-body">${message}</div>
            <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
        </div>
    `;

    toastContainer.appendChild(toast);
    const bsToast = new bootstrap.Toast(toast, { delay: 3000 });
    bsToast.show();

    // Xóa toast sau khi ẩn
    toast.addEventListener('hidden.bs.toast', function() {
        toast.remove();
    });
}

// ========== CONFIRM ACTION ==========
/**
 * Confirm trước khi thực hiện action
 * @param {string} message - Thông báo xác nhận
 * @param {function} callback - Hàm callback nếu user confirm
 */
function confirmAction(message, callback) {
    if (confirm(message)) {
        callback();
    }
}

// ========== EXPORT FUNCTIONS ==========
// Export các hàm để có thể gọi từ HTML
window.toggleSidebar = toggleSidebar;
window.openDeleteModal = openDeleteModal;
window.showToast = showToast;
window.confirmAction = confirmAction;
window.initTableSearch = initTableSearch;
