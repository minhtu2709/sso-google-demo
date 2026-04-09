package com.example.demo.repository;

import com.example.demo.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // Lấy tất cả đơn hàng của 1 user
    // 1 user có nhiều order nên dùng List
    List<Order> findByUserId(Long userId);
}