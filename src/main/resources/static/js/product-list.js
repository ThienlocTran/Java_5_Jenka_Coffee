// ============================================
// PRODUCT LIST - Filter & Search Functionality
// ============================================

$(document).ready(function () {
    // Cache DOM elements
    const $products = $('.product-card').parent(); // Get parent col divs
    const $priceFilters = $('input[id^="price"]');
    const $searchInput = $('#searchInput');
    const $sortSelect = $('#sortSelect');

    // --- CLIENT-SIDE FILTERING ---

    // Price Filter
    $priceFilters.on('change', function () {
        filterProducts();
    });

    // Search by name
    if ($searchInput.length) {
        $searchInput.on('keyup', debounce(function () {
            filterProducts();
        }, 300));
    }

    // Sort functionality
    if ($sortSelect.length) {
        $sortSelect.on('change', function () {
            sortProducts($(this).val());
        });
    }

    function filterProducts() {
        const searchTerm = ($searchInput.val() || '').toLowerCase();

        // Get selected price ranges
        const selectedPrices = [];
        $('input[id^="price"]:checked').each(function () {
            const id = $(this).attr('id');
            switch (id) {
                case 'price1': selectedPrices.push({ min: 0, max: 1000000 }); break;
                case 'price2': selectedPrices.push({ min: 1000000, max: 5000000 }); break;
                case 'price3': selectedPrices.push({ min: 5000000, max: 10000000 }); break;
                case 'price4': selectedPrices.push({ min: 10000000, max: Infinity }); break;
            }
        });

        let visibleCount = 0;

        $products.each(function () {
            const $card = $(this).find('.product-card');
            const productName = $card.find('.card-title').text().toLowerCase();
            const priceText = $card.find('.text-danger.fw-bold').text().replace(/[₫,.\s]/g, '');
            const productPrice = parseInt(priceText);

            // Filter by search term
            const matchesSearch = searchTerm === '' || productName.includes(searchTerm);

            // Filter by price range
            let matchesPrice = selectedPrices.length === 0; // Show all if no filter selected
            if (selectedPrices.length > 0) {
                matchesPrice = selectedPrices.some(range =>
                    productPrice >= range.min && productPrice < range.max
                );
            }

            // Show/hide product
            if (matchesSearch && matchesPrice) {
                $(this).show();
                visibleCount++;
            } else {
                $(this).hide();
            }
        });

        // Update count
        $('.text-muted.small b').text(visibleCount);
    }

    function sortProducts(sortType) {
        const $container = $('.row.row-cols-1.row-cols-md-3');
        const $items = $container.find('.col').get();

        $items.sort(function (a, b) {
            const $aCard = $(a).find('.product-card');
            const $bCard = $(b).find('.product-card');

            switch (sortType) {
                case '1': // Price low to high
                    const priceA = parseInt($aCard.find('.text-danger.fw-bold').text().replace(/[₫,.\s]/g, ''));
                    const priceB = parseInt($bCard.find('.text-danger.fw-bold').text().replace(/[₫,.\s]/g, ''));
                    return priceA - priceB;

                case '2': // Price high to low
                    const priceA2 = parseInt($aCard.find('.text-danger.fw-bold').text().replace(/[₫,.\s]/g, ''));
                    const priceB2 = parseInt($bCard.find('.text-danger.fw-bold').text().replace(/[₫,.\s]/g, ''));
                    return priceB2 - priceA2;

                case '3': // Best sellers (placeholder - would need sold count data)
                    // For now, keep original order or randomize
                    return 0;

                default: // Newest (original order)
                    return 0;
            }
        });

        $.each($items, function (idx, item) {
            $container.append(item);
        });
    }

    // Debounce utility
    function debounce(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    }
});
