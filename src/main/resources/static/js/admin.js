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

// ========== TOGGLE STATUS MODAL HANDLER ==========
let toggleUrl = '';

/**
 * Mở modal xác nhận toggle status
 * @param {string} url - URL để toggle (VD: /admin/product/toggle/123)
 * @param {string} itemName - Tên item để hiển thị (optional)
 * @param {boolean} currentStatus - Trạng thái hiện tại (true = đang hiện, false = đang ẩn)
 */
function openToggleModal(url, itemName = '', currentStatus = true) {
    toggleUrl = url;
    
    // Cập nhật message dựa trên trạng thái hiện tại
    const messageElement = document.querySelector('.toggle-message');
    const iconElement = document.querySelector('.delete-icon i');
    
    if (messageElement) {
        if (currentStatus) {
            // Đang hiện -> sẽ ẩn
            messageElement.textContent = itemName 
                ? `Bạn có chắc chắn muốn ẩn "${itemName}"?`
                : 'Bạn có chắc chắn muốn ẩn sản phẩm này?';
            if (iconElement) {
                iconElement.className = 'fas fa-eye-slash';
            }
        } else {
            // Đang ẩn -> sẽ hiện
            messageElement.textContent = itemName 
                ? `Bạn có chắc chắn muốn hiện "${itemName}"?`
                : 'Bạn có chắc chắn muốn hiện sản phẩm này?';
            if (iconElement) {
                iconElement.className = 'fas fa-eye';
            }
        }
    }
    
    // Hiển thị modal
    const modalElement = document.getElementById('toggleModal');
    if (modalElement) {
        const modal = new bootstrap.Modal(modalElement);
        modal.show();
    }
}

/**
 * Khởi tạo event listeners cho các nút toggle
 */
function initToggleButtons() {
    document.querySelectorAll('.toggle-btn').forEach(btn => {
        btn.addEventListener('click', function(e) {
            e.preventDefault();
            const url = this.getAttribute('data-toggle-url');
            const itemName = this.getAttribute('data-item-name') || '';
            const currentStatus = this.getAttribute('data-current-status') === 'true';
            openToggleModal(url, itemName, currentStatus);
        });
    });
}

/**
 * Xử lý khi click nút Xác nhận trong modal
 */
document.addEventListener('DOMContentLoaded', function() {
    // Khởi tạo toggle buttons
    initToggleButtons();
    
    const confirmBtn = document.getElementById('confirmToggleBtn');
    if (confirmBtn) {
        confirmBtn.addEventListener('click', function(e) {
            e.preventDefault();
            if (toggleUrl) {
                window.location.href = toggleUrl;
            }
        });
    }

    // Reset khi đóng modal
    const modalElement = document.getElementById('toggleModal');
    if (modalElement) {
        modalElement.addEventListener('hidden.bs.modal', function() {
            toggleUrl = '';
            const messageElement = document.querySelector('.toggle-message');
            if (messageElement) {
                messageElement.textContent = 'Bạn có chắc chắn muốn thay đổi trạng thái?';
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
window.openToggleModal = openToggleModal;
window.showToast = showToast;
window.confirmAction = confirmAction;
window.initTableSearch = initTableSearch;
