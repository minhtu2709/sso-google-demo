package com.example.demo.repository;

import com.example.demo.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // Tìm CartItem theo cart và product
    // Dùng khi user thêm sản phẩm đã có trong giỏ → tăng số lượng thay vì tạo mới
    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);
}