package com.example.demo.repository;

import com.example.demo.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // Tìm CartItem theo cart và product
    // Dùng khi user thêm sản phẩm đã có trong giỏ → tăng số lượng thay vì tạo mới
    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);

    @Modifying
    @Transactional
    @Query("DELETE FROM CartItem ci WHERE ci.createdAt < :expiryDate")
    void deleteExpiredItems(LocalDateTime expiryDate);
}