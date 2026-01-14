$(document).ready(function() {
    // Event delegation for dynamically added elements
    $(document).on('click', '.btn-add-to-cart', function(e) {
        e.preventDefault();
        var productId = $(this).data('id');
        var btn = $(this);
        
        // Animation
        var img = btn.closest('.product-card').find('.card-img-top');
        if (img.length) {
            var cartIcon = $('.cart-icon-wrap');
            var imgClone = img.clone().offset({
                top: img.offset().top,
                left: img.offset().left
            }).css({
                'opacity': '0.8',
                'position': 'absolute',
                'height': '150px',
                'width': '150px',
                'z-index': '1000'
            }).appendTo($('body')).animate({
                'top': cartIcon.offset().top + 10,
                'left': cartIcon.offset().left + 10,
                'width': '75px',
                'height': '75px'
            }, 1000, 'easeInOutExpo');
            
            setTimeout(function() {
                cartIcon.effect("shake", {
                    times: 2
                }, 200);
            }, 1500);

            imgClone.animate({
                'width': 0,
                'height': 0
            }, function() {
                $(this).detach();
            });
        }

        // AJAX Call
        $.ajax({
            url: '/cart/api/add/' + productId,
            type: 'GET',
            success: function(response) {
                // Update Badge
                $('.cart-badge').text(response.count);
                $('.mini-cart-total .text-danger').text(new Intl.NumberFormat('vi-VN').format(response.total) + ' đ');
                
                // Show Notification Popup (after animation roughly finishes)
                setTimeout(function() {
                    $('.cart-notification').fadeIn(300).css('display', 'flex');
                    setTimeout(function() {
                        $('.cart-notification').fadeOut(300);
                    }, 2000); // Show for 2 seconds
                }, 1000);

                // Show Dropdown temporarily (Restored per user feedback style)
                $('.mini-cart-dropdown').css('display', 'block');
                setTimeout(function() {
                    $('.mini-cart-dropdown').css('display', '');
                }, 3000);
                
                // Update Mini Cart Items
                var itemsHtml = '';
                if (response.items.length === 0) {
                    itemsHtml = '<div class="text-center py-3"><p class="mb-0 text-muted small">Giỏ hàng trống</p></div>';
                } else {
                    var reversedItems = response.items.slice().reverse();
                    var count = 0;
                    $.each(reversedItems, function(index, item) {
                        if (count < 3) {
                            itemsHtml += `
                            <div class="mini-cart-item">
                                <img src="/images/${item.image}" alt="Product" class="mini-cart-img">
                                <div class="mini-cart-info">
                                    <div class="d-flex justify-content-between align-items-start">
                                        <div>
                                            <a href="/cart/view" class="text-decoration-none text-dark">
                                                <p class="mini-cart-title">${item.name}</p>
                                            </a>
                                            <span class="mini-cart-price">${new Intl.NumberFormat('vi-VN').format(item.price)} đ</span>
                                        </div>
                                        <a href="/cart/remove/${item.productId}" class="text-secondary remove-item"><i class="fas fa-times-circle"></i></a>
                                    </div>
                                    <div class="d-flex align-items-center mt-2 justify-content-between">
                                         <span class="small text-muted">x <span>${item.quantity}</span></span>
                                    </div>
                                </div>
                            </div>`;
                            count++;
                        }
                    });
                }
                $('.mini-cart-items').html(itemsHtml);
            },
            error: function(err) {
                console.error('Error adding to cart', err);
            }
        });
    });
});
