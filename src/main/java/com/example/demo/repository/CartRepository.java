package com.example.demo.repository;

import com.example.demo.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    // Tìm giỏ hàng theo user
    // Mỗi user chỉ có 1 cart nên dùng Optional
    Optional<Cart> findByUserId(Long userId);
}