package com.springboot.jenka_coffee.repository;

import com.springboot.jenka_coffee.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByCartKey(String cartKey);

    Optional<CartItem> findByCartKeyAndProductId(String cartKey, Integer productId);

    void deleteByCartKey(String cartKey);

    void deleteByCartKeyAndProductId(String cartKey, Integer productId);

    long countByCartKey(String cartKey);

    /**
     * Xóa ONLY anonymous carts (cart_key LIKE 'anon:%') không được cập nhật quá threshold.
     *
     * Logged-in user carts (cart_key = username) KHÔNG bao giờ bị tự xóa.
     * Chúng tồn tại vĩnh viễn để kích thích mua hàng — user đăng nhập bất kỳ
     * thiết bị nào vẫn thấy giỏ hàng cũ của mình.
     *
     * @param threshold cutoff time — xóa cart_items có updated_at < threshold
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM CartItem c WHERE c.cartKey LIKE 'anon:%' AND c.updatedAt < :threshold")
    int deleteStaleAnonymousCartItems(@Param("threshold") LocalDateTime threshold);

    /**
     * Xóa cart items của user đã bị xóa khỏi Accounts (orphan cleanup).
     * Chỉ target cart_key không phải anon và không tồn tại trong danh sách username.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM CartItem c WHERE c.cartKey NOT LIKE 'anon:%' AND c.cartKey NOT IN :existingUsernames")
    int deleteOrphanUserCartItems(@Param("existingUsernames") List<String> existingUsernames);
}
