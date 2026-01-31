// ============================================
// PRODUCT QUICK VIEW MODAL - OPTIMIZED
// ============================================

$(document).ready(function () {
    // Quick View Button Click Handler
    $(document).on('click', '.btn-quick-view', function (e) {
        e.preventDefault();
        e.stopPropagation(); // Prevent card click

        var productId = $(this).data('id');

        // Show loading state
        $('#quickViewModal').modal('show');
        $('#quickViewContent').html('<div class="text-center py-5"><i class="fas fa-spinner fa-spin fa-3x text-primary"></i><p class="mt-3">Đang tải...</p></div>');

        // Fetch product details via JSON API (MUCH FASTER!)
        $.ajax({
            url: '/api/product/quick-view/' + productId,
            type: 'GET',
            dataType: 'json',
            success: function (data) {
                // data structure: { product, relatedProducts }
                var product = data.product;
                var stockBadge = getStockBadgeHTML(product.quantity);

                // Format price
                var price = new Intl.NumberFormat('vi-VN').format(product.price) + ' ₫';

                // Build modal content
                var quickViewHTML = `
                    <div class="row">
                        <div class="col-md-6">
                            <img src="${product.image || '/images/logo/logo.png'}" 
                                 class="img-fluid rounded shadow-sm" 
                                 alt="${product.name}"
                                 onerror="this.src='/images/logo/logo.png'">
                        </div>
                        <div class="col-md-6">
                            <h4 class="fw-bold mb-3">${product.name}</h4>
                            <div class="mb-3">
                                ${stockBadge}
                            </div>
                            <h3 class="text-danger fw-bold mb-3">${price}</h3>
                            <div class="product-description mb-4">
                                <p class="text-muted">${product.description || 'Liên hệ để biết thêm chi tiết.'}</p>
                            </div>
                            <div class="d-flex gap-2">
                                ${product.quantity > 0 ?
                        `<button class="btn btn-primary flex-fill btn-add-to-cart" data-id="${productId}">
                                        <i class="fas fa-shopping-cart me-2"></i> Thêm vào giỏ
                                    </button>` :
                        `<button class="btn btn-secondary flex-fill" disabled>
                                        <i class="fas fa-ban me-2"></i> Hết hàng
                                    </button>`
                    }
                                <a href="/product/detail/${productId}" class="btn btn-outline-secondary">
                                    <i class="fas fa-info-circle me-2"></i> Chi tiết
                                </a>
                            </div>
                        </div>
                    </div>
                `;

                $('#quickViewContent').html(quickViewHTML);
            },
            error: function () {
                $('#quickViewContent').html(`
                    <div class="text-center py-5">
                        <i class="fas fa-exclamation-triangle fa-3x text-danger mb-3"></i>
                        <p>Không thể tải thông tin sản phẩm.</p>
                        <button class="btn btn-secondary" data-bs-dismiss="modal">Đóng</button>
                    </div>
                `);
            }
        });
    });

    // Helper function to generate stock badge HTML
    function getStockBadgeHTML(quantity) {
        if (!quantity || quantity === 0) {
            return '<span class="stock-badge out-of-stock">Hết hàng</span>';
        } else if (quantity < 10) {
            return `<span class="stock-badge low-stock">Chỉ còn lại ${quantity} sản phẩm!</span>`;
        } else {
            return '<span class="stock-badge in-stock">Còn hàng</span>';
        }
    }
});
