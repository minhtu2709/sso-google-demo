package com.example.demo.dto;

import com.example.demo.entity.Product;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ProductResponse {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private String imageUrl;
    private String status;
    private CategoryResponse category;
    private LocalDateTime createdAt;  // thêm vào
    private LocalDateTime updatedAt;  // thêm vào

    public static ProductResponse from(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .imageUrl(product.getImageUrl())
                .status(product.getStatus())
                .category(CategoryResponse.from(product.getCategory()))
                .createdAt(product.getCreatedAt())  // thêm vào
                .updatedAt(product.getUpdatedAt())  // thêm vào
                .build();
    }
}