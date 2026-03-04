package com.springboot.jenka_coffee.tuan_khoa;

import com.springboot.jenka_coffee.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class CartServiceIntegrationTest {

    @Autowired
    private CartService cartService;

    @BeforeEach
    void setUp() {
        // Đảm bảo giỏ hàng luôn trống trước mỗi Test Case
        cartService.clear();
    }

    @Test
    @DisplayName("TC_CART_001: Thêm sản phẩm mới vào giỏ hàng")
    void testTC_CART_001_AddNewProduct() {
        // Hành động: Thêm SP ID = 1 (Máy Pha Breville - Giá DB thật: 18.500.000đ)
        cartService.add(1);

        // Kiểm tra
        assertEquals(1, cartService.getItems().size(), "Giỏ hàng phải có 1 SP");
        assertEquals(1, cartService.getItems().iterator().next().getQuantity(), "Số lượng phải là 1");

        assertEquals(18500000.0, cartService.getAmount(), "Tổng tiền phải bằng 18.500.000đ");
    }

    @Test
    @DisplayName("TC_CART_002: Cộng dồn số lượng SP đã có trong giỏ")
    void testTC_CART_002_AddExistingProduct() {
        cartService.add(1); // Thêm lần 1
        cartService.add(1); // Thêm lần 2

        assertEquals(1, cartService.getItems().size(), "Không tạo dòng mới, chỉ giữ 1 dòng");
        assertEquals(2, cartService.getItems().iterator().next().getQuantity(), "Số lượng phải tăng lên 2");
        assertEquals(37000000.0, cartService.getAmount(), "Tổng tiền phải là 37.000.000đ");
    }

    @Test
    @DisplayName("TC_CART_003: Xóa sản phẩm khỏi giỏ hàng")
    void testTC_CART_003_RemoveProduct() {
        cartService.add(1); // Thêm vào
        assertEquals(1, cartService.getItems().size());

        cartService.remove(1); // Xóa đi

        assertEquals(0, cartService.getItems().size(), "Giỏ hàng phải trống");
        assertEquals(0.0, cartService.getAmount(), "Tổng tiền phải về 0");
    }

    @Test
    @DisplayName("TC_CART_004: Cập nhật số lượng tăng")
    void testTC_CART_004_UpdateQuantityIncrease() {
        cartService.add(1); // Mặc định qty = 1

        // Cập nhật lên 5
        cartService.update(1, 5);

        assertEquals(5, cartService.getItems().iterator().next().getQuantity());
        assertEquals(92500000.0, cartService.getAmount(), "Tổng tiền: 18.5m x 5 = 92.5m");
    }

    @Test
    @DisplayName("TC_CART_005: Cập nhật số lượng giảm")
    void testTC_CART_005_UpdateQuantityDecrease() {
        cartService.add(1);
        cartService.update(1, 5); // Tăng lên 5
        cartService.update(1, 2); // Giảm xuống 2

        assertEquals(2, cartService.getItems().iterator().next().getQuantity());
        assertEquals(37000000.0, cartService.getAmount(), "Tổng tiền: 18.5m x 2 = 37m");
    }

    @Test
    @DisplayName("TC_CART_006: Cập nhật số lượng bằng 0 (Tự động xóa)")
    void testTC_CART_006_UpdateQuantityToZero() {
        cartService.add(1);

        // Nếu code chuẩn, truyền số 0 vào thì món hàng sẽ bị remove
        cartService.update(1, 0);

        assertEquals(0, cartService.getItems().size(), "Sản phẩm phải bị xóa khỏi giỏ");
    }
    // Khi người dùng nhập số lượng bằng 0, thay vì tự động xóa khỏi giỏ hàng thì hệ thống lại giữ nguyên

    @Test
    @DisplayName("TC_CART_007: Cập nhật số lượng âm")
    void testTC_CART_007_UpdateNegativeQuantity() {
        cartService.add(1);

        // Nếu code của bạn quăng lỗi (ném Exception) khi nhập số âm:
        Exception exception = assertThrows(Exception.class, () -> {
            cartService.update(1, -1);
        });
    }
    // cái này fail do code ko ném exception ra nên sẽ báo fail, nghiệp vụ xử lý nếu như ném exception thì test case này sẽ true, ở đây code ko ném ra gì cả là fail

    @Test
    @DisplayName("TC_CART_009: Xóa toàn bộ giỏ hàng")
    void testTC_CART_009_ClearCart() {
        cartService.add(1); // Thêm máy Breville
        cartService.add(2); // Thêm máy Nuova Simonelli (65m)

        assertEquals(2, cartService.getItems().size()); // Có 2 mặt hàng

        cartService.clear(); // Bấm "Xóa tất cả"

        assertTrue(cartService.getItems().isEmpty(), "Giỏ hàng phải trống hoàn toàn");
        assertEquals(0.0, cartService.getAmount(), "Tổng tiền = 0");
    }

    @Test
    @DisplayName("TC_CART_010: Kiểm tra tổng tiền chính xác (Nhiều loại SP)")
    void testTC_CART_010_CheckExactTotalAmount() {
        // Thêm 2 Máy Breville (18.500.000 x 2 = 37.000.000)
        cartService.add(1);
        cartService.add(1);

        // Thêm 1 Máy Nuova Simonelli (65.000.000)
        cartService.add(2);

        // Tổng cộng: 37.000.000 + 65.000.000 = 102.000.000đ
        assertEquals(102000000.0, cartService.getAmount(), "Tổng tiền phải là 102 triệu đồng");
    }


}