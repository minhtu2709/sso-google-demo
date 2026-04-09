package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "categories")
@Data
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    // Tự động gán thời điểm INSERT, không bao giờ thay đổi sau đó
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // Tự động cập nhật mỗi khi record bị UPDATE
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}