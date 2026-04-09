package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Đơn hàng thuộc về ai
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    // Trạng thái đơn hàng — dùng Enum cho chắc chắn
    // không để String vì dễ bị typo như "PEDING", "pending"...
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    // Tổng tiền của đơn hàng — lưu lại tại thời điểm đặt
    // vì giá sản phẩm có thể thay đổi sau này
    @Column(nullable = false)
    private BigDecimal totalPrice;

    // Địa chỉ giao hàng
    private String shippingAddress;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // Enum trạng thái đơn hàng — để ngay trong class cho gọn
    public enum OrderStatus {
        PENDING,    // Chờ xác nhận
        CONFIRMED,  // Đã xác nhận
        SHIPPING,   // Đang giao
        DONE,       // Giao thành công
        CANCELLED   // Đã hủy
    }
}