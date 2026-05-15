// ============================================
// ADMIN FILTER - Universal filter for all admin pages
// ============================================

document.addEventListener('DOMContentLoaded', function () {

    // ========== PRODUCT PAGE FILTER ==========
    if (document.querySelector('#categoryFilter') && document.querySelector('#statusFilter')) {
        initProductFilter();
    }

    // ========== ACCOUNT PAGE FILTER ==========
    if (document.querySelector('#roleFilter') && document.querySelector('#accountsTable')) {
        initAccountFilter();
    }

    // ========== CATEGORY PAGE FILTER ==========
    if (document.querySelector('#sortFilter') && document.querySelector('#sortButton')) {
        initCategoryFilter();
    }
});

// ========== PRODUCT FILTER ==========
function initProductFilter() {
    const categoryMap = {
        'MAY_PHA': 'Máy Pha Cà Phê',
        'MAY_XAY': 'Máy Xay Cà Phê',
        'XAY_EP': 'Máy Xay & Máy Ép',
        'CF_AN_VAT': 'Cà Phê & Đồ Ăn Vặt',
        'DUNG_CU': 'Dụng Cụ Pha Chế',
        'HANG_CU': 'Máy Pha & Xay Cũ Lướt'
    };

    function applyProductFilter() {
        const searchValue = document.getElementById('searchInput').value.toLowerCase();
        const categoryValue = document.getElementById('categoryFilter').value;
        const statusValue = document.getElementById('statusFilter').value;
        const rows = document.querySelectorAll('tbody tr');

        rows.forEach(row => {
            if (row.querySelector('td[colspan]')) return;

            let show = true;
            const cells = row.querySelectorAll('td');

            if (searchValue && cells[2]) {
                const productName = cells[2].textContent.toLowerCase();
                if (!productName.includes(searchValue)) show = false;
            }

            if (show && categoryValue && cells[3]) {
                const categoryBadge = cells[3].querySelector('.badge');
                if (categoryBadge) {
                    const categoryName = categoryBadge.textContent.trim();
                    const expectedCategory = categoryMap[categoryValue];
                    if (categoryName !== expectedCategory) show = false;
                }
            }

            if (show && statusValue && cells[6]) {
                const statusBadge = cells[6].querySelector('.badge');
                if (statusBadge) {
                    if (statusValue === '1' && !statusBadge.classList.contains('bg-success')) show = false;
                    if (statusValue === '0' && !statusBadge.classList.contains('bg-secondary')) show = false;
                }
            }

            row.style.display = show ? '' : 'none';
        });
    }

    document.getElementById('searchInput').addEventListener('keyup', applyProductFilter);
    document.getElementById('categoryFilter').addEventListener('change', applyProductFilter);
    document.getElementById('statusFilter').addEventListener('change', applyProductFilter);

    const resetBtn = document.getElementById('resetFilterBtn');
    if (resetBtn) {
        resetBtn.addEventListener('click', function () {
            document.getElementById('searchInput').value = '';
            document.getElementById('categoryFilter').value = '';
            document.getElementById('statusFilter').value = '';
            document.querySelectorAll('tbody tr').forEach(row => row.style.display = '');
        });
    }
}

// ========== ACCOUNT FILTER ==========
function initAccountFilter() {
    function applyAccountFilter() {
        const searchTerm = document.getElementById('searchInput').value.toLowerCase();
        const roleFilter = document.getElementById('roleFilter').value;
        const statusFilter = document.getElementById('statusFilter').value;
        const rows = document.querySelectorAll('#accountsTable tbody tr');

        rows.forEach(row => {
            let showRow = true;

            if (searchTerm) {
                const text = row.textContent.toLowerCase();
                if (!text.includes(searchTerm)) showRow = false;
            }

            if (showRow && roleFilter) {
                const roleBadge = row.querySelector('td:nth-child(5) .badge');
                const isAdmin = roleBadge && roleBadge.classList.contains('bg-danger');
                if ((roleFilter === 'admin' && !isAdmin) || (roleFilter === 'user' && isAdmin)) {
                    showRow = false;
                }
            }

            if (showRow && statusFilter) {
                const statusBadge = row.querySelector('td:nth-child(6) .badge');
                const isActive = statusBadge && statusBadge.classList.contains('bg-success');
                if ((statusFilter === '1' && !isActive) || (statusFilter === '0' && isActive)) {
                    showRow = false;
                }
            }

            row.style.display = showRow ? '' : 'none';
        });
    }

    document.getElementById('searchInput').addEventListener('input', applyAccountFilter);
    document.getElementById('roleFilter').addEventListener('change', applyAccountFilter);
    document.getElementById('statusFilter').addEventListener('change', applyAccountFilter);

    const resetBtn = document.getElementById('resetFilterBtn');
    if (resetBtn) {
        resetBtn.addEventListener('click', function () {
            document.getElementById('searchInput').value = '';
            document.getElementById('roleFilter').value = '';
            document.getElementById('statusFilter').value = '';
            document.querySelectorAll('#accountsTable tbody tr').forEach(row => row.style.display = '');
        });
    }
}

// ========== CATEGORY FILTER ==========
function initCategoryFilter() {
    function applySearch() {
        const searchValue = document.getElementById('searchInput').value.toLowerCase();
        const rows = document.querySelectorAll('tbody tr');

        rows.forEach(row => {
            if (row.querySelector('td[colspan]')) return;

            const cells = row.querySelectorAll('td');
            const categoryId = cells[2] ? cells[2].textContent.toLowerCase() : '';
            const categoryName = cells[3] ? cells[3].textContent.toLowerCase() : '';

            row.style.display = (categoryId.includes(searchValue) || categoryName.includes(searchValue)) ? '' : 'none';
        });
    }

    function applySort() {
        const sortBy = document.getElementById('sortFilter').value;
        const tbody = document.querySelector('tbody');
        const rows = Array.from(tbody.querySelectorAll('tr')).filter(row => !row.querySelector('td[colspan]'));

        rows.sort((a, b) => {
            const aCells = a.querySelectorAll('td');
            const bCells = b.querySelectorAll('td');

            switch (sortBy) {
                case 'id':
                    return (aCells[2]?.textContent.trim() || '').localeCompare(bCells[2]?.textContent.trim() || '');
                case 'name':
                    return (aCells[3]?.textContent.trim() || '').localeCompare(bCells[3]?.textContent.trim() || '');
                case 'products':
                    const aCount = parseInt((aCells[4]?.textContent || '0').match(/\d+/)?.[0] || '0');
                    const bCount = parseInt((bCells[4]?.textContent || '0').match(/\d+/)?.[0] || '0');
                    return bCount - aCount;
                default:
                    return 0;
            }
        });

        rows.forEach(row => tbody.appendChild(row));
    }

    document.getElementById('searchInput').addEventListener('keyup', applySearch);
    document.getElementById('sortFilter').addEventListener('change', applySort);
    document.getElementById('sortButton').addEventListener('click', applySort);

    const resetBtn = document.getElementById('resetFilterBtn');
    if (resetBtn) {
        resetBtn.addEventListener('click', function () {
            document.getElementById('searchInput').value = '';
            document.getElementById('sortFilter').value = 'id';
            document.querySelectorAll('tbody tr').forEach(row => row.style.display = '');
        });
    }
}
