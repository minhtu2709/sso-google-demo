package com.example.demo.dto;

import com.example.demo.entity.Category;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CategoryResponse {

    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt;  // thêm vào
    private LocalDateTime updatedAt;  // thêm vào

    public static CategoryResponse from(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .createdAt(category.getCreatedAt())  // thêm vào
                .updatedAt(category.getUpdatedAt())  // thêm vào
                .build();
    }
}