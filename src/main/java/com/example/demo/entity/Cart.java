package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carts")
@Data
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Mỗi user chỉ có 1 giỏ hàng — quan hệ 1-1
    // unique = true đảm bảo không có 2 cart cùng 1 user
    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    // 1 cart có nhiều CartItem
    // cascade = ALL: khi xóa Cart thì xóa luôn các CartItem bên trong
    // orphanRemoval = true: nếu xóa CartItem khỏi list thì xóa luôn trong DB
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}