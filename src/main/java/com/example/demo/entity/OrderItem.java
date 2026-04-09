package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Data
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nhiều OrderItem thuộc về 1 Order
    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    // Sản phẩm nào
    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    // Số lượng mua
    private Integer quantity;

    // Giá TẠI THỜI ĐIỂM MUA — quan trọng!
    // Không lấy product.getPrice() vì giá có thể thay đổi sau
    @Column(nullable = false)
    private BigDecimal priceAtTime;
}