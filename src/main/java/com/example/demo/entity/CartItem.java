package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "cart_items", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"cart_id", "product_id"})
})
@Data
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nhiều CartItem thuộc về 1 Cart
    @ManyToOne
    @JoinColumn(name = "cart_id")
    private Cart cart;

    // Nhiều CartItem có thể trỏ đến cùng 1 Product
    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    // Số lượng sản phẩm trong giỏ
    private Integer quantity;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}