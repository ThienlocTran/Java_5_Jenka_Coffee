// ============================================
// PRODUCT QUICK VIEW MODAL
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

        // Fetch product details via AJAX
        $.ajax({
            url: '/product/detail/' + productId,
            type: 'GET',
            success: function (html) {
                // Parse the HTML response to extract product details
                var $html = $(html);

                // Extract product information (adjust selectors based on actual detail page structure)
                var productName = $html.find('h1, h2, .product-title').first().text().trim();
                var productPrice = $html.find('.product-price, .text-danger').first().text().trim();
                var productImage = $html.find('.product-image img, .detail-img img').first().attr('src');
                var productDescription = $html.find('.product-description, .description').first().html() || '<p>Liên hệ để biết thêm chi tiết.</p>';
                var stockBadge = $html.find('.stock-badge').first().clone();

                // Build modal content
                var quickViewHTML = `
                    <div class="row">
                        <div class="col-md-6">
                            <img src="${productImage || '/images/logo/logo.png'}" 
                                 class="img-fluid rounded shadow-sm" 
                                 alt="${productName}"
                                 onerror="this.src='/images/logo/logo.png'">
                        </div>
                        <div class="col-md-6">
                            <h4 class="fw-bold mb-3">${productName}</h4>
                            <div class="mb-3">
                                ${stockBadge ? stockBadge[0].outerHTML : ''}
                            </div>
                            <h3 class="text-danger fw-bold mb-3">${productPrice}</h3>
                            <div class="product-description mb-4">
                                ${productDescription}
                            </div>
                            <div class="d-flex gap-2">
                                <button class="btn btn-primary flex-fill btn-add-to-cart" data-id="${productId}">
                                    <i class="fas fa-shopping-cart me-2"></i> Thêm vào giỏ
                                </button>
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
});
